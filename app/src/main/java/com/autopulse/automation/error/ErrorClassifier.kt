package com.autopulse.automation.error

sealed class AutoPulseError(val code: String, val message: String, val recoverySuggestion: String) {

    // Network Errors
    object NoInternet : AutoPulseError(
        code = "NET_001",
        message = "No active internet connection.",
        recoverySuggestion = "Check Wi-Fi or mobile data connectivity on Phone 1."
    )

    data class NetworkTimeout(val timeoutSeconds: Int) : AutoPulseError(
        code = "NET_002",
        message = "Connection timed out after ${timeoutSeconds}s.",
        recoverySuggestion = "Check network signal quality. AutoPulse will retry transient failures automatically."
    )

    // Telegram Bot Errors
    object InvalidBotToken : AutoPulseError(
        code = "TG_401",
        message = "Invalid Telegram Bot Token.",
        recoverySuggestion = "Open @BotFather on Telegram, copy the exact HTTP API token, and update Telegram Settings."
    )

    object ChatNotFound : AutoPulseError(
        code = "TG_404",
        message = "Chat ID not found or bot hasn't been started.",
        recoverySuggestion = "Open Telegram, start a chat with your bot, type /start, and verify your Chat ID."
    )

    object BotBlocked : AutoPulseError(
        code = "TG_403",
        message = "Bot was blocked by user or kicked from channel.",
        recoverySuggestion = "Unblock the bot in Telegram or ensure it has permission to post in your channel."
    )

    data class RateLimited(val retryAfterSeconds: Int) : AutoPulseError(
        code = "TG_429",
        message = "Telegram API rate limit exceeded.",
        recoverySuggestion = "Wait $retryAfterSeconds seconds before dispatching further messages."
    )

    data class TelegramApiError(val httpCode: Int, val desc: String) : AutoPulseError(
        code = "TG_$httpCode",
        message = "Telegram API error: $desc",
        recoverySuggestion = "Verify message template syntax and parameters."
    )

    // System & Permission Errors
    object NotificationListenerDisabled : AutoPulseError(
        code = "SYS_PERM_NOTIF",
        message = "Notification Listener Service access is disabled.",
        recoverySuggestion = "Navigate to Permissions > Notification Access and enable AutoPulse."
    )

    object NotificationsDisabled : AutoPulseError(
        code = "SYS_PERM_POST",
        message = "Post Notifications permission is revoked.",
        recoverySuggestion = "Allow notifications in Android app settings to display persistent STOP controls."
    )

    object BatteryOptimized : AutoPulseError(
        code = "SYS_BATTERY",
        message = "Battery optimization is active; Android Doze may throttle repeating tasks.",
        recoverySuggestion = "Go to Permissions > Battery Optimization and select 'Unrestricted / Don't optimize'."
    )

    // Collision & Execution
    data class CollisionIgnored(val ruleName: String, val activeTaskId: String) : AutoPulseError(
        code = "EXEC_COLLISION",
        message = "Subsequent lead ignored for '$ruleName' because repeating task ($activeTaskId) is already active.",
        recoverySuggestion = "To process multiple leads concurrently, change Collision Strategy to 'Start New' in Rule Builder."
    )
}
