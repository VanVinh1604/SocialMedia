package com.example.socialmedia.project.ViewModel // (Hoặc package ViewModel của bạn)

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.socialmedia.project.Domain.Model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class BlockListViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    // LiveData cho danh sách người dùng đã chặn
    private val _blockedUsersList = MutableLiveData<List<UserModel>>()
    val blockedUsersList: LiveData<List<UserModel>> get() = _blockedUsersList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun loadBlockedUsers() {
        _isLoading.postValue(true)
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            _errorMessage.postValue("Người dùng chưa đăng nhập")
            _isLoading.postValue(false)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Lấy danh sách ID đã chặn từ /block_list/
                val blockListRef = database.reference.child("block_list").child(currentUid)
                val snapshot = blockListRef.get().await()

                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    Log.d("BlockListViewModel", "Không có ai trong danh sách chặn.")
                    _blockedUsersList.postValue(emptyList())
                    _isLoading.postValue(false)
                    return@launch
                }

                val userModels = mutableListOf<UserModel>()

                // 2. Với mỗi ID, lấy thông tin User
                for (childSnap in snapshot.children) {
                    val targetId = childSnap.key
                    if (targetId != null) {
                        try {
                            val userSnap = database.reference.child("InfoUser").child(targetId).get().await()
                            if (userSnap.exists()) {

                                // === ĐÃ SỬA LỖI TẠI ĐÂY ===
                                val baseUser = userSnap.getValue(UserModel::class.java)
                                if (baseUser != null) {
                                    // Dùng .copy() để tạo đối tượng mới vì val không thể gán lại
                                    val userWithCorrectData = baseUser.copy(
                                        userId = userSnap.key!!, // Gán ID
                                        fullName = "${baseUser.firstName} ${baseUser.lastName}".trim()
                                    )
                                    userModels.add(userWithCorrectData)
                                }
                                // === KẾT THÚC SỬA LỖI ===

                            }
                        } catch (e: Exception) {
                            Log.e("BlockListViewModel", "Lỗi tải user info cho $targetId", e)
                        }
                    }
                }

                _blockedUsersList.postValue(userModels)
                _isLoading.postValue(false)

            } catch (e: Exception) {
                Log.e("BlockListViewModel", "Lỗi tải danh sách chặn", e)
                _errorMessage.postValue(e.message)
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Bỏ chặn một người dùng (Logic 2 chiều).
     * (Logic này được sao chép từ ProfileViewModel)
     */
    fun unblockUser(targetId: String) {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            _errorMessage.postValue("Không thể bỏ chặn khi chưa đăng nhập")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Tạo các đường dẫn (paths) bằng tay
                val myBlockListPath = "/block_list/$currentUid/$targetId"
                val targetBlockedByPath = "/blocked_by/$targetId/$currentUid"

                // 2. Xóa đồng thời ở cả hai nơi
                val updates = mapOf<String, Any?>(
                    myBlockListPath to null,
                    targetBlockedByPath to null
                )
                database.reference.updateChildren(updates).await()
                Log.d("BlockListViewModel", "Đã BỎ chặn 2 chiều: $targetId bởi $currentUid")

                // 3. Tải lại danh sách
                loadBlockedUsers()

            } catch (e: Exception) {
                _errorMessage.postValue("Lỗi khi bỏ chặn: ${e.message}")
            }
        }
    }
}