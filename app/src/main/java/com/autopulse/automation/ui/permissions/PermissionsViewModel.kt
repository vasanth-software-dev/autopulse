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
        return try {
            val context = getApplication<Application>()

            // 1. Notification Listener Permission (defensive against SecurityException)
            val listenerGranted = try {
                val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
                if (enabledListeners.contains(context.packageName)) {
                    true
                } else {
                    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                    flat != null && flat.contains(context.packageName)
                }
            } catch (_: Throwable) {
                false
            }

            // 2. Post Notifications (Android 13+)
            val postNotificationsGranted = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    NotificationManagerCompat.from(context).areNotificationsEnabled()
                } else {
                    true
                }
            } catch (_: Throwable) {
                true
            }

            // 3. Battery Optimizations
            val batteryIgnored = try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
            } catch (_: Throwable) {
                false
            }

            // 4. Exact Alarms (Android 12+)
            val exactAlarmAllowed = try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager?.canScheduleExactAlarms() == true
                } else {
                    true
                }
            } catch (_: Throwable) {
                false
            }

            PermissionState(
                isNotificationListenerGranted = listenerGranted,
                isPostNotificationsGranted = postNotificationsGranted,
                isBatteryOptimizationIgnored = batteryIgnored,
                isExactAlarmAllowed = exactAlarmAllowed
            )
        } catch (_: Throwable) {
            PermissionState()
        }
    }

    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (t: Throwable) {
            android.util.Log.e("PermissionsVM", "Cannot open notification listener settings", t)
        }
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Throwable) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (t: Throwable) {
                android.util.Log.e("PermissionsVM", "Cannot open battery settings", t)
            }
        }
    }

    fun openAppNotificationSettings(context: Context) {
        try {
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
        } catch (t: Throwable) {
            android.util.Log.e("PermissionsVM", "Cannot open app notification settings", t)
        }
    }

    fun openExactAlarmSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (t: Throwable) {
            android.util.Log.e("PermissionsVM", "Cannot open exact alarm settings", t)
        }
    }
}
