package com.example.socialmedia.project.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmedia.project.Domain.Model.ConversationModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.Repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ConversationViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _conversations = MutableLiveData<List<ConversationModel>>()
    val conversations: LiveData<List<ConversationModel>> get() = _conversations

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val chatRepository = ChatRepository()

    private var conversationListeners = mutableMapOf<String, ListenerRegistration>()



    fun markConversationAsRead(conversationId: String, userId: String) {
        db.collection("conversations").document(conversationId)
            .update("unreadCount.$userId", 0)
    }

    fun listenConversationsRealtime(currentUserId: String) {
        chatRepository.loadAllConversations(currentUserId) { list ->
            _conversations.postValue(list)
        }
    }


    fun deleteMessage(conversationId: String, messageId: String) {
        chatRepository.deleteMessage(conversationId, messageId) { success ->
            if (success) listenConversationsRealtime(currentUserId)
        }
    }



    fun removeAllListeners() {
        conversationListeners.values.forEach { it.remove() }
        conversationListeners.clear()
    }
}
