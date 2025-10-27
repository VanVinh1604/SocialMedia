package com.example.socialmedia.utils

import com.example.socialmedia.project.Domain.Model.UserModel

/**
 * Extension functions cho UserModel
 * File này chứa các hàm tiện ích để xử lý UserModel
 *
 * Đặt tại: app/src/main/java/com/example/socialmedia/utils/UserExtensions.kt
 */

/**
 * Lấy tên đầy đủ từ UserModel
 * Ưu tiên: fullName > firstName + lastName > "Unknown User"
 */
fun UserModel.getDisplayName(): String {
    return when {
        fullName.isNotBlank() -> fullName
        firstName.isNotBlank() || lastName.isNotBlank() -> "${firstName} ${lastName}".trim()
        else -> "Unknown User"
    }
}

/**
 * Lấy avatar URL (hoặc empty string nếu null)
 */
fun UserModel.getAvatarUrl(): String {
    return profilePictureUrl ?: ""
}

/**
 * Kiểm tra xem user có được verify và active không
 */
fun UserModel.isVerifiedUser(): Boolean {
    return Verified && Active
}

/**
 * Lấy thông tin tóm tắt để hiển thị trong post
 * Return: Triple(userId, displayName, avatarUrl)
 */
fun UserModel.toPostUserInfo(): Triple<String, String, String> {
    return Triple(
        userId,
        getDisplayName(),
        getAvatarUrl()
    )
}

/**
 * Format số lượng (followers/following/posts) thành dạng ngắn gọn
 * VD: 1500 -> 1.5K, 1000000 -> 1.0M
 */
fun Int.formatCount(): String {
    return when {
        this >= 1_000_000 -> String.format("%.1fM", this / 1_000_000.0)
        this >= 1_000 -> String.format("%.1fK", this / 1_000.0)
        else -> this.toString()
    }
}

/**
 * Extension cho UserModel để format các số đếm
 */
fun UserModel.getFormattedFollowerCount(): String = followerCount.formatCount()
fun UserModel.getFormattedFollowingCount(): String = followingCount.formatCount()
fun UserModel.getFormattedPostCount(): String = postCount.formatCount()

/**
 * Kiểm tra xem profile có hoàn chỉnh không
 */
fun UserModel.isProfileComplete(): Boolean {
    return firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            email.isNotBlank()
}

/**
 * Lấy initials (chữ cái đầu) từ tên
 * VD: "Nguyễn Văn A" -> "NA"
 */
fun UserModel.getInitials(): String {
    val name = getDisplayName()
    return name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
}