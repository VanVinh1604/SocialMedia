package com.example.socialmedia.project.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmedia.R
import com.example.socialmedia.project.Adapter.UserOnlineAdapter
import com.example.socialmedia.project.Domain.UserTestMess

class MessageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_message, container, false)

        val rvUsers = view.findViewById<RecyclerView>(R.id.rvUsers)
        rvUsers.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        // Tạo dữ liệu mẫu
        val userList = listOf(
            UserTestMess("Alice", isOnline = true),
            UserTestMess("Bob", isOnline = false),
            UserTestMess("Charlie", isOnline = true),
            UserTestMess("David", isOnline = true),
            UserTestMess("Eve", isOnline = false)
        )

        val adapter = UserOnlineAdapter(userList)
        rvUsers.adapter = adapter

        return view
    }
}
