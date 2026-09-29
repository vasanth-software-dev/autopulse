package com.autopulse.automation.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.autopulse.automation.data.model.RepeatingTask
import com.autopulse.automation.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RepeatingTaskDao {

    @Query("SELECT * FROM repeating_tasks ORDER BY startTimestamp DESC")
    fun getAllTasks(): Flow<List<RepeatingTask>>

    @Query("SELECT * FROM repeating_tasks WHERE status = 'RUNNING' ORDER BY startTimestamp DESC")
    fun getRunningTasks(): Flow<List<RepeatingTask>>

    @Query("SELECT * FROM repeating_tasks WHERE status = 'RUNNING'")
    suspend fun getRunningTasksList(): List<RepeatingTask>

    @Query("SELECT COUNT(*) FROM repeating_tasks WHERE status = 'RUNNING'")
    fun getRunningTasksCount(): Flow<Int>

    @Query("SELECT * FROM repeating_tasks WHERE id = :id")
    fun getTaskById(id: String): Flow<RepeatingTask?>

    @Query("SELECT * FROM repeating_tasks WHERE id = :id")
    suspend fun getTaskByIdDirect(id: String): RepeatingTask?

    @Query("SELECT * FROM repeating_tasks WHERE automationId = :automationId AND status = 'RUNNING' LIMIT 1")
    suspend fun getRunningTaskForAutomation(automationId: Long): RepeatingTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: RepeatingTask)

    @Update
    suspend fun updateTask(task: RepeatingTask)

    @Query("UPDATE repeating_tasks SET status = :status, errorMessage = :errorMessage WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus, errorMessage: String? = null)

    @Query("""
        UPDATE repeating_tasks 
        SET lastExecutionTimestamp = :lastExecution, 
            nextExecutionTimestamp = :nextExecution, 
            executionCount = executionCount + 1 
        WHERE id = :taskId
    """)
    suspend fun recordExecutionTick(taskId: String, lastExecution: Long, nextExecution: Long)

    @Query("UPDATE repeating_tasks SET status = 'STOPPED' WHERE id = :taskId")
    suspend fun stopTask(taskId: String)

    @Query("UPDATE repeating_tasks SET status = 'STOPPED' WHERE status = 'RUNNING'")
    suspend fun stopAllRunningTasks()

    @Query("UPDATE repeating_tasks SET intervalSeconds = :intervalSeconds WHERE automationId = :automationId AND status = 'RUNNING'")
    suspend fun updateRunningTaskIntervalForAutomation(automationId: Long, intervalSeconds: Long)

    @Delete
    suspend fun deleteTask(task: RepeatingTask)

    @Query("DELETE FROM repeating_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM repeating_tasks WHERE status = 'STOPPED' OR status = 'COMPLETED'")
    suspend fun clearStoppedTasks()
}
