package com.example.socialmedia.project.ViewModel

import androidx.lifecycle.*
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.Domain.Enum.Gender
import com.example.socialmedia.project.Domain.Enum.ThemePreference
import com.example.socialmedia.project.Fragment.State.Resource
import com.example.socialmedia.project.Repository.RegisterRepository
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val repository: RegisterRepository = RegisterRepository()
) : ViewModel() {

    private val _userTemp = MutableLiveData<UserModel>()
    val userTemp: LiveData<UserModel> get() = _userTemp

    private val _registerResult = MutableLiveData<Resource<String>>()
    val registerResult: LiveData<Resource<String>> get() = _registerResult

    private var tempEmail: String = ""
    private var tempPassword: String = ""

    fun setEmailAndPassword(email: String, password: String) {
        tempEmail = email
        tempPassword = password
    }

    fun setPersonalInfo(
        firstName: String,
        lastName: String,
        gender: Gender,
        dateOfBirth: Long?,
        phone: String?
    ) {
        _userTemp.value = UserModel(
            email = tempEmail,
            password = tempPassword,
            firstName = firstName,
            lastName = lastName,
            phoneNumber = phone,
            gender = gender,
            dateOfBirth = dateOfBirth,
            fullName = "$firstName $lastName",
            themePreference = ThemePreference.AUTO
        )
    }

    fun registerUser() {
        val user = _userTemp.value ?: return
        _registerResult.postValue(Resource.Loading())

        viewModelScope.launch {
            val result = repository.register(tempEmail, tempPassword, user)
            _registerResult.postValue(result)
        }
    }
}
