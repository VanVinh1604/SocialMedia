package com.example.socialmedia.project.Adapter

import com.example.socialmedia.project.Domain.Model.NotificationModel

// Các loại item có thể hiển thị trong RecyclerView
sealed class NotificationItem {
    data class Header(val title: String) : NotificationItem()
    data class NotificationData(val data: NotificationModel) : NotificationItem()
}

// Các hằng số cho view type
const val TYPE_HEADER = 0
const val TYPE_NOTIFICATION = 1
