package com.autopulse.automation.service

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.StatusBarNotification
import com.autopulse.automation.event.NotificationEvent
import java.util.concurrent.ConcurrentHashMap

object NotificationExtractor {

    private val appLabelCache = ConcurrentHashMap<String, String>()

    fun extract(context: Context, sbn: StatusBarNotification): NotificationEvent {
        val packageName = sbn.packageName.orEmpty()
        val appName = getAppLabel(context, packageName)

        val notification = sbn.notification
        val extras = notification.extras

        // Extract title
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence("android.title")?.toString().orEmpty()

        // Extract standard text
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence("android.text")?.toString().orEmpty()

        // Extract big text for expanded notifications
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()

        // Extract sub text (often contains sender group or category)
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()

        // Handle InboxStyle notifications (multi-line messages common in chat apps)
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        val textLinesCombined = lines?.joinToString("\n") { it.toString() }.orEmpty()

        val effectiveBigText = when {
            bigText.isNotBlank() -> bigText
            textLinesCombined.isNotBlank() -> textLinesCombined
            else -> ""
        }

        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0

        val postTime = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()

        return NotificationEvent(
            packageName = packageName,
            appName = appName,
            title = title.trim(),
            text = text.trim(),
            subText = subText.trim(),
            bigText = effectiveBigText.trim(),
            timestamp = postTime,
            isOngoing = isOngoing,
            notificationKey = sbn.key.orEmpty()
        )
    }

    private fun getAppLabel(context: Context, packageName: String): String {
        if (packageName.isBlank()) return "Unknown"
        appLabelCache[packageName]?.let { return it }

        val pm = context.packageManager
        val label = try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            // If app isn't installed directly or package is unknown, fallback to last segment of package name
            packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }

        appLabelCache[packageName] = label
        return label
    }

    fun clearCache() {
        appLabelCache.clear()
    }
}
