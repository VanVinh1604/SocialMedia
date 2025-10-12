package com.example.socialmedia.project.Domain.Model

import java.util.UUID

data class MusicModel(
    val musicId: String = UUID.randomUUID().toString(),
    val title: String = "",
    val artist: String = "",
    val audioUrl: String = "",
    val duration: Int = 0,                // Seconds
    val coverImageUrl: String? = null,
    val isPopular: Boolean = false,
    val useCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
