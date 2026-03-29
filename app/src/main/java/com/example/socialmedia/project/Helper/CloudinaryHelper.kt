package com.example.socialmedia.project.Helper

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CloudinaryHelper {
    private const val TAG = "CloudinaryHelper"
    private var isInitialized = false

    fun initialize(context: Context) {
        if (!isInitialized) {
            try {
                val config = mapOf(
                    "cloud_name" to CloudinaryConfig.CLOUD_NAME,
                    "api_key" to CloudinaryConfig.API_KEY,
                    "api_secret" to CloudinaryConfig.API_SECRET
                )
                MediaManager.init(context, config)
                isInitialized = true
                Log.d(TAG, "✅ Cloudinary initialized successfully")
                Log.d(TAG, "Cloud Name: ${CloudinaryConfig.CLOUD_NAME}")
                Log.d(TAG, "Upload Preset: ${CloudinaryConfig.UPLOAD_PRESET}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error initializing Cloudinary", e)
            }
        }
    }

    suspend fun uploadImage(context: Context, imageUri: Uri): String = suspendCancellableCoroutine { continuation ->
        try {
            if (!isInitialized) {
                initialize(context)
            }

            Log.d(TAG, "📤 Starting image upload for: $imageUri")

            val requestId = MediaManager.get().upload(imageUri)
                .unsigned(CloudinaryConfig.UPLOAD_PRESET)
                .option("folder", "social_media_posts")
                .option("resource_type", "image")
                .option("quality", "auto")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d(TAG, "🚀 Image upload started: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = (bytes.toDouble() / totalBytes.toDouble() * 100).toInt()
                        Log.d(TAG, "📊 Image upload progress: $progress% ($bytes/$totalBytes bytes)")
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        if (url != null) {
                            Log.d(TAG, "✅ Image upload successful!")
                            Log.d(TAG, "🔗 Image URL: $url")
                            continuation.resume(url)
                        } else {
                            val error = Exception("URL not found in response")
                            Log.e(TAG, "❌ Image upload failed: URL not found in response")
                            continuation.resumeWithException(error)
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        val exception = Exception("Image upload failed: ${error.description} (Code: ${error.code})")
                        Log.e(TAG, "❌ Image upload error: ${error.description}")
                        Log.e(TAG, "Error code: ${error.code}")
                        continuation.resumeWithException(exception)
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w(TAG, "⏰ Image upload rescheduled: ${error.description}")
                    }
                })
                .dispatch()

            continuation.invokeOnCancellation {
                MediaManager.get().cancelRequest(requestId)
                Log.d(TAG, "🚫 Image upload cancelled: $requestId")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in uploadImage", e)
            continuation.resumeWithException(e)
        }
    }

    // ===== THÊM HÀM UPLOAD VIDEO =====
    suspend fun uploadVideo(context: Context, videoUri: Uri): String = suspendCancellableCoroutine { continuation ->
        try {
            if (!isInitialized) {
                initialize(context)
            }

            Log.d(TAG, "📤 Starting video upload for: $videoUri")

            val requestId = MediaManager.get().upload(videoUri)
                .unsigned(CloudinaryConfig.UPLOAD_PRESET)
                .option("folder", "social_media_posts")
                .option("resource_type", "video")  // ← Quan trọng: resource_type là "video"
                .option("quality", "auto")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d(TAG, "🚀 Video upload started: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = (bytes.toDouble() / totalBytes.toDouble() * 100).toInt()
                        Log.d(TAG, "📊 Video upload progress: $progress% ($bytes/$totalBytes bytes)")
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        if (url != null) {
                            Log.d(TAG, "✅ Video upload successful!")
                            Log.d(TAG, "🔗 Video URL: $url")
                            continuation.resume(url)
                        } else {
                            val error = Exception("URL not found in response")
                            Log.e(TAG, "❌ Video upload failed: URL not found in response")
                            continuation.resumeWithException(error)
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        val exception = Exception("Video upload failed: ${error.description} (Code: ${error.code})")
                        Log.e(TAG, "❌ Video upload error: ${error.description}")
                        Log.e(TAG, "Error code: ${error.code}")
                        continuation.resumeWithException(exception)
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w(TAG, "⏰ Video upload rescheduled: ${error.description}")
                    }
                })
                .dispatch()

            continuation.invokeOnCancellation {
                MediaManager.get().cancelRequest(requestId)
                Log.d(TAG, "🚫 Video upload cancelled: $requestId")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in uploadVideo", e)
            continuation.resumeWithException(e)
        }
    }
}