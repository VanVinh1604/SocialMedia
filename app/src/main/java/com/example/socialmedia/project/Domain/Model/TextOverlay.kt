package com.example.socialmedia.project.Domain.Model


data class TextOverlay(
    val text: String = "",
    val positionX: Double = 0.5,
    val positionY: Double = 0.5,
    val color: String = "#FFFFFF",
    val fontSize: Int = 16,
    val fontFamily: String = "Arial"
)