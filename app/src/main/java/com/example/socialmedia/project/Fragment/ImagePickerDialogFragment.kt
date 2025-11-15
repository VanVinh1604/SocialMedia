package com.example.socialmedia.project.Fragment

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmedia.R
import com.example.socialmedia.project.Adapter.ImagePickerAdapter
import kotlinx.coroutines.*

class ImagePickerDialogFragment : DialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnConfirm: FrameLayout

    private lateinit var btnCancel: TextView
    private lateinit var txtSelectedCount: TextView

    private val allImages = mutableListOf<Uri>()
    private val selectedImages = mutableListOf<Uri>()
    private lateinit var adapter: ImagePickerAdapter

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    private var maxSelection: Int = 10
    private var onImagesSelectedCallback: ((List<Uri>) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        // Get arguments
        arguments?.let {
            maxSelection = it.getInt(ARG_MAX_SELECTION, 10)
            it.getParcelableArrayList<Uri>(ARG_SELECTED_IMAGES)?.let { uris ->
                selectedImages.addAll(uris)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_image_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerView)
        btnConfirm = view.findViewById(R.id.btnConfirm)
        btnCancel = view.findViewById(R.id.btnCancel)
        txtSelectedCount = view.findViewById(R.id.txtSelectedCount)

        // Setup RecyclerView
        setupRecyclerView()

        // Setup buttons
        setupButtons()

        // Load images
        loadImagesFromGallery()

        // Update selection count
        updateSelectionCount()
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = ImagePickerAdapter(
            images = allImages,
            selectedImages = selectedImages,
            maxSelection = maxSelection
        ) { count ->
            updateSelectionCount()
        }
        recyclerView.adapter = adapter
    }

    private fun setupButtons() {
        btnConfirm.setOnClickListener {
            if (selectedImages.isEmpty()) {
                Toast.makeText(context, "Vui lòng chọn ít nhất 1 ảnh", Toast.LENGTH_SHORT).show()
            } else {
                onImagesSelectedCallback?.invoke(selectedImages.toList())
                dismiss()
            }
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun updateSelectionCount() {
        txtSelectedCount.text = "Đã chọn ${selectedImages.size}/$maxSelection ảnh"
        btnConfirm.isEnabled = selectedImages.isNotEmpty()
        btnConfirm.alpha = if (selectedImages.isNotEmpty()) 1.0f else 0.5f
    }

    private fun loadImagesFromGallery() {
        coroutineScope.launch {
            val images = withContext(Dispatchers.IO) {
                fetchImagesFromMediaStore()
            }

            allImages.clear()
            allImages.addAll(images)
            adapter.notifyDataSetChanged()

            if (images.isEmpty()) {
                Toast.makeText(context, "Không tìm thấy ảnh nào", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchImagesFromMediaStore(): List<Uri> {
        val imageList = mutableListOf<Uri>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val cursor: Cursor? = requireContext().contentResolver.query(
            uri,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                imageList.add(contentUri)
            }
        }

        return imageList
    }

    fun setOnImagesSelectedListener(callback: (List<Uri>) -> Unit) {
        onImagesSelectedCallback = callback
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel()
    }

    companion object {
        const val TAG = "ImagePickerDialog"
        private const val ARG_MAX_SELECTION = "max_selection"
        private const val ARG_SELECTED_IMAGES = "selected_images"

        fun newInstance(
            alreadySelectedImages: List<Uri>,
            maxSelection: Int = 10
        ): ImagePickerDialogFragment {
            return ImagePickerDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_MAX_SELECTION, maxSelection)
                    putParcelableArrayList(ARG_SELECTED_IMAGES, ArrayList(alreadySelectedImages))
                }
            }
        }
    }
}