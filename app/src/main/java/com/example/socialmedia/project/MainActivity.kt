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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var currentItemId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUserPresence()
        // Save Firebase user info
        FirebaseAuth.getInstance().currentUser?.let { user ->
            val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean("is_logged_in", true)
                putString("user_id", user.uid)
                putString("email", user.email)
                putString("full_name", user.displayName ?: "User")
                apply()
            }
        }

        setupNavigation()

        // Mặc định hiển thị HomeFragment và phóng to icon
        binding.bottomNavigation.selectedItemId = R.id.nav_home
        animateIcon(R.id.nav_home)
        currentItemId = R.id.nav_home

        // Upload FAB
        binding.btnUpload.setOnClickListener {
            navController.navigate(R.id.uploadFragment)
            resetPreviousIcon()
            currentItemId = null
        }

        // BottomNavigation selection
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragmentId = when (item.itemId) {
                R.id.nav_home -> R.id.homeFragment
                R.id.nav_search -> R.id.searchFragment
                R.id.nav_reels -> R.id.reelsFragment
                R.id.nav_profile -> R.id.personalProfileFragment
                else -> null
            }

            fragmentId?.let {
                if (navController.currentDestination?.id != it) {
                    navController.navigate(it)
                }
            }

            // Animation icon
            animateIcon(item.itemId)
            currentItemId = item.itemId
            true
        }

        // Listener để ẩn BottomNavigation + FAB trên các fragment full screen
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.chatFragment,
                R.id.messageFragment,
                R.id.notificationFragment -> {
                    // Ẩn BottomNavigation + FAB
                    binding.container.visibility = View.GONE
                    binding.navHostFragment.setPadding(0, 0, 0, 0)
                }
                else -> {
                    // Hiện lại BottomNavigation + FAB
                    binding.container.visibility = View.VISIBLE
                    binding.navHostFragment.setPadding(0, 0, 0, dpToPx(80)) // padding cũ
                }
            }
        }

    }

    private fun setupUserPresence() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("InfoUser").child(user.uid)
        val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")

        connectedRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    // 🔹 Khi user kết nối vào Firebase
                    userRef.child("isOnline").setValue(true)

                    // 🔹 Khi mất kết nối hoặc đóng app
                    userRef.child("isOnline").onDisconnect().setValue(false)
                    userRef.child("lastLogin").onDisconnect().setValue(ServerValue.TIMESTAMP)
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Back press dispatcher
        onBackPressedDispatcher.addCallback(this) {
            if (navController.currentDestination?.id != R.id.homeFragment) {
                navController.navigate(R.id.homeFragment)
                binding.bottomNavigation.selectedItemId = R.id.nav_home
            } else {
                finish()
            }
        }
    }

    private fun animateIcon(itemId: Int) {
        currentItemId?.let { prevId ->
            if (prevId != itemId) {
                val prevView = binding.bottomNavigation.findViewById<View>(prevId)
                ObjectAnimator.ofPropertyValuesHolder(
                    prevView,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f)
                ).apply { duration = 150 }.start()
            }
        }

        val view = binding.bottomNavigation.findViewById<View>(itemId)
        ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1.2f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.2f)
        ).apply { duration = 150 }.start()
    }

    private fun resetPreviousIcon() {
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

        // Bỏ chọn tất cả
        binding.bottomNavigation.menu.setGroupCheckable(0, false, false)
        for (i in 0 until binding.bottomNavigation.menu.size()) {
            binding.bottomNavigation.menu.getItem(i).isChecked = false
        }
        binding.bottomNavigation.menu.setGroupCheckable(0, true, false)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
