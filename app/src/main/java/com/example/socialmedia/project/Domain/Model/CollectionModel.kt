package com.example.socialmedia.project.Domain.Model

import java.util.UUID


data class CollectionModel(
    val collectionId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val name: String = "",
    val coverPostId: String? = null,
    val isPrivate: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val postCount: Int = 0
)
