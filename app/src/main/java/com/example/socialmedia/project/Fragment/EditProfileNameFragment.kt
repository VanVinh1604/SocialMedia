package com.example.socialmedia.project.Fragment

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels // Quan trọng: dùng navGraphViewModels
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentEditProfileNameBinding // Import View Binding
import com.example.socialmedia.project.Domain.Enum.Gender
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.ViewModel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditProfileNameFragment : Fragment() {

    // Thiết lập View Binding
    private var _binding: FragmentEditProfileNameBinding? = null
    private val binding get() = _binding!!

    // LẤY VIEWMODEL ĐƯỢC CHIA SẺ TỪ NAV GRAPH
    private val viewModel: ProfileViewModel by navGraphViewModels(R.id.nav_graph)

    // Biến để lưu ngày sinh đã chọn
    private var selectedDateOfBirth: Long? = null
    private val calendar = Calendar.getInstance()

    // --- PHẦN MỚI: Logic chọn ảnh ---
    private var imageTypeToUpdate: String? = null // "avatar" hoặc "header"

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null && imageTypeToUpdate != null) {
                // Có ảnh, gọi ViewModel để upload
                Toast.makeText(context, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show()
                viewModel.uploadProfileImage(imageUri, imageTypeToUpdate!!)
            }
        }
    }
    // --- HẾT PHẦN MỚI ---


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout bằng View Binding
        _binding = FragmentEditProfileNameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGenderDropdown()
        setupClickListeners()
        setupObservers()

        // ViewModel đã tự động tải dữ liệu (listener trong init)
    }

    /**
     * Lắng nghe dữ liệu từ ViewModel
     */
    private fun setupObservers() {
        // 1. Lắng nghe dữ liệu profile (đã được fetch bởi fragment trước)
        viewModel.userProfile.observe(viewLifecycleOwner, Observer { user ->
            if (user != null) {
                Log.d("EditProfileFragment", "Observer nhận được user: ${user.fullName}")
                populateData(user)
            } else {
                Log.d("EditProfileFragment", "Observer nhận được user NULL")
            }
        })

        // 2. Lắng nghe trạng thái update TEXT
        viewModel.updateStatus.observe(viewLifecycleOwner, Observer { success ->
            if (success) {
                Toast.makeText(context, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show()
                viewModel.resetUpdateStatus() // Reset cờ
                findNavController().popBackStack() // Quay lại
            }
        })

        // 3. --- PHẦN MỚI: Lắng nghe trạng thái update ẢNH ---
        viewModel.imageUpdateStatus.observe(viewLifecycleOwner, Observer { status ->
            when {
                status == "success" -> {
                    Toast.makeText(context, "Cập nhật ảnh thành công!", Toast.LENGTH_SHORT).show()
                    viewModel.resetImageUpdateStatus() // Reset cờ
                    // Ảnh sẽ tự động cập nhật trong Observer (mục 1)
                }
                status?.startsWith("error:") == true -> {
                    Toast.makeText(context, status, Toast.LENGTH_LONG).show()
                    viewModel.resetImageUpdateStatus()
                }
            }
        })
        // --- HẾT PHẦN MỚI ---
    }

    /**
     * Điền dữ liệu có sẵn của user vào các trường
     */
    private fun populateData(user: UserModel) {
        binding.etName.setText(user.fullName)

        // === CẬP NHẬT 1: Điền bio (ghi chú) có sẵn ===
        binding.etBio.setText(user.bio ?: "")
        // ==========================================

        // --- CẬP NHẬT: Điền ảnh bìa ---
        Glide.with(this)
            .load(user.headerPictureUrl) // Đã thêm
            .centerCrop()
            .placeholder(R.drawable.image_backgroud)
            .error(R.drawable.image_backgroud)
            .into(binding.ivHeaderImage)

        // Điền avatar
        Glide.with(this)
            .load(user.profilePictureUrl)
            .circleCrop()
            .placeholder(R.drawable.image_avata_user)
            .error(R.drawable.image_avata_user)
            .into(binding.ivAvatar)

        // Điền ngày sinh
        user.dateOfBirth?.let { dob ->
            selectedDateOfBirth = dob
            calendar.timeInMillis = dob
            binding.etDob.setText(formatDate(dob))
        }

        // Điền giới tính
        val genderString = when (user.gender) {
            Gender.MALE -> "Male"
            Gender.FEMALE -> "Female"
            Gender.OTHER -> "Other"
            Gender.PREFER_NOT_TO_SAY -> "Prefer not to say"
        }
        binding.actvGender.setText(genderString, false) // false để không filter
    }

    /**
     * Thiết lập các nút bấm
     */
    private fun setupClickListeners() {
        // Nút quay lại
        binding.ivBackButton.setOnClickListener {
            findNavController().popBackStack()
        }

        // Nút Hủy
        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        // Bấm vào ô ngày sinh
        binding.etDob.setOnClickListener {
            showDatePicker()
        }

        // Nút Lưu (lưu thay đổi TEXT)
        binding.btnSave.setOnClickListener {
            saveTextChanges() // Đổi tên hàm cho rõ
        }

        // --- PHẦN MỚI: Click để đổi ảnh ---
        // Bấm vào avatar
        binding.ivAvatar.setOnClickListener {
            imageTypeToUpdate = "avatar"
            openGallery()
        }

        // Bấm vào ảnh bìa
        binding.ivHeaderImage.setOnClickListener {
            imageTypeToUpdate = "header"
            openGallery()
        }
        // --- HẾT PHẦN MỚI ---
    }

    /**
     * Hiển thị bảng chọn ngày
     */
    private fun showDatePicker() {
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                selectedDateOfBirth = calendar.timeInMillis
                binding.etDob.setText(formatDate(selectedDateOfBirth))
            }

        DatePickerDialog(
            requireContext(),
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * Thiết lập menu thả xuống cho Giới tính
     */
    private fun setupGenderDropdown() {
        val genders = listOf("Male", "Female", "Other", "Prefer not to say")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genders)
        binding.actvGender.setAdapter(adapter)
    }

    // --- PHẦN MỚI: Hàm mở thư viện ---
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }
    // --- HẾT PHẦN MỚI ---

    /**
     * Lấy dữ liệu TEXT và gọi ViewModel để lưu
     */
    private fun saveTextChanges() { // Đổi tên từ saveChanges
        // Lấy tên và tách ra
        val fullName = binding.etName.text.toString().trim()
        val names = fullName.split(" ", limit = 2)
        val firstName = names.getOrNull(0) ?: ""
        val lastName = names.getOrNull(1) ?: ""

        // Lấy giới tính và chuyển về Enum
        val genderString = binding.actvGender.text.toString()
            .uppercase(Locale.ROOT)
            .replace(" ", "_")
            .replace("PREFER_NOT_TO_SAY", "PREFER_NOT_TO_SAY")

        val gender = try {
            Gender.valueOf(genderString)
        } catch (e: Exception) {
            Gender.PREFER_NOT_TO_SAY
        }

        // Lấy ngày sinh (đã lưu trong selectedDateOfBirth)
        val dob = selectedDateOfBirth

        // === CẬP NHẬT 2: Lấy text từ ô bio ===
        val bio = binding.etBio.text.toString().trim()
        // ======================================

        // === CẬP NHẬT 3: Gọi ViewModel với 'bio' ===
        viewModel.updateProfile(firstName, lastName, dob, gender, bio)
        // ========================================
    }

    private fun formatDate(milliseconds: Long?): String {
        if (milliseconds == null) return ""
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formatter.format(milliseconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Dọn dẹp binding
    }
}