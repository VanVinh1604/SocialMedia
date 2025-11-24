package com.example.socialmedia.project.Domain.Model

import com.example.socialmedia.project.Domain.Enum.ConversationType
import java.util.UUID

data class ConversationModel(
    var conversationId: String = UUID.randomUUID().toString(),

    val type: ConversationType = ConversationType.DIRECT,

    val participants: List<String> = emptyList(),

    val name: String? = null,
    val photoUrl: String? = null,
    val createdBy: String = "",

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    val lastMessageAt: Long? = null,
    val lastMessagePreview: String? = null,

    val unreadCount: Map<String, Long> = emptyMap(),

    val lastMessageSenderId: String? = null,
    val lastMessageSenderName: String? = null,
    val lastMessageSenderAvatar: String? = null,
    val lastMessageIsDeleted: Boolean = false,

    val adminIds: List<String> = emptyList(),
    val memberCount: Int = 0,

    val groupPreview: List<String?>? = null,
) {
    val isGroup: Boolean get() = type == ConversationType.GROUP
}
