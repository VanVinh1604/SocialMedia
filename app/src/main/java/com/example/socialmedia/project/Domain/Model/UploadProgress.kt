package com.example.socialmedia.project.ViewModel

sealed class UploadProgress {
    object Idle : UploadProgress()
    object GettingUserInfo : UploadProgress()
    data class UploadingImages(val current: Int, val total: Int, val progress: Int) : UploadProgress()
    object SavingPost : UploadProgress()
    object SavingDraft : UploadProgress()
}