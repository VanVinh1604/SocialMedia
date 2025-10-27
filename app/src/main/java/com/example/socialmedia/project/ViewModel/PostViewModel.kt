package com.example.socialmedia.project.ViewModel

import androidx.lifecycle.*
import com.example.socialmedia.project.Domain.Model.PostModel
import com.example.socialmedia.project.Repository.PostRepository
import kotlinx.coroutines.launch

class PostViewModel(
    private val postRepository: PostRepository = PostRepository()
) : ViewModel() {

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
            _error.value = e.message
        }
    }
}
