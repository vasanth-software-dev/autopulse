package com.autopulse.automation.engine

import com.autopulse.automation.data.model.MatchType
import com.autopulse.automation.data.model.Trigger
import com.autopulse.automation.data.model.TriggerType
import com.autopulse.automation.event.NotificationEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerMatchingTest {

    @Test
    fun testExactPackageMatch() {
        val trigger = Trigger(
            automationId = 1,
            type = TriggerType.NOTIFICATION_RECEIVED,
            packageName = "com.olx.southasia",
            matchType = MatchType.EXACT
        )

        val matchingEvent = NotificationEvent(
            packageName = "com.olx.southasia",
            appName = "OLX",
            title = "New Lead",
            text = "Hello"
        )
        assertTrue(TriggerManager.matches(trigger, matchingEvent).isMatch)

        val nonMatchingEvent = NotificationEvent(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "Chat",
            text = "Hello"
        )
        assertFalse(TriggerManager.matches(trigger, nonMatchingEvent).isMatch)
    }

    @Test
    fun testContainsPackageOrAppNameMatch() {
        val trigger = Trigger(
            automationId = 1,
            type = TriggerType.NOTIFICATION_RECEIVED,
            packageName = "olx",
            appName = "OLX",
            matchType = MatchType.CONTAINS
        )

        val event = NotificationEvent(
            packageName = "com.olx.southasia",
            appName = "OLX India",
            title = "New Lead",
            text = "Offer received"
        )
        assertTrue(TriggerManager.matches(trigger, event).isMatch)
    }

    @Test
    fun testAnyAppTriggerMatch() {
        val anyAppTrigger = Trigger(
            automationId = 1,
            type = TriggerType.NOTIFICATION_RECEIVED,
            packageName = null,
            appName = null,
            matchType = MatchType.ANY
        )

        val event1 = NotificationEvent(packageName = "com.olx.southasia", appName = "OLX", title = "T", text = "M")
        val event2 = NotificationEvent(packageName = "com.google.android.gm", appName = "Gmail", title = "T", text = "M")

        assertTrue(TriggerManager.matches(anyAppTrigger, event1).isMatch)
        assertTrue(TriggerManager.matches(anyAppTrigger, event2).isMatch)
    }

    @Test
    fun testRegexTriggerMatch() {
        val regexTrigger = Trigger(
            automationId = 1,
            type = TriggerType.NOTIFICATION_RECEIVED,
            packageName = "^com\\.(olx|quikr)\\..*",
            matchType = MatchType.REGEX
        )

        val olxEvent = NotificationEvent(packageName = "com.olx.southasia", appName = "OLX", title = "T", text = "M")
        val quikrEvent = NotificationEvent(packageName = "com.quikr.classifieds", appName = "Quikr", title = "T", text = "M")
        val ebayEvent = NotificationEvent(packageName = "com.ebay.mobile", appName = "eBay", title = "T", text = "M")

        assertTrue(TriggerManager.matches(regexTrigger, olxEvent).isMatch)
        assertTrue(TriggerManager.matches(regexTrigger, quikrEvent).isMatch)
        assertFalse(TriggerManager.matches(regexTrigger, ebayEvent).isMatch)
    }
}
