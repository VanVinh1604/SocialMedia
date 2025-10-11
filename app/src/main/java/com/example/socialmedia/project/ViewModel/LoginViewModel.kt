package com.example.socialmedia.project.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.socialmedia.project.Fragment.State.Resource
import com.example.socialmedia.project.Repository.LoginRepository
import kotlinx.coroutines.Dispatchers

class LoginViewModel: ViewModel() {
    private val repository = LoginRepository()
    fun login(email: String, password: String) = liveData(Dispatchers.IO) {
        emit(Resource.Loading())
        val result = repository.login(email, password)
        emit(result) // ✅ Trả về trực tiếp Resource từ repository (Success / Error)
    }

}
