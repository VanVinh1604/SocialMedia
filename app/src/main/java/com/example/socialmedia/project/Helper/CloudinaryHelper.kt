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

            Log.d(TAG, "📤 Starting upload for: $imageUri")

            val requestId = MediaManager.get().upload(imageUri)
                .unsigned(CloudinaryConfig.UPLOAD_PRESET)
                .option("folder", "social_media_posts")
                .option("resource_type", "image")
                .option("quality", "auto")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d(TAG, "🚀 Upload started: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = (bytes.toDouble() / totalBytes.toDouble() * 100).toInt()
                        Log.d(TAG, "📊 Upload progress: $progress% ($bytes/$totalBytes bytes)")
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        if (url != null) {
                            Log.d(TAG, "✅ Upload successful!")
                            Log.d(TAG, "🔗 Image URL: $url")
                            continuation.resume(url)
                        } else {
                            val error = Exception("URL not found in response")
                            Log.e(TAG, "❌ Upload failed: URL not found in response")
                            continuation.resumeWithException(error)
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        val exception = Exception("Upload failed: ${error.description} (Code: ${error.code})")
                        Log.e(TAG, "❌ Upload error: ${error.description}")
                        Log.e(TAG, "Error code: ${error.code}")
                        continuation.resumeWithException(exception)
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w(TAG, "⏰ Upload rescheduled: ${error.description}")
                    }
                })
                .dispatch()

            continuation.invokeOnCancellation {
                MediaManager.get().cancelRequest(requestId)
                Log.d(TAG, "🚫 Upload cancelled: $requestId")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in uploadImage", e)
            continuation.resumeWithException(e)
        }
    }
}