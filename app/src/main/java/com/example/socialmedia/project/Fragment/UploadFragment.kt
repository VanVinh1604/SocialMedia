package com.example.socialmedia.project.Fragment

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentUploadBinding
import com.example.socialmedia.project.Domain.Model.MusicModel
import com.example.socialmedia.project.Domain.Model.MediaItem
import com.example.socialmedia.project.Domain.Enum.AudienceType
import com.example.socialmedia.project.Domain.Enum.MediaType
import com.example.socialmedia.project.Domain.Enum.UploadType
import com.example.socialmedia.project.ViewModel.UploadProgress
import com.example.socialmedia.project.ViewModel.UploadResult
import com.example.socialmedia.project.ViewModel.UploadViewModel


class UploadFragment : Fragment() {

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UploadViewModel by viewModels()
    private val selectedMedia = mutableListOf<MediaItem>()
    private val selectedImages = mutableListOf<Uri>()
    private var currentMediaIndex = 0
    private val MAX_CAPTION_LENGTH = 2200
    private val MAX_VIDEO_DURATION = 60000L // 60 seconds
    private val MAX_REEL_DURATION = 90000L // 90 seconds for reels
    private val MAX_MEDIA_COUNT = 10
    private var selectedMusic: MusicModel? = null
    private val taggedPeople = mutableListOf<String>()
    private var selectedLocation: String? = null
    private var audienceType = AudienceType.PUBLIC
    private val TAG = "UploadFragment"

    // Upload type
    private var uploadType = UploadType.POST // Default is POST

    // Reel settings
    private var allowsComments = true
    private var allowsDuet = true
    private var allowsRemix = true

    // Flags for media replacement
    private var isReplacingMedia = false
    private var replaceMediaIndex = -1

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showUploadTypeSelector()
        else {
            Toast.makeText(context, "Cần cấp quyền truy cập media", Toast.LENGTH_SHORT).show()
            showBottomNavigation()
            parentFragmentManager.popBackStack()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView called")
        _binding = FragmentUploadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")

        hideBottomNavigation()
        setupUI()
        setupListeners()
        observeViewModel()

        if (selectedMedia.isEmpty()) {
            checkPermissionAndOpenPicker()
        } else {
            updateUIWithMedia()
        }

        viewModel.musicList.observe(viewLifecycleOwner) {
            // Music list loaded
        }
    }

    override fun onResume() {
        super.onResume()
        hideBottomNavigation()
    }

    override fun onPause() {
        super.onPause()
        _binding?.let { binding ->
            if (binding.previewVideo.visibility == View.VISIBLE) {
                binding.previewVideo.pause()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showBottomNavigation()
        _binding = null
    }

    private fun hideBottomNavigation() {
        val bottomContainer = activity?.findViewById<View>(R.id.container)
        bottomContainer?.visibility = View.GONE

        val navHostFragment = activity?.findViewById<View>(R.id.navHostFragment)
        navHostFragment?.setPadding(
            navHostFragment.paddingLeft,
            navHostFragment.paddingTop,
            navHostFragment.paddingRight,
            0
        )

        Log.d(TAG, "Bottom navigation hidden")
    }

    private fun showBottomNavigation() {
        val bottomContainer = activity?.findViewById<View>(R.id.container)
        bottomContainer?.visibility = View.VISIBLE

        val navHostFragment = activity?.findViewById<View>(R.id.navHostFragment)
        val paddingBottom = (80 * resources.displayMetrics.density).toInt()
        navHostFragment?.setPadding(
            navHostFragment.paddingLeft,
            navHostFragment.paddingTop,
            navHostFragment.paddingRight,
            paddingBottom
        )

        Log.d(TAG, "Bottom navigation shown")
    }

    private fun setupUI() {
        binding.editCaption.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                binding.characterCount.text = "$length/$MAX_CAPTION_LENGTH"
                binding.characterCount.setTextColor(
                    if (length > MAX_CAPTION_LENGTH * 0.9)
                        ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                    else ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
                )
            }
        })

        updateUIForUploadType()
    }

    private fun updateUIForUploadType() {
        when (uploadType) {
            UploadType.POST -> {
                binding.txtUploadType.text = "Đăng bài viết"
                binding.layoutReelSettings.visibility = View.GONE
                binding.layoutPostSettings.visibility = View.VISIBLE
                binding.btnPost.text = "Post"
            }
            UploadType.REEL -> {
                binding.txtUploadType.text = "Đăng Reel"
                binding.layoutReelSettings.visibility = View.VISIBLE
                binding.layoutPostSettings.visibility = View.GONE
                binding.btnPost.text = "Post Reel"
            }
        }
    }

    private fun updateUIWithMedia() {
        if (selectedMedia.isNotEmpty()) {
            binding.previewImage.visibility = View.VISIBLE
            binding.imageContainer.visibility = View.VISIBLE
            binding.scrollContainer.visibility = View.VISIBLE

            val currentMedia = selectedMedia[currentMediaIndex]

            when (currentMedia.type) {
                MediaType.IMAGE -> {
                    binding.previewImage.visibility = View.VISIBLE
                    binding.previewVideo.visibility = View.GONE
                    binding.videoControls.visibility = View.GONE

                    Glide.with(this).load(currentMedia.uri).into(binding.previewImage)
                    binding.previewImage.background = null
                }
                MediaType.VIDEO -> {
                    binding.previewImage.visibility = View.GONE
                    binding.previewVideo.visibility = View.VISIBLE
                    binding.videoControls.visibility = View.VISIBLE

                    setupVideoPlayer(currentMedia.uri)
                }
            }

            if (selectedMedia.size > 1) {
                binding.imageCounter.visibility = View.VISIBLE
                binding.imageCounter.text = "${currentMediaIndex + 1}/${selectedMedia.size}"
                binding.thumbnailScrollView.visibility = View.VISIBLE
                updateThumbnails()
            } else {
                binding.imageCounter.visibility = View.GONE
                binding.thumbnailScrollView.visibility = View.GONE
            }

            val layoutParams = binding.previewImage.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            layoutParams.height = 0
            binding.previewImage.layoutParams = layoutParams
        }
    }

    private fun setupVideoPlayer(uri: Uri) {
        binding.previewVideo.apply {
            setVideoURI(uri)
            setOnPreparedListener { mp ->
                mp.isLooping = true
                start()
                binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
            }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "Video error: what=$what, extra=$extra")
                Toast.makeText(context, "Lỗi phát video", Toast.LENGTH_SHORT).show()
                true
            }

            // Nhấn vào video để pause/play
            setOnClickListener {
                if (isPlaying) {
                    pause()
                    binding.btnPlayPause.setImageResource(R.drawable.ic_play)
                } else {
                    start()
                    binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
                }
            }
        }

        // Button play/pause vẫn hoạt động bình thường
        binding.btnPlayPause.setOnClickListener {
            if (binding.previewVideo.isPlaying) {
                binding.previewVideo.pause()
                binding.btnPlayPause.setImageResource(R.drawable.ic_play)
            } else {
                binding.previewVideo.start()
                binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
            }
        }


    }

    private fun updateThumbnails() {
        binding.thumbnailContainer.removeAllViews()
        selectedMedia.forEachIndexed { index, mediaItem ->
            val thumbnailView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.thumbnail_size),
                    resources.getDimensionPixelSize(R.dimen.thumbnail_size)
                ).apply { marginEnd = resources.getDimensionPixelSize(R.dimen.thumbnail_margin) }

                Glide.with(this@UploadFragment).load(mediaItem.uri).into(this)

                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(4, 4, 4, 4)
                setBackgroundResource(
                    if (index == currentMediaIndex)
                        R.drawable.thumbnail_border_selected
                    else
                        R.drawable.thumbnail_border
                )
                setOnClickListener {
                    currentMediaIndex = index
                    updateUIWithMedia()
                }
                setOnLongClickListener {
                    showDeleteMediaDialog(index)
                    true
                }
            }

            if (mediaItem.type == MediaType.VIDEO) {
                val frameLayout = android.widget.FrameLayout(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        resources.getDimensionPixelSize(R.dimen.thumbnail_size),
                        resources.getDimensionPixelSize(R.dimen.thumbnail_size)
                    ).apply { marginEnd = resources.getDimensionPixelSize(R.dimen.thumbnail_margin) }
                }

                frameLayout.addView(thumbnailView)

                val playIcon = ImageView(requireContext()).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        40, 40, android.view.Gravity.CENTER
                    )
                    setImageResource(R.drawable.ic_play_circle_filled)
                }
                frameLayout.addView(playIcon)

                binding.thumbnailContainer.addView(frameLayout)
            } else {
                binding.thumbnailContainer.addView(thumbnailView)
            }
        }
    }

    private fun showDeleteMediaDialog(index: Int) {
        val mediaType = if (selectedMedia[index].type == MediaType.IMAGE) "ảnh" else "video"
        AlertDialog.Builder(requireContext())
            .setTitle("Xóa $mediaType")
            .setMessage("Bạn có chắc muốn xóa $mediaType này?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteMedia(index)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteMedia(index: Int) {
        selectedMedia.removeAt(index)
        selectedImages.removeAt(index)

        if (selectedMedia.isEmpty()) {
            Toast.makeText(context, "Vui lòng chọn ít nhất 1 media", Toast.LENGTH_SHORT).show()
            checkPermissionAndOpenPicker()
        } else {
            if (currentMediaIndex >= selectedMedia.size) {
                currentMediaIndex = selectedMedia.size - 1
            }
            updateUIWithMedia()
            Toast.makeText(context, "Đã xóa media", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            showBottomNavigation()
            parentFragmentManager.popBackStack()
        }

        binding.previewImage.setOnClickListener { showMediaOptionsDialog() }
        binding.previewVideo.setOnClickListener { showMediaOptionsDialog() }
        binding.btnEmoji.setOnClickListener { showEmojiPicker() }
        binding.btnSuggestHashtag.setOnClickListener { showHashtagSuggestions() }
        binding.editMusic.setOnClickListener { showMusicPicker() }
        binding.btnTagPeople.setOnClickListener { showPeopleTagDialog() }
        binding.btnAddLocation.setOnClickListener { showLocationPicker() }
        binding.btnAdvancedSettings.setOnClickListener { showAdvancedSettings() }
        binding.btnAudienceSelector.setOnClickListener { showAudienceSelector() }

        // Button to switch between POST and REEL
        binding.btnChangeUploadType.setOnClickListener {
            showUploadTypeSelector()
        }

        // Post settings
        binding.switchDisableComments.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(
                context,
                if (isChecked) "Đã tắt bình luận" else "Đã bật bình luận",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.switchHideLikes.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(
                context,
                if (isChecked) "Đã ẩn lượt thích" else "Đã hiện lượt thích",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Reel settings
        binding.switchAllowComments.setOnCheckedChangeListener { _, isChecked ->
            allowsComments = isChecked
        }

        binding.switchAllowDuet.setOnCheckedChangeListener { _, isChecked ->
            allowsDuet = isChecked
        }

        binding.switchAllowRemix.setOnCheckedChangeListener { _, isChecked ->
            allowsRemix = isChecked
        }

        binding.btnPost.setOnClickListener {
            if (validatePost()) confirmAndPost()
        }

        binding.btnSaveDraft.setOnClickListener {
            saveDraft()
        }
    }

    private fun showUploadTypeSelector() {
        val options = arrayOf("Đăng bài viết", "Đăng Reel")
        val currentSelection = when (uploadType) {
            UploadType.POST -> 0
            UploadType.REEL -> 1
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Chọn loại đăng tải")
            .setSingleChoiceItems(options, currentSelection) { dialog, which ->
                val newUploadType = when (which) {
                    0 -> UploadType.POST
                    1 -> UploadType.REEL
                    else -> UploadType.POST
                }

                val shouldOpenPicker = selectedMedia.isEmpty()

                // Check if switching to REEL with invalid media
                if (newUploadType == UploadType.REEL && selectedMedia.isNotEmpty()) {
                    if (selectedMedia.size > 1) {
                        Toast.makeText(context, "Reel chỉ cho phép 1 video. Vui lòng chọn lại.", Toast.LENGTH_LONG).show()
                        selectedMedia.clear()
                        selectedImages.clear()
                        uploadType = newUploadType
                        updateUIForUploadType()
                        dialog.dismiss()
                        openVideoPicker()
                        return@setSingleChoiceItems
                    }

                    if (selectedMedia[0].type != MediaType.VIDEO) {
                        Toast.makeText(context, "Reel phải là video. Vui lòng chọn lại.", Toast.LENGTH_LONG).show()
                        selectedMedia.clear()
                        selectedImages.clear()
                        uploadType = newUploadType
                        updateUIForUploadType()
                        dialog.dismiss()
                        openVideoPicker()
                        return@setSingleChoiceItems
                    }
                }

                uploadType = newUploadType
                updateUIForUploadType()
                Toast.makeText(
                    context,
                    if (uploadType == UploadType.POST) "Chế độ: Đăng bài viết" else "Chế độ: Đăng Reel",
                    Toast.LENGTH_SHORT
                ).show()

                dialog.dismiss()

                // Auto open picker if no media selected
                if (shouldOpenPicker) {
                    if (uploadType == UploadType.REEL) {
                        openVideoPicker()
                    } else {
                        showMediaTypeSelector()
                    }
                }
            }
            .setNegativeButton("Hủy") { _, _ ->
                if (selectedMedia.isEmpty()) {
                    showBottomNavigation()
                    parentFragmentManager.popBackStack()
                }
            }
            .show()
    }

    private fun showMediaOptionsDialog() {
        val options = arrayOf(
            "Thay thế media hiện tại",
            "Thêm media mới (${selectedMedia.size}/$MAX_MEDIA_COUNT)",
            "Xóa media hiện tại"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Tùy chọn media")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        isReplacingMedia = true
                        replaceMediaIndex = currentMediaIndex
                        checkPermissionAndOpenPicker()
                    }
                    1 -> {
                        if (uploadType == UploadType.REEL) {
                            Toast.makeText(
                                context,
                                "Reel chỉ cho phép 1 video",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (selectedMedia.size >= MAX_MEDIA_COUNT) {
                            Toast.makeText(
                                context,
                                "Đã đạt giới hạn $MAX_MEDIA_COUNT media",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            isReplacingMedia = false
                            replaceMediaIndex = -1
                            checkPermissionAndOpenPicker()
                        }
                    }
                    2 -> showDeleteMediaDialog(currentMediaIndex)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun checkPermissionAndOpenPicker() {
        // Nếu chưa có media, cho phép chọn loại upload trước
        if (selectedMedia.isEmpty() && !isReplacingMedia) {
            showUploadTypeSelector()
            return
        }

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) ==
                    PackageManager.PERMISSION_GRANTED -> {
                if (uploadType == UploadType.REEL) {
                    openVideoPicker()
                } else {
                    showMediaTypeSelector()
                }
            }
            else -> requestPermissionLauncher.launch(permission)
        }
    }

    private fun showMediaTypeSelector() {
        val options = arrayOf("Chọn ảnh", "Chọn video")

        AlertDialog.Builder(requireContext())
            .setTitle("Chọn loại media")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openImagePicker()
                    1 -> openVideoPicker()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Hủy") { _, _ ->
                if (selectedMedia.isEmpty()) {
                    showBottomNavigation()
                    parentFragmentManager.popBackStack()
                }
            }
            .show()
    }

    private fun openImagePicker() {
        val maxSelect = if (isReplacingMedia) 1 else (MAX_MEDIA_COUNT - selectedMedia.size)

        val alreadySelectedUris: List<Uri> = if (isReplacingMedia) {
            emptyList()
        } else {
            selectedMedia.filter { it.type == MediaType.IMAGE }.map { it.uri }
        }

        val dialog = ImagePickerDialogFragment.newInstance(
            alreadySelectedImages = alreadySelectedUris,
            maxSelection = maxSelect
        )

        dialog.setOnImagesSelectedListener { newSelectedImages: List<Uri> ->
            val newMediaItems = newSelectedImages.map { uri -> MediaItem(uri, MediaType.IMAGE) }
            handleMediaSelection(newMediaItems)
        }

        dialog.show(childFragmentManager, ImagePickerDialogFragment.TAG)
    }

    private fun openVideoPicker() {
        val maxSelect = if (uploadType == UploadType.REEL) {
            1 // Reel chỉ cho phép 1 video
        } else if (isReplacingMedia) {
            1
        } else {
            MAX_MEDIA_COUNT - selectedMedia.size
        }

        val maxDuration = if (uploadType == UploadType.REEL) {
            MAX_REEL_DURATION
        } else {
            MAX_VIDEO_DURATION
        }

        val dialog = VideoPickerDialogFragment.newInstance(
            maxSelection = maxSelect,
            maxDuration = maxDuration
        )

        dialog.setOnVideosSelectedListener { newSelectedVideos ->
            handleMediaSelection(newSelectedVideos)
        }

        dialog.show(childFragmentManager, VideoPickerDialogFragment.TAG)
    }

    private fun handleMediaSelection(newMedia: List<MediaItem>) {
        if (isReplacingMedia && replaceMediaIndex >= 0 && newMedia.isNotEmpty()) {
            selectedMedia[replaceMediaIndex] = newMedia[0]
            selectedImages[replaceMediaIndex] = newMedia[0].uri
            updateUIWithMedia()
            Toast.makeText(context, "Đã thay thế media", Toast.LENGTH_SHORT).show()

            isReplacingMedia = false
            replaceMediaIndex = -1
        } else {
            val beforeSize = selectedMedia.size
            newMedia.forEach { newMediaItem ->
                if (!selectedMedia.any { it.uri == newMediaItem.uri } && selectedMedia.size < MAX_MEDIA_COUNT) {
                    selectedMedia.add(newMediaItem)
                    selectedImages.add(newMediaItem.uri)
                }
            }

            val addedCount = selectedMedia.size - beforeSize

            if (selectedMedia.isNotEmpty()) {
                updateUIWithMedia()
                if (addedCount > 0) {
                    Toast.makeText(
                        context,
                        "Đã thêm $addedCount media",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Các media đã được chọn trước đó",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    context,
                    "Không có media nào được chọn",
                    Toast.LENGTH_SHORT
                ).show()
                showBottomNavigation()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun showEmojiPicker() {
        val emojis = arrayOf(
            "😀", "😂", "🥰", "😍", "🤩", "😎",
            "🔥", "❤️", "👍", "🎉", "✨", "🌟",
            "💯", "🙌", "👏", "💪"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Chọn emoji")
            .setItems(emojis) { dialog, which ->
                val currentText = binding.editCaption.text.toString()
                val cursorPosition = binding.editCaption.selectionStart
                val newText = currentText.substring(0, cursorPosition) +
                        emojis[which] +
                        currentText.substring(cursorPosition)
                binding.editCaption.setText(newText)
                binding.editCaption.setSelection(cursorPosition + emojis[which].length)
                dialog.dismiss()
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun showHashtagSuggestions() {
        val suggestions = arrayOf(
            "#travel", "#food", "#photography", "#nature",
            "#lifestyle", "#fashion", "#fitness", "#art",
            "#music", "#sunset", "#love", "#instagood"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Gợi ý hashtag")
            .setMultiChoiceItems(suggestions, null) { _, which, isChecked ->
                if (isChecked) {
                    val currentText = binding.editHashtag.text.toString()
                    val newText = if (currentText.isEmpty())
                        suggestions[which]
                    else
                        "$currentText ${suggestions[which]}"
                    binding.editHashtag.setText(newText)
                }
            }
            .setPositiveButton("Xong", null)
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showMusicPicker() {
        viewModel.musicList.value?.let { musicList ->
            if (musicList.isEmpty()) {
                Toast.makeText(context, "Danh sách chưa có nhạc", Toast.LENGTH_SHORT).show()
            } else {
                val musicTitles = musicList.map { "${it.title} - ${it.artist}" }.toTypedArray()
                AlertDialog.Builder(requireContext())
                    .setTitle("Chọn nhạc nền")
                    .setItems(musicTitles) { dialog, which ->
                        selectedMusic = musicList[which]
                        binding.editMusic.text = "${selectedMusic?.title} - ${selectedMusic?.artist}"
                        binding.editMusic.setTextColor(
                            ContextCompat.getColor(requireContext(), android.R.color.black)
                        )
                        Toast.makeText(
                            context,
                            "Đã chọn: ${selectedMusic?.title} - ${selectedMusic?.artist}",
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        } ?: run {
            Toast.makeText(context, "Danh sách chưa có nhạc", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPeopleTagDialog() {
        val friendsList = arrayOf(
            "Nguyễn Văn A", "Trần Thị B", "Lê Văn C",
            "Phạm Thị D", "Hoàng Văn E"
        )
        val checkedItems = BooleanArray(friendsList.size)

        AlertDialog.Builder(requireContext())
            .setTitle("Gắn thẻ người khác")
            .setMultiChoiceItems(friendsList, checkedItems) { _, which, isChecked ->
                if (isChecked)
                    taggedPeople.add(friendsList[which])
                else
                    taggedPeople.remove(friendsList[which])
            }
            .setPositiveButton("Xong") { _, _ ->
                binding.txtTagPeople.text = if (taggedPeople.isNotEmpty())
                    "Đã gắn thẻ ${taggedPeople.size} người"
                else
                    "Gắn thẻ người khác"

                binding.txtTagPeople.setTextColor(
                    if (taggedPeople.isNotEmpty())
                        ContextCompat.getColor(requireContext(), R.color.primary_blue)
                    else
                        ContextCompat.getColor(requireContext(), android.R.color.black)
                )
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showLocationPicker() {
        val locations = arrayOf(
            "📍 Vị trí hiện tại",
            "Hồ Chí Minh, Việt Nam",
            "Hà Nội, Việt Nam",
            "Đà Nẵng, Việt Nam",
            "Nha Trang, Việt Nam",
            "Phú Quốc, Việt Nam",
            "Tìm kiếm vị trí..."
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Thêm vị trí")
            .setItems(locations) { dialog, which ->
                selectedLocation = locations[which]
                binding.txtLocation.text = selectedLocation
                binding.txtLocation.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.primary_blue)
                )
                Toast.makeText(context, "Đã chọn: $selectedLocation", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showAdvancedSettings() {
        val settings = arrayOf(
            "Cho phép chia sẻ",
            "Cho phép lưu media",
            "Tự động phụ đề",
            "Đăng đồng thời lên Facebook",
            "Đăng đồng thời lên Twitter",
            "Lên lịch đăng bài"
        )
        val checkedItems = BooleanArray(settings.size) { false }

        AlertDialog.Builder(requireContext())
            .setTitle("Cài đặt nâng cao")
            .setMultiChoiceItems(settings, checkedItems, null)
            .setPositiveButton("Lưu") { _, _ ->
                Toast.makeText(context, "Đã lưu cài đặt", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showAudienceSelector() {
        val audiences = AudienceType.values().map { it.displayName }
        val currentSelection = audienceType.ordinal

        AlertDialog.Builder(requireContext())
            .setTitle("Ai có thể xem bài viết này?")
            .setSingleChoiceItems(audiences.toTypedArray(), currentSelection) { dialog, which ->
                audienceType = AudienceType.values()[which]
                binding.txtAudience.text = audienceType.displayName
                Toast.makeText(
                    context,
                    "Đã chọn: ${audienceType.displayName}",
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun validatePost(): Boolean {
        if (selectedMedia.isEmpty()) {
            Toast.makeText(context, "Vui lòng chọn ít nhất 1 media", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validate for REEL
        if (uploadType == UploadType.REEL) {
            if (selectedMedia.size > 1) {
                Toast.makeText(context, "Reel chỉ cho phép 1 video", Toast.LENGTH_SHORT).show()
                return false
            }

            if (selectedMedia[0].type != MediaType.VIDEO) {
                Toast.makeText(context, "Reel phải là video", Toast.LENGTH_SHORT).show()
                return false
            }

            val duration = selectedMedia[0].duration ?: 0
            if (duration > MAX_REEL_DURATION) {
                Toast.makeText(
                    context,
                    "Reel không được dài quá ${MAX_REEL_DURATION / 1000} giây",
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }
        }

        val captionLength = binding.editCaption.text.toString().length
        if (captionLength > MAX_CAPTION_LENGTH) {
            Toast.makeText(
                context,
                "Mô tả quá dài (tối đa $MAX_CAPTION_LENGTH ký tự)",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        // Validate video duration for POST
        if (uploadType == UploadType.POST) {
            selectedMedia.forEach { media ->
                if (media.type == MediaType.VIDEO && media.duration != null && media.duration > MAX_VIDEO_DURATION) {
                    Toast.makeText(
                        context,
                        "Video không được dài quá ${MAX_VIDEO_DURATION / 1000} giây",
                        Toast.LENGTH_SHORT
                    ).show()
                    return false
                }
            }
        }

        return true
    }

    private fun confirmAndPost() {
        val message = when (uploadType) {
            UploadType.POST -> "Bạn có chắc muốn đăng bài viết này?"
            UploadType.REEL -> "Bạn có chắc muốn đăng Reel này?"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận đăng")
            .setMessage(message)
            .setPositiveButton("Đăng") { _, _ -> performPost() }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun performPost() {
        val message = when (uploadType) {
            UploadType.POST -> "Đang đăng bài..."
            UploadType.REEL -> "Đang đăng Reel..."
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

        when (uploadType) {
            UploadType.POST -> {
                viewModel.uploadPost(
                    requireContext(),
                    selectedMedia,
                    binding.editCaption.text.toString(),
                    binding.editHashtag.text.toString(),
                    selectedMusic,
                    taggedPeople,
                    selectedLocation,
                    audienceType,
                    binding.switchDisableComments.isChecked,
                    binding.switchHideLikes.isChecked
                )
            }
            UploadType.REEL -> {
                viewModel.uploadReel(
                    requireContext(),
                    selectedMedia[0], // Reel chỉ có 1 video
                    binding.editCaption.text.toString(),
                    selectedMusic?.musicId,
                    allowsComments,
                    allowsDuet,
                    allowsRemix
                )
            }
        }
    }

    private fun saveDraft() {
        if (selectedMedia.isEmpty() && binding.editCaption.text.toString().isEmpty()) {
            Toast.makeText(context, "Không có nội dung để lưu", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Lưu nháp")
            .setMessage("Bài viết sẽ được lưu vào nháp để bạn có thể tiếp tục sau.")
            .setPositiveButton("Lưu") { _, _ ->
                viewModel.saveDraft(
                    requireContext(),
                    selectedMedia,
                    binding.editCaption.text.toString(),
                    binding.editHashtag.text.toString(),
                    selectedMusic,
                    taggedPeople,
                    selectedLocation,
                    audienceType,
                    binding.switchDisableComments.isChecked,
                    binding.switchHideLikes.isChecked
                )
                Toast.makeText(context, "Đã lưu nháp", Toast.LENGTH_SHORT).show()
                resetForm()
                showBottomNavigation()
                parentFragmentManager.popBackStack()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun resetForm() {
        selectedMedia.clear()
        selectedImages.clear()
        currentMediaIndex = 0
        binding.editCaption.setText("")
        binding.editHashtag.setText("")
        binding.editMusic.text = "Thêm nhạc"
        binding.editMusic.setTextColor(
            ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
        )
        selectedMusic = null
        taggedPeople.clear()
        binding.txtTagPeople.text = "Gắn thẻ người khác"
        binding.txtTagPeople.setTextColor(
            ContextCompat.getColor(requireContext(), android.R.color.black)
        )
        selectedLocation = null
        binding.txtLocation.text = "Thêm vị trí"
        binding.txtLocation.setTextColor(
            ContextCompat.getColor(requireContext(), android.R.color.black)
        )
        binding.switchDisableComments.isChecked = false
        binding.switchHideLikes.isChecked = false
        binding.switchAllowComments.isChecked = true
        binding.switchAllowDuet.isChecked = true
        binding.switchAllowRemix.isChecked = true
        audienceType = AudienceType.PUBLIC
        binding.txtAudience.text = audienceType.displayName
        isReplacingMedia = false
        replaceMediaIndex = -1
        uploadType = UploadType.POST
        allowsComments = true
        allowsDuet = true
        allowsRemix = true
        updateUIForUploadType()
    }

    private fun observeViewModel() {
        viewModel.uploadProgress.observe(viewLifecycleOwner) { progress ->
            when (progress) {
                is UploadProgress.GettingUserInfo ->
                    Log.d(TAG, "Getting user info...")
                is UploadProgress.UploadingImages ->
                    Log.d(TAG, "Uploading media ${progress.current}/${progress.total} - ${progress.progress}%")
                is UploadProgress.UploadingReel ->
                    Log.d(TAG, "Uploading reel - ${progress.progress}%")
                UploadProgress.SavingPost ->
                    Log.d(TAG, "Saving post...")
                UploadProgress.SavingReel ->
                    Log.d(TAG, "Saving reel...")
                UploadProgress.SavingDraft ->
                    Log.d(TAG, "Saving draft...")
                UploadProgress.Idle -> Unit
            }
        }

        viewModel.uploadResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is UploadResult.Success -> {
                    Toast.makeText(context, "Đã đăng bài thành công!", Toast.LENGTH_LONG).show()
                    resetForm()
                    showBottomNavigation()
                    parentFragmentManager.popBackStack()
                }
                is UploadResult.ReelSuccess -> {
                    Toast.makeText(context, "Đã đăng Reel thành công!", Toast.LENGTH_LONG).show()
                    resetForm()
                    showBottomNavigation()
                    parentFragmentManager.popBackStack()
                }
                is UploadResult.DraftSaved -> {
                    Toast.makeText(context, "Đã lưu nháp thành công!", Toast.LENGTH_LONG).show()
                    resetForm()
                    showBottomNavigation()
                    parentFragmentManager.popBackStack()
                }
                is UploadResult.Error -> {
                    Toast.makeText(context, "Lỗi: ${result.message}", Toast.LENGTH_LONG).show()
                }
                UploadResult.Idle -> Unit
            }
        }
    }
}