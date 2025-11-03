package com.example.socialmedia.project.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels // <-- Đảm bảo bạn import 'viewModels'
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.socialmedia.databinding.FragmentChangePasswordBinding
import com.example.socialmedia.project.ViewModel.SettingViewModel // <-- Import ViewModel

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    // 1. Khởi tạo ViewModel
    private val viewModel: SettingViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        // 2. Gọi hàm lắng nghe ViewModel
        setupObservers()
    }

    private fun setupListeners() {
        // Nút Back
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // Nút Save
        binding.btnSaveChanges.setOnClickListener {
            handleChangePassword()
        }
    }

    /**
     * 3. Thiết lập lắng nghe LiveData từ ViewModel
     */
    private fun setupObservers() {
        // Lắng nghe trạng thái loading
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.btnSaveChanges.isEnabled = !isLoading
            binding.etCurrentPassword.isEnabled = !isLoading
            binding.etNewPassword.isEnabled = !isLoading
            binding.etConfirmPassword.isEnabled = !isLoading
        })

        // Lắng nghe lỗi
        viewModel.errorMessage.observe(viewLifecycleOwner, Observer { error ->
            if (error != null) {
                // Hiển thị lỗi (ví dụ: "Mật khẩu hiện tại không đúng")
                binding.tilCurrentPassword.error = error
                viewModel.clearErrorMessage() // Reset lỗi sau khi hiển thị
            } else {
                binding.tilCurrentPassword.error = null
            }
        })

        // Lắng nghe thành công
        viewModel.changeSuccess.observe(viewLifecycleOwner, Observer { isSuccess ->
            if (isSuccess) {
                // BÂY GIỜ ĐÂY LÀ THÔNG BÁO THÀNH CÔNG THẬT
                Toast.makeText(context, "Đổi mật khẩu thành công!", Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
            }
        })
    }


    /**
     * 4. Sửa lại hàm này để gọi ViewModel (ĐÃ SỬA)
     */
    private fun handleChangePassword() {
        val currentPass = binding.etCurrentPassword.text.toString()
        val newPass = binding.etNewPassword.text.toString()
        val confirmPass = binding.etConfirmPassword.text.toString()

        // Xóa lỗi cũ
        binding.tilCurrentPassword.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null

        // (Validation cơ bản vẫn giữ ở Fragment)
        if (currentPass.isEmpty()) {
            binding.tilCurrentPassword.error = "Không được để trống"
            return
        }

        if (newPass.length < 6) {
            binding.tilNewPassword.error = "Mật khẩu mới phải ít nhất 6 ký tự"
            return
        }

        if (newPass != confirmPass) {
            binding.tilConfirmPassword.error = "Mật khẩu xác nhận không khớp"
            return
        }

        // === SỬA LỖI Ở ĐÂY ===
        // Xóa Toast giả lập
        // Toast.makeText(context, "Password changed successfully! (Simulation)", Toast.LENGTH_LONG).show()

        // Gọi ViewModel để xử lý logic Firebase
        viewModel.updateFirebasePassword(currentPass, newPass)

        // (Không quay lại (popBackStack) nữa, chúng ta sẽ đợi LiveData 'changeSuccess' báo thành công)
        // findNavController().popBackStack()
        // === KẾT THÚC SỬA LỖI ===
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

