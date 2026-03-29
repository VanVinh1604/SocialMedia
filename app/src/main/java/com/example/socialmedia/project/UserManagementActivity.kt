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


        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // ✅ Đọc SharedPreferences để kiểm tra user đã login hay chưa
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)
        val userId = sharedPref.getString("user_id", null)
        val fullName = sharedPref.getString("full_name", null)

        if (isLoggedIn && userId != null) {
            // 🔹 Đã có session → mở MainActivity, init Zego/ZIM trong đó
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("payload_user_id", userId)
            intent.putExtra("payload_full_name", fullName)
            startActivity(intent)
            finish()
        } else {
            // 🔹 Chưa đăng nhập → hiển thị LoginFragment
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, LoginFragment())
                    .commit()
            }
        }

        // Chưa đăng nhập -> hiển thị màn hình login
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}
