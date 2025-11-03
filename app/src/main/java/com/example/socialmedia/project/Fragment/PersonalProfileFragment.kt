package com.example.socialmedia.project.Fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentPersonalProfileBinding
import com.example.socialmedia.project.Adapter.ProfilePostAdapter
import com.example.socialmedia.project.Domain.Model.PostModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.ViewModel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

class PersonalProfileFragment : Fragment() {

    private var _binding: FragmentPersonalProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by navGraphViewModels(R.id.nav_graph)

    private lateinit var postAdapter: ProfilePostAdapter

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
        setupObservers()
        setupClickListeners()
    }

    // Tải lại dữ liệu mỗi khi quay về Fragment này
    override fun onResume() {
        super.onResume()
        Log.d("PersonalProfile", "onResume: Đang tải lại hồ sơ cá nhân...")
        viewModel.loadProfile(null) // 'null' có nghĩa là "tải user hiện tại"
    }


    private fun setupPostGrid() {
        postAdapter = ProfilePostAdapter { post ->
            Toast.makeText(context, "Clicked post ${post.postId}", Toast.LENGTH_SHORT).show()
        }

        binding.rvPhotos.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = postAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupObservers() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        // 1. Quan sát dữ liệu người dùng
        viewModel.userProfile.observe(viewLifecycleOwner, Observer { user ->
            if (user != null) {
                // Chỉ cập nhật UI nếu user tải về LÀ TÔI
                if (user.userId == currentUid) {
                    updateUi(user)
                }
            }
        })

        // 2. Quan sát danh sách bài đăng (cho lưới Photos)
        viewModel.userPosts.observe(viewLifecycleOwner, Observer { posts ->
            if (view == null) return@Observer

            // Chỉ cập nhật POSTS nếu user hiện tại LÀ TÔI
            if (viewModel.userProfile.value?.userId == currentUid) {
                if (posts.isNotEmpty()) {
                    Log.d("ProfileFragment", "Nhận được ${posts.size} bài đăng")
                    postAdapter.submitList(posts)
                    binding.rvPhotos.visibility = View.VISIBLE
                } else {
                    Log.d("ProfileFragment", "Không có bài đăng nào")
                    postAdapter.submitList(emptyList())
                    binding.rvPhotos.visibility = View.GONE
                }
            }
        })

        // 3. Quan sát thông báo lỗi
        viewModel.errorMessage.observe(viewLifecycleOwner, Observer { errorMsg ->
            if (!errorMsg.isNullOrEmpty()) {
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
        })
    }


    private fun setupClickListeners() {

        binding.ivBackButton.setOnClickListener { findNavController().popBackStack() }

        binding.ivMenuButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_personalProfileFragment_to_settingFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi NavGraph: ...to_settingFragment", Toast.LENGTH_LONG).show()
            }
        }

        // === SỬA LỖI ĐIỀU HƯỚNG THEO YÊU CẦU CỦA BẠN ===
        binding.btnEditProfile.setOnClickListener {
            try {
                // Sửa: Điều hướng đến trang Sửa Tên
                findNavController().navigate(R.id.action_personalProfileFragment_to_editProfileNameFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi NavGraph: ...to_editProfileNameFragment", Toast.LENGTH_LONG).show()
            }
        }
        // === KẾT THÚC SỬA LỖI ===

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

        // Click "Followers"
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

        // Click "Following"
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
        val shareText = """
            Check out ${user.fullName} on SocialApp!
            
            "${user.bio}"
            
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
        binding.tvBio.text = user.bio
        binding.tvPostCount.text = user.postCount.toString()

        // (Đã sửa theo layout mới)
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
            count >= 1_000_000 -> String.format("%.1fm", count / 1_000_000.0) // 1.5m
            count >= 100_000 -> "${count / 1_000}k" // 150k
            count >= 10_000 -> String.format("%.1fk", count / 1_000.0) // 15.5k
            count >= 1_000 -> "${count / 1_000}k" // 1k
            else -> count.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvPhotos.adapter = null
        _binding = null
    }
}

