package com.example.socialmedia.project.Domain.Model

import com.example.socialmedia.project.Domain.Enum.NotificationTargetType
import com.example.socialmedia.project.Domain.Enum.NotificationType
import java.util.UUID

data class NotificationModel(
    val notificationId: String = UUID.randomUUID().toString(),
    val userId: String = "",              // Recipient
    val actorId: String = "",             // Person who triggered
    val notificationType: NotificationType = NotificationType.LIKE,
    val targetType: NotificationTargetType? = null,
    val targetId: String? = null,
    val content: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val readAt: Long? = null
)

