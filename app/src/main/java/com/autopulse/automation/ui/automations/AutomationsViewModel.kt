package com.autopulse.automation.ui.automations

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autopulse.automation.AutoPulseApplication
import com.autopulse.automation.data.model.AutomationWithRules
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AutomationsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AutoPulseApplication
    private val automationRepo = app.automationRepository
    private val logRepo = app.executionLogRepository

    val automations: StateFlow<List<AutomationWithRules>> = automationRepo.allAutomations
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun toggleEnabled(id: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            automationRepo.setEnabled(id, isEnabled)
            logRepo.logInfo(
                message = "Automation ${if (isEnabled) "enabled" else "disabled"}.",
                automationId = id
            )
        }
    }

    fun deleteAutomation(id: Long) {
        viewModelScope.launch {
            val auto = automationRepo.getAutomationByIdDirect(id)
            val name = auto?.automation?.name ?: "ID: $id"
            automationRepo.deleteAutomation(id)
            logRepo.logWarn(
                message = "Deleted automation '$name'.",
                automationId = id
            )
        }
    }
}
