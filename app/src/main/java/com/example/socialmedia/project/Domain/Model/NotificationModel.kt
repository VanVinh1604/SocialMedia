package com.example.socialmedia.project.Domain.Model

import com.example.socialmedia.project.Domain.Enum.NotificationTargetType
import com.example.socialmedia.project.Domain.Enum.NotificationType
import java.util.UUID

data class NotificationModel(
    val notificationId: String = UUID.randomUUID().toString(),
    val userId: String = "",              // người nhận thông báo
    val actorId: String = "",             // người thực hiện hành động (ví dụ: user A like bài của bạn)
    val notificationType: NotificationType = NotificationType.LIKE, // loại thông báo
    val actorName: String = "",   // 🔥 phải có để Firebase map dữ liệu
    val actorAvatar: String = "",
    val targetType: NotificationTargetType? = null,  // loại đối tượng (post, comment, story...)
    val targetId: String? = null,         // id đối tượng (id bài viết, id comment...)
    val content: String? = null,          // nội dung tùy chỉnh
    val isRead: Boolean = false,          // đã đọc hay chưa
    val createdAt: Long = System.currentTimeMillis(),// thời gian tạo
    val readAt: Long? = null              // thời gian đọc
)

