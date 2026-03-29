package com.example.socialmedia.project.Domain.Model

data class MessageItem(
    val userId: String = "",
    val name: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val userProfileImage: String? = null
)
