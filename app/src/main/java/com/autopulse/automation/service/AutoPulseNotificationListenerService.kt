package com.autopulse.automation.service

import android.content.ComponentName
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.autopulse.automation.AutoPulseApplication
import com.autopulse.automation.event.NotificationEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class AutoPulseNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val processedNotificationCache = ConcurrentHashMap<String, Long>()

    private val app by lazy { application as? AutoPulseApplication }
    private val logRepo by lazy { app?.executionLogRepository }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "AutoPulse NotificationListenerService connected successfully.")
        serviceScope.launch {
            logRepo?.logInfo("Notification Listener Service connected and listening.")
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "AutoPulse NotificationListenerService disconnected.")
        serviceScope.launch {
            logRepo?.logWarn("Notification Listener Service disconnected.")
        }

        // Attempt automatic rebind on modern Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(ComponentName(this, AutoPulseNotificationListenerService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request rebind", e)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // 1. Never inspect or trigger on AutoPulse's own notifications
        if (sbn.packageName == packageName) {
            return
        }

        // 2. Extract notification data into clean domain event
        val event = NotificationExtractor.extract(this, sbn)

        // 3. Skip completely empty notifications
        if (event.title.isBlank() && event.text.isBlank() && event.bigText.isBlank()) {
            return
        }

        // 4. Debounce duplicate OS dispatches for the exact same notification within 1.5s
        val notificationFingerprint = "${event.packageName}:${event.title}:${event.text}"
        val now = System.currentTimeMillis()
        val lastSeen = processedNotificationCache[notificationFingerprint] ?: 0L

        if (now - lastSeen < 1500L) {
            // Duplicate notification broadcast within debounce window, skip
            return
        }
        processedNotificationCache[notificationFingerprint] = now
        cleanOldCacheEntries(now)

        Log.d(TAG, "Notification intercepted: [${event.appName}] ${event.title} - ${event.text}")

        // 5. Publish to reactive event bus and log event
        serviceScope.launch {
            logRepo?.logInfo(
                message = "Notification received from ${event.appName}: \"${event.title.take(40)}\"",
                detailsJson = "Package: ${event.packageName} | Content: ${event.text.take(80)}"
            )
            NotificationEventBus.publish(event)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    private fun cleanOldCacheEntries(currentTime: Long) {
        if (processedNotificationCache.size > 100) {
            processedNotificationCache.entries.removeIf { (currentTime - it.value) > 10000L }
        }
    }

    companion object {
        private const val TAG = "AutoPulseNotifListener"
    }
}
