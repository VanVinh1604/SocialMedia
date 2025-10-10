package com.example.socialmedia.Fragment.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.socialmedia.Fragment.Fragment.State.Resource
import com.example.socialmedia.Fragment.Repository.LoginRepository
import kotlinx.coroutines.Dispatchers

class LoginViewModel: ViewModel() {
    private val repository = LoginRepository()
    fun login(email: String, password: String) = liveData(Dispatchers.IO) {
        emit(Resource.Loading())
        try {
            val user = repository.login(email, password)
            if (user != null) emit(Resource.Success(user))
            else emit(Resource.Error("Login failed"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown Error"))
        }
    }


}
