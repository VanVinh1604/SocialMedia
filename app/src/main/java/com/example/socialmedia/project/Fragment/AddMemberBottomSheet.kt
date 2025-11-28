package com.example.socialmedia.project.Fragment

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmedia.R
import com.example.socialmedia.project.Adapter.UserAddMemberAdapter
import com.example.socialmedia.project.Domain.Model.UserModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddMemberBottomSheet(
    private val followedUsers: List<UserModel>,   // tất cả user mà bạn follow
    private val currentMembers: List<String>,     // userId đã trong nhóm
    private val onMembersAdded: (List<String>) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var adapter: UserAddMemberAdapter
    private lateinit var edtSearch: EditText
    private lateinit var rvUsers: RecyclerView
    private lateinit var tvDone: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottomsheet_add_members, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edtSearch = view.findViewById(R.id.edtSearch)
        rvUsers = view.findViewById(R.id.rvUsers)
        tvDone = view.findViewById(R.id.tvDone)

        var selected = listOf<String>()

        adapter = UserAddMemberAdapter(
            followedUsers,
            currentMembers
        ) { selectedList ->
            selected = selectedList
            tvDone.setTextColor(
                if (selected.isNotEmpty()) Color.parseColor("#007AFF")
                else Color.parseColor("#C7C7CC")
            )
        }

        rvUsers.layoutManager = LinearLayoutManager(context)
        rvUsers.adapter = adapter

        // Search realtime
        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString())
            }
        })

        view.findViewById<TextView>(R.id.tvCancel).setOnClickListener {
            dismiss()
        }

        tvDone.setOnClickListener {
            if (selected.isNotEmpty()) {
                onMembersAdded(selected)
                dismiss()
            }
        }
    }
}
