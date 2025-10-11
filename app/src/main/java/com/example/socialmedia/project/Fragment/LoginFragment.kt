package com.example.socialmedia.project.Fragment

import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.example.socialmedia.project.Fragment.State.Resource
import com.example.socialmedia.project.ViewModel.LoginViewModel
import com.example.socialmedia.MainActivity
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentLoginBinding


class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val loginViewModel : LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Gradient chữ Social App
        val width = binding.tvAppName.paint.measureText(binding.tvAppName.text.toString())
        val textShader = LinearGradient(
            0f, 0f, width, binding.tvAppName.textSize,
            intArrayOf(0xFFFF6FB1.toInt(), 0xFF9B59B6.toInt()),
            null, Shader.TileMode.CLAMP
        )
        binding.tvAppName.paint.shader = textShader


        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginViewModel.login(email, password).observe(viewLifecycleOwner) { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.btnLogin.isEnabled = false
                        binding.btnLogin.text = "Đang đăng nhập..."
                    }

                    is Resource.Success -> {
                        binding.btnLogin.isEnabled = true
                        binding.btnLogin.text = "Đăng nhập"
                        Toast.makeText(requireContext(), "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(requireContext(), MainActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    }

                    is Resource.Error -> {
                        binding.btnLogin.isEnabled = true
                        binding.btnLogin.text = "Đăng nhập"
                        Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        var isPasswordVisible = false

        binding.ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                // Hiển thị mật khẩu
                binding.etPassword.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            } else {
                // Ẩn mật khẩu
                binding.etPassword.inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility)
            }

            // Giữ nguyên vị trí con trỏ
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }

        binding.btnRegister.setOnClickListener {
            val registerFragment = RegisterFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, registerFragment)
                .addToBackStack(null)
                .commit()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}