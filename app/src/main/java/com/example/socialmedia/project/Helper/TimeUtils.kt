package com.example.socialmedia.project.Helper

import android.content.Context
import com.example.socialmedia.R
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object TimeUtils {

    fun getTimeAgo(timeInMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timeInMillis

        // Trường hợp bài đăng tương lai (do lệch giờ)
        if (diff < 0) return "Vừa xong"

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            seconds < 60 -> "Vừa xong"
            minutes < 60 -> "$minutes phút trước"
            hours < 24 -> "$hours giờ trước"
            days == 1L -> "Hôm qua"
            days < 7 -> "$days ngày trước"
            days < 30 -> "${days / 7} tuần trước"
            days < 365 -> "${days / 30} tháng trước"
            else -> {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                dateFormat.format(Date(timeInMillis))
            }
        }
    }
}
