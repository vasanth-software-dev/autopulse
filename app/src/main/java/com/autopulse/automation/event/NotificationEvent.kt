package com.autopulse.automation.event

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class NotificationEvent(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val subText: String = "",
    val bigText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isOngoing: Boolean = false,
    val notificationKey: String = ""
) {
    /**
     * Full combined textual content of the notification for unified matching.
     */
    val combinedContent: String
        get() = buildString {
            if (title.isNotBlank()) append(title).append(" ")
            if (text.isNotBlank()) append(text).append(" ")
            if (subText.isNotBlank()) append(subText).append(" ")
            if (bigText.isNotBlank()) append(bigText)
        }.trim()

    /**
     * Returns a dictionary of dynamic variables for template substitution.
     */
    fun toVariableMap(): Map<String, String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val fullFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateObj = Date(timestamp)

        val effectiveText = when {
            bigText.isNotBlank() -> bigText
            text.isNotBlank() -> text
            else -> subText
        }

        return mapOf(
            "app_name" to appName,
            "package_name" to packageName,
            "notification_title" to title,
            "notification_text" to effectiveText,
            "title" to title,
            "text" to effectiveText,
            "timestamp" to fullFormat.format(dateObj),
            "date" to dateFormat.format(dateObj),
            "time" to timeFormat.format(dateObj)
        )
    }

    /**
     * Interpolates dynamic variables in a template string, e.g.:
     * "Lead: {{notification_title}} - {{notification_text}}"
     */
    fun interpolate(template: String): String {
        var result = template
        val variables = toVariableMap()
        for ((key, value) in variables) {
            result = result.replace("{{$key}}", value, ignoreCase = true)
        }
        return result
    }
}
