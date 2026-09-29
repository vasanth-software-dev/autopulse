package com.autopulse.automation.ui.dashboard

import android.app.Application
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autopulse.automation.AutoPulseApplication
import com.autopulse.automation.data.model.ExecutionLog
import com.autopulse.automation.util.MockNotificationDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val totalAutomations: Int = 0,
    val enabledAutomations: Int = 0,
    val runningTasks: Int = 0,
    val lastExecutionLog: ExecutionLog? = null,
    val isNotificationListenerEnabled: Boolean = false,
    val isTelegramConfigured: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AutoPulseApplication
    private val automationRepo = app.automationRepository
    private val taskRepo = app.repeatingTaskRepository
    private val logRepo = app.executionLogRepository
    private val telegramRepo = app.telegramSettingsRepository

    private val _systemStatus = MutableStateFlow(checkSystemStatus())
    val systemStatus: StateFlow<Triple<Boolean, Boolean, Boolean>> = _systemStatus.asStateFlow()

    val uiState: StateFlow<DashboardUiState> = combine(
        automationRepo.totalCount,
        automationRepo.enabledCount,
        taskRepo.runningCount,
        logRepo.lastLog,
        _systemStatus
    ) { total, enabled, running, lastLog, (listenerEnabled, telegramReady, batteryIgnored) ->
        DashboardUiState(
            totalAutomations = total,
            enabledAutomations = enabled,
            runningTasks = running,
            lastExecutionLog = lastLog,
            isNotificationListenerEnabled = listenerEnabled,
            isTelegramConfigured = telegramReady,
            isBatteryOptimizationIgnored = batteryIgnored
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardUiState()
    )

    fun refreshSystemStatus() {
        _systemStatus.value = checkSystemStatus()
    }

    fun simulateOlxLead() {
        viewModelScope.launch {
            MockNotificationDispatcher.dispatchMockOlxLead(logRepo)
        }
    }

    private fun checkSystemStatus(): Triple<Boolean, Boolean, Boolean> {
        val context = getApplication<Application>()

        // 1. Check Notification Listener Permission
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val listenerEnabled = flat != null && flat.contains(context.packageName)

        // 2. Check Telegram Bot Credentials
        val telegramReady = telegramRepo.isConfigured()

        // 3. Check Battery Optimization Exemption
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryIgnored = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

        return Triple(listenerEnabled, telegramReady, batteryIgnored)
    }
}
