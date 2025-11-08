package com.example.socialmedia.project.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmedia.project.Domain.Model.StoryModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.data.repository.StoryRepository
import com.example.socialmedia.project.Server.Firebase.FirebaseService

class StoryViewModel(
    private val repository: StoryRepository = StoryRepository(),
    private val firebaseService: FirebaseService = FirebaseService()
) : ViewModel() {

    private val _stories = MutableLiveData<List<StoryModel>>()
    val stories: LiveData<List<StoryModel>> get() = _stories

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private var currentUserId: String = ""


    fun loadStories(currentUserId: String) {
        this.currentUserId = currentUserId

        firebaseService.listenUsers(onResult = { users ->
            firebaseService.listenStories(onResult = { storyList ->
                repository.fetchStoriesAndFollowState(
                    currentUserId,
                    onSuccess = { _, _ ->
                        val finalList = mutableListOf<StoryModel>()

                        finalList.add(StoryModel(isAddStory = true))

                        val followingList = storyList.map { it.userId }
                        val notFollowedUsers =
                            users.filter { it.userId !in followingList && it.userId != currentUserId }
                        notFollowedUsers.forEach { user ->
                            finalList.add(
                                StoryModel(
                                    userId = user.userId,
                                    userName = user.fullName,
                                    userProfileImage = user.profilePictureUrl ?: "",
                                    isSuggestFriend = true
                                )
                            )
                        }

                        // Map tất cả story → gán fullname + avatar mới
                        finalList.addAll(storyList.map { story ->
                            val user = users.find { it.userId == story.userId }
                            if (user != null) story.copy(
                                userName = user.fullName,
                                userProfileImage = user.profilePictureUrl ?: ""
                            ) else story
                        })

                        _stories.value = finalList
                    },
                    onFailure = { e ->
                        _error.value = e.message
                    }
                )
            }, onError = { e -> _error.value = e.message })
        }, onError = { e -> _error.value = e.message })
    }

    fun loadUserStories(userId: String) {
        firebaseService.getStoriesByUserId(userId) { stories ->
            firebaseService.getUserById(userId) { user ->
                val storiesWithUser = stories.map { story ->
                    story.copy(
                        userName = user.fullName,
                        userProfileImage = user.profilePictureUrl ?: ""
                    )
                }
                _stories.value = storiesWithUser
            }
        }
    }
}


