package com.example.socialmedia.project.Domain.Model

import com.example.socialmedia.project.Domain.Enum.Gender
import com.example.socialmedia.project.Domain.Enum.ThemePreference
import com.google.android.gms.common.internal.AccountType
import java.util.UUID

data class UserModel(
    val userId: String = UUID.randomUUID().toString(),
    val username: String = "",
    val email: String = "",
    val phoneNumber: String? = null,
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val fullName: String = "$firstName $lastName",
    val bio: String? = null,
    val profilePictureUrl: String? = null,
    val website: String? = null,
    val gender: Gender = Gender.PREFER_NOT_TO_SAY,
    val dateOfBirth: Long? = null,
    val isPrivate: Boolean = false,
    val isVerified: Boolean = false,
    val isActive: Boolean = true,
//    val accountType: AccountType = AccountType.PERSONAL,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastLogin: Long? = null,
    val languagePreference: String = "vi",
    val themePreference: ThemePreference = ThemePreference.AUTO,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val postCount: Int = 0
)
