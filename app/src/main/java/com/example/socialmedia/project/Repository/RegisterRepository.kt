package com.example.socialmedia.project.Repository

import android.util.Log
import com.example.socialmedia.project.Domain.UserModel
import com.example.socialmedia.project.Fragment.State.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class RegisterRepository {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    suspend fun register(email: String, password: String, userInfo: UserModel): Resource<String> {
        return try {
            if (!email.contains("@") || !email.contains(".")) {
                return Resource.Error("Email không hợp lệ. Vui lòng nhập đầy đủ, ví dụ: tennguoidung@gmail.com")
            }

            // ✅ Kiểm tra xem email đã tồn tại trong Firebase Auth chưa
            val existingMethods = auth.fetchSignInMethodsForEmail(email).await().signInMethods
            if (!existingMethods.isNullOrEmpty()) {
                Log.e("RegisterRepository", "❌ Email đã tồn tại: $email")
                return Resource.Error("Email này đã được đăng ký. Vui lòng chọn email khác.")
            }

            // 1️⃣ Tạo tài khoản Firebase Auth
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Resource.Error("Không thể lấy UID")

            // 2️⃣ Thêm user vào Realtime Database
            val userWithId = userInfo.copy(userId = uid)
            database.child("InfoUser").child(uid).setValue(userWithId).await()

            // 3️⃣ Đăng xuất user hiện tại để quay về LoginFragment
            auth.signOut()

            Log.i("RegisterRepository", "✅ Tạo user thành công: $email (UID: $uid)")
            Resource.Success(uid)
        } catch (e: Exception) {
            Log.e("RegisterRepository", "❌ Lỗi khi đăng ký: ${e.message}")
            Resource.Error(e.message ?: "Lỗi không xác định")
        }
    }

}
