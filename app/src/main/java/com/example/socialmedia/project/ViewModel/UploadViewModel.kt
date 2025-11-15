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

// --- CÁC IMPORT CỦA HÀM ĐẾM (Transaction) ---
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
// ------------------------------------

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

            val userSnapshot = database.reference.child("InfoUser").child(userId).get().await()
            val currentUser = userSnapshot.getValue(UserModel::class.java)
                ?: throw Exception("Không tìm thấy thông tin người dùng")

            val postId = UUID.randomUUID().toString()
            val postModel = PostModel(
                postId = postId,
                userId = userId, // Đảm bảo gán userId
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
            // 1. Lưu bài đăng
            uploadRepository.savePostToDatabase(postModel, mediaModels, currentUser)

            // 2. CẬP NHẬT BỘ ĐẾM (Transaction)
            updatePostCount(userId, 1) // +1

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
                userId = userId, // Đảm bảo gán userId
                caption = caption.takeIf { it.isNotBlank() },
                hashtags = parseHashtags(hashtags),
                locationName = location,
                musicId = selectedMusic?.musicId,
                taggedUserIds = taggedPeople,
                audienceType = audienceType.name,
                postType = PostType.PHOTO,
                allowsComments = !disableComments,
                allowsLikesVisible = !hideLikes,
                isDraft = true // Quan trọng: Đây là bài nháp
            )

            val mediaModels = uploadRepository.uploadImagesToCloudinary(context, draftId, imageUris) { current, total ->
                _uploadProgress.postValue(UploadProgress.UploadingImages(current, total, (current * 100) / total))
            }

            // Lưu nháp (Không cần cập nhật postCount)
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


    // ==================================================================
    // === HÀM ĐẾM (Transaction) ===
    // ==================================================================
    private fun updatePostCount(userId: String, delta: Int) {
        val userPostCountRef = database.reference.child("InfoUser").child(userId).child("postCount")

        userPostCountRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentCount = currentData.getValue(Int::class.java) ?: 0
                val newCount = currentCount + delta
                currentData.value = if (newCount < 0) 0 else newCount
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    Log.e("UploadViewModel", "Lỗi transaction cập nhật postCount: ${error.message}")
                } else if (committed) {
                    Log.d("UploadViewModel", "Cập nhật postCount thành công! Giá trị mới: ${currentData?.value}")
                }
            }
        })
    }
}
