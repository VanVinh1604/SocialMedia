package com.example.socialmedia.project.Domain.Enum

enum class AudienceType(val displayName: String) {
    PUBLIC("Public"),
    FRIENDS("Friends"),
    ONLY_ME("Only me"),
    FRIENDS_EXCEPT("Friends except..."),
    SPECIFIC_FRIENDS("Specific friends");
}

