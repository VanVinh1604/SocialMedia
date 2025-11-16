package com.example.socialmedia.project.Domain.Model

import com.example.socialmedia.project.Domain.Enum.ConversationType
import java.util.UUID

data class ConversationModel(
    val conversationId: String = UUID.randomUUID().toString(),
    val conversationType: ConversationType = ConversationType.DIRECT,
    val participants: List<String> = emptyList(),
    val name: String? = null,            // Group name
    val photoUrl: String? = null,        // Group photo
    val createdBy: String = "",          // User ID
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastMessageAt: Long? = null,
    val lastMessagePreview: String? = null,
    val unreadCount: Map<String, Long> = emptyMap(),

    val lastMessageSenderId: String? = null,
    val lastMessageSenderName: String? = null,
    val lastMessageSenderAvatar: String? = null,
    val lastMessageIsDeleted: Boolean? = false // ✅ thêm

)
