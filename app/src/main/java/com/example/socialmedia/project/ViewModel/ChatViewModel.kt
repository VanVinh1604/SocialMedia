// ChatViewModel.kt
package com.example.socialmedia.project.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmedia.project.Domain.Model.MessageModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.Repository.ChatRepository
import com.example.socialmedia.project.Repository.GroupRepository
import com.example.socialmedia.project.Repository.UserRepository
import com.example.socialmedia.project.Server.Firebase.FirebaseService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class ChatViewModel : ViewModel() {

    private val repository = ChatRepository()
    private val db = FirebaseFirestore.getInstance()

    private val firebaseService = FirebaseService()

    private val _leaveGroupResult = MutableLiveData<Boolean>()
    val leaveGroupResult: LiveData<Boolean> get() = _leaveGroupResult
    private val userRepo = UserRepository()

    val messages: LiveData<List<MessageModel>> get() = repository.messagesLiveData

    val currentUserId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _currentParticipants = MutableLiveData<List<UserModel>>()
    val currentParticipants: LiveData<List<UserModel>> get() = _currentParticipants

    fun loadLatestMessages(conversationId: String?, limit: Long = 20, onLoaded: (List<MessageModel>) -> Unit) {
        repository.loadLatestMessages(conversationId, limit, onLoaded)
    }

    fun getParticipants(conversationId: String, callback: (List<UserModel>) -> Unit) {
        db.collection("conversations")
            .document(conversationId)
            .get()
            .addOnSuccessListener { doc ->
                val participantIds = doc.get("participants") as? List<*>
                val userIds = participantIds?.mapNotNull { it as? String }?.filter { it.isNotBlank() }
                    ?: emptyList()

                if (userIds.isEmpty()) {
                    callback(emptyList())
                    return@addOnSuccessListener
                }

                // Lấy thông tin từng user từ Realtime Database
                val users = mutableListOf<UserModel>()
                var loadedCount = 0

                userIds.forEach { userId ->
                    FirebaseDatabase.getInstance()
                        .getReference("InfoUser")
                        .child(userId)
                        .get()
                        .addOnSuccessListener { userSnap ->
                            val fullName = userSnap.child("fullName").getValue(String::class.java)
                                ?: "Người dùng"
                            val avatar = userSnap.child("profilePictureUrl").getValue(String::class.java)

                            users.add(
                                UserModel(
                                    userId = userId,
                                    fullName = fullName,
                                    profilePictureUrl = avatar
                                )
                            )

                            loadedCount++
                            if (loadedCount == userIds.size) {
                                callback(users)
                            }
                        }
                        .addOnFailureListener {
                            loadedCount++
                            if (loadedCount == userIds.size) {
                                callback(users)
                            }
                        }
                }
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

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

    fun getGroupPreviewAvatar(userIds: List<String>, callback: (List<String?>) -> Unit) {
        repository.getGroupPreviewAvatar(userIds, callback)
    }

    fun getParticipantsLive(conversationId: String): LiveData<List<UserModel>> {
        val result = MutableLiveData<List<UserModel>>()
        getParticipants(conversationId) { users ->
            result.postValue(users)
        }
        return result
    }


    fun sendMessage(
        conversationId: String?,
        participants: List<String>,
        message: MessageModel,
        onComplete: (Boolean) -> Unit
    ) {
        repository.sendMessage(conversationId, participants, message, onComplete)
    }

    fun addMembersToConversation(conversationId: String, newUserIds: List<String>) {
        val repo = GroupRepository()
        repo.addMembersToExistingGroup(conversationId, newUserIds) {
            // reload để UI cập nhật ngay
            listenParticipants(conversationId)
        }
    }
    fun getFollowedUsers(): LiveData<List<UserModel>> {
        val result = MutableLiveData<List<UserModel>>()
        val uid = firebaseService.getCurrentUserId() ?: return result

        userRepo.getFriends(uid) { users ->
            result.postValue(users)
        }

        return result
    }


    fun leaveGroup(conversationId: String) {
        repository.leaveGroup(conversationId, currentUserId) { success ->
            _leaveGroupResult.postValue(success)
        }
    }

    fun listenParticipants(conversationId: String) {
        db.collection("conversations")
            .document(conversationId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val participantIds = snapshot.get("participants") as? List<String> ?: emptyList()
                    getUsersByIds(participantIds) { users ->
                        _currentParticipants.postValue(users)
                    }
                }
            }
    }

    private fun getUsersByIds(
        userIds: List<String>,
        callback: (List<UserModel>) -> Unit
    ) {
        if (userIds.isEmpty()) {
            callback(emptyList())
            return
        }

        val users = mutableListOf<UserModel>()
        var loaded = 0

        userIds.forEach { uid ->
            FirebaseDatabase.getInstance().getReference("InfoUser")
                .child(uid)
                .get()
                .addOnSuccessListener { snap ->
                    val fullName = snap.child("fullName").getValue(String::class.java) ?: "Người dùng"
                    val avatar = snap.child("profilePictureUrl").getValue(String::class.java)

                    users.add(UserModel(uid, fullName, avatar))

                    loaded++
                    if (loaded == userIds.size) callback(users)
                }
                .addOnFailureListener {
                    loaded++
                    if (loaded == userIds.size) callback(users)
                }
        }
    }



    fun removeListener() {
        repository.removeListener()
    }

}