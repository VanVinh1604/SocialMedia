package com.example.socialmedia.project.ViewModel // (Hoặc package ViewModel của bạn)

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class PrivacyViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val currentUid = auth.currentUser?.uid

    // LiveData cho trạng thái "Tài khoản riêng tư"
    private val _isPrivateAccount = MutableLiveData<Boolean>()
    val isPrivateAccount: LiveData<Boolean> get() = _isPrivateAccount

    // LiveData cho trạng thái "Hiển thị hoạt động"
    private val _showActivityStatus = MutableLiveData<Boolean>()
    val showActivityStatus: LiveData<Boolean> get() = _showActivityStatus

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> get() = _toastMessage

    private val infoUserRef = currentUid?.let { database.reference.child("InfoUser").child(it) }

    init {
        loadPrivacySettings()
    }

    private fun loadPrivacySettings() {
        _isLoading.postValue(true)
        if (infoUserRef == null) {
            _isLoading.postValue(false)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Tải trạng thái Private
                val privateSnap = infoUserRef.child("Private").get().await()
                _isPrivateAccount.postValue(privateSnap.getValue(Boolean::class.java) ?: false)

                // Tải trạng thái Activity Status
                val activitySnap = infoUserRef.child("showActivityStatus").get().await()
                _showActivityStatus.postValue(activitySnap.getValue(Boolean::class.java) ?: true) // Mặc định là true

                _isLoading.postValue(false)
            } catch (e: Exception) {
                Log.e("PrivacyViewModel", "Lỗi tải cài đặt: ${e.message}")
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Cập nhật trạng thái "Tài khoản riêng tư"
     */
    fun setPrivateAccount(isPrivate: Boolean) {
        if (infoUserRef == null) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                infoUserRef.child("Private").setValue(isPrivate).await()
                _isPrivateAccount.postValue(isPrivate)
                _toastMessage.postValue(if (isPrivate) "Đã bật tài khoản riêng tư" else "Đã tắt tài khoản riêng tư")
            } catch (e: Exception) {
                _toastMessage.postValue("Lỗi: ${e.message}")
            }
        }
    }

    /**
     * Cập nhật trạng thái "Hiển thị hoạt động"
     */
    fun setActivityStatus(showStatus: Boolean) {
        if (infoUserRef == null) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                infoUserRef.child("showActivityStatus").setValue(showStatus).await()
                _showActivityStatus.postValue(showStatus)
                _toastMessage.postValue(if (showStatus) "Đã bật trạng thái hoạt động" else "Đã tắt trạng thái hoạt động")
            } catch (e: Exception) {
                _toastMessage.postValue("Lỗi: ${e.message}")
            }
        }
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }
}