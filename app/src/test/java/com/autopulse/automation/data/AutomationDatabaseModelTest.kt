package com.autopulse.automation.data

import com.autopulse.automation.data.db.Converters
import com.autopulse.automation.data.model.Action
import com.autopulse.automation.data.model.ActionType
import com.autopulse.automation.data.model.Automation
import com.autopulse.automation.data.model.CollisionStrategy
import com.autopulse.automation.data.model.Condition
import com.autopulse.automation.data.model.ConditionType
import com.autopulse.automation.data.model.FieldToMatch
import com.autopulse.automation.data.model.LogLevel
import com.autopulse.automation.data.model.RepeatingTask
import com.autopulse.automation.data.model.TaskStatus
import com.autopulse.automation.data.model.Trigger
import com.autopulse.automation.data.model.TriggerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationDatabaseModelTest {

    private val converters = Converters()

    @Test
    fun testConverters() {
        assertEquals("NOTIFICATION_RECEIVED", converters.fromTriggerType(TriggerType.NOTIFICATION_RECEIVED))
        assertEquals(TriggerType.NOTIFICATION_RECEIVED, converters.toTriggerType("NOTIFICATION_RECEIVED"))

        assertEquals("START_REPEAT", converters.fromActionType(ActionType.START_REPEAT))
        assertEquals(ActionType.START_REPEAT, converters.toActionType("START_REPEAT"))

        assertEquals("RUNNING", converters.fromTaskStatus(TaskStatus.RUNNING))
        assertEquals(TaskStatus.RUNNING, converters.toTaskStatus("RUNNING"))

        assertEquals("SUCCESS", converters.fromLogLevel(LogLevel.SUCCESS))
        assertEquals(LogLevel.SUCCESS, converters.toLogLevel("SUCCESS"))
    }

    @Test
    fun testAutomationEntityCreation() {
        val automation = Automation(
            name = "OLX Lead Alert",
            description = "Test lead rule",
            isEnabled = true,
            collisionStrategy = CollisionStrategy.IGNORE
        )

        assertEquals("OLX Lead Alert", automation.name)
        assertTrue(automation.isEnabled)
        assertEquals(CollisionStrategy.IGNORE, automation.collisionStrategy)
    }

    @Test
    fun testTriggerAndConditionModels() {
        val trigger = Trigger(
            automationId = 1,
            type = TriggerType.NOTIFICATION_RECEIVED,
            packageName = "com.olx.southasia",
            appName = "OLX"
        )
        assertEquals("com.olx.southasia", trigger.packageName)

        val condition = Condition(
            automationId = 1,
            type = ConditionType.TEXT_CONTAINS,
            fieldToMatch = FieldToMatch.NOTIFICATION_TEXT,
            value = "lead"
        )
        assertEquals("lead", condition.value)
        assertEquals(FieldToMatch.NOTIFICATION_TEXT, condition.fieldToMatch)
    }

    @Test
    fun testRepeatingTaskCreation() {
        val task = RepeatingTask(
            automationId = 42,
            automationName = "OLX Lead Alert",
            intervalSeconds = 30L,
            leadTitle = "Interested in iPhone 15",
            leadText = "Hi, is this lead available?",
            leadPackage = "com.olx.southasia"
        )

        assertNotNull(task.id)
        assertEquals(TaskStatus.RUNNING, task.status)
        assertEquals(30L, task.intervalSeconds)
        assertEquals(0, task.executionCount)
    }
}
