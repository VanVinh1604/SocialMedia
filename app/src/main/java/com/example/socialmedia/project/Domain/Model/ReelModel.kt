package com.example.socialmedia.project.Domain.Model

import java.util.UUID


data class ReelModel(
    val reelId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val caption: String? = null,
    val duration: Int = 0,
    val musicId: String? = null,
    val allowsComments: Boolean = true,
    val allowsDuet: Boolean = true,
    val allowsRemix: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val viewCount: Int = 0,
    var likeCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val likedBy: Map<String, Boolean> = emptyMap()
)