package com.example.socialmedia.project.Server.Firebase

import com.example.socialmedia.project.Domain.Model.StoryModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.*

class FirebaseService {

    private val database = FirebaseDatabase.getInstance().reference

    fun listenStories(onResult: (List<StoryModel>) -> Unit, onError: (Exception) -> Unit) {
        val storiesRef = database.child("stories")
        storiesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val storyList = mutableListOf<StoryModel>()
                val tasks = mutableListOf<Task<DataSnapshot>>()

                for (child in snapshot.children) {
                    val story = child.getValue(StoryModel::class.java)
                    if (story != null) {
                        val userTask = database.child("InfoUser").child(story.userId).get()
                        tasks.add(userTask)
                        userTask.addOnSuccessListener { userSnap ->
                            val userName = userSnap.child("fullName").getValue(String::class.java) ?: "Unknown"
                            val userAvatar = userSnap.child("userProfileImage").getValue(String::class.java) ?: ""
                            val merged = story.copy(
                                userName = userName,
                                userProfileImage = userAvatar
                            )
                            storyList.add(merged)
                        }
                    }
                }

                // Khi tất cả user info load xong → trả về list
                Tasks.whenAllComplete(tasks).addOnSuccessListener {
                    onResult(storyList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.toException())
            }
        })
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


}
