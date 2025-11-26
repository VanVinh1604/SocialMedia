package com.example.socialmedia.Fragment.Fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.socialmedia.MainActivity
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentSettingBinding
import com.example.socialmedia.project.UserManagementActivity
import com.google.firebase.auth.FirebaseAuth
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import im.zego.zim.ZIM
import com.example.socialmedia.project.ViewModel.SettingMenuViewModel

class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

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
        setupObservers()
        viewModel.loadBlockCount()
    }

    private fun setupObservers() {
        viewModel.blockCount.observe(viewLifecycleOwner) { count ->
            if (count > 0) {
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

        // === [CẬP NHẬT MỚI] SỰ KIỆN CLICK CHO 3 MỤC ACTIVITY ===

        // 1. Saved (Đã lưu)
        binding.llSaved.setOnClickListener {
            try {
                // Đảm bảo ID này khớp với id action trong nav_graph.xml
                findNavController().navigate(R.id.action_settingFragment_to_savedFragment)
            } catch (e: Exception) {
                // Nếu lỗi, thử dùng ID đích trực tiếp (nếu action chưa đặt tên đúng)
                try {
                    findNavController().navigate(R.id.savedFragment)
                } catch (e2: Exception) {
                    showToast("Lỗi NavGraph: Chưa tạo action tới SavedFragment")
                }
            }
        }

        // 2. History (Lịch sử)
        binding.llHistory.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_settingFragment_to_historyFragment)
            } catch (e: Exception) {
                try {
                    findNavController().navigate(R.id.historyFragment)
                } catch (e2: Exception) {
                    showToast("Lỗi NavGraph: Chưa tạo action tới HistoryFragment")
                }
            }
        }

        // 3. Stories storage (Kho lưu trữ tin)
        binding.llStoriesStorage.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_settingFragment_to_storiesStorageFragment)
            } catch (e: Exception) {
                try {
                    findNavController().navigate(R.id.storiesStorageFragment)
                } catch (e2: Exception) {
                    showToast("Lỗi NavGraph: Chưa tạo action tới StoriesStorageFragment")
                }
            }
        }
        // ========================================================

        // Nút Block
        binding.llBlock.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_settingFragment_to_blockListFragment)
            } catch (e: Exception) {
                showToast("Lỗi NavGraph: " + e.message)
            }
        }

        // Nút Privacy
        binding.llPrivacy.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_settingFragment_to_privacySettingsFragment)
            } catch (e: Exception) {
                showToast("Lỗi NavGraph: " + e.message)
            }
        }

        binding.cvAddAccount.setOnClickListener {
            showToast("Chức năng 'Add account' đang phát triển")
        }

        // Xử lý Logout
        binding.cvLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun performLogout() {
        try {
            // 1. Logout ZIM
            try {
                val zimInstance = ZIM.getInstance()
                if (MainActivity.isZIMLoggedIn && zimInstance != null) {
                    zimInstance.logout()
                    MainActivity.isZIMLoggedIn = false
                }
            } catch (e: Exception) { Log.e("SettingFragment", "Lỗi ZIM: ${e.message}") }

            // 2. UnInit Zego
            try {
                if (MainActivity.isZegoInitialized) {
                    ZegoUIKitPrebuiltCallService.unInit()
                    MainActivity.isZegoInitialized = false
                }
            } catch (e: Exception) { Log.e("SettingFragment", "Lỗi Zego: ${e.message}") }

            // 3. Firebase SignOut
            FirebaseAuth.getInstance().signOut()

            // 4. Clear Prefs
            val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                clear()
                apply()
            }

            // 5. Navigate to Login
            val intent = Intent(requireContext(), UserManagementActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()

            Toast.makeText(requireContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            showToast("Lỗi đăng xuất: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}