package com.example.socialmedia.project.Fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmedia.R
import com.example.socialmedia.project.Adapter.SelectUserAdapter
import com.example.socialmedia.project.Domain.Model.ConversationMemberModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.ViewModel.CreateGroupViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class BottomSheetCreateGroup : BottomSheetDialogFragment() {

    private lateinit var adapter: SelectUserAdapter
    private val selectedUsers = mutableListOf<UserModel>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottomsheet_create_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rvUsers)
        val tvCreate = view.findViewById<TextView>(R.id.tvCreate)
        val tvCancel = view.findViewById<TextView>(R.id.tvCancel)
        val edtGroupName = view.findViewById<EditText>(R.id.edtGroupName)

        rv.layoutManager = LinearLayoutManager(context)

        val viewModel = CreateGroupViewModel()

        viewModel.friends.observe(viewLifecycleOwner) { users ->
            val adapter = SelectUserAdapter(users) { user, isSelected ->
                if (isSelected) selectedUsers.add(user)
                else selectedUsers.remove(user)

                tvCreate.setTextColor(
                    if (selectedUsers.size >= 2) Color.parseColor("#007AFF")
                    else Color.parseColor("#C7C7CC")
                )
            }
            rv.adapter = adapter
        }

        viewModel.loadFriends()

        tvCancel.setOnClickListener { dismiss() }

        tvCreate.setOnClickListener {
            if (selectedUsers.size < 2) return@setOnClickListener

            // Lấy tên nhóm từ EditText
            val customName = edtGroupName.text.toString().trim()

            // Chỉ tạo với những user đã chọn
            viewModel.createGroup(selectedUsers, customName) { conversationId ->
                Log.d("BottomSheetCreateGroup", "Group created: $conversationId")
                Toast.makeText(context, "Nhóm đã được tạo!", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }



}
