package com.example.socialmedia.project.Fragment // (Hoặc package Fragment của bạn)

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.socialmedia.databinding.FragmentPrivacySettingsBinding
import com.example.socialmedia.project.ViewModel.PrivacyViewModel

class PrivacySettingsFragment : Fragment() {

    private var _binding: FragmentPrivacySettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PrivacyViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPrivacySettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Tắt listener đi, chỉ cho phép thay đổi khi dữ liệu đã tải xong
        binding.switchPrivate.isClickable = false
        binding.switchActivityStatus.isClickable = false
    }

    private fun setupObservers() {
        // Lắng nghe trạng thái tải
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // (Bạn có thể thêm ProgressBar nếu muốn)
            binding.switchPrivate.isClickable = !isLoading
            binding.switchActivityStatus.isClickable = !isLoading
        }

        // Lắng nghe trạng thái Private Account
        viewModel.isPrivateAccount.observe(viewLifecycleOwner) { isPrivate ->
            // Cập nhật Switch (và tắt listener để tránh lặp)
            binding.switchPrivate.setOnCheckedChangeListener(null)
            binding.switchPrivate.isChecked = isPrivate
            // Bật listener trở lại
            binding.switchPrivate.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setPrivateAccount(isChecked)
            }
        }

        // Lắng nghe trạng thái Activity Status
        viewModel.showActivityStatus.observe(viewLifecycleOwner) { showStatus ->
            binding.switchActivityStatus.setOnCheckedChangeListener(null)
            binding.switchActivityStatus.isChecked = showStatus
            binding.switchActivityStatus.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setActivityStatus(isChecked)
            }
        }

        // Lắng nghe thông báo (Toast)
        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                viewModel.clearToastMessage()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}