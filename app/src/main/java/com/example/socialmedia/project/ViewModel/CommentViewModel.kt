package com.example.socialmedia.project.ViewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmedia.project.Domain.Model.CommentModel
import com.example.socialmedia.project.Repository.CommentRepository
import kotlinx.coroutines.launch

class CommentViewModel : ViewModel() {

    private val repository = CommentRepository()
    val comments = MutableLiveData<List<CommentModel>>()

    fun loadComments(postId: String) {
        viewModelScope.launch {
            comments.value = repository.getCommentsByPostId(postId)
        }
    }

    fun addComment(postId: String, userId: String, content: String) {
        viewModelScope.launch {
            repository.addComment(postId, userId, content)
            loadComments(postId) // reload sau khi thêm
        }
    }
}
