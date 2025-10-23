package com.example.socialmedia.project.data.repository

import com.example.socialmedia.project.Domain.Model.NotificationModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NotificationRepository {

    private val database = FirebaseDatabase.getInstance().getReference("notifications")
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    fun observeNotifications(callback: (List<NotificationModel>) -> Unit) {
        if (userId == null) {
            callback(emptyList())
            return
        }

        // Lắng nghe thay đổi realtime
        database.orderByChild("userId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<NotificationModel>()
                    for (child in snapshot.children) {
                        child.getValue(NotificationModel::class.java)?.let {
                            list.add(it)
                        }
                    }
                    // Sắp xếp mới nhất lên đầu
                    callback(list.sortedByDescending { it.createdAt })
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
    }
}
