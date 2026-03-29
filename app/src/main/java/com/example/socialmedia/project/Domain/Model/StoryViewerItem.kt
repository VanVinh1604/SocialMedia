package com.example.socialmedia.project.Domain.Model

data class StoryViewerItem(
    val userId: String,
    val userName: String,
    val userAvatar: String?,
    val hasLiked: Boolean = false
)
