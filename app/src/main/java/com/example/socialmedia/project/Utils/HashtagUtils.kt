package com.example.socialmedia.project.Utils

object HashtagUtils {

    fun extractHashtags(caption: String?): List<String> {
        if (caption.isNullOrBlank()) return emptyList()

        return caption.split("\\s+".toRegex())
            .filter { it.startsWith("#") && it.length > 1 }
            .map { it.substring(1).lowercase().replace("[^a-z0-9]".toRegex(), "") }
            .filter { it.isNotEmpty() }
            .distinct()
    }


    fun formatHashtags(hashtags: List<String>): String {
        return hashtags.joinToString(" ") { "#$it" }
    }
}