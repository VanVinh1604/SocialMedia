package com.example.socialmedia.project.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmedia.project.Domain.Model.NotificationModel
import com.example.socialmedia.project.data.repository.NotificationRepository

class NotificationViewModel : ViewModel() {

    private val repo = NotificationRepository()
    private val _notifications = MutableLiveData<List<NotificationModel>>()
    val notifications: LiveData<List<NotificationModel>> get() = _notifications

    private var latestTimestamp: Long? = null

    fun loadNotifications() {
        repo.loadNotificationsPaged(
            limit = 20,
            onResult = { list ->
                _notifications.value = list
                latestTimestamp = list.firstOrNull()?.createdAt
                // Sau khi có batch đầu → bắt đầu listen realtime mới hơn
                latestTimestamp?.let { observeRealtime(it) }
            },
            onError = { _notifications.value = emptyList() }
        )
    }

    private fun observeRealtime(after: Long) {
        repo.observeNewNotifications(after) { newList ->
            val current = _notifications.value?.toMutableList() ?: mutableListOf()
            // Thêm vào đầu danh sách, tránh trùng lặp
            val merged = (newList + current).distinctBy { it.notificationId }
            _notifications.postValue(merged)
        }
    }

    fun loadMore() {
        repo.loadMoreNotifications(
            limit = 20,
            onResult = { more ->
                val current = _notifications.value?.toMutableList() ?: mutableListOf()
                current.addAll(more)
                _notifications.value = current
            },
            onError = { }
        )
    }
}
