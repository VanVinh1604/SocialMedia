package com.example.socialmedia.project.ViewModel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.*
import com.example.socialmedia.project.Domain.Enum.AudienceType
import com.example.socialmedia.project.Domain.Enum.PostType
import com.example.socialmedia.project.Domain.Model.MusicModel
import com.example.socialmedia.project.Domain.Model.PostModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.Repository.UploadRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class UploadViewModel(
    private val uploadRepository: UploadRepository = UploadRepository()
) : ViewModel() {

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
            _musicList.value = uploadRepository.loadMusicList()
        }
    }

    /**
     * 🚀 Upload bài đăng chính thức
     */
    fun uploadPost(
        context: Context,
        imageUris: List<Uri>,
        caption: String,
        hashtags: String,
        selectedMusic: MusicModel?,
        taggedPeople: List<String>,
        location: String?,
        audienceType: AudienceType,
        disableComments: Boolean,
        hideLikes: Boolean
    ) = viewModelScope.launch {
        try {
            _uploadProgress.value = UploadProgress.GettingUserInfo
            val userId = auth.currentUser?.uid ?: throw Exception("Người dùng chưa đăng nhập")

            // 🔹 Lấy thông tin UserModel hiện tại
            val userSnapshot = database.reference.child("InfoUser").child(userId).get().await()
            val currentUser = userSnapshot.getValue(UserModel::class.java)
                ?: throw Exception("Không tìm thấy thông tin người dùng")

            val postId = UUID.randomUUID().toString()
            val postModel = PostModel(
                postId = postId,
                caption = caption.takeIf { it.isNotBlank() },
                hashtags = parseHashtags(hashtags),
                locationName = location,
                musicId = selectedMusic?.musicId,
                taggedUserIds = taggedPeople,
                audienceType = audienceType.name,
                postType = PostType.PHOTO,
                allowsComments = !disableComments,
                allowsLikesVisible = !hideLikes,
                isDraft = false
            )

            val mediaModels = uploadRepository.uploadImagesToCloudinary(context, postId, imageUris) { current, total ->
                _uploadProgress.postValue(UploadProgress.UploadingImages(current, total, (current * 100) / total))
            }

            _uploadProgress.value = UploadProgress.SavingPost
            uploadRepository.savePostToDatabase(postModel, mediaModels, currentUser)
            _uploadResult.value = UploadResult.Success(postId)

        } catch (e: Exception) {
            _uploadResult.value = UploadResult.Error(e.message ?: "Lỗi upload")
        } finally {
            _uploadProgress.value = UploadProgress.Idle
        }
    }

    /**
     * 💾 Lưu bài viết vào NHÁP
     */
    fun saveDraft(
        context: Context,
        imageUris: List<Uri>,
        caption: String,
        hashtags: String,
        selectedMusic: MusicModel?,
        taggedPeople: List<String>,
        location: String?,
        audienceType: AudienceType,
        disableComments: Boolean,
        hideLikes: Boolean
    ) = viewModelScope.launch {
        try {
            _uploadProgress.value = UploadProgress.SavingDraft
            val userId = auth.currentUser?.uid ?: throw Exception("Người dùng chưa đăng nhập")

            val userSnapshot = database.reference.child("InfoUser").child(userId).get().await()
            val currentUser = userSnapshot.getValue(UserModel::class.java)
                ?: throw Exception("Không tìm thấy thông tin người dùng")

            val draftId = UUID.randomUUID().toString()
            val postModel = PostModel(
                postId = draftId,
                caption = caption.takeIf { it.isNotBlank() },
                hashtags = parseHashtags(hashtags),
                locationName = location,
                musicId = selectedMusic?.musicId,
                taggedUserIds = taggedPeople,
                audienceType = audienceType.name,
                postType = PostType.PHOTO,
                allowsComments = !disableComments,
                allowsLikesVisible = !hideLikes,
                isDraft = true
            )

            val mediaModels = uploadRepository.uploadImagesToCloudinary(context, draftId, imageUris) { current, total ->
                _uploadProgress.postValue(UploadProgress.UploadingImages(current, total, (current * 100) / total))
            }

            uploadRepository.saveDraftToDatabase(postModel, mediaModels, currentUser)
            _uploadResult.value = UploadResult.DraftSaved(draftId)

        } catch (e: Exception) {
            _uploadResult.value = UploadResult.Error(e.message ?: "Lỗi khi lưu nháp")
        } finally {
            _uploadProgress.value = UploadProgress.Idle
        }
    }

    private fun parseHashtags(hashtags: String): List<String> {
        return hashtags.split(" ")
            .map { it.trim() }
            .filter { it.isNotBlank() && it.startsWith("#") }
            .map { it.removePrefix("#") }
    }
}
