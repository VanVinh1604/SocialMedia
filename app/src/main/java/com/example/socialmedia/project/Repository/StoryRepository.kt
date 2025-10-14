package com.example.socialmedia.project.data.repository

import com.example.socialmedia.project.Domain.Model.StoryModel
import com.example.socialmedia.project.Server.Firebase.FirebaseService

class StoryRepository(
    private val firebaseService: FirebaseService = FirebaseService()
) {

    fun fetchStoriesAndFollowState(
        currentUserId: String,
        onSuccess: (List<StoryModel>, Boolean) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firebaseService.listenStories(onResult = { stories ->
            firebaseService.getUserFollowing(
                currentUserId = currentUserId,
                onResult = { followingList ->
                    val hasFollowed = followingList.isNotEmpty()
                    onSuccess(stories, hasFollowed)
                },
                onError = onFailure
            )
        }, onError = onFailure)
    }
}
