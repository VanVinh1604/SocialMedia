package com.example.socialmedia.project.Server.Firebase

import android.util.Log
import com.example.socialmedia.project.Domain.Model.MessageModel
import com.example.socialmedia.project.Domain.Model.StoryModel
import com.example.socialmedia.project.Domain.Model.StoryViewerItem
import com.example.socialmedia.project.Domain.Model.UserModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseService {

    val database = FirebaseDatabase.getInstance().reference
    val firestore = FirebaseFirestore.getInstance()
    private val viewCountListenerMap = mutableMapOf<String, ValueEventListener>()

    fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }


    fun listenStories(onResult: (List<StoryModel>) -> Unit, onError: (Exception) -> Unit) {
        val currentUserId = getCurrentUserId()
        val storiesRef = database.child("stories")
        storiesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allStories = mutableListOf<StoryModel>()
                val tasks = mutableListOf<Task<DataSnapshot>>()
                val storiesWithUser = mutableListOf<StoryModel>()
                val now = System.currentTimeMillis()

                for (child in snapshot.children) {
                    val story = child.getValue(StoryModel::class.java) ?: continue
                    val now = System.currentTimeMillis()

                    val hiddenByMap = child.child("hiddenBy").value as? Map<String, Boolean>
                    if (currentUserId != null && hiddenByMap?.containsKey(currentUserId) == true) continue
                    // ✅ Nếu story hết hạn → cập nhật Firebase và bỏ qua
                    if (story.expiresAt <= now) {
                        if (story.isExpired != true) {
                            database.child("stories")
                                .child(story.storyId)
                                .child("isExpired")
                                .setValue(true)
                        }
                        continue // ⛔ Bỏ qua story đã hết hạn
                    }

                    // ✅ Nếu story hợp lệ → thêm vào danh sách
                    allStories.add(story)
                }


                if (allStories.isEmpty()) {
                    onResult(emptyList())
                    return
                }

                // Lấy thông tin user cho từng story còn hạn
                allStories.filter { it.expiresAt > now && it.isExpired == false }
                    .forEach { story ->
                        val userTask = database.child("InfoUser").child(story.userId).get()
                        tasks.add(userTask)
                        userTask.addOnSuccessListener { userSnap ->
                            val userName = userSnap.child("fullName").getValue(String::class.java) ?: "Unknown"
                            val userAvatar = userSnap.child("profilePictureUrl").getValue(String::class.java) ?: ""
                            storiesWithUser.add(story.copy(userName = userName, userProfileImage = userAvatar))
                        }
                    }

                Tasks.whenAllComplete(tasks).addOnSuccessListener {
                    // Sắp xếp story theo thời gian tạo
                    onResult(storiesWithUser.sortedBy { it.createdAt })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.toException())
            }
        })
    }


//    fun listenStoriesByFollowedUsers(
//        currentUserId: String,
//        onResult: (List<StoryModel>) -> Unit,
//        onError: (Exception) -> Unit
//    ) {
//        // Lấy danh sách user mà currentUser đang follow
//        getUserFollowing(currentUserId, { followingList ->
//
//            // Lắng nghe tất cả story
//            val storiesRef = database.child("stories")
//            storiesRef.addValueEventListener(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    val allStories = mutableListOf<StoryModel>()
//
//                    for (child in snapshot.children) {
//                        val story = child.getValue(StoryModel::class.java)
//                        if (story != null) {
//                            // Chỉ lấy story mà user đang follow và chưa quá 24h
//                            if (story.userId in followingList && story.expiresAt > System.currentTimeMillis()) {
//                                allStories.add(story)
//                            }
//                        }
//                    }
//
//                    // Lấy story cũ nhất mỗi user
//                    val firstStoryPerUser = allStories
//                        .groupBy { it.userId }
//                        .map { (_, stories) ->
//                            stories.minByOrNull { it.createdAt }!!
//                        }
//
//                    onResult(firstStoryPerUser)
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    onError(error.toException())
//                }
//            })
//        }, onError)
//    }


    // -------------------------------
// FirebaseService.kt
    fun getMutualFollowUsers(
        currentUserId: String,
        onResult: (List<String>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val followRef = database.child("Follow")

        followRef.child(currentUserId).child("following").get().addOnSuccessListener { followingSnap ->
            followRef.child(currentUserId).child("followers").get().addOnSuccessListener { followersSnap ->
                val following = followingSnap.children.mapNotNull { it.key }
                val followers = followersSnap.children.mapNotNull { it.key }
                val mutual = following.intersect(followers.toSet()).toList()
                onResult(mutual)
            }
        }.addOnFailureListener { onError(it) }
    }

    fun listenMessages(
        conversationId: String,
        onSuccess: (List<MessageModel>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val ref = database.child("messages").child(conversationId)
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = mutableListOf<Task<DataSnapshot>>()
                val messageList = mutableListOf<MessageModel>()

                for (child in snapshot.children) {
                    val map = child.value as? Map<String, Any> ?: continue
                    val senderId = map["senderId"] as? String ?: continue

                    val task = database.child("InfoUser").child(senderId).get()
                    tasks.add(task)
                    task.addOnSuccessListener { userSnap ->
                        val senderName = userSnap.child("fullName").getValue(String::class.java) ?: "Người dùng"
                        val senderAvatar = userSnap.child("profilePictureUrl").getValue(String::class.java)
                        val msg = MessageModel(
                            messageId = map["id"] as? String ?: "",
                            conversationId = conversationId,
                            senderId = senderId,
                            senderName = senderName,
                            senderAvatar = senderAvatar,
                            content = map["text"] as? String ?: "",
                            createdAt = map["timestamp"] as? Long ?: System.currentTimeMillis()
                        )
                        messageList.add(msg)
                    }
                }

                Tasks.whenAllComplete(tasks).addOnSuccessListener {
                    // Sắp xếp theo thời gian
                    onSuccess(messageList.sortedBy { it.createdAt })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onFailure(error.toException())
            }
        })
    }


    fun sendMessage(
        conversationId: String,
        senderId: String,
        text: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val ref = database.child("messages").child(conversationId).push()
        val message = mapOf(
            "id" to ref.key,
            "senderId" to senderId,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )
        ref.setValue(message)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }


    // Lắng nghe users realtime
    fun listenUsers(onResult: (List<UserModel>) -> Unit, onError: (Exception) -> Unit) {
        val usersRef = database.child("InfoUser")
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<UserModel>()
                for (child in snapshot.children) {
                    val user = child.getValue(UserModel::class.java)
                    user?.let { list.add(it) }
                }
                onResult(list)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.toException())
            }
        })
    }


    fun getUserFollowing(
        currentUserId: String,
        onResult: (List<String>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val ref = database.child("Follow").child(currentUserId).child("following")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.key }
                onResult(list)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.toException())
            }
        })
    }

    fun followUser(
        currentUserId: String,
        targetUserId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val followRef = database.child("Follow")

        val updates = hashMapOf<String, Any>(
            "$currentUserId/following/$targetUserId" to true,
            "$targetUserId/followers/$currentUserId" to true
        )

        followRef.updateChildren(updates)
            .addOnSuccessListener {
                // ✅ Cập nhật lại đếm followers/following
                updateUserFollowCount(currentUserId, targetUserId, {
                    // ✅ Sau khi follow thành công → gửi thông báo
                    sendFollowNotification(currentUserId, targetUserId)
                    onSuccess()
                }, onError)
            }
            .addOnFailureListener { onError(it) }
    }

    fun isUserFollowing(
        currentUserId: String,
        targetUserId: String,
        callback: (Boolean) -> Unit
    ) {
        val followRef = database.child("Follow")
            .child(currentUserId)
            .child("following")
            .child(targetUserId)

        followRef.get()
            .addOnSuccessListener { snapshot ->
                callback(snapshot.exists()) // ✅ true nếu đã theo dõi
            }
            .addOnFailureListener {
                callback(false)
            }
    }


    fun unfollowUser(
        currentUserId: String,
        targetUserId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val followRef = database.child("Follow")

        val updates = hashMapOf<String, Any?>(
            "$currentUserId/following/$targetUserId" to null,
            "$targetUserId/followers/$currentUserId" to null
        )

        followRef.updateChildren(updates)
            .addOnSuccessListener {
                // ✅ Cập nhật lại số followers/following
                updateUserFollowCount(currentUserId, targetUserId, onSuccess, onError)
            }
            .addOnFailureListener { onError(it) }
    }

    private fun updateUserFollowCount(
        currentUserId: String,
        targetUserId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userRef = database.child("InfoUser")

        // Kiểm tra xem đang follow hay unfollow
        val followCheckRef = database.child("Follow")
            .child(currentUserId)
            .child("following")
            .child(targetUserId)

        followCheckRef.get().addOnSuccessListener { snapshot ->
            val isFollowing = snapshot.exists()
            val delta = if (isFollowing) 1 else -1 // ✅ follow thì +1, unfollow thì -1

            // Cập nhật followingCount
            userRef.child(currentUserId).child("followingCount")
                .runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val currentCount = currentData.getValue(Int::class.java) ?: 0
                        currentData.value = (currentCount + delta).coerceAtLeast(0)
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                        if (error != null) onError(error.toException())
                    }
                })

            // Cập nhật followerCount
            userRef.child(targetUserId).child("followerCount")
                .runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val currentCount = currentData.getValue(Int::class.java) ?: 0
                        currentData.value = (currentCount + delta).coerceAtLeast(0)
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                        if (error == null) onSuccess() else onError(error.toException())
                    }
                })
        }.addOnFailureListener { onError(it) }
    }

    private fun sendFollowNotification(actorId: String, targetUserId: String) {
        val notifRef = database.child("notifications")
        val notifId = notifRef.push().key ?: return

        // Lấy tên & avatar của người gửi (actor)
        val userRef = database.child("InfoUser").child(actorId)
        userRef.get().addOnSuccessListener { snapshot ->
            val actorName = snapshot.child("fullName").getValue(String::class.java) ?: "Người dùng"
            val actorAvatar = snapshot.child("profilePictureUrl").getValue(String::class.java) ?: ""

            val data = mapOf(
                "id" to notifId,
                "userId" to targetUserId,
                "actorId" to actorId,
                "actorName" to actorName,
                "actorAvatar" to actorAvatar,
                "notificationType" to "FOLLOW",
                "content" to "đã bắt đầu theo dõi bạn",
                "createdAt" to System.currentTimeMillis(),
                "isRead" to false
            )

            notifRef.child(notifId).setValue(data)
        }
    }


    fun hasUserViewedAllStories(
        userId: String,
        viewerId: String,
        onResult: (allViewed: Boolean) -> Unit
    ) {
        getStoriesByUserId(userId) { stories ->
            if (stories.isEmpty()) {
                onResult(true) // Không có story → coi như đã xem hết
                return@getStoriesByUserId
            }

            val tasks = stories.map { story ->
                database.child("stories").child(story.storyId)
                    .child("views").child(viewerId).get()
            }

            Tasks.whenAllComplete(tasks).addOnSuccessListener { results ->
                val allViewed = results.all { task ->
                    val snapshot = task.result as? DataSnapshot
                    snapshot?.getValue(Boolean::class.java) == true
                }
                onResult(allViewed)
            }
        }
    }

    fun markStoryAsViewed(storyId: String, viewerId: String) {
        val storyRef = FirebaseDatabase.getInstance().getReference("stories").child(storyId)

        // Ghi người xem vào node views
        storyRef.child("views").child(viewerId).setValue(true)
            .addOnSuccessListener {
                // Cập nhật lại viewCount = số lượng views hiện tại
                storyRef.child("views").get().addOnSuccessListener { snapshot ->
                    val count = snapshot.childrenCount
                    storyRef.child("viewCount").setValue(count)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseService", "❌ markStoryAsViewed lỗi: ${e.message}")
            }
    }

    fun listenStoryViewCount(storyId: String, onCountChanged: (Int) -> Unit) {
        val ref = FirebaseDatabase.getInstance()
            .getReference("stories")
            .child(storyId)
            .child("views")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount.toInt()
                onCountChanged(count)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }



    // Thêm vào FirebaseService.kt
    // Kiểm tra user trước khi thao tác
    private fun canModifyStory(userId: String, storyId: String, callback: (canModify: Boolean) -> Unit) {
        val currentUserId = getCurrentUserId() ?: run {
            callback(false)
            return
        }

        database.child("stories").child(storyId).child("userId").get()
            .addOnSuccessListener { snapshot ->
                val ownerId = snapshot.getValue(String::class.java)
                callback(ownerId == currentUserId)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    // Xóa story với kiểm tra user
    fun deleteStory(storyId: String, onComplete: (success: Boolean) -> Unit = {}) {
        canModifyStory(getCurrentUserId() ?: "", storyId) { canModify ->
            if (!canModify) {
                Log.e("FirebaseService", "❌ Không có quyền xóa story này")
                onComplete(false)
                return@canModifyStory
            }

            database.child("stories").child(storyId).removeValue()
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { e ->
                    Log.e("FirebaseService", "❌ deleteStory lỗi: ${e.message}")
                    onComplete(false)
                }
        }
    }

    // Lưu trữ (archive) story với kiểm tra user
    fun archiveStory(storyId: String, onComplete: (success: Boolean) -> Unit = {}) {
        canModifyStory(getCurrentUserId() ?: "", storyId) { canModify ->
            if (!canModify) {
                Log.e("FirebaseService", "❌ Không có quyền lưu/ẩn story này")
                onComplete(false)
                return@canModifyStory
            }

            database.child("stories").child(storyId).child("isArchived").setValue(true)
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { e ->
                    Log.e("FirebaseService", "❌ archiveStory lỗi: ${e.message}")
                    onComplete(false)
                }
        }
    }

    // Upload story cũng nên kiểm tra userId trước khi push
//    fun uploadStoryToFirebase(
//        userId: String,
//        mediaUrl: String,
//        isVideo: Boolean,
//        duration: Long? = null,
//        thumbnailUrl: String? = null,
//        saveToDatabase: Boolean = true,
//        onComplete: (success: Boolean) -> Unit
//    ) {
//        if (getCurrentUserId() != userId) {
//            Log.e("FirebaseService", "❌ Không có quyền upload story cho user khác")
//            onComplete(false)
//            return
//        }
//
//        if (!saveToDatabase) {
//            onComplete(true)
//            return
//        }
//
//        val storyRef = database.child("stories").push()
//        val storyId = storyRef.key ?: return onComplete(false)
//        val storyType = if (isVideo) "video" else "image"
//
//        val storyData = mutableMapOf(
//            "storyId" to storyId,
//            "userId" to userId,
//            "mediaUrl" to mediaUrl,
//            "type" to storyType,
//            "createdAt" to System.currentTimeMillis(),
//            "expiresAt" to (System.currentTimeMillis() + 24 * 60 * 60 * 1000),
//            "viewCount" to 0,
//            "views" to mapOf<String, Boolean>(),
//            "isExpired" to false
//        )
//
//        if (isVideo) {
//            thumbnailUrl?.let { storyData["thumbnailUrl"] = it }
//            duration?.let { storyData["duration"] = it }
//        }
//
//        storyRef.setValue(storyData)
//            .addOnSuccessListener { onComplete(true) }
//            .addOnFailureListener { onComplete(false) }
//    }

    fun uploadStoryToFirebase(
        userId: String,
        mediaUrl: String,
        isVideo: Boolean,
        duration: Long? = null,
        thumbnailUrl: String? = null,
        saveToDatabase: Boolean = true,
        onComplete: (success: Boolean) -> Unit
    ) {
        if (getCurrentUserId() != userId) {
            Log.e("FirebaseService", "❌ Không có quyền upload story cho user khác")
            onComplete(false)
            return
        }

        val storyRef = database.child("stories").push()
        val storyId = storyRef.key ?: return onComplete(false)
        val storyType = if (isVideo) "video" else "image"

        val storyData = mutableMapOf(
            "storyId" to storyId,
            "userId" to userId,
            "mediaUrl" to mediaUrl,
            "type" to storyType,
            "createdAt" to System.currentTimeMillis(),
            "expiresAt" to (System.currentTimeMillis() + 24 * 60 * 60 * 1000),
            "viewCount" to 0,
            "views" to mapOf<String, Boolean>(),
            "isExpired" to false
        )

        if (isVideo) {
            thumbnailUrl?.let { storyData["thumbnailUrl"] = it }
            duration?.let { storyData["duration"] = it }
        }

        storyRef.setValue(storyData)
            .addOnSuccessListener {
                syncStoryToFirestore(storyId) // ✅ đồng bộ sang Firestore ngay sau khi upload
                onComplete(true)
            }
            .addOnFailureListener { onComplete(false) }
    }


    private fun syncStoryToFirestore(storyId: String) {
        val ref = database.child("stories").child(storyId)
        ref.get().addOnSuccessListener { snapshot ->
            val story = snapshot.getValue(StoryModel::class.java) ?: return@addOnSuccessListener
            val storyData = hashMapOf<String, Any>(
                "storyId" to story.storyId,
                "userId" to story.userId,
                "mediaUrl" to story.mediaUrl,
                "type" to story.mediaType,
                "createdAt" to story.createdAt,
                "expiresAt" to story.expiresAt,
//                "viewCount" to snapshot.child("viewCount").getValue(Long::class.java) ?: 0,
//                "views" to snapshot.child("views").value as? Map<String, Boolean> ?: emptyMap<String, Boolean>(),
                "isExpired" to story.isExpired
            )

            firestore.collection("stories").document(story.storyId)
                .set(storyData)
        }
    }


    fun syncAllStoriesToFirestore() {
        database.child("stories").get().addOnSuccessListener { snapshot ->
            for (child in snapshot.children) {
                val storyId = child.key ?: continue
                syncStoryToFirestore(storyId)
            }
        }
    }


    fun getStoriesByUserId(userId: String, onResult: (List<StoryModel>) -> Unit) {
        val ref = database.child("stories").orderByChild("userId").equalTo(userId)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<StoryModel>()
                for (child in snapshot.children) {
                    val story = child.getValue(StoryModel::class.java)
                    if (story != null) list.add(story)
                }
                onResult(list.sortedBy { it.createdAt }) // từ cũ đến mới
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun getUserById(userId: String, callback: (UserModel) -> Unit) {
        database.child("InfoUser").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(UserModel::class.java)
                if (user != null) callback(user)
            }
            .addOnFailureListener {
                // nếu lỗi, gửi user mặc định
                callback(UserModel(userId = userId, fullName = "Người dùng", profilePictureUrl = ""))
            }
    }

    fun listenStoryViews(
        storyId: String,
        onViewsChanged: (Map<String, Boolean>) -> Unit
    ) {
        val ref = database.child("stories").child(storyId).child("views")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val views = mutableMapOf<String, Boolean>()
                for (child in snapshot.children) {
                    val uid = child.key ?: continue
                    val liked = child.getValue(Boolean::class.java) ?: false
                    views[uid] = liked
                }
                onViewsChanged(views)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun toggleStoryLike(storyId: String, userId: String, onComplete: (Boolean) -> Unit) {
        val likeRef = database.child("stories").child(storyId).child("likes").child(userId)

        likeRef.get().addOnSuccessListener { snapshot ->
            val currentlyLiked = snapshot.exists()
            if (currentlyLiked) {
                // Người dùng đã like → giữ nguyên, không được unlike
                onComplete(true)
            } else {
                // Người dùng chưa like → set true
                likeRef.setValue(true).addOnSuccessListener {
                    onComplete(true)
                }.addOnFailureListener {
                    onComplete(false)
                }
            }
        }.addOnFailureListener {
            onComplete(false)
        }
    }





}
