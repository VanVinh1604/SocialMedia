package com.example.socialmedia.project.Domain.Model

import java.util.UUID

data class CloseFriendModel(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val friendId: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

