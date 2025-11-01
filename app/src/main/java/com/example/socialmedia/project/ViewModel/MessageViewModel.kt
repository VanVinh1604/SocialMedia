package com.example.socialmedia.project.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmedia.project.Domain.Model.MessageItem
import com.example.socialmedia.project.Domain.Model.MessageModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.data.repository.MessageRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MessageViewModel(private val repository: MessageRepository = MessageRepository()) : ViewModel() {

    private val _chatUsers = MutableLiveData<List<UserModel>>()
    val chatUsers: LiveData<List<UserModel>> = _chatUsers


    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _messages = MutableLiveData<List<MessageItem>>()
    val messages: LiveData<List<MessageItem>> get() = _messages

    private val database = FirebaseDatabase.getInstance().getReference("LastMessages")
    private var messageListener: ValueEventListener? = null


    fun loadChatUsers(currentUserId: String) {
        repository.getChatAvailableUsers(currentUserId,
            onSuccess = { _chatUsers.value = it },
            onFailure = { _error.value = it.message }
        )
    }

    fun getMessagesWithUser(currentUserId: String, otherUserId: String) {
        repository.listenMessagesForUser(currentUserId, otherUserId,
            onSuccess = { messageModels ->
                val messageItems = if (messageModels.isEmpty()) {
                    // Tạo placeholder khi chưa có tin nhắn
                    listOf(
                        MessageItem(
                            userId = otherUserId,
                            name = "Người dùng", // Hoặc lấy từ InfoUser
                            lastMessage = "Bạn đã kết bạn với người này!",
                            time = formatTime(System.currentTimeMillis()),
                            userProfileImage = null
                        )
                    )
                } else {
                    messageModels.map { msg ->
                        MessageItem(
                            userId = if (msg.senderId == currentUserId) otherUserId else msg.senderId,
                            name = msg.senderName,
                            lastMessage = msg.content,
                            time = formatTime(msg.createdAt),
                            userProfileImage = msg.senderAvatar
                        )
                    }
                }

                _messages.value = messageItems
            },
            onFailure = { _error.value = it.message }
        )
    }

    fun loadLastMessages(currentUserId: String) {
        repository.getChatAvailableUsers(currentUserId,
            onSuccess = { users ->
                val messageItems = mutableListOf<MessageItem>()

                users.forEach { user ->
                    // Lấy conversationId
                    val conversationId = if (currentUserId < user.userId)
                        "$currentUserId-${user.userId}"
                    else
                        "${user.userId}-$currentUserId"

                    // Kiểm tra LastMessage từ DB
                    database.child(currentUserId).child(conversationId).get()
                        .addOnSuccessListener { snapshot ->
                            val lastMessageText = snapshot.child("lastMessage").getValue(String::class.java)
                            val lastMessageTime = snapshot.child("time").getValue(Long::class.java) ?: System.currentTimeMillis()

                            val item = MessageItem(
                                userId = user.userId,
                                name = user.fullName,
                                lastMessage = lastMessageText ?: "Bạn đã kết bạn với người này!",
                                time = formatTime(lastMessageTime),
                                userProfileImage = user.profilePictureUrl,
                                isOnline = user.isOnline
                            )

                            messageItems.add(item)

                            // Khi đã load xong tất cả → cập nhật adapter
                            if (messageItems.size == users.size) {
                                _messages.value = messageItems.sortedByDescending { it.time }
                            }
                        }
                }

            },
            onFailure = { _error.value = it.message }
        )
    }


    override fun onCleared() {
        super.onCleared()
        messageListener?.let { database.removeEventListener(it) }
    }

    fun sendMessage(conversationId: String, senderId: String, text: String,
                    onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        repository.sendMessage(conversationId, senderId, text, onSuccess, onFailure)
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }


}

