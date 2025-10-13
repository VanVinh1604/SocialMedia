package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.socialmedia.project.Helper.TextGradientUtils
import com.example.socialmedia.project.ViewModel.RegisterViewModel
import com.example.socialmedia.R
import com.example.socialmedia.project.Fragment.State.Resource

class RegisterFragment : Fragment() {

    private val registerViewModel: RegisterViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvCreateTitle = view.findViewById<TextView>(R.id.tvCreateTitle)
        val btnBack = view.findViewById<View>(R.id.btnBack)
        val tvLoginNow = view.findViewById<TextView>(R.id.tvLoginNow)
        val txtSocialApp = view.findViewById<TextView>(R.id.txtSocialApp)

        val etEmail = view.findViewById<EditText>(R.id.etRegisterEmail)
        val etPassword = view.findViewById<EditText>(R.id.txtRegisterPassword)
        val etConfirmPassword = view.findViewById<EditText>(R.id.txtConfirmPassword)
        val ivTogglePassword = view.findViewById<ImageView>(R.id.ivToggleRegisterPassword)
        val ivToggleConfirmPassword =
            view.findViewById<ImageView>(R.id.ivToggleRegisterPasswordConfirm)
        val btnNext = view.findViewById<Button>(R.id.btnRegister)

        // 🌈 Gradient chữ "Create" và "Social App"
        TextGradientUtils.applyGradient(tvCreateTitle, "#FF6FB1", "#9B59B6")
        TextGradientUtils.applyGradient(txtSocialApp, "#FF6FB1", "#9B59B6")

        // ⬅️ Back button
        btnBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, LoginFragment())
                .addToBackStack(null)
                .commit()
        }

        // 📝 Login now
        tvLoginNow.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, LoginFragment())
                .addToBackStack(null)
                .commit()
        }

        // Toggle hiển thị password
        var isPasswordVisible = false
        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            } else {
                etPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.ic_visibility)
            }
            etPassword.setSelection(etPassword.text?.length ?: 0)
        }

        var isConfirmPasswordVisible = false
        ivToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            if (isConfirmPasswordVisible) {
                etConfirmPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_visibility_off)
            } else {
                etConfirmPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_visibility)
            }
            etConfirmPassword.setSelection(etConfirmPassword.text?.length ?: 0)
        }

        btnNext.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            Log.d("RegisterCheck", "Email: $email, Password: $password, Confirm: $confirmPassword")

            // 🔍 Kiểm tra dữ liệu nhập
            if (email.isEmpty()) {
                etEmail.error = "Email cannot be empty"
                Toast.makeText(requireContext(), "Please enter your email", Toast.LENGTH_SHORT)
                    .show()
                Log.e("RegisterCheck", "❌ Email is empty")
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Invalid email format"
                Toast.makeText(
                    requireContext(),
                    "Email format is invalid (must include @)",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("RegisterCheck", "❌ Email format invalid")
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Password cannot be empty"
                Toast.makeText(requireContext(), "Please enter password", Toast.LENGTH_SHORT).show()
                Log.e("RegisterCheck", "❌ Password is empty")
                return@setOnClickListener
            }

            if (password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                Toast.makeText(
                    requireContext(),
                    "Password must be at least 6 characters",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("RegisterCheck", "❌ Password too short")
                return@setOnClickListener
            }

            if (confirmPassword.isEmpty()) {
                etConfirmPassword.error = "Please confirm password"
                Toast.makeText(requireContext(), "Please confirm password", Toast.LENGTH_SHORT)
                    .show()
                Log.e("RegisterCheck", "❌ Confirm password is empty")
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT)
                    .show()
                Log.e("RegisterCheck", "❌ Passwords not matching")
                return@setOnClickListener
            }

            // ✅ Nếu hợp lệ: lưu vào ViewModel và chuyển sang UserInforFragment
            registerViewModel.setEmailAndPassword(email, password)
            Log.i("RegisterCheck", "✅ All inputs valid, moving to UserInforFragment")

            Toast.makeText(requireContext(), "Email & password are valid", Toast.LENGTH_SHORT)
                .show()

            // Không gọi registerUser ở đây
// Chỉ lưu email và password vào ViewModel và chuyển sang UserInforFragment
            registerViewModel.setEmailAndPassword(email, password)

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, UserInforFragment())
                .addToBackStack(null)
                .commit()


            registerViewModel.registerResult.observe(viewLifecycleOwner) { result ->
                when (result) {
                    is Resource.Success -> {
                        Log.i("RegisterStatus", "✅ Đăng ký thành công với UID: ${result.data}")
                        Toast.makeText(requireContext(), "Đăng ký thành công!", Toast.LENGTH_SHORT)
                            .show()

                        // 👉 Chỉ chuyển sang UserInforFragment nếu đăng ký thành công
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, UserInforFragment())
                            .addToBackStack(null)
                            .commit()
                    }

                    is Resource.Error -> {
                        Log.e("RegisterStatus", "❌ Đăng ký thất bại: ${result.message}")
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()

                        // ❌ Không chuyển fragment, dừng lại ở RegisterFragment
                    }

                    is Resource.Loading -> {
                        Log.d("RegisterStatus", "⏳ Đang xử lý đăng ký...")
                    }
                }
            }

        }
    }
}

