package com.example.socialmedia.project.ViewModel // (Hoặc package ViewModel của bạn)

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.lang.Exception

// File này CHỈ dùng cho SettingFragment để đếm số người bị chặn
class SettingMenuViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    // LiveData để giữ số lượng người bị chặn
    private val _blockCount = MutableLiveData<Long>(0L) // Bắt đầu bằng 0
    val blockCount: LiveData<Long> get() = _blockCount

    fun loadBlockCount() {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            _blockCount.postValue(0L)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val blockListRef = database.reference.child("block_list").child(currentUid)
                val snapshot = blockListRef.get().await()

                if (snapshot.exists()) {
                    _blockCount.postValue(snapshot.childrenCount)
                } else {
                    _blockCount.postValue(0L)
                }
            } catch (e: Exception) {
                Log.e("SettingMenuViewModel", "Lỗi tải số lượng chặn", e)
                _blockCount.postValue(0L)
            }
        }
    }
}