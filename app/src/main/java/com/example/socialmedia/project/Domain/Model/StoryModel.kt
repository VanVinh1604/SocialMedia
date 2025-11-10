package com.example.socialmedia.project.Domain.Model

import com.example.socialmedia.project.Domain.Enum.MediaType
import java.util.UUID

data class StoryModel(
    val storyId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val userName: String = "",                // 🔹 tên người đăng
    val userProfileImage: String = "",
    val mediaType: MediaType = MediaType.IMAGE,
    val mediaUrl: String = "",
    val thumbnailUrl: String? = null,
    val duration: Int = 5,
    val backgroundColor: String? = null,
    val textOverlay: TextOverlay? = null,
    val stickers: List<Sticker>? = null,
    val musicId: String? = null,
    val linkUrl: String? = null,
    val allowsReplies: Boolean = true,
    val allowsSharing: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86400000,
    val viewCount: Int = 0,

    val isViewed: Boolean = false,
    val userStoryViews: MutableMap<String, Boolean> = mutableMapOf(),

    var isFollowing: Boolean = false,

    val isAddStory: Boolean = false,          // ✅ thêm flag này
    val isSuggestFriend: Boolean = false,      // ✅ thêm flag này

    val storyCount: Int = 1
)