package com.example.socialmedia

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
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

    private val pickImagesLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val bundle = Bundle().apply {
                    putParcelable("data", data)
                }
                supportFragmentManager.setFragmentResult("imagePickerResult", bundle)
            }
        }

    fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        pickImagesLauncher.launch(Intent.createChooser(intent, "Chọn ảnh"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        replaceFragment(HomeFragment())

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
