package com.example.socialmedia.project.Repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.socialmedia.project.Domain.Model.ConversationModel
import com.example.socialmedia.project.Domain.Model.MessageModel
import com.google.firebase.firestore.*
import java.util.UUID

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val _messagesLiveData = MutableLiveData<List<MessageModel>>()
    val messagesLiveData: LiveData<List<MessageModel>> get() = _messagesLiveData

    private var listener: ListenerRegistration? = null

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
                _messagesLiveData.postValue(messages)
                onLoaded(messages)
            }
            .addOnFailureListener {
                Log.e("ChatRepository", "Failed loadLatestMessages", it)
                onLoaded(emptyList())
            }
    }

    fun listenNewMessages(conversationId: String?) {
        if (conversationId.isNullOrEmpty()) return

        listener?.remove()
        listener = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val messages = snapshot.documents.mapNotNull { it.toObject(MessageModel::class.java) }
                _messagesLiveData.postValue(messages)
            }
    }

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

    // ✅ FINAL: CHỈ lưu lastMessageSenderId, KHÔNG lưu name/avatar
    fun sendMessage(
        conversationId: String?,
        participants: List<String>,
        message: MessageModel,
        onComplete: (Boolean) -> Unit
    ) {
        if (conversationId.isNullOrEmpty()) {
            onComplete(false)
            return
        }

        val msgId = if (message.messageId.isNotEmpty()) message.messageId else UUID.randomUUID().toString()
        val messageWithId = message.copy(messageId = msgId)
        val conversationRef = db.collection("conversations").document(conversationId)

        // Gửi tin nhắn
        conversationRef.collection("messages")
            .document(msgId)
            .set(messageWithId)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val preview = when {
                        message.content.isNotEmpty() -> message.content
                        message.messageType == com.example.socialmedia.project.Domain.Enum.MessageType.IMAGE -> "[Hình ảnh]"
                        message.messageType == com.example.socialmedia.project.Domain.Enum.MessageType.VOICE -> "[Tin nhắn thoại]"
                        else -> ""
                    }

                    // ✅ CHỈ lưu preview, timestamp và senderId
                    val updateMap = mutableMapOf<String, Any>(
                        "lastMessagePreview" to preview,
                        "lastMessageAt" to System.currentTimeMillis(),
                        "lastMessageSenderId" to message.senderId,
                        "updatedAt" to System.currentTimeMillis(),
                        "participants" to participants
                    )

                    participants.filter { it != message.senderId }.forEach { receiverId ->
                        updateMap["unreadCount.$receiverId"] = FieldValue.increment(1)
                    }

                    conversationRef.set(updateMap, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d("ChatRepository", "✅ Updated conversation")
                            onComplete(true)
                        }
                        .addOnFailureListener { e ->
                            Log.e("ChatRepository", "❌ Failed to update", e)
                            onComplete(false)
                        }
                } else {
                    onComplete(false)
                }
            }
            .addOnFailureListener {
                Log.e("ChatRepository", "Failed sendMessage", it)
                onComplete(false)
            }
    }

    fun markMessagesAsRead(conversationId: String?, userId: String) {
        if (conversationId.isNullOrEmpty() || userId.isEmpty()) return
        db.collection("conversations")
            .document(conversationId)
            .set(mapOf("unreadCount.$userId" to 0), SetOptions.merge())
    }

    fun createOrGetConversation(
        currentUserId: String,
        otherUserId: String,
        onComplete: (String) -> Unit
    ) {
        val ids = listOf(currentUserId, otherUserId).sorted()
        val conversationId = ids.joinToString("_")
        val conversationRef = db.collection("conversations").document(conversationId)

        conversationRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val data = mapOf(
                    "conversationId" to conversationId,
                    "participants" to ids,
                    "lastMessagePreview" to "",
                    "lastMessageAt" to null,
                    "lastMessageSenderId" to "",
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis(),
                    "unreadCount" to mapOf(currentUserId to 0, otherUserId to 0)
                )
                conversationRef.set(data).addOnSuccessListener { onComplete(conversationId) }
            } else {
                onComplete(conversationId)
            }
        }
    }

    fun loadAllConversations(currentUserId: String, onLoaded: (List<ConversationModel>) -> Unit) {
        db.collection("conversations")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Log.e("ChatRepository", "Failed to load conversations", error)
                    onLoaded(emptyList())
                    return@addSnapshotListener
                }

                val conversations = snapshot.documents.mapNotNull { doc ->
                    val conv = doc.toObject(ConversationModel::class.java)
                    conv?.copy(conversationId = doc.id)
                }

                Log.d("ChatRepository", "✅ Loaded ${conversations.size} conversations for $currentUserId")
                onLoaded(conversations)
            }
    }



    fun removeListener() {
        listener?.remove()
        listener = null
    }
}