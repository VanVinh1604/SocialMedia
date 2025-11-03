package com.example.socialmedia

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.socialmedia.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.example.socialmedia.project.Helper.Constants
import com.example.socialmedia.project.ViewModel.SharedUserViewModel
import im.zego.zim.ZIM
import im.zego.zim.callback.ZIMLoggedInCallback
import im.zego.zim.entity.ZIMError
import im.zego.zim.entity.ZIMUserInfo
import im.zego.zim.enums.ZIMErrorCode

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var currentItemId: Int? = null
    private lateinit var sharedUserViewModel: SharedUserViewModel
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "MainActivity"
        var isZegoInitialized = false
        var isZIMLoggedIn = false // ✅ Thêm flag cho ZIM
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedUserViewModel = ViewModelProvider(this)[SharedUserViewModel::class.java]

        setupUserPresence()
        saveUserSession()
        setupNavigation()
        setupBottomNav()

        // ✅ Khởi tạo Zego và ZIM
        initZegoCallServiceSync()
    }

    // -------------------- Zego Call Service + ZIM Login --------------------
    private fun initZegoCallServiceSync() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Log.e(TAG, "❌ User chưa login, không thể init Zego")
            finish()
            return
        }

        // ✅ Init Zego Call Service
        setupZegoForUser(currentUser.uid, currentUser.displayName ?: "User")

        // ✅ Login ZIM (quan trọng!)
        loginZIM(currentUser.uid, currentUser.displayName ?: "User")
    }

    private fun setupZegoForUser(userId: String, userName: String) {
        try {
            val appID: Long = Constants.APP_ID.toLong()
            val appSign: String = Constants.APP_SIGN
            val config = ZegoUIKitPrebuiltCallInvitationConfig()

            if (isZegoInitialized) {
                ZegoUIKitPrebuiltCallService.unInit()
                Log.d(TAG, "⚠️ Zego đã init trước đó, đang re-init...")
            }

            ZegoUIKitPrebuiltCallService.init(
                application,
                appID,
                appSign,
                userId,
                userName,
                config
            )

            isZegoInitialized = true
            Log.d(TAG, "✅ Zego Call Service initialized for user: $userId ($userName)")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi khi init Zego: ${e.message}", e)
            isZegoInitialized = false
        }
    }

    // ✅ QUAN TRỌNG: Login ZIM để có thể gửi/nhận call invitation
    private fun loginZIM(userId: String, userName: String) {
        try {
            val zimUserInfo = ZIMUserInfo().apply {
                userID = userId
                this.userName = userName
            }

            ZIM.getInstance()?.login(zimUserInfo, object : ZIMLoggedInCallback {
                override fun onLoggedIn(errorInfo: ZIMError?) {
                    if (errorInfo == null || errorInfo.code == ZIMErrorCode.SUCCESS) {
                        isZIMLoggedIn = true
                        Log.d(TAG, "✅ ZIM logged in successfully for user: $userId")
                    } else {
                        isZIMLoggedIn = false
                        Log.e(TAG, "❌ ZIM login failed: ${errorInfo.code} - ${errorInfo.message}")
                    }
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi khi login ZIM: ${e.message}", e)
            isZIMLoggedIn = false
        }
    }

    // -------------------- Firebase Presence --------------------
    private fun setupUserPresence() {
        val user = auth.currentUser ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("InfoUser").child(user.uid)
        val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")

        connectedRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (snapshot.getValue(Boolean::class.java) == true) {
                    userRef.child("isOnline").setValue(true)
                    userRef.child("isOnline").onDisconnect().setValue(false)
                    userRef.child("lastLogin").onDisconnect().setValue(ServerValue.TIMESTAMP)
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    // -------------------- Save Session --------------------
    private fun saveUserSession() {
        auth.currentUser?.let { user ->
            val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean("is_logged_in", true)
                putString("user_id", user.uid)
                putString("email", user.email)
                putString("full_name", user.displayName ?: "User")
                apply()
            }
        }
    }

    // -------------------- Navigation --------------------
    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        onBackPressedDispatcher.addCallback(this) {
            if (navController.currentDestination?.id != R.id.homeFragment) {
                navController.navigate(R.id.homeFragment)
                binding.bottomNavigation.selectedItemId = R.id.nav_home
            } else finish()
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.chatFragment, R.id.callFragment, R.id.messageFragment, R.id.notificationFragment -> {
                    binding.container.visibility = View.GONE
                    binding.navHostFragment.setPadding(0, 0, 0, 0)
                }
                else -> {
                    binding.container.visibility = View.VISIBLE
                    binding.navHostFragment.setPadding(0, 0, 0, dpToPx(80))
                }
            }
        }
    }

    // -------------------- Bottom Navigation --------------------
    private fun setupBottomNav() {
        binding.bottomNavigation.selectedItemId = R.id.nav_home
        animateIcon(R.id.nav_home)
        currentItemId = R.id.nav_home

        binding.btnUpload.setOnClickListener {
            navController.navigate(R.id.uploadFragment)
            resetPreviousIcon()
            currentItemId = null
        }

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

            animateIcon(item.itemId)
            currentItemId = item.itemId
            true
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
        }
        binding.bottomNavigation.menu.setGroupCheckable(0, false, false)
        for (i in 0 until binding.bottomNavigation.menu.size()) {
            binding.bottomNavigation.menu.getItem(i).isChecked = false
        }
        binding.bottomNavigation.menu.setGroupCheckable(0, true, false)
    }

    // -------------------- Utils --------------------
    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp() || super.onSupportNavigateUp()

    override fun onDestroy() {
        super.onDestroy()

        // ✅ Logout ZIM trước
        if (isZIMLoggedIn) {
            ZIM.getInstance()?.logout()
            isZIMLoggedIn = false
            Log.d(TAG, "🧹 ZIM logged out")
        }

        // ✅ Cleanup Zego Call Service
        if (isZegoInitialized) {
            ZegoUIKitPrebuiltCallService.unInit()
            isZegoInitialized = false
            Log.d(TAG, "🧹 Zego service cleaned up")
        }
    }
}