package com.example.socialmedia.project.Domain.Model

import com.example.socialmedia.project.Domain.Enum.MediaType
import java.util.UUID

data class StoryModel(
    val storyId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val userName: String = "",
    val userProfileImage: String = "",

    // 🔹 Firebase sẽ map vào đây
    val type: String? = "image",
    var isExpired: Boolean = false,

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
    var viewCount: Int = 0,
    var isViewed: Boolean = false,
    val userStoryViews: MutableMap<String, Boolean> = mutableMapOf(),
    val userLikes: MutableMap<String, Boolean> = mutableMapOf(), // ✅ thêm map like

    var isFollowing: Boolean = false,
    val isAddStory: Boolean = false,
    val isSuggestFriend: Boolean = false,
    val storyCount: Int = 1
) {
    // 🔹 Chuyển "type" thành Enum an toàn
    val mediaType: MediaType
        get() = when (type?.lowercase()) {
            "video" -> MediaType.VIDEO
            else -> MediaType.IMAGE
        }
}
