package com.example.socialmedia.project.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmedia.project.Domain.Model.StoryModel
import com.example.socialmedia.project.Domain.Model.StoryViewerItem
import com.example.socialmedia.project.Server.Firebase.FirebaseService
import com.example.socialmedia.project.data.repository.StoryRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

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

                    // --- Thay phần sắp xếp cũ bằng đoạn này ---
                    val sortedList = finalList.sortedWith(
                        compareByDescending<StoryModel> { it.isAddStory }   // Add Story (true) ở đầu
                            .thenBy { it.isSuggestFriend }                  // Suggest friend (true) sẽ ở cuối
                            .thenBy { it.isViewed }                         // chưa xem (false) trước đã xem (true)
                    )

                    _stories.value = sortedList


                }, onError = { e -> _error.value = e.message }) 

            }, onError = { e -> _error.value = e.message })

        }, onError = { e -> _error.value = e.message })
    }

    fun loadUserStories(userId: String) {
        firebaseService.getStoriesByUserId(userId) { stories ->

            val now = System.currentTimeMillis()

            // 🔹 Lọc bỏ story hết hạn (sau 24h hoặc expiresAt < now)
            val validStories = stories.filter { story ->
                val createdAt = story.createdAt
                val diff = now - createdAt
                val hoursPassed = diff / (1000 * 60 * 60)
                val notExpired = (hoursPassed < 24) && (story.expiresAt > now)
                notExpired
            }

            Log.d(
                "StoryViewModel",
                "✅ Loaded ${validStories.size}/${stories.size} hợp lệ cho user $userId"
            )

            firebaseService.getUserById(userId) { user ->
                val storiesWithUser = validStories.map { story ->
                    story.copy(
                        userName = user.fullName,
                        userProfileImage = user.profilePictureUrl ?: ""
                    )
                }
                _stories.postValue(storiesWithUser)
            }
        }
    }

    fun observeStoryViews(story: StoryModel, onUpdate: (List<StoryViewerItem>) -> Unit) {
        // Lắng nghe realtime map "views" trong Firebase: chỉ lưu userId -> hasLiked
        firebaseService.listenStoryViews(story.storyId) { viewsMap ->
            val viewers = mutableListOf<StoryViewerItem>()
            val tasks = mutableListOf<com.google.android.gms.tasks.Task<*>>()

            viewsMap.forEach { (uid, _) ->
                // Lấy thông tin user
                val task = firebaseService.database.child("InfoUser").child(uid).get()
                    .addOnSuccessListener { snapshot ->
                        val name = snapshot.child("fullName").getValue(String::class.java) ?: "Người dùng"
                        val avatar = snapshot.child("profilePictureUrl").getValue(String::class.java)
                        val hasLiked = story.userLikes[uid] == true

                        viewers.add(
                            StoryViewerItem(
                                userId = uid,
                                userName = name,
                                userAvatar = avatar,
                                hasLiked = hasLiked // ✅ Chỉ true nếu thật sự đã like
                            )
                        )
                    }
                tasks.add(task)
            }

            // Khi tất cả task hoàn thành, trả về danh sách viewers
            com.google.android.gms.tasks.Tasks.whenAllComplete(tasks).addOnSuccessListener {
                onUpdate(viewers.sortedBy { it.userName }) // có thể sắp xếp tùy ý
            }
        }
    }


    fun observeStoryViewsAndLikesRealtime(
        story: StoryModel,
        onUpdate: (List<StoryViewerItem>) -> Unit
    ) {
        val viewersMap = mutableMapOf<String, StoryViewerItem>()

        // Lắng nghe views realtime
        val viewsListener = firebaseService.database.child("stories")
            .child(story.storyId).child("views")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { child ->
                        val uid = child.key ?: return@forEach
                        val existing = viewersMap[uid]
                        viewersMap[uid] = existing?.copy() ?: StoryViewerItem(
                            userId = uid,
                            userName = "",
                            userAvatar = null,
                            hasLiked = story.userLikes[uid] == true
                        )
                    }
                    // Lấy info user từ Firebase
                    loadUserInfoForViewers(viewersMap, onUpdate)
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        // Lắng nghe likes realtime
        val likesListener = firebaseService.database.child("stories")
            .child(story.storyId).child("likes")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val likedUsers = snapshot.children.mapNotNull { it.key }.toSet()
                    likedUsers.forEach { uid ->
                        val existing = viewersMap[uid]
                        viewersMap[uid] = existing?.copy(hasLiked = true)
                            ?: StoryViewerItem(
                                userId = uid,
                                userName = "null",
                                userAvatar = null,
                                hasLiked = true
                            )
                    }
                    // Cập nhật lại danh sách viewers
                    loadUserInfoForViewers(viewersMap, onUpdate)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // Hàm load thông tin user (name/avatar) nếu chưa có
    private fun loadUserInfoForViewers(
        viewersMap: MutableMap<String, StoryViewerItem>,
        onUpdate: (List<StoryViewerItem>) -> Unit
    ) {
        val tasks = mutableListOf<com.google.android.gms.tasks.Task<*>>()

        viewersMap.forEach { (uid, viewer) ->
            if (viewer.userName == null || viewer.userAvatar == null) {
                val task = FirebaseService().database.child("InfoUser").child(uid).get()
                    .addOnSuccessListener { snapshot ->
                        val name = snapshot.child("fullName").getValue(String::class.java) ?: "Người dùng"
                        val avatar = snapshot.child("profilePictureUrl").getValue(String::class.java)
                        viewersMap[uid] = viewer.copy(userName = name, userAvatar = avatar)
                    }
                tasks.add(task)
            }
        }

        // Khi tất cả task hoàn tất
        if (tasks.isEmpty()) {
            onUpdate(viewersMap.values.sortedBy { it.userName })
        } else {
            com.google.android.gms.tasks.Tasks.whenAllComplete(tasks).addOnSuccessListener {
                onUpdate(viewersMap.values.sortedBy { it.userName })
            }
        }
    }



}