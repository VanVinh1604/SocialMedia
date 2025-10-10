package com.example.socialmedia.Fragment.Fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.socialmedia.Fragment.ViewModel.RegisterViewModel
import com.example.socialmedia.Fragment.Fragment.State.Resource
import com.example.socialmedia.R
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
        // optional nếu có ProgressBar trong layout

        // Chọn ngày sinh
        etDOB.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(requireContext(),
                { _, year, month, day ->
                    etDOB.setText(String.format("%02d/%02d/%04d", day, month + 1, year))
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Quan sát LiveData registerResult
        registerViewModel.registerResult.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {

                    btnNext.isEnabled = false
                }
                is Resource.Success -> {

                    btnNext.isEnabled = true
                    Toast.makeText(requireContext(), "Register success! ID: ${resource.data?.userId}", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, LoginFragment())
                        .commit()
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
            val gender = when {
                rbMale.isChecked -> "male"
                rbFemale.isChecked -> "female"
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

            // Lưu vào ViewModel
            registerViewModel.setPersonalInfo(firstName, lastName, gender, dobTimestamp)
            registerViewModel.setPhoneNumber(phone)

            // Gọi async đăng ký
            registerViewModel.registerUser()
        }
    }
}
