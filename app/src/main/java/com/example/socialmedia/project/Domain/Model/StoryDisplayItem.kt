package com.example.socialmedia.project.Domain.Model

import com.example.socialmedia.project.Domain.Enum.StoryItemType

data class StoryDisplayItem(
    val type: StoryItemType,
    val user: UserModel? = null,
    val story: StoryModel? = null
)

