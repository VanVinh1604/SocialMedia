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
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentProfileBinding
import com.example.socialmedia.project.Adapter.ProfilePostAdapter
import com.example.socialmedia.project.Adapter.StoryHighlightAdapter
import com.example.socialmedia.project.Domain.Model.PostModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.ViewModel.ProfileViewModel
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by navGraphViewModels(R.id.nav_graph)

    private lateinit var postAdapter: ProfilePostAdapter
    private lateinit var highlightAdapter: StoryHighlightAdapter

    // === [CẬP NHẬT] Tách riêng 2 danh sách để quản lý tab dễ hơn ===
    private var allPosts: List<PostModel> = emptyList() // Chứa bài viết thường (Posts)
    private var allReels: List<PostModel> = emptyList() // Chứa Reels (đã convert)
    // ==============================================================

    private var targetUserId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        targetUserId = arguments?.getString("userId")
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        if (targetUserId == null || targetUserId == currentUid) {
            Toast.makeText(context, "Đang mở trang cá nhân của bạn...", Toast.LENGTH_SHORT).show()
            try { findNavController().navigate(R.id.personalProfileFragment) }
            catch (e: Exception) { findNavController().popBackStack() }
            return
        }

        // Load toàn bộ thông tin (Profile, Posts, Reels, Highlights)
        viewModel.loadProfile(targetUserId)
        if (targetUserId != null) {
            viewModel.loadTargetUserStories(targetUserId!!)
        }

        setupUI()
        setupListeners()
        setupObservers()
        setupTabs()
    }

    private fun setupUI() {
        // === [CẬP NHẬT] Logic click: Phân loại Reel và Post thường ===
        postAdapter = ProfilePostAdapter { post ->
            if (post.isReel) {
                // MỞ REELS
                val bundle = Bundle().apply {
                    putString("userId", post.userId)       // ID người đang xem
                    putString("startReelId", post.postId)  // ID video để scroll tới
                }
                try {
                    // Thử tìm action hoặc ID fragment
                    try {
                        findNavController().navigate(R.id.action_profileFragment_to_reelsFragment, bundle)
                    } catch (e: Exception) {
                        findNavController().navigate(R.id.reelsFragment, bundle)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Không thể mở Reels: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                // MỞ POST CHI TIẾT
                try {
                    val bundle = Bundle().apply {
                        putString("postId", post.postId)
                        putString("userId", post.userId ?: targetUserId ?: "")
                    }
                    findNavController().navigate(R.id.action_profileFragment_to_postDetailFragment, bundle)
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi điều hướng: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.rvPhotos.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = postAdapter
            isNestedScrollingEnabled = false
        }

        highlightAdapter = StoryHighlightAdapter(emptyList()) { highlight ->
            val bundle = Bundle().apply {
                putString("highlightId", highlight.id)
                putString("userId", targetUserId)
            }
            try {
                findNavController().navigate(R.id.action_profileFragment_to_highlightViewerFragment, bundle)
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Lỗi mở highlight: ${e.message}")
                Toast.makeText(context, "Không thể mở highlight này", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvStoryHighlights.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = highlightAdapter
        }
    }

    private fun setupListeners() {
        binding.ivBackButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnFollow.setOnClickListener {
            val isFollowing = viewModel.isFollowing.value ?: false
            targetUserId?.let { id ->
                if (isFollowing) {
                    viewModel.unfollowUser(id)
                } else {
                    viewModel.followUser(id)
                }
            }
        }

        binding.btnShareProfile.setOnClickListener {
            val user = viewModel.userProfile.value
            if (user != null) {
                shareUserProfile(user)
            } else {
                Toast.makeText(context, "Đang tải dữ liệu...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.llFollowersWrapper.setOnClickListener {
            targetUserId?.let { id ->
                val bundle = Bundle().apply { putString("userId", id); putString("listType", "followers") }
                try { findNavController().navigate(R.id.action_profileFragment_to_followListFragment, bundle) }
                catch (e: Exception) { Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show() }
            }
        }

        binding.llFollowingWrapper.setOnClickListener {
            targetUserId?.let { id ->
                val bundle = Bundle().apply { putString("userId", id); putString("listType", "following") }
                try { findNavController().navigate(R.id.action_profileFragment_to_followListFragment, bundle) }
                catch (e: Exception) { Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show() }
            }
        }

        binding.ivMenuButton.setOnClickListener {
            targetUserId?.let { id ->
                val bottomSheet = ProfileOptionsBottomSheet.newInstance(id)
                bottomSheet.show(parentFragmentManager, "ProfileOptionsBottomSheet")
            } ?: run {
                Toast.makeText(context, "Không thể tải tùy chọn...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareUserProfile(user: UserModel) {
        val profileLink = "https://my-social-app.com/profile/${user.userId}"
        val bioLine = if (user.bio.isNullOrEmpty()) "" else "\"${user.bio}\"\n\n"
        val shareText = "Check out ${user.fullName} on SocialApp!\n\n$bioLine\nSee their profile: $profileLink".trimIndent()
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out ${user.fullName}'s Profile")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Profile via"))
    }

    private fun setupObservers() {

        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            if (user == null) {
                // Xử lý khi không tìm thấy user hoặc bị chặn
                Toast.makeText(context, "Không tìm thấy người dùng.", Toast.LENGTH_LONG).show()
                binding.llStats.visibility = View.GONE
                binding.llUserInfo.visibility = View.GONE
                binding.llActionButtons.visibility = View.GONE
                binding.tabLayout.visibility = View.GONE
                binding.rvPhotos.visibility = View.GONE
                binding.rvStoryHighlights.visibility = View.GONE
                binding.llEmptyPhotos.visibility = View.VISIBLE
                binding.tvEmptyMessage.text = "Không tìm thấy"
                binding.tvEmptySubMessage.text = "Người dùng này không tồn tại hoặc đã chặn bạn."

            } else if (user.userId == targetUserId) {
                // Hiển thị profile
                binding.llStats.visibility = View.VISIBLE
                binding.llUserInfo.visibility = View.VISIBLE
                binding.llActionButtons.visibility = View.VISIBLE
                binding.tabLayout.visibility = View.VISIBLE

                updateProfileUI(user)
                filterAndDisplayPosts()
            }

            viewModel.targetUserStories.observe(viewLifecycleOwner) { stories ->
                val currentTime = System.currentTimeMillis()
                val twentyFourHours = 24 * 60 * 60 * 1000L

                // Lọc story còn hạn (chưa quá 24h)
                val activeStories = stories.filter { story ->
                    (currentTime - story.createdAt) < twentyFourHours
                }

                if (activeStories.isNotEmpty()) {
                    updateStoryRing(true)
                } else {
                    updateStoryRing(false)
                }
            }

        }




        // 1. Lắng nghe Posts thường
        viewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            if (view == null) return@observe
            allPosts = posts
            // Số lượng bài viết chỉ đếm post thường
            binding.tvPostCount.text = posts.size.toString()
            filterAndDisplayPosts()
        }

        // 2. === [MỚI] Lắng nghe Reels và Convert sang PostModel ===
        viewModel.userReels.observe(viewLifecycleOwner) { reels ->
            if (view == null) return@observe

            // Mapping Reels -> PostModel để hiển thị chung adapter
            allReels = reels.map { reel ->
                PostModel(
                    postId = reel.reelId,
                    userId = reel.userId,

                    // Đánh dấu là Reel
                    isReel = true,
                    thumbnail = reel.thumbnailUrl,
                    videoUrl = reel.videoUrl,
                    caption = reel.caption,

                    // Mapping số liệu
                    likeCount = reel.likeCount,
                    commentCount = reel.commentCount,
                    shareCount = reel.shareCount,
                    viewCount = reel.viewCount,

                    createdAt = reel.createdAt
                )
            }
            filterAndDisplayPosts()
        }

        viewModel.userHighlights.observe(viewLifecycleOwner) { highlights ->
            highlightAdapter.submitList(highlights)
        }

        viewModel.isFollowing.observe(viewLifecycleOwner) { isFollowing ->
            updateFollowButtonUI()
        }

        viewModel.theyAreFollowingMe.observe(viewLifecycleOwner) { theyFollowMe ->
            updateFollowButtonUI()
        }

        viewModel.isMutualFriend.observe(viewLifecycleOwner) { isFriend ->
            Log.d("ProfileFragment", "Trạng thái bạn bè 2 chiều: $isFriend")
            filterAndDisplayPosts()
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(context, "Lỗi: $error", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.blockStatus.observe(viewLifecycleOwner) { hasBlocked ->
            if (hasBlocked) {
                Toast.makeText(context, "Đã chặn người dùng này", Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
                viewModel.resetBlockStatus()
            }
        }

        viewModel.unblockSuccess.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess == true) {
                Toast.makeText(context, "Đã bỏ chặn", Toast.LENGTH_SHORT).show()
                targetUserId?.let { viewModel.loadProfile(it) }
                viewModel.resetUnblockSuccessStatus()
            } else if (isSuccess == false) {
                Toast.makeText(context, "Bỏ chặn thất bại", Toast.LENGTH_SHORT).show()
                viewModel.resetUnblockSuccessStatus()
            }
        }
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


    // --- [HÀM ĐIỀU KHIỂN VÒNG SÁNG] ---
    private fun updateStoryRing(hasStory: Boolean) {
        val density = resources.displayMetrics.density
        val padding3dp = (3 * density).toInt()

        if (hasStory) {
            // Có Story: Hiện vòng Gradient
            binding.flProfileImageContainer.setBackgroundResource(R.drawable.bg_story_ring)
            binding.flProfileImageContainer.setPadding(padding3dp, padding3dp, padding3dp, padding3dp)

            // (Tùy chọn) Sự kiện khi click vào avatar có story -> Xem story
            binding.ivProfileImage.setOnClickListener {
                // Code mở StoryViewerFragment (bạn có thể thêm sau)
                Toast.makeText(context, "Xem Story của người này", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Không có Story: Tắt vòng
            binding.flProfileImageContainer.background = null
            binding.flProfileImageContainer.setPadding(0, 0, 0, 0)

            // Click vào avatar -> Xem ảnh to (mặc định)
            binding.ivProfileImage.setOnClickListener {
                // Code xem ảnh avatar to
            }
        }
    }

    private fun filterAndDisplayPosts() {
        val user = viewModel.userProfile.value
        val isMutualFriend = viewModel.isMutualFriend.value ?: false
        val allHighlights = viewModel.userHighlights.value ?: emptyList()

        if (user == null) {
            // ... (Code xử lý null giữ nguyên) ...
            return
        }

        // Kiểm tra quyền riêng tư (Private & Chưa follow)
        if (user.Private && !isMutualFriend) {
            postAdapter.submitList(emptyList())
            binding.rvPhotos.visibility = View.GONE
            binding.rvStoryHighlights.visibility = View.GONE

            binding.llEmptyPhotos.visibility = View.VISIBLE
            binding.tvEmptyMessage.text = "Tài khoản này là riêng tư"
            binding.tvEmptySubMessage.text = "Hãy theo dõi và được ${user.fullName} chấp nhận để xem bài đăng."
            return
        }

        // Hiển thị Highlight
        if (allHighlights.isNotEmpty()) {
            binding.rvStoryHighlights.visibility = View.VISIBLE
            highlightAdapter.submitList(allHighlights)
        } else {
            binding.rvStoryHighlights.visibility = View.GONE
        }

        // === [CẬP NHẬT] Logic hiển thị theo Tab ===
        val selectedTabPosition = binding.tabLayout.selectedTabPosition
        val (postsToShow, emptyMessageTitle, emptyMessageSub) = if (selectedTabPosition == 0) {
            // TAB 0: Bài đăng thường (Lấy từ allPosts)
            Triple(allPosts, "Chưa có bài đăng", "Người dùng này chưa đăng bài.")
        } else {
            // TAB 1: Reels (Lấy từ allReels)
            Triple(allReels, "Chưa có Reels", "Người dùng này chưa đăng Reels.")
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

    private fun updateProfileUI(user: UserModel) {
        binding.tvUsername.text = user.fullName
        if (user.bio.isNullOrEmpty()) {
            binding.tvBio.visibility = View.GONE
        } else {
            binding.tvBio.visibility = View.VISIBLE
            binding.tvBio.text = user.bio
        }
        binding.tvFollowingCount.text = user.followingCount.toString()
        binding.tvFollowersCount.text = user.followerCount.toString()
        Glide.with(this).load(user.headerPictureUrl).placeholder(R.drawable.image_backgroud).into(binding.ivHeaderImage)
        Glide.with(this).load(user.profilePictureUrl).placeholder(R.drawable.image_avata_user).circleCrop().into(binding.ivProfileImage)
    }

    private fun updateFollowButtonUI() {
        val isFollowing = viewModel.isFollowing.value ?: false
        val theyAreFollowingMe = viewModel.theyAreFollowingMe.value ?: false

        if (isFollowing) {
            binding.btnFollow.text = "Unfollow"
        } else {
            if (theyAreFollowingMe) {
                binding.btnFollow.text = "Follow Back"
            } else {
                binding.btnFollow.text = "Follow"
            }
        }
    }





    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvPhotos.adapter = null
        binding.rvStoryHighlights.adapter = null
        _binding = null
    }
}