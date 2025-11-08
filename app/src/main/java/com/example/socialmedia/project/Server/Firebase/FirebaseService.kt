package com.example.socialmedia.project.Server.Firebase

import com.example.socialmedia.project.Domain.Model.MessageModel
import com.example.socialmedia.project.Domain.Model.StoryModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.*

class FirebaseService {

    val database = FirebaseDatabase.getInstance().reference

    fun listenStories(onResult: (List<StoryModel>) -> Unit, onError: (Exception) -> Unit) {
        val storiesRef = database.child("stories")
        storiesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allStories = mutableListOf<StoryModel>()
                val tasks = mutableListOf<Task<DataSnapshot>>()
                val storiesWithUser = mutableListOf<StoryModel>()

                // Lấy tất cả story
                for (child in snapshot.children) {
                    val story = child.getValue(StoryModel::class.java)
                    if (story != null) allStories.add(story)
                }

                if (allStories.isEmpty()) {
                    onResult(emptyList())
                    return
                }

                // Lấy thông tin user cho từng story
                allStories.forEach { story ->
                    val userTask = database.child("InfoUser").child(story.userId).get()
                    tasks.add(userTask)
                    userTask.addOnSuccessListener { userSnap ->
                        val userName = userSnap.child("fullName").getValue(String::class.java) ?: "Unknown"
                        val userAvatar = userSnap.child("profilePictureUrl").getValue(String::class.java) ?: ""
                        storiesWithUser.add(story.copy(userName = userName, userProfileImage = userAvatar))
                    }
                }

                // Khi tất cả task hoàn tất
                Tasks.whenAllComplete(tasks).addOnSuccessListener {
                    // Sắp xếp theo thời gian tạo story (từ cũ đến mới)
                    onResult(storiesWithUser.sortedBy { it.createdAt })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.toException())
            }
        })
    }


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

    fun getUserStatus(userId: String, callback: (exists: Boolean, isBanned: Boolean) -> Unit) {
        val infoRef = database.child("InfoUser").child(userId)
        infoRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val isBanned = snapshot.child("isBanned").getValue(Boolean::class.java) ?: false
                callback(true, isBanned)
            } else {
                callback(false, false) // Không tồn tại
            }
        }.addOnFailureListener {
            callback(false, false)
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
                "userId" to targetUserId,        // Người nhận thông báo
                "actorId" to actorId,            // ID người thực hiện hành động
                "actorName" to actorName,        // ✅ Lưu thêm tên
                "actorAvatar" to actorAvatar,    // ✅ Lưu thêm ảnh đại diện
                "notificationType" to "FOLLOW",
                "content" to "đã bắt đầu theo dõi bạn",
                "createdAt" to System.currentTimeMillis(),
                "isRead" to false
            )

            notifRef.child(notifId).setValue(data)
        }
    }

    // Thêm vào FirebaseService.kt
    fun uploadStoryToFirebase(
        userId: String,
        mediaUrl: String,
        type: String, // "image" hoặc "video"
        onComplete: (success: Boolean) -> Unit
    ) {
        val storyRef = database.child("stories").push()
        val storyId = storyRef.key ?: return onComplete(false)
        val storyData = mapOf(
            "storyId" to storyId,
            "userId" to userId,
            "mediaUrl" to mediaUrl,
            "type" to type,
            "createdAt" to System.currentTimeMillis()
        )

        storyRef.setValue(storyData)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
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


}
