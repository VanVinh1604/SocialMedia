package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentPersonalInformationBinding
import com.example.socialmedia.project.Domain.Enum.Gender
import com.example.socialmedia.project.ViewModel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class PersonalInformationFragment : Fragment() {

    private var _binding: FragmentPersonalInformationBinding? = null
    private val binding get() = _binding!!

    // Dùng ViewModel CHIA SẺ từ nav_graph
    private val viewModel: ProfileViewModel by navGraphViewModels(R.id.nav_graph)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPersonalInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Nút Back
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // Lắng nghe dữ liệu user (đã được tải bởi ProfileFragment)
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvEmail.text = user.email
                binding.tvPhone.text = user.phoneNumber ?: "Chưa cập nhật"

                binding.tvGender.text = when (user.gender) {
                    Gender.MALE -> "Nam"
                    Gender.FEMALE -> "Nữ"
                    Gender.OTHER -> "Khác"
                    else -> "Chưa cập nhật"
                }

                binding.tvBirthday.text = user.dateOfBirth?.let {
                    formatDate(it)
                } ?: "Chưa cập nhật"
            }
        }
    }

    private fun formatDate(milliseconds: Long): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formatter.format(milliseconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}