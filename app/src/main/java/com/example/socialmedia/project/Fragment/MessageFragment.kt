package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialmedia.databinding.FragmentMessageBinding
import com.example.socialmedia.project.Adapter.MessageAdapter
import com.example.socialmedia.project.Adapter.UserOnlineAdapter
import com.example.socialmedia.project.Domain.Enum.ConversationType
import com.example.socialmedia.project.Domain.Model.ConversationModel
import com.example.socialmedia.project.Helper.TextGradientUtils
import com.example.socialmedia.project.ViewModel.ConversationViewModel
import com.example.socialmedia.project.ViewModel.MessageViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class MessageFragment : Fragment() {

    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!

    private lateinit var userAdapter: UserOnlineAdapter
    private lateinit var messageAdapter: MessageAdapter

    private lateinit var viewModel: MessageViewModel
    private val conversationViewModel by viewModels<ConversationViewModel>()

    private var conversationId: String = "" // conversationId đang chat
    private var participants: List<String> = listOf() // id các thành viên

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentMessageBinding.inflate(inflater, container, false).also {
        _binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[MessageViewModel::class.java]
        TextGradientUtils.applyGradient(binding.tvMessage, "#FF6FB1", "#9B59B6")
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }

        setupUserOnlineRecycler()
        setupLastMessagesRecycler()

        viewModel.loadChatUsers(currentUserId)
        observeViewModels()

        // Observe conversations và enrich với info phù hợp
        conversationViewModel.conversations.observe(viewLifecycleOwner) { convList ->
            enrichConversationsWithUserInfo(convList) { enrichedList ->
                messageAdapter.updateList(enrichedList)

                val totalUnread = enrichedList.sumOf { it.unreadCount[currentUserId] ?: 0 }
                if (totalUnread > 0) {
                    binding.tvTotalUnread.text = totalUnread.toString()
                    binding.tvTotalUnread.visibility = View.VISIBLE
                } else {
                    binding.tvTotalUnread.visibility = View.GONE
                }
            }
        }



        // start listening
        conversationViewModel.listenConversationsRealtime()

        binding.ivWrite.setOnClickListener {
            BottomSheetCreateGroup().show(childFragmentManager, "createGroup")
        }
    }

    private fun setupUserOnlineRecycler() {
        binding.rvUsers.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        userAdapter = UserOnlineAdapter(mutableListOf()) { clickedUser ->
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
    }

    private fun setupLastMessagesRecycler() {
        binding.rvMessages.layoutManager = LinearLayoutManager(context)
        messageAdapter = MessageAdapter(emptyList(), currentUserId, conversationViewModel) { conv ->
            val action = if (conv.type == ConversationType.GROUP) {
                MessageFragmentDirections.actionMessageFragmentToChatFragment(
                    conversationId = conv.conversationId,
                    userId = "",
                    userName = conv.name ?: "Nhóm",
                    userAvatar = conv.photoUrl ?: ""
                )
            } else {
                val otherUserId = conv.participants.firstOrNull { it != currentUserId } ?: conv.conversationId
                MessageFragmentDirections.actionMessageFragmentToChatFragment(
                    conversationId = conv.conversationId,
                    userId = otherUserId,
                    userName = conv.name ?: "Người dùng",
                    userAvatar = conv.photoUrl ?: ""
                )
            }
            findNavController().navigate(action)
        }

        binding.rvMessages.adapter = messageAdapter
    }


    // ✅ Enrich conversations với thông tin user thực tế
//    private fun enrichConversationsWithUserInfo(
//        conversations: List<ConversationModel>,
//        callback: (List<ConversationModel>) -> Unit
//    ) {
//        if (conversations.isEmpty()) {
//            callback(emptyList())
//            return
//        }
//
//        val enrichedList = mutableListOf<ConversationModel>()
//        var processedCount = 0
//
//        conversations.forEach { conv ->
//            // Tìm userId của người còn lại (không phải currentUser)
//            val otherUserId = conv.participants.firstOrNull { it != currentUserId }
//
//            if (otherUserId != null) {
//                // ✅ Fetch thông tin user từ InfoUser
//                fetchUserInfoForDisplay(otherUserId) { name, avatar ->
//                    val enrichedConv = conv.copy(
//                        name = name,
//                        photoUrl = avatar
//                    )
//                    enrichedList.add(enrichedConv)
//                    processedCount++
//
//                    // Khi đã xử lý hết tất cả conversations
//                    if (processedCount == conversations.size) {
//                        // Sort theo thời gian tin nhắn cuối
//                        val sortedList = enrichedList.sortedByDescending { it.lastMessageAt ?: 0 }
//                        callback(sortedList)
//                    }
//                }
//            } else {
//                // Trường hợp không tìm thấy otherUserId (lỗi data)
//                enrichedList.add(conv)
//                processedCount++
//
//                if (processedCount == conversations.size) {
//                    val sortedList = enrichedList.sortedByDescending { it.lastMessageAt ?: 0 }
//                    callback(sortedList)
//                }
//            }
//        }
//    }

    // ✅ Fetch thông tin user để hiển thị trong danh sách chat
//    private fun fetchUserInfoForDisplay(userId: String, callback: (String, String?) -> Unit) {
//        val userRef = FirebaseDatabase.getInstance()
//            .getReference("InfoUser")
//            .child(userId)
//
//        userRef.get().addOnSuccessListener { snapshot ->
//            val name = snapshot.child("fullName").getValue(String::class.java) ?: "Người dùng"
//            val avatar = snapshot.child("profilePictureUrl").getValue(String::class.java)
//            callback(name, avatar)
//            Log.d("MessageFragment", "Fetching info for user: $userId")
//            Log.d("MessageFragment", "Got name: $name, avatar: $avatar")
//        }.addOnFailureListener {
//            callback("Người dùng", null)
//
//        }
//        // Trong fetchUserInfoForDisplay()
//
//    }

    private fun observeViewModels() {
        viewModel.chatUsers.observe(viewLifecycleOwner) { users ->
            val sortedUsers = users.sortedByDescending { it.isOnline }
            userAdapter.updateList(sortedUsers)
        }
    }
    private fun enrichConversationsWithUserInfo(
        conversations: List<ConversationModel>,
        callback: (List<ConversationModel>) -> Unit
    ) {
        if (conversations.isEmpty()) {
            callback(emptyList())
            return
        }

        val enrichedList = mutableListOf<ConversationModel>()
        var processed = 0
        conversations.forEach { conv ->
            if (conv.type == ConversationType.GROUP) {
                // Nếu đã có name (admin đặt) thì dùng luôn.
                // Nếu không có name, fallback thành "Nhóm"
                val name = conv.name ?: "Nhóm"
                val updated = conv.copy(name = name)
                // try to fetch photo/avatar preview for group (2 avatars)
                // We'll leave photoUrl empty (adapter uses getGroupPreviewAvatar)
                enrichedList.add(updated)
                processed++
                if (processed == conversations.size) {
                    val sorted = enrichedList.sortedByDescending { it.lastMessageAt ?: 0 }
                    callback(sorted)
                }
            } else {
                // direct chat: tìm other user
                val otherUserId = conv.participants.firstOrNull { it != currentUserId }
                if (otherUserId != null) {
                    fetchUserInfoForDisplay(otherUserId) { name, avatar ->
                        val updated = conv.copy(name = name, photoUrl = avatar)
                        enrichedList.add(updated)
                        processed++
                        if (processed == conversations.size) {
                            val sorted = enrichedList.sortedByDescending { it.lastMessageAt ?: 0 }
                            callback(sorted)
                        }
                    }
                } else {
                    // fallback
                    enrichedList.add(conv)
                    processed++
                    if (processed == conversations.size) {
                        val sorted = enrichedList.sortedByDescending { it.lastMessageAt ?: 0 }
                        callback(sorted)
                    }
                }
            }
        }
    }

    private fun fetchUserInfoForDisplay(userId: String, callback: (String, String?) -> Unit) {
        val userRef = FirebaseDatabase.getInstance()
            .getReference("InfoUser")
            .child(userId)

        userRef.get().addOnSuccessListener { snapshot ->
            val name = snapshot.child("fullName").getValue(String::class.java) ?: "Người dùng"
            val avatar = snapshot.child("profilePictureUrl").getValue(String::class.java)
            callback(name, avatar)
            Log.d("MessageFragment", "Fetched direct user $userId -> $name")
        }.addOnFailureListener {
            callback("Người dùng", null)
        }
    }

    private fun getOrCreateConversationId(
        currentUserId: String,
        otherUserId: String,
        onComplete: (String) -> Unit
    ) {
        val ids = listOf(currentUserId, otherUserId).sorted()
        val conversationId = ids.joinToString("_")
        val docRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("conversations")
            .document(conversationId)

        docRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val data = mapOf(
                    "conversationId" to conversationId,
                    "participants" to listOf(currentUserId, otherUserId).sorted(),
                    "name" to "",
                    "photoUrl" to "",
                    "type" to "DIRECT",
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis(),
                    "lastMessagePreview" to "",
                    "lastMessageAt" to null,
                    "unreadCount" to mapOf(currentUserId to 0L, otherUserId to 0L)
                )
                docRef.set(data)
                    .addOnSuccessListener { onComplete(conversationId) }
                    .addOnFailureListener { onComplete(conversationId) }
            } else onComplete(conversationId)
        }.addOnFailureListener {
            onComplete(conversationId)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        conversationViewModel.removeAllListeners()
        _binding = null
    }
}