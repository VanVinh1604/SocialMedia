package com.example.socialmedia.project.ViewModel

sealed class UploadProgress {
    object Idle : UploadProgress()
    object GettingUserInfo : UploadProgress()
    data class UploadingImages(val current: Int, val total: Int, val progress: Int) : UploadProgress()
    data class UploadingReel(val progress: Int) : UploadProgress()
    object SavingPost : UploadProgress()
    object SavingReel : UploadProgress()
    object SavingDraft : UploadProgress()
}