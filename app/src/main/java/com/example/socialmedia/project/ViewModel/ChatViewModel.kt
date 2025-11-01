// ChatViewModel.kt
package com.example.socialmedia.project.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.socialmedia.project.Domain.Model.MessageModel
import com.example.socialmedia.project.Repository.ChatRepository

class ChatViewModel : ViewModel() {

    private val repository = ChatRepository()
    val messages: LiveData<List<MessageModel>> get() = repository.messagesLiveData

    fun loadLatestMessages(conversationId: String?, limit: Long = 20, onLoaded: (List<MessageModel>) -> Unit) {
        repository.loadLatestMessages(conversationId, limit, onLoaded)
    }

    fun listenNewMessages(conversationId: String?) {
        repository.listenNewMessages(conversationId)
    }

    fun loadMoreMessages(conversationId: String?, oldestTimestamp: Long, onLoaded: (List<MessageModel>) -> Unit) {
        repository.loadMoreMessages(conversationId, oldestTimestamp, onLoaded)
    }

    fun sendMessage(conversationId: String?, message: MessageModel, onComplete: (Boolean) -> Unit) {
        repository.sendMessage(conversationId, message, onComplete)
    }

    fun removeListener() {
        repository.removeListener()
    }

}