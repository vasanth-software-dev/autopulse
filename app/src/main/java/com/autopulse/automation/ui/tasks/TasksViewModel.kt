package com.autopulse.automation.ui.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autopulse.automation.AutoPulseApplication
import com.autopulse.automation.data.model.RepeatingTask
import com.autopulse.automation.service.AutomationExecutionService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TasksViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AutoPulseApplication
    private val taskRepo = app.repeatingTaskRepository
    private val logRepo = app.executionLogRepository

    val allTasks: StateFlow<List<RepeatingTask>> = taskRepo.allTasks
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun stopTask(taskId: String) {
        viewModelScope.launch {
            // Cancel foreground service job & alarms
            AutomationExecutionService.stopTask(app, taskId)

            val task = taskRepo.getTaskByIdDirect(taskId)
            taskRepo.stopTask(taskId)
            logRepo.logInfo(
                message = "User manually pressed STOP for repeating task.",
                automationId = task?.automationId,
                automationName = task?.automationName,
                taskId = taskId
            )
        }
    }

    fun stopAllTasks() {
        viewModelScope.launch {
            // Stop foreground service completely
            AutomationExecutionService.stopAll(app)

            taskRepo.stopAllTasks()
            logRepo.logWarn(message = "User stopped all active repeating tasks.")
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            taskRepo.clearStoppedTasks()
        }
    }
}
