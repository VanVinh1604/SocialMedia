package com.example.socialmedia.project.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedUserViewModel : ViewModel() {

    // LiveData giữ user id được chọn
    private val _otherUserId = MutableLiveData<String>()
    val otherUserId: LiveData<String> get() = _otherUserId

    fun setOtherUserId(userId: String) {
        _otherUserId.value = userId
    }
}
