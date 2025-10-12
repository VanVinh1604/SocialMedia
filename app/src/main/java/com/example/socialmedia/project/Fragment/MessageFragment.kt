package com.example.socialmedia.project.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentMessageBinding
import com.example.socialmedia.project.Adapter.MessageAdapter
import com.example.socialmedia.project.Adapter.MessageItem
import com.example.socialmedia.project.Adapter.UserOnlineAdapter
import com.example.socialmedia.project.Domain.UserTestMess
import com.example.socialmedia.project.Helper.TextGradientUtils

class MessageFragment : Fragment() {

    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        TextGradientUtils.applyGradient(binding.tvMessage, "#FF6FB1", "#9B59B6")

        setupUserRecyclerView()
        setupMessageRecyclerView()
        setupClickListeners()
    }

    private fun setupUserRecyclerView() {
        val userList = listOf(
            UserTestMess("Alice", isOnline = true),
            UserTestMess("Bob", isOnline = false),
            UserTestMess("Charlie", isOnline = true),
            UserTestMess("David", isOnline = true),
            UserTestMess("Eve", isOnline = false)
        )

        binding.rvUsers.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = UserOnlineAdapter(userList)
        }
    }

    private fun setupMessageRecyclerView() {
        val messages = listOf(
            MessageItem("Alice", "Hello, how are you?", "12:45 PM", 3, true),
            MessageItem("Bob", "Did you see the news?", "11:20 AM", 0, false),
            MessageItem("Charlie", "Let's meet tomorrow.", "Yesterday", 12, true)
        )

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = MessageAdapter(messages) { item ->
                // Xử lý click vào message item
            }
        }
    }


    private fun setupClickListeners() {
        // Quay về HomeFragment
        binding.ivBack.setOnClickListener {
            findNavController().navigate(R.id.action_messageFragment_to_homeFragment)
        }

        // Nếu muốn có nút "Write" nav sang fragment khác, ví dụ UploadFragment

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
