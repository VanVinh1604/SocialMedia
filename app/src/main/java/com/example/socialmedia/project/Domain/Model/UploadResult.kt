package com.example.socialmedia.project.ViewModel

sealed class UploadResult {
    object Idle : UploadResult()
    object Success : UploadResult()
    object ReelSuccess : UploadResult()
    object DraftSaved : UploadResult()
    data class Error(val message: String) : UploadResult()
}