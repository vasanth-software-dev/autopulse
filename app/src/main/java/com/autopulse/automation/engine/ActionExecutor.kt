package com.autopulse.automation.engine

import android.content.Context
import android.util.Log
import com.autopulse.automation.data.model.Action
import com.autopulse.automation.data.model.ActionType
import com.autopulse.automation.data.model.Automation
import com.autopulse.automation.data.model.RepeatingTask
import com.autopulse.automation.data.model.TaskStatus
import com.autopulse.automation.data.repository.ExecutionLogRepository
import com.autopulse.automation.data.repository.RepeatingTaskRepository
import com.autopulse.automation.data.repository.TelegramSettingsRepository
import com.autopulse.automation.error.AutoPulseError
import com.autopulse.automation.error.NetworkMonitor
import com.autopulse.automation.event.NotificationEvent
import com.autopulse.automation.telegram.TelegramBotClient
import com.autopulse.automation.telegram.TelegramMessageFormatter
import com.autopulse.automation.telegram.TelegramResult
import org.json.JSONObject
import java.util.UUID

class ActionExecutor(
    private val context: Context,
    private val taskRepository: RepeatingTaskRepository,
    private val logRepository: ExecutionLogRepository,
    private val telegramSettingsRepository: TelegramSettingsRepository,
    private val telegramBotClient: TelegramBotClient = TelegramBotClient(),
    private val startServiceCallback: ((taskId: String) -> Unit)? = null
) {

    /**
     * Executes the ordered list of actions configured for an automation.
     */
    suspend fun executeActions(
        automation: Automation,
        actions: List<Action>,
        event: NotificationEvent
    ) {
        val sortedActions = actions.sortedBy { it.orderIndex }
        var currentTaskId: String? = null

        for (action in sortedActions) {
            when (action.type) {
                ActionType.START_REPEAT -> {
                    currentTaskId = handleStartRepeat(automation, action, event)
                }
                ActionType.SEND_TELEGRAM -> {
                    handleSendTelegram(automation, action, event, currentTaskId)
                }
                ActionType.STOP_REPEAT -> {
                    handleStopRepeat(automation)
                }
                ActionType.LOG_MESSAGE -> {
                    handleLogMessage(automation, action, event)
                }
            }
        }
    }

    private suspend fun handleStartRepeat(
        automation: Automation,
        action: Action,
        event: NotificationEvent
    ): String {
        val payload = try {
            JSONObject(action.payloadJson)
        } catch (_: Exception) {
            JSONObject()
        }

        val intervalSeconds = payload.optLong("intervalSeconds", 30L)
        val template = payload.optString("messageTemplate", "").ifBlank { null }
        val taskId = UUID.randomUUID().toString()

        val task = RepeatingTask(
            id = taskId,
            automationId = automation.id,
            automationName = automation.name,
            status = TaskStatus.RUNNING,
            intervalSeconds = intervalSeconds,
            startTimestamp = System.currentTimeMillis(),
            lastExecutionTimestamp = 0L,
            nextExecutionTimestamp = System.currentTimeMillis(),
            executionCount = 0,
            leadTitle = event.title,
            leadText = if (event.bigText.isNotBlank()) event.bigText else event.text,
            leadPackage = event.packageName,
            leadAppName = event.appName,
            customMessageTemplate = template
        )

        taskRepository.insertOrUpdateTask(task)

        logRepository.logInfo(
            message = "Repeating task started (Interval: ${intervalSeconds}s).",
            automationId = automation.id,
            automationName = automation.name,
            taskId = taskId
        )

        // Signal execution service to start foreground repeating execution
        startServiceCallback?.invoke(taskId)

        return taskId
    }

    suspend fun handleSendTelegram(
        automation: Automation,
        action: Action,
        event: NotificationEvent,
        taskId: String?
    ): Boolean {
        // 1. Check network connectivity before making request
        if (!NetworkMonitor.isNetworkAvailable(context)) {
            val err = AutoPulseError.NoInternet
            logRepository.logWarn(
                message = "Telegram dispatch deferred: ${err.message} (${err.recoverySuggestion})",
                automationId = automation.id,
                automationName = automation.name,
                taskId = taskId
            )
            return false
        }

        // 2. Check credentials
        val token = telegramSettingsRepository.getBotToken()
        val chatId = telegramSettingsRepository.getChatId()

        if (token.isBlank() || chatId.isBlank()) {
            val errorMsg = "Cannot send Telegram notification: Bot Token or Chat ID not configured in Settings."
            logRepository.logError(
                message = errorMsg,
                automationId = automation.id,
                automationName = automation.name,
                taskId = taskId
            )
            return false
        }

        // 3. Format message safely
        val customTemplate = try {
            val json = JSONObject(action.payloadJson)
            json.optString("messageTemplate", "").ifBlank { null }
        } catch (_: Exception) {
            null
        }

        val htmlFormatted = TelegramMessageFormatter.formatHtmlMessage(customTemplate, event)

        // 4. Attach inline STOP ALERT button if linked to an active repeating task
        val replyMarkup = if (!taskId.isNullOrBlank()) {
            """{"inline_keyboard":[[{"text":"🛑 STOP ALERT","callback_data":"stop:$taskId"}]]}"""
        } else null

        // 5. Send with retry
        return when (val result = telegramBotClient.sendMessage(
            botToken = token,
            chatId = chatId,
            text = htmlFormatted,
            parseMode = "HTML",
            replyMarkupJson = replyMarkup
        )) {
            is TelegramResult.Success -> {
                logRepository.logSuccess(
                    message = "Telegram message sent for '${automation.name}'. (ID: ${result.data.messageId})",
                    automationId = automation.id,
                    automationName = automation.name,
                    taskId = taskId
                )
                true
            }
            is TelegramResult.Failure -> {
                logRepository.logError(
                    message = "Telegram delivery failed: ${result.description}",
                    automationId = automation.id,
                    automationName = automation.name,
                    taskId = taskId,
                    details = "HTTP ${result.httpCode ?: 0} (Code: ${result.errorCode ?: 0})"
                )
                false
            }
        }
    }

    private suspend fun handleStopRepeat(automation: Automation) {
        val existingTask = taskRepository.getRunningTaskForAutomation(automation.id)
        if (existingTask != null) {
            taskRepository.stopTask(existingTask.id)
            logRepository.logInfo(
                message = "Repeating task stopped by STOP action.",
                automationId = automation.id,
                automationName = automation.name,
                taskId = existingTask.id
            )
        }
    }

    private suspend fun handleLogMessage(
        automation: Automation,
        action: Action,
        event: NotificationEvent
    ) {
        val rawMessage = try {
            JSONObject(action.payloadJson).optString("message", "Rule triggered")
        } catch (_: Exception) {
            "Rule triggered"
        }
        val message = event.interpolate(rawMessage)
        logRepository.logInfo(
            message = message,
            automationId = automation.id,
            automationName = automation.name
        )
    }
}
