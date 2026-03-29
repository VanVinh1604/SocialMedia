package com.example.socialmedia.project.Fragment

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.socialmedia.R
import com.example.socialmedia.databinding.DialogMediaPickerBinding
import com.example.socialmedia.project.Adapter.MediaPickerAdapter
import com.example.socialmedia.project.Domain.Enum.MediaType
import com.example.socialmedia.project.Domain.Enum.UploadType
import com.example.socialmedia.project.Domain.Model.MediaItem
import com.google.android.material.tabs.TabLayout

class MediaPickerDialogFragment : DialogFragment() {

    private var _binding: DialogMediaPickerBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MediaPickerAdapter
    private val allMediaItems = mutableListOf<MediaItem>()
    private val selectedItems = mutableListOf<MediaItem>()

    private var uploadType = UploadType.POST
    private var maxSelection = 10
    private var maxDuration = 60000L

    private var onMediaSelectedListener: ((List<MediaItem>, UploadType) -> Unit)? = null

    companion object {
        const val TAG = "MediaPickerDialog"

        fun newInstance(
            uploadType: UploadType = UploadType.POST,
            maxSelection: Int = 10,
            maxDuration: Long = 60000L,
            alreadySelected: List<Uri> = emptyList()
        ): MediaPickerDialogFragment {
            return MediaPickerDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("uploadType", uploadType.name)
                    putInt("maxSelection", maxSelection)
                    putLong("maxDuration", maxDuration)
                    putParcelableArrayList("alreadySelected", ArrayList(alreadySelected))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)

        arguments?.let {
            uploadType = UploadType.valueOf(it.getString("uploadType", "POST"))
            maxSelection = it.getInt("maxSelection", 10)
            maxDuration = it.getLong("maxDuration", 60000L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogMediaPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupTabs()
        setupRecyclerView()
        setupButtons()

        checkPermissionAndLoadMedia()
    }

    private fun setupToolbar() {
        binding.txtTitle.text = when (uploadType) {
            UploadType.POST -> "Bài viết mới"
            UploadType.REEL -> "Reel mới"
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        updateSelectionCount()
    }

    private fun setupTabs() {
        // Add tabs
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("BÀI VIẾT"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("TIN"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("THƯỚC PHIM"))

        // Set selected tab based on upload type
        when (uploadType) {
            UploadType.POST -> binding.tabLayout.getTabAt(0)?.select()
            UploadType.REEL -> binding.tabLayout.getTabAt(2)?.select()
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        uploadType = UploadType.POST
                        maxSelection = 10
                        binding.txtTitle.text = "Bài viết mới"
                        adapter.setMaxSelection(maxSelection)
                        filterMediaByType()
                    }
                    1 -> {
                        Toast.makeText(context, "Tính năng Tin đang phát triển", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        uploadType = UploadType.REEL
                        maxSelection = 1
                        binding.txtTitle.text = "Reel mới"
                        adapter.setMaxSelection(maxSelection)
                        filterMediaByType()
                    }
                }
                updateSelectionCount()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        adapter = MediaPickerAdapter(
            mediaItems = allMediaItems,
            selectedItems = selectedItems,
            maxSelection = maxSelection,
            onSelectionChanged = { updateSelectionCount() }
        )

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerView.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnConfirm.setOnClickListener {
            if (selectedItems.isEmpty()) {
                Toast.makeText(context, "Vui lòng chọn ít nhất 1 media", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate for REEL
            if (uploadType == UploadType.REEL) {
                if (selectedItems.size > 1) {
                    Toast.makeText(context, "Reel chỉ cho phép 1 video", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (selectedItems[0].type != MediaType.VIDEO) {
                    Toast.makeText(context, "Reel phải là video", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val duration = selectedItems[0].duration ?: 0
                if (duration > 90000L) {
                    Toast.makeText(context, "Video không được dài quá 90 giây", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            onMediaSelectedListener?.invoke(selectedItems.toList(), uploadType)
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun checkPermissionAndLoadMedia() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission)
            == PackageManager.PERMISSION_GRANTED) {
            loadAllMedia()
        } else {
            Toast.makeText(context, "Cần cấp quyền truy cập media", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun loadAllMedia() {
        binding.progressBar.visibility = View.VISIBLE
        allMediaItems.clear()

        // Load images
        loadImages()

        // Load videos
        loadVideos()

        binding.progressBar.visibility = View.GONE

        if (allMediaItems.isEmpty()) {
            binding.txtEmpty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.txtEmpty.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            filterMediaByType()
        }
    }

    private fun loadImages() {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor: Cursor? = requireContext().contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
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

                allMediaItems.add(MediaItem(contentUri, MediaType.IMAGE))
            }
        }
    }

    private fun loadVideos() {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        val cursor: Cursor? = requireContext().contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val duration = it.getLong(durationColumn)

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                allMediaItems.add(MediaItem(contentUri, MediaType.VIDEO, duration))
            }
        }
    }

    private fun filterMediaByType() {
        val filteredList = when (uploadType) {
            UploadType.POST -> {
                // Show all media for posts
                allMediaItems
            }
            UploadType.REEL -> {
                // Show only videos for reels with max 90s duration
                allMediaItems.filter {
                    it.type == MediaType.VIDEO && (it.duration ?: 0) <= 90000L
                }
            }
        }

        adapter.updateMediaList(filteredList)
    }

    private fun updateSelectionCount() {
        val count = selectedItems.size
        binding.txtSelectedCount.text = "Đã chọn $count/$maxSelection"

        binding.btnConfirm.isEnabled = count > 0
        binding.btnConfirm.alpha = if (count > 0) 1f else 0.5f
    }

    fun setOnMediaSelectedListener(listener: (List<MediaItem>, UploadType) -> Unit) {
        onMediaSelectedListener = listener
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}