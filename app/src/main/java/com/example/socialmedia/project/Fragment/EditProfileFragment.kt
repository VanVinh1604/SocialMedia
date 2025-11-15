package com.example.socialmedia.project.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentEditProfileBinding

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()    
    }

    private fun setupListeners() {
        // (Giả sử toolbar của bạn có ID là 'toolbar')
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // Nút "Password and security"
        // (Giả sử layout của bạn có ID là 'llPassword')
        binding.llPassword.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_editProfileFragment_to_passwordSecurityFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show()
            }
        }

        // Nút "Profile" (đi đến trang sửa tên/bio)
        // (Giả sử layout của bạn có ID là 'llProfile')
        binding.llProfile.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_editProfileFragment_to_editProfileNameFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show()
            }
        }

        // === CẬP NHẬT: Nút "Personal Information" ===
        // (Giả sử layout của bạn có ID là 'llPersonalInfo')
        binding.llPersonalInfo.setOnClickListener {
            try {
                // SỬA TỪ TOAST SANG ĐIỀU HƯỚNG
                findNavController().navigate(R.id.action_editProfileFragment_to_personalInformationFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}