package com.autopulse.automation.event

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationEventTest {

    @Test
    fun testNotificationEventVariablesAndInterpolation() {
        val event = NotificationEvent(
            packageName = "com.olx.southasia",
            appName = "OLX",
            title = "New Inquiry: Royal Enfield 350",
            text = "Hi, is this bike still available?",
            timestamp = 1717200000000L
        )

        val vars = event.toVariableMap()
        assertEquals("OLX", vars["app_name"])
        assertEquals("com.olx.southasia", vars["package_name"])
        assertEquals("New Inquiry: Royal Enfield 350", vars["notification_title"])
        assertEquals("Hi, is this bike still available?", vars["notification_text"])

        val template = "🔥 NEW LEAD\nApp: {{app_name}}\nTitle: {{notification_title}}\nMessage: {{notification_text}}"
        val interpolated = event.interpolate(template)

        assertTrue(interpolated.contains("App: OLX"))
        assertTrue(interpolated.contains("Title: New Inquiry: Royal Enfield 350"))
        assertTrue(interpolated.contains("Message: Hi, is this bike still available?"))
    }

    @Test
    fun testCombinedContent() {
        val event = NotificationEvent(
            packageName = "com.olx.southasia",
            appName = "OLX",
            title = "Buyer Message",
            text = "I want to buy",
            subText = "Chat Alert"
        )

        val combined = event.combinedContent
        assertTrue(combined.contains("Buyer Message"))
        assertTrue(combined.contains("I want to buy"))
        assertTrue(combined.contains("Chat Alert"))
    }

    @Test
    fun testNotificationEventBusReactiveDelivery() = runTest {
        val testEvent = NotificationEvent(
            packageName = "com.olx.southasia",
            appName = "OLX",
            title = "Test Lead",
            text = "Interested in purchase"
        )

        var receivedEvent: NotificationEvent? = null
        val job = launch {
            receivedEvent = NotificationEventBus.events.first()
        }

        NotificationEventBus.publish(testEvent)
        job.join()

        assertEquals(testEvent.id, receivedEvent?.id)
        assertEquals("OLX", receivedEvent?.appName)
        assertEquals("Test Lead", receivedEvent?.title)
    }
}
