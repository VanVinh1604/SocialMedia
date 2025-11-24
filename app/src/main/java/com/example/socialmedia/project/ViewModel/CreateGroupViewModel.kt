package com.example.socialmedia.project.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.Repository.GroupRepository
import com.example.socialmedia.project.Repository.UserRepository
import com.example.socialmedia.project.Server.Firebase.FirebaseService

class CreateGroupViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val repository = UserRepository()

    private val groupRepo = GroupRepository()


    private val _friends = MutableLiveData<List<UserModel>>()
    val friends: LiveData<List<UserModel>> get() = _friends

    fun loadFriends() {
        val currentUserId = firebaseService.getCurrentUserId() ?: return
        repository.getFriends(currentUserId) { users ->
            // Loại bỏ chính user nếu có
            val filtered = users.filter { it.userId != currentUserId }
            _friends.postValue(filtered)
        }
    }

    //    fun createGroup(users: List<UserModel>, callback: (String?) -> Unit) {
//        val ids = users.map { it.userId }.toMutableList()
//        ids.add(firebaseService.getCurrentUserId()!!)
//
//        val groupName = users.take(3).joinToString(", ") { it.fullName } +
//                if (users.size > 3) "..." else ""
//
//        groupRepo.createGroup(ids, groupName) { result ->
//            callback(result)  // luôn gọi callback, kể cả null
//        }
//    }
    fun createGroup(users: List<UserModel>, customName: String?, callback: (String?) -> Unit) {
        groupRepo.createGroup(users, customName, callback)
    }


}
