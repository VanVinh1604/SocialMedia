package com.example.socialmedia.project.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.socialmedia.project.Domain.Model.PostModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PostDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    // Danh sách tất cả bài viết của user đó
    private val _postList = MutableLiveData<List<PostModel>>()
    val postList: LiveData<List<PostModel>> get() = _postList

    // Vị trí của bài viết mà người dùng đã bấm vào lúc đầu
    private val _initialPosition = MutableLiveData<Int>()
    val initialPosition: LiveData<Int> get() = _initialPosition

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    /**
     * Tải danh sách bài viết của targetUserId.
     * @param targetUserId: ID của người đăng bài.
     * @param startPostId: ID của bài viết vừa bấm vào (để scroll tới đó đầu tiên).
     */
    fun loadUserPosts(targetUserId: String, startPostId: String) {
        val query = database.child("posts").orderByChild("userId").equalTo(targetUserId)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val posts = ArrayList<PostModel>()
                for (snap in snapshot.children) {
                    val post = snap.getValue(PostModel::class.java)
                    if (post != null) {
                        post.postId = snap.key ?: "" // Đảm bảo gán ID từ key
                        posts.add(post)
                    }
                }

                // Sắp xếp: Bài mới nhất lên đầu (giảm dần theo thời gian)
                posts.sortByDescending { it.createdAt }

                _postList.postValue(posts)

                // Tìm vị trí của bài viết startPostId trong danh sách đã sắp xếp
                val index = posts.indexOfFirst { it.postId == startPostId }
                if (index != -1) {
                    _initialPosition.postValue(index)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _errorMessage.postValue("Lỗi tải danh sách bài viết: ${error.message}")
            }
        })
    }

    /**
     * Xử lý Like/Unlike.
     * Lưu ý: ViewModel chỉ gọi Firebase, việc update UI realtime do Adapter tự lắng nghe hoặc quan sát LiveData nếu cần.
     */
    fun toggleLike(postId: String, currentStatus: Boolean) {
        val currentUid = auth.currentUser?.uid ?: return

        // 1. Chỗ lưu cũ (để đếm like cho bài viết)
        val postLikesRef = database.child("post_likes").child(postId).child(currentUid)

        // 2. [MỚI] Chỗ lưu mới (để làm Lịch sử cho User)
        val userHistoryRef = database.child("user_likes").child(currentUid).child(postId)

        if (currentStatus) {
            // Nếu đang Like -> Bấm phát nữa là Unlike -> Xóa khỏi cả 2 nơi
            postLikesRef.removeValue()
            userHistoryRef.removeValue()
        } else {
            // Nếu chưa Like -> Bấm là Like -> Lưu vào cả 2 nơi
            postLikesRef.setValue(true)
            userHistoryRef.setValue(true) // Giá trị là timestamp hoặc true đều được
        }
    }

    /**
     * Xử lý Bookmark/Unbookmark.
     */
    fun toggleBookmark(postId: String, currentStatus: Boolean) {
        val currentUid = auth.currentUser?.uid ?: return
        val ref = database.child("bookmarks").child(currentUid).child(postId)

        if (currentStatus) {
            ref.removeValue() // Bỏ lưu
        } else {
            ref.setValue(true) // Lưu
        }
    }
}