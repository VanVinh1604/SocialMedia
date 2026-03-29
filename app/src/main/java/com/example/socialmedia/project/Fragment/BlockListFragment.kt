package com.example.socialmedia.project.Fragment // (Hoặc package Fragment của bạn)

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.socialmedia.databinding.FragmentBlockListBinding
import com.example.socialmedia.project.Adapter.BlockedUserAdapter
import com.example.socialmedia.project.ViewModel.BlockListViewModel

class BlockListFragment : Fragment() {

    private var _binding: FragmentBlockListBinding? = null
    private val binding get() = _binding!!

    // Khởi tạo ViewModel
    private val viewModel: BlockListViewModel by viewModels()
    private lateinit var adapter: BlockedUserAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBlockListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()

        // Tải danh sách khi fragment được tạo
        viewModel.loadBlockedUsers()
    }

    private fun setupUI() {
        // Nút Back
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Thiết lập Adapter
        adapter = BlockedUserAdapter { userId ->
            // Đây là sự kiện khi nhấn nút "Bỏ chặn"
            Toast.makeText(context, "Đang bỏ chặn...", Toast.LENGTH_SHORT).show()
            viewModel.unblockUser(userId)
        }
        binding.rvBlockedList.adapter = adapter
    }

    private fun setupObservers() {
        // Lắng nghe danh sách người dùng bị chặn
        viewModel.blockedUsersList.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)

            // Hiển thị/ẩn thông báo "danh sách rỗng"
            if (users.isEmpty()) {
                binding.tvEmptyList.visibility = View.VISIBLE
                binding.rvBlockedList.visibility = View.GONE
            } else {
                binding.tvEmptyList.visibility = View.GONE
                binding.rvBlockedList.visibility = View.VISIBLE
            }
        }

        // Lắng nghe trạng thái tải
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Lắng nghe lỗi
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(context, "Lỗi: $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}