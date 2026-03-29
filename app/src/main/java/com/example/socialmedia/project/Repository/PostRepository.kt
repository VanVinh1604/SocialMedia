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
     * 📥 Lấy tất cả bài đăng (cho Feed)
     */
    suspend fun getAllPosts(): List<PostModel> = withContext(Dispatchers.IO) {
        try {
            // Đọc từ "posts" (p thường) - Đã đúng
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
            // Đọc từ "postMedia" (p thường) - Đã đúng
            val snapshot = database.child("postMedia")
                .child(postId)
                .get().await()
            // Đọc map các object con
            snapshot.children.mapNotNull { it.getValue(PostMediaModel::class.java) }
                .sortedBy { it.mediaOrder }
        } catch (e: Exception) {
            Log.e("PostRepository", "❌ Lỗi khi lấy media: ${e.message}")
            emptyList()
        }
    }


    /**
     * 📦 Hợp nhất bài đăng với thông tin user + media (CHO FEED)
     */
    suspend fun getPostsWithFullInfo(): List<PostModel> = withContext(Dispatchers.IO) {
        try {
            val posts = getAllPosts()
            val fullPosts = mutableListOf<PostModel>()

            for (post in posts) {
                val user = post.userId?.let { userRepository.getUserById(it) }

                // === SỬA LỖI LOGIC: KIỂM TRA mediaList ===
                val mediaList: List<PostMediaModel>

                // 1. Kiểm tra xem mediaList có sẵn bên trong post không
                if (post.mediaList.isNotEmpty()) {
                    // Nếu có, dùng luôn
                    mediaList = post.mediaList
                    Log.d("PostRepository", "Feed: Post ${post.postId} dùng mediaList có sẵn (${mediaList.size} ảnh)")
                } else {
                    // 2. Nếu không, đi tìm trong 'postMedia'
                    mediaList = getMediaByPostId(post.postId)
                    Log.d("PostRepository", "Feed: Post ${post.postId} tìm trong 'postMedia' (${mediaList.size} ảnh)")
                }
                // === KẾT THÚC SỬA LỖI ===

                val enrichedPost = post.copy(
                    userName = post.userName ?: user?.fullName,
                    userProfileUrl = post.userProfileUrl ?: user?.profilePictureUrl,
                    mediaList = mediaList // Gán mediaList vừa lấy được
                )
                fullPosts.add(enrichedPost)
            }
            fullPosts
        } catch (e: Exception) {
            Log.e("PostRepository", "❌ Lỗi khi hợp nhất bài đăng Feed: ${e.message}")
            emptyList()
        }
    }

    suspend fun getLikedUsers(postId: String): List<UserModel> = withContext(Dispatchers.IO) {
        try {
            // Sửa lại cho nhất quán: đọc từ "posts" (p thường)
            val snapshot = database.child("Posts").child(postId).child("likedUsers").get().await()
            val userIds = snapshot.children.mapNotNull { it.key }
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

    // =================================================================
    // === 🔽 HÀM LẤY POST CHO PROFILE 🔽 ===
    // =================================================================

    /**
     * 📥 [Profile] Lấy tất cả bài đăng của MỘT user
     */
    private suspend fun getAllPostsByUserId(userId: String): List<PostModel> = withContext(Dispatchers.IO) {
        try {
            // Đọc từ "posts" (p thường) - Đã đúng
            val snapshot = database.child("posts")
                .orderByChild("userId")
                .equalTo(userId)
                .get().await()

            val posts = snapshot.children.mapNotNull { it.getValue(PostModel::class.java) }
            Log.d("PostRepository", "Tìm thấy ${posts.size} bài đăng cho user $userId")
            return@withContext posts.sortedByDescending { it.createdAt }

        } catch (e: Exception) {
            Log.e("PostRepository", "❌ Lỗi khi lấy bài đăng theo UserID: ${e.message}")
            emptyList()
        }
    }

    /**
     * 📦 [Profile] Hợp nhất bài đăng (user + media) cho MỘT user
     */
    suspend fun getPostsWithFullInfoByUserId(userId: String): List<PostModel> = withContext(Dispatchers.IO) {
        try {
            val userPosts = getAllPostsByUserId(userId)
            val fullPosts = mutableListOf<PostModel>()

            for (post in userPosts) {
                // === SỬA LỖI LOGIC: KIỂM TRA mediaList ===
                val mediaList: List<PostMediaModel>

                // 1. Kiểm tra xem mediaList có sẵn bên trong post không
                if (post.mediaList.isNotEmpty()) {
                    // Nếu có, dùng luôn
                    mediaList = post.mediaList
                    Log.d("PostRepository", "Profile: Post ${post.postId} dùng mediaList có sẵn (${mediaList.size} ảnh)")
                } else {
                    // 2. Nếu không, đi tìm trong 'postMedia'
                    mediaList = getMediaByPostId(post.postId)
                    Log.d("PostRepository", "Profile: Post ${post.postId} tìm trong 'postMedia' (${mediaList.size} ảnh)")
                }
                // === KẾT THÚC SỬA LỖI ===

                val enrichedPost = post.copy(
                    mediaList = mediaList // Gán mediaList vừa lấy được
                )
                fullPosts.add(enrichedPost)
            }

            Log.d("PostRepository", "Trả về ${fullPosts.size} bài đăng đầy đủ cho user $userId")
            fullPosts
        } catch (e: Exception) {
            Log.e("PostRepository", "❌ Lỗi khi hợp nhất bài đăng user: ${e.message}")
            emptyList()
        }
    }
}

