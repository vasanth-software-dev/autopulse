package com.autopulse.automation.util

import com.autopulse.automation.data.repository.ExecutionLogRepository
import com.autopulse.automation.event.NotificationEvent
import com.autopulse.automation.event.NotificationEventBus

object MockNotificationDispatcher {

    suspend fun dispatchMockOlxLead(
        logRepo: ExecutionLogRepository? = null,
        title: String = "You have new messages",
        text: String = "Open olx to check for missed updates"
    ): NotificationEvent {
        val event = NotificationEvent(
            packageName = "com.olx.southasia",
            appName = "OLX",
            title = title,
            text = text,
            subText = "Buyer Inquiry",
            bigText = text,
            timestamp = System.currentTimeMillis()
        )

        logRepo?.logInfo(
            message = "SIMULATED: Mock OLX Lead Notification dispatched.",
            detailsJson = "App: ${event.appName} (${event.packageName}) | Title: ${event.title}"
        )

        NotificationEventBus.publish(event)
        return event
    }

    suspend fun dispatchCustomMockNotification(
        packageName: String,
        appName: String,
        title: String,
        text: String,
        logRepo: ExecutionLogRepository? = null
    ): NotificationEvent {
        val event = NotificationEvent(
            packageName = packageName,
            appName = appName,
            title = title,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        logRepo?.logInfo(
            message = "SIMULATED: Mock Notification dispatched from $appName.",
            detailsJson = "Package: $packageName | Title: $title"
        )

        NotificationEventBus.publish(event)
        return event
    }
}
