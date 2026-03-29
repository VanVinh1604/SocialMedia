package com.example.socialmedia.project.Domain.Model

import com.example.socialmedia.project.Domain.Enum.CommentableType
import java.util.UUID

data class CommentModel(
    val commentId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val commentableType: CommentableType = CommentableType.POST,
    val commentableId: String = "",
    val parentCommentId: String? = null,
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val editedAt: Long? = null,
    var likeCount: Int = 0,
    val replyCount: Int = 0,
    val isPinned: Boolean = false

)
