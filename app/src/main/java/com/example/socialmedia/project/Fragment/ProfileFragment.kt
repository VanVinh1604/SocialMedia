package com.example.socialmedia.project.Fragment

import android.content.Intent // <-- THÊM IMPORT MỚI
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentProfileBinding // Sử dụng ViewBinding
import com.example.socialmedia.project.Adapter.ProfilePostAdapter
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.ViewModel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

/**
 * Fragment này hiển thị trang cá nhân của NGƯỜI KHÁC
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Dùng ViewModel CHIA SẺ (shared)
    private val viewModel: ProfileViewModel by navGraphViewModels(R.id.nav_graph)

    private lateinit var postAdapter: ProfilePostAdapter
    private var targetUserId: String? = null
    private var isCurrentlyFollowing: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Dùng ViewBinding để liên kết với fragment_profile.xml
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Lấy userId (Sửa lỗi: Lấy từ arguments Bundle)
        targetUserId = arguments?.getString("userId")
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        // 2. Kiểm tra
        if (targetUserId == null || targetUserId == currentUid) {
            // Nếu đây là trang CỦA TÔI, điều hướng về PersonalProfileFragment
            Toast.makeText(context, "Đang mở trang cá nhân của bạn...", Toast.LENGTH_SHORT).show()
            try {
                findNavController().navigate(R.id.personalProfileFragment)
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Lỗi điều hướng về PersonalProfileFragment: ${e.message}")
                findNavController().popBackStack()
            }
            return
        }

        // 3. Nếu là của người khác, tải profile của họ
        viewModel.loadProfile(targetUserId)

        setupUI()
        setupListeners()
        setupObservers()
    }

    private fun setupUI() {
        postAdapter = ProfilePostAdapter { post ->
            Toast.makeText(context, "Clicked post ${post.postId}", Toast.LENGTH_SHORT).show()
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

        // Nút Follow/Following chính
        binding.btnFollow.setOnClickListener {
            targetUserId?.let { id ->
                if (isCurrentlyFollowing) {
                    viewModel.unfollowUser(id)
                } else {
                    viewModel.followUser(id)
                }
            }
        }

        // === THÊM MỚI NÚT SHARE ===
        binding.btnShareProfile.setOnClickListener {
            // Lấy user (của người khác) từ ViewModel
            val user = viewModel.userProfile.value
            if (user != null) {
                shareUserProfile(user)
            } else {
                Toast.makeText(context, "Đang tải dữ liệu...", Toast.LENGTH_SHORT).show()
            }
        }
        // === KẾT THÚC THÊM MỚI ===

        // Click vào "Followers" (của người khác)
        binding.llFollowersWrapper.setOnClickListener {
            targetUserId?.let { id ->
                val bundle = Bundle().apply {
                    putString("userId", id) // Gửi ID của người này
                    putString("listType", "followers")
                }
                try {
                    // (Bạn cần tạo action này trong nav_graph)
                    findNavController().navigate(R.id.action_profileFragment_to_followListFragment, bundle)
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Click vào "Following" (của người khác)
        binding.llFollowingWrapper.setOnClickListener {
            targetUserId?.let { id ->
                val bundle = Bundle().apply {
                    putString("userId", id) // Gửi ID của người này
                    putString("listType", "following")
                }
                try {
                    // (Bạn cần tạo action này trong nav_graph)
                    findNavController().navigate(R.id.action_profileFragment_to_followListFragment, bundle)
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // binding.ivMenuButton.setOnClickListener { ... }
    }

    // === THÊM HÀM MỚI ĐỂ SHARE ===
    /**
     * Hàm này tạo ra một Intent chia sẻ "phổ thông"
     */
    private fun shareUserProfile(user: UserModel) {
        val profileLink = "https://my-social-app.com/profile/${user.userId}"
        // (Layout này không có bio, nên chúng ta chỉ share tên)
        val shareText = """
            Check out ${user.fullName} on SocialApp!
            
            See their profile: $profileLink
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out ${user.fullName}'s Profile")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Profile via"))
    }
    // === KẾT THÚC HÀM MỚI ===

    private fun setupObservers() {
        // 1. Quan sát Profile
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            // Chỉ cập nhật nếu user này là user chúng ta đang xem
            if (user != null && user.userId == targetUserId) {
                updateProfileUI(user)
            }
        }

        // 2. Quan sát Bài đăng
        viewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            postAdapter.submitList(posts)
        }

        // 3. Quan sát Trạng thái Follow
        viewModel.isFollowing.observe(viewLifecycleOwner) { isFollowing ->
            isCurrentlyFollowing = isFollowing
            updateFollowButtonUI(isFollowing)
        }

        // 4. Quan sát Lỗi
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(context, "Lỗi: $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateProfileUI(user: UserModel) {
        binding.tvUsername.text = user.fullName
        // (Layout của bạn không có tvBio)
        binding.tvPostCount.text = user.postCount.toString()
        binding.tvFollowingCount.text = user.followingCount.toString()

        // Sửa lỗi ID: Dùng tvFollowersCount (từ layout trên Canvas)
        binding.tvFollowersCount.text = user.followerCount.toString()

        Glide.with(this).load(user.headerPictureUrl).placeholder(R.drawable.image_backgroud).into(binding.ivHeaderImage)
        Glide.with(this).load(user.profilePictureUrl).placeholder(R.drawable.image_avata_user).circleCrop().into(binding.ivProfileImage)
    }

    private fun updateFollowButtonUI(isFollowing: Boolean) {
        val context = context ?: return
        if (isFollowing) {
            binding.btnFollow.text = "Following"
            binding.btnFollow.setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        } else {
            binding.btnFollow.text = "Follow"
            binding.btnFollow.setBackgroundColor(ContextCompat.getColor(context, R.color.purple_main))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Gỡ listener của ViewModel khi thoát
        viewModel.userProfile.removeObservers(viewLifecycleOwner)
        viewModel.userPosts.removeObservers(viewLifecycleOwner)
        viewModel.isFollowing.removeObservers(viewLifecycleOwner)

        binding.rvPhotos.adapter = null
        _binding = null
    }
}

