package com.example.socialmedia.project.ViewModel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmedia.project.Domain.Enum.AudienceType
import com.example.socialmedia.project.Domain.Enum.MediaType
import com.example.socialmedia.project.Domain.Enum.PostType
import com.example.socialmedia.project.Domain.Model.MusicModel
import com.example.socialmedia.project.Domain.Model.PostMediaModel
import com.example.socialmedia.project.Domain.Model.PostModel
import com.example.socialmedia.project.Helper.CloudinaryHelper
import com.example.socialmedia.project.ViewModel.UploadProgress
import com.example.socialmedia.project.ViewModel.UploadResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class UploadViewModel : ViewModel() {

    private val _uploadProgress = MutableLiveData<UploadProgress>(UploadProgress.Idle)
    val uploadProgress: LiveData<UploadProgress> = _uploadProgress

    private val _uploadResult = MutableLiveData<UploadResult>(UploadResult.Idle)
    val uploadResult: LiveData<UploadResult> = _uploadResult

    private val _musicList = MutableLiveData<List<MusicModel>>(emptyList())
    val musicList: LiveData<List<MusicModel>> = _musicList

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    init {
        loadMusicList()
    }

    private fun loadMusicList() {
        viewModelScope.launch {
            try {
                val snapshot = database.reference.child("music").get().await()
                val musicList = snapshot.children.mapNotNull { it.getValue(MusicModel::class.java) }
                _musicList.postValue(musicList)
            } catch (e: Exception) {
                _musicList.postValue(emptyList()) // Xử lý lỗi bằng cách trả về danh sách rỗng
            }
        }
    }

    fun uploadPost(
        context: Context,
        imageUris: List<Uri>,
        caption: String,
        hashtags: String,
        selectedMusic: MusicModel?,
        taggedPeople: List<String>,
        location: String?,
        audienceType: AudienceType, // Cập nhật từ String thành AudienceType
        disableComments: Boolean,
        hideLikes: Boolean
    ) {
        viewModelScope.launch {
            try {
                _uploadProgress.value = UploadProgress.GettingUserInfo
                val userId = auth.currentUser?.uid ?: throw Exception("Người dùng chưa đăng nhập")

                val hashtagList = parseHashtags(hashtags)

                val postId = UUID.randomUUID().toString()
                val postModel = PostModel(
                    postId = postId,
                    userId = userId,
                    caption = caption.takeIf { it.isNotBlank() },
                    hashtags = hashtagList,
                    locationName = location,
                    musicId = selectedMusic?.musicId,
                    taggedUserIds = taggedPeople,
                    audienceType = audienceType.name, // Lưu tên của AudienceType
                    postType = PostType.PHOTO,
                    allowsComments = !disableComments,
                    allowsLikesVisible = !hideLikes,
                    isDraft = false
                )

                val mediaModels = uploadImagesToCloudinary(context, postId, imageUris)

                _uploadProgress.value = UploadProgress.SavingPost
                savePostToDatabase(postModel, mediaModels)

                _uploadResult.value = UploadResult.Success(postId)
                _uploadProgress.value = UploadProgress.Idle

            } catch (e: Exception) {
                _uploadResult.value = UploadResult.Error(e.message ?: "Lỗi không xác định")
                _uploadProgress.value = UploadProgress.Idle
            }
        }
    }

    fun saveDraft(
        context: Context,
        imageUris: List<Uri>,
        caption: String,
        hashtags: String,
        selectedMusic: MusicModel?,
        taggedPeople: List<String>,
        location: String?,
        audienceType: AudienceType, // Cập nhật từ String thành AudienceType
        disableComments: Boolean,
        hideLikes: Boolean
    ) {
        viewModelScope.launch {
            try {
                _uploadProgress.value = UploadProgress.SavingDraft
                val userId = auth.currentUser?.uid ?: throw Exception("Người dùng chưa đăng nhập")

                val hashtagList = parseHashtags(hashtags)
                val postId = UUID.randomUUID().toString()

                val postModel = PostModel(
                    postId = postId,
                    userId = userId,
                    caption = caption.takeIf { it.isNotBlank() },
                    hashtags = hashtagList,
                    locationName = location,
                    musicId = selectedMusic?.musicId,
                    taggedUserIds = taggedPeople,
                    audienceType = audienceType.name, // Lưu tên của AudienceType
                    postType = PostType.PHOTO,
                    allowsComments = !disableComments,
                    allowsLikesVisible = !hideLikes,
                    isDraft = true
                )

                val mediaModels = if (imageUris.isNotEmpty()) {
                    uploadImagesToCloudinary(context, postId, imageUris)
                } else {
                    emptyList()
                }

                savePostToDatabase(postModel, mediaModels)

                _uploadResult.value = UploadResult.DraftSaved(postId)
                _uploadProgress.value = UploadProgress.Idle

            } catch (e: Exception) {
                _uploadResult.value = UploadResult.Error(e.message ?: "Lỗi lưu nháp")
                _uploadProgress.value = UploadProgress.Idle
            }
        }
    }

    private suspend fun uploadImagesToCloudinary(
        context: Context,
        postId: String,
        imageUris: List<Uri>
    ): List<PostMediaModel> = withContext(Dispatchers.IO) {
        val mediaModels = mutableListOf<PostMediaModel>()

        imageUris.forEachIndexed { index, uri ->
            _uploadProgress.postValue(
                UploadProgress.UploadingImages(
                    current = index + 1,
                    total = imageUris.size,
                    progress = ((index + 1) * 100) / imageUris.size
                )
            )

            try {
                val imageUrl = CloudinaryHelper.uploadImage(context, uri)

                val mediaModel = PostMediaModel(
                    mediaId = UUID.randomUUID().toString(),
                    postId = postId,
                    mediaType = MediaType.IMAGE,
                    mediaUrl = imageUrl,
                    mediaOrder = index,
                    width = 1080,
                    height = 1080,
                    fileSize = 0
                )

                mediaModels.add(mediaModel)

            } catch (e: Exception) {
                throw Exception("Lỗi upload ảnh ${index + 1}: ${e.message}")
            }
        }

        return@withContext mediaModels
    }

    private suspend fun savePostToDatabase(
        postModel: PostModel,
        mediaModels: List<PostMediaModel>
    ) {
        val databaseRef = database.reference

        databaseRef.child("posts")
            .child(postModel.postId)
            .setValue(postModel)
            .await()

        mediaModels.forEach { media ->
            databaseRef.child("postMedia")
                .child(media.mediaId)
                .setValue(media)
                .await()
        }
    }

    private fun parseHashtags(hashtags: String): List<String> {
        return hashtags.split(" ")
            .map { it.trim() }
            .filter { it.isNotBlank() && it.startsWith("#") }
            .map { it.removePrefix("#") }
    }
}