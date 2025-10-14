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

        // Lắng nghe users realtime
        firebaseService.listenUsers(onResult = { users ->
            // Lắng nghe stories realtime
            firebaseService.listenStories(onResult = { storyList ->
                // Lấy danh sách following
                repository.fetchStoriesAndFollowState(
                    currentUserId,
                    onSuccess = { _, _ ->
                        val finalList = mutableListOf<StoryModel>()

                        // Add "Add Story"
                        finalList.add(StoryModel(isAddStory = true))

                        // Suggest Friend nếu chưa follow ai
                        val followingList = storyList.map { it.userId }
                        val notFollowedUsers = users.filter { it.userId !in followingList && it.userId != currentUserId }
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
}
