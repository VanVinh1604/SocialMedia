package com.example.socialmedia.project.Domain.Model

import com.example.socialmedia.project.Domain.Enum.MediaType
import java.util.UUID

data class PostMediaModel(
    val mediaId: String = UUID.randomUUID().toString(),
    val postId: String = "",
    val mediaType: MediaType = MediaType.IMAGE,
    val mediaUrl: String = "",
    val thumbnailUrl: String? = null,
    val mediaOrder: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val duration: Int? = null,
    val fileSize: Long = 0,
    val altText: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)