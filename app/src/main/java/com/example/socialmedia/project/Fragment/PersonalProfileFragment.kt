package com.example.socialmedia.project.Fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentPersonalProfileBinding
import com.example.socialmedia.project.Adapter.ProfilePostAdapter
import com.example.socialmedia.project.Adapter.StoryHighlightAdapter
import com.example.socialmedia.project.Domain.Model.PostModel
import com.example.socialmedia.project.Domain.Model.StoryHighlightModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.ViewModel.ProfileViewModel
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth

class PersonalProfileFragment : Fragment() {

    private var _binding: FragmentPersonalProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by navGraphViewModels(R.id.nav_graph)

    private lateinit var postAdapter: ProfilePostAdapter
    private lateinit var highlightAdapter: StoryHighlightAdapter // Adapter cho Story Highlight

    private var allPosts: List<PostModel> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPersonalProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPostGrid()
        setupHighlights() // Cài đặt adapter cho Highlight
        setupObservers()
        setupClickListeners()
        setupTabs()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProfile(null) // Tải profile của chính mình
    }

    // === [QUAN TRỌNG] CÀI ĐẶT HIGHLIGHT ===
    private fun setupHighlights() {
        highlightAdapter = StoryHighlightAdapter(emptyList()) { highlight ->
            // Kiểm tra xem user bấm vào nút "Mới" hay bấm vào tin đã có
            if (highlight.id == "ADD_NEW") {
                // 1. Tạo mới Highlight
                try {
                    findNavController().navigate(R.id.action_personalProfileFragment_to_createHighlightFragment)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                // 2. [MỚI] Xem Highlight đã có
                val bundle = Bundle().apply {
                    putString("highlightId", highlight.id) // Truyền ID để ViewModel tải dữ liệu
                }
                try {
                    findNavController().navigate(R.id.action_personalProfileFragment_to_highlightViewerFragment, bundle)
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi điều hướng: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.rvStoryHighlights.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = highlightAdapter
        }
    }
    private fun setupPostGrid() {
        postAdapter = ProfilePostAdapter { post ->
            val postId = post.postId

            // === SỬA Ở ĐÂY ===
            // Thêm "userId" to post.userId vào trong bundleOf
            val bundle = bundleOf(
                "postId" to postId,
                "userId" to post.userId  // <--- Dòng bạn cần thêm nằm ở đây
            )
            // =================

            try {
                findNavController().navigate(R.id.action_personalProfileFragment_to_postDetailFragment, bundle)
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvPhotos.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = postAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupObservers() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        viewModel.userProfile.observe(viewLifecycleOwner, Observer { user ->
            if (user != null && user.userId == currentUid) {
                updateUi(user)
            }
        })

        viewModel.userPosts.observe(viewLifecycleOwner, Observer { posts ->
            if (view == null) return@Observer
            allPosts = posts
            binding.tvPostCount.text = posts.size.toString()
            filterAndDisplayPosts()
        })

        // === [QUAN TRỌNG] OBSERVER HIGHLIGHTS VỚI NÚT ADD ===
        viewModel.userHighlights.observe(viewLifecycleOwner) { firebaseHighlights ->
            // 1. Tạo danh sách hiển thị
            val displayList = ArrayList<StoryHighlightModel>()

            // 2. Luôn chèn nút "Mới" vào vị trí đầu tiên
            displayList.add(
                StoryHighlightModel(
                    id = "ADD_NEW",
                    name = "Mới",
                    coverUrl = "" // Adapter sẽ tự xử lý để hiện icon dấu cộng
                )
            )

            // 3. Chèn tiếp dữ liệu thật từ Firebase (nếu có)
            displayList.addAll(firebaseHighlights)

            // 4. Cập nhật Adapter
            highlightAdapter.submitList(displayList)

            // 5. Luôn hiện RecyclerView vì luôn có ít nhất nút "Mới"
            binding.rvStoryHighlights.visibility = View.VISIBLE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner, Observer { errorMsg ->
            if (!errorMsg.isNullOrEmpty()) {
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filterAndDisplayPosts()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun filterAndDisplayPosts() {
        val selectedTabPosition = binding.tabLayout.selectedTabPosition
        val (postsToShow, emptyMessageTitle, emptyMessageSub) = if (selectedTabPosition == 0) {
            val gridPosts = allPosts.filter { !it.isReel }
            Triple(gridPosts, "Chưa có bài đăng", "Ảnh và video của bạn sẽ ở đây.")
        } else {
            val reelPosts = allPosts.filter { it.isReel }
            Triple(reelPosts, "Chưa có Reels", "Reels của bạn sẽ xuất hiện ở đây.")
        }

        postAdapter.submitList(postsToShow)

        if (postsToShow.isNotEmpty()) {
            binding.rvPhotos.visibility = View.VISIBLE
            binding.llEmptyPhotos.visibility = View.GONE
        } else {
            binding.rvPhotos.visibility = View.GONE
            binding.llEmptyPhotos.visibility = View.VISIBLE
            binding.tvEmptyMessage.text = emptyMessageTitle
            binding.tvEmptySubMessage.text = emptyMessageSub
        }
    }

    private fun setupClickListeners() {
        binding.ivBackButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.ivMenuButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_personalProfileFragment_to_settingFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi NavGraph: ...to_settingFragment", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnEditProfile.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_personalProfileFragment_to_editProfileNameFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi NavGraph: ...to_editProfileNameFragment", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnShareProfile.setOnClickListener {
            val currentUser = viewModel.userProfile.value
            if (currentUser != null) {
                shareUserProfile(currentUser)
            } else {
                Toast.makeText(context, "Đang tải dữ liệu...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.ivProfileImage.setOnClickListener {
            Toast.makeText(context, "Mở trình chọn ảnh", Toast.LENGTH_SHORT).show()
        }

        binding.llFollowersWrapper.setOnClickListener {
            val userId = viewModel.userProfile.value?.userId
            if (userId != null) {
                val bundle = Bundle().apply {
                    putString("userId", userId)
                    putString("listType", "followers")
                }
                try {
                    findNavController().navigate(R.id.action_personalProfileFragment_to_followListFragment, bundle)
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.llFollowingWrapper.setOnClickListener {
            val userId = viewModel.userProfile.value?.userId
            if (userId != null) {
                val bundle = Bundle().apply {
                    putString("userId", userId)

                    putString("listType", "following")
                }
                try {
                    findNavController().navigate(R.id.action_personalProfileFragment_to_followListFragment, bundle)
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun shareUserProfile(user: UserModel) {
        val profileLink = "https://my-social-app.com/profile/${user.userId}"
        val bioLine = if (user.bio.isNullOrEmpty()) "" else "\"${user.bio}\"\n\n"
        val shareText = """
            Check out ${user.fullName} on SocialApp!
            
            $bioLine
            See their profile: $profileLink
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out ${user.fullName}'s Profile")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Profile via"))
    }

    private fun updateUi(user: UserModel) {
        binding.tvUsername.text = user.fullName

        if (user.bio.isNullOrEmpty()) {
            binding.tvBio.visibility = View.GONE
        } else {
            binding.tvBio.visibility = View.VISIBLE
            binding.tvBio.text = user.bio
        }

        binding.tvFollowersCount.text = formatCount(user.followerCount)
        binding.tvFollowingCount.text = formatCount(user.followingCount)

        Glide.with(this)
            .load(user.headerPictureUrl)
            .centerCrop()
            .placeholder(R.drawable.image_backgroud)
            .error(R.drawable.image_backgroud)
            .into(binding.ivHeaderImage)

        Glide.with(this)
            .load(user.profilePictureUrl)
            .placeholder(R.drawable.image_avata_user)
            .error(R.drawable.image_avata_user)
            .circleCrop()
            .into(binding.ivProfileImage)
    }

    private fun formatCount(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fm", count / 1_000_000.0)
            count >= 100_000 -> "${count / 1_000}k"
            count >= 10_000 -> String.format("%.1fk", count / 1_000.0)
            count >= 1_000 -> "${count / 1_000}k"
            else -> count.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvPhotos.adapter = null
        binding.rvStoryHighlights.adapter = null
        _binding = null
    }
}