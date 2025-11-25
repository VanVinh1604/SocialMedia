// ChatViewModel.kt
package com.example.socialmedia.project.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmedia.project.Domain.Model.MessageModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.Repository.ChatRepository
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class ChatViewModel : ViewModel() {

    private val repository = ChatRepository()
    private val db = FirebaseFirestore.getInstance()

    val messages: LiveData<List<MessageModel>> get() = repository.messagesLiveData

    fun loadLatestMessages(conversationId: String?, limit: Long = 20, onLoaded: (List<MessageModel>) -> Unit) {
        repository.loadLatestMessages(conversationId, limit, onLoaded)
    }

    fun getParticipants(conversationId: String, callback: (List<UserModel>) -> Unit) {
        db.collection("conversations")
            .document(conversationId)
            .get()
            .addOnSuccessListener { doc ->
                val participantIds = doc.get("participants") as? List<*>
                val userIds = participantIds?.mapNotNull { it as? String }?.filter { it.isNotBlank() }
                    ?: emptyList()

                if (userIds.isEmpty()) {
                    callback(emptyList())
                    return@addOnSuccessListener
                }

                // Lấy thông tin từng user từ Realtime Database
                val users = mutableListOf<UserModel>()
                var loadedCount = 0

                userIds.forEach { userId ->
                    FirebaseDatabase.getInstance()
                        .getReference("InfoUser")
                        .child(userId)
                        .get()
                        .addOnSuccessListener { userSnap ->
                            val fullName = userSnap.child("fullName").getValue(String::class.java)
                                ?: "Người dùng"
                            val avatar = userSnap.child("profilePictureUrl").getValue(String::class.java)

                            users.add(
                                UserModel(
                                    userId = userId,
                                    fullName = fullName,
                                    profilePictureUrl = avatar
                                )
                            )

                            loadedCount++
                            if (loadedCount == userIds.size) {
                                callback(users)
                            }
                        }
                        .addOnFailureListener {
                            loadedCount++
                            if (loadedCount == userIds.size) {
                                callback(users)
                            }
                        }
                }
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    fun loadMoreMessages(conversationId: String?, oldestTimestamp: Long, onLoaded: (List<MessageModel>) -> Unit) {
        repository.loadMoreMessages(conversationId, oldestTimestamp, onLoaded)
    }

    fun markMessagesAsRead(conversationId: String, userId: String) {
        repository.markMessagesAsRead(conversationId, userId)
    }


    fun updateMessageLocal(newMsg: MessageModel) {
        val current = repository.messagesLiveData.value?.toMutableList() ?: mutableListOf()
        val index = current.indexOfFirst { it.messageId == newMsg.messageId }
        if (index != -1) {
            current[index] = newMsg

            (repository.messagesLiveData as? MutableLiveData)?.postValue(current)
        }
    }


    fun deleteMessage(conversationId: String, messageId: String, onComplete: (Boolean) -> Unit) {
        repository.deleteMessage(conversationId, messageId) { success ->
            if (success) {
                // Cập nhật local LiveData ngay lập tức để adapter refresh
                val current = repository.messagesLiveData.value?.toMutableList() ?: mutableListOf()
                val index = current.indexOfFirst { it.messageId == messageId }
                if (index != -1) {
                    val updated = current[index].copy(isDeleted = true)
                    current[index] = updated
                    (repository.messagesLiveData as? MutableLiveData)?.postValue(current)
                }
            }
            onComplete(success)
        }
    }

    fun editMessage(conversationId: String, message: MessageModel) {
        repository.editMessage(conversationId, message) { success ->
            if (success) updateMessageLocal(message) // cập nhật local LiveData
        }
    }

    fun getGroupPreviewAvatar(userIds: List<String>, callback: (List<String?>) -> Unit) {
        repository.getGroupPreviewAvatar(userIds, callback)
    }


    fun sendMessage(
        conversationId: String?,
        participants: List<String>,
        message: MessageModel,
        onComplete: (Boolean) -> Unit
    ) {
        repository.sendMessage(conversationId, participants, message, onComplete)
    }

    fun removeListener() {
        repository.removeListener()
    }

}