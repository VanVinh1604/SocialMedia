package com.example.socialmedia.project.Domain.Model

import java.util.UUID

data class UserSettingsModel(
    val settingsId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val privateAccount: Boolean = false,
    val showActivityStatus: Boolean = true,
    val allowTagging: Boolean = true,
    val allowMentions: Boolean = true,
    val hideStoryFrom: List<String> = emptyList(),
    val allowMessagesFrom: String = "EVERYONE",
    val pushNotifications: Boolean = true,
    val emailNotifications: Boolean = true,
    val likeNotifications: Boolean = true,
    val commentNotifications: Boolean = true,
    val followNotifications: Boolean = true,
    val messageNotifications: Boolean = true,
    val sensitiveContentFilter: Boolean = true,
    val hideOffensiveComments: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
