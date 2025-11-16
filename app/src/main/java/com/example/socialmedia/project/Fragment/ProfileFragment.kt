package com.example.socialmedia.project.Fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentProfileBinding
import com.example.socialmedia.project.Adapter.ProfilePostAdapter
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

        setupUI() // <-- SỬA LỖI TRONG HÀM NÀY
        setupListeners()
        setupObservers()
        setupTabs()
    }

    // === HÀM ĐÃ SỬA LỖI ===
    private fun setupUI() {
        postAdapter = ProfilePostAdapter { post ->
            // --- SỬA LỖI TẠI ĐÂY ---
            // Dòng code cũ:
            // Toast.makeText(context, "Clicked post ${post.postId}", Toast.LENGTH_SHORT).show()

            // Dòng code mới:
            Log.d("ProfileFragment", "Đã nhấp vào postId: ${post.postId}")
            try {
                // 1. Tạo bundle
                val bundle = Bundle().apply {
                    putString("postId", post.postId) // Key "postId" phải khớp với nav_graph
                }

                // 2. Điều hướng (Sử dụng action bạn đã tạo trong nav_graph)
                findNavController().navigate(R.id.action_profileFragment_to_postDetailFragment, bundle)

            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi điều hướng: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            // --- KẾT THÚC SỬA LỖI ---
        }

        binding.rvPhotos.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = postAdapter
            isNestedScrollingEnabled = false
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
                // (Xử lý bị chặn / không tìm thấy)
                Toast.makeText(context, "Không tìm thấy người dùng.", Toast.LENGTH_LONG).show()
                binding.llStats.visibility = View.GONE
                binding.llUserInfo.visibility = View.GONE
                binding.llActionButtons.visibility = View.GONE
                binding.tabLayout.visibility = View.GONE
                binding.rvPhotos.visibility = View.GONE
                binding.llEmptyPhotos.visibility = View.VISIBLE
                binding.tvEmptyMessage.text = "Không tìm thấy"
                binding.tvEmptySubMessage.text = "Người dùng này không tồn tại hoặc đã chặn bạn."

            } else if (user.userId == targetUserId) {
                // (Hiển thị UI)
                binding.llStats.visibility = View.VISIBLE
                binding.llUserInfo.visibility = View.VISIBLE
                binding.llActionButtons.visibility = View.VISIBLE
                binding.tabLayout.visibility = View.VISIBLE

                updateProfileUI(user)
                filterAndDisplayPosts() // GỌI LỌC KHI USER THAY ĐỔI
            }
        }

        viewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            if (view == null) return@observe
            binding.tvPostCount.text = posts.size.toString()
            filterAndDisplayPosts() // GỌI LỌC KHI BÀI ĐĂNG THAY ĐỔI
        }

        viewModel.isFollowing.observe(viewLifecycleOwner) { isFollowing ->
            updateFollowButtonUI() // Chỉ cập nhật nút
        }

        viewModel.theyAreFollowingMe.observe(viewLifecycleOwner) { theyFollowMe ->
            updateFollowButtonUI()
        }

        // Lắng nghe trạng thái bạn bè 2 chiều
        viewModel.isMutualFriend.observe(viewLifecycleOwner) { isFriend ->
            Log.d("ProfileFragment", "Trạng thái bạn bè 2 chiều: $isFriend")
            filterAndDisplayPosts() // <<-- ĐÂY LÀ CHÌA KHÓA: Gọi lại hàm lọc
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
                targetUserId?.let {
                    viewModel.loadProfile(it)
                }
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

    // === HÀM QUAN TRỌNG NHẤT VỚI LOGIC ĐÚNG ===
    private fun filterAndDisplayPosts() {
        val user = viewModel.userProfile.value
        val allPosts = viewModel.userPosts.value ?: emptyList()
        val isMutualFriend = viewModel.isMutualFriend.value ?: false

        if (user == null) {
            // Trường hợp này xử lý user bị chặn, hoặc chưa kịp tải
            postAdapter.submitList(emptyList())
            binding.rvPhotos.visibility = View.GONE

            if (binding.llStats.visibility == View.GONE) {
                binding.llEmptyPhotos.visibility = View.VISIBLE
            } else {
                binding.llEmptyPhotos.visibility = View.GONE
            }
            return
        }

        // === LOGIC MÀ BẠN YÊU CẦU NẰM Ở ĐÂY ===
        if (user.Private && !isMutualFriend) {
            // NẾU: Tài khoản là RIÊNG TƯ (Private = true)
            // VÀ:   Không phải là bạn 2 chiều (!isMutualFriend = true)

            postAdapter.submitList(emptyList())
            binding.rvPhotos.visibility = View.GONE

            // THÌ: Ẩn bài đăng và hiển thị thông báo
            binding.llEmptyPhotos.visibility = View.VISIBLE
            binding.tvEmptyMessage.text = "Tài khoản này là riêng tư"
            binding.tvEmptySubMessage.text = "Hãy theo dõi và được ${user.fullName} chấp nhận (follow lại) để xem bài đăng."
            return // Dừng hàm tại đây
        }
        // ===================================

        // NGƯỢC LẠI: (Nếu là tài khoản Public HOẶC là bạn bè 2 chiều)
        // Hiển thị bài đăng bình thường
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

    // === HÀM HIỂN THỊ "Follow Back" ===
    private fun updateFollowButtonUI() {
        val context = context ?: return

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

        viewModel.userProfile.removeObservers(viewLifecycleOwner)
        viewModel.userPosts.removeObservers(viewLifecycleOwner)
        viewModel.isFollowing.removeObservers(viewLifecycleOwner)
        viewModel.blockStatus.removeObservers(viewLifecycleOwner)
        viewModel.unblockSuccess.removeObservers(viewLifecycleOwner)
        viewModel.isTargetUserBlocked.removeObservers(viewLifecycleOwner)
        viewModel.isMutualFriend.removeObservers(viewLifecycleOwner)
        viewModel.theyAreFollowingMe.removeObservers(viewLifecycleOwner)

        binding.rvPhotos.adapter = null
        _binding = null
    }
}