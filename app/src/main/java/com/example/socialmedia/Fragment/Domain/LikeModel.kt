package com.example.socialmedia.Fragment.Domain

data class LikeModel(
    val likeId: String = "",
    val postId: String = "",
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis()

)
