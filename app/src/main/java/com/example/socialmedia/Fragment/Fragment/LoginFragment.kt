package com.example.socialmedia.Fragment.Fragment

import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.example.socialmedia.Fragment.Fragment.State.Resource
import com.example.socialmedia.Fragment.Repository.LoginRepository
import com.example.socialmedia.Fragment.ViewModel.LoginViewModel
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

            loginViewModel.login(email, password).observe(viewLifecycleOwner) { resource ->
                when(resource) {
                    is Resource.Loading -> { /* show loading */ }
                    is Resource.Success -> { Toast.makeText(requireContext(),"Thanh Cong" , Toast.LENGTH_SHORT).show() }
                    is Resource.Error -> {
                        Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}