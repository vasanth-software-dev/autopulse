package com.autopulse.automation.ui.builder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autopulse.automation.AutoPulseApplication
import com.autopulse.automation.data.model.Action
import com.autopulse.automation.data.model.ActionType
import com.autopulse.automation.data.model.Automation
import com.autopulse.automation.data.model.CollisionStrategy
import com.autopulse.automation.data.model.Condition
import com.autopulse.automation.data.model.ConditionType
import com.autopulse.automation.data.model.FieldToMatch
import com.autopulse.automation.data.model.MatchType
import com.autopulse.automation.data.model.Trigger
import com.autopulse.automation.data.model.TriggerType
import com.autopulse.automation.telegram.TelegramMessageFormatter
import com.autopulse.automation.util.AppInfo
import com.autopulse.automation.util.AppInfoProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

data class BuilderUiState(
    val automationId: Long = 0L,
    val name: String = "OLX Message Alert",
    val description: String = "Repeats Telegram alert every 5s when a new message arrives until stopped.",
    val collisionStrategy: CollisionStrategy = CollisionStrategy.IGNORE,

    // Trigger
    val triggerType: TriggerType = TriggerType.NOTIFICATION_RECEIVED,
    val selectedPackageName: String = "olx",
    val selectedAppName: String = "OLX",
    val matchType: MatchType = MatchType.CONTAINS,

    // Condition
    val conditionType: ConditionType = ConditionType.REGEX_MATCH,
    val conditionField: FieldToMatch = FieldToMatch.ANY,
    val conditionValue: String = "new messages|missed updates",
    val isConditionNegated: Boolean = false,
    val isConditionCaseSensitive: Boolean = false,

    // Actions
    val intervalSeconds: Long = 5L,
    val repeatUntilStopped: Boolean = true,
    val messageTemplate: String = TelegramMessageFormatter.DEFAULT_LEAD_TEMPLATE_HTML,

    // Available Apps
    val installedApps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class BuilderViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AutoPulseApplication
    private val automationRepo = app.automationRepository
    private val taskRepo = app.repeatingTaskRepository
    private val logRepo = app.executionLogRepository

    private val _uiState = MutableStateFlow(BuilderUiState())
    val uiState: StateFlow<BuilderUiState> = _uiState.asStateFlow()

    init {
        loadInstalledApps()
    }

    fun loadAutomation(id: Long) {
        if (id <= 0L) {
            // New automation: initialize with sensible OLX template defaults
            _uiState.value = BuilderUiState(installedApps = _uiState.value.installedApps)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val rule = automationRepo.getAutomationByIdDirect(id)
            if (rule != null) {
                val auto = rule.automation
                val trigger = rule.triggers.firstOrNull()
                val condition = rule.conditions.firstOrNull()
                val repeatAction = rule.actions.firstOrNull { it.type == ActionType.START_REPEAT }
                val telegramAction = rule.actions.firstOrNull { it.type == ActionType.SEND_TELEGRAM }

                val interval = try {
                    JSONObject(repeatAction?.payloadJson ?: "{}").optLong("intervalSeconds", 30L)
                } catch (_: Exception) {
                    30L
                }

                val template = try {
                    val json = JSONObject(telegramAction?.payloadJson ?: "{}")
                    json.optString("messageTemplate", TelegramMessageFormatter.DEFAULT_LEAD_TEMPLATE_HTML)
                } catch (_: Exception) {
                    TelegramMessageFormatter.DEFAULT_LEAD_TEMPLATE_HTML
                }

                _uiState.value = _uiState.value.copy(
                    automationId = auto.id,
                    name = auto.name,
                    description = auto.description,
                    collisionStrategy = auto.collisionStrategy,
                    triggerType = trigger?.type ?: TriggerType.NOTIFICATION_RECEIVED,
                    selectedPackageName = trigger?.packageName.orEmpty(),
                    selectedAppName = trigger?.appName.orEmpty(),
                    matchType = trigger?.matchType ?: MatchType.CONTAINS,
                    conditionType = condition?.type ?: ConditionType.TEXT_CONTAINS,
                    conditionField = condition?.fieldToMatch ?: FieldToMatch.ANY,
                    conditionValue = condition?.value.orEmpty(),
                    isConditionNegated = condition?.isNegated ?: false,
                    isConditionCaseSensitive = condition?.isCaseSensitive ?: false,
                    intervalSeconds = interval,
                    messageTemplate = template,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Automation not found.")
            }
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = AppInfoProvider.getInstalledApps(getApplication())
            _uiState.value = _uiState.value.copy(installedApps = apps)
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateCollisionStrategy(strategy: CollisionStrategy) {
        _uiState.value = _uiState.value.copy(collisionStrategy = strategy)
    }

    fun updateApp(packageName: String, appName: String) {
        _uiState.value = _uiState.value.copy(
            selectedPackageName = packageName,
            selectedAppName = appName
        )
    }

    fun updateMatchType(matchType: MatchType) {
        _uiState.value = _uiState.value.copy(matchType = matchType)
    }

    fun updateConditionField(field: FieldToMatch) {
        _uiState.value = _uiState.value.copy(conditionField = field)
    }

    fun updateConditionType(type: ConditionType) {
        _uiState.value = _uiState.value.copy(conditionType = type)
    }

    fun updateConditionValue(value: String) {
        _uiState.value = _uiState.value.copy(conditionValue = value)
    }

    fun toggleConditionNegated() {
        _uiState.value = _uiState.value.copy(isConditionNegated = !_uiState.value.isConditionNegated)
    }

    fun toggleConditionCaseSensitive() {
        _uiState.value = _uiState.value.copy(isConditionCaseSensitive = !_uiState.value.isConditionCaseSensitive)
    }

    fun updateIntervalSeconds(seconds: Long) {
        val safeSeconds = seconds.coerceAtLeast(1L)
        _uiState.value = _uiState.value.copy(intervalSeconds = safeSeconds)
    }

    fun updateMessageTemplate(template: String) {
        _uiState.value = _uiState.value.copy(messageTemplate = template)
    }

    fun insertVariable(token: String) {
        val current = _uiState.value.messageTemplate
        _uiState.value = _uiState.value.copy(messageTemplate = "$current {{$token}}")
    }

    fun saveAutomation(onComplete: () -> Unit) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter an automation name.")
            return
        }

        viewModelScope.launch {
            val automation = Automation(
                id = state.automationId,
                name = state.name.trim(),
                description = state.description.trim(),
                isEnabled = true,
                collisionStrategy = state.collisionStrategy,
                updatedAt = System.currentTimeMillis()
            )

            val trigger = Trigger(
                automationId = state.automationId,
                type = state.triggerType,
                packageName = state.selectedPackageName.ifBlank { null },
                appName = state.selectedAppName.ifBlank { null },
                matchType = state.matchType
            )

            val conditions = if (state.conditionValue.isNotBlank()) {
                listOf(
                    Condition(
                        automationId = state.automationId,
                        type = state.conditionType,
                        fieldToMatch = state.conditionField,
                        value = state.conditionValue.trim(),
                        isNegated = state.isConditionNegated,
                        isCaseSensitive = state.isConditionCaseSensitive
                    )
                )
            } else {
                emptyList()
            }

            val repeatPayload = JSONObject().apply {
                put("intervalSeconds", state.intervalSeconds)
                put("untilStopped", state.repeatUntilStopped)
            }.toString()

            val telegramPayload = JSONObject().apply {
                put("messageTemplate", state.messageTemplate)
            }.toString()

            val actions = listOf(
                Action(
                    automationId = state.automationId,
                    orderIndex = 0,
                    type = ActionType.START_REPEAT,
                    payloadJson = repeatPayload
                ),
                Action(
                    automationId = state.automationId,
                    orderIndex = 1,
                    type = ActionType.SEND_TELEGRAM,
                    payloadJson = telegramPayload
                )
            )

            val savedId = automationRepo.saveAutomation(automation, listOf(trigger), conditions, actions)

            // Immediately reflect updated interval on any currently running task for this automation
            try {
                taskRepo.updateRunningTaskIntervalForAutomation(savedId, state.intervalSeconds)
            } catch (_: Exception) {}

            logRepo.logSuccess(
                message = "Automation '${state.name}' saved (Interval: ${state.intervalSeconds}s).",
                automationId = savedId
            )

            _uiState.value = state.copy(isSaved = true)
            onComplete()
        }
    }
}
