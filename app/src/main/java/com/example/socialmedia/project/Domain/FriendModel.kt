package com.example.socialmedia.project.Domain

data class FriendModel(
    val friendshipId: String = "",
    val requesterId: String = "",
    val receiverId: String = "",
    val status: String = "pending", // pending, accepted, blocked
    val createdAt: Long = System.currentTimeMillis()

)
