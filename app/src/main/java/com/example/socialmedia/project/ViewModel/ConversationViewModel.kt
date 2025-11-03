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


    fun createOrGetConversation(currentUserId: String, otherUserId: String, onComplete: (String) -> Unit) {
        chatRepository.createOrGetConversation(currentUserId, otherUserId, onComplete)
    }

    fun listenConversationsRealtime() {
        db.collection("conversations")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val conversations = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ConversationModel::class.java)?.copy(conversationId = doc.id)
                }
                _conversations.postValue(conversations)

                // Tạo listener cho từng conversation để cập nhật unreadCount realtime
                conversations.forEach { conv ->
                    if (conversationListeners[conv.conversationId] == null) {
                        val listener = db.collection("conversations")
                            .document(conv.conversationId)
                            .addSnapshotListener { docSnapshot, _ ->
                                if (docSnapshot != null && docSnapshot.exists()) {
                                    val updatedConv = docSnapshot.toObject(ConversationModel::class.java)
                                        ?.copy(conversationId = docSnapshot.id)
                                    updatedConv?.let { updated ->
                                        // Cập nhật conversation trong list
                                        val currentList = _conversations.value?.toMutableList() ?: mutableListOf()
                                        val index = currentList.indexOfFirst { it.conversationId == updated.conversationId }
                                        if (index >= 0) {
                                            currentList[index] = updated
                                            _conversations.postValue(currentList)
                                        }
                                    }
                                }
                            }
                        conversationListeners[conv.conversationId] = listener
                    }
                }
            }
    }
    fun loadConversations() {
        db.collection("conversations")
            .whereArrayContains("participants", currentUserId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _conversations.postValue(emptyList())
                    return@addSnapshotListener
                }

                val conversations = snapshot.documents.mapNotNull { doc ->
                    val conv = doc.toObject(ConversationModel::class.java)?.copy(conversationId = doc.id)
                    conv?.copy(
                        name = doc.getString("lastMessageSenderName") ?: "Người dùng",
                        photoUrl = doc.getString("lastMessageSenderAvatar") ?: ""
                    )
                }

                _conversations.postValue(conversations)
            }
    }

    fun markConversationAsRead(conversationId: String, userId: String) {
        db.collection("conversations").document(conversationId)
            .update("unreadCount.$userId", 0)
    }

    fun listenConversationsRealtime(currentUserId: String) {
        chatRepository.loadAllConversations(currentUserId) { list ->
            _conversations.postValue(list)
        }
    }




    fun removeAllListeners() {
        conversationListeners.values.forEach { it.remove() }
        conversationListeners.clear()
    }
}
