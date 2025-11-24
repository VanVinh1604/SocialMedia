package com.example.socialmedia.project.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmedia.project.Domain.Model.ConversationModel
import com.example.socialmedia.project.Domain.Model.MessageModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.Repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ConversationViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _conversations = MutableLiveData<List<ConversationModel>>()

    val messagesLiveData: LiveData<List<MessageModel>> get() = chatRepository.messagesLiveData

    val conversations: LiveData<List<ConversationModel>> get() = _conversations

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val chatRepository = ChatRepository()

    private var conversationListeners = mutableMapOf<String, ListenerRegistration>()



    fun markConversationAsRead(conversationId: String, userId: String) {
        db.collection("conversations").document(conversationId)
            .update("unreadCount.$userId", 0)
        Log.e("ConversationViewModel", "Marked conversation $conversationId as read for user $userId")
    }

    fun listenConversationsRealtime() {
        chatRepository.loadAllConversations(currentUserId) { list ->
            _conversations.postValue(list)
        }
    }


    fun deleteMessage(conversationId: String, messageId: String) {
        chatRepository.deleteMessage(conversationId, messageId) { success ->
            if (success) listenConversationsRealtime()
        }
    }



    fun getGroupPreviewAvatar(userIds: List<String>, callback: (List<String?>) -> Unit) {
        chatRepository.getGroupPreviewAvatar(userIds, callback)
    }




    fun removeAllListeners() {
        conversationListeners.values.forEach { it.remove() }
        conversationListeners.clear()
    }
}
