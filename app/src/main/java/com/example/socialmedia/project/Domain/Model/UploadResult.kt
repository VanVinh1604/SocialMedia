package com.example.socialmedia.project.ViewModel

sealed class UploadResult {
    object Idle : UploadResult()
    data class Success(val postId: String) : UploadResult()
    data class DraftSaved(val postId: String) : UploadResult()
    data class Error(val message: String) : UploadResult()
}