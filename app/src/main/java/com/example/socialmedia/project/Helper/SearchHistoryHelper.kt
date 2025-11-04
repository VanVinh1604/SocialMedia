package com.example.socialmedia.project.Helper

import android.content.Context
import android.content.SharedPreferences
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.Domain.Model.SimpleUserModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SearchHistoryHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val maxHistorySize = 10

    companion object {
        private const val KEY_HISTORY = "history_list"
    }

    fun addSearchHistory(user: UserModel) {
        val currentHistory = getSearchHistorySimple().toMutableList()

        // Convert sang SimpleUserModel
        val simpleUser = SimpleUserModel.fromUserModel(user)

        // Xóa user nếu đã tồn tại
        currentHistory.removeAll { it.userId == simpleUser.userId }

        // Thêm vào đầu list
        currentHistory.add(0, simpleUser)

        // Giới hạn số lượng
        if (currentHistory.size > maxHistorySize) {
            currentHistory.removeAt(currentHistory.size - 1)
        }

        saveHistory(currentHistory)
    }

    fun getSearchHistory(): List<UserModel> {
        return getSearchHistorySimple().map { it.toUserModel() }
    }

    private fun getSearchHistorySimple(): List<SimpleUserModel> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val type = object : TypeToken<List<SimpleUserModel>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("SearchHistoryHelper", "Error parsing history", e)
            emptyList()
        }
    }

    fun removeSearchHistory(userId: String) {
        val currentHistory = getSearchHistorySimple().toMutableList()
        currentHistory.removeAll { it.userId == userId }
        saveHistory(currentHistory)
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun saveHistory(history: List<SimpleUserModel>) {
        val json = gson.toJson(history)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }
}