package com.example.socialmedia.project.Domain.Model

import com.example.socialmedia.project.Domain.Enum.FollowStatus
import java.util.UUID

data class FollowModel(
    val followId: String = UUID.randomUUID().toString(),
    val followerId: String = "",
    val followingId: String = "",
    val status: FollowStatus = FollowStatus.ACCEPTED,
    val createdAt: Long = System.currentTimeMillis()
)