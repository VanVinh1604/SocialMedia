package com.example.socialmedia.Fragment.Repository

import com.example.socialmedia.Fragment.Domain.UserModel
import kotlin.random.Random

class RegisterRepository {

    // Hàm register giả lập
    suspend fun register(email: String, password: String): UserModel? {
        return try {
            UserModel(
                userId = "U00" + Random.nextInt(100, 999),
                email = email,
                password = password,
                firstName = "New",
                lastName = "User",
                fullName = "New User",
                phoneNumber = "0987654321",
                dateOfBirth = null,
                gender = "other",
                profilePicture = "https://example.com/default_avatar.png",
                bio = "This is a newly registered account",
                createdAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
