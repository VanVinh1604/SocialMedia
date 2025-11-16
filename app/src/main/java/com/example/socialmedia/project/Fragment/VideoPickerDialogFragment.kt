package com.example.socialmedia.project.Fragment

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmedia.R
import com.example.socialmedia.databinding.DialogVideoPickerBinding
import com.example.socialmedia.project.Domain.Model.MediaItem
import com.example.socialmedia.project.Domain.Enum.MediaType

class VideoPickerDialogFragment : DialogFragment() {

    private var _binding: DialogVideoPickerBinding? = null
    private val binding get() = _binding!!

    private lateinit var videoAdapter: VideoAdapter
    private val allVideos = mutableListOf<VideoItem>()
    private val selectedVideos = mutableListOf<VideoItem>()

    private var maxSelection = 10
    private var maxDuration = 60000L // 60 seconds
    private var onVideosSelectedListener: ((List<MediaItem>) -> Unit)? = null

    companion object {
        const val TAG = "VideoPickerDialog"
        private const val ARG_MAX_SELECTION = "max_selection"
        private const val ARG_MAX_DURATION = "max_duration"

        fun newInstance(
            maxSelection: Int = 10,
            maxDuration: Long = 60000L
        ): VideoPickerDialogFragment {
            return VideoPickerDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_MAX_SELECTION, maxSelection)
                    putLong(ARG_MAX_DURATION, maxDuration)
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadVideos()
        } else {
            Toast.makeText(context, "Cần cấp quyền truy cập video", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)

        arguments?.let {
            maxSelection = it.getInt(ARG_MAX_SELECTION, 10)
            maxDuration = it.getLong(ARG_MAX_DURATION, 60000L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogVideoPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupListeners()
        checkPermissionAndLoadVideos()
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

    private fun setupUI() {
        binding.txtTitle.text = "Chọn video (Tối đa ${maxDuration / 1000}s)"

        videoAdapter = VideoAdapter(
            onVideoClick = { video ->
                handleVideoSelection(video)
            }
        )

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = videoAdapter
        }

        updateSelectionCount()
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnDone.setOnClickListener {
            if (selectedVideos.isNotEmpty()) {
                val mediaItems = selectedVideos.map { video ->
                    MediaItem(
                        uri = video.uri,
                        type = MediaType.VIDEO,
                        duration = video.duration
                    )
                }
                onVideosSelectedListener?.invoke(mediaItems)
                dismiss()
            } else {
                Toast.makeText(context, "Vui lòng chọn ít nhất 1 video", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissionAndLoadVideos() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                loadVideos()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun loadVideos() {
        binding.progressBar.visibility = View.VISIBLE

        allVideos.clear()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        requireContext().contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getLong(durationColumn)
                val size = cursor.getLong(sizeColumn)

                val uri = Uri.withAppendedPath(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )

                // Filter by max duration
                if (duration <= maxDuration) {
                    allVideos.add(
                        VideoItem(
                            uri = uri,
                            name = name,
                            duration = duration,
                            size = size
                        )
                    )
                }
            }
        }

        binding.progressBar.visibility = View.GONE
        videoAdapter.submitList(allVideos)

        if (allVideos.isEmpty()) {
            binding.txtEmpty.visibility = View.VISIBLE
            binding.txtEmpty.text = "Không tìm thấy video (tối đa ${maxDuration / 1000}s)"
        } else {
            binding.txtEmpty.visibility = View.GONE
        }
    }

    private fun handleVideoSelection(video: VideoItem) {
        if (selectedVideos.contains(video)) {
            selectedVideos.remove(video)
            video.isSelected = false
        } else {
            if (selectedVideos.size >= maxSelection) {
                Toast.makeText(
                    context,
                    "Chỉ được chọn tối đa $maxSelection video",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            selectedVideos.add(video)
            video.isSelected = true
        }

        videoAdapter.notifyDataSetChanged()
        updateSelectionCount()
    }

    private fun updateSelectionCount() {
        binding.txtSelectionCount.text = "${selectedVideos.size}/$maxSelection video đã chọn"
        binding.btnDone.isEnabled = selectedVideos.isNotEmpty()
    }

    fun setOnVideosSelectedListener(listener: (List<MediaItem>) -> Unit) {
        onVideosSelectedListener = listener
    }

    // Data class for video item
    data class VideoItem(
        val uri: Uri,
        val name: String,
        val duration: Long,
        val size: Long,
        var isSelected: Boolean = false
    )

    // Adapter for video grid
    inner class VideoAdapter(
        private val onVideoClick: (VideoItem) -> Unit
    ) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

        private val videos = mutableListOf<VideoItem>()

        fun submitList(newVideos: List<VideoItem>) {
            videos.clear()
            videos.addAll(newVideos)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_video_picker, parent, false)
            return VideoViewHolder(view)
        }

        override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
            holder.bind(videos[position])
        }

        override fun getItemCount(): Int = videos.size

        inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val videoThumbnail: android.widget.ImageView =
                itemView.findViewById(R.id.videoThumbnail)
            private val checkIcon: android.widget.ImageView =
                itemView.findViewById(R.id.checkIcon)
            private val durationText: android.widget.TextView =
                itemView.findViewById(R.id.durationText)
            private val playIcon: android.widget.ImageView =
                itemView.findViewById(R.id.playIcon)
            private val checkOverlay: View =
                itemView.findViewById(R.id.checkOverlay)

            fun bind(video: VideoItem) {
                // Load thumbnail using Glide
                com.bumptech.glide.Glide.with(itemView.context)
                    .load(video.uri)
                    .centerCrop()
                    .into(videoThumbnail)

                // Show duration
                durationText.text = formatDuration(video.duration)

                // Show selection state
                checkOverlay.visibility = if (video.isSelected) View.VISIBLE else View.GONE
                checkIcon.visibility = if (video.isSelected) View.VISIBLE else View.GONE

                itemView.setOnClickListener {
                    onVideoClick(video)
                }
            }

            private fun formatDuration(durationMs: Long): String {
                val seconds = (durationMs / 1000) % 60
                val minutes = (durationMs / (1000 * 60)) % 60
                return String.format("%02d:%02d", minutes, seconds)
            }
        }
    }
}