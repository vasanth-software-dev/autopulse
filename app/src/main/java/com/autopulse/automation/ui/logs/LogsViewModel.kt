package com.autopulse.automation.ui.logs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autopulse.automation.AutoPulseApplication
import com.autopulse.automation.data.model.ExecutionLog
import com.autopulse.automation.data.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AutoPulseApplication
    private val logRepo = app.executionLogRepository

    private val _selectedLevel = MutableStateFlow<LogLevel?>(null)
    val selectedLevel: StateFlow<LogLevel?> = _selectedLevel.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredLogs: StateFlow<List<ExecutionLog>> = combine(
        logRepo.recentLogs,
        _selectedLevel,
        _searchQuery
    ) { logs, level, query ->
        logs.filter { log ->
            val levelMatches = (level == null || log.level == level)
            val queryMatches = if (query.isBlank()) true else {
                log.message.contains(query, ignoreCase = true) ||
                        log.automationName?.contains(query, ignoreCase = true) == true ||
                        log.taskId?.contains(query, ignoreCase = true) == true
            }
            levelMatches && queryMatches
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun setLevelFilter(level: LogLevel?) {
        _selectedLevel.value = level
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearLogs() {
        viewModelScope.launch {
            logRepo.clearLogs()
            logRepo.logInfo("Execution logs cleared by user.")
        }
    }

    fun getExportableLogText(): String {
        val logs = filteredLogs.value
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

        return buildString {
            appendLine("=== AutoPulse Execution Logs Export (${logs.size} entries) ===")
            for (log in logs.reversed()) {
                val time = timeFormat.format(Date(log.timestamp))
                val rule = if (log.automationName != null) " [${log.automationName}]" else ""
                val task = if (log.taskId != null) " (Task: ${log.taskId.take(8)})" else ""
                appendLine("[$time] [${log.level.name}]$rule$task: ${log.message}")
                if (log.detailsJson != null) {
                    appendLine("    Details: ${log.detailsJson}")
                }
            }
        }
    }
}
