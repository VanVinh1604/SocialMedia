package com.example.socialmedia.project.Domain.Model

import com.example.socialmedia.project.Domain.Enum.LikeableType
import java.util.UUID

data class LikeModel(
    val likeId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val likeableType: LikeableType = LikeableType.POST,
    val likeableId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)