package com.autopulse.automation.telegram

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class TelegramBotClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) {

    /**
     * Sends a message to a Telegram chat with automatic retry on transient network failures.
     */
    suspend fun sendMessage(
        botToken: String,
        chatId: String,
        text: String,
        parseMode: String = "HTML",
        maxRetries: Int = 2
    ): TelegramResult<TelegramMessageResult> {
        val cleanToken = botToken.trim()
        val cleanChatId = chatId.trim()

        if (cleanToken.isBlank()) {
            return TelegramResult.Failure(
                errorCode = 401,
                description = "Telegram Bot Token is empty. Please configure it in Telegram Settings.",
                isRetryable = false
            )
        }
        if (cleanChatId.isBlank()) {
            return TelegramResult.Failure(
                errorCode = 400,
                description = "Telegram Chat ID is empty. Please configure it in Telegram Settings.",
                isRetryable = false
            )
        }

        val url = "https://api.telegram.org/bot$cleanToken/sendMessage"

        val formBody = FormBody.Builder()
            .add("chat_id", cleanChatId)
            .add("text", text)
            .add("parse_mode", parseMode)
            .add("disable_web_page_preview", "true")
            .build()

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        var attempt = 0
        var lastError: TelegramResult.Failure? = null

        while (attempt <= maxRetries) {
            attempt++
            try {
                return withContext(Dispatchers.IO) {
                    executeSendMessageRequest(request)
                }
            } catch (e: IOException) {
                val isRetryable = attempt <= maxRetries
                lastError = TelegramResult.Failure(
                    description = "Network failure: ${e.localizedMessage ?: "Connection timed out"}",
                    isRetryable = isRetryable
                )
                if (isRetryable) {
                    val backoffMs = (attempt * 1500L)
                    Log.w(TAG, "Transient network failure on attempt $attempt. Retrying in ${backoffMs}ms...")
                    delay(backoffMs)
                }
            } catch (e: Exception) {
                return TelegramResult.Failure(
                    description = "Unexpected error: ${e.localizedMessage ?: "Unknown"}",
                    isRetryable = false
                )
            }
        }

        return lastError ?: TelegramResult.Failure(
            description = "Message delivery failed after $maxRetries retries.",
            isRetryable = false
        )
    }

    /**
     * Queries Bot information to verify credentials.
     */
    suspend fun getMe(botToken: String): TelegramResult<TelegramBotInfo> {
        val cleanToken = botToken.trim()
        if (cleanToken.isBlank()) {
            return TelegramResult.Failure(description = "Token is empty", errorCode = 401)
        }

        val url = "https://api.telegram.org/bot$cleanToken/getMe"
        val request = Request.Builder().url(url).get().build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val result = json.getJSONObject("result")
                    TelegramResult.Success(
                        TelegramBotInfo(
                            id = result.getLong("id"),
                            username = result.optString("username", ""),
                            firstName = result.optString("first_name", ""),
                            canJoinGroups = result.optBoolean("can_join_groups", false)
                        )
                    )
                } else {
                    parseTelegramError(response.code, responseBody)
                }
            } catch (e: Exception) {
                TelegramResult.Failure(
                    description = "Connection failed: ${e.localizedMessage ?: "Network error"}",
                    isRetryable = true
                )
            }
        }
    }

    private fun executeSendMessageRequest(request: Request): TelegramResult<TelegramMessageResult> {
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()

        return if (response.isSuccessful) {
            val json = JSONObject(responseBody)
            val resultObj = json.getJSONObject("result")
            val messageId = resultObj.getLong("message_id")
            val date = resultObj.optLong("date", System.currentTimeMillis() / 1000)
            val chatObj = resultObj.optJSONObject("chat")
            val chatTitle = chatObj?.optString("title", chatObj.optString("username", ""))

            TelegramResult.Success(
                TelegramMessageResult(
                    messageId = messageId,
                    date = date,
                    chatTitle = chatTitle
                )
            )
        } else {
            parseTelegramError(response.code, responseBody)
        }
    }

    private fun parseTelegramError(httpCode: Int, responseBody: String): TelegramResult.Failure {
        val (errorCode, description, retryAfter) = try {
            val json = JSONObject(responseBody)
            val code = json.optInt("error_code", httpCode)
            val desc = json.optString("description", "HTTP $httpCode")
            val retry = json.optJSONObject("parameters")?.optInt("retry_after")
            Triple(code, desc, retry)
        } catch (_: Exception) {
            Triple(httpCode, "HTTP $httpCode: $responseBody", null)
        }

        val isRetryable = when (httpCode) {
            429 -> true // Rate limit
            in 500..599 -> true // Telegram server error
            else -> false // 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found
        }

        val userFriendlyDesc = when (httpCode) {
            400 -> "Bad Request: $description"
            401 -> "Invalid Bot Token. Check @BotFather token in Settings."
            403 -> "Forbidden: Bot was blocked or lacks permission in chat."
            404 -> "Chat ID not found. Send /start to the bot first."
            429 -> "Telegram Rate Limit exceeded. Retry after ${retryAfter ?: 30}s."
            else -> description
        }

        return TelegramResult.Failure(
            httpCode = httpCode,
            errorCode = errorCode,
            description = userFriendlyDesc,
            isRetryable = isRetryable,
            retryAfterSeconds = retryAfter
        )
    }

    companion object {
        private const val TAG = "TelegramBotClient"
    }
}
