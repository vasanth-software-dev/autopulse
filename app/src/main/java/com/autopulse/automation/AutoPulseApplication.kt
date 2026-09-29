package com.autopulse.automation

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.autopulse.automation.data.db.AutoPulseDatabase
import com.autopulse.automation.data.repository.AutomationRepository
import com.autopulse.automation.data.repository.ExecutionLogRepository
import com.autopulse.automation.data.repository.RepeatingTaskRepository
import com.autopulse.automation.data.repository.TelegramSettingsRepository
import com.autopulse.automation.engine.ActionExecutor
import com.autopulse.automation.engine.AutomationEngine
import com.autopulse.automation.error.AutoPulseCrashHandler
import com.autopulse.automation.error.NetworkMonitor
import com.autopulse.automation.service.AutomationExecutionService
import com.autopulse.automation.telegram.TelegramBotClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AutoPulseApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AutoPulseDatabase.getDatabase(this, applicationScope) }

    val automationRepository by lazy { AutomationRepository(database.automationDao()) }
    val repeatingTaskRepository by lazy { RepeatingTaskRepository(database.repeatingTaskDao()) }
    val executionLogRepository by lazy { ExecutionLogRepository(database.executionLogDao()) }
    val telegramSettingsRepository by lazy { TelegramSettingsRepository(this) }
    val telegramBotClient by lazy { TelegramBotClient() }
    val networkMonitor by lazy { NetworkMonitor(this) }

    val actionExecutor by lazy {
        ActionExecutor(
            context = this,
            taskRepository = repeatingTaskRepository,
            logRepository = executionLogRepository,
            telegramSettingsRepository = telegramSettingsRepository,
            telegramBotClient = telegramBotClient,
            startServiceCallback = { taskId ->
                AutomationExecutionService.startTask(this, taskId)
            }
        )
    }

    val automationEngine by lazy {
        AutomationEngine(
            context = this,
            automationRepository = automationRepository,
            taskRepository = repeatingTaskRepository,
            logRepository = executionLogRepository,
            actionExecutor = actionExecutor,
            engineScope = applicationScope
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Install crash handler to record fatal issues into database logs
        AutoPulseCrashHandler.install(executionLogRepository)

        createNotificationChannels()
        automationEngine.start()

        // Check for active tasks to recover after process restart
        applicationScope.launch {
            val running = repeatingTaskRepository.getRunningTasksList()
            if (running.isNotEmpty()) {
                executionLogRepository.logInfo("Application launched: Recovering ${running.size} active repeating task(s).")
                for (task in running) {
                    AutomationExecutionService.startTask(this@AutoPulseApplication, task.id)
                }
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val foregroundChannel = NotificationChannel(
                CHANNEL_FOREGROUND_SERVICE,
                getString(R.string.notification_channel_foreground),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows notifications for ongoing background automations"
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                getString(R.string.notification_channel_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows urgent alerts and automation status notifications"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(foregroundChannel)
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }

    companion object {
        const val CHANNEL_FOREGROUND_SERVICE = "autopulse_foreground_channel"
        const val CHANNEL_ALERTS = "autopulse_alerts_channel"

        lateinit var instance: AutoPulseApplication
            private set
    }
}
