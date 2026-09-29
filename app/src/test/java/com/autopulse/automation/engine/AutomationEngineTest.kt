package com.autopulse.automation.engine

import android.content.Context
import com.autopulse.automation.data.model.Action
import com.autopulse.automation.data.model.ActionType
import com.autopulse.automation.data.model.Automation
import com.autopulse.automation.data.model.AutomationWithRules
import com.autopulse.automation.data.model.CollisionStrategy
import com.autopulse.automation.data.model.Condition
import com.autopulse.automation.data.model.ConditionType
import com.autopulse.automation.data.model.FieldToMatch
import com.autopulse.automation.data.model.MatchType
import com.autopulse.automation.data.model.RepeatingTask
import com.autopulse.automation.data.model.TaskStatus
import com.autopulse.automation.data.model.Trigger
import com.autopulse.automation.data.model.TriggerType
import com.autopulse.automation.data.repository.AutomationRepository
import com.autopulse.automation.data.repository.ExecutionLogRepository
import com.autopulse.automation.data.repository.RepeatingTaskRepository
import com.autopulse.automation.event.NotificationEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutomationEngineTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val context: Context = mockk(relaxed = true)
    private val automationRepository: AutomationRepository = mockk(relaxed = true)
    private val taskRepository: RepeatingTaskRepository = mockk(relaxed = true)
    private val logRepository: ExecutionLogRepository = mockk(relaxed = true)
    private val actionExecutor: ActionExecutor = mockk(relaxed = true)

    private lateinit var engine: AutomationEngine

    @Before
    fun setup() {
        engine = AutomationEngine(
            context = context,
            automationRepository = automationRepository,
            taskRepository = taskRepository,
            logRepository = logRepository,
            actionExecutor = actionExecutor,
            engineScope = testScope
        )
    }

    @Test
    fun testEvaluationMatchesAndExecutesActions() = runTest {
        val automation = Automation(
            id = 10L,
            name = "OLX Lead Alert",
            isEnabled = true,
            collisionStrategy = CollisionStrategy.IGNORE
        )
        val trigger = Trigger(
            id = 1L,
            automationId = 10L,
            type = TriggerType.NOTIFICATION_RECEIVED,
            packageName = "com.olx.southasia",
            matchType = MatchType.CONTAINS
        )
        val condition = Condition(
            id = 1L,
            automationId = 10L,
            type = ConditionType.TEXT_CONTAINS,
            fieldToMatch = FieldToMatch.ANY,
            value = "lead"
        )
        val action = Action(
            id = 1L,
            automationId = 10L,
            type = ActionType.START_REPEAT,
            payloadJson = """{"intervalSeconds":30}"""
        )

        val rule = AutomationWithRules(
            automation = automation,
            triggers = listOf(trigger),
            conditions = listOf(condition),
            actions = listOf(action)
        )

        coEvery { automationRepository.getEnabledAutomationsList() } returns listOf(rule)
        coEvery { taskRepository.getRunningTaskForAutomation(10L) } returns null

        val event = NotificationEvent(
            packageName = "com.olx.southasia",
            appName = "OLX",
            title = "New Lead received",
            text = "Someone is interested in your item"
        )

        engine.evaluateEvent(event)

        // Verify actions were executed
        coVerify(exactly = 1) {
            actionExecutor.executeActions(automation, listOf(action), event)
        }
    }

    @Test
    fun testCollisionIgnorePreventsDuplicateTasks() = runTest {
        val automation = Automation(
            id = 20L,
            name = "OLX Lead Alert",
            isEnabled = true,
            collisionStrategy = CollisionStrategy.IGNORE
        )
        val trigger = Trigger(
            id = 2L,
            automationId = 20L,
            type = TriggerType.NOTIFICATION_RECEIVED,
            packageName = "com.olx.southasia"
        )
        val condition = Condition(
            id = 2L,
            automationId = 20L,
            type = ConditionType.TEXT_CONTAINS,
            value = "lead"
        )
        val action = Action(
            id = 2L,
            automationId = 20L,
            type = ActionType.START_REPEAT
        )

        val rule = AutomationWithRules(
            automation = automation,
            triggers = listOf(trigger),
            conditions = listOf(condition),
            actions = listOf(action)
        )

        val existingRunningTask = RepeatingTask(
            id = "task-uuid-123",
            automationId = 20L,
            automationName = "OLX Lead Alert",
            status = TaskStatus.RUNNING,
            intervalSeconds = 30L
        )

        coEvery { automationRepository.getEnabledAutomationsList() } returns listOf(rule)
        coEvery { taskRepository.getRunningTaskForAutomation(20L) } returns existingRunningTask

        val secondLeadEvent = NotificationEvent(
            packageName = "com.olx.southasia",
            appName = "OLX",
            title = "Second lead received",
            text = "Another lead"
        )

        engine.evaluateEvent(secondLeadEvent)

        // Action executor must NOT be called when IGNORE strategy is active
        coVerify(exactly = 0) {
            actionExecutor.executeActions(any(), any(), any())
        }

        // Must log the collision warning
        coVerify(atLeast = 1) {
            logRepository.logWarn(any(), 20L, "OLX Lead Alert", "task-uuid-123")
        }
    }
}
