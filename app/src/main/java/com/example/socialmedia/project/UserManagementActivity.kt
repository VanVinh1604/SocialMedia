package com.example.socialmedia.project

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.socialmedia.MainActivity
import com.example.socialmedia.project.Fragment.LoginFragment
import com.example.socialmedia.R
import com.example.socialmedia.databinding.ActivityUserManagementBinding
import com.google.firebase.auth.FirebaseAuth

class UserManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserManagementBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_user_management)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Đã đăng nhập -> chuyển thẳng vào MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return // ✅ Dừng không chạy code phía dưới
        }

        // Chưa đăng nhập -> hiển thị màn hình login
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hiển thị LoginFragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, LoginFragment())
                .commit()
        }

        // Load LoginFragment đầu tiên
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, LoginFragment())
            .commit()
    }
}
