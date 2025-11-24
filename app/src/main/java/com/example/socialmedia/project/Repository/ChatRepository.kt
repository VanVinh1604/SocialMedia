package com.example.socialmedia.project.Repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.socialmedia.project.Domain.Enum.MessageType
import com.example.socialmedia.project.Domain.Model.ConversationModel
import com.example.socialmedia.project.Domain.Model.MessageModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.*
import java.util.UUID
import kotlin.text.set

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val _messagesLiveData = MutableLiveData<List<MessageModel>>()
    val messagesLiveData: LiveData<List<MessageModel>> get() = _messagesLiveData

    private var listener: ListenerRegistration? = null
    private var initialLoadLimit: Long = 20

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
        listener?.remove()

        listener = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        val m = doc.toObject(MessageModel::class.java)
                        if (m != null) {
                            m.messageId = doc.id
                            m.isDeleted = doc.getBoolean("isDeleted") ?: false
                            m.isEdited = doc.getBoolean("isEdited") ?: false
                            m
                        } else null
                    }
                    _messagesLiveData.postValue(messages)
                    onLoaded(messages)
                }
            }
    }

    fun getGroupPreviewAvatar(
        userIds: List<String>,
        callback: (List<String?>) -> Unit
    ) {
        val limit = userIds.take(3)
        val result = mutableListOf<String?>()
        var count = 0

        limit.forEach { uid ->
            FirebaseDatabase.getInstance()
                .getReference("InfoUser")
                .child(uid)
                .get()
                .addOnSuccessListener { snap ->
                    val avatar = snap.child("profilePictureUrl").getValue(String::class.java)
                    result.add(avatar)
                    count++
                    if (count == limit.size) callback(result)
                }
                .addOnFailureListener {
                    result.add(null)
                    count++
                    if (count == limit.size) callback(result)
                }
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
                val messages = snapshot.documents.mapNotNull { doc ->
                    val message = doc.toObject(MessageModel::class.java)
                    if (message != null) {
                        message.messageId = doc.id
                        message.isDeleted = doc.getBoolean("isDeleted") ?: false
                        message.isEdited = doc.getBoolean("isEdited") ?: false
                        message
                    } else null
                }.sortedBy { it.createdAt }
                onLoaded(messages)
            }
            .addOnFailureListener {
                Log.e("ChatRepository", "Failed loadMoreMessages", it)
                onLoaded(emptyList())
            }
    }

    fun editMessage(conversationId: String, message: MessageModel, onComplete: (Boolean) -> Unit) {
        val msgRef = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .document(message.messageId)

        msgRef.get().addOnSuccessListener { snapshot ->
            val oldMessage = snapshot.toObject(MessageModel::class.java)
            if (oldMessage != null) {
                val newHistory = oldMessage.editHistory?.toMutableList() ?: mutableListOf()
                newHistory.add(oldMessage.content)

                val updateMap = mapOf(
                    "content" to message.content,
                    "isEdited" to true,
                    "editedAt" to System.currentTimeMillis(),
                    "editHistory" to newHistory
                )

                msgRef.update(updateMap)
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { e ->
                        Log.e("ChatRepository", "Failed to edit message", e)
                        onComplete(false)
                    }
            } else {
                onComplete(false)
            }
        }.addOnFailureListener {
            Log.e("ChatRepository", "Failed to get old message", it)
            onComplete(false)
        }
    }

    fun sendMessage(
        conversationId: String?,
        participants: List<String>,
        message: MessageModel,
        onComplete: (Boolean) -> Unit
    ) {
        val validParticipants = participants.filter { it.isNotBlank() }.distinct()
        if (validParticipants.isEmpty()) {
            Log.e("ChatRepository", "❌ Participants list empty after filtering")
            onComplete(false)
            return
        }

        // Hàm tạo conversation nếu chưa có (1-1 hoặc nhóm)
        fun createConversationAndSend() {
            getOrCreateConversation(validParticipants) { newConvId ->
                if (newConvId.isNotEmpty()) {
                    sendMessage(newConvId, validParticipants, message, onComplete)
                } else {
                    Log.e("ChatRepository", "❌ Failed to create conversation")
                    onComplete(false)
                }
            }
        }

        // Nếu chưa có conversationId -> tạo mới
        if (conversationId.isNullOrEmpty()) {
            createConversationAndSend()
            return
        }

        val msgId = if (message.messageId.isNotEmpty()) message.messageId else UUID.randomUUID().toString()
        val msgWithId = message.copy(messageId = msgId)
        val convRef = db.collection("conversations").document(conversationId)

        // Gửi message
        convRef.collection("messages")
            .document(msgId)
            .set(msgWithId)
            .addOnSuccessListener {
                // Tạo preview message
                val preview = when {
                    message.content.isNotEmpty() -> message.content
                    message.messageType == MessageType.IMAGE -> "[Hình ảnh]"
                    message.messageType == MessageType.VOICE -> "[Tin nhắn thoại]"
                    message.messageType == MessageType.STORY_REPLY -> "[Trả lời story]"
                    else -> ""
                }

                // Cập nhật conversation
                val updateMap = mutableMapOf<String, Any>(
                    "lastMessagePreview" to preview,
                    "lastMessageAt" to System.currentTimeMillis(),
                    "lastMessageSenderId" to message.senderId,
                    "updatedAt" to System.currentTimeMillis()
                )

                // Tăng unreadCount cho tất cả trừ sender
                validParticipants.filter { it != message.senderId }.forEach { uid ->
                    updateMap["unreadCount.$uid"] = FieldValue.increment(1)
                }
//                val unreadMap = participants
//                    .filter { it != message.senderId }
//                    .associateWith { FieldValue.increment(1) }
//                if (unreadMap.isNotEmpty()) updateMap["unreadCount"] = unreadMap
//

                convRef.set(updateMap, SetOptions.merge())
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { e ->
                        Log.e("ChatRepository", "❌ Failed to update conversation", e)
                        onComplete(false)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ChatRepository", "❌ Failed to send message", e)
                onComplete(false)
            }
    }

    fun markMessagesAsRead(conversationId: String, userId: String) {
        val convRef = db.collection("conversations").document(conversationId)
        convRef.update("unreadCount.$userId", 0)
            .addOnSuccessListener { Log.d("ChatRepository", "Marked read for $userId") }
            .addOnFailureListener { e -> Log.e("ChatRepository", "Failed mark read", e) }
    }

    fun deleteMessage(conversationId: String, messageId: String, onComplete: (Boolean) -> Unit) {
        val msgRef = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .document(messageId)

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

    fun loadAllConversations(currentUserId: String, onUpdate: (List<ConversationModel>) -> Unit) {
        db.collection("conversations")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    val conv = doc.toObject(ConversationModel::class.java) ?: return@mapNotNull null
                    val unreadRaw = doc.get("unreadCount") as? Map<*, *>
                    val unreadMap = unreadRaw?.mapNotNull {
                        val uid = it.key as? String
                        val count = (it.value as? Number)?.toLong()
                        if (uid != null && count != null) uid to count else null
                    }?.toMap() ?: emptyMap()

                    conv.copy(
                        conversationId = doc.id,
                        unreadCount = unreadMap,
                        lastMessageAt = doc.getLong("lastMessageAt") ?: 0L,
                        lastMessageSenderId = doc.getString("lastMessageSenderId") ?: ""
                    )
                }.sortedByDescending { it.lastMessageAt ?: 0L }

                Log.e("dChatRepository", "Loaded ${list.size} conversations for $currentUserId")
                onUpdate(list)
            }
    }


    fun getOrCreateConversation(participants: List<String>, onResult: (String) -> Unit) {
        if (participants.size != 2) return

        val sorted = participants.sorted()
        val a = sorted[0]
        val b = sorted[1]

        db.collection("conversations").whereArrayContains("participants", a).get()
            .addOnSuccessListener { snap ->
                val exist = snap.documents.find { doc ->
                    val part = doc.get("participants") as? List<*>
                    part?.contains(b) == true && part.size == 2
                }

                if (exist != null) { onResult(exist.id); return@addOnSuccessListener }

                val ref = db.collection("conversations").document()
                val newConv = mapOf(
                    "conversationId" to ref.id,
                    "participants" to sorted,
                    "type" to "DIRECT",
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis(),
                    "lastMessageAt" to 0L,
                    "unreadCount" to sorted.associateWith { 0L }
                )
                ref.set(newConv)
                    .addOnSuccessListener { onResult(ref.id) }
                    .addOnFailureListener { e -> Log.e("ChatRepository", "Failed create conversation", e) }
            }
    }

    fun removeListener() {
        listener?.remove()
        listener = null
    }
}