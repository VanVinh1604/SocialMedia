package com.example.socialmedia.project.Domain.Model

import java.util.UUID

data class Sticker(
    val stickerId: String = UUID.randomUUID().toString(),
    val type: String = "",
    val positionX: Double = 0.5,
    val positionY: Double = 0.5,
    val rotation: Double = 0.0,
    val scale: Double = 1.0,
    val data: Map<String, Any>? = null
)
