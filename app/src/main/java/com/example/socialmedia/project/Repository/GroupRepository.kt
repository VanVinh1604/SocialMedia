package com.example.socialmedia.project.Repository

import android.util.Log
import com.example.socialmedia.project.Domain.Model.ConversationMemberModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class GroupRepository {

    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    fun createGroup(
        users: List<UserModel>,
        customName: String?,
        onComplete: (String?) -> Unit
    ) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // STEP 1: lấy thông tin user đang tạo nhóm
        db.collection("InfoUser")
            .document(currentUid)
            .get()
            .addOnSuccessListener { snapshot ->

                val currentUser = snapshot.toObject(UserModel::class.java)?.copy(userId = currentUid)

                if (currentUser != null) {
                    // tiếp tục tạo nhóm trực tiếp trong hàm
                    buildGroup(users, currentUser, customName, onComplete)
                } else {
                    // Nếu Firestore không có → lấy từ RealTime
                    FirebaseDatabase.getInstance().getReference("InfoUser")
                        .child(currentUid)
                        .get()
                        .addOnSuccessListener { rtSnapshot ->

                            val rtUser = rtSnapshot.getValue(UserModel::class.java)

                            val finalUser = rtUser?.copy(userId = currentUid)
                                ?: UserModel(userId = currentUid, fullName = "User")

                            // Sync vào Firestore
                            db.collection("InfoUser")
                                .document(currentUid)
                                .set(finalUser)

                            buildGroup(users, finalUser, customName, onComplete)
                        }
                }
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }

    private fun buildGroup(
        users: List<UserModel>,
        currentUser: UserModel,
        customName: String?,
        onComplete: (String?) -> Unit
    ) {

        val currentUid = currentUser.userId

        // STEP 2: merge vào list
        val validUsers = (users + currentUser)
            .filter { it.userId.isNotBlank() }
            .distinctBy { it.userId }
            .sortedBy { it.fullName }

        if (validUsers.isEmpty()) {
            onComplete(null)
            return
        }

        // STEP 3: tạo tên nhóm
        val finalGroupName = if (!customName.isNullOrBlank()) {
            customName
        } else {
            validUsers.take(3).joinToString(", ") { it.fullName.ifBlank { "User" } } +
                    if (validUsers.size > 3) "..." else ""
        }

        val conversationId = db.collection("conversations").document().id
        val now = System.currentTimeMillis()

        val unreadMap = validUsers.associate { it.userId to 0L }

        val data = hashMapOf(
            "conversationId" to conversationId,
            "type" to "GROUP",
            "name" to finalGroupName,
            "participants" to validUsers.map { it.userId },
            "adminIds" to listOf(currentUid),
            "memberCount" to validUsers.size,
            "createdAt" to now,
            "updatedAt" to now,
            "lastMessagePreview" to "Nhóm được tạo",
            "lastMessageSenderId" to "system",
            "lastMessageAt" to now,
            "unreadCount" to unreadMap
        )

        // STEP 4: lưu vào Firestore
        db.collection("conversations")
            .document(conversationId)
            .set(data)
            .addOnSuccessListener {

                validUsers.forEach { user ->
                    val member = ConversationMemberModel(
                        conversationId = conversationId,
                        userId = user.userId,
                        role = if (user.userId == currentUid) "ADMIN" else "MEMBER"
                    ).ensureId()

                    db.collection("conversation_members")
                        .document(member.memberId)
                        .set(member)
                }

                pushSystemMessage(conversationId, "Nhóm '$finalGroupName' đã được tạo")
                onComplete(conversationId)
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }



    fun addMembersToExistingGroup(
        conversationId: String,
        newUserIds: List<String>,
        callback: () -> Unit
    ) {
        val validNewUserIds = newUserIds.filter { it.isNotBlank() }.distinct()

        if (validNewUserIds.isEmpty()) {
            Log.w("GroupRepository", "⚠️ No valid users to add")
            callback()
            return
        }

        Log.d("GroupRepository", "📥 Adding ${validNewUserIds.size} members to group $conversationId")

        // Thêm members vào conversation_members
        validNewUserIds.forEach { uid ->
            val member = ConversationMemberModel(
                conversationId = conversationId,
                userId = uid,
                role = "MEMBER"
            ).ensureId()

            db.collection("conversation_members")
                .document(member.memberId)
                .set(member)
                .addOnSuccessListener {
                    Log.d("GroupRepository", "✅ Added new member: $uid")
                }
        }

        // Cập nhật participants trong conversation
        db.collection("conversations")
            .document(conversationId)
            .get()
            .addOnSuccessListener { doc ->
                val currentParticipants = doc.get("participants") as? List<*>
                    ?: emptyList<String>()

                val updatedParticipants = (currentParticipants.mapNotNull { it as? String } + validNewUserIds)
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()

                val memberCount = updatedParticipants.size

                // Tạo unreadCount cho members mới
                val newUnreadMap = validNewUserIds.associateWith { FieldValue.increment(0) }

                val updateData = mutableMapOf<String, Any>(
                    "participants" to updatedParticipants,
                    "memberCount" to memberCount,
                    "lastMessagePreview" to "${validNewUserIds.size} thành viên mới được thêm",
                    "lastMessageSenderId" to "system",
                    "lastMessageAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis()
                )

                // Merge unreadCount cho members mới
                newUnreadMap.forEach { (uid, value) ->
                    updateData["unreadCount.$uid"] = value
                }

                db.collection("conversations")
                    .document(conversationId)
                    .update(updateData)
                    .addOnSuccessListener {
                        Log.d("GroupRepository", "✅ Updated group with new members")
                        pushSystemMessage(conversationId, "${validNewUserIds.size} thành viên mới đã tham gia nhóm")
                        callback()
                    }
                    .addOnFailureListener { e ->
                        Log.e("GroupRepository", "❌ Failed to update group", e)
                        pushSystemMessage(conversationId, "Có người mới tham gia nhóm")
                        callback()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("GroupRepository", "❌ Failed to get conversation", e)
                callback()
            }
    }

    private fun pushSystemMessage(conversationId: String, text: String) {
        val msgRef = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .document()

        val systemMessage = mapOf(
            "messageId" to msgRef.id,
            "type" to "system",
            "content" to text,
            "senderId" to "system",
            "createdAt" to System.currentTimeMillis(),
            "isDeleted" to false
        )

        msgRef.set(systemMessage)
            .addOnSuccessListener {
                Log.d("GroupRepository", "✅ System message sent: $text")
            }
            .addOnFailureListener { e ->
                Log.e("GroupRepository", "❌ Failed to send system message", e)
            }
    }
}