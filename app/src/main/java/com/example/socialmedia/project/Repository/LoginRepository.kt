package com.example.socialmedia.project.Repository

import com.example.socialmedia.project.Domain.UserModel
import com.example.socialmedia.project.Fragment.State.Resource
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class LoginRepository {

    private val auth = FirebaseAuth.getInstance()

    suspend fun login(email: String, password: String): Resource<UserModel> {
        return try {
            // Đăng nhập bằng Firebase Auth
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Resource.Error("Không thể lấy thông tin người dùng.")

            // Nếu có user, tạo UserModel
            val userModel = UserModel(
                userId = user.uid,
                email = user.email ?: email,
                fullName = user.displayName ?: "Unknown User",
                password = password,
                firstName = "",
                lastName = "",
                phoneNumber = user.phoneNumber ?: "",
                dateOfBirth = 0L,
                gender = "",
                profilePicture = user.photoUrl?.toString() ?: "",
                bio = "",
                createdAt = System.currentTimeMillis()
            )

            Resource.Success(userModel)

        } catch (e: Exception) {
            // Kiểm tra lỗi cụ thể từ Firebase
            val message = when {
                e.message?.contains("no user record", true) == true -> "Tài khoản không tồn tại."
                e.message?.contains("password is invalid", true) == true -> "Mật khẩu không chính xác."
                else -> e.message ?: "Lỗi không xác định."
            }
            Resource.Error(message)
        }
    }
}
