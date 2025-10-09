package com.example.socialmedia.Fragment

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.socialmedia.Fragment.Fragment.LoginFragment
import com.example.socialmedia.R

class UserManagementActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_user_management)

        // Load LoginFragment đầu tiên
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, LoginFragment())
            .commit()
    }
}
