package com.example.socialmedia.project.Repository

import android.content.Context
import android.net.Uri
import com.example.socialmedia.project.Domain.Enum.MediaType
import com.example.socialmedia.project.Domain.Model.MediaItem
import com.example.socialmedia.project.Domain.Model.MusicModel
import com.example.socialmedia.project.Domain.Model.PostMediaModel
import com.example.socialmedia.project.Domain.Model.PostModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.Helper.CloudinaryHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class UploadRepository {

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * 📀 Lấy danh sách nhạc từ Firebase
     */
    suspend fun loadMusicList(): List<MusicModel> = withContext(Dispatchers.IO) {
        try {
            val snapshot = database.reference.child("music").get().await()
            snapshot.children.mapNotNull { it.getValue(MusicModel::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * ☁️ Upload danh sách media (ảnh + video) lên Cloudinary
     */
    suspend fun uploadMediaToCloudinary(
        context: Context,
        postId: String,
        mediaItems: List<MediaItem>,  // ← Nhận List<MediaItem> thay vì List<Uri>
        onProgress: (current: Int, total: Int) -> Unit
    ): List<PostMediaModel> = withContext(Dispatchers.IO) {
        val mediaModels = mutableListOf<PostMediaModel>()

        mediaItems.forEachIndexed { index, mediaItem ->
            onProgress(index + 1, mediaItems.size)
            try {
                // Upload tùy theo loại media
                val mediaUrl = when (mediaItem.type) {
                    MediaType.IMAGE -> CloudinaryHelper.uploadImage(context, mediaItem.uri)
                    MediaType.VIDEO -> CloudinaryHelper.uploadVideo(context, mediaItem.uri)
                }

                mediaModels.add(
                    PostMediaModel(
                        mediaId = UUID.randomUUID().toString(),
                        postId = postId,
                        mediaType = mediaItem.type,
                        mediaUrl = mediaUrl,
                        mediaOrder = index,
                        width = 1080,
                        height = 1080,
                        duration = mediaItem.duration?.toInt()  // Duration cho video
                    )
                )
            } catch (e: Exception) {
                val mediaTypeName = if (mediaItem.type == MediaType.IMAGE) "ảnh" else "video"
                throw Exception("Lỗi upload $mediaTypeName ${index + 1}: ${e.message}")
            }
        }

        return@withContext mediaModels
    }

    /**
     * ☁️ Upload danh sách ảnh lên Cloudinary (Giữ lại để tương thích ngược)
     */
    @Deprecated("Use uploadMediaToCloudinary instead", ReplaceWith("uploadMediaToCloudinary"))
    suspend fun uploadImagesToCloudinary(
        context: Context,
        postId: String,
        imageUris: List<Uri>,
        onProgress: (current: Int, total: Int) -> Unit
    ): List<PostMediaModel> = withContext(Dispatchers.IO) {
        val mediaItems = imageUris.map { MediaItem(it, MediaType.IMAGE) }
        return@withContext uploadMediaToCloudinary(context, postId, mediaItems, onProgress)
    }

    /**
     * 💾 Lưu bài đăng chính thức vào Firebase
     */
    suspend fun savePostToDatabase(
        postModel: PostModel,
        mediaModels: List<PostMediaModel>,
        currentUser: UserModel
    ) = withContext(Dispatchers.IO) {
        val ref = database.reference

        val updatedPost = postModel.copy(
            userId = currentUser.userId,
            userName = currentUser.fullName,
            userProfileUrl = currentUser.profilePictureUrl,
            mediaList = mediaModels
        )

        ref.child("posts").child(updatedPost.postId).setValue(updatedPost).await()
    }

    /**
     * 💾 Lưu bài viết dạng NHÁP (draft)
     */
    suspend fun saveDraftToDatabase(
        postModel: PostModel,
        mediaModels: List<PostMediaModel>,
        currentUser: UserModel
    ) = withContext(Dispatchers.IO) {
        val ref = database.reference

        val updatedDraft = postModel.copy(
            userId = currentUser.userId,
            userName = currentUser.fullName,
            userProfileUrl = currentUser.profilePictureUrl,
            mediaList = mediaModels
        )

        ref.child("drafts").child(updatedDraft.postId).setValue(updatedDraft).await()
    }
}