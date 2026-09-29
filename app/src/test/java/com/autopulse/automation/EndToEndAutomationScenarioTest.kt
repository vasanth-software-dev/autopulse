package com.autopulse.automation

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
import com.autopulse.automation.data.repository.TelegramSettingsRepository
import com.autopulse.automation.engine.ActionExecutor
import com.autopulse.automation.engine.AutomationEngine
import com.autopulse.automation.event.NotificationEvent
import com.autopulse.automation.telegram.TelegramBotClient
import com.autopulse.automation.telegram.TelegramMessageResult
import com.autopulse.automation.telegram.TelegramResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EndToEndAutomationScenarioTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val context: Context = mockk(relaxed = true)
    private val automationRepo: AutomationRepository = mockk(relaxed = true)
    private val taskRepo: RepeatingTaskRepository = mockk(relaxed = true)
    private val logRepo: ExecutionLogRepository = mockk(relaxed = true)
    private val telegramSettingsRepo: TelegramSettingsRepository = mockk(relaxed = true)
    private val telegramBotClient: TelegramBotClient = mockk(relaxed = true)

    private lateinit var actionExecutor: ActionExecutor
    private lateinit var engine: AutomationEngine

    @Before
    fun setup() {
        coEvery { telegramSettingsRepo.getBotToken() } returns "123456789:ABCdefGHIjklMNOpqrsTUVwxyz"
        coEvery { telegramSettingsRepo.getChatId() } returns "987654321"

        actionExecutor = ActionExecutor(
            context = context,
            taskRepository = taskRepo,
            logRepository = logRepo,
            telegramSettingsRepository = telegramSettingsRepo,
            telegramBotClient = telegramBotClient
        )

        engine = AutomationEngine(
            context = context,
            automationRepository = automationRepo,
            taskRepository = taskRepo,
            logRepository = logRepo,
            actionExecutor = actionExecutor,
            engineScope = testScope
        )
    }

    @Test
    fun testCompleteOlxLeadToTelegramFlowWithStop() = runTest {
        // 1. Setup OLX Lead Automation in Room repository mock
        val olxAutomation = Automation(
            id = 1L,
            name = "OLX Lead Alert",
            isEnabled = true,
            collisionStrategy = CollisionStrategy.IGNORE
        )
        val olxTrigger = Trigger(
            id = 1L,
            automationId = 1L,
            type = TriggerType.NOTIFICATION_RECEIVED,
            packageName = "com.olx.southasia",
            appName = "OLX",
            matchType = MatchType.CONTAINS
        )
        val olxCondition = Condition(
            id = 1L,
            automationId = 1L,
            type = ConditionType.TEXT_CONTAINS,
            fieldToMatch = FieldToMatch.ANY,
            value = "lead"
        )
        val repeatAction = Action(
            id = 1L,
            automationId = 1L,
            orderIndex = 0,
            type = ActionType.START_REPEAT,
            payloadJson = """{"intervalSeconds":30,"untilStopped":true}"""
        )
        val telegramAction = Action(
            id = 2L,
            automationId = 1L,
            orderIndex = 1,
            type = ActionType.SEND_TELEGRAM,
            payloadJson = """{"messageTemplate":"🔥 NEW {{app_name}} LEAD\nTitle: {{notification_title}}\nMessage: {{notification_text}}"}"""
        )

        val fullRule = AutomationWithRules(
            automation = olxAutomation,
            triggers = listOf(olxTrigger),
            conditions = listOf(olxCondition),
            actions = listOf(repeatAction, telegramAction)
        )

        coEvery { automationRepo.getEnabledAutomationsList() } returns listOf(fullRule)
        coEvery { taskRepo.getRunningTaskForAutomation(1L) } returns null

        val taskSlot = slot<RepeatingTask>()
        coEvery { taskRepo.insertOrUpdateTask(capture(taskSlot)) } returns Unit

        coEvery {
            telegramBotClient.sendMessage(any(), any(), any(), any(), any())
        } returns TelegramResult.Success(TelegramMessageResult(messageId = 555L, date = 1717200000L))

        // 2. Incoming Notification Event: OLX Buyer Lead arrives
        val leadEvent = NotificationEvent(
            packageName = "com.olx.southasia",
            appName = "OLX",
            title = "New Lead: iPhone 15 Pro",
            text = "Hi, is this lead still available?"
        )

        // 3. Engine evaluates the incoming lead event
        engine.evaluateEvent(leadEvent)

        // 4. Verify RepeatingTask was created with interval 30s
        coVerify(exactly = 1) { taskRepo.insertOrUpdateTask(any()) }
        val createdTask = taskSlot.captured
        assertNotNull(createdTask)
        assertEquals(1L, createdTask.automationId)
        assertEquals("OLX Lead Alert", createdTask.automationName)
        assertEquals(30L, createdTask.intervalSeconds)
        assertEquals(TaskStatus.RUNNING, createdTask.status)
        assertEquals("New Lead: iPhone 15 Pro", createdTask.leadTitle)

        // 5. Verify immediate first Telegram alert was sent
        coVerify(exactly = 1) {
            telegramBotClient.sendMessage(
                botToken = "123456789:ABCdefGHIjklMNOpqrsTUVwxyz",
                chatId = "987654321",
                text = match { it.contains("NEW OLX LEAD") && it.contains("iPhone 15 Pro") },
                parseMode = "HTML"
            )
        }

        // 6. Test Collision Prevention: Second lead arrives while task is active
        coEvery { taskRepo.getRunningTaskForAutomation(1L) } returns createdTask

        val secondLeadEvent = NotificationEvent(
            packageName = "com.olx.southasia",
            appName = "OLX",
            title = "Another Lead",
            text = "Is price negotiable?"
        )

        engine.evaluateEvent(secondLeadEvent)

        // CollisionStrategy.IGNORE must prevent duplicate task creation or duplicate Telegram dispatch
        coVerify(exactly = 1) { taskRepo.insertOrUpdateTask(any()) } // still 1
        coVerify(exactly = 1) { telegramBotClient.sendMessage(any(), any(), any(), any(), any()) } // still 1

        // 7. Test STOP action: User manually presses STOP
        coEvery { taskRepo.getTaskByIdDirect(createdTask.id) } returns createdTask.copy(status = TaskStatus.STOPPED)

        taskRepo.stopTask(createdTask.id)
        coVerify(exactly = 1) { taskRepo.stopTask(createdTask.id) }

        val stoppedTask = taskRepo.getTaskByIdDirect(createdTask.id)
        assertEquals(TaskStatus.STOPPED, stoppedTask?.status)
    }

    @Test
    fun testMultipleAutomationsEvaluation() = runTest {
        val olxRule = AutomationWithRules(
            automation = Automation(id = 1L, name = "OLX Rule", isEnabled = true),
            triggers = listOf(Trigger(automationId = 1L, type = TriggerType.NOTIFICATION_RECEIVED, packageName = "com.olx.southasia")),
            conditions = listOf(Condition(automationId = 1L, type = ConditionType.TEXT_CONTAINS, value = "olx")),
            actions = listOf(Action(automationId = 1L, type = ActionType.LOG_MESSAGE))
        )

        val whatsAppRule = AutomationWithRules(
            automation = Automation(id = 2L, name = "WhatsApp Rule", isEnabled = true),
            triggers = listOf(Trigger(automationId = 2L, type = TriggerType.NOTIFICATION_RECEIVED, packageName = "com.whatsapp")),
            conditions = listOf(Condition(automationId = 2L, type = ConditionType.TEXT_CONTAINS, value = "urgent")),
            actions = listOf(Action(automationId = 2L, type = ActionType.LOG_MESSAGE))
        )

        coEvery { automationRepo.getEnabledAutomationsList() } returns listOf(olxRule, whatsAppRule)
        coEvery { taskRepo.getRunningTaskForAutomation(any()) } returns null

        val whatsAppEvent = NotificationEvent(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "VIP Client",
            text = "This is urgent, please call."
        )

        engine.evaluateEvent(whatsAppEvent)

        // Verify only WhatsApp rule executed
        coVerify(exactly = 1) {
            logRepo.logInfo(
                message = match { it.contains("Automation matched: \"WhatsApp Rule\"") },
                automationId = 2L,
                automationName = "WhatsApp Rule",
                detailsJson = any()
            )
        }

        coVerify(exactly = 0) {
            logRepo.logInfo(
                message = match { it.contains("Automation matched: \"OLX Rule\"") },
                automationId = 1L,
                automationName = any(),
                detailsJson = any()
            )
        }
    }
}
