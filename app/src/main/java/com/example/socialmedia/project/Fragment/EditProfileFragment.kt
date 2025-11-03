package com.example.socialmedia.project.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentEditProfileBinding // <-- Import ViewBinding

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

        // TODO: Tải tên user hiện tại và hiển thị
        // binding.tvCurrentUsername.text = "Huy Neeewwd"
    }

    private fun setupListeners() {
        // Nút Back trên toolbar
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // Nút "Password and security"
        binding.llPassword.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_editProfileFragment_to_changePasswordFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show()
            }
        }

        // Nút "Profile" (đi đến trang sửa tên)
//        binding.llProfile.setOnClickListener {
//            try {
//                // (Bạn cần tạo action này trong nav_graph)
//                findNavController().navigate(R.id.action_editProfileFragment_to_editProfileNameFragment)
//            } catch (e: Exception) {
//                Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show()
//            }
//        }

        // Nút "Personal Information"
        binding.llPersonalInfo.setOnClickListener {
            Toast.makeText(context, "Mở trang Personal Information", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Tránh memory leak
    }
}

