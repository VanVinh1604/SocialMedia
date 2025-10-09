package com.example.socialmedia.Fragment.Domain

data class ShareModel(
    val shareId: String = "",
    val postId: String = "",
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
