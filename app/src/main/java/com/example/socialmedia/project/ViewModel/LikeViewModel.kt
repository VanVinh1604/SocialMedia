package com.example.socialmedia.project.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.Repository.PostRepository
import kotlinx.coroutines.launch

class LikeViewModel(
    private val postRepository: PostRepository = PostRepository()
) : ViewModel() {

    private val _likedUsers = MutableLiveData<List<UserModel>>(emptyList())
    val likedUsers: LiveData<List<UserModel>> = _likedUsers

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadLikedUsers(postId: String) {
        viewModelScope.launch {
            try {
                val users = postRepository.getLikedUsers(postId)
                _likedUsers.value = users
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
