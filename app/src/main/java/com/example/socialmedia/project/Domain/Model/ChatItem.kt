package com.example.socialmedia.project.Domain.Model

data class ChatItem(
    val user: UserModel,
    val lastMessage: MessageModel?
)
