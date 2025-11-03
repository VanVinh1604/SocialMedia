package com.example.socialmedia.project.Utils

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object ChatCloudinaryHelper {

    private const val CLOUD_NAME = "demhfqz6u"
    private const val UPLOAD_PRESET = "voice_messages"
    private const val CLOUDINARY_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/upload"
    private const val TAG = "CloudinaryHelper"

    suspend fun uploadFile(file: File, resourceType: String = "video"): String? = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) {
                Log.e(TAG, "File không tồn tại: ${file.absolutePath}")
                return@withContext null
            }

            Log.d(TAG, "Bắt đầu upload: ${file.name}, size: ${file.length()} bytes")

            val client = OkHttpClient()

            val mimeType = when (file.extension.lowercase()) {
                "mp3" -> "audio/mpeg"
                "m4a", "mp4" -> "audio/mp4"
                "wav" -> "audio/wav"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                else -> "application/octet-stream"
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody(mimeType.toMediaTypeOrNull()))
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .addFormDataPart("resource_type", resourceType)
                .build()

            val request = Request.Builder()
                .url(CLOUDINARY_URL)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(response.body?.string() ?: "{}")
                val secureUrl = jsonResponse.getString("secure_url")
                Log.d(TAG, "Upload thành công: $secureUrl")
                Log.d(TAG, "Full response: $jsonResponse")
                secureUrl
            } else {
                Log.e(TAG, "Upload thất bại: ${response.code} - ${response.message}")
                Log.e(TAG, "Response body: ${response.body?.string()}")
                null
            }

        } catch (e: IOException) {
            Log.e(TAG, "Lỗi network khi upload", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi không xác định khi upload", e)
            null
        }
    }

    suspend fun uploadImage(uri: Uri, context: Context): String? = withContext(Dispatchers.IO) {
        try {
            // Copy URI sang file cache để upload
            val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)
            outputStream.close()
            inputStream.close()

            uploadImage(tempFile).also {
                tempFile.delete() // xóa file tạm sau khi upload
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi upload ảnh từ URI", e)
            null
        }
    }


    suspend fun uploadVoiceMessage(file: File): String? = uploadFile(file, "video")
    suspend fun uploadImage(file: File): String? = uploadFile(file, "image")
    fun isConfigured(): Boolean = CLOUD_NAME != "your_cloud_name" && UPLOAD_PRESET != "your_upload_preset"
}
