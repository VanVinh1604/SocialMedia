package com.example.socialmedia

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.socialmedia.project.Fragment.HomeFragment
import com.example.socialmedia.project.Fragment.ProfileFragment
import com.example.socialmedia.project.Fragment.ReelsFragment
import com.example.socialmedia.project.Fragment.SearchFragment
import com.example.socialmedia.project.Fragment.UploadFragment
import com.example.socialmedia.databinding.ActivityMainBinding
import com.example.socialmedia.project.Domain.UserModel
import com.example.socialmedia.project.Fragment.LoginFragment
import com.example.socialmedia.project.UserManagementActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentItemId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val sharedPref = getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean("is_logged_in", true)
                putString("user_id", currentUser.uid)
                putString("email", currentUser.email)
                putString("full_name", currentUser.displayName ?: "User")
                apply()
            }
        }
        // ✅ Mặc định hiển thị HomeFragment
        replaceFragment(HomeFragment())
        binding.bottomNavigation.selectedItemId = R.id.nav_home

        // ✅ Thêm hiệu ứng phóng to mặc định cho icon Home
        val homeView = binding.bottomNavigation.findViewById<View>(R.id.nav_home)
        val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
            homeView,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1.2f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.2f)
        )
        scaleUp.duration = 150
        scaleUp.start()

        currentItemId = R.id.nav_home

        // ✅ Xử lý chọn các item khác
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val view = binding.bottomNavigation.findViewById<View>(item.itemId)

            val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
                view,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1.2f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.2f)
            )
            val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                view,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f)
            )

            scaleUp.duration = 150
            scaleDown.duration = 150

            // ✅ Thu nhỏ icon trước đó
            currentItemId?.let { prevId ->
                if (prevId != item.itemId) {
                    val prevView = binding.bottomNavigation.findViewById<View>(prevId)
                    ObjectAnimator.ofPropertyValuesHolder(
                        prevView,
                        PropertyValuesHolder.ofFloat(View.SCALE_X, 1f),
                        PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f)
                    ).apply { duration = 150 }.start()
                }
            }

            // ✅ Phóng to icon mới
            scaleUp.start()
            currentItemId = item.itemId

            when (item.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_search -> replaceFragment(SearchFragment())
//                R.id.nav_upload -> replaceFragment(UploadFragment())
                R.id.nav_reels -> replaceFragment(ReelsFragment())
                R.id.nav_profile -> replaceFragment(ProfileFragment())
            }
            true
        }
    }


    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, fragment)
            .commit()
    }
}
