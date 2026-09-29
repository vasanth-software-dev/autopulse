package com.autopulse.automation.engine

import com.autopulse.automation.data.model.Condition
import com.autopulse.automation.data.model.ConditionType
import com.autopulse.automation.data.model.FieldToMatch
import com.autopulse.automation.event.NotificationEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionMatchingTest {

    @Test
    fun testTextContainsCaseInsensitive() {
        val condition = Condition(
            automationId = 1,
            type = ConditionType.TEXT_CONTAINS,
            fieldToMatch = FieldToMatch.NOTIFICATION_TEXT,
            value = "lead",
            isCaseSensitive = false
        )

        val eventWithUpper = NotificationEvent(
            packageName = "com.olx.southasia",
            appName = "OLX",
            title = "Notification",
            text = "You received a new LEAD inquiry."
        )
        val result = ConditionManager.evaluate(listOf(condition), eventWithUpper)
        assertTrue(result.allPassed)

        val eventWithout = NotificationEvent(
            packageName = "com.olx.southasia",
            appName = "OLX",
            title = "Promo",
            text = "Discount coupon available!"
        )
        assertFalse(ConditionManager.evaluate(listOf(condition), eventWithout).allPassed)
    }

    @Test
    fun testRegexCondition() {
        val regexCondition = Condition(
            automationId = 1,
            type = ConditionType.REGEX_MATCH,
            fieldToMatch = FieldToMatch.ANY,
            value = "\\b(urgent|lead|buyer)\\b",
            isCaseSensitive = false
        )

        val matchEvent = NotificationEvent(
            packageName = "com.olx.southasia",
            appName = "OLX",
            title = "Item update",
            text = "A buyer is asking for final price"
        )
        assertTrue(ConditionManager.evaluate(listOf(regexCondition), matchEvent).allPassed)

        val noMatchEvent = NotificationEvent(
            packageName = "com.olx.southasia",
            appName = "OLX",
            title = "General",
            text = "Your ad has been published"
        )
        assertFalse(ConditionManager.evaluate(listOf(regexCondition), noMatchEvent).allPassed)
    }

    @Test
    fun testNegatedCondition() {
        val notSpamCondition = Condition(
            automationId = 1,
            type = ConditionType.TEXT_CONTAINS,
            fieldToMatch = FieldToMatch.ANY,
            value = "promotional",
            isNegated = true
        )

        val legitimateLead = NotificationEvent(
            packageName = "com.olx.southasia",
            appName = "OLX",
            title = "Inquiry",
            text = "Is this available?"
        )
        assertTrue(ConditionManager.evaluate(listOf(notSpamCondition), legitimateLead).allPassed)

        val promoEvent = NotificationEvent(
            packageName = "com.olx.southasia",
            appName = "OLX",
            title = "Special Offer",
            text = "Check out this promotional deal!"
        )
        assertFalse(ConditionManager.evaluate(listOf(notSpamCondition), promoEvent).allPassed)
    }

    @Test
    fun testMultipleConditionsAndLogic() {
        val cond1 = Condition(
            automationId = 1,
            type = ConditionType.TEXT_CONTAINS,
            fieldToMatch = FieldToMatch.NOTIFICATION_TITLE,
            value = "Lead"
        )
        val cond2 = Condition(
            automationId = 1,
            type = ConditionType.TEXT_CONTAINS,
            fieldToMatch = FieldToMatch.NOTIFICATION_TEXT,
            value = "iPhone"
        )

        val matchBoth = NotificationEvent(
            packageName = "com.olx.southasia",
            appName = "OLX",
            title = "New Lead received",
            text = "Inquiry on iPhone 15"
        )
        assertTrue(ConditionManager.evaluate(listOf(cond1, cond2), matchBoth).allPassed)

        val matchOnlyOne = NotificationEvent(
            packageName = "com.olx.southasia",
            appName = "OLX",
            title = "New Lead received",
            text = "Inquiry on Samsung Galaxy"
        )
        assertFalse(ConditionManager.evaluate(listOf(cond1, cond2), matchOnlyOne).allPassed)
    }
}
