package com.example.socialmedia.project.Repository

import android.util.Log
import com.example.socialmedia.project.Domain.Model.PostMediaModel
import com.example.socialmedia.project.Domain.Model.PostModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PostRepository {

    private val database = FirebaseDatabase.getInstance().reference
    private val userRepository = UserRepository()

    /**
     * 📥 Lấy tất cả bài đăng
     */
    suspend fun getAllPosts(): List<PostModel> = withContext(Dispatchers.IO) {
        try {
            val snapshot = database.child("posts").get().await()
            snapshot.children.mapNotNull { it.getValue(PostModel::class.java) }
                .sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            Log.e("PostRepository", "❌ Lỗi khi lấy bài đăng: ${e.message}")
            emptyList()
        }
    }

    /**
     * 📸 Lấy danh sách media theo postId
     */
    suspend fun getMediaByPostId(postId: String): List<PostMediaModel> = withContext(Dispatchers.IO) {
        try {
            val snapshot = database.child("postMedia")
                .child(postId)
                .get().await()
            snapshot.children.mapNotNull { it.getValue(PostMediaModel::class.java) }
                .sortedBy { it.mediaOrder }
        } catch (e: Exception) {
            Log.e("PostRepository", "❌ Lỗi khi lấy media: ${e.message}")
            emptyList()
        }
    }


    /**
     * 📦 Hợp nhất bài đăng với thông tin user + media
     */
    suspend fun getPostsWithFullInfo(): List<PostModel> = withContext(Dispatchers.IO) {
        try {
            val posts = getAllPosts()
            val fullPosts = mutableListOf<PostModel>()


            for (post in posts) {
                // 🔍 Lấy thêm thông tin user nếu cần cập nhật
                val user = post.userId?.let { userRepository.getUserById(it) }

                // 🔹 Lấy danh sách media cho post này
                val mediaList = getMediaByPostId(post.postId)

                // 🔹 Log xem mediaList có bao nhiêu phần tử
                Log.d("PostRepository", "Post ${post.postId} mediaList: ${mediaList.size}")


                val enrichedPost = post.copy(
                    userName = post.userName ?: user?.fullName,
                    userProfileUrl = post.userProfileUrl ?: user?.profilePictureUrl,
//                    mediaList = getMediaByPostId(post.postId)
                    mediaList = post.mediaList // Lấy trực tiếp
                )
                fullPosts.add(enrichedPost)
            }

            fullPosts
        } catch (e: Exception) {
            Log.e("PostRepository", "❌ Lỗi khi hợp nhất bài đăng: ${e.message}")
            emptyList()
        }
    }

    suspend fun getLikedUsers(postId: String): List<UserModel> = withContext(Dispatchers.IO) {
        try {
            val snapshot = database.child("Posts").child(postId).child("likedUsers").get().await()
            val userIds = snapshot.children.mapNotNull { it.key } // Lấy userId đã like
            val users = mutableListOf<UserModel>()
            for (uid in userIds) {
                userRepository.getUserById(uid)?.let { users.add(it) }
            }
            users
        } catch (e: Exception) {
            Log.e("PostRepository", "❌ Lỗi khi lấy user đã like: ${e.message}")
            emptyList()
        }
    }


}
