package com.example.socialmedia.project.Server.Firebase

import com.example.socialmedia.project.Domain.Model.StoryModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.google.firebase.database.*

class FirebaseService {

    private val database = FirebaseDatabase.getInstance().reference

    // Lắng nghe stories realtime
    fun listenStories(onResult: (List<StoryModel>) -> Unit, onError: (Exception) -> Unit) {
        val storiesRef = database.child("stories")
        storiesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<StoryModel>()
                for (child in snapshot.children) {
                    val story = child.getValue(StoryModel::class.java)
                    story?.let { list.add(it) }
                }
                onResult(list)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.toException())
            }
        })
    }

    // Lắng nghe users realtime
    fun listenUsers(onResult: (List<UserModel>) -> Unit, onError: (Exception) -> Unit) {
        val usersRef = database.child("InfoUser")
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<UserModel>()
                for (child in snapshot.children) {
                    val user = child.getValue(UserModel::class.java)
                    user?.let { list.add(it) }
                }
                onResult(list)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.toException())
            }
        })
    }

    fun getUserFollowing(
        currentUserId: String,
        onResult: (List<String>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val ref = database.child("Follow").child(currentUserId).child("following")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.key }
                onResult(list)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.toException())
            }
        })
    }
}
