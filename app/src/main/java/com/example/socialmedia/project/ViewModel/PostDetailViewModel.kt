package com.example.socialmedia.project.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
// Xóa import CommentModel
import com.example.socialmedia.project.Domain.Model.PostModel
import com.google.firebase.auth.FirebaseAuth // Thêm import
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch

class PostDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance() // Thêm auth

    private val _post = MutableLiveData<PostModel?>()
    val post: LiveData<PostModel?> get() = _post

    // === XÓA HOÀN TOÀN LOGIC COMMENT Ở ĐÂY ===
    // private val _comments = MutableLiveData<List<CommentModel>>()
    // val comments: LiveData<List<CommentModel>> get() = _comments

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    // === THÊM LOGIC LIKE/BOOKMARK VÀO ĐÂY ===
    private val _isLikedByCurrentUser = MutableLiveData<Boolean>(false)
    val isLikedByCurrentUser: LiveData<Boolean> get() = _isLikedByCurrentUser

    private val _likeCount = MutableLiveData<Long>(0)
    val likeCount: LiveData<Long> get() = _likeCount

    private val _isBookmarked = MutableLiveData<Boolean>(false)
    val isBookmarked: LiveData<Boolean> get() = _isBookmarked
    // =====================================

    // === CÁC BIẾN ĐỂ GIỮ LISTENER ===
    private var postListener: ValueEventListener? = null
    private var likesListener: ValueEventListener? = null
    private var bookmarkListener: ValueEventListener? = null
    private var postRef: DatabaseReference? = null
    private var likesRef: DatabaseReference? = null
    private var bookmarkRef: DatabaseReference? = null


    fun loadPostDetails(postId: String) {
        val currentUid = auth.currentUser?.uid
        if (postId.isEmpty() || currentUid == null) {
            _errorMessage.postValue("Lỗi: PostId hoặc User không hợp lệ")
            return
        }

        cleanupListeners() // Xóa listener cũ

        viewModelScope.launch {
            loadPost(postId)
            loadLikeAndBookmarkStatus(postId, currentUid)
            // === XÓA loadRootComments(postId) ===
        }
    }

    // Tải thông tin chính của bài post
    private fun loadPost(postId: String) {
        postRef = database.child("posts").child(postId)
        postListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _post.postValue(snapshot.getValue(PostModel::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                _post.postValue(null)
                _errorMessage.postValue("Lỗi tải post: ${error.message}")
            }
        }
        postRef?.addValueEventListener(postListener!!)
    }

    // Tải trạng thái Like và Bookmark
    private fun loadLikeAndBookmarkStatus(postId: String, currentUid: String) {
        // 1. Tải Likes
        likesRef = database.child("post_likes").child(postId)
        likesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _likeCount.postValue(snapshot.childrenCount)
                _isLikedByCurrentUser.postValue(snapshot.hasChild(currentUid))
            }
            override fun onCancelled(error: DatabaseError) {
                _errorMessage.postValue("Lỗi tải Like: ${error.message}")
            }
        }
        likesRef?.addValueEventListener(likesListener!!)

        // 2. Tải Bookmark (Giả sử bạn lưu ở "bookmarks/USER_ID/POST_ID")
        bookmarkRef = database.child("bookmarks").child(currentUid).child(postId)
        bookmarkListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _isBookmarked.postValue(snapshot.exists())
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        bookmarkRef?.addValueEventListener(bookmarkListener!!)
    }

    // === XÓA HÀM loadRootComments ===

    // === THÊM CÁC HÀM TÁI SỬ DỤNG LOGIC ===
    fun toggleLike(postId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val ref = database.child("post_likes").child(postId).child(currentUid)

        if (_isLikedByCurrentUser.value == true) {
            ref.removeValue()
        } else {
            ref.setValue(true)
        }
    }

    fun toggleBookmark(postId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val ref = database.child("bookmarks").child(currentUid).child(postId)

        if (_isBookmarked.value == true) {
            ref.removeValue()
        } else {
            ref.setValue(true)
        }
    }

    // Hàm này sẽ được gọi khi Fragment bị hủy
    private fun cleanupListeners() {
        postListener?.let { postRef?.removeEventListener(it) }
        likesListener?.let { likesRef?.removeEventListener(it) }
        bookmarkListener?.let { bookmarkRef?.removeEventListener(it) }
    }

    override fun onCleared() {
        super.onCleared()
        cleanupListeners()
    }
}