package com.example.socialmedia.Fragment.Fragment

import android.Manifest
import android.app.Activity
import android.content.Intent
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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentUploadBinding
import com.example.socialmedia.MainActivity

class UploadFragment : Fragment() {

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!

    private val selectedImages = mutableListOf<Uri>()
    private var currentImageIndex = 0
    private val MAX_CAPTION_LENGTH = 2200
    private var selectedMusic: String? = null
    private val taggedPeople = mutableListOf<String>()
    private var selectedLocation: String? = null
    private var audienceType = "Công khai"

    private val TAG = "UploadFragment"

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(context, "Cần cấp quyền truy cập ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")

        parentFragmentManager.setFragmentResultListener("imagePickerResult", this) { _, bundle ->
            val data = bundle.getParcelable<Intent>("data")
            data?.let {
                try {
                    handleImageSelection(it)
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling image selection", e)
                    Toast.makeText(context, "Lỗi khi chọn ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView called")
        try {
            _binding = FragmentUploadBinding.inflate(inflater, container, false)
            return binding.root
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreateView", e)
            throw e
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")

        try {
            // Ẩn Bottom Navigation và các thành phần khác
            hideMainActivityUI()

            setupUI()
            setupListeners()
            resetToEmptyState()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated", e)
            Toast.makeText(context, "Lỗi khởi tạo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Ẩn Bottom Navigation từ MainActivity
     */
    private fun hideMainActivityUI() {
        try {
            val mainActivity = activity as? MainActivity
            mainActivity?.let {
                // Ẩn Bottom Navigation
                val bottomNav = it.findViewById<View>(R.id.bottomNavigation)
                bottomNav?.visibility = View.GONE

                Log.d(TAG, "Bottom Navigation hidden")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding MainActivity UI", e)
        }
    }

    /**
     * Hiện lại Bottom Navigation khi rời Fragment
     */
    private fun showMainActivityUI() {
        try {
            val mainActivity = activity as? MainActivity
            mainActivity?.let {
                // Hiện Bottom Navigation
                val bottomNav = it.findViewById<View>(R.id.bottomNavigation)
                bottomNav?.visibility = View.VISIBLE

                Log.d(TAG, "Bottom Navigation shown")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing MainActivity UI", e)
        }
    }

    private fun setupUI() {
        try {
            // Setup caption character counter
            binding.editCaption.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val length = s?.length ?: 0
                    binding.characterCount.text = "$length/$MAX_CAPTION_LENGTH"

                    if (length > MAX_CAPTION_LENGTH * 0.9) {
                        binding.characterCount.setTextColor(
                            ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                        )
                    } else {
                        binding.characterCount.setTextColor(
                            ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
                        )
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupUI", e)
        }
    }

    private fun setupListeners() {
        try {
            // Click vào ảnh để chọn ảnh mới
            binding.previewImage.setOnClickListener {
                if (selectedImages.isEmpty()) {
                    checkPermissionAndOpenPicker()
                }
            }

            // Nút filter/edit ảnh
            binding.btnFilter.setOnClickListener {
                if (selectedImages.isNotEmpty()) {
                    showFilterOptions()
                } else {
                    Toast.makeText(context, "Vui lòng chọn ảnh trước", Toast.LENGTH_SHORT).show()
                }
            }

            // Nút crop ảnh
            binding.btnCrop.setOnClickListener {
                if (selectedImages.isNotEmpty()) {
                    showCropOptions()
                } else {
                    Toast.makeText(context, "Vui lòng chọn ảnh trước", Toast.LENGTH_SHORT).show()
                }
            }

            // Nút thêm nhiều ảnh
            binding.btnAddMore.setOnClickListener {
                if (selectedImages.size < 10) {
                    checkPermissionAndOpenPicker()
                } else {
                    Toast.makeText(context, "Tối đa 10 ảnh", Toast.LENGTH_SHORT).show()
                }
            }

            // Emoji picker
            binding.btnEmoji.setOnClickListener {
                showEmojiPicker()
            }

            // Suggest hashtag
            binding.btnSuggestHashtag.setOnClickListener {
                showHashtagSuggestions()
            }

            // Chọn nhạc
            binding.editMusic.setOnClickListener {
                showMusicPicker()
            }

            // Tag người
            binding.btnTagPeople.setOnClickListener {
                showPeopleTagDialog()
            }

            // Thêm vị trí
            binding.btnAddLocation.setOnClickListener {
                showLocationPicker()
            }

            // Cài đặt nâng cao
            binding.btnAdvancedSettings.setOnClickListener {
                showAdvancedSettings()
            }

            // Chọn đối tượng xem
            binding.btnAudienceSelector.setOnClickListener {
                showAudienceSelector()
            }

            // Toggle disable comments
            binding.switchDisableComments.setOnCheckedChangeListener { _, isChecked ->
                Toast.makeText(
                    context,
                    if (isChecked) "Đã tắt bình luận" else "Đã bật bình luận",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Toggle hide likes
            binding.switchHideLikes.setOnCheckedChangeListener { _, isChecked ->
                Toast.makeText(
                    context,
                    if (isChecked) "Đã ẩn lượt thích" else "Đã hiện lượt thích",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Nút đăng bài
            binding.btnPost.setOnClickListener {
                if (validatePost()) {
                    confirmAndPost()
                }
            }

            // Nút lưu nháp
            binding.btnSaveDraft.setOnClickListener {
                saveDraft()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupListeners", e)
        }
    }

    private fun checkPermissionAndOpenPicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openImagePicker() {
        try {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening image picker", e)
            Toast.makeText(context, "Không thể mở trình chọn ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            data?.let {
                try {
                    handleImageSelection(it)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onActivityResult", e)
                    Toast.makeText(context, "Lỗi khi xử lý ảnh", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleImageSelection(data: Intent) {
        try {
            val newImages = mutableListOf<Uri>()

            if (data.clipData != null) {
                val count = minOf(data.clipData!!.itemCount, 10 - selectedImages.size)
                for (i in 0 until count) {
                    val imageUri = data.clipData!!.getItemAt(i).uri
                    if (!selectedImages.contains(imageUri)) {
                        newImages.add(imageUri)
                    }
                }
            } else if (data.data != null) {
                val imageUri = data.data!!
                if (!selectedImages.contains(imageUri) && selectedImages.size < 10) {
                    newImages.add(imageUri)
                }
            }

            if (newImages.isNotEmpty()) {
                selectedImages.addAll(newImages)
                updateUIWithImages()
                Toast.makeText(
                    context,
                    "Đã thêm ${newImages.size} ảnh",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling image selection", e)
            Toast.makeText(context, "Lỗi khi chọn ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUIWithImages() {
        try {
            if (selectedImages.isNotEmpty()) {
                binding.previewImage.setImageURI(selectedImages[currentImageIndex])
                binding.previewImage.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                binding.scrollContainer.visibility = View.VISIBLE

                if (selectedImages.size > 1) {
                    binding.imageCounter.visibility = View.VISIBLE
                    binding.imageCounter.text = "${currentImageIndex + 1}/${selectedImages.size}"
                    binding.thumbnailScrollView.visibility = View.VISIBLE
                    updateThumbnails()
                } else {
                    binding.imageCounter.visibility = View.GONE
                    binding.thumbnailScrollView.visibility = View.GONE
                }

                val layoutParams = binding.previewImage.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.height = 0
                layoutParams.dimensionRatio = "H,1:1"
                binding.previewImage.layoutParams = layoutParams
            } else {
                resetToEmptyState()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating UI with images", e)
        }
    }

    private fun updateThumbnails() {
        try {
            binding.thumbnailContainer.removeAllViews()

            selectedImages.forEachIndexed { index, uri ->
                val thumbnailView = ImageView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        resources.getDimensionPixelSize(R.dimen.thumbnail_size),
                        resources.getDimensionPixelSize(R.dimen.thumbnail_size)
                    ).apply {
                        marginEnd = resources.getDimensionPixelSize(R.dimen.thumbnail_margin)
                    }
                    setImageURI(uri)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setPadding(4, 4, 4, 4)

                    setBackgroundResource(
                        if (index == currentImageIndex)
                            R.drawable.thumbnail_border_selected
                        else
                            R.drawable.thumbnail_border
                    )

                    setOnClickListener {
                        currentImageIndex = index
                        updateUIWithImages()
                    }
                }

                binding.thumbnailContainer.addView(thumbnailView)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating thumbnails", e)
        }
    }

    private fun resetToEmptyState() {
        try {
            binding.previewImage.setImageDrawable(null)
            binding.previewImage.setBackgroundColor(android.graphics.Color.WHITE)
            binding.scrollContainer.visibility = View.GONE
            binding.imageCounter.visibility = View.GONE
            binding.thumbnailScrollView.visibility = View.GONE

            val layoutParams = binding.previewImage.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.height = ConstraintLayout.LayoutParams.MATCH_PARENT
            layoutParams.dimensionRatio = null
            binding.previewImage.layoutParams = layoutParams
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting to empty state", e)
        }
    }

    private fun showFilterOptions() {
        val filters = arrayOf(
            "Normal", "Clarendon", "Gingham", "Moon",
            "Lark", "Reyes", "Juno", "Slumber", "Crema"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Chọn bộ lọc")
            .setItems(filters) { dialog, which ->
                Toast.makeText(
                    context,
                    "Đã áp dụng filter: ${filters[which]}",
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showCropOptions() {
        val cropOptions = arrayOf(
            "Vuông (1:1)",
            "Dọc (4:5)",
            "Ngang (16:9)",
            "Chân dung (9:16)",
            "Tự do"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Tỷ lệ cắt ảnh")
            .setItems(cropOptions) { dialog, which ->
                Toast.makeText(
                    context,
                    "Đã chọn: ${cropOptions[which]}",
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showEmojiPicker() {
        val emojis = arrayOf(
            "😀", "😂", "🥰", "😍", "🤩", "😎", "🔥", "❤️",
            "👍", "🎉", "✨", "🌟", "💯", "🙌", "👏", "💪"
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
                    val newText = if (currentText.isEmpty()) {
                        suggestions[which]
                    } else {
                        "$currentText ${suggestions[which]}"
                    }
                    binding.editHashtag.setText(newText)
                }
            }
            .setPositiveButton("Xong", null)
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showMusicPicker() {
        val musicList = arrayOf(
            "Trending #1 - Shape of You",
            "Trending #2 - Blinding Lights",
            "Trending #3 - Dance Monkey",
            "Pop Hits",
            "Chill Vibes",
            "K-Pop Mix",
            "Vietnamese Top 100",
            "Tìm kiếm nhạc..."
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Chọn nhạc nền")
            .setItems(musicList) { dialog, which ->
                selectedMusic = musicList[which]
                binding.editMusic.text = selectedMusic
                binding.editMusic.setTextColor(
                    ContextCompat.getColor(requireContext(), android.R.color.black)
                )
                Toast.makeText(context, "Đã chọn: $selectedMusic", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showPeopleTagDialog() {
        val friendsList = arrayOf(
            "Nguyễn Văn A",
            "Trần Thị B",
            "Lê Văn C",
            "Phạm Thị D",
            "Hoàng Văn E"
        )

        val checkedItems = BooleanArray(friendsList.size)

        AlertDialog.Builder(requireContext())
            .setTitle("Gắn thẻ người khác")
            .setMultiChoiceItems(friendsList, checkedItems) { _, which, isChecked ->
                if (isChecked) {
                    taggedPeople.add(friendsList[which])
                } else {
                    taggedPeople.remove(friendsList[which])
                }
            }
            .setPositiveButton("Xong") { _, _ ->
                if (taggedPeople.isNotEmpty()) {
                    binding.txtTagPeople.text = "Đã gắn thẻ ${taggedPeople.size} người"
                    binding.txtTagPeople.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.primary_blue)
                    )
                } else {
                    binding.txtTagPeople.text = "Gắn thẻ người khác"
                    binding.txtTagPeople.setTextColor(
                        ContextCompat.getColor(requireContext(), android.R.color.black)
                    )
                }
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
            "Cho phép lưu ảnh",
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
        val audiences = arrayOf(
            "Công khai",
            "Bạn bè",
            "Chỉ mình tôi",
            "Bạn bè ngoại trừ...",
            "Bạn bè cụ thể"
        )

        val currentSelection = audiences.indexOf(audienceType)

        AlertDialog.Builder(requireContext())
            .setTitle("Ai có thể xem bài viết này?")
            .setSingleChoiceItems(audiences, currentSelection) { dialog, which ->
                audienceType = audiences[which]
                binding.txtAudience.text = audienceType
                Toast.makeText(context, "Đã chọn: $audienceType", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun validatePost(): Boolean {
        if (selectedImages.isEmpty()) {
            Toast.makeText(context, "Vui lòng chọn ít nhất 1 ảnh", Toast.LENGTH_SHORT).show()
            return false
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

        return true
    }

    private fun confirmAndPost() {
        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận đăng bài")
            .setMessage("Bạn có chắc muốn đăng bài viết này?")
            .setPositiveButton("Đăng") { _, _ ->
                performPost()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun performPost() {
        Toast.makeText(context, "Đang đăng bài...", Toast.LENGTH_SHORT).show()

        // TODO: Implement actual upload logic here

        binding.root.postDelayed({
            Toast.makeText(context, "Đã đăng bài thành công!", Toast.LENGTH_LONG).show()
            resetForm()

            // Hiện lại UI MainActivity trước khi quay lại
            showMainActivityUI()
            parentFragmentManager.popBackStack()
        }, 2000)
    }

    private fun saveDraft() {
        if (selectedImages.isEmpty() &&
            binding.editCaption.text.toString().isEmpty()) {
            Toast.makeText(context, "Không có nội dung để lưu", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Lưu nháp")
            .setMessage("Bài viết sẽ được lưu vào nháp để bạn có thể tiếp tục sau.")
            .setPositiveButton("Lưu") { _, _ ->
                Toast.makeText(context, "Đã lưu nháp", Toast.LENGTH_SHORT).show()
                resetForm()

                // Hiện lại UI MainActivity trước khi quay lại
                showMainActivityUI()
                parentFragmentManager.popBackStack()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun resetForm() {
        selectedImages.clear()
        currentImageIndex = 0
        binding.editCaption.setText("")
        binding.editHashtag.setText("")
        binding.editMusic.text = "Thêm nhạc"
        binding.editMusic.setTextColor(
            ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
        )
        selectedMusic = null
        taggedPeople.clear()
        binding.txtTagPeople.text = "Gắn thẻ người khác"
        selectedLocation = null
        binding.txtLocation.text = "Thêm vị trí"
        binding.switchDisableComments.isChecked = false
        binding.switchHideLikes.isChecked = false
        audienceType = "Công khai"
        binding.txtAudience.text = audienceType
        resetToEmptyState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Hiện lại UI MainActivity khi destroy fragment
        showMainActivityUI()
        _binding = null
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 1001
    }
}