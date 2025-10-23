package com.example.socialmedia.project.Domain

import com.example.socialmedia.R

data class UserTestMess(
    val name: String,
    val avatarRes: Int = R.drawable.baseline_person_24,
    val isOnline: Boolean
)

