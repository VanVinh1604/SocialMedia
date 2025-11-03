package com.example.socialmedia.project.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.Repository.FollowRepository
import kotlinx.coroutines.launch
// --- IMPORT MỚI ---
import com.google.firebase.auth.FirebaseAuth

class FollowListViewModel(
    private val repository: FollowRepository = FollowRepository()
) : ViewModel() {

    // --- THÊM MỚI ĐỂ LẤY UID ---
    private val auth = FirebaseAuth.getInstance()
    // --- KẾT THÚC THÊM MỚI ---

    private val _userList = MutableLiveData<List<UserModel>>()
    val userList: LiveData<List<UserModel>> = _userList

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Biến này RẤT QUAN TRỌNG để hiển thị "Unfollow"
    private val _myFollowingIds = MutableLiveData<Set<String>>(emptySet())
    val myFollowingIds: LiveData<Set<String>> = _myFollowingIds

    // === THÊM LẠI BIẾN CỦA BẠN ===
    // Biến để lưu trạng thái hiện tại (để biết khi nào cần refresh)
    private var currentUserId: String = ""
    private var currentType: String = ""
    // === KẾT THÚC THÊM LẠI ===


    /**
     * Hàm này SẼ được gọi TRƯỚC
     */
    private suspend fun loadMyFollowingStatus() {
        try {
            _myFollowingIds.value = repository.getMyFollowingIdsSet()
        } catch (e: Exception) {
            _error.postValue("Lỗi tải trạng thái follow: ${e.message}")
        }
    }


    fun loadList(userId: String, type: String) {
        // === THÊM LẠI LOGIC CỦA BẠN ===
        // Lưu lại để refresh
        currentUserId = userId
        currentType = type
        // === KẾT THÚC THÊM LẠI ===

        viewModelScope.launch {
            _isLoading.value = true

            val titleText = if (type == "following") "Following" else "Followers"
            _title.value = titleText

            try {
                // 1. Tải và ĐỢI danh sách following CỦA TÔI (để hiển thị nút)
                loadMyFollowingStatus()

                // 2. Tải danh sách người (followers/following của người khác)
                val list = repository.getFollowList(userId, type)
                _userList.value = list // Adapter sẽ có cả 2 danh sách

            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * [GỘP CODE] Gọi Repository để follow VÀ cập nhật UI
     */
    fun followUser(targetUserId: String) = viewModelScope.launch {
        try {
            // 1. Kiểm tra để không follow trùng (đã có trong Repository)
            repository.followUser(targetUserId)

            // 2. Cập nhật UI nút bấm tức thì
            _myFollowingIds.value = _myFollowingIds.value?.plus(targetUserId)

            // 3. [LOGIC CỦA BẠN] Tải lại danh sách (nếu đang ở trang "following" của chính mình)

            // === SỬA LỖI Ở ĐÂY ===
            // So sánh với auth.currentUser.uid thay vì repository.currentUid
            if (currentType == "following" && currentUserId == auth.currentUser?.uid) {
                val list = repository.getFollowList(currentUserId, currentType)
                _userList.value = list
            }
            // === KẾT THÚC SỬA LỖI ===

        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    /**
     * [GỘP CODE] Gọi Repository để unfollow VÀ cập nhật UI
     */
    fun unfollowUser(targetUserId: String) = viewModelScope.launch {
        try {
            // 1. Gọi Repository
            repository.unfollowUser(targetUserId)

            // 2. Cập nhật UI nút bấm tức thì
            _myFollowingIds.value = _myFollowingIds.value?.minus(targetUserId)

            // 3. [LOGIC CỦA BẠN] Xóa người đó khỏi danh sách tức thì
            if (currentType == "following") {
                // Lấy danh sách hiện tại, xóa người đó đi
                val currentList = _userList.value?.toMutableList() ?: mutableListOf()
                currentList.removeAll { it.userId == targetUserId }
                _userList.value = currentList // Cập nhật UI (biến mất)
            }

        } catch (e: Exception) {
            _error.value = e.message
        }
    }
}

