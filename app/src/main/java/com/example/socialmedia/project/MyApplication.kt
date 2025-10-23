package com.example.socialmedia

import android.app.Application
import com.example.socialmedia.project.Helper.CloudinaryHelper

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Khởi tạo Cloudinary khi app start
        CloudinaryHelper.initialize(this)
    }
}