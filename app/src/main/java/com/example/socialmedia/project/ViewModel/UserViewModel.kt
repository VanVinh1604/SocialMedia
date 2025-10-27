package com.example.socialmedia.project.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.Repository.UserRepository
import kotlinx.coroutines.launch

class UserViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _user = MutableLiveData<UserModel?>()
    val user: LiveData<UserModel?> = _user

    fun loadCurrentUser() = viewModelScope.launch {
        _user.value = userRepository.getCurrentUser()
    }

    fun updateBio(newBio: String) = viewModelScope.launch {
        val userId = userRepository.getCurrentUser()?.userId ?: return@launch
        val success = userRepository.updateUserInfo(userId, mapOf("bio" to newBio))
        if (success) loadCurrentUser()
    }
}
