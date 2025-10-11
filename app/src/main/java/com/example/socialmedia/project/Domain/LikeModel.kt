package com.example.socialmedia.project.Domain

data class LikeModel(
    val likeId: String = "",
    val postId: String = "",
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis()

)
