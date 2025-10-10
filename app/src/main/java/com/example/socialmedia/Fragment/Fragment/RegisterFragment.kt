package com.example.socialmedia.Fragment.Fragment


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.socialmedia.Fragment.Helper.TextGradientUtils
import com.example.socialmedia.Fragment.ViewModel.RegisterViewModel

import com.example.socialmedia.R

class RegisterFragment : Fragment() {

    private val registerViewModel: RegisterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvCreateTitle = view.findViewById<TextView>(R.id.tvCreateTitle)
        val btnBack = view.findViewById<View>(R.id.btnBack) // lấy LinearLayout
        val tvLoginNow = view.findViewById<TextView>(R.id.tvLoginNow)
        val txtSocialApp = view.findViewById<TextView>(R.id.txtSocialApp)

        val etEmail = view.findViewById<EditText>(R.id.etRegisterEmail)
        val etPassword = view.findViewById<EditText>(R.id.etRegisterPassword)
        val btnNext = view.findViewById<Button>(R.id.btnRegister)

        // 🌈 Áp dụng gradient cho chữ "Create"
        TextGradientUtils.applyGradient(
            tvCreateTitle,
            startColor = "#FF6FB1",
            endColor = "#9B59B6"
        )

        TextGradientUtils.applyGradient(
            txtSocialApp, startColor = "#FF6FB1", endColor = "#9B59B6"
        )

        // ⬅️ Khi bấm nút Back, quay lại LoginFragment
        btnBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, LoginFragment())
                .addToBackStack(null) // để có thể quay lại bằng nút back system
                .commit()
        }

        // 📝 Khi bấm "Login" cũng quay lại LoginFragment
        tvLoginNow.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, LoginFragment())
                .addToBackStack(null)
                .commit()
        }

        btnNext.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error = "Email cannot be empty"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "Password cannot be empty"
                return@setOnClickListener
            }

            // Lưu vào ViewModel tạm
            registerViewModel.setEmailAndPassword(email, password)

            // Chuyển sang fragment nhập Personal Info
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, UserInforFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
