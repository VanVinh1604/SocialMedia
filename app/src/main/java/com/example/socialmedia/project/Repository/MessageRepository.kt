package com.example.socialmedia.project.data.repository

import com.example.socialmedia.project.Domain.Model.MessageModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.Server.Firebase.FirebaseService
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot

class MessageRepository(private val firebaseService: FirebaseService = FirebaseService()) {

    fun getChatAvailableUsers(
        currentUserId: String,
        onSuccess: (List<UserModel>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firebaseService.getMutualFollowUsers(currentUserId,
            onResult = { mutualIds ->
                if (mutualIds.isEmpty()) {
                    onSuccess(emptyList())
                    return@getMutualFollowUsers
                }

                val userList = mutableListOf<UserModel>()
                val tasks = mutableListOf<Task<DataSnapshot>>()

                for (id in mutualIds) {
                    val task = firebaseService.database.child("InfoUser").child(id).get()
                    tasks.add(task)
                    task.addOnSuccessListener { snapshot ->
                        snapshot.getValue(UserModel::class.java)?.let { userList.add(it) }
                    }
                }

                Tasks.whenAllComplete(tasks).addOnSuccessListener { onSuccess(userList) }
            },
            onError = onFailure
        )
    }

    fun listenMessagesForUser(
        currentUserId: String,
        otherUserId: String,
        onSuccess: (List<MessageModel>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val conversationId = if (currentUserId < otherUserId) "$currentUserId-$otherUserId"
        else "$otherUserId-$currentUserId"

        firebaseService.listenMessages(conversationId, onSuccess, onFailure)
    }

    fun sendMessage(
        conversationId: String,
        senderId: String,
        text: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firebaseService.sendMessage(conversationId, senderId, text, onSuccess, onFailure)
    }
}

