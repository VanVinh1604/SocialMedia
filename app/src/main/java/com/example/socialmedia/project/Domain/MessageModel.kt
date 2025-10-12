package com.example.socialmedia.project.Domain

data class MessageModel(
    val messageId: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Long
)
