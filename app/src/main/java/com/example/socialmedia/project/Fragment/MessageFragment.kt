package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialmedia.databinding.FragmentMessageBinding
import com.example.socialmedia.project.Adapter.UserOnlineAdapter
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.Helper.TextGradientUtils
import com.example.socialmedia.project.ViewModel.MessageViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MessageFragment : Fragment() {

    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MessageViewModel
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private var userAdapter: UserOnlineAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentMessageBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[MessageViewModel::class.java]

        TextGradientUtils.applyGradient(binding.tvMessage, "#FF6FB1", "#9B59B6")

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }


        binding.rvUsers.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        setupUserAdapter()
        viewModel.loadChatUsers(currentUserId)
    }

    private fun setupUserAdapter() {
        userAdapter = UserOnlineAdapter(mutableListOf()) { clickedUser ->
            // Tạo conversationId nếu chưa có
            getOrCreateConversationId(currentUserId, clickedUser.userId) { convId ->
                val action = MessageFragmentDirections.actionMessageFragmentToChatFragment(
                    conversationId = convId,
                    userId = clickedUser.userId,
                    userName = clickedUser.fullName,
                    userAvatar = clickedUser.profilePictureUrl ?: ""
                )
                findNavController().navigate(action)
            }
        }
        binding.rvUsers.adapter = userAdapter

        viewModel.chatUsers.observe(viewLifecycleOwner) { users ->
            val sortedUsers = users.sortedByDescending { it.isOnline }
            userAdapter?.updateList(sortedUsers)
        }
    }

    private fun getOrCreateConversationId(currentUserId: String, otherUserId: String, onComplete: (String) -> Unit) {
        val ids = listOf(currentUserId, otherUserId).sorted()
        val conversationId = ids.joinToString("_")
        val docRef = FirebaseFirestore.getInstance().collection("conversations").document(conversationId)

        docRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                docRef.set(mapOf("createdAt" to System.currentTimeMillis()))
                    .addOnSuccessListener { onComplete(conversationId) }
                    .addOnFailureListener { onComplete(conversationId) }
            } else {
                onComplete(conversationId)
            }
        }.addOnFailureListener { onComplete(conversationId) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}