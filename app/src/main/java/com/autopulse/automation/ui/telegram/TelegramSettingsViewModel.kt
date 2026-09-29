package com.autopulse.automation.ui.telegram

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autopulse.automation.AutoPulseApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class TelegramTestState {
    object Idle : TelegramTestState()
    object Loading : TelegramTestState()
    data class Success(val message: String) : TelegramTestState()
    data class Error(val error: String) : TelegramTestState()
}

class TelegramSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AutoPulseApplication
    private val telegramRepo = app.telegramSettingsRepository
    private val logRepo = app.executionLogRepository

    val botToken: StateFlow<String> = telegramRepo.botToken
    val chatId: StateFlow<String> = telegramRepo.chatId

    private val _testState = MutableStateFlow<TelegramTestState>(TelegramTestState.Idle)
    val testState: StateFlow<TelegramTestState> = _testState.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun save(token: String, chat: String) {
        telegramRepo.saveSettings(token, chat)
        viewModelScope.launch {
            logRepo.logInfo("Telegram credentials updated securely.")
        }
    }

    fun testConnection(token: String) {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            _testState.value = TelegramTestState.Error("Please enter a Telegram Bot Token.")
            return
        }

        _testState.value = TelegramTestState.Loading
        viewModelScope.launch {
            try {
                val url = "https://api.telegram.org/bot$cleanToken/getMe"
                val request = Request.Builder().url(url).build()

                val (success, message) = withContext(Dispatchers.IO) {
                    val response = httpClient.newCall(request).execute()
                    val body = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        val json = JSONObject(body)
                        val botUser = json.optJSONObject("result")?.optString("username", "Unknown")
                        true to "Connected! Bot: @$botUser"
                    } else {
                        false to "Telegram API Error (HTTP ${response.code}): Invalid Bot Token."
                    }
                }

                if (success) {
                    _testState.value = TelegramTestState.Success(message)
                    logRepo.logSuccess("Telegram Bot connection verified.")
                } else {
                    _testState.value = TelegramTestState.Error(message)
                    logRepo.logError(message)
                }
            } catch (e: Exception) {
                val err = "Network error: ${e.localizedMessage ?: "Unable to connect"}"
                _testState.value = TelegramTestState.Error(err)
                logRepo.logError(err)
            }
        }
    }

    fun sendTestMessage(token: String, chat: String) {
        val cleanToken = token.trim()
        val cleanChatId = chat.trim()

        if (cleanToken.isBlank() || cleanChatId.isBlank()) {
            _testState.value = TelegramTestState.Error("Both Bot Token and Chat ID are required.")
            return
        }

        _testState.value = TelegramTestState.Loading
        viewModelScope.launch {
            try {
                val testText = "⚡ *AutoPulse System Test*\n\nTelegram integration is functioning properly!\nTimestamp: " +
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

                val formBody = okhttp3.FormBody.Builder()
                    .add("chat_id", cleanChatId)
                    .add("text", testText)
                    .add("parse_mode", "Markdown")
                    .build()

                val url = "https://api.telegram.org/bot$cleanToken/sendMessage"
                val request = Request.Builder().url(url).post(formBody).build()

                val (success, msg) = withContext(Dispatchers.IO) {
                    val response = httpClient.newCall(request).execute()
                    val body = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        true to "Test message delivered to chat $cleanChatId!"
                    } else {
                        val json = try { JSONObject(body) } catch (_: Exception) { null }
                        val desc = json?.optString("description", "HTTP ${response.code}") ?: "HTTP ${response.code}"
                        false to "Telegram Error: $desc"
                    }
                }

                if (success) {
                    _testState.value = TelegramTestState.Success(msg)
                    logRepo.logSuccess(msg)
                } else {
                    _testState.value = TelegramTestState.Error(msg)
                    logRepo.logError(msg)
                }
            } catch (e: Exception) {
                val err = "Delivery failed: ${e.localizedMessage ?: "Network error"}"
                _testState.value = TelegramTestState.Error(err)
                logRepo.logError(err)
            }
        }
    }

    fun resetState() {
        _testState.value = TelegramTestState.Idle
    }
}
