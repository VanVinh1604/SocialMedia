package com.example.socialmedia.project.Domain.Model

import java.util.UUID

data class HashtagModel(
    val hashtagId: String = UUID.randomUUID().toString(),
    val name: String = "",                // Without #
    val postCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
