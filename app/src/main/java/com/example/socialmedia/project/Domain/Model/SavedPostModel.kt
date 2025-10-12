package com.example.socialmedia.project.Domain.Model

import java.util.UUID


data class SavedPostModel(
    val savedId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val postId: String = "",
    val collectionId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)