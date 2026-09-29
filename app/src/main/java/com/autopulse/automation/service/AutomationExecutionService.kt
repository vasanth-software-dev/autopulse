package com.autopulse.automation.service

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.autopulse.automation.AutoPulseApplication
import com.autopulse.automation.R
import com.autopulse.automation.data.model.RepeatingTask
import com.autopulse.automation.data.model.TaskStatus
import com.autopulse.automation.telegram.TelegramMessageFormatter
import com.autopulse.automation.telegram.TelegramResult
import com.autopulse.automation.ui.MainActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class AutomationExecutionService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activeTaskJobs = ConcurrentHashMap<String, Job>()
    private var wakeLock: PowerManager.WakeLock? = null

    private val app by lazy { application as AutoPulseApplication }
    private val taskRepo by lazy { app.repeatingTaskRepository }
    private val logRepo by lazy { app.executionLogRepository }
    private val telegramRepo by lazy { app.telegramSettingsRepository }
    private val telegramClient by lazy { app.telegramBotClient }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "AutomationExecutionService created.")
        acquireWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_START_TASK -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                if (!taskId.isNullOrBlank()) {
                    startRepeatingTask(taskId)
                }
            }
            ACTION_STOP_TASK -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                if (!taskId.isNullOrBlank()) {
                    stopRepeatingTask(taskId, "User pressed STOP from notification.")
                }
            }
            ACTION_STOP_ALL -> {
                stopAllTasks("User pressed STOP ALL from notification.")
            }
            ACTION_RECOVER_RUNNING_TASKS -> {
                recoverRunningTasks()
            }
            else -> {
                recoverRunningTasks()
            }
        }

        return START_STICKY
    }

    private fun startRepeatingTask(taskId: String) {
        // Prevent duplicate jobs for the same task
        if (activeTaskJobs.containsKey(taskId) && activeTaskJobs[taskId]?.isActive == true) {
            Log.d(TAG, "Task $taskId is already actively running. Skipping duplicate job launch.")
            return
        }

        serviceScope.launch {
            val task = taskRepo.getTaskByIdDirect(taskId)
            if (task == null || task.status != TaskStatus.RUNNING) {
                Log.w(TAG, "Cannot start task $taskId: Task not found or not in RUNNING state.")
                checkIfServiceShouldStop()
                return@launch
            }

            // Immediately promote service to foreground with persistent notification
            val notification = buildForegroundNotification(task)
            startForeground(NOTIFICATION_ID, notification)

            // Launch repeating execution job
            val job = launch(Dispatchers.Default) {
                runRepeatingTaskLoop(taskId)
            }
            activeTaskJobs[taskId] = job
        }
    }

    private suspend fun runRepeatingTaskLoop(taskId: String) {
        val intervalMs = 30_000L // 30-second repeating cycle
        var isFirstExecution = true

        try {
            while (true) {
                // 1. Guard check: Must be in RUNNING state
                val currentTask = taskRepo.getTaskByIdDirect(taskId)
                if (currentTask == null || currentTask.status != TaskStatus.RUNNING) {
                    Log.i(TAG, "Task $taskId status is no longer RUNNING. Terminating loop.")
                    break
                }

                // 2. Execute Telegram notification tick
                executeTelegramTick(currentTask, isFirstExecution)
                isFirstExecution = false

                // 3. Double-check after network call before recording tick or scheduling delay
                val verifiedTask = taskRepo.getTaskByIdDirect(taskId)
                if (verifiedTask == null || verifiedTask.status != TaskStatus.RUNNING) {
                    break
                }

                val now = System.currentTimeMillis()
                val nextExecution = now + intervalMs
                taskRepo.recordTick(taskId, lastExecution = now, nextExecution = nextExecution)

                // Update foreground notification display
                updateNotification(verifiedTask.copy(
                    executionCount = verifiedTask.executionCount + 1,
                    lastExecutionTimestamp = now,
                    nextExecutionTimestamp = nextExecution
                ))

                // Schedule alarm fallback for deep Doze mode
                scheduleAlarmWakeup(nextExecution)

                // 4. Wait for 30 seconds before next notification
                delay(intervalMs)
            }
        } catch (e: CancellationException) {
            Log.i(TAG, "Task $taskId coroutine cancelled cleanly.")
        } catch (e: Exception) {
            Log.e(TAG, "Unhandled exception in task loop for $taskId", e)
            taskRepo.markFailed(taskId, e.localizedMessage ?: "Execution error")
            logRepo.logError("Repeating task failure: ${e.localizedMessage}", taskId = taskId)
        } finally {
            activeTaskJobs.remove(taskId)
            checkIfServiceShouldStop()
        }
    }

    private suspend fun executeTelegramTick(task: RepeatingTask, isInitialTick: Boolean) {
        val token = telegramRepo.getBotToken()
        val chatId = telegramRepo.getChatId()

        if (token.isBlank() || chatId.isBlank()) {
            val err = "Telegram credentials not configured. Skipping repeat tick."
            logRepo.logError(err, automationId = task.automationId, automationName = task.automationName, taskId = task.id)
            taskRepo.markFailed(task.id, err)
            return
        }

        // Format message safely
        val eventMock = com.autopulse.automation.event.NotificationEvent(
            packageName = task.leadPackage,
            appName = task.leadAppName.ifBlank { "OLX" },
            title = task.leadTitle,
            text = task.leadText,
            timestamp = System.currentTimeMillis()
        )

        val message = TelegramMessageFormatter.formatHtmlMessage(task.customMessageTemplate, eventMock)

        val result = telegramClient.sendMessage(
            botToken = token,
            chatId = chatId,
            text = message,
            parseMode = "HTML"
        )

        when (result) {
            is TelegramResult.Success -> {
                val tickLabel = if (isInitialTick) "Initial lead alert" else "30s repeat alert"
                logRepo.logSuccess(
                    message = "Telegram message sent ($tickLabel) for '${task.automationName}'.",
                    automationId = task.automationId,
                    automationName = task.automationName,
                    taskId = task.id
                )
            }
            is TelegramResult.Failure -> {
                logRepo.logError(
                    message = "Telegram delivery failed: ${result.description}",
                    automationId = task.automationId,
                    automationName = task.automationName,
                    taskId = task.id,
                    details = "HTTP ${result.httpCode ?: 0}"
                )
            }
        }
    }

    fun stopRepeatingTask(taskId: String, reason: String = "User pressed STOP") {
        serviceScope.launch {
            // Cancel running coroutine immediately
            activeTaskJobs[taskId]?.cancel()
            activeTaskJobs.remove(taskId)

            val task = taskRepo.getTaskByIdDirect(taskId)
            taskRepo.stopTask(taskId)

            logRepo.logInfo(
                message = "Repeating task stopped ($reason).",
                automationId = task?.automationId,
                automationName = task?.automationName,
                taskId = taskId
            )

            checkIfServiceShouldStop()
        }
    }

    fun stopAllTasks(reason: String = "User stopped all tasks") {
        serviceScope.launch {
            activeTaskJobs.values.forEach { it.cancel() }
            activeTaskJobs.clear()

            taskRepo.stopAllTasks()
            logRepo.logWarn("All active repeating tasks stopped ($reason).")

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun recoverRunningTasks() {
        serviceScope.launch {
            val runningTasks = taskRepo.getRunningTasksList()
            if (runningTasks.isEmpty()) {
                checkIfServiceShouldStop()
                return@launch
            }

            logRepo.logInfo("Recovering ${runningTasks.size} active repeating task(s) after process recreation.")
            for (task in runningTasks) {
                startRepeatingTask(task.id)
            }
        }
    }

    private suspend fun checkIfServiceShouldStop() {
        val remainingRunningTasks = taskRepo.getRunningTasksList()
        if (remainingRunningTasks.isEmpty() && activeTaskJobs.isEmpty()) {
            Log.i(TAG, "No remaining active tasks. Terminating Foreground Service.")
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildForegroundNotification(task: RepeatingTask): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action button to STOP the repeating task directly from notification bar
        val stopIntent = Intent(this, AutomationExecutionService::class.java).apply {
            action = ACTION_STOP_TASK
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            task.id.hashCode(),
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val lastRun = if (task.lastExecutionTimestamp > 0) timeFormat.format(Date(task.lastExecutionTimestamp)) else "Starting"

        return NotificationCompat.Builder(this, AutoPulseApplication.CHANNEL_FOREGROUND_SERVICE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("AutoPulse Running: ${task.automationName}")
            .setContentText("Interval: ${task.intervalSeconds}s • Sent: ${task.executionCount} • Last: $lastRun")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "STOP",
                stopPendingIntent
            )
            .build()
    }

    private fun updateNotification(task: RepeatingTask) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildForegroundNotification(task))
    }

    private fun scheduleAlarmWakeup(triggerAtMillis: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val alarmIntent = Intent(this, AutomationExecutionService::class.java).apply {
            action = ACTION_ALARM_TICK
        }
        val pendingIntent = PendingIntent.getService(
            this,
            ALARM_REQUEST_CODE,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission not granted, relying on foreground service coroutine: ${e.message}")
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AutoPulse:ExecutionWakeLock")
            wakeLock?.setReferenceCounted(false)
            wakeLock?.acquire(60 * 60 * 1000L) // 1 hour max safety timeout
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock", e)
        } finally {
            wakeLock = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        activeTaskJobs.values.forEach { it.cancel() }
        activeTaskJobs.clear()
        Log.i(TAG, "AutomationExecutionService destroyed.")
    }

    companion object {
        private const val TAG = "AutoPulseExecution"
        private const val NOTIFICATION_ID = 9001
        private const val ALARM_REQUEST_CODE = 9002

        const val ACTION_START_TASK = "com.autopulse.action.START_TASK"
        const val ACTION_STOP_TASK = "com.autopulse.action.STOP_TASK"
        const val ACTION_STOP_ALL = "com.autopulse.action.STOP_ALL"
        const val ACTION_RECOVER_RUNNING_TASKS = "com.autopulse.action.RECOVER_RUNNING_TASKS"
        const val ACTION_ALARM_TICK = "com.autopulse.action.ALARM_TICK"

        const val EXTRA_TASK_ID = "extra_task_id"

        fun startTask(context: Context, taskId: String) {
            val intent = Intent(context, AutomationExecutionService::class.java).apply {
                action = ACTION_START_TASK
                putExtra(EXTRA_TASK_ID, taskId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopTask(context: Context, taskId: String) {
            val intent = Intent(context, AutomationExecutionService::class.java).apply {
                action = ACTION_STOP_TASK
                putExtra(EXTRA_TASK_ID, taskId)
            }
            context.startService(intent)
        }

        fun stopAll(context: Context) {
            val intent = Intent(context, AutomationExecutionService::class.java).apply {
                action = ACTION_STOP_ALL
            }
            context.startService(intent)
        }
    }
}
