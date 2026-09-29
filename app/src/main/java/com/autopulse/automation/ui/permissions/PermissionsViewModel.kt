package com.autopulse.automation.ui.permissions

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autopulse.automation.AutoPulseApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PermissionState(
    val isNotificationListenerGranted: Boolean = false,
    val isPostNotificationsGranted: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val isExactAlarmAllowed: Boolean = false
)

class PermissionsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AutoPulseApplication
    private val logRepo = app.executionLogRepository
    private val telegramRepo = app.telegramSettingsRepository

    private val _permissions = MutableStateFlow(checkPermissions())
    val permissions: StateFlow<PermissionState> = _permissions.asStateFlow()

    private val _diagnosticMessage = MutableStateFlow<String?>(null)
    val diagnosticMessage: StateFlow<String?> = _diagnosticMessage.asStateFlow()

    fun refresh() {
        _permissions.value = checkPermissions()
    }

    fun runDiagnosticAudit() {
        val state = checkPermissions()
        _permissions.value = state

        viewModelScope.launch {
            val isTelegramReady = telegramRepo.isConfigured()

            val allPassed = state.isNotificationListenerGranted &&
                    state.isPostNotificationsGranted &&
                    state.isBatteryOptimizationIgnored &&
                    isTelegramReady

            val auditSummary = buildString {
                append("Health Audit: ")
                if (allPassed) {
                    append("All systems operational. AutoPulse is ready to detect and repeat alerts.")
                } else {
                    val missing = mutableListOf<String>()
                    if (!state.isNotificationListenerGranted) missing.add("Notification Access")
                    if (!state.isPostNotificationsGranted) missing.add("Post Notifications")
                    if (!state.isBatteryOptimizationIgnored) missing.add("Battery Exemption")
                    if (!isTelegramReady) missing.add("Telegram Credentials")
                    append("Action required for: ${missing.joinToString(", ")}.")
                }
            }

            _diagnosticMessage.value = auditSummary

            if (allPassed) {
                logRepo.logSuccess("System Health Audit PASSED: All background services and permissions verified.")
            } else {
                logRepo.logWarn("System Health Audit: Some permissions or credentials require configuration.")
            }
        }
    }

    private fun checkPermissions(): PermissionState {
        val context = getApplication<Application>()

        // 1. Notification Listener Permission
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val listenerGranted = flat != null && flat.contains(context.packageName)

        // 2. Post Notifications (Android 13+)
        val postNotificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true
        }

        // 3. Battery Optimizations
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryIgnored = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

        // 4. Exact Alarms (Android 12+)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val exactAlarmAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }

        return PermissionState(
            isNotificationListenerGranted = listenerGranted,
            isPostNotificationsGranted = postNotificationsGranted,
            isBatteryOptimizationIgnored = batteryIgnored,
            isExactAlarmAllowed = exactAlarmAllowed
        )
    }

    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    }

    fun openAppNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
