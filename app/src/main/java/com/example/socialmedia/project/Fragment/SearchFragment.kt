package com.example.socialmedia.project.Fragment

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentSearchBinding
import com.example.socialmedia.project.Adapter.ExploreGridAdapter
import com.example.socialmedia.project.Adapter.SearchAdapter
import com.example.socialmedia.project.Adapter.SearchHistoryAdapter
import com.example.socialmedia.project.Domain.Model.PostMediaModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.Helper.SearchHistoryHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchAdapter: SearchAdapter
    private lateinit var historyAdapter: SearchHistoryAdapter
    private lateinit var exploreGridAdapter: ExploreGridAdapter
    private val database = FirebaseDatabase.getInstance()
    private lateinit var searchHistoryHelper: SearchHistoryHelper
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    private var searchJob: Job? = null
    private val followingList = mutableSetOf<String>()
    private val followersList = mutableSetOf<String>()

    private var isSearchMode = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchHistoryHelper = SearchHistoryHelper(requireContext())
        hideBottomNavigation()

        setupRecyclerViews()
        setupSearchBar()
        loadFollowData()

        // Hiển thị explore grid khi mới vào
        showExploreGrid()
        loadExplorePosts()
    }

    private fun hideBottomNavigation() {
        val bottomContainer = activity?.findViewById<View>(R.id.container)
        bottomContainer?.visibility = View.GONE

        val navHostFragment = activity?.findViewById<View>(R.id.navHostFragment)
        navHostFragment?.setPadding(
            navHostFragment.paddingLeft,
            navHostFragment.paddingTop,
            navHostFragment.paddingRight,
            0
        )
    }

    private fun showBottomNavigation() {
        val bottomContainer = activity?.findViewById<View>(R.id.container)
        bottomContainer?.visibility = View.VISIBLE

        val navHostFragment = activity?.findViewById<View>(R.id.navHostFragment)
        val paddingBottom = (80 * resources.displayMetrics.density).toInt()
        navHostFragment?.setPadding(
            navHostFragment.paddingLeft,
            navHostFragment.paddingTop,
            navHostFragment.paddingRight,
            paddingBottom
        )
    }

    private fun setupRecyclerViews() {
        // Explore Grid Adapter
        exploreGridAdapter = ExploreGridAdapter(
            onPostClick = { postMediaModel ->
                onPostClick(postMediaModel)
            }
        )

        binding.recyclerViewExplore.apply {
            // Sử dụng 3 columns để tạo layout 2x2 + 1 large
            val gridLayoutManager = GridLayoutManager(requireContext(), 3)
            gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return exploreGridAdapter.getSpanSize(position)
                }
            }
            layoutManager = gridLayoutManager
            adapter = exploreGridAdapter

            // Tắt animation để tránh glitch
            itemAnimator = null
        }

        // Search User Adapter
        searchAdapter = SearchAdapter(
            onUserClick = { user ->
                searchHistoryHelper.addSearchHistory(user)
                android.util.Log.d("SearchFragment", "User clicked: ${user.firstName} ${user.lastName}")
                // TODO: Navigate to user profile
            }
        )
        binding.recyclerViewSearch.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
        }

        // History Adapter
        historyAdapter = SearchHistoryAdapter(
            onUserClick = { user ->
                searchHistoryHelper.addSearchHistory(user)
                android.util.Log.d("SearchFragment", "History user clicked: ${user.firstName} ${user.lastName}")
                // TODO: Navigate to user profile
            },
            onRemoveClick = { user ->
                searchHistoryHelper.removeSearchHistory(user.userId)
                loadSearchHistory()
            }
        )
        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun onPostClick(postMediaModel: PostMediaModel) {
        android.util.Log.d("SearchFragment", "Post clicked: ${postMediaModel.postId}")
        // TODO: Navigate to post detail
    }

    private fun loadFollowData() {
        currentUserId?.let { userId ->
            database.reference.child(userId).child("following")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        followingList.clear()
                        for (followSnapshot in snapshot.children) {
                            val followedUserId = followSnapshot.key
                            val isFollowing = followSnapshot.getValue(Boolean::class.java)
                            if (isFollowing == true && followedUserId != null) {
                                followingList.add(followedUserId)
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })

            database.reference.child(userId).child("followers")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        followersList.clear()
                        for (followerSnapshot in snapshot.children) {
                            val followerUserId = followerSnapshot.key
                            val isFollower = followerSnapshot.getValue(Boolean::class.java)
                            if (isFollower == true && followerUserId != null) {
                                followersList.add(followerUserId)
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    private fun setupSearchBar() {
        // Khi focus vào search bar
        binding.editTextSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                isSearchMode = true
                if (binding.editTextSearch.text.toString().trim().isEmpty()) {
                    showHistory()
                }
            }
        }

        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                searchJob?.cancel()

                if (!isSearchMode) {
                    isSearchMode = true
                }

                if (query.isEmpty()) {
                    binding.btnClearSearch.visibility = View.GONE
                    if (binding.editTextSearch.hasFocus()) {
                        showHistory()
                    } else {
                        showExploreGrid()
                    }
                } else {
                    binding.btnClearSearch.visibility = View.VISIBLE
                    searchJob = viewLifecycleOwner.lifecycleScope.launch {
                        delay(300)
                        searchUsers(query)
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.editTextSearch.text.clear()
        }

        binding.textViewSeeMore.setOnClickListener {
            android.util.Log.d("SearchFragment", "See more clicked")
        }
    }

    private fun loadExplorePosts() {
        database.reference.child("posts")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val mediaList = mutableListOf<PostMediaModel>()

                    for (postSnapshot in snapshot.children) {
                        val postId = postSnapshot.key ?: continue

                        // Lấy media từ node mediaList
                        val mediaListSnapshot = postSnapshot.child("mediaList")
                        for (media in mediaListSnapshot.children) {
                            val mediaType = media.child("mediaType").getValue(String::class.java)
                            if (mediaType == "IMAGE") {
                                val postMediaModel = PostMediaModel(
                                    mediaId = media.child("mediaId").getValue(String::class.java) ?: "",
                                    postId = postId,
                                    mediaUrl = media.child("mediaUrl").getValue(String::class.java) ?: "",
                                    mediaOrder = media.child("mediaOrder").getValue(Int::class.java) ?: 0,
                                    width = media.child("width").getValue(Int::class.java) ?: 1080,
                                    height = media.child("height").getValue(Int::class.java) ?: 1080
                                )
                                mediaList.add(postMediaModel)
                            }
                        }
                    }

                    // Shuffle để hiển thị ngẫu nhiên
                    val shuffledList = mediaList.shuffled()
                    exploreGridAdapter.submitList(shuffledList)

                    android.util.Log.d("SearchFragment", "Loaded ${shuffledList.size} explore images")
                }

                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("SearchFragment", "Error loading posts: ${error.message}")
                }
            })
    }

    private fun showExploreGrid() {
        isSearchMode = false
        binding.recyclerViewExplore.visibility = View.VISIBLE
        binding.recyclerViewSearch.visibility = View.GONE
        binding.recyclerViewHistory.visibility = View.GONE
        binding.textViewSeeMore.visibility = View.GONE
        binding.layoutHistoryHeader.visibility = View.GONE
    }

    private fun searchUsers(query: String) {
        binding.recyclerViewExplore.visibility = View.GONE
        binding.recyclerViewHistory.visibility = View.GONE
        binding.recyclerViewSearch.visibility = View.VISIBLE
        binding.textViewSeeMore.visibility = View.GONE
        binding.layoutHistoryHeader.visibility = View.GONE

        database.reference.child("InfoUser")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val users = mutableListOf<UserModel>()
                    val searchQuery = query.lowercase()

                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(UserModel::class.java)
                        user?.let {
                            if (it.userId == currentUserId) return@let

                            val fullName = "${it.firstName} ${it.lastName}".trim().lowercase()
                            val email = it.email.lowercase()
                            val firstName = it.firstName.lowercase()
                            val lastName = it.lastName.lowercase()

                            if (fullName.contains(searchQuery) ||
                                email.contains(searchQuery) ||
                                firstName.startsWith(searchQuery) ||
                                lastName.startsWith(searchQuery) ||
                                firstName.contains(searchQuery) ||
                                lastName.contains(searchQuery)) {
                                users.add(it)
                            }
                        }
                    }

                    val sortedUsers = users.sortedWith(
                        compareByDescending<UserModel> { user ->
                            val isFollowing = followingList.contains(user.userId)
                            val isFollower = followersList.contains(user.userId)
                            when {
                                isFollowing && isFollower -> 3
                                isFollowing -> 2
                                isFollower -> 1
                                else -> 0
                            }
                        }.thenByDescending { user ->
                            val fullName = "${user.firstName} ${user.lastName}".trim().lowercase()
                            fullName.startsWith(searchQuery) ||
                                    user.firstName.lowercase().startsWith(searchQuery) ||
                                    user.lastName.lowercase().startsWith(searchQuery)
                        }.thenBy { user ->
                            "${user.firstName} ${user.lastName}".trim()
                        }
                    )

                    searchAdapter.submitList(sortedUsers)
                }

                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("SearchFragment", "Error: ${error.message}")
                }
            })
    }

    private fun showHistory() {
        binding.recyclerViewExplore.visibility = View.GONE
        binding.recyclerViewSearch.visibility = View.GONE

        val history = searchHistoryHelper.getSearchHistory()

        if (history.isNotEmpty()) {
            binding.recyclerViewHistory.visibility = View.VISIBLE
            binding.textViewSeeMore.visibility = View.VISIBLE
            binding.layoutHistoryHeader.visibility = View.VISIBLE
            historyAdapter.submitList(history)
        } else {
            binding.recyclerViewHistory.visibility = View.GONE
            binding.textViewSeeMore.visibility = View.GONE
            binding.layoutHistoryHeader.visibility = View.GONE
        }
    }

    private fun loadSearchHistory() {
        val history = searchHistoryHelper.getSearchHistory()
        historyAdapter.submitList(history)

        if (history.isNotEmpty()) {
            binding.recyclerViewHistory.visibility = View.VISIBLE
            binding.textViewSeeMore.visibility = View.VISIBLE
            binding.layoutHistoryHeader.visibility = View.VISIBLE
        } else {
            binding.recyclerViewHistory.visibility = View.GONE
            binding.textViewSeeMore.visibility = View.GONE
            binding.layoutHistoryHeader.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showBottomNavigation()
        searchJob?.cancel()
        _binding = null
    }
}
