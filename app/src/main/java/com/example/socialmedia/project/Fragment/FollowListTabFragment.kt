package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentFollowListTabBinding // Dùng layout tab
import com.example.socialmedia.project.Adapter.FollowUserAdapter
import com.example.socialmedia.project.ViewModel.FollowListViewModel
import com.google.firebase.auth.FirebaseAuth // <-- THÊM IMPORT NÀY

/**
 * Fragment này hiển thị MỘT danh sách (Followers HOẶC Following)
 * Nó sẽ được đặt bên trong ViewPager của FollowListFragment
 */
class FollowListTabFragment : Fragment() {

    private var _binding: FragmentFollowListTabBinding? = null
    private val binding get() = _binding!!

    // Mỗi tab sẽ có ViewModel riêng
    private val viewModel: FollowListViewModel by viewModels()
    private lateinit var adapter: FollowUserAdapter

    private var userId: String = ""
    private var listType: String = ""
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid // Lấy ID của user hiện tại

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Nhận dữ liệu từ ViewPagerAdapter
        arguments?.let {
            userId = it.getString("userId") ?: ""
            listType = it.getString("listType") ?: "followers"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFollowListTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        // Tải (hoặc tải lại) danh sách MỖI KHI tab này được hiển thị
        if (userId.isNotEmpty()) {
            Log.d("FollowListTabFragment", "Đang tải lại danh sách cho: $listType")
            viewModel.loadList(userId, listType)
        } else if (::adapter.isInitialized) {
            adapter.submitList(emptyList())
        }
    }

    private fun setupUI() {
        // Cài đặt Adapter
        adapter = FollowUserAdapter(
            currentUserId = currentUid ?: "", // Truyền ID của user hiện tại vào Adapter
            onUserClick = { user ->
                // Điều hướng đến trang cá nhân của người dùng này

                // 1. Tạo Bundle để gửi userId của người ĐƯỢC CLICK
                val bundle = bundleOf("userId" to user.userId)

                try {
                    // 2. Gọi action (từ FollowListFragment -> ProfileFragment)
                    parentFragment?.findNavController()?.navigate(
                        R.id.action_followListFragment_to_profileFragment,
                        bundle
                    )
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show()
                }
            },
            onFollowClick = { user, isFollowing ->
                if (isFollowing) {
                    viewModel.unfollowUser(user.userId)
                } else {
                    viewModel.followUser(user.userId)
                }
            }
        )
        binding.rvFollowList.adapter = adapter
    }

    private fun setupObservers() {
        // Quan sát trạng thái loading
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBar.isVisible = isLoading && adapter.currentList.isEmpty()
        })

        // === [BƯỚC QUAN TRỌNG] ===
        // Quan sát danh sách ID BẠN đang follow (để đổi nút)
        viewModel.myFollowingIds.observe(viewLifecycleOwner, Observer { ids ->
            // Truyền danh sách ID này vào Adapter
            Log.d("FollowListTabFragment", "Adapter đã nhận được ${ids.size} following IDs")
            adapter.updateMyFollowingIds(ids)
        })
        // === KẾT THÚC BƯỚC QUAN TRỌNG ===

        // Quan sát danh sách NGƯỜI (để hiển thị)
        viewModel.userList.observe(viewLifecycleOwner, Observer { userList ->
            adapter.submitList(userList)
        })

        // Quan sát lỗi
        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            if (error != null) {
                Toast.makeText(context, "Lỗi: $error", Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvFollowList.adapter = null
        _binding = null
    }
}

