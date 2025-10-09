package com.example.socialmedia.Fragment.Domain

data class CommentModel(
    val commentId: String = "",
    val postId: String = "",
    val userId: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis()

)
