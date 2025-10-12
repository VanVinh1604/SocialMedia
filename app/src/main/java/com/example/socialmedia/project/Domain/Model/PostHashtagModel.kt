package com.example.socialmedia.project.Domain.Model

import java.util.UUID

data class PostHashtagModel(
    val id: String = UUID.randomUUID().toString(),
    val postId: String = "",
    val hashtagId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
