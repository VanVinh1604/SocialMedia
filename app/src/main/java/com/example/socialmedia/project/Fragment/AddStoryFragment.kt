package com.example.socialmedia.project.Fragment

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.socialmedia.databinding.FragmentAddStoryBinding
import com.example.socialmedia.project.Adapter.ImagePreviewAdapter
import com.example.socialmedia.project.Server.Firebase.FirebaseService
import com.example.socialmedia.project.Utils.ChatCloudinaryHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AddStoryFragment : Fragment() {

    private lateinit var binding: FragmentAddStoryBinding
    private val allMedia = mutableListOf<Uri>()
    private val selectedMedia = mutableListOf<Uri>()
    private val maxSelection = 10
    private lateinit var cameraFile: File

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", cameraFile)
            selectedMedia.add(uri)
            binding.recyclerView.adapter?.notifyDataSetChanged()
            binding.txtSelectedCount.text = "Đã chọn ${selectedMedia.size}/$maxSelection"
        }
    }

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) loadAllMedia()
        else Toast.makeText(context, "Cần cấp quyền đọc ảnh/video và camera", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAddStoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = GridLayoutManager(context, 3)
        binding.recyclerView.adapter = ImagePreviewAdapter(allMedia, selectedMedia) { uri ->
            if (uri == null) {
                // Mở camera
                openCamera()
            } else {
                // Chọn/deselect ảnh
                binding.txtSelectedCount.text = "Đã chọn ${selectedMedia.size}/$maxSelection"
            }
        }


        binding.btnCancel.setOnClickListener { findNavController().navigateUp() }
        binding.btnConfirm.setOnClickListener { uploadSelectedMedia() }

        binding.txtTitle.setOnLongClickListener {
            openCamera()
            true
        }

        requestPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA))
    }

    private fun openCamera() {
        cameraFile = File(requireContext().cacheDir, "story_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", cameraFile)
        takePhoto.launch(uri)
    }

    private fun loadAllMedia() {
        lifecycleScope.launch {
            val uris = queryAllMedia(requireContext())
            allMedia.clear()
            allMedia.addAll(uris)
            binding.recyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private suspend fun queryAllMedia(context: Context): List<Uri> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<Uri>()

        // Load ảnh
        val imageCursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )
        imageCursor?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                mediaList.add(contentUri)
            }
        }

        // Load video
        val videoCursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Video.Media._ID),
            null, null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )
        videoCursor?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                mediaList.add(contentUri)
            }
        }

        mediaList.sortedByDescending { it.toString() } // Sắp xếp gần đây nhất lên đầu
    }

    private fun uploadSelectedMedia() {
        if (selectedMedia.isEmpty()) {
            Toast.makeText(context, "Chọn ít nhất 1 ảnh/video", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val firebaseService = FirebaseService()
            val auth = FirebaseAuth.getInstance()

            val results = selectedMedia.map { uri ->
                async(Dispatchers.IO) {
                    try {
                        val file = uriToFile(uri)
                        val url = ChatCloudinaryHelper.uploadStory(file)
                        if (url == null) {
                            Log.e("UploadStory", "❌ Upload Cloudinary thất bại: ${file.path}")
                            return@async false
                        }

                        // Gửi từng ảnh riêng biệt lên Firebase
                        suspendCancellableCoroutine<Boolean> { cont ->
                            firebaseService.uploadStoryToFirebase(
                                auth.currentUser!!.uid,
                                url,
                                if (uri.toString().endsWith("mp4")) "video" else "image"
                            ) { success ->
                                cont.resume(success) {}
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                }
            }.awaitAll()

            val successCount = results.count { it }
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "✅ Đã đăng $successCount/${selectedMedia.size} story",
                    Toast.LENGTH_SHORT
                ).show()
                if (successCount > 0) findNavController().navigateUp()
            }
        }
    }


    private fun uriToFile(uri: Uri): File {
        val mimeType = requireContext().contentResolver.getType(uri)
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
        val tempFile = File(requireContext().cacheDir, "story_${System.currentTimeMillis()}.$ext")
        requireContext().contentResolver.openInputStream(uri)!!.use { input ->
            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
        }
        return tempFile
    }
}
