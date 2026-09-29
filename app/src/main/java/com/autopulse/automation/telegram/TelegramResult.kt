package com.autopulse.automation.telegram

sealed class TelegramResult<out T> {
    data class Success<out T>(val data: T) : TelegramResult<T>()
    data class Failure(
        val httpCode: Int? = null,
        val errorCode: Int? = null,
        val description: String,
        val isRetryable: Boolean = false,
        val retryAfterSeconds: Int? = null
    ) : TelegramResult<Nothing>()
}

data class TelegramMessageResult(
    val messageId: Long,
    val date: Long,
    val chatTitle: String? = null
)

data class TelegramBotInfo(
    val id: Long,
    val username: String,
    val firstName: String,
    val canJoinGroups: Boolean = false
)

data class TelegramUpdate(
    val updateId: Long,
    val messageText: String? = null,
    val callbackQueryId: String? = null,
    val callbackData: String? = null,
    val chatId: String? = null
)

