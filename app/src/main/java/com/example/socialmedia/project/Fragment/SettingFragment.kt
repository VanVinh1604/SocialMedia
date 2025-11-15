package com.example.socialmedia.Fragment.Fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels // <-- THÊM IMPORT NÀY
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentSettingBinding
import com.example.socialmedia.project.ViewModel.SettingMenuViewModel // <-- SỬA IMPORT NÀY
import com.google.firebase.auth.FirebaseAuth

class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    // === SỬA LỖI: Khởi tạo SettingMenuViewModel (tên mới) ===
    private val viewModel: SettingMenuViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        setupObservers() // <-- GỌI HÀM OBSERVER MỚI

        // Yêu cầu ViewModel tải số lượng
        viewModel.loadBlockCount()
    }

    // === MỚI: Hàm lắng nghe LiveData từ ViewModel ===
    private fun setupObservers() {
        viewModel.blockCount.observe(viewLifecycleOwner) { count ->
            if (count > 0) {
                // Đảm bảo bạn đã thêm tv_block_count vào XML
                binding.tvBlockCount.text = count.toString()
                binding.tvBlockCount.visibility = View.VISIBLE
            } else {
                binding.tvBlockCount.visibility = View.GONE
            }
        }
    }

    private fun setupListeners() {
        // Nút Back trên toolbar
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Nút Account Center
        binding.cvAccountCenter.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_settingFragment_to_editProfileFragment)
            } catch (e: Exception) {
                showToast("Lỗi NavGraph: " + e.message)
            }
        }

        // Nút Log Out
        binding.cvLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            try {
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build()
                findNavController().navigate(R.id.action_settingFragment_to_loginFragment, null, navOptions)

            } catch (e: Exception) {
                showToast("Lỗi NavGraph: " + e.message)
                Log.e("SettingFragment", "Lỗi điều hướng logout: ", e)
            }
        }

        // Nút Block
        binding.llBlock.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_settingFragment_to_blockListFragment)
            } catch (e: Exception) {
                showToast("Lỗi NavGraph: " + e.message)
            }
        }

        // Các nút khác
        binding.llSaved.setOnClickListener {
            showToast("Chức năng 'Saved' đang phát triển")
        }
        binding.llHistory.setOnClickListener {
            showToast("Chức năng 'History' đang phát triển")
        }
        binding.llStoriesStorage.setOnClickListener {
            showToast("Chức năng 'Stories storage' đang phát triển")
        }
        binding.llPrivacy.setOnClickListener {
            try {
                // Điều hướng đến màn hình PrivacySettingsFragment bạn vừa tạo
                findNavController().navigate(R.id.action_settingFragment_to_privacySettingsFragment)
            } catch (e: Exception) {
                showToast("Lỗi NavGraph: " + e.message)
            }
        }
        binding.cvAddAccount.setOnClickListener {
            showToast("Chức năng 'Add account' đang phát triển")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Tránh memory leak
    }
}