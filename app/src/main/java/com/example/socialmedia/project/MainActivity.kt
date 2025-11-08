package com.example.socialmedia

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.os.Looper
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
        var isZIMLoggedIn = false
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

        // ✅ Đảm bảo lấy userId và userName một cách đáng tin cậy
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = sharedPref.getString("user_id", null)
        val fullName = sharedPref.getString("full_name", null)

        if (!userId.isNullOrEmpty() && !fullName.isNullOrEmpty()) {
            Log.d(TAG, "✅ User data found: userId=$userId, name=$fullName")
            initZegoWithRetry(userId, fullName, maxRetries = 3)
        } else {
            Log.e(TAG, "❌ User data missing! userId=$userId, fullName=$fullName")
            // Thử lấy từ FirebaseAuth nếu có
            auth.currentUser?.let { user ->
                val fallbackUserId = user.uid
                val fallbackName = user.displayName ?: "User"
                Log.d(TAG, "⚠️ Using FirebaseAuth fallback: $fallbackUserId, $fallbackName")
                initZegoWithRetry(fallbackUserId, fallbackName, maxRetries = 3)
            }
        }
    }

    // ✅ Khởi tạo Zego với retry mechanism
    private fun initZegoWithRetry(userId: String, userName: String, maxRetries: Int, currentAttempt: Int = 1) {
        try {
            // Kiểm tra lại dữ liệu trước khi init
            if (userId.isEmpty() || userName.isEmpty()) {
                Log.e(TAG, "❌ Invalid user data: userId=$userId, userName=$userName")
                return
            }

            val appID: Long = Constants.APP_ID.toLong()
            val appSign: String = Constants.APP_SIGN
            val config = ZegoUIKitPrebuiltCallInvitationConfig()

            // ✅ UnInit nếu đã init trước đó
            if (isZegoInitialized) {
                Log.d(TAG, "⚠️ Zego đã init, đang unInit...")
                ZegoUIKitPrebuiltCallService.unInit()
                isZegoInitialized = false
                // Đợi một chút để cleanup hoàn tất
                Thread.sleep(500)
            }

            // ✅ Init Zego Call Service
            ZegoUIKitPrebuiltCallService.init(
                application,
                appID,
                appSign,
                userId,
                userName,
                config
            )

            isZegoInitialized = true
            Log.d(TAG, "✅ Zego initialized successfully for: $userId ($userName)")

            // ✅ Đợi một chút để Zego khởi tạo hoàn tất
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                loginZIMWithRetry(userId, userName, maxRetries = 3)
            }, 1000)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Zego init failed (attempt $currentAttempt/$maxRetries): ${e.message}", e)
            isZegoInitialized = false

            // Retry nếu chưa hết số lần thử
            if (currentAttempt < maxRetries) {
                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    Log.d(TAG, "🔄 Retrying Zego init... (attempt ${currentAttempt + 1}/$maxRetries)")
                    initZegoWithRetry(userId, userName, maxRetries, currentAttempt + 1)
                }, 2000L * currentAttempt) // Tăng dần delay: 2s, 4s, 6s...
            }
        }
    }

    // ✅ Login ZIM với retry và null check
    private fun loginZIMWithRetry(userId: String, userName: String, maxRetries: Int, currentAttempt: Int = 1) {
        try {
            // ✅ QUAN TRỌNG: Kiểm tra ZIM instance có sẵn không
            val zimInstance = ZIM.getInstance()
            if (zimInstance == null) {
                Log.e(TAG, "❌ ZIM instance is null! (attempt $currentAttempt/$maxRetries)")

                if (currentAttempt < maxRetries) {
                    android.os.Handler(Looper.getMainLooper()).postDelayed({
                        Log.d(TAG, "🔄 Retrying ZIM login... (attempt ${currentAttempt + 1}/$maxRetries)")
                        loginZIMWithRetry(userId, userName, maxRetries, currentAttempt + 1)
                    }, 1500L * currentAttempt)
                }
                return
            }

            // ✅ Tạo ZIMUserInfo với null check
            val zimUserInfo = ZIMUserInfo()
            zimUserInfo.userID = userId
            zimUserInfo.userName = userName

            // ✅ Kiểm tra dữ liệu trước khi login
            if (zimUserInfo.userID.isNullOrEmpty()) {
                Log.e(TAG, "❌ ZIMUserInfo.userID is null or empty!")
                return
            }

            Log.d(TAG, "🔐 Attempting ZIM login: userId=$userId, userName=$userName")

            zimInstance.login(zimUserInfo, object : ZIMLoggedInCallback {
                override fun onLoggedIn(errorInfo: ZIMError?) {
                    if (errorInfo == null || errorInfo.code == ZIMErrorCode.SUCCESS) {
                        isZIMLoggedIn = true
                        Log.d(TAG, "✅ ZIM logged in successfully for: $userId")
                    } else {
                        isZIMLoggedIn = false
                        Log.e(TAG, "❌ ZIM login failed: ${errorInfo.code} - ${errorInfo.message}")

                        // Retry nếu chưa hết lần thử
                        if (currentAttempt < maxRetries) {
                            android.os.Handler(Looper.getMainLooper()).postDelayed({
                                Log.d(TAG, "🔄 Retrying ZIM login after error... (attempt ${currentAttempt + 1}/$maxRetries)")
                                loginZIMWithRetry(userId, userName, maxRetries, currentAttempt + 1)
                            }, 2000L * currentAttempt)
                        }
                    }
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "❌ ZIM login exception (attempt $currentAttempt/$maxRetries): ${e.message}", e)
            isZIMLoggedIn = false

            if (currentAttempt < maxRetries) {
                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    Log.d(TAG, "🔄 Retrying ZIM login after exception... (attempt ${currentAttempt + 1}/$maxRetries)")
                    loginZIMWithRetry(userId, userName, maxRetries, currentAttempt + 1)
                }, 2000L * currentAttempt)
            }
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
                R.id.chatFragment, R.id.callFragment, R.id.messageFragment, R.id.notificationFragment,R.id.addStoryFragment,R.id.storyViewerFragment -> {
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
            try {
                ZIM.getInstance()?.logout()
                isZIMLoggedIn = false
                Log.d(TAG, "🧹 ZIM logged out")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Error during ZIM logout: ${e.message}")
            }
        }

        // ✅ Cleanup Zego Call Service
        if (isZegoInitialized) {
            try {
                ZegoUIKitPrebuiltCallService.unInit()
                isZegoInitialized = false
                Log.d(TAG, "🧹 Zego service cleaned up")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Error during Zego cleanup: ${e.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = sharedPref.getString("user_id", null)
        val fullName = sharedPref.getString("full_name", null)

        if (!isZegoInitialized || !isZIMLoggedIn) {
            Log.w(TAG, "⚠️ Reconnecting Zego/ZIM after resume")
            if (!userId.isNullOrEmpty() && !fullName.isNullOrEmpty()) {
                initZegoWithRetry(userId, fullName, maxRetries = 3)
            }
        }
    }

}