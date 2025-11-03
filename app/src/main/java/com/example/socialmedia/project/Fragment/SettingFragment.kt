package com.example.socialmedia.Fragment.Fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentSettingBinding // <-- Import ViewBinding
import com.google.firebase.auth.FirebaseAuth // <-- THÊM IMPORT FIREBASE

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

        // === SỬA LỖI NÚT LOG OUT ===
        binding.cvLogout.setOnClickListener {
            // 1. Đăng xuất khỏi Firebase
            FirebaseAuth.getInstance().signOut()

            // 2. Điều hướng về LoginFragment và XÓA SẠCH back stack
            try {
                // Cấu hình NavOptions để xóa tất cả các fragment trước đó
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true) // Xóa back stack đến tận gốc của nav_graph
                    .build()

                // (Bạn cần đảm bảo đã tạo action này trong nav_graph)
                findNavController().navigate(R.id.action_settingFragment_to_loginFragment, null, navOptions)

            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show()

                // === SỬA LỖI Ở ĐÂY ===
                // Truyền 'e' (Throwable) chứ không phải 'e.message' (String)
                Log.e("SettingFragment", "Lỗi điều hướng logout: ", e)
                // === KẾT THÚC SỬA LỖI ===
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


