package com.example.socialmedia.project.Fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager // [MỚI]
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentProfileBinding
import com.example.socialmedia.project.Adapter.ProfilePostAdapter
import com.example.socialmedia.project.Adapter.StoryHighlightAdapter // [MỚI] Import
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.ViewModel.ProfileViewModel
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by navGraphViewModels(R.id.nav_graph)

    private lateinit var postAdapter: ProfilePostAdapter
    private lateinit var highlightAdapter: StoryHighlightAdapter // [MỚI] Khai báo Adapter

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

        viewModel.loadProfile(targetUserId)

        setupUI()
        setupListeners()
        setupObservers()
        setupTabs()
    }

    private fun setupUI() {
        // Setup Grid Ảnh
        postAdapter = ProfilePostAdapter { post ->
            Log.d("ProfileFragment", "Đã nhấp vào postId: ${post.postId}")
            try {
                val bundle = Bundle().apply {
                    putString("postId", post.postId)
                }
                findNavController().navigate(R.id.action_profileFragment_to_postDetailFragment, bundle)
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi điều hướng: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvPhotos.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = postAdapter
            isNestedScrollingEnabled = false
        }

        // [CẬP NHẬT] Setup Highlights với sự kiện Click
        highlightAdapter = StoryHighlightAdapter(emptyList()) { highlight ->
            val bundle = Bundle().apply {
                putString("highlightId", highlight.id)
                putString("userId", targetUserId)
            }
            try {
                // Điều hướng sang màn hình xem highlight
                findNavController().navigate(R.id.action_profileFragment_to_highlightViewerFragment, bundle)
            } catch (e: Exception) {
                // Log lỗi nếu action chưa được khai báo trong nav_graph
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
                Toast.makeText(context, "Không tìm thấy người dùng.", Toast.LENGTH_LONG).show()
                binding.llStats.visibility = View.GONE
                binding.llUserInfo.visibility = View.GONE
                binding.llActionButtons.visibility = View.GONE
                binding.tabLayout.visibility = View.GONE
                binding.rvPhotos.visibility = View.GONE
                binding.rvStoryHighlights.visibility = View.GONE // [MỚI] Ẩn highlights khi lỗi
                binding.llEmptyPhotos.visibility = View.VISIBLE
                binding.tvEmptyMessage.text = "Không tìm thấy"
                binding.tvEmptySubMessage.text = "Người dùng này không tồn tại hoặc đã chặn bạn."

            } else if (user.userId == targetUserId) {
                binding.llStats.visibility = View.VISIBLE
                binding.llUserInfo.visibility = View.VISIBLE
                binding.llActionButtons.visibility = View.VISIBLE
                binding.tabLayout.visibility = View.VISIBLE

                updateProfileUI(user)
                filterAndDisplayPosts()
            }
        }

        viewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            if (view == null) return@observe
            binding.tvPostCount.text = posts.size.toString()
            filterAndDisplayPosts()
        }

        // [MỚI] Lắng nghe Highlights
        viewModel.userHighlights.observe(viewLifecycleOwner) { highlights ->
            highlightAdapter.submitList(highlights)
            // Logic ẩn/hiện tùy thuộc vào quyền riêng tư (xử lý dưới filterAndDisplayPosts)
            // Tạm thời nếu có data thì hiện:
            if (highlights.isNotEmpty()) {
                // Kiểm tra xem có đang bị Private không? (Đã xử lý ở filterAndDisplayPosts nhưng đây là Observer riêng)
                // Tốt nhất để filterAndDisplayPosts xử lý visibility của toàn bộ phần Content
                // Tuy nhiên, ở mức cơ bản, ta cứ set adapter
            }
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

    private fun filterAndDisplayPosts() {
        val user = viewModel.userProfile.value
        val allPosts = viewModel.userPosts.value ?: emptyList()
        val isMutualFriend = viewModel.isMutualFriend.value ?: false
        // [MỚI] Lấy highlights
        val allHighlights = viewModel.userHighlights.value ?: emptyList()

        if (user == null) {
            postAdapter.submitList(emptyList())
            binding.rvPhotos.visibility = View.GONE
            binding.rvStoryHighlights.visibility = View.GONE // [MỚI]
            if (binding.llStats.visibility == View.GONE) binding.llEmptyPhotos.visibility = View.VISIBLE
            else binding.llEmptyPhotos.visibility = View.GONE
            return
        }

        if (user.Private && !isMutualFriend) {
            // RIÊNG TƯ -> Ẩn hết
            postAdapter.submitList(emptyList())
            binding.rvPhotos.visibility = View.GONE
            binding.rvStoryHighlights.visibility = View.GONE // [MỚI]

            binding.llEmptyPhotos.visibility = View.VISIBLE
            binding.tvEmptyMessage.text = "Tài khoản này là riêng tư"
            binding.tvEmptySubMessage.text = "Hãy theo dõi và được ${user.fullName} chấp nhận để xem bài đăng."
            return
        }

        // CÔNG KHAI HOẶC BẠN BÈ -> Hiển thị

        // 1. Xử lý Highlights
        if (allHighlights.isNotEmpty()) {
            binding.rvStoryHighlights.visibility = View.VISIBLE
            highlightAdapter.submitList(allHighlights)
        } else {
            binding.rvStoryHighlights.visibility = View.GONE
        }

        // 2. Xử lý Posts
        val selectedTabPosition = binding.tabLayout.selectedTabPosition
        val (postsToShow, emptyMessageTitle, emptyMessageSub) = if (selectedTabPosition == 0) {
            val gridPosts = allPosts.filter { !it.isReel }
            Triple(gridPosts, "Chưa có bài đăng", "Người dùng này chưa đăng bài.")
        } else {
            val reelPosts = allPosts.filter { it.isReel }
            Triple(reelPosts, "Chưa có Reels", "Người dùng này chưa đăng Reels.")
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
        binding.rvStoryHighlights.adapter = null // [MỚI]
        _binding = null
    }
}