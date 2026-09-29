package com.autopulse.automation.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TelegramSettingsRepository(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (t: Throwable) {
        Log.w(TAG, "EncryptedSharedPreferences unavailable (${t.message}), using standard storage.", t)
        context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun safeGetString(key: String): String {
        return try {
            prefs.getString(key, "").orEmpty()
        } catch (t: Throwable) {
            Log.e(TAG, "Error reading setting '$key', clearing cached value.", t)
            ""
        }
    }

    private val _botToken = MutableStateFlow(safeGetString(KEY_BOT_TOKEN))
    val botToken: StateFlow<String> = _botToken.asStateFlow()

    private val _chatId = MutableStateFlow(safeGetString(KEY_CHAT_ID))
    val chatId: StateFlow<String> = _chatId.asStateFlow()

    fun getBotToken(): String = _botToken.value

    fun getChatId(): String = _chatId.value

    fun isConfigured(): Boolean {
        return _botToken.value.isNotBlank() && _chatId.value.isNotBlank()
    }

    fun saveSettings(token: String, chat: String) {
        val cleanToken = token.trim()
        val cleanChatId = chat.trim()

        try {
            prefs.edit()
                .putString(KEY_BOT_TOKEN, cleanToken)
                .putString(KEY_CHAT_ID, cleanChatId)
                .apply()
        } catch (t: Throwable) {
            Log.e(TAG, "Error saving settings to preferences", t)
        }

        _botToken.value = cleanToken
        _chatId.value = cleanChatId
    }

    fun clearSettings() {
        try {
            prefs.edit().clear().apply()
        } catch (t: Throwable) {
            Log.e(TAG, "Error clearing settings", t)
        }
        _botToken.value = ""
        _chatId.value = ""
    }

    companion object {
        private const val TAG = "TelegramSettingsRepo"
        private const val PREFS_NAME = "encrypted_telegram_prefs"
        private const val FALLBACK_PREFS_NAME = "autopulse_telegram_settings"
        private const val KEY_BOT_TOKEN = "telegram_bot_token"
        private const val KEY_CHAT_ID = "telegram_chat_id"
    }
}
