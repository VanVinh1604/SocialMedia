package com.example.socialmedia.project.Repository

import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.Fragment.State.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class LoginRepository {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    suspend fun login(email: String, password: String): Resource<UserModel> {
        return try {
            // 1️⃣ Đăng nhập bằng Firebase Auth
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Resource.Error("Không thể lấy thông tin người dùng.")

            // 2️⃣ Lấy UID và đọc thêm dữ liệu trong Realtime Database
            val uid = user.uid
            val snapshot = database.child("InfoUser").child(uid).get().await()

            if (!snapshot.exists()) {
                // Nếu database chưa có, tạo UserModel cơ bản
                val userModel = UserModel(
                    userId = uid,
                    email = user.email ?: email,
                    fullName = user.displayName ?: "Unknown User",
                    password = password
                )
                return Resource.Success(userModel)
            }

            // 3️⃣ Parse data thành UserModel đầy đủ
            val userModel = snapshot.getValue(UserModel::class.java)
                ?: return Resource.Error("Không thể đọc dữ liệu người dùng từ database.")

            Resource.Success(userModel)

        } catch (e: Exception) {
            val message = when {
                e.message?.contains("no user record", true) == true -> "Tài khoản không tồn tại."
                e.message?.contains("password is invalid", true) == true -> "Mật khẩu không chính xác."
                else -> e.message ?: "Lỗi không xác định."
            }
            Resource.Error(message)
        }
    }
}
