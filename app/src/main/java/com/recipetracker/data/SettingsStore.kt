package com.recipetracker.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _userName = MutableStateFlow(prefs.getString(KEY_NAME, "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    fun setUserName(name: String) {
        val trimmed = name.trim()
        prefs.edit().putString(KEY_NAME, trimmed).apply()
        _userName.value = trimmed
    }

    private companion object {
        const val KEY_NAME = "user_name"
    }
}
