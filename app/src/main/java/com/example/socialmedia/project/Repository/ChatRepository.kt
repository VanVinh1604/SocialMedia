package com.example.socialmedia.project.Repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.socialmedia.project.Domain.Enum.MessageType
import com.example.socialmedia.project.Domain.Model.ConversationModel
import com.example.socialmedia.project.Domain.Model.MessageModel
import com.example.socialmedia.project.Domain.Model.StoryModel
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
//    fun sendMessage(
//        conversationId: String?,
//        participants: List<String>,
//        message: MessageModel,
//        onComplete: (Boolean) -> Unit
//    ) {
//        if (participants.isEmpty()) {
//            onComplete(false)
//            return
//        }
//
//        if (conversationId.isNullOrEmpty()) {
//            getOrCreateConversation(participants) { newConvId ->
//                sendMessage(newConvId, participants, message, onComplete)
//            }
//            return
//        }
//
//
//        val msgId = if (message.messageId.isNotEmpty()) message.messageId else UUID.randomUUID().toString()
//        val messageWithId = message.copy(messageId = msgId)
//        val conversationRef = db.collection("conversations").document(conversationId)
//
//        // 1️⃣ Thêm message
//        conversationRef.collection("messages")
//            .document(msgId)
//            .set(messageWithId)
//            .addOnCompleteListener { task ->
//                if (!task.isSuccessful) {
//                    Log.e("ChatRepository", "Failed sendMessage", task.exception)
//                    onComplete(false)
//                    return@addOnCompleteListener
//                }
//
//                // 2️⃣ Cập nhật conversation preview + unreadCount
//                val preview = when {
//                    message.content.isNotEmpty() -> message.content
//                    message.messageType == MessageType.IMAGE -> "[Hình ảnh]"
//                    message.messageType == MessageType.VOICE -> "[Tin nhắn thoại]"
//                    message.messageType == MessageType.STORY_REPLY -> "[Trả lời story]"
//                    else -> ""
//                }
//
//                val updateMap = mutableMapOf<String, Any>(
//                    "lastMessagePreview" to preview,
//                    "lastMessageAt" to System.currentTimeMillis(),
//                    "lastMessageSenderId" to message.senderId,
//                    "updatedAt" to System.currentTimeMillis(),
//                    "participants" to participants
//                )
//
//                // Tăng unreadCount cho tất cả người nhận trừ sender
//                val unreadMap = participants
//                    .filter { it != message.senderId }
//                    .associateWith { FieldValue.increment(1) }
//                if (unreadMap.isNotEmpty()) updateMap["unreadCount"] = unreadMap
//
//                conversationRef.set(updateMap, SetOptions.merge())
//                    .addOnSuccessListener { onComplete(true) }
//                    .addOnFailureListener { e ->
//                        Log.e("ChatRepository", "Failed update conversation", e)
//                        onComplete(false)
//                    }
//
//            }
//            .addOnFailureListener {
//                Log.e("ChatRepository", "Failed sendMessage", it)
//                onComplete(false)
//            }
//    }

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

        // Nếu chưa có conversationId, tìm hoặc tạo conversation
        if (conversationId.isNullOrEmpty()) {
            getOrCreateConversation(participants) { newConvId ->
                sendMessage(newConvId, participants, message, onComplete)
            }
            return
        }

        val msgId = if (message.messageId.isNotEmpty()) message.messageId else UUID.randomUUID().toString()
        val messageWithId = message.copy(messageId = msgId)
        val conversationRef = db.collection("conversations").document(conversationId)

        // 1️⃣ Thêm message vào collection messages
        conversationRef.collection("messages")
            .document(msgId)
            .set(messageWithId)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("ChatRepository", "Failed sendMessage", task.exception)
                    onComplete(false)
                    return@addOnCompleteListener
                }

                // 2️⃣ Cập nhật conversation preview + lastMessageAt + unreadCount
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

                // Tăng unreadCount cho tất cả người nhận trừ sender
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


    fun sendStoryReply(
        senderId: String,
        receiverId: String,
        story: StoryModel
    ) {
        val participants = listOf(senderId, receiverId)

        // Tìm hoặc tạo conversation chung
        getOrCreateConversation(participants) { conversationId ->
            val replyMsg = MessageModel(
                senderId = senderId,
                content = "Đã trả lời story của bạn",
                messageType = MessageType.STORY_REPLY,
                createdAt = System.currentTimeMillis(),
                isStoryReply = true,
                storyId = story.storyId,
                storyThumbnail = story.thumbnailUrl ?: story.mediaUrl,
                storyOwnerId = story.userId
            )

            sendMessage(conversationId, participants, replyMsg) { success ->
                Log.d("ChatRepository", "Send story reply: $success")
            }
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

                val conversations = snapshot.documents.mapNotNull { doc ->
                    val conv = doc.toObject(ConversationModel::class.java)
                    if (conv != null) {
                        // Đọc unreadCount theo đúng key participantId
                        val rawUnread = doc.get("unreadCount") as? Map<*, *>
                        val unreadMap = rawUnread?.mapNotNull { entry ->
                            val key = entry.key as? String
                            val value = (entry.value as? Number)?.toLong()
                            if (key != null && value != null) key to value else null
                        }?.toMap() ?: emptyMap()

                        conv.copy(
                            conversationId = doc.id,
                            unreadCount = unreadMap
                        )
                    } else null
                }.sortedByDescending { it.lastMessageAt ?: 0 }

                onLoaded(conversations)
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
                // tìm conversation còn lại
                val existingConv = snapshot.documents.find { doc ->
                    val part = doc.get("participants") as? List<*>
                    part?.contains(secondUser) == true && part.size == 2
                }

                if (existingConv != null) {
                    onResult(existingConv.id)
                } else {
                    // tạo mới
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