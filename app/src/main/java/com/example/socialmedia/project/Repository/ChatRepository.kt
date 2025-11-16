package com.example.socialmedia.project.Repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.socialmedia.project.Domain.Enum.MessageType
import com.example.socialmedia.project.Domain.Model.ConversationModel
import com.example.socialmedia.project.Domain.Model.MessageModel
import com.google.firebase.firestore.*
import java.util.UUID

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val _messagesLiveData = MutableLiveData<List<MessageModel>>()
    val messagesLiveData: LiveData<List<MessageModel>> get() = _messagesLiveData

    private var listener: ListenerRegistration? = null
    private var initialLoadLimit: Long = 20

    // ✅ Sử dụng snapshot listener ngay từ đầu để realtime update
    fun loadLatestMessages(
        conversationId: String?,
        limit: Long,
        onLoaded: (List<MessageModel>) -> Unit
    ) {
        if (conversationId.isNullOrEmpty()) {
            Log.e("ChatRepository", "Invalid conversationId!")
            onLoaded(emptyList())
            return
        }

        initialLoadLimit = limit

        // Loại bỏ listener cũ
        listener?.remove()
        listener = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Failed loadLatestMessages", error)
                    onLoaded(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    onLoaded(emptyList())
                    return@addSnapshotListener
                }

                val messages = snapshot.documents.mapNotNull { doc ->
                    // Lấy isDeleted từ Firestore, ép Boolean
                    val isDeleted = doc.getBoolean("isDeleted") ?: false
                    Log.d("ChatRepository", "🔍 Document ${doc.id}: isDeleted from Firestore = $isDeleted")

                    // Map object
                    val message = doc.toObject(MessageModel::class.java)
                    if (message != null) {
                        Log.d("ChatRepository", "📦 Mapped message ${doc.id}: isDeleted = $isDeleted")
                        message.copy(
                            messageId = doc.id,
                            isDeleted = isDeleted
                        )
                    } else {
                        Log.e("ChatRepository", "❌ Failed to map document ${doc.id}")
                        null
                    }
                }

                Log.d("ChatRepository", "✅ Loaded ${messages.size} messages, isDeleted flags: ${messages.map { it.isDeleted }}")

                // Cập nhật LiveData và callback
                _messagesLiveData.postValue(messages)
                onLoaded(messages)
            }
    }


    // ✅ Loại bỏ listenNewMessages vì loadLatestMessages đã dùng snapshot listener
    @Deprecated("Use loadLatestMessages with snapshot listener instead")
    fun listenNewMessages(conversationId: String?) {
        // Không cần nữa vì loadLatestMessages đã listen realtime
        Log.d("ChatRepository", "listenNewMessages is deprecated, using snapshot in loadLatestMessages")
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
                val messages = snapshot.documents.mapNotNull {
                    it.toObject(MessageModel::class.java)?.copy(
                        messageId = it.id
                    )
                }.sortedBy { it.createdAt }
                onLoaded(messages)
            }
            .addOnFailureListener {
                Log.e("ChatRepository", "Failed loadMoreMessages", it)
                onLoaded(emptyList())
            }
    }

    fun editMessage(conversationId: String, message: MessageModel, onComplete: (Boolean) -> Unit) {
        db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .document(message.messageId)
            .update(
                mapOf(
                    "content" to message.content,
                    "isEdited" to true,
                    "editedAt" to message.editedAt,
                    "editHistory" to message.editHistory
                )
            )
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                Log.e("ChatRepository", "Failed to edit message", e)
                onComplete(false)
            }
    }

    fun sendMessage(
        conversationId: String?,
        participants: List<String>,
        message: MessageModel,
        onComplete: (Boolean) -> Unit
    ) {
        if (participants.isEmpty()) {
            onComplete(false)
            return
        }

        if (conversationId.isNullOrEmpty()) {
            getOrCreateConversation(participants) { newConvId ->
                sendMessage(newConvId, participants, message, onComplete)
            }
            return
        }

        val msgId = if (message.messageId.isNotEmpty()) message.messageId else UUID.randomUUID().toString()
        val messageWithId = message.copy(messageId = msgId)
        val conversationRef = db.collection("conversations").document(conversationId)

        conversationRef.collection("messages")
            .document(msgId)
            .set(messageWithId)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("ChatRepository", "Failed sendMessage", task.exception)
                    onComplete(false)
                    return@addOnCompleteListener
                }

                val preview = when {
                    message.content.isNotEmpty() -> message.content
                    message.messageType == MessageType.IMAGE -> "[Hình ảnh]"
                    message.messageType == MessageType.VOICE -> "[Tin nhắn thoại]"
                    message.messageType == MessageType.STORY_REPLY -> "[Đã trả lời story của bạn]"
                    else -> ""
                }

                val updateMap = mutableMapOf<String, Any>(
                    "lastMessagePreview" to preview,
                    "lastMessageAt" to System.currentTimeMillis(),
                    "lastMessageSenderId" to message.senderId,
                    "updatedAt" to System.currentTimeMillis(),
                    "participants" to participants.sorted()
                )

                val unreadMap = participants
                    .filter { it != message.senderId }
                    .associateWith { FieldValue.increment(1) }
                if (unreadMap.isNotEmpty()) updateMap["unreadCount"] = unreadMap

                conversationRef.set(updateMap, SetOptions.merge())
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { e ->
                        Log.e("ChatRepository", "Failed update conversation", e)
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
            .update("unreadCount.$userId", 0)
            .addOnSuccessListener { Log.d("ChatRepository", "Unread reset for $userId") }
            .addOnFailureListener { e -> Log.e("ChatRepository", "Failed reset unread", e) }
    }

    fun deleteMessage(conversationId: String, messageId: String, onComplete: (Boolean) -> Unit) {
        val msgRef = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .document(messageId)

        // Soft delete: chỉ đánh dấu isDeleted = true
        msgRef.update("isDeleted", true)
            .addOnSuccessListener {
                Log.d("ChatRepository", "✅ Message $messageId marked as deleted")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("ChatRepository", "❌ Failed to mark message deleted: ${e.message}", e)
                onComplete(false)
            }
    }


    fun loadAllConversations(currentUserId: String, onLoaded: (List<ConversationModel>) -> Unit) {
        db.collection("conversations")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onLoaded(emptyList())
                    return@addSnapshotListener
                }

                val conversationList = mutableListOf<ConversationModel>()

                snapshot.documents.forEach { doc ->
                    val conv = doc.toObject(ConversationModel::class.java) ?: return@forEach

                    val rawUnread = doc.get("unreadCount") as? Map<*, *>
                    val unreadMap = rawUnread?.mapNotNull { entry ->
                        val key = entry.key as? String
                        val value = (entry.value as? Number)?.toLong()
                        if (key != null && value != null) key to value else null
                    }?.toMap() ?: emptyMap()

                    val conversationId = doc.id
                    val conversationRef = db.collection("conversations").document(conversationId)
                        .collection("messages")
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(1)

                    // Lấy tin nhắn cuối cùng để check isDeleted
                    conversationRef.get().addOnSuccessListener { msgSnapshot ->
                        val lastMsg = msgSnapshot.documents.firstOrNull()
                        val lastMessageIsDeleted = lastMsg?.getBoolean("isDeleted") ?: false

                        val updatedConv = conv.copy(
                            conversationId = conversationId,
                            unreadCount = unreadMap,
                            lastMessageIsDeleted = lastMessageIsDeleted
                        )

                        conversationList.add(updatedConv)

                        // Khi đã load hết tất cả conversation, sort theo lastMessageAt
                        if (conversationList.size == snapshot.documents.size) {
                            val sortedList = conversationList.sortedByDescending { it.lastMessageAt ?: 0 }
                            onLoaded(sortedList)
                        }
                    }.addOnFailureListener {
                        Log.e("ChatRepository", "Failed to get last message for $conversationId", it)
                    }
                }
            }
    }


    fun getOrCreateConversation(
        participants: List<String>,
        onResult: (conversationId: String) -> Unit
    ) {
        if (participants.size < 2) return
        val sortedParticipants = participants.sorted()
        val firstUser = sortedParticipants[0]
        val secondUser = sortedParticipants[1]

        db.collection("conversations")
            .whereArrayContains("participants", firstUser)
            .get()
            .addOnSuccessListener { snapshot ->
                val existingConv = snapshot.documents.find { doc ->
                    val part = doc.get("participants") as? List<*>
                    part?.contains(secondUser) == true && part.size == 2
                }

                if (existingConv != null) {
                    onResult(existingConv.id)
                } else {
                    val newConvRef = db.collection("conversations").document()
                    val newConversation = mapOf(
                        "participants" to sortedParticipants,
                        "createdAt" to System.currentTimeMillis(),
                        "lastMessageAt" to System.currentTimeMillis(),
                        "lastMessagePreview" to "",
                        "unreadCount" to sortedParticipants.associateWith { 0L }
                    )
                    newConvRef.set(newConversation)
                        .addOnSuccessListener { onResult(newConvRef.id) }
                        .addOnFailureListener { e -> Log.e("ChatRepository", "Failed create conversation", e) }
                }
            }
            .addOnFailureListener { e -> Log.e("ChatRepository", "Failed getOrCreateConversation", e) }
    }

    fun removeListener() {
        listener?.remove()
        listener = null
    }
}