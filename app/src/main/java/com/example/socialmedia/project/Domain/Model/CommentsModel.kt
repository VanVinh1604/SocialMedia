package com.example.socialmedia.project.Domain.Model

import java.util.UUID

data class CommentsModel(
    var commentId: String = UUID.randomUUID().toString(),
    var userId: String = "",
    var commentableType: String = "REEL",  // CHANGED: String instead of Enum
    var commentableId: String = "",

    var parentCommentId: String? = null,
    var content: String = "",
    var likeCount: Int = 0,
    var replyCount: Int = 0,
    var pinned: Boolean = false,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
) {
    // Constructor không tham số cho Firebase
    constructor() : this(
        commentId = UUID.randomUUID().toString(),
        userId = "",
        commentableType = "REEL",
        commentableId = "",
        parentCommentId = null,
        content = "",
        likeCount = 0,
        replyCount = 0,
        pinned = false,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}