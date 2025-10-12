package com.example.socialmedia.project.Domain.Model

import java.util.UUID

data class BlockedUserModel(
    val blockId: String = UUID.randomUUID().toString(),
    val blockerId: String = "",           // Person blocking
    val blockedId: String = "",           // Person being blocked
    val createdAt: Long = System.currentTimeMillis()
)