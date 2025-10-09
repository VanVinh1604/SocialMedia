package com.example.socialmedia.Fragment.Domain

data class PostModel(
    val postId: String = "",
    val userId: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val likeCount: Int = 0,
    val shareCount: Int = 0
)

