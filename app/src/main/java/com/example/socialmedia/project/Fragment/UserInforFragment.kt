package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.socialmedia.project.ViewModel.RegisterViewModel
import com.example.socialmedia.project.Fragment.State.Resource
import com.example.socialmedia.project.Helper.TextGradientUtils
import com.example.socialmedia.R
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.*

class UserInforFragment : Fragment() {

    private val registerViewModel: RegisterViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_infor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etFirstName = view.findViewById<EditText>(R.id.etFirstName)
        val etLastName = view.findViewById<EditText>(R.id.etLastName)
        val etDOB = view.findViewById<EditText>(R.id.etDOB)
        val rbMale = view.findViewById<RadioButton>(R.id.rbMale)
        val rbFemale = view.findViewById<RadioButton>(R.id.rbFemale)
        val etPhone = view.findViewById<EditText>(R.id.etPhoneNumber)
        val btnNext = view.findViewById<Button>(R.id.btnNext)
        val ivBack = view.findViewById<LinearLayout>(R.id.btnBack)
        val tvTitle = view.findViewById<TextView>(R.id.tvUserInfoTitle)
        val genderGroup = view.findViewById<RadioGroup>(R.id.genderGroup)

        // Áp dụng gradient giống Social App
        TextGradientUtils.applyGradient(tvTitle, "#FF6FB1", "#9B59B6")

        // Nút back
        ivBack.setOnClickListener {
            parentFragmentManager.popBackStack() // quay lại fragment trước đó
        }

        etDOB.setOnClickListener {
            val builder = MaterialDatePicker.Builder.datePicker()
            builder.setTitleText("Select your birth date")

            // Giới hạn ngày lớn nhất là hôm nay (không cho chọn tương lai)
            val constraintsBuilder = CalendarConstraints.Builder()
                .setEnd(MaterialDatePicker.todayInUtcMilliseconds()) // max = hôm nay
                .build()
            builder.setCalendarConstraints(constraintsBuilder)

            val picker = builder.build()

            picker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection

                etDOB.setText(
                    String.format(
                        "%02d/%02d/%04d",
                        calendar.get(Calendar.DAY_OF_MONTH),
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.YEAR)
                    )
                )
            }

            picker.show(parentFragmentManager, "DATE_PICKER")
        }


        registerViewModel.registerResult.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> btnNext.isEnabled = false
                is Resource.Success -> {
                    btnNext.isEnabled = true
                    Toast.makeText(requireContext(), "Register success!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, LoginFragment())
                        .commitAllowingStateLoss()
                }
                is Resource.Error -> {
                    btnNext.isEnabled = true
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnNext.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val dobStr = etDOB.text.toString().trim()
            val gender = when (genderGroup.checkedRadioButtonId) {
                R.id.rbMale -> "male"
                R.id.rbFemale -> "female"
                else -> ""
            }
            val phone = etPhone.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || dobStr.isEmpty() || gender.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val parts = dobStr.split("/")
            val calendar = Calendar.getInstance().apply {
                set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
            }
            val dobTimestamp = calendar.timeInMillis

            registerViewModel.setPersonalInfo(firstName, lastName, gender, dobTimestamp, phone)
            registerViewModel.registerUser()
        }
    }
}
