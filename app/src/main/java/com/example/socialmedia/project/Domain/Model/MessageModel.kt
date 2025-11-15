package com.example.socialmedia.project.Domain.Model

import com.example.socialmedia.project.Domain.Enum.MessageType
import java.util.UUID

data class MessageModel(
    var messageId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String? = null,
    val messageType: MessageType = MessageType.TEXT,
    val content: String = "",
    val duration: String? = null,
    val mediaUrl: String? = null,
    val replyToMessageId: String? = null,
    val postId: String? = null,           // For shared posts
    val story: StoryModel? = null,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val editedAt: Long? = null,
    val readBy: List<String> = emptyList(),


    val isStoryReply: Boolean = false,
    val storyId: String? = null,
    val storyThumbnail: String? = null,
    val storyOwnerId: String? = null
)
