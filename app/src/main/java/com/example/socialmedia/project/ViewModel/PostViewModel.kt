package com.example.socialmedia.project.ViewModel

import androidx.lifecycle.*
import com.example.socialmedia.project.Domain.Model.PostModel
import com.example.socialmedia.project.Repository.PostRepository
import kotlinx.coroutines.launch

// --- Imports MỚI CẦN THÊM ---
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await

class PostViewModel(
    private val postRepository: PostRepository = PostRepository()
) : ViewModel() {

    // --- Code cũ của bạn (Giữ nguyên) ---
    private val _posts = MutableLiveData<List<PostModel>>(emptyList())
    val posts: LiveData<List<PostModel>> = _posts

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /**
     * 📥 Tải bài đăng (gồm cả user + media)
     */
    fun loadPosts() = viewModelScope.launch {
        try {
            val fullPosts = postRepository.getPostsWithFullInfo()
            _posts.value = fullPosts
        } catch (e: Exception) {
            _error.value = "Lỗi tải bài đăng: ${e.message}"
        }
    }
    // --- Hết code cũ của bạn ---


    // ==================================================================
    // === PHẦN CODE MỚI THÊM VÀO ĐỂ XÓA VÀ CẬP NHẬT COUNT ===
    // ==================================================================

    // Thêm các tham chiếu Firebase
    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    /**
     * 🆕 HÀM MỚI: Xóa một bài đăng
     * @param postId ID của bài đăng cần xóa
     * @param postAuthorId ID của người tạo bài đăng (để kiểm tra quyền)
     */
    fun deletePost(postId: String, postAuthorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = auth.currentUser?.uid

            // Kiểm tra xem người dùng hiện tại có phải là tác giả không
            if (uid != postAuthorId) {
                Log.w("PostViewModel", "Không thể xóa: Người dùng không sở hữu post")
                _error.postValue("Bạn không có quyền xóa bài đăng này")
                return@launch
            }

            try {
                // 1. Xóa bài đăng
                database.child("posts").child(postId).removeValue().await()
                // 2. Xóa media (nếu có)
                database.child("postMedia").child(postId).removeValue().await()
                // ... (Bạn cũng nên xóa likes, comments... liên quan) ...

                // 3. CẬP NHẬT BỘ ĐẾM (Quan trọng)
                // Gọi hàm transaction để -1
                updatePostCount(uid, -1)

                Log.d("PostViewModel", "Xóa bài đăng thành công: $postId")

            } catch (e: Exception) {
                Log.e("PostViewModel", "Lỗi khi xóa bài đăng: ${e.message}")
                _error.postValue("Lỗi khi xóa bài đăng: ${e.message}")
            }
        }
    }

    /**
     * 🆕 HÀM MỚI (private):
     * Sử dụng Transaction để tăng/giảm bộ đếm postCount trong InfoUser một cách an toàn.
     * @param userId ID của người dùng cần cập nhật
     * @param delta Giá trị thay đổi (VD: 1 để thêm, -1 để bớt)
     */
    private fun updatePostCount(userId: String, delta: Int) {
        // Trỏ tới nút /InfoUser/{uid}/postCount
        val userPostCountRef = database.child("InfoUser").child(userId).child("postCount")

        // Chạy một transaction
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
                    Log.e("PostViewModel", "Lỗi transaction cập nhật postCount: ${error.message}")
                } else if (committed) {
                    Log.d("PostViewModel", "Cập nhật postCount thành công! Giá trị mới: ${currentData?.value}")
                }
            }
        })
    }
}
