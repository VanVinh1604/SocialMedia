package com.example.socialmedia.Fragment.Domain

// User entity mở rộng cho đăng ký/đăng nhập
data class UserModel(
    val userId: String = "",
    val email: String = "",
    val password: String = "",      // Nếu dùng Firebase Auth, có thể bỏ
    val firstName: String = "",
    val lastName: String = "",
    val fullName: String = "$firstName $lastName",
    val phoneNumber: String? = null,
    val dateOfBirth: Long? = null,  // lưu timestamp
    val gender: String? = null,     // male, female, other
    val profilePicture: String? = null,
    val bio: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

