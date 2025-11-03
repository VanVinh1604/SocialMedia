package com.example.socialmedia

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.socialmedia.project.Helper.CloudinaryHelper

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Khởi tạo Cloudinary khi app start
        CloudinaryHelper.initialize(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "call_channel",
                "Call Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming call notifications"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

    }
}