package com.example.socialmedia.project.Fragment

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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentUploadBinding
import com.example.socialmedia.MainActivity
import com.example.socialmedia.project.Domain.Model.MusicModel
import com.example.socialmedia.project.Domain.Enum.AudienceType
import com.example.socialmedia.project.ViewModel.UploadProgress
import com.example.socialmedia.project.ViewModel.UploadResult
import com.example.socialmedia.project.ViewModel.UploadViewModel
import kotlin.math.min

class UploadFragment : Fragment() {

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UploadViewModel by viewModels()
    private val selectedImages = mutableListOf<Uri>()
    private var currentImageIndex = 0
    private val MAX_CAPTION_LENGTH = 2200
    private var selectedMusic: MusicModel? = null
    private val taggedPeople = mutableListOf<String>()
    private var selectedLocation: String? = null
    private var audienceType = AudienceType.PUBLIC // Cập nhật từ String thành AudienceType
    private val TAG = "UploadFragment"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openImagePicker()
        else {
            Toast.makeText(context, "Cần cấp quyền truy cập ảnh", Toast.LENGTH_SHORT).show()
            showMainActivityUI()
            parentFragmentManager.popBackStack()
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            handleImageSelection(result.data!!)
        } else {
            Toast.makeText(context, "Đã hủy chọn ảnh", Toast.LENGTH_SHORT).show()
            showMainActivityUI()
            parentFragmentManager.popBackStack()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        parentFragmentManager.setFragmentResultListener("imagePickerResult", this) { _, bundle ->
            val data = bundle.getParcelable<Intent>("data")
            data?.let { handleImageSelection(it) }
        }
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

        hideMainActivityUI()
        setupUI()
        setupListeners()
        observeViewModel()

        if (selectedImages.isEmpty()) {
            checkPermissionAndOpenPicker()
        } else {
            updateUIWithImages()
        }

        viewModel.musicList.observe(viewLifecycleOwner) {
            // Không cần làm gì thêm vì danh sách đã được tải
        }
    }

    override fun onResume() {
        super.onResume()
        hideMainActivityUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showMainActivityUI()
        _binding = null
    }

    private fun hideMainActivityUI() {
        (activity as? MainActivity)?.findViewById<View>(R.id.container)?.visibility = View.GONE
    }

    private fun showMainActivityUI() {
        (activity as? MainActivity)?.findViewById<View>(R.id.container)?.visibility = View.VISIBLE
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
    }

    private fun updateUIWithImages() {
        if (selectedImages.isNotEmpty()) {
            binding.previewImage.visibility = View.VISIBLE
            binding.imageContainer.visibility = View.VISIBLE
            binding.scrollContainer.visibility = View.VISIBLE

            Glide.with(this).load(selectedImages[currentImageIndex]).into(binding.previewImage)
            binding.previewImage.background = null

            if (selectedImages.size > 1) {
                binding.imageCounter.visibility = View.VISIBLE
                binding.imageCounter.text = "${currentImageIndex + 1}/${selectedImages.size}"
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

    private fun updateThumbnails() {
        binding.thumbnailContainer.removeAllViews()
        selectedImages.forEachIndexed { index, uri ->
            val thumbnailView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.thumbnail_size),
                    resources.getDimensionPixelSize(R.dimen.thumbnail_size)
                ).apply { marginEnd = resources.getDimensionPixelSize(R.dimen.thumbnail_margin) }
                Glide.with(this@UploadFragment).load(uri).into(this)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(4, 4, 4, 4)
                setBackgroundResource(if (index == currentImageIndex) R.drawable.thumbnail_border_selected else R.drawable.thumbnail_border)
                setOnClickListener {
                    currentImageIndex = index
                    updateUIWithImages()
                }
            }
            binding.thumbnailContainer.addView(thumbnailView)
        }
    }

    private fun setupListeners() {
        binding.previewImage.setOnClickListener { checkPermissionAndOpenPicker() }
        binding.btnEmoji.setOnClickListener { showEmojiPicker() }
        binding.btnSuggestHashtag.setOnClickListener { showHashtagSuggestions() }
        binding.editMusic.setOnClickListener { showMusicPicker() }
        binding.btnTagPeople.setOnClickListener { showPeopleTagDialog() }
        binding.btnAddLocation.setOnClickListener { showLocationPicker() }
        binding.btnAdvancedSettings.setOnClickListener { showAdvancedSettings() }
        binding.btnAudienceSelector.setOnClickListener { showAudienceSelector() }

        binding.switchDisableComments.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(context, if (isChecked) "Đã tắt bình luận" else "Đã bật bình luận", Toast.LENGTH_SHORT).show()
        }
        binding.switchHideLikes.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(context, if (isChecked) "Đã ẩn lượt thích" else "Đã hiện lượt thích", Toast.LENGTH_SHORT).show()
        }

        binding.btnPost.setOnClickListener { if (validatePost()) confirmAndPost() }
        binding.btnSaveDraft.setOnClickListener { saveDraft() }
    }

    private fun checkPermissionAndOpenPicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED ->
                openImagePicker()
            else -> requestPermissionLauncher.launch(permission)
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        imagePickerLauncher.launch(intent)
    }

    private fun handleImageSelection(data: Intent) {
        val newImages = mutableListOf<Uri>()
        if (data.clipData != null) {
            val count = minOf(data.clipData!!.itemCount, 10 - selectedImages.size)
            for (i in 0 until count) {
                val imageUri = data.clipData!!.getItemAt(i).uri
                if (!selectedImages.contains(imageUri)) newImages.add(imageUri)
            }
        } else if (data.data != null) {
            val imageUri = data.data!!
            if (!selectedImages.contains(imageUri) && selectedImages.size < 10) newImages.add(imageUri)
        }

        if (newImages.isNotEmpty()) {
            selectedImages.addAll(newImages)
            updateUIWithImages()
            Toast.makeText(context, "Đã thêm ${newImages.size} ảnh", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Không có ảnh nào được chọn", Toast.LENGTH_SHORT).show()
            showMainActivityUI()
            parentFragmentManager.popBackStack()
        }
    }

    private fun showEmojiPicker() {
        val emojis = arrayOf("😀", "😂", "🥰", "😍", "🤩", "😎", "🔥", "❤️", "👍", "🎉", "✨", "🌟", "💯", "🙌", "👏", "💪")
        AlertDialog.Builder(requireContext()).setTitle("Chọn emoji")
            .setItems(emojis) { dialog, which ->
                val currentText = binding.editCaption.text.toString()
                val cursorPosition = binding.editCaption.selectionStart
                val newText = currentText.substring(0, cursorPosition) + emojis[which] + currentText.substring(cursorPosition)
                binding.editCaption.setText(newText)
                binding.editCaption.setSelection(cursorPosition + emojis[which].length)
                dialog.dismiss()
            }.setNegativeButton("Đóng", null).show()
    }

    private fun showHashtagSuggestions() {
        val suggestions = arrayOf("#travel", "#food", "#photography", "#nature", "#lifestyle", "#fashion", "#fitness", "#art", "#music", "#sunset", "#love", "#instagood")
        AlertDialog.Builder(requireContext()).setTitle("Gợi ý hashtag")
            .setMultiChoiceItems(suggestions, null) { _, which, isChecked ->
                if (isChecked) {
                    val currentText = binding.editHashtag.text.toString()
                    val newText = if (currentText.isEmpty()) suggestions[which] else "$currentText ${suggestions[which]}"
                    binding.editHashtag.setText(newText)
                }
            }.setPositiveButton("Xong", null).setNegativeButton("Hủy", null).show()
    }

    private fun showMusicPicker() {
        viewModel.musicList.value?.let { musicList ->
            if (musicList.isEmpty()) {
                Toast.makeText(context, "Danh sách chưa có nhạc", Toast.LENGTH_SHORT).show()
            } else {
                val musicTitles = musicList.map { "${it.title} - ${it.artist}" }.toTypedArray()
                AlertDialog.Builder(requireContext()).setTitle("Chọn nhạc nền")
                    .setItems(musicTitles) { dialog, which ->
                        selectedMusic = musicList[which]
                        binding.editMusic.text = "${selectedMusic?.title} - ${selectedMusic?.artist}"
                        binding.editMusic.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                        Toast.makeText(context, "Đã chọn: ${selectedMusic?.title} - ${selectedMusic?.artist}", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }.setNegativeButton("Hủy", null).show()
            }
        } ?: run {
            Toast.makeText(context, "Danh sách chưa có nhạc", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPeopleTagDialog() {
        val friendsList = arrayOf("Nguyễn Văn A", "Trần Thị B", "Lê Văn C", "Phạm Thị D", "Hoàng Văn E")
        val checkedItems = BooleanArray(friendsList.size)
        AlertDialog.Builder(requireContext()).setTitle("Gắn thẻ người khác")
            .setMultiChoiceItems(friendsList, checkedItems) { _, which, isChecked ->
                if (isChecked) taggedPeople.add(friendsList[which])
                else taggedPeople.remove(friendsList[which])
            }.setPositiveButton("Xong") { _, _ ->
                binding.txtTagPeople.text = if (taggedPeople.isNotEmpty()) "Đã gắn thẻ ${taggedPeople.size} người" else "Gắn thẻ người khác"
                binding.txtTagPeople.setTextColor(
                    if (taggedPeople.isNotEmpty())
                        ContextCompat.getColor(requireContext(), R.color.primary_blue)
                    else ContextCompat.getColor(requireContext(), android.R.color.black)
                )
            }.setNegativeButton("Hủy", null).show()
    }

    private fun showLocationPicker() {
        val locations = arrayOf("📍 Vị trí hiện tại", "Hồ Chí Minh, Việt Nam", "Hà Nội, Việt Nam", "Đà Nẵng, Việt Nam", "Nha Trang, Việt Nam", "Phú Quốc, Việt Nam", "Tìm kiếm vị trí...")
        AlertDialog.Builder(requireContext()).setTitle("Thêm vị trí")
            .setItems(locations) { dialog, which ->
                selectedLocation = locations[which]
                binding.txtLocation.text = selectedLocation
                binding.txtLocation.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
                Toast.makeText(context, "Đã chọn: $selectedLocation", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }.setNegativeButton("Hủy", null).show()
    }

    private fun showAdvancedSettings() {
        val settings = arrayOf("Cho phép chia sẻ", "Cho phép lưu ảnh", "Tự động phụ đề", "Đăng đồng thời lên Facebook", "Đăng đồng thời lên Twitter", "Lên lịch đăng bài")
        val checkedItems = BooleanArray(settings.size) { false }
        AlertDialog.Builder(requireContext()).setTitle("Cài đặt nâng cao")
            .setMultiChoiceItems(settings, checkedItems, null)
            .setPositiveButton("Lưu") { _, _ ->
                Toast.makeText(context, "Đã lưu cài đặt", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Hủy", null).show()
    }

    private fun showAudienceSelector() {
        // 💡 SỬA: Lấy danh sách tên hiển thị (displayName) thay vì tên hằng số (name)
        val audiences = AudienceType.values().map { it.displayName }
        val currentSelection = audienceType.ordinal // Sử dụng ordinal để xác định vị trí hiện tại

        AlertDialog.Builder(requireContext()).setTitle("Ai có thể xem bài viết này?")
            .setSingleChoiceItems(audiences.toTypedArray(), currentSelection) { dialog, which ->
                audienceType = AudienceType.values()[which] // Cập nhật audienceType

                // 💡 SỬA: Hiển thị displayName
                binding.txtAudience.text = audienceType.displayName

                // 💡 SỬA: Hiển thị displayName trong Toast
                Toast.makeText(context, "Đã chọn: ${audienceType.displayName}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }.setNegativeButton("Hủy", null).show()
    }

    private fun validatePost(): Boolean {
        if (selectedImages.isEmpty()) {
            Toast.makeText(context, "Vui lòng chọn ít nhất 1 ảnh", Toast.LENGTH_SHORT).show()
            return false
        }
        val captionLength = binding.editCaption.text.toString().length
        if (captionLength > MAX_CAPTION_LENGTH) {
            Toast.makeText(context, "Mô tả quá dài (tối đa $MAX_CAPTION_LENGTH ký tự)", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun confirmAndPost() {
        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận đăng bài")
            .setMessage("Bạn có chắc muốn đăng bài viết này?")
            .setPositiveButton("Đăng") { _, _ -> performPost() }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun performPost() {
        Toast.makeText(context, "Đang đăng bài...", Toast.LENGTH_SHORT).show()
        viewModel.uploadPost(
            requireContext(), // Context
            selectedImages, // List<Uri>
            binding.editCaption.text.toString(), // String
            binding.editHashtag.text.toString(), // String
            selectedMusic, // MusicModel?
            taggedPeople, // List<String>
            selectedLocation, // String?
            audienceType,// Sử dụng tên của AudienceType thay vì String trực tiếp
            binding.switchDisableComments.isChecked, // Boolean
            binding.switchHideLikes.isChecked // Boolean
        )
    }

    private fun saveDraft() {
        if (selectedImages.isEmpty() && binding.editCaption.text.toString().isEmpty()) {
            Toast.makeText(context, "Không có nội dung để lưu", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Lưu nháp")
            .setMessage("Bài viết sẽ được lưu vào nháp để bạn có thể tiếp tục sau.")
            .setPositiveButton("Lưu") { _, _ ->
                viewModel.saveDraft(
                    requireContext(), // Context
                    selectedImages, // List<Uri>
                    binding.editCaption.text.toString(), // String
                    binding.editHashtag.text.toString(), // String
                    selectedMusic, // MusicModel?
                    taggedPeople, // List<String>
                    selectedLocation, // String?
                    audienceType, // Sử dụng tên của AudienceType thay vì String trực tiếp
                    binding.switchDisableComments.isChecked, // Boolean
                    binding.switchHideLikes.isChecked // Boolean
                )
                Toast.makeText(context, "Đã lưu nháp", Toast.LENGTH_SHORT).show()
                resetForm()
                showMainActivityUI()
                parentFragmentManager.popBackStack()
            }.setNegativeButton("Hủy", null)
            .show()
    }

    private fun resetForm() {
        selectedImages.clear()
        currentImageIndex = 0
        binding.editCaption.setText("")
        binding.editHashtag.setText("")
        binding.editMusic.text = "Thêm nhạc"
        binding.editMusic.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        selectedMusic = null
        taggedPeople.clear()
        binding.txtTagPeople.text = "Gắn thẻ người khác"
        selectedLocation = null
        binding.txtLocation.text = "Thêm vị trí"
        binding.switchDisableComments.isChecked = false
        binding.switchHideLikes.isChecked = false
        audienceType = AudienceType.PUBLIC // Reset về giá trị mặc định
        // 💡 SỬA: Hiển thị displayName khi reset
        binding.txtAudience.text = audienceType.displayName
    }

    private fun observeViewModel() {
        viewModel.uploadProgress.observe(viewLifecycleOwner) { progress ->
            when (progress) {
                is UploadProgress.GettingUserInfo -> Log.d(TAG, "Getting user info...")
                is UploadProgress.UploadingImages -> Log.d(TAG, "Uploading image ${progress.current}/${progress.total} - ${progress.progress}%")
                UploadProgress.SavingPost -> Log.d(TAG, "Saving post...")
                UploadProgress.SavingDraft -> Log.d(TAG, "Saving draft...")
                UploadProgress.Idle -> Unit
            }
        }

        viewModel.uploadResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is UploadResult.Success -> {
                    Toast.makeText(context, "Đã đăng bài thành công!", Toast.LENGTH_LONG).show()
                    resetForm()
                    showMainActivityUI()
                    parentFragmentManager.popBackStack()
                }
                is UploadResult.DraftSaved -> {
                    Toast.makeText(context, "Đã lưu nháp thành công!", Toast.LENGTH_LONG).show()
                    resetForm()
                    showMainActivityUI()
                    parentFragmentManager.popBackStack()
                }
                is UploadResult.Error -> {
                    Toast.makeText(context, "Lỗi: ${result.message}", Toast.LENGTH_LONG).show()
                }
                UploadResult.Idle -> Unit
            }
        }
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 1001
    }
}