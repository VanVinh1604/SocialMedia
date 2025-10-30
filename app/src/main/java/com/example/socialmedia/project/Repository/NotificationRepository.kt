package com.example.socialmedia.project.data.repository

import com.example.socialmedia.project.Domain.Model.NotificationModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class NotificationRepository {

    private val database = FirebaseDatabase.getInstance().getReference("notifications")
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    fun observeNotifications(callback: (List<NotificationModel>) -> Unit) {
        if (userId == null) {
            callback(emptyList())
            return
        }

        database.orderByChild("userId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val rawList = mutableListOf<NotificationModel>()
                    for (child in snapshot.children) {
                        child.getValue(NotificationModel::class.java)?.let {
                            rawList.add(it)
                        }
                    }
                    // Sắp xếp theo thời gian mới nhất
                    val sortedList = rawList.sortedByDescending { it.createdAt }
                    callback(sortedList)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
    }

    fun sendNotificationMap(
        receiverId: String,
        actorId: String,
        actorName: String,
        actorAvatar: String? = "",
        targetId: String? = null,
        type: String,
        content: String
    ) {
        val notifRef = database
        val notifId = notifRef.push().key ?: return

        val data = mapOf(
            "notificationId" to notifId,
            "userId" to receiverId,
            "actorId" to actorId,
            "actorName" to actorName,
            "actorAvatar" to (actorAvatar ?: ""),
            "notificationType" to type,
            "targetId" to (targetId ?: ""),
            "content" to content,
            "createdAt" to System.currentTimeMillis(),
            "isRead" to false
        )

        notifRef.child(notifId).setValue(data)
    }

    fun sendLikeNotification(actorId: String, postOwnerId: String, postId: String) {
        if (actorId == postOwnerId) return

        val userRef = FirebaseDatabase.getInstance().getReference("InfoUser").child(actorId)
        userRef.get().addOnSuccessListener { snapshot ->
            val actorName = snapshot.child("fullName").getValue(String::class.java) ?: "Người dùng"
            val actorAvatar = snapshot.child("profilePictureUrl").getValue(String::class.java) ?: ""
            sendNotificationMap(
                receiverId = postOwnerId,
                actorId = actorId,
                actorName = actorName,
                actorAvatar = actorAvatar,
                targetId = postId,
                type = "LIKE",
                content = "đã thích bài viết của bạn"
            )
        }
    }

    fun sendCommentNotification(actorId: String, postOwnerId: String, postId: String) {
        if (actorId == postOwnerId) return

        val userRef = FirebaseDatabase.getInstance().getReference("InfoUser").child(actorId)
        userRef.get().addOnSuccessListener { snapshot ->
            val actorName = snapshot.child("fullName").getValue(String::class.java) ?: "Người dùng"
            val actorAvatar = snapshot.child("profilePictureUrl").getValue(String::class.java) ?: ""
            sendNotificationMap(
                receiverId = postOwnerId,
                actorId = actorId,
                actorName = actorName,
                actorAvatar = actorAvatar,
                targetId = postId,
                type = "COMMENT",
                content = "đã bình luận về bài viết của bạn"
            )
        }
    }

    // Có thể thêm các wrapper khác như FOLLOW, FOLLOW_REQUEST,... tương tự

    fun sendMentionNotification(actorId: String, mentionUserId: String, postId: String) {
        if (actorId == mentionUserId) return
        val userRef = FirebaseDatabase.getInstance().getReference("InfoUser").child(actorId)
        userRef.get().addOnSuccessListener { snapshot ->
            val actorName = snapshot.child("fullName").getValue(String::class.java) ?: "Người dùng"
            val actorAvatar = snapshot.child("profilePictureUrl").getValue(String::class.java) ?: ""
            sendNotificationMap(
                receiverId = mentionUserId,
                actorId = actorId,
                actorName = actorName,
                actorAvatar = actorAvatar,
                targetId = postId,
                type = "MENTION",
                content = "đã nhắc đến bạn trong bình luận"
            )
        }
    }

}


