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
// import com.example.socialmedia.project.Repository.FollowRepository // <-- XÓA DÒNG NÀY
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import java.util.UUID

// ... (các import của Cloudinary/OkHttp) ...
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

    private val CLOUD_NAME = "durfebos5"
    private val UPLOAD_PRESET = "unsigned_android_upload"

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val httpClient = OkHttpClient()

    // private val followRepository: FollowRepository = FollowRepository() // <-- XÓA DÒNG NÀY

    private var userRef: DatabaseReference? = null
    private var userListener: ValueEventListener? = null
    private var postsRef: Query? = null
    private var postsListener: ValueEventListener? = null

    // LiveData cho Profile
    private val _userProfile = MutableLiveData<UserModel?>()
    val userProfile: LiveData<UserModel?> get() = _userProfile

    // LiveData cho danh sách bài đăng
    private val _userPosts = MutableLiveData<List<PostModel>>()
    val userPosts: LiveData<List<PostModel>> get() = _userPosts

    // LiveData cho trạng thái Follow (1 chiều: Tôi có theo dõi họ không?)
    private val _isFollowing = MutableLiveData<Boolean>()
    val isFollowing: LiveData<Boolean> get() = _isFollowing

    // Họ có theo dõi tôi không? (Để hiển thị "Follow Back")
    private val _theyAreFollowingMe = MutableLiveData<Boolean>(false)
    val theyAreFollowingMe: LiveData<Boolean> get() = _theyAreFollowingMe

    // Trạng thái Bạn bè (2 chiều)
    private val _isMutualFriend = MutableLiveData<Boolean>(false)
    val isMutualFriend: LiveData<Boolean> get() = _isMutualFriend

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage
    private val _updateStatus = MutableLiveData<Boolean>()
    val updateStatus: LiveData<Boolean> get() = _updateStatus
    private val _imageUpdateStatus = MutableLiveData<String?>()
    val imageUpdateStatus: LiveData<String?> get() = _imageUpdateStatus

    private val _blockStatus = MutableLiveData<Boolean>()
    val blockStatus: LiveData<Boolean> get() = _blockStatus

    private val _isTargetUserBlocked = MutableLiveData<Boolean>()
    val isTargetUserBlocked: LiveData<Boolean> get() = _isTargetUserBlocked

    private val _unblockSuccess = MutableLiveData<Boolean?>()
    val unblockSuccess: LiveData<Boolean?> get() = _unblockSuccess


    // =================================================================
    // PHẦN 1: LOGIC TẢI PROFILE (Đã sửa lỗi)
    // =================================================================

    fun loadProfile(userId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Xác định ID cần tải
                val uidToLoad = userId ?: auth.currentUser?.uid
                val myUid = auth.currentUser?.uid
                if (uidToLoad == null || myUid == null) {
                    _errorMessage.postValue("Không thể xác định người dùng")
                    return@launch
                }

                // === KIỂM TRA CHẶN 2 CHIỀU ===
                if (uidToLoad != myUid) {
                    val iBlockedThemSnap = database.reference.child("block_list").child(myUid).child(uidToLoad).get().await()
                    if (iBlockedThemSnap.exists()) {
                        _errorMessage.postValue("Không tìm thấy người dùng (bạn đã chặn họ)")
                        _userProfile.postValue(null); _userPosts.postValue(emptyList()); return@launch
                    }

                    val theyBlockedMeSnap = database.reference.child("blocked_by").child(myUid).child(uidToLoad).get().await()
                    if (theyBlockedMeSnap.exists()) {
                        _errorMessage.postValue("Không tìm thấy người dùng.")
                        _userProfile.postValue(null); _userPosts.postValue(emptyList()); return@launch
                    }
                }

                // === KIỂM TRA BẠN BÈ 2 CHIỀU ===
                if (userId != null && userId != myUid) {

                    // 4a. Tôi có theo dõi họ không? (Kiểm tra trực tiếp)
                    val iFollowThemSnap = database.reference.child("following").child(myUid).child(uidToLoad).get().await()
                    val iFollowThem = iFollowThemSnap.exists()
                    _isFollowing.postValue(iFollowThem)

                    // 4b. Họ có theo dõi tôi không? (Kiểm tra trực tiếp)
                    val theyFollowMeSnap = database.reference.child("following").child(uidToLoad).child(myUid).get().await()
                    val theyFollowMe = theyFollowMeSnap.exists()
                    _theyAreFollowingMe.postValue(theyFollowMe)

                    // 4c. Cập nhật trạng thái bạn bè
                    _isMutualFriend.postValue(iFollowThem && theyFollowMe)
                } else {
                    _isFollowing.postValue(false)
                    _theyAreFollowingMe.postValue(false)
                    _isMutualFriend.postValue(true) // Mình luôn là "bạn" của mình
                }

                // === TẢI PROFILE (chạy song song) ===
                userListener?.let { userRef?.removeEventListener(it) }
                userRef = database.reference.child("InfoUser").child(uidToLoad)
                userListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            _userProfile.postValue(buildUserFromSnapshot(snapshot)) // <-- Hàm này đã được sửa
                        } else {
                            _errorMessage.postValue("Không tìm thấy hồ sơ"); _userProfile.postValue(null)
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        _errorMessage.postValue(error.message); _userProfile.postValue(null)
                    }
                }
                userRef?.addValueEventListener(userListener!!)

                // === TẢI BÀI ĐĂNG (chạy song song) ===
                attachPostsListener(uidToLoad)

            } catch (e: Exception) {
                _errorMessage.postValue("Lỗi tải hồ sơ: ${e.message}")
                _userProfile.postValue(null)
            }
        }
    }

    private fun attachPostsListener(userId: String) {
        postsListener?.let { postsRef?.removeEventListener(it) }
        postsRef = database.reference.child("posts")
            .orderByChild("userId")
            .equalTo(userId)
        postsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val posts = snapshot.children.mapNotNull { it.getValue(PostModel::class.java) }
                        _userPosts.postValue(posts.sortedByDescending { it.createdAt })
                    } catch (e: Exception) {
                        _userPosts.postValue(emptyList())
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                _errorMessage.postValue(error.message)
            }
        }
        postsRef?.addValueEventListener(postsListener!!)
    }

    // === HÀM ĐÃ SỬA LỖI ===
    private fun buildUserFromSnapshot(snap: DataSnapshot): UserModel {
        val email = snap.child("email").getValue(String::class.java) ?: ""
        val password = snap.child("password").getValue(String::class.java) ?: ""
        val firstName = snap.child("firstName").getValue(String::class.java) ?: ""
        val lastName = snap.child("lastName").getValue(String::class.java) ?: ""
        val langPref = snap.child("languagePreference").getValue(String::class.java) ?: "vi"

        // === ĐỌC ĐÚNG NODE "Private" (viết hoa) ===
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
        val gender = try { Gender.valueOf(snap.child("gender").getValue(String::class.java) ?: "PREFER_NOT_TO_SAY") } catch (e: Exception) { Gender.PREFER_NOT_TO_SAY }
        val theme = try { com.example.socialmedia.project.Domain.Enum.ThemePreference.valueOf(snap.child("themePreference").getValue(String::class.java) ?: "AUTO") } catch (e: Exception) { com.example.socialmedia.project.Domain.Enum.ThemePreference.AUTO }

        return UserModel(
            userId = snap.key ?: UUID.randomUUID().toString(),
            email = email, phoneNumber = phone, password = password,
            firstName = firstName, lastName = lastName, fullName = "$firstName $lastName".trim(),
            bio = bio, profilePictureUrl = picUrl, headerPictureUrl = headerUrl,
            website = website, gender = gender, dateOfBirth = dob,

            // Đảm bảo UserModel của bạn có 'val Private: Boolean' (viết hoa P)
            Private = isPrivate,

            Verified = isVerified, Active = isActive,
            createdAt = createdAt, updatedAt = updatedAt, lastLogin = lastLogin,
            languagePreference = langPref, themePreference = theme,
            followerCount = followers, followingCount = following, postCount = posts
        )
    }

    // =================================================================
    // PHẦN 2: LOGIC UPDATE PROFILE (Không thu gọn)
    // =================================================================

    fun updateProfile(firstName: String, lastName: String, dob: Long?, gender: Gender, bio: String) {
        val uid = auth.currentUser?.uid ?: return
        val newFullName = "$firstName $lastName".trim()
        viewModelScope.launch {
            try {
                val updates = mapOf<String, Any?>(
                    "bio" to bio,
                    "firstName" to firstName, "lastName" to lastName,
                    "fullName" to newFullName, "dateOfBirth" to dob,
                    "gender" to gender.name, "updatedAt" to System.currentTimeMillis()
                )
                database.reference.child("InfoUser").child(uid).updateChildren(updates).await()
                _updateStatus.postValue(true)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Lỗi update: ", e); _updateStatus.postValue(false)
            }
        }
    }

    fun resetUpdateStatus() { _updateStatus.value = false }

    fun uploadProfileImage(imageUri: Uri, imageType: String) {
        val uid = auth.currentUser?.uid
        if (uid == null) { _imageUpdateStatus.postValue("error: Người dùng không xác định"); return }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(imageUri)
                val imageBytes = inputStream?.readBytes(); inputStream?.close()
                if (imageBytes == null) { _imageUpdateStatus.postValue("error: Không thể đọc file ảnh"); return@launch }
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "profile_image.jpg", imageBytes.toRequestBody("image/*".toMediaTypeOrNull()))
                    .addFormDataPart("upload_preset", UPLOAD_PRESET).build()
                val request = Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
                    .post(requestBody).build()
                httpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("ProfileViewModel", "Cloudinary Upload Lỗi: ", e); _imageUpdateStatus.postValue("error: ${e.message}")
                    }
                    override fun onResponse(call: Call, response: Response) {
                        if (!response.isSuccessful) { _imageUpdateStatus.postValue("error: Lỗi Cloudinary Server ${response.code}"); return }
                        try {
                            val responseBody = response.body?.string() ?: ""
                            val imageUrl = JSONObject(responseBody).optString("secure_url")
                            if (imageUrl.isNotEmpty()) { updateImageUrlInDatabase(uid, imageType, imageUrl) }
                            else { _imageUpdateStatus.postValue("error: Lỗi response từ Cloudinary") }
                        } catch (e: Exception) {
                            Log.e("ProfileViewModel", "Lỗi phân tích JSON: ", e); _imageUpdateStatus.postValue("error: ${e.message}")
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Lỗi đọc file URI: ", e); _imageUpdateStatus.postValue("error: ${e.message}")
            }
        }
    }

    private fun updateImageUrlInDatabase(uid: String, imageType: String, imageUrl: String) {
        val fieldToUpdate = if (imageType == "avatar") "profilePictureUrl" else "headerPictureUrl"
        database.reference.child("InfoUser").child(uid).child(fieldToUpdate).setValue(imageUrl)
            .addOnSuccessListener { _imageUpdateStatus.postValue("success") }
            .addOnFailureListener { _imageUpdateStatus.postValue("error: ${it.message}") }
    }

    fun resetImageUpdateStatus() { _imageUpdateStatus.value = null }

    // =================================================================
    // PHẦN 3: LOGIC FOLLOW (ĐÃ SỬA LỖI HOÀN TOÀN)
    // =================================================================

    fun followUser(targetUserId: String) = viewModelScope.launch(Dispatchers.IO) {
        val myUid = auth.currentUser?.uid
        if (myUid == null) {
            _errorMessage.postValue("Chưa đăng nhập"); return@launch
        }

        try {
            // 1. Tạo các đường dẫn (paths) bằng tay
            val followingPath = "/following/$myUid/$targetUserId"
            val followerPath = "/followers/$targetUserId/$myUid"
            val myInfoPath = "/InfoUser/$myUid/followingCount"
            val targetInfoPath = "/InfoUser/$targetUserId/followerCount"

            // 2. Tạo map để cập nhật 4 nơi cùng lúc
            val updates = mapOf(
                followingPath to true,
                followerPath to true,
                myInfoPath to ServerValue.increment(1),
                targetInfoPath to ServerValue.increment(1)
            )

            // 3. Ghi lên Firebase
            database.reference.updateChildren(updates).await()

            // 4. Cập nhật LiveData
            _isFollowing.postValue(true) // Tôi follow họ = true

            // 5. Kiểm tra lại 'theyFollowMe' (Họ có follow tôi không?)
            val theyFollowMe = _theyAreFollowingMe.value ?: false // Lấy giá trị cũ
            _isMutualFriend.postValue(true && theyFollowMe) // Bạn bè = (tôi vừa follow) VÀ (họ đã follow)

        } catch (e: Exception) {
            _errorMessage.postValue(e.message)
        }
    }

    fun unfollowUser(targetUserId: String) = viewModelScope.launch(Dispatchers.IO) {
        val myUid = auth.currentUser?.uid
        if (myUid == null) {
            _errorMessage.postValue("Chưa đăng nhập"); return@launch
        }

        try {
            // 1. Tạo các đường dẫn (paths) bằng tay
            val followingPath = "/following/$myUid/$targetUserId"
            val followerPath = "/followers/$targetUserId/$myUid"
            val myInfoPath = "/InfoUser/$myUid/followingCount"
            val targetInfoPath = "/InfoUser/$targetUserId/followerCount"

            // 2. Tạo map để xóa 4 nơi cùng lúc
            val updates = mapOf(
                followingPath to null,
                followerPath to null,
                myInfoPath to ServerValue.increment(-1),
                targetInfoPath to ServerValue.increment(-1)
            )

            // 3. Ghi lên Firebase
            database.reference.updateChildren(updates).await()

            // 4. Cập nhật LiveData
            _isFollowing.postValue(false) // Tôi unfollow họ
            _isMutualFriend.postValue(false) // Nếu tôi unfollow, chắc chắn không còn là bạn

        } catch (e: Exception) {
            _errorMessage.postValue(e.message)
        }
    }

    // =================================================================
    // PHẦN 4: LOGIC CHẶN (BLOCK) & BÁO CÁO (REPORT) (Không thu gọn)
    // =================================================================

    fun checkBlockStatus(targetId: String) {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            _isTargetUserBlocked.postValue(false)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val iBlockedThemSnap = database.reference.child("block_list").child(currentUid).child(targetId).get().await()
                _isTargetUserBlocked.postValue(iBlockedThemSnap.exists())
            } catch (e: Exception) {
                _isTargetUserBlocked.postValue(false)
            }
        }
    }

    fun blockUser(targetId: String) {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            _errorMessage.postValue("Không thể chặn khi chưa đăng nhập")
            return
        }
        if (currentUid == targetId) return // Không thể tự chặn

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val myBlockListPath = "/block_list/$currentUid/$targetId"
                val targetBlockedByPath = "/blocked_by/$targetId/$currentUid"

                val updates = mapOf<String, Any?>(
                    myBlockListPath to true,
                    targetBlockedByPath to true
                )
                database.reference.updateChildren(updates).await()
                Log.d("ProfileViewModel", "Đã chặn 2 chiều: $targetId bởi $currentUid")

                // Gọi hàm Unfollow (đã được sửa)
                unfollowUser(targetId)
                removeFollower(targetId, currentUid) // Gọi hàm private

                _blockStatus.postValue(true)

            } catch (e: Exception) {
                _errorMessage.postValue("Lỗi khi thực hiện chặn 2 chiều: ${e.message}")
            }
        }
    }

    fun unblockUser(targetId: String) {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            _errorMessage.postValue("Không thể bỏ chặn khi chưa đăng nhập")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val myBlockListPath = "/block_list/$currentUid/$targetId"
                val targetBlockedByPath = "/blocked_by/$targetId/$currentUid"

                val updates = mapOf<String, Any?>(
                    myBlockListPath to null,
                    targetBlockedByPath to null
                )
                database.reference.updateChildren(updates).await()
                Log.d("ProfileViewModel", "Đã BỎ chặn 2 chiều: $targetId bởi $currentUid")

                _unblockSuccess.postValue(true)

            } catch (e: Exception) {
                _errorMessage.postValue("Lỗi khi bỏ chặn: ${e.message}")
                _unblockSuccess.postValue(false)
            }
        }
    }

    fun resetUnblockSuccessStatus() {
        _unblockSuccess.value = null
    }

    fun reportUser(targetId: String) {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            _errorMessage.postValue("Không thể báo cáo khi chưa đăng nhập")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reportId = database.reference.child("Reports").push().key ?: UUID.randomUUID().toString()
                val reportData = mapOf(
                    "reportedUserId" to targetId,
                    "reportedByUserId" to currentUid,
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "pending"
                )
                database.reference.child("Reports").child(reportId).setValue(reportData).await()
                Log.d("ProfileViewModel", "Đã báo cáo: $targetId bởi $currentUid")
            } catch (e: Exception) {
                _errorMessage.postValue("Lỗi khi gửi báo cáo: ${e.message}")
            }
        }
    }

    fun resetBlockStatus() {
        _blockStatus.value = false
    }

    // =================================================================
    // PHẦN 5: HÀM PRIVATE HELPER (Không thu gọn)
    // =================================================================

    private suspend fun removeFollower(unfollowerId: String, unfollowedId: String) {
        try {
            val followingPath = "/following/$unfollowerId/$unfollowedId"
            val followerPath = "/followers/$unfollowedId/$unfollowerId"
            val unfollowerInfoPath = "/InfoUser/$unfollowerId/followingCount"
            val unfollowedInfoPath = "/InfoUser/$unfollowedId/followerCount"

            val snapshot = database.reference.child("following").child(unfollowerId).child(unfollowedId).get().await()

            if (snapshot.exists()) {
                val updates = mapOf<String, Any?>(
                    followingPath to null,
                    followerPath to null,
                    unfollowerInfoPath to ServerValue.increment(-1),
                    unfollowedInfoPath to ServerValue.increment(-1)
                )
                database.reference.updateChildren(updates).await()
                Log.d("ProfileViewModel", "$unfollowerId đã bị buộc unfollow $unfollowedId")
            } else {
                Log.w("ProfileViewModel", "$unfollowerId không follow $unfollowedId, không cần buộc unfollow.")
            }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Lỗi khi buộc unfollow: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.let { userRef?.removeEventListener(it) }
        postsListener?.let { postsRef?.removeEventListener(it) }
    }
}