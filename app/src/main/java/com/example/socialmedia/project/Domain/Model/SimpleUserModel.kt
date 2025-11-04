package com.example.socialmedia.project.Domain.Model



data class SimpleUserModel(
    var userId: String = "",
    var fullName: String = "",
    var email: String = "",
    var bio: String? = null,
    var profilePictureUrl: String? = null
) {
    // Convert từ UserModel sang SimpleUserModel
    companion object {
        fun fromUserModel(user: UserModel): SimpleUserModel {
            return SimpleUserModel(
                userId = user.userId,
                fullName = user.fullName,
                email = user.email,
                bio = user.bio,
                profilePictureUrl = user.profilePictureUrl
            )
        }
    }

    // Convert sang UserModel (chỉ các field cần thiết)
    fun toUserModel(): UserModel {
        return UserModel(
            userId = this.userId,
            fullName = this.fullName,
            email = this.email,
            bio = this.bio,
            profilePictureUrl = this.profilePictureUrl
        )
    }
}