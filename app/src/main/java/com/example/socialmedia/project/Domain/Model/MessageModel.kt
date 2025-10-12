package com.example.socialmedia.project.Domain.Model

import com.example.socialmedia.project.Domain.Enum.MessageType
import java.util.UUID

data class MessageModel(
    val messageId: String = UUID.randomUUID().toString(),
    val conversationId: String = "",
    val senderId: String = "",
    val messageType: MessageType = MessageType.TEXT,
    val content: String? = null,
    val mediaUrl: String? = null,
    val replyToMessageId: String? = null,
    val postId: String? = null,           // For shared posts
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val editedAt: Long? = null,
    val readBy: List<String> = emptyList()
)
