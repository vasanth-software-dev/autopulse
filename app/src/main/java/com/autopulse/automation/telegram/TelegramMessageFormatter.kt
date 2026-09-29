package com.autopulse.automation.telegram

import com.autopulse.automation.event.NotificationEvent

object TelegramMessageFormatter {

    const val DEFAULT_LEAD_TEMPLATE_HTML = """🔥 <b>NEW {{app_name}} LEAD</b>

<b>Title:</b> {{notification_title}}
<b>Message:</b> {{notification_text}}
<b>Time:</b> {{timestamp}}"""

    /**
     * Escapes HTML special characters to prevent Telegram parse errors.
     */
    fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    /**
     * Escapes Telegram MarkdownV2 special characters.
     */
    fun escapeMarkdownV2(text: String): String {
        val escapeChars = "_*[]()~`>#+-=|{}.!"
        val sb = StringBuilder()
        for (ch in text) {
            if (escapeChars.contains(ch)) {
                sb.append('\\')
            }
            sb.append(ch)
        }
        return sb.toString()
    }

    /**
     * Formats a notification event with dynamic variable interpolation and safe HTML escaping.
     */
    fun formatHtmlMessage(template: String?, event: NotificationEvent): String {
        val rawTemplate = if (template.isNullOrBlank()) DEFAULT_LEAD_TEMPLATE_HTML else template

        val safeAppName = escapeHtml(event.appName)
        val safePkgName = escapeHtml(event.packageName)
        val safeTitle = escapeHtml(event.title)
        val rawText = if (event.bigText.isNotBlank()) event.bigText else event.text
        val safeText = escapeHtml(rawText)

        val vars = event.toVariableMap()
        val safeTimestamp = escapeHtml(vars["timestamp"].orEmpty())
        val safeDate = escapeHtml(vars["date"].orEmpty())
        val safeTime = escapeHtml(vars["time"].orEmpty())

        var result = rawTemplate
            .replace("{{app_name}}", safeAppName, ignoreCase = true)
            .replace("{{package_name}}", safePkgName, ignoreCase = true)
            .replace("{{notification_title}}", safeTitle, ignoreCase = true)
            .replace("{{title}}", safeTitle, ignoreCase = true)
            .replace("{{notification_text}}", safeText, ignoreCase = true)
            .replace("{{text}}", safeText, ignoreCase = true)
            .replace("{{timestamp}}", safeTimestamp, ignoreCase = true)
            .replace("{{date}}", safeDate, ignoreCase = true)
            .replace("{{time}}", safeTime, ignoreCase = true)

        return result
    }
}
