// ChatViewModel.kt
package com.example.socialmedia.project.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmedia.project.Domain.Model.MessageModel
import com.example.socialmedia.project.Repository.ChatRepository

class ChatViewModel : ViewModel() {

    private val repository = ChatRepository()
    val messages: LiveData<List<MessageModel>> get() = repository.messagesLiveData

    fun loadLatestMessages(conversationId: String?, limit: Long = 20, onLoaded: (List<MessageModel>) -> Unit) {
        repository.loadLatestMessages(conversationId, limit, onLoaded)
    }

//    fun listenNewMessages(conversationId: String?) {
//        repository.listenNewMessages(conversationId)
//    }

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