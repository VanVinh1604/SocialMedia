package com.example.socialmedia.project.ViewModel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.socialmedia.project.Domain.Enum.Gender
import com.example.socialmedia.project.Domain.Model.PostModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.Repository.FollowRepository // <-- Đã thêm
import com.example.socialmedia.project.Repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import java.util.UUID

// ... (các import của Cloudinary/OkHttp nếu bạn có) ...
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    // --- (Hằng số Cloudinary của bạn) ---
    // (Hãy đảm bảo cloud_name và preset của bạn là đúng)
    private val CLOUD_NAME = "durfebos5"
    private val UPLOAD_PRESET = "unsigned_android_upload"
    // ------------------------------------

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val httpClient = OkHttpClient()

    // === REPOSITORIES ===
    private val postRepository: PostRepository = PostRepository()
    private val followRepository: FollowRepository = FollowRepository()
    // ===========================

    private var userRef: DatabaseReference? = null
    private var userListener: ValueEventListener? = null

    // LiveData cho Profile
    private val _userProfile = MutableLiveData<UserModel>()

    // === SỬA LỖI 1 & 2 TẠI ĐÂY ===
    // Sửa `_userSProfile` thành `_userProfile`
    // Sửa block body `{...}` thành expression body `= _userProfile`
    val userProfile: LiveData<UserModel> get() = _userProfile
    // === KẾT THÚC SỬA LỖI ===

    // LiveData cho danh sách bài đăng
    private val _userPosts = MutableLiveData<List<PostModel>>()
    val userPosts: LiveData<List<PostModel>> get() = _userPosts

    // LiveData cho trạng thái Follow
    private val _isFollowing = MutableLiveData<Boolean>()
    val isFollowing: LiveData<Boolean> get() = _isFollowing
    // ========================

    // ... (Các LiveData cũ: _errorMessage, _updateStatus, _imageUpdateStatus) ...
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage
    private val _updateStatus = MutableLiveData<Boolean>()
    val updateStatus: LiveData<Boolean> get() = _updateStatus
    private val _imageUpdateStatus = MutableLiveData<String?>()
    val imageUpdateStatus: LiveData<String?> get() = _imageUpdateStatus

    // =================================================================
    // PHẦN 1: LOGIC TẢI PROFILE (ĐÃ NÂNG CẤP)
    // =================================================================

    /**
     * [HÀM MỚI]
     * Tải hồ sơ và bài đăng của một người dùng.
     * @param userId ID của người dùng cần tải. Nếu là null, tải user hiện tại.
     */
    fun loadProfile(userId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Xác định ID cần tải
                val uidToLoad = userId ?: auth.currentUser?.uid
                if (uidToLoad == null) {
                    _errorMessage.postValue("Không thể xác định người dùng")
                    return@launch
                }

                // 2. Tải thông tin Profile (InfoUser)
                // Gỡ listener cũ (nếu có) trước khi gắn listener mới
                userListener?.let { userRef?.removeEventListener(it) }

                userRef = database.reference.child("InfoUser").child(uidToLoad)
                userListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val user = buildUserFromSnapshot(snapshot)
                            _userProfile.postValue(user)
                        } else {
                            _errorMessage.postValue("Không tìm thấy hồ sơ")
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        _errorMessage.postValue(error.message)
                    }
                }
                userRef?.addValueEventListener(userListener!!)

                // 3. Tải danh sách bài đăng (Posts)
                val posts = postRepository.getPostsWithFullInfoByUserId(uidToLoad)
                _userPosts.postValue(posts)

                // 4. Kiểm tra trạng thái Follow (chỉ khi xem profile người khác)
                if (userId != null && userId != auth.currentUser?.uid) {
                    val myFollowingSet = followRepository.getMyFollowingIdsSet()
                    _isFollowing.postValue(myFollowingSet.contains(uidToLoad))
                }

            } catch (e: Exception) {
                _errorMessage.postValue("Lỗi tải hồ sơ: ${e.message}")
            }
        }
    }

    // Hàm này của bạn (buildUserFromSnapshot) giữ nguyên
    private fun buildUserFromSnapshot(snap: DataSnapshot): UserModel {
        val email = snap.child("email").getValue(String::class.java) ?: ""
        val password = snap.child("password").getValue(String::class.java) ?: ""
        val firstName = snap.child("firstName").getValue(String::class.java) ?: ""
        val lastName = snap.child("lastName").getValue(String::class.java) ?: ""
        val langPref = snap.child("languagePreference").getValue(String::class.java) ?: "vi"

        val isPrivate = snap.child("Private").getValue(Boolean::class.java) ?: false
        val isVerified = snap.child("Verified").getValue(Boolean::class.java) ?: false
        val isActive = snap.child("Active").getValue(Boolean::class.java) ?: true

        val followers = snap.child("followerCount").getValue(Int::class.java) ?: 0
        val following = snap.child("followingCount").getValue(Int::class.java) ?: 0
        val posts = snap.child("postCount").getValue(Int::class.java) ?: 0
        val createdAt = snap.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
        val updatedAt = snap.child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()

        val phone = snap.child("phoneNumber").getValue(String::class.java)
        val bio = snap.child("bio").getValue(String::class.java)
        val picUrl = snap.child("profilePictureUrl").getValue(String::class.java)
        val headerUrl = snap.child("headerPictureUrl").getValue(String::class.java)
        val website = snap.child("website").getValue(String::class.java)
        val dob = snap.child("dateOfBirth").getValue(Long::class.java)
        val lastLogin = snap.child("lastLogin").getValue(Long::class.java)

        val gender = try {
            Gender.valueOf(snap.child("gender").getValue(String::class.java) ?: "PREFER_NOT_TO_SAY")
        } catch (e: Exception) { Gender.PREFER_NOT_TO_SAY }

        // (Hãy đảm bảo import ThemePreference là đúng)
        val theme = try {
            com.example.socialmedia.project.Domain.Enum.ThemePreference.valueOf(snap.child("themePreference").getValue(String::class.java) ?: "AUTO")
        } catch (e: Exception) { com.example.socialmedia.project.Domain.Enum.ThemePreference.AUTO }

        return UserModel(
            userId = snap.key ?: UUID.randomUUID().toString(),
            email = email,
            phoneNumber = phone,
            password = password,
            firstName = firstName,
            lastName = lastName,
            fullName = "$firstName $lastName".trim(),
            bio = bio,
            profilePictureUrl = picUrl,
            headerPictureUrl = headerUrl,
            website = website,
            gender = gender,
            dateOfBirth = dob,
            Private = isPrivate,
            Verified = isVerified,
            Active = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastLogin = lastLogin,
            languagePreference = langPref,
            themePreference = theme,
            followerCount = followers,
            followingCount = following,
            postCount = posts
        )
    }

    // Các hàm update (Edit Profile) của bạn giữ nguyên
    fun updateProfile(firstName: String, lastName: String, dob: Long?, gender: Gender) {
        val uid = auth.currentUser?.uid ?: return
        val newFullName = "$firstName $lastName".trim()

        viewModelScope.launch {
            try {
                val updates = mapOf<String, Any?>(
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "fullName" to newFullName,
                    "dateOfBirth" to dob,
                    "gender" to gender.name,
                    "updatedAt" to System.currentTimeMillis()
                )

                database.reference
                    .child("InfoUser")
                    .child(uid)
                    .updateChildren(updates)
                    .await()

                _updateStatus.postValue(true)

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Lỗi update: ", e)
                _updateStatus.postValue(false)
            }
        }
    }

    fun resetUpdateStatus() {
        _updateStatus.value = false
    }

    // Các hàm upload ảnh của bạn giữ nguyên
    fun uploadProfileImage(imageUri: Uri, imageType: String) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _imageUpdateStatus.postValue("error: Người dùng không xác định")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(imageUri)
                val imageBytes = inputStream?.readBytes()
                inputStream?.close()

                if (imageBytes == null) {
                    _imageUpdateStatus.postValue("error: Không thể đọc file ảnh")
                    return@launch
                }

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "profile_image.jpg", imageBytes.toRequestBody("image/*".toMediaTypeOrNull()))
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .build()

                val request = Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
                    .post(requestBody)
                    .build()

                httpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("ProfileViewModel", "Cloudinary Upload Lỗi: ", e)
                        _imageUpdateStatus.postValue("error: ${e.message}")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (!response.isSuccessful) {
                            _imageUpdateStatus.postValue("error: Lỗi Cloudinary Server ${response.code}")
                            return
                        }

                        try {
                            val responseBody = response.body?.string() ?: ""
                            val imageUrl = JSONObject(responseBody).optString("secure_url")

                            if (imageUrl.isNotEmpty()) {
                                updateImageUrlInDatabase(uid, imageType, imageUrl)
                            } else {
                                _imageUpdateStatus.postValue("error: Lỗi response từ Cloudinary")
                            }
                        } catch (e: Exception) {
                            Log.e("ProfileViewModel", "Lỗi phân tích JSON: ", e)
                            _imageUpdateStatus.postValue("error: ${e.message}")
                        }
                    }
                })

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Lỗi đọc file URI: ", e)
                _imageUpdateStatus.postValue("error: ${e.message}")
            }
        }
    }

    private fun updateImageUrlInDatabase(uid: String, imageType: String, imageUrl: String) {
        val fieldToUpdate = if (imageType == "avatar") {
            "profilePictureUrl"
        } else {
            "headerPictureUrl"
        }

        database.reference
            .child("InfoUser")
            .child(uid)
            .child(fieldToUpdate)
            .setValue(imageUrl)
            .addOnSuccessListener {
                _imageUpdateStatus.postValue("success")
            }
            .addOnFailureListener {
                _imageUpdateStatus.postValue("error: ${it.message}")
            }
    }

    fun resetImageUpdateStatus() {
        _imageUpdateStatus.value = null
    }

    // =================================================================
    // PHẦN 2: LOGIC FOLLOW (MỚI)
    // =================================================================

    fun followUser(targetUserId: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            followRepository.followUser(targetUserId)
            _isFollowing.postValue(true) // Cập nhật UI
        } catch (e: Exception) {
            _errorMessage.postValue(e.message)
        }
    }

    fun unfollowUser(targetUserId: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            followRepository.unfollowUser(targetUserId)
            _isFollowing.postValue(false) // Cập nhật UI
        } catch (e: Exception) {
            _errorMessage.postValue(e.message)
        }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.let {
            userRef?.removeEventListener(it)
        }
    }
}

