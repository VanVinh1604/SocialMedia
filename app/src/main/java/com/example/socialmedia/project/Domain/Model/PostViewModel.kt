package com.example.socialmedia.project.Domain.Model

import java.util.UUID

data class PostViewModel(
    val viewId: String = UUID.randomUUID().toString(),
    val postId: String = "",
    val viewerId: String? = null,         // Null for anonymous
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val viewedAt: Long = System.currentTimeMillis(),
    val viewDuration: Int? = null         // Seconds
)