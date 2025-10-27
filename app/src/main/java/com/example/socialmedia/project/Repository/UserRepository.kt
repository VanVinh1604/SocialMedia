package com.example.socialmedia.project.Repository

import android.util.Log
import com.example.socialmedia.project.Domain.Model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    /**
     * 📥 Lấy thông tin người dùng hiện tại
     */
    suspend fun getCurrentUser(): UserModel? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snapshot = database.child("InfoUser").child(uid).get().await()
            snapshot.getValue(UserModel::class.java)
        } catch (e: Exception) {
            Log.e("UserRepository", "❌ Lỗi khi lấy user hiện tại: ${e.message}")
            null
        }
    }

    /**
     * 📥 Lấy thông tin user theo ID (dùng khi hiển thị bài đăng, comment, follower,...)
     */
    suspend fun getUserById(userId: String): UserModel? {
        return try {
            val snapshot = database.child("InfoUser").child(userId).get().await()
            snapshot.getValue(UserModel::class.java)
        } catch (e: Exception) {
            Log.e("UserRepository", "❌ Lỗi khi lấy user theo ID: ${e.message}")
            null
        }
    }

    /**
     * 📤 Cập nhật thông tin người dùng (ví dụ sửa bio, tên, avatar,...)
     */
    suspend fun updateUserInfo(userId: String, newInfo: Map<String, Any>): Boolean {
        return try {
            database.child("InfoUser").child(userId).updateChildren(newInfo).await()
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "❌ Lỗi khi cập nhật user: ${e.message}")
            false
        }
    }

    /**
     * 🔍 Lấy danh sách tất cả user (phục vụ tìm kiếm, gợi ý kết bạn,...)
     */
    suspend fun getAllUsers(): List<UserModel> {
        return try {
            val snapshot = database.child("InfoUser").get().await()
            snapshot.children.mapNotNull { it.getValue(UserModel::class.java) }
        } catch (e: Exception) {
            Log.e("UserRepository", "❌ Lỗi khi lấy danh sách user: ${e.message}")
            emptyList()
        }
    }

    /**
     * ❌ Xóa user (chỉ dùng khi cần quản trị hoặc xóa tài khoản)
     */
    suspend fun deleteUser(userId: String): Boolean {
        return try {
            database.child("InfoUser").child(userId).removeValue().await()
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "❌ Lỗi khi xóa user: ${e.message}")
            false
        }
    }
}
