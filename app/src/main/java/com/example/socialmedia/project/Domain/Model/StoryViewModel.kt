package com.example.socialmedia.project.Domain.Model

import java.util.UUID

data class StoryViewModel(
    val viewId: String = UUID.randomUUID().toString(),
    val storyId: String = "",
    val viewerId: String = "",
    val viewedAt: Long = System.currentTimeMillis(),
    val completionRate: Double = 0.0      // 0.0 - 1.0
)
