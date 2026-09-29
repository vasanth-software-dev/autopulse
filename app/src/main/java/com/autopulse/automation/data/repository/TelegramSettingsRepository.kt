package com.autopulse.automation.data.repository

import android.content.Context
import android.content.SharedPreferences
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
    } catch (e: Exception) {
        // Fallback for emulator/tests where MasterKey might fail
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _botToken = MutableStateFlow(prefs.getString(KEY_BOT_TOKEN, "").orEmpty())
    val botToken: StateFlow<String> = _botToken.asStateFlow()

    private val _chatId = MutableStateFlow(prefs.getString(KEY_CHAT_ID, "").orEmpty())
    val chatId: StateFlow<String> = _chatId.asStateFlow()

    fun getBotToken(): String = _botToken.value

    fun getChatId(): String = _chatId.value

    fun isConfigured(): Boolean {
        return _botToken.value.isNotBlank() && _chatId.value.isNotBlank()
    }

    fun saveSettings(token: String, chat: String) {
        val cleanToken = token.trim()
        val cleanChatId = chat.trim()

        prefs.edit()
            .putString(KEY_BOT_TOKEN, cleanToken)
            .putString(KEY_CHAT_ID, cleanChatId)
            .apply()

        _botToken.value = cleanToken
        _chatId.value = cleanChatId
    }

    fun clearSettings() {
        prefs.edit().clear().apply()
        _botToken.value = ""
        _chatId.value = ""
    }

    companion object {
        private const val PREFS_NAME = "encrypted_telegram_prefs"
        private const val KEY_BOT_TOKEN = "telegram_bot_token"
        private const val KEY_CHAT_ID = "telegram_chat_id"
    }
}
