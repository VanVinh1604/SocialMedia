package com.example.socialmedia.project.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    // LiveData để giao tiếp với Fragment
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _changeSuccess = MutableLiveData<Boolean>(false)
    val changeSuccess: LiveData<Boolean> = _changeSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Hàm chính để thay đổi mật khẩu trên Firebase
     */
    fun updateFirebasePassword(currentPass: String, newPass: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)

            try {
                val user = auth.currentUser
                if (user == null || user.email == null) {
                    throw Exception("User not found or email is missing")
                }

                // 1. Lấy thông tin đăng nhập (credential)
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPass)

                // 2. Yêu cầu Firebase xác thực lại (kiểm tra mật khẩu cũ)
                user.reauthenticate(credential).await()

                // 3. Nếu xác thực thành công, cập nhật mật khẩu mới trong Auth
                user.updatePassword(newPass).await()

                // 4. Cập nhật mật khẩu (plain text) trong Realtime Database
                // (Lưu ý: Đây là một rủi ro bảo mật, nhưng làm theo cấu trúc DB của bạn)
                database.child("InfoUser").child(user.uid).child("password").setValue(newPass).await()

                // 5. Báo thành công
                _isLoading.postValue(false)
                _changeSuccess.postValue(true)

            } catch (e: Exception) {
                _isLoading.postValue(false)
                when (e) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        _errorMessage.postValue("Mật khẩu hiện tại không đúng")
                    }
                    else -> {
                        _errorMessage.postValue(e.message ?: "Đã xảy ra lỗi không xác định")
                    }
                }
                Log.e("SettingViewModel", "Lỗi đổi mật khẩu: ", e)
            }
        }
    }

    /**
     * Reset thông báo lỗi sau khi đã hiển thị
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}

