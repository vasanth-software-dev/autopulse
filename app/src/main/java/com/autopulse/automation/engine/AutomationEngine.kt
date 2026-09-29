package com.autopulse.automation.engine

import android.content.Context
import android.util.Log
import com.autopulse.automation.data.model.Automation
import com.autopulse.automation.data.model.AutomationWithRules
import com.autopulse.automation.data.model.CollisionStrategy
import com.autopulse.automation.data.repository.AutomationRepository
import com.autopulse.automation.data.repository.ExecutionLogRepository
import com.autopulse.automation.data.repository.RepeatingTaskRepository
import com.autopulse.automation.event.NotificationEvent
import com.autopulse.automation.event.NotificationEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AutomationEngine(
    private val context: Context,
    private val automationRepository: AutomationRepository,
    private val taskRepository: RepeatingTaskRepository,
    private val logRepository: ExecutionLogRepository,
    private val actionExecutor: ActionExecutor,
    private val engineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    private var eventListenerJob: Job? = null

    /**
     * Starts listening to reactive notification events.
     */
    fun start() {
        if (eventListenerJob?.isActive == true) return

        Log.i(TAG, "AutoPulse AutomationEngine started.")
        eventListenerJob = engineScope.launch {
            NotificationEventBus.events.collect { event ->
                try {
                    evaluateEvent(event)
                } catch (e: Exception) {
                    Log.e(TAG, "Error evaluating notification event: ${e.localizedMessage}", e)
                    logRepository.logError(
                        message = "Engine evaluation error: ${e.localizedMessage ?: "Unknown error"}",
                        details = e.stackTraceToString()
                    )
                }
            }
        }
    }

    /**
     * Stops the event collector.
     */
    fun stop() {
        eventListenerJob?.cancel()
        eventListenerJob = null
        Log.i(TAG, "AutoPulse AutomationEngine stopped.")
    }

    /**
     * Evaluates a single event against all currently enabled automations.
     */
    suspend fun evaluateEvent(event: NotificationEvent) {
        val enabledAutomations = automationRepository.getEnabledAutomationsList()
        if (enabledAutomations.isEmpty()) {
            return
        }

        for (rule in enabledAutomations) {
            val automation = rule.automation
            val triggers = rule.triggers
            val conditions = rule.conditions
            val actions = rule.actions

            // 1. Evaluate Triggers (At least one trigger must match)
            val triggerMatch = triggers.firstOrNull { trigger ->
                TriggerManager.matches(trigger, event).isMatch
            }

            if (triggerMatch == null) {
                // Event does not match any trigger for this rule
                continue
            }

            // 2. Evaluate Conditions (All conditions must pass)
            val conditionResult = ConditionManager.evaluate(conditions, event)
            if (!conditionResult.allPassed) {
                // Rule triggers matched but conditions failed
                continue
            }

            // 3. Automation Matched! Log the match.
            logRepository.logInfo(
                message = "Automation matched: \"${automation.name}\"",
                automationId = automation.id,
                automationName = automation.name,
                detailsJson = "App: ${event.appName} | Title: ${event.title}"
            )

            // 4. Handle Collision & Duplicate Prevention
            val activeTask = taskRepository.getRunningTaskForAutomation(automation.id)
            if (activeTask != null) {
                when (automation.collisionStrategy) {
                    CollisionStrategy.IGNORE -> {
                        logRepository.logWarn(
                            message = "Collision: Repeating task is already active for '${automation.name}'. Subsequent lead ignored.",
                            automationId = automation.id,
                            automationName = automation.name,
                            taskId = activeTask.id
                        )
                        // Do not proceed with actions to prevent spamming
                        continue
                    }
                    CollisionStrategy.RESTART -> {
                        logRepository.logInfo(
                            message = "Collision: Restarting repeating task for '${automation.name}' with newest lead.",
                            automationId = automation.id,
                            automationName = automation.name,
                            taskId = activeTask.id
                        )
                        taskRepository.stopTask(activeTask.id)
                    }
                    CollisionStrategy.UPDATE_PAYLOAD -> {
                        logRepository.logInfo(
                            message = "Collision: Updating lead payload on active task for '${automation.name}'.",
                            automationId = automation.id,
                            automationName = automation.name,
                            taskId = activeTask.id
                        )
                        taskRepository.insertOrUpdateTask(
                            activeTask.copy(
                                leadTitle = event.title,
                                leadText = if (event.bigText.isNotBlank()) event.bigText else event.text,
                                lastExecutionTimestamp = System.currentTimeMillis()
                            )
                        )
                        continue
                    }
                    CollisionStrategy.START_NEW -> {
                        // Allow parallel task creation
                    }
                }
            }

            // 5. Execute Actions
            actionExecutor.executeActions(automation, actions, event)
        }
    }

    companion object {
        private const val TAG = "AutoPulseEngine"
    }
}
