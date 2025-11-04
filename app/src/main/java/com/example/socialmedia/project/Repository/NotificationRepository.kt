package com.example.socialmedia.project.data.repository

import com.example.socialmedia.project.Domain.Model.NotificationModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class NotificationRepository {

    private val database = FirebaseDatabase.getInstance().getReference("notifications")
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private var lastTimestampLoaded: Long? = null

    fun loadNotificationsPaged(
        limit: Int = 20,
        onResult: (List<NotificationModel>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (userId == null) {
            onResult(emptyList())
            return
        }

        val baseQuery = database.orderByChild("userId").equalTo(userId)

        baseQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allList = snapshot.children.mapNotNull { it.getValue(NotificationModel::class.java) }
                val sortedList = allList.sortedByDescending { it.createdAt }

                val now = System.currentTimeMillis()
                val todayStart = getStartOfDay(now)
                val dayMillis = 24 * 60 * 60 * 1000L

                val resultList = mutableListOf<NotificationModel>()
                var currentDay = todayStart

                // Lấy lần lượt từng ngày cho đến khi đủ limit
                while (resultList.size < limit && currentDay > 0) {
                    val dayStart = currentDay
                    val dayEnd = currentDay + dayMillis

                    val dayItems = sortedList.filter {
                        it.createdAt in dayStart..dayEnd
                    }

                    resultList.addAll(dayItems)

                    if (resultList.size >= limit) break
                    currentDay -= dayMillis // Lùi lại 1 ngày
                }

                // Lưu lại timestamp cuối cùng đã load
                lastTimestampLoaded = resultList.lastOrNull()?.createdAt
                onResult(resultList.take(limit))
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.toException())
            }
        })
    }

    /**
     * Load thêm các thông báo cũ hơn
     */
    fun loadMoreNotifications(
        limit: Int = 20,
        onResult: (List<NotificationModel>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (userId == null || lastTimestampLoaded == null) {
            onResult(emptyList())
            return
        }

        database.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val allList = snapshot.children.mapNotNull { it.getValue(NotificationModel::class.java) }
                    val sortedList = allList.sortedByDescending { it.createdAt }

                    val older = sortedList.filter { it.createdAt < lastTimestampLoaded!! }
                    val nextBatch = older.take(limit)

                    if (nextBatch.isNotEmpty()) {
                        lastTimestampLoaded = nextBatch.last().createdAt
                    }

                    onResult(nextBatch)
                }

                override fun onCancelled(error: DatabaseError) {
                    onError(error.toException())
                }
            })
    }

    fun observeNewNotifications(afterTimestamp: Long, callback: (List<NotificationModel>) -> Unit) {
        if (userId == null) {
            callback(emptyList())
            return
        }

        database.orderByChild("createdAt")
            .startAfter(afterTimestamp.toDouble()) // chỉ lấy mới hơn batch đầu
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val newList = snapshot.children.mapNotNull {
                        it.getValue(NotificationModel::class.java)
                    }.filter { it.userId == userId }
                        .sortedByDescending { it.createdAt }

                    if (newList.isNotEmpty()) callback(newList)
                }

                override fun onCancelled(error: DatabaseError) {}
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

    private fun getStartOfDay(timeMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

}


