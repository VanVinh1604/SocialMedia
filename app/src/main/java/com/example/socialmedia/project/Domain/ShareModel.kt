package com.example.socialmedia.project.Domain

data class ShareModel(
    val shareId: String = "",
    val postId: String = "",
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
