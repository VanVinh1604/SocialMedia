package com.example.socialmedia.project.Domain.Model

import java.util.UUID

data class ConversationMemberModel(
    val memberId: String = UUID.randomUUID().toString(),
    val conversationId: String = "",
    val userId: String = "",
    val role: String = "MEMBER",         // ADMIN, MEMBER
    val joinedAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long? = null,
    val isMuted: Boolean = false
)

