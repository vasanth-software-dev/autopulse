package com.autopulse.automation.data.repository

import com.autopulse.automation.data.db.ExecutionLogDao
import com.autopulse.automation.data.model.ExecutionLog
import com.autopulse.automation.data.model.LogLevel
import kotlinx.coroutines.flow.Flow

class ExecutionLogRepository(private val dao: ExecutionLogDao) {

    val recentLogs: Flow<List<ExecutionLog>> = dao.getRecentLogs(300)
    val lastLog: Flow<ExecutionLog?> = dao.getLastLog()

    fun getLogsForAutomation(automationId: Long): Flow<List<ExecutionLog>> =
        dao.getLogsForAutomation(automationId)

    fun getLogsForTask(taskId: String): Flow<List<ExecutionLog>> =
        dao.getLogsForTask(taskId)

    suspend fun log(
        message: String,
        level: LogLevel = LogLevel.INFO,
        automationId: Long? = null,
        automationName: String? = null,
        taskId: String? = null,
        detailsJson: String? = null
    ): Long {
        // Redact any bot token patterns (e.g. 123456789:ABCdef...) to avoid sensitive leaks in logs
        val sanitizedMessage = redactSecrets(message)
        val sanitizedDetails = detailsJson?.let { redactSecrets(it) }

        val log = ExecutionLog(
            automationId = automationId,
            automationName = automationName,
            taskId = taskId,
            timestamp = System.currentTimeMillis(),
            level = level,
            message = sanitizedMessage,
            detailsJson = sanitizedDetails
        )
        val id = dao.insertLog(log)
        dao.trimLogs(1000)
        return id
    }

    suspend fun logInfo(
        message: String,
        automationId: Long? = null,
        automationName: String? = null,
        taskId: String? = null,
        detailsJson: String? = null
    ) {
        log(message, LogLevel.INFO, automationId, automationName, taskId, detailsJson)
    }

    suspend fun logSuccess(
        message: String,
        automationId: Long? = null,
        automationName: String? = null,
        taskId: String? = null,
        detailsJson: String? = null
    ) {
        log(message, LogLevel.SUCCESS, automationId, automationName, taskId, detailsJson)
    }

    suspend fun logWarn(
        message: String,
        automationId: Long? = null,
        automationName: String? = null,
        taskId: String? = null,
        detailsJson: String? = null
    ) {
        log(message, LogLevel.WARN, automationId, automationName, taskId, detailsJson)
    }

    suspend fun logError(
        message: String,
        automationId: Long? = null,
        automationName: String? = null,
        taskId: String? = null,
        details: String? = null,
        detailsJson: String? = details
    ) {
        val finalDetails = detailsJson ?: details
        log(message, LogLevel.ERROR, automationId, automationName, taskId, finalDetails)
    }

    suspend fun clearLogs() {
        dao.clearAllLogs()
    }

    private fun redactSecrets(input: String): String {
        // Telegram token regex: 8-10 digits followed by colon and 35 alphanumeric chars
        val tokenRegex = Regex("""\b\d{8,12}:[A-Za-z0-9_-]{30,50}\b""")
        return input.replace(tokenRegex, "BOT_TOKEN_REDACTED")
    }
}
