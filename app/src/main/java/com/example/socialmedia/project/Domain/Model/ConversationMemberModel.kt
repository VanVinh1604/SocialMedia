package com.example.socialmedia.project.Domain.Model

import java.util.UUID

data class ConversationMemberModel(
    var memberId: String = UUID.randomUUID().toString(),
    val conversationId: String = "",
    val userId: String = "",
    val role: String = "MEMBER",         // ADMIN, MEMBER
    val joinedAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long? = null,
    val isMuted: Boolean = false
){
    // Ensure memberId always has a value when serialized
    fun ensureId(): ConversationMemberModel {
        if (memberId.isBlank()) memberId = UUID.randomUUID().toString()
        return this
    }
}
