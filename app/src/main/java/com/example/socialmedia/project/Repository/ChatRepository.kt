package com.example.socialmedia.project.Repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.socialmedia.project.Domain.Model.MessageModel
import com.google.firebase.firestore.*

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val _messagesLiveData = MutableLiveData<List<MessageModel>>()
    val messagesLiveData: LiveData<List<MessageModel>> get() = _messagesLiveData

    private var listener: ListenerRegistration? = null

    // Load tin nhắn mới nhất
    fun loadLatestMessages(conversationId: String?, limit: Long, onLoaded: (List<MessageModel>) -> Unit) {
        if (conversationId.isNullOrEmpty()) {
            Log.e("ChatRepository", "Invalid conversationId!")
            onLoaded(emptyList())
            return
        }

        db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener { snapshot ->
                val messages = snapshot.documents.mapNotNull { it.toObject(MessageModel::class.java) }
                    .sortedBy { it.createdAt }
                _messagesLiveData.postValue(messages)
                onLoaded(messages)
            }
            .addOnFailureListener {
                Log.e("ChatRepository", "Failed loadLatestMessages", it)
                onLoaded(emptyList())
            }
    }

    // Listen tin nhắn mới realtime
    fun listenNewMessages(conversationId: String?) {
        if (conversationId.isNullOrEmpty()) return

        listener?.remove()
        listener = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val messages = snapshot.documents.mapNotNull { it.toObject(MessageModel::class.java) }
                    .sortedBy { it.createdAt } // Sort theo thời gian
                _messagesLiveData.postValue(messages)
            }
    }

    // Load tin nhắn cũ
    fun loadMoreMessages(conversationId: String?, oldestTimestamp: Long, onLoaded: (List<MessageModel>) -> Unit) {
        if (conversationId.isNullOrEmpty()) {
            onLoaded(emptyList())
            return
        }

        db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .startAfter(oldestTimestamp)
            .limit(20)
            .get()
            .addOnSuccessListener { snapshot ->
                val messages = snapshot.documents.mapNotNull { it.toObject(MessageModel::class.java) }
                    .sortedBy { it.createdAt }
                onLoaded(messages)
            }
            .addOnFailureListener {
                Log.e("ChatRepository", "Failed loadMoreMessages", it)
                onLoaded(emptyList())
            }
    }

    // Gửi tin nhắn
    fun sendMessage(conversationId: String?, message: MessageModel, onComplete: (Boolean) -> Unit) {
        if (conversationId.isNullOrEmpty()) {
            onComplete(false)
            return
        }

        val msgId = if (message.messageId.isNotEmpty()) message.messageId else java.util.UUID.randomUUID().toString()
        val messageWithId = message.copy(messageId = msgId)

        db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .document(msgId) // dùng ID cố định
            .set(messageWithId)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
            .addOnFailureListener {
                android.util.Log.e("ChatRepository", "Failed sendMessage", it)
                onComplete(false)
            }


    }


    fun removeListener() {
        listener?.remove()
        listener = null
    }

}