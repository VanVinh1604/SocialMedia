package com.example.socialmedia.project.Domain.Model

data class StoryHighlightModel(
    val id: String = "",
    val name: String = "",         // Tên bộ highlight (VD: "Đà Lạt")
    val coverUrl: String = "",     // Ảnh bìa của vòng tròn
    val userId: String = "",
    val storyIds: List<String> = emptyList(), // Danh sách ID các story bên trong
    val createdAt: Long = System.currentTimeMillis()
)