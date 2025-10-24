package com.example.socialmedia.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.example.socialmedia.project.Domain.Model.UserModel
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class Post(
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val caption: String = "",
    val hashtags: String = "",
    val imageUrls: List<String> = emptyList(),
    val music: String? = null,
    val taggedPeople: List<String> = emptyList(),
    val location: String? = null,
    val audience: String = "Công khai",
    val commentsDisabled: Boolean = false,
    val likesHidden: Boolean = false,
    val timestamp: Any = ServerValue.TIMESTAMP,
    val likes: Int = 0,
    val comments: Int = 0,
    val shares: Int = 0
)

object FirebaseHelper {

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getCurrentUserInfo(): Triple<String, String, String>? {
        val currentUser = auth.currentUser ?: return null
        val userId = currentUser.uid

        return try {
            val snapshot = database.reference
                .child("users")
                .child(userId)
                .get()
                .await()

            if (snapshot.exists()) {
                val userModel = snapshot.getValue(UserModel::class.java)
                if (userModel != null) {
                    Triple(userId, userModel.fullName, userModel.profilePictureUrl ?: "")
                } else {
                    val fullName = snapshot.child("fullName").getValue(String::class.java)
                        ?: "${snapshot.child("firstName").getValue(String::class.java) ?: ""} ${snapshot.child("lastName").getValue(String::class.java) ?: ""}".trim()
                        ?: currentUser.displayName ?: "Unknown User"
                    val avatar = snapshot.child("profilePictureUrl").getValue(String::class.java)
                        ?: currentUser.photoUrl?.toString() ?: ""
                    Triple(userId, fullName, avatar)
                }
            } else {
                Triple(userId, currentUser.displayName ?: "Unknown User", currentUser.photoUrl?.toString() ?: "")
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseHelper", "Error getting user info: ${e.message}", e)
            Triple(userId, currentUser.displayName ?: "Unknown User", currentUser.photoUrl?.toString() ?: "")
        }
    }

    suspend fun getCurrentUser(): UserModel? {
        val currentUser = auth.currentUser ?: return null
        val userId = currentUser.uid

        return try {
            val snapshot = database.reference
                .child("users")
                .child(userId)
                .get()
                .await()
            if (snapshot.exists()) snapshot.getValue(UserModel::class.java) else null
        } catch (e: Exception) {
            android.util.Log.e("FirebaseHelper", "Error getting user model: ${e.message}", e)
            null
        }
    }

    suspend fun savePost(post: Post): String {
        val postId = database.reference.child("posts").push().key ?: UUID.randomUUID().toString()
        val postData = post.copy(postId = postId, timestamp = ServerValue.TIMESTAMP)

        val postMap = hashMapOf<String, Any?>(
            "postId" to postData.postId,
            "userId" to postData.userId,
            "userName" to postData.userName,
            "userAvatar" to postData.userAvatar,
            "caption" to postData.caption,
            "hashtags" to postData.hashtags,
            "imageUrls" to postData.imageUrls,
            "music" to postData.music,
            "taggedPeople" to postData.taggedPeople,
            "location" to postData.location,
            "audience" to postData.audience,
            "commentsDisabled" to postData.commentsDisabled,
            "likesHidden" to postData.likesHidden,
            "timestamp" to ServerValue.TIMESTAMP,
            "likes" to postData.likes,
            "comments" to postData.comments,
            "shares" to postData.shares
        )

        return try {
            database.reference.child("posts").child(postId).setValue(postMap).await()
            database.reference.child("user-posts").child(postData.userId).child(postId).setValue(true).await()
            android.util.Log.d("FirebaseHelper", "Post saved successfully: $postId")
            postId
        } catch (e: Exception) {
            android.util.Log.e("FirebaseHelper", "Error saving post: ${e.message}", e)
            throw Exception("Failed to save post: ${e.message}")
        }
    }

    suspend fun saveDraft(post: Post): String {
        val draftId = database.reference.child("drafts").push().key ?: UUID.randomUUID().toString()
        val draftData = post.copy(postId = draftId, timestamp = ServerValue.TIMESTAMP)

        val draftMap = hashMapOf<String, Any?>(
            "postId" to draftData.postId,
            "userId" to draftData.userId,
            "userName" to draftData.userName,
            "userAvatar" to draftData.userAvatar,
            "caption" to draftData.caption,
            "hashtags" to draftData.hashtags,
            "imageUrls" to draftData.imageUrls,
            "music" to draftData.music,
            "taggedPeople" to draftData.taggedPeople,
            "location" to draftData.location,
            "audience" to draftData.audience,
            "commentsDisabled" to draftData.commentsDisabled,
            "likesHidden" to draftData.likesHidden,
            "timestamp" to ServerValue.TIMESTAMP
        )

        return try {
            database.reference.child("drafts").child(draftData.userId).child(draftId).setValue(draftMap).await()
            android.util.Log.d("FirebaseHelper", "Draft saved successfully: $draftId")
            draftId
        } catch (e: Exception) {
            android.util.Log.e("FirebaseHelper", "Error saving draft: ${e.message}", e)
            throw Exception("Failed to save draft: ${e.message}")
        }
    }

    suspend fun updateUserPostCount(userId: String) {
        try {
            val userRef = database.reference.child("users").child(userId)
            val snapshot = userRef.child("postCount").get().await()
            val currentCount = snapshot.getValue(Int::class.java) ?: 0
            userRef.child("postCount").setValue(currentCount + 1).await()
            userRef.child("updatedAt").setValue(ServerValue.TIMESTAMP).await()
            android.util.Log.d("FirebaseHelper", "User post count updated: ${currentCount + 1}")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseHelper", "Error updating post count: ${e.message}", e)
        }
    }

    suspend fun deleteDraft(userId: String, draftId: String) {
        try {
            database.reference.child("drafts").child(userId).child(draftId).removeValue().await()
            android.util.Log.d("FirebaseHelper", "Draft deleted: $draftId")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseHelper", "Error deleting draft: ${e.message}", e)
        }
    }

    suspend fun getUserPosts(userId: String): List<Post> {
        return try {
            val postIds = mutableListOf<String>()
            val userPostsSnapshot = database.reference.child("user-posts").child(userId).get().await()
            userPostsSnapshot.children.forEach { snapshot -> snapshot.key?.let { postIds.add(it) } }

            val posts = mutableListOf<Post>()
            for (postId in postIds) {
                val postSnapshot = database.reference.child("posts").child(postId).get().await()
                postSnapshot.getValue(Post::class.java)?.let { posts.add(it) }
            }

            posts.sortedByDescending {
                when (val timestamp = it.timestamp) {
                    is Long -> timestamp
                    else -> 0L
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseHelper", "Error getting user posts: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun savePostCategory(postId: String, category: String) {
        try {
            database.reference.child("post_categories").child(postId).setValue(category).await()
            android.util.Log.d("FirebaseHelper", "Post category saved for $postId: $category")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseHelper", "Error saving post category: ${e.message}", e)
        }
    }
}