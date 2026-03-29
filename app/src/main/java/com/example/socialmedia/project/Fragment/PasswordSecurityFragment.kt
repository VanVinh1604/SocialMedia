package com.example.socialmedia.project.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentPasswordSecurityBinding

class PasswordSecurityFragment : Fragment() {

    private var _binding: FragmentPasswordSecurityBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPasswordSecurityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }

    private fun setupListeners() {
        // Nút Back (ID: btnBack)
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Nút "Change password" (ID: layoutChangePassword)
        binding.layoutChangePassword.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_passwordSecurityFragment_to_changePasswordFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show()
            }
        }

        // === CẬP NHẬT: Thêm 3 nút mới ===

        // Nút "Two-factor authentication" (ID: layoutTwoFactor)
        binding.layoutTwoFactor.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_passwordSecurityFragment_to_twoFactorAuthFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show()
            }
        }

        // Nút "Where to log in" (ID: layoutWhereToLogin)
        binding.layoutWhereToLogin.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_passwordSecurityFragment_to_whereToLogInFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show()
            }
        }

        // Nút "Login warning" (ID: layoutLoginWarning)
        binding.layoutLoginWarning.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_passwordSecurityFragment_to_loginWarningFragment)
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