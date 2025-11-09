package com.example.socialmedia.project.Fragment

import LikedUsersBottomSheetFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentHomeBinding
import com.example.socialmedia.project.Adapter.PostAdapter
import com.example.socialmedia.project.Adapter.StoryAdapter
import com.example.socialmedia.project.Domain.Model.StoryModel
import com.example.socialmedia.project.ViewModel.PostViewModel
import com.example.socialmedia.project.ViewModel.StoryViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!


    private val postViewModel: PostViewModel by viewModels()

    private val storyViewModel: StoryViewModel by viewModels()
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        setupObservers()
        setupClicks()
        observeNotificationBadge()

        setupPostRecycler()
        observePosts()

        postViewModel.loadPosts()



        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        storyViewModel.loadStories(currentUserId)
    }

    private fun setupRecycler() {
        binding.recyclerStory.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    private fun setupObservers() {
        storyViewModel.stories.observe(viewLifecycleOwner) { storyList ->
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@observe

            // Tính số story của user
            val userStoriesCount = storyList.count { it.userId == currentUserId }

            binding.recyclerStory.adapter = StoryAdapter(
                stories = storyList,
                currentUserId = currentUserId,
                onAddStoryClick = {
                    findNavController().navigate(R.id.action_homeFragment_to_addStoryFragment)
                },
                onStoryClick = { story ->
                    // Truyền số lượng story vào bundle
                    val action = HomeFragmentDirections.actionHomeFragmentToStoryViewerFragment(
                        story.userId
                    )
                    findNavController().navigate(action)
                }
            )
        }
    }



    private fun setupClicks() {
        binding.ivMessage.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_messageFragment)
        }

        binding.ivNotification.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_notificationFragment)
        }
    }

    private fun setupPostRecycler() {
        binding.recyclerPost.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPost.isNestedScrollingEnabled = false

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        postAdapter = PostAdapter(emptyList(), currentUserId,
            onLikesClickListener = { postId ->
                LikedUsersBottomSheetFragment(postId)
                    .show(parentFragmentManager, "likedUsers")
            },
            onCommentClickListener = { postId, postAuthorId ->
                CommentBottomSheetFragment(postId, currentUserId, postAuthorId)
                    .show(parentFragmentManager, "comments")
            }
        )
        binding.recyclerPost.adapter = postAdapter
    }

    private fun observePosts() {
        postViewModel.posts.observe(viewLifecycleOwner) { posts ->
            postAdapter.updatePosts(posts)
        }

        postViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { println("⚠️ Post Firebase error: $it") }
        }
    }


    private fun observeNotificationBadge() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().getReference("notifications")

        // Lắng nghe realtime thông báo mới của user
        database.orderByChild("userId").equalTo(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var hasNew = false
                    for (child in snapshot.children) {
                        val isRead = child.child("isRead").getValue(Boolean::class.java) ?: false
                        if (!isRead) {
                            hasNew = true
                            break
                        }
                    }
                    binding.badgeNotification.visibility = if (hasNew) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    // Không cần xử lý đặc biệt
                }
            })
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
