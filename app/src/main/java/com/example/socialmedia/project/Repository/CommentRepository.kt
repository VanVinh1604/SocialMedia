package com.example.socialmedia.project.Repository

import com.example.socialmedia.project.Domain.Model.CommentModel
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CommentRepository {

    private val database = FirebaseDatabase.getInstance().reference

    suspend fun getCommentsByPostId(postId: String): List<CommentModel> = withContext(Dispatchers.IO) {
        try {
            val snapshot = database.child("comments").child(postId).get().await()
            snapshot.children.mapNotNull { it.getValue(CommentModel::class.java) }
                .sortedBy { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addComment(postId: String, userId: String, content: String) = withContext(Dispatchers.IO) {
        val comment = CommentModel(
            userId = userId,
            commentableId = postId,
            content = content
        )
        database.child("comments").child(postId).child(comment.commentId).setValue(comment).await()
    }
}
