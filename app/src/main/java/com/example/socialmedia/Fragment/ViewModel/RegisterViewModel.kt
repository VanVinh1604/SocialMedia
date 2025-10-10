package com.example.socialmedia.Fragment.ViewModel

import androidx.lifecycle.*
import com.example.socialmedia.Fragment.Domain.UserModel
import com.example.socialmedia.Fragment.Fragment.State.Resource
import com.example.socialmedia.Fragment.Repository.RegisterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterViewModel: ViewModel() {

    private val repository = RegisterRepository()
    private val _userTemp = MutableLiveData<UserModel>()
    val userTemp: LiveData<UserModel> get() = _userTemp

    private val _registerResult = MutableLiveData<Resource<UserModel>>()
    val registerResult: LiveData<Resource<UserModel>> get() = _registerResult

    // Lưu email/password
    fun setEmailAndPassword(email: String, password: String) {
        _userTemp.value = UserModel(
            userId = "000",
            email = email,
            password = password,
            firstName = "Name",
            lastName = "null",
            fullName = "VanVinh",
            phoneNumber = "",
            dateOfBirth = null,
            gender = null,
            profilePicture = null,
            bio = null,
            createdAt = System.currentTimeMillis()
        )
    }

    // Lưu thông tin cá nhân
    fun setPersonalInfo(firstName: String, lastName: String, gender: String, dateOfBirth: Long) {
        _userTemp.value = _userTemp.value?.copy(
            firstName = firstName,
            lastName = lastName,
            fullName = "$firstName $lastName",
            gender = gender,
            dateOfBirth = dateOfBirth
        )
    }

    fun setPhoneNumber(phoneNumber: String) {
        _userTemp.value = _userTemp.value?.copy(phoneNumber = phoneNumber)
    }

    // Gọi repository đăng ký async
    fun registerUser() {
        val tempUser = _userTemp.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            _registerResult.postValue(Resource.Loading())
            try {
                val result = repository.register(tempUser.email, tempUser.password)
                if (result != null) {
                    _registerResult.postValue(Resource.Success(result))
                } else {
                    _registerResult.postValue(Resource.Error("Register failed!"))
                }
            } catch (e: Exception) {
                _registerResult.postValue(Resource.Error(e.message ?: "Unknown error"))
            }
        }
    }
}
