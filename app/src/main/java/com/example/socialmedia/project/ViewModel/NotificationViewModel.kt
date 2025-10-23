package com.example.socialmedia.project.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmedia.project.Domain.Model.NotificationModel
import com.example.socialmedia.project.data.repository.NotificationRepository

class NotificationViewModel : ViewModel() {

    private val repository = NotificationRepository()
    private val _notifications = MutableLiveData<List<NotificationModel>>()
    val notifications: LiveData<List<NotificationModel>> = _notifications

    fun loadNotifications() {
        repository.observeNotifications { list ->
            _notifications.postValue(list)
        }
    }
}
