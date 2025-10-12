package com.example.socialmedia

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.socialmedia.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var currentItemId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Save Firebase user info
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

        // Setup Navigation
        setupNavigation()

        // Mặc định hiển thị HomeFragment
        binding.bottomNavigation.selectedItemId = R.id.nav_home

        // Hiệu ứng phóng to icon Home ban đầu
        val homeView = binding.bottomNavigation.findViewById<View>(R.id.nav_home)
        val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
            homeView,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1.2f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.2f)
        )
        scaleUp.duration = 150
        scaleUp.start()
        currentItemId = R.id.nav_home

        // Upload Button
        binding.btnUpload.setOnClickListener {
            // Navigate to Upload Fragment
            navController.navigate(R.id.uploadFragment)

            // Thu nhỏ icon bottom nav hiện tại
            currentItemId?.let { prevId ->
                val prevView = binding.bottomNavigation.findViewById<View>(prevId)
                ObjectAnimator.ofPropertyValuesHolder(
                    prevView,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f)
                ).apply { duration = 150 }.start()

                prevView.isSelected = false
                prevView.invalidate()
            }

            // Bỏ chọn tất cả items trong bottom nav
            binding.bottomNavigation.menu.setGroupCheckable(0, false, false)
            for (i in 0 until binding.bottomNavigation.menu.size()) {
                binding.bottomNavigation.menu.getItem(i).isChecked = false
            }
            binding.bottomNavigation.menu.setGroupCheckable(0, true, false)

            currentItemId = null
        }

        // Bottom Navigation Selection
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val view = binding.bottomNavigation.findViewById<View>(item.itemId)

            // Thu nhỏ icon trước đó
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

            // Phóng to icon mới
            val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
                view,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1.2f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.2f)
            )
            scaleUp.duration = 150
            scaleUp.start()

            currentItemId = item.itemId

            // Navigate với Navigation Component
            val fragmentId = when (item.itemId) {
                R.id.nav_home -> R.id.homeFragment
                R.id.nav_search -> R.id.searchFragment
                R.id.nav_reels -> R.id.reelsFragment
                R.id.nav_profile -> R.id.profileFragment
                else -> null
            }

            fragmentId?.let {
                // Chỉ navigate nếu không phải fragment hiện tại
                if (navController.currentDestination?.id != it) {
                    navController.navigate(it)
                }
            }

            true
        }
    }


    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Setup OnBackPressedDispatcher (thay thế onBackPressed)
        onBackPressedDispatcher.addCallback(this) {
            // Nếu đang ở fragment khác, back về Home
            if (navController.currentDestination?.id != R.id.homeFragment) {
                navController.navigate(R.id.homeFragment)
                binding.bottomNavigation.selectedItemId = R.id.nav_home
            } else {
                // Nếu đang ở Home, thoát app
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}