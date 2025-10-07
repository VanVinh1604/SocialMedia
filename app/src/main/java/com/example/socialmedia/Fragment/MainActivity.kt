package com.example.socialmedia

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.socialmedia.Fragment.Fragment.HomeFragment
import com.example.socialmedia.Fragment.Fragment.ProfileFragment
import com.example.socialmedia.Fragment.Fragment.ReelsFragment
import com.example.socialmedia.Fragment.Fragment.SearchFragment
import com.example.socialmedia.Fragment.Fragment.UploadFragment
import com.example.socialmedia.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Chỉ inflate 1 lần
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Mặc định hiển thị HomeFragment khi mở app
        replaceFragment(HomeFragment())

        // ✅ Xử lý chọn Bottom Navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_search -> replaceFragment(SearchFragment())
                R.id.nav_upload -> replaceFragment(UploadFragment())
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
