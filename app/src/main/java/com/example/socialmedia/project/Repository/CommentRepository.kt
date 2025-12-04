package com.example.socialmedia.project.Repository

import com.example.socialmedia.project.Domain.Model.CommentModel
import com.example.socialmedia.project.data.repository.NotificationRepository
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CommentRepository {

    private val database = FirebaseDatabase.getInstance().reference
    private val notificationRepo = NotificationRepository()

    // 🔹 Lấy danh sách comment của post
    suspend fun getCommentsByPostId(postId: String): List<CommentModel> = withContext(Dispatchers.IO) {
        try {
            val snapshot = database.child("comments").child(postId).get().await()
            snapshot.children.mapNotNull { it.getValue(CommentModel::class.java) }
                .filter { it.parentCommentId == null } // chỉ lấy comment gốc
                .sortedBy { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 🔹 Thêm bình luận mới
    suspend fun addComment(postId: String, userId: String, postOwnerId: String, content: String) {
        val comment = CommentModel(
            userId = userId,
            commentableId = postId,
            content = content
        )

        val commentRef = database.child("comments").child(postId).child(comment.commentId)
        commentRef.setValue(comment).await()

        // Gửi notification cho chủ bài viết nếu không phải comment của chính mình
        if (userId != postOwnerId) {
            notificationRepo.sendCommentNotification(
                actorId = userId,
                postOwnerId = postOwnerId,
                postId = postId
            )
        }

        // Kiểm tra và gửi thông báo nếu có @mention
        detectMentionsAndNotify(userId, postId, content)
    }


    // 🔹 Thêm reply vào comment
    suspend fun addReply(
        postId: String,
        rootCommentId: String, // luôn là comment cha gốc
        userId: String,
        content: String,
        mentionUserId: String? = null // người bị tag (nếu có)
    ) {
        val reply = CommentModel(
            userId = userId,
            commentableId = postId,
            parentCommentId = rootCommentId,
            content = content
        )

        val replyRef = database.child("comments")
            .child(postId)
            .child(rootCommentId)
            .child("replies")
            .child(reply.commentId)

        replyRef.setValue(reply).await()

        // 🔹 Nếu có mentionUserId thì gửi thông báo cho người đó
        if (!mentionUserId.isNullOrEmpty() && mentionUserId != userId) {
            notificationRepo.sendMentionNotification(
                actorId = userId,
                mentionUserId = mentionUserId,
                postId = postId
            )
        }

        // 🔹 Nếu không có mentionUserId, gửi thông báo cho chủ comment cha
        if (mentionUserId.isNullOrEmpty()) {
            FirebaseDatabase.getInstance().getReference("comments")
                .child(postId)
                .child(rootCommentId)
                .child("userId")
                .get()
                .addOnSuccessListener { snapshot ->
                    val parentUserId = snapshot.getValue(String::class.java)
                    if (parentUserId != null && parentUserId != userId) {
                        notificationRepo.sendMentionNotification(
                            actorId = userId,
                            mentionUserId = parentUserId,
                            postId = postId
                        )
                    }
                }
        }

        detectMentionsAndNotify(userId, postId, content)
    }


    // 🔹 Theo dõi danh sách comment realtime
    fun listenCommentsByPostId(postId: String, onResult: (List<CommentModel>) -> Unit) {
        val ref = database.child("comments").child(postId)
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val comments = snapshot.children.mapNotNull { it.getValue(CommentModel::class.java) }
                    .filter { it.parentCommentId == null }
                    .sortedBy { it.createdAt }
                onResult(comments)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 🔹 Tự động phát hiện @mention và gửi thông báo
    private fun detectMentionsAndNotify(actorId: String, postId: String, content: String) {
        val mentionRegex = "@([A-Za-z0-9_À-ỹ]+)".toRegex()
        val foundMentions = mentionRegex.findAll(content).map { it.groupValues[1] }.toList()

        if (foundMentions.isEmpty()) return

        val userRef = FirebaseDatabase.getInstance().getReference("InfoUser")
        for (username in foundMentions) {
            userRef.orderByChild("fullName").equalTo(username)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (child in snapshot.children) {
                            val mentionedUserId = child.key ?: continue
                            if (mentionedUserId != actorId) {
                                notificationRepo.sendMentionNotification(
                                    actorId = actorId,
                                    mentionUserId = mentionedUserId,
                                    postId = postId
                                )
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }
}
