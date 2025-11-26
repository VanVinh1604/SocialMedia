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
import com.example.socialmedia.project.Domain.Model.StoryHighlightModel
import com.example.socialmedia.project.Domain.Model.StoryModel
import com.example.socialmedia.project.Domain.Model.UserModel
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
    private val _currentViewingStories = MutableLiveData<List<StoryModel>>()
    val currentViewingStories: LiveData<List<StoryModel>> get() = _currentViewingStories
    private val CLOUD_NAME = "durfebos5"
    private val UPLOAD_PRESET = "unsigned_android_upload"

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val httpClient = OkHttpClient()

    private var userRef: DatabaseReference? = null
    private var userListener: ValueEventListener? = null
    private var postsRef: Query? = null
    private var postsListener: ValueEventListener? = null

    // Listener cho Highlights
    private var highlightsRef: DatabaseReference? = null
    private var highlightsListener: ValueEventListener? = null

    // LiveData cho Profile
    private val _userProfile = MutableLiveData<UserModel?>()
    val userProfile: LiveData<UserModel?> get() = _userProfile

    // LiveData cho danh sách bài đăng
    private val _userPosts = MutableLiveData<List<PostModel>>()
    val userPosts: LiveData<List<PostModel>> get() = _userPosts

    // LiveData cho Story Highlights
    private val _userHighlights = MutableLiveData<List<StoryHighlightModel>>()
    val userHighlights: LiveData<List<StoryHighlightModel>> get() = _userHighlights

    // LiveData cho danh sách Story CÁ NHÂN (để chọn khi tạo highlight)
    private val _myStories = MutableLiveData<List<StoryModel>>()
    val myStories: LiveData<List<StoryModel>> get() = _myStories

    // LiveData cho trạng thái Follow
    private val _isFollowing = MutableLiveData<Boolean>()
    val isFollowing: LiveData<Boolean> get() = _isFollowing

    private val _theyAreFollowingMe = MutableLiveData<Boolean>(false)
    val theyAreFollowingMe: LiveData<Boolean> get() = _theyAreFollowingMe

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
    // PHẦN 1: LOGIC TẢI PROFILE & HIGHLIGHTS
    // =================================================================

    fun loadProfile(userId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
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
                    val iFollowThemSnap = database.reference.child("following").child(myUid).child(uidToLoad).get().await()
                    val iFollowThem = iFollowThemSnap.exists()
                    _isFollowing.postValue(iFollowThem)

                    val theyFollowMeSnap = database.reference.child("following").child(uidToLoad).child(myUid).get().await()
                    val theyFollowMe = theyFollowMeSnap.exists()
                    _theyAreFollowingMe.postValue(theyFollowMe)

                    _isMutualFriend.postValue(iFollowThem && theyFollowMe)
                } else {
                    _isFollowing.postValue(false)
                    _theyAreFollowingMe.postValue(false)
                    _isMutualFriend.postValue(true)
                }

                // === TẢI PROFILE ===
                userListener?.let { userRef?.removeEventListener(it) }
                userRef = database.reference.child("InfoUser").child(uidToLoad)
                userListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            _userProfile.postValue(buildUserFromSnapshot(snapshot))
                        } else {
                            _errorMessage.postValue("Không tìm thấy hồ sơ"); _userProfile.postValue(null)
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        _errorMessage.postValue(error.message); _userProfile.postValue(null)
                    }
                }
                userRef?.addValueEventListener(userListener!!)

                // === TẢI BÀI ĐĂNG ===
                attachPostsListener(uidToLoad)

                // === TẢI STORY HIGHLIGHTS ===
                attachHighlightsListener(uidToLoad)

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

    private fun attachHighlightsListener(userId: String) {
        highlightsListener?.let { highlightsRef?.removeEventListener(it) }
        highlightsRef = database.reference.child("StoryHighlights").child(userId)
        highlightsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<StoryHighlightModel>()
                for (child in snapshot.children) {
                    val item = child.getValue(StoryHighlightModel::class.java)
                    if (item != null) {
                        list.add(item)
                    }
                }
                _userHighlights.postValue(list.sortedByDescending { it.createdAt })
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("ProfileViewModel", "Lỗi tải highlights: ${error.message}")
            }
        }
        highlightsRef?.addValueEventListener(highlightsListener!!)
    }

    // =================================================================
    // HÀM TẢI STORY
    // =================================================================
    fun loadUserStories() {
        val uid = auth.currentUser?.uid ?: return
        val foundStories = mutableListOf<StoryModel>()

        database.reference.child("story").orderByChild("userId").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) child.getValue(StoryModel::class.java)?.let { foundStories.add(it) }

                    database.reference.child("stories").orderByChild("userId").equalTo(uid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snap2: DataSnapshot) {
                                for (child in snap2.children) {
                                    val item = child.getValue(StoryModel::class.java)
                                    if (item != null && foundStories.none { it.storyId == item.storyId }) foundStories.add(item)
                                }
                                _myStories.postValue(foundStories.sortedByDescending { it.createdAt })
                            }
                            override fun onCancelled(error: DatabaseError) {
                                _myStories.postValue(foundStories.sortedByDescending { it.createdAt })
                            }
                        })
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // 2. Tạo Highlight mới
    fun createHighlight(name: String, coverUrl: String, storyIds: List<String>) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val highlightId = database.reference.child("StoryHighlights").child(uid).push().key ?: return@launch
                val highlight = StoryHighlightModel(
                    id = highlightId, userId = uid, name = name, coverUrl = coverUrl,
                    storyIds = storyIds, createdAt = System.currentTimeMillis()
                )
                database.reference.child("StoryHighlights").child(uid).child(highlightId).setValue(highlight).await()
            } catch (e: Exception) { Log.e("ViewModel", "Lỗi tạo: ${e.message}") }
        }
    }

    // 3. Tải Story chi tiết cho 1 Highlight (Để xem lại)
    fun loadStoriesForHighlight(highlightId: String, targetUserId: String? = null) {
        val uid = targetUserId ?: auth.currentUser?.uid ?: return

        database.reference.child("StoryHighlights").child(uid).child(highlightId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(highlightSnap: DataSnapshot) {
                    val highlight = highlightSnap.getValue(StoryHighlightModel::class.java)
                    val storyIds = highlight?.storyIds ?: emptyList()

                    if (storyIds.isEmpty()) {
                        _currentViewingStories.postValue(emptyList())
                        return
                    }

                    val loadedStories = mutableListOf<StoryModel>()
                    var loadedCount = 0

                    for (id in storyIds) {
                        database.reference.child("stories").child(id)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snap: DataSnapshot) {
                                    if (snap.exists()) {
                                        snap.getValue(StoryModel::class.java)?.let {
                                            loadedStories.add(it)
                                        }
                                        checkDone()
                                    } else {
                                        database.reference.child("story").child(id).get()
                                            .addOnSuccessListener { sSnap ->
                                                sSnap.getValue(StoryModel::class.java)?.let {
                                                    loadedStories.add(it)
                                                }
                                                checkDone()
                                            }
                                            .addOnFailureListener { checkDone() }
                                    }
                                }

                                override fun onCancelled(e: DatabaseError) {
                                    checkDone()
                                }

                                fun checkDone() {
                                    loadedCount++
                                    if (loadedCount == storyIds.size) {
                                        _currentViewingStories.postValue(
                                            loadedStories.sortedBy { it.createdAt }
                                        )
                                    }
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ProfileViewModel", "Lỗi tải highlight: ${error.message}")
                    _currentViewingStories.postValue(emptyList())
                }
            })
    }

    // =================================================================
    // PHẦN CÒN LẠI (HELPER, UPDATE, FOLLOW...)
    // =================================================================

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
        val gender = try { Gender.valueOf(snap.child("gender").getValue(String::class.java) ?: "PREFER_NOT_TO_SAY") } catch (e: Exception) { Gender.PREFER_NOT_TO_SAY }
        val theme = try { com.example.socialmedia.project.Domain.Enum.ThemePreference.valueOf(snap.child("themePreference").getValue(String::class.java) ?: "AUTO") } catch (e: Exception) { com.example.socialmedia.project.Domain.Enum.ThemePreference.AUTO }

        return UserModel(
            userId = snap.key ?: UUID.randomUUID().toString(),
            email = email, phoneNumber = phone, password = password,
            firstName = firstName, lastName = lastName, fullName = "$firstName $lastName".trim(),
            bio = bio, profilePictureUrl = picUrl, headerPictureUrl = headerUrl,
            website = website, gender = gender, dateOfBirth = dob,
            Private = isPrivate,
            Verified = isVerified, Active = isActive,
            createdAt = createdAt, updatedAt = updatedAt, lastLogin = lastLogin,
            languagePreference = langPref, themePreference = theme,
            followerCount = followers, followingCount = following, postCount = posts
        )
    }

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

    fun followUser(targetUserId: String) = viewModelScope.launch(Dispatchers.IO) {
        val myUid = auth.currentUser?.uid ?: run { _errorMessage.postValue("Chưa đăng nhập"); return@launch }
        try {
            val updates = mapOf(
                "/following/$myUid/$targetUserId" to true,
                "/followers/$targetUserId/$myUid" to true,
                "/InfoUser/$myUid/followingCount" to ServerValue.increment(1),
                "/InfoUser/$targetUserId/followerCount" to ServerValue.increment(1)
            )
            database.reference.updateChildren(updates).await()
            _isFollowing.postValue(true)
            val theyFollowMe = _theyAreFollowingMe.value ?: false
            _isMutualFriend.postValue(true && theyFollowMe)
        } catch (e: Exception) {
            _errorMessage.postValue(e.message)
        }
    }

    fun unfollowUser(targetUserId: String) = viewModelScope.launch(Dispatchers.IO) {
        val myUid = auth.currentUser?.uid ?: run { _errorMessage.postValue("Chưa đăng nhập"); return@launch }
        try {
            val updates = mapOf(
                "/following/$myUid/$targetUserId" to null,
                "/followers/$targetUserId/$myUid" to null,
                "/InfoUser/$myUid/followingCount" to ServerValue.increment(-1),
                "/InfoUser/$targetUserId/followerCount" to ServerValue.increment(-1)
            )
            database.reference.updateChildren(updates).await()
            _isFollowing.postValue(false)
            _isMutualFriend.postValue(false)
        } catch (e: Exception) {
            _errorMessage.postValue(e.message)
        }
    }

    fun checkBlockStatus(targetId: String) {
        val currentUid = auth.currentUser?.uid ?: run { _isTargetUserBlocked.postValue(false); return }
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
        val currentUid = auth.currentUser?.uid ?: return
        if (currentUid == targetId) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updates = mapOf<String, Any?>(
                    "/block_list/$currentUid/$targetId" to true,
                    "/blocked_by/$targetId/$currentUid" to true
                )
                database.reference.updateChildren(updates).await()

                unfollowUser(targetId)
                removeFollower(targetId, currentUid)
                _blockStatus.postValue(true)
            } catch (e: Exception) {
                _errorMessage.postValue("Lỗi chặn: ${e.message}")
            }
        }
    }

    fun unblockUser(targetId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updates = mapOf<String, Any?>(
                    "/block_list/$currentUid/$targetId" to null,
                    "/blocked_by/$targetId/$currentUid" to null
                )
                database.reference.updateChildren(updates).await()
                _unblockSuccess.postValue(true)
            } catch (e: Exception) {
                _unblockSuccess.postValue(false)
            }
        }
    }

    fun resetUnblockSuccessStatus() { _unblockSuccess.value = null }

    fun reportUser(targetId: String) {
        val currentUid = auth.currentUser?.uid ?: return
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
            } catch (e: Exception) {
                _errorMessage.postValue("Lỗi báo cáo: ${e.message}")
            }
        }
    }

    fun resetBlockStatus() { _blockStatus.value = false }

    private suspend fun removeFollower(unfollowerId: String, unfollowedId: String) {
        try {
            val snapshot = database.reference.child("following").child(unfollowerId).child(unfollowedId).get().await()
            if (snapshot.exists()) {
                val updates = mapOf<String, Any?>(
                    "/following/$unfollowerId/$unfollowedId" to null,
                    "/followers/$unfollowedId/$unfollowerId" to null,
                    "/InfoUser/$unfollowerId/followingCount" to ServerValue.increment(-1),
                    "/InfoUser/$unfollowedId/followerCount" to ServerValue.increment(-1)
                )
                database.reference.updateChildren(updates).await()
            }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Lỗi buộc unfollow: ${e.message}")
        }
    }

    // =================================================================
    // [MỚI] PHẦN ADMIN: CẬP NHẬT HIGHLIGHT (Rename, Delete, Add Story)
    // =================================================================

    // 1. Đổi tên Highlight
    fun updateHighlightName(highlightId: String, newName: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.reference.child("StoryHighlights")
                    .child(uid)
                    .child(highlightId)
                    .child("name")
                    .setValue(newName)
                    .await()
            } catch (e: Exception) {
                _errorMessage.postValue("Lỗi đổi tên: ${e.message}")
            }
        }
    }

    // 2. Xóa Highlight
    fun deleteHighlight(highlightId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.reference.child("StoryHighlights")
                    .child(uid)
                    .child(highlightId)
                    .removeValue()
                    .await()
            } catch (e: Exception) {
                _errorMessage.postValue("Lỗi xóa highlight: ${e.message}")
            }
        }
    }

    // 3. Xóa 1 Story khỏi Highlight
    fun removeStoryFromHighlight(highlightId: String, storyIdToRemove: String) {
        val uid = auth.currentUser?.uid ?: return
        val highlightRef = database.reference.child("StoryHighlights").child(uid).child(highlightId)

        highlightRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val highlight = snapshot.getValue(StoryHighlightModel::class.java) ?: return
                // Fix null safety
                val currentStoryIds = highlight.storyIds?.toMutableList() ?: mutableListOf()

                if (currentStoryIds.contains(storyIdToRemove)) {
                    currentStoryIds.remove(storyIdToRemove)
                    highlightRef.child("storyIds").setValue(currentStoryIds)
                        .addOnSuccessListener {
                            // Reload UI để thấy thay đổi
                            loadStoriesForHighlight(highlightId, uid)
                        }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                _errorMessage.postValue(error.message)
            }
        })
    }

    // 4. Thêm Story Mới vào Highlight
    fun addStoryToHighlight(highlightId: String, newStory: StoryModel) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Bước 1: Lưu Story mới vào bảng "stories"
                database.reference.child("stories").child(newStory.storyId).setValue(newStory).await()

                // Bước 2: Thêm ID vào Highlight
                val highlightRef = database.reference.child("StoryHighlights").child(uid).child(highlightId)
                val snapshot = highlightRef.get().await()
                val highlight = snapshot.getValue(StoryHighlightModel::class.java)

                if (highlight != null) {
                    // Fix null safety
                    val currentList = highlight.storyIds?.toMutableList() ?: mutableListOf()
                    currentList.add(newStory.storyId)
                    highlightRef.child("storyIds").setValue(currentList).await()

                    // Reload lại UI
                    loadStoriesForHighlight(highlightId, uid)
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Lỗi thêm story: ${e.message}")
            }
        }
    }

    // 5. Upload Ảnh Story (Trả về URL qua callback)
    fun uploadStoryImage(imageUri: Uri, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) { onError("User not logged in"); return }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(imageUri)
                val imageBytes = inputStream?.readBytes()
                inputStream?.close()

                if (imageBytes == null) {
                    launch(Dispatchers.Main) { onError("Không đọc được file ảnh") }
                    return@launch
                }

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "story_image.jpg", imageBytes.toRequestBody("image/*".toMediaTypeOrNull()))
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .build()

                val request = Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
                    .post(requestBody)
                    .build()

                httpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        launch(Dispatchers.Main) { onError(e.message ?: "Upload failed") }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val responseBody = response.body?.string()
                            val imageUrl = JSONObject(responseBody ?: "").optString("secure_url")
                            launch(Dispatchers.Main) { onSuccess(imageUrl) }
                        } else {
                            launch(Dispatchers.Main) { onError("Lỗi Server: ${response.code}") }
                        }
                    }
                })
            } catch (e: Exception) {
                launch(Dispatchers.Main) { onError(e.message ?: "Unknown error") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.let { userRef?.removeEventListener(it) }
        postsListener?.let { postsRef?.removeEventListener(it) }
        highlightsListener?.let { highlightsRef?.removeEventListener(it) }
    }
}