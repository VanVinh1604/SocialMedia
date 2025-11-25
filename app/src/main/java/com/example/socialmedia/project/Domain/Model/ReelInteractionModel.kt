// ReelInteractionModel.kt – giữ lại để đọc dữ liệu
package com.example.socialmedia.project.Domain.Model

data class ReelInteractionModel(
    val interactionId: String = "",
    val userId: String = "",
    val reelId: String = "",
    val interactionType: String = "",
    val hashtags: List<String> = emptyList(),
    val timestamp: Long = 0L,
    val duration: Long = 0L
)