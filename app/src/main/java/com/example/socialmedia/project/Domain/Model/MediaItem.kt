package com.example.socialmedia.project.Domain.Model

import android.net.Uri
import com.example.socialmedia.project.Domain.Enum.MediaType

data class MediaItem(
    val uri: Uri,
    val type: MediaType,
    val duration: Long? = null, // Thời lượng video (milliseconds)
    val thumbnailUri: Uri? = null // Thumbnail cho video (optional)
)