package com.example.socialmedia.Fragment.Repository

import android.R
import com.example.socialmedia.Fragment.Domain.UserModel
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import kotlin.String

class LoginRepository {

//    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun login(email: String, password: String): UserModel? {
        return try {
         //   val result = auth.signInWithEmailAndPassword(email, password).await()
         //   result.user
            return  UserModel(
                userId = "U001",
                email = "example@gmail.com",
                password = "123456",  // Nếu bạn không dùng Firebase Auth, có thể bỏ
                firstName = "Vinh",
                lastName = "Van",
                fullName = "Vinh Van",
                phoneNumber = "0123456789",
                dateOfBirth = 1104537600000, // 02/01/2005 (timestamp ví dụ)
                gender = "male",
                profilePicture = "https://example.com/avatar.jpg",
                bio = "Hello! I love coding and coffee ☕",
                createdAt = System.currentTimeMillis()
            )

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun register(email: String, password: String): UserModel? {
        return try {
//            val result = auth.createUserWithEmailAndPassword(email, password).await()
//            result.user
            return UserModel(
                userId = "U00" + (100..999).random().toString(), // random id cho vui
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
                createdAt = System.currentTimeMillis())

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
