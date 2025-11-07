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
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.socialmedia.MainActivity
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentSettingBinding // <-- Import ViewBinding
import com.example.socialmedia.project.UserManagementActivity
import com.google.firebase.auth.FirebaseAuth // <-- THÊM IMPORT FIREBASE
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import im.zego.zim.ZIM

class SettingFragment : Fragment() {

    // Sử dụng ViewBinding
    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout bằng ViewBinding
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Gọi hàm thiết lập các nút bấm
        setupListeners()
    }

    private fun setupListeners() {
        // Nút Back trên toolbar
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Nút Account Center (như bạn yêu cầu)
        binding.cvAccountCenter.setOnClickListener {
            try {
                // Điều hướng đến EditProfileFragment
                findNavController().navigate(R.id.action_settingFragment_to_editProfileFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show()
            }
        }

        // SettingFragment.kt - Phần xử lý logout
        binding.cvLogout.setOnClickListener {
            try {
                // 1️⃣ Logout ZIM (nếu đang đăng nhập)
                try {
                    val zimInstance = ZIM.getInstance()
                    if (MainActivity.isZIMLoggedIn && zimInstance != null) {
                        zimInstance.logout()
                        MainActivity.isZIMLoggedIn = false
                        Log.d("SettingFragment", "✅ ZIM logout thành công")
                    }
                } catch (e: Exception) {
                    Log.e("SettingFragment", "⚠️ Lỗi khi logout ZIM: ${e.message}")
                }

                // 2️⃣ Dọn Zego Call Service
                try {
                    if (MainActivity.isZegoInitialized) {
                        ZegoUIKitPrebuiltCallService.unInit()
                        MainActivity.isZegoInitialized = false
                        Log.d("SettingFragment", "✅ Zego Call Service unInit thành công")
                    }
                } catch (e: Exception) {
                    Log.e("SettingFragment", "⚠️ Lỗi khi unInit Zego: ${e.message}")
                }

                // 3️⃣ Đăng xuất Firebase
                FirebaseAuth.getInstance().signOut()
                Log.d("SettingFragment", "✅ Firebase signOut thành công")

                // 4️⃣ Xóa SharedPreferences (xoá session cũ)
                val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    clear()
                    apply()
                }
                Log.d("SettingFragment", "✅ SharedPreferences cleared")

                // 5️⃣ Chuyển về màn hình UserManagementActivity (Login)
                val intent = Intent(requireContext(), UserManagementActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()

                Toast.makeText(requireContext(), "Đã đăng xuất hoàn toàn", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Log.e("SettingFragment", "❌ Lỗi khi đăng xuất: ${e.message}", e)
                Toast.makeText(requireContext(), "Lỗi đăng xuất: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // === KẾT THÚC SỬA LỖI ===

        // Các nút khác (hiển thị Toast tạm thời)
        binding.llSaved.setOnClickListener {
            showToast("Chức năng 'Saved' đang phát triển")
        }
        binding.llHistory.setOnClickListener {
            showToast("ChứcACY 'History' đang phát triển")
        }
        binding.llStoriesStorage.setOnClickListener {
            showToast("Chức năng 'Stories storage' đang phát triển")
        }
        binding.llPrivacy.setOnClickListener {
            showToast("Chức năng 'Privacy' đang phát triển")
        }
        binding.llBlock.setOnClickListener {
            showToast("Chức năng 'Block' đang phát triển")
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


