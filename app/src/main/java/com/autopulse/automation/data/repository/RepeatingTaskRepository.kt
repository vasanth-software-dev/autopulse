package com.autopulse.automation.data.repository

import com.autopulse.automation.data.db.RepeatingTaskDao
import com.autopulse.automation.data.model.RepeatingTask
import com.autopulse.automation.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

class RepeatingTaskRepository(private val dao: RepeatingTaskDao) {

    val allTasks: Flow<List<RepeatingTask>> = dao.getAllTasks()
    val runningTasks: Flow<List<RepeatingTask>> = dao.getRunningTasks()
    val runningCount: Flow<Int> = dao.getRunningTasksCount()

    fun getTaskById(taskId: String): Flow<RepeatingTask?> = dao.getTaskById(taskId)

    suspend fun getTaskByIdDirect(taskId: String): RepeatingTask? = dao.getTaskByIdDirect(taskId)

    suspend fun getRunningTasksList(): List<RepeatingTask> = dao.getRunningTasksList()

    suspend fun getRunningTaskForAutomation(automationId: Long): RepeatingTask? =
        dao.getRunningTaskForAutomation(automationId)

    suspend fun insertOrUpdateTask(task: RepeatingTask) {
        dao.insertTask(task)
    }

    suspend fun stopTask(taskId: String) {
        dao.stopTask(taskId)
    }

    suspend fun stopAllTasks() {
        dao.stopAllRunningTasks()
    }

    suspend fun recordTick(taskId: String, lastExecution: Long, nextExecution: Long) {
        dao.recordExecutionTick(taskId, lastExecution, nextExecution)
    }

    suspend fun markFailed(taskId: String, errorMessage: String) {
        dao.updateTaskStatus(taskId, TaskStatus.FAILED, errorMessage)
    }

    suspend fun deleteTask(taskId: String) {
        dao.deleteTaskById(taskId)
    }

    suspend fun clearStoppedTasks() {
        dao.clearStoppedTasks()
    }
}
