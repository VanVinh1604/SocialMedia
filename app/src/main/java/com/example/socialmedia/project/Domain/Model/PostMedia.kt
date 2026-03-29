package com.example.socialmedia.project.Domain.Model

data class PostMedia(
    val mediaId: String = "",
    val postId: String = "",
    val mediaUrl: String = "",
    val mediaOrder: Int = 0,
    val width: Int = 1080,
    val height: Int = 1080

)