package com.example.socialmedia.project.Domain.Model

import com.example.socialmedia.project.Domain.Enum.AspectRatio
import com.example.socialmedia.project.Domain.Enum.PostType
import java.util.UUID

data class PostModel(
    var postId: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val userName: String? = null,
    val userProfileUrl: String? = null,
    val caption: String? = null,
    val hashtags: List<String> = emptyList(),  // THÊM MỚI
    val locationName: String? = null,
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val musicId: String? = null,  // THÊM MỚI
    val taggedUserIds: List<String> = emptyList(),  // THÊM MỚI
    val audienceType: String = "Công khai",  // THÊM MỚI
    val postType: PostType = PostType.PHOTO,
    val aspectRatio: AspectRatio? = AspectRatio.SQUARE_1_1,
    val isArchived: Boolean = false,
    val isDraft: Boolean = false,  // THÊM MỚI
    val allowsComments: Boolean = true,
    var mediaList: List<PostMediaModel> = emptyList(),
    val allowsLikesVisible: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val editedAt: Long? = null,
    val scheduledAt: Long? = null,
    var likeCount: Int = 0,
    var isLikedByCurrentUser: Boolean = false,
    var commentCount: Int = 0,
    var shareCount: Int = 0,
    val isReel: Boolean = false,
    var viewCount: Int = 0
)