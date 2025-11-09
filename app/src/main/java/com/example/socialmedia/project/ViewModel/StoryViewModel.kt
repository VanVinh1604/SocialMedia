package com.example.socialmedia.project.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmedia.project.Domain.Model.StoryModel
import com.example.socialmedia.project.Server.Firebase.FirebaseService
import com.example.socialmedia.project.data.repository.StoryRepository

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
            firebaseService.getUserFollowing(currentUserId, { followingList ->

                firebaseService.listenStories(onResult = { allStories ->

                    val finalList = mutableListOf<StoryModel>()

                    // ---- Step 1: Thêm nút Add Story ----
                    finalList.add(StoryModel(isAddStory = true))

                    // ---- Step 2: Story của user hiện tại ----
                    val userStories = allStories
                        .filter { it.userId == currentUserId && it.expiresAt > System.currentTimeMillis() }
                        .sortedBy { it.createdAt }

                    if (userStories.isNotEmpty()) {
                        // ✅ Lấy story đầu tiên nhưng gắn thêm số lượng story
                        val firstStory = userStories.first().copy(storyCount = userStories.size)
                        finalList.add(firstStory)
                    }

                    // ---- Step 3: Story của người user follow ----
                    val followedStoriesGrouped = allStories
                        .filter { it.userId in followingList && it.userId != currentUserId && it.expiresAt > System.currentTimeMillis() }
                        .groupBy { it.userId }

                    followedStoriesGrouped.forEach { (userId, stories) ->
                        // ✅ Lấy story đầu tiên, gắn số lượng
                        val firstStory = stories.minByOrNull { it.createdAt }!!
                        finalList.add(firstStory.copy(storyCount = stories.size))
                    }

                    // ---- Step 4: Gợi ý kết bạn ----
                    val notFollowedUsers = users.filter {
                        it.userId !in followingList && it.userId != currentUserId
                    }
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

                    _stories.value = finalList

                }, onError = { e -> _error.value = e.message })

            }, onError = { e -> _error.value = e.message })

        }, onError = { e -> _error.value = e.message })
    }

    fun loadUserStories(userId: String) {
        firebaseService.getStoriesByUserId(userId) { stories ->
            Log.d("StoryViewModel", "Loaded ${stories.size} stories for $userId")
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