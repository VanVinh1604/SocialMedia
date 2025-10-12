package com.example.socialmedia.project.Domain.Model

import com.example.socialmedia.project.Domain.Enum.AspectRatio
import com.example.socialmedia.project.Domain.Enum.PostType
import java.util.UUID

data class PostModel(
    val postId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val caption: String? = null,
    val locationName: String? = null,
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val postType: PostType = PostType.PHOTO,
    val aspectRatio: AspectRatio? = AspectRatio.SQUARE_1_1,
    val isArchived: Boolean = false,
    val allowsComments: Boolean = true,
    val allowsLikesVisible: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val editedAt: Long? = null,
    val scheduledAt: Long? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val viewCount: Int = 0
)