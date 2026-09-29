package com.autopulse.automation.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.autopulse.automation.data.model.ExecutionLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutionLogDao {

    @Query("SELECT * FROM execution_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 300): Flow<List<ExecutionLog>>

    @Query("SELECT * FROM execution_logs WHERE automationId = :automationId ORDER BY timestamp DESC LIMIT :limit")
    fun getLogsForAutomation(automationId: Long, limit: Int = 200): Flow<List<ExecutionLog>>

    @Query("SELECT * FROM execution_logs WHERE taskId = :taskId ORDER BY timestamp DESC")
    fun getLogsForTask(taskId: String): Flow<List<ExecutionLog>>

    @Query("SELECT * FROM execution_logs ORDER BY timestamp DESC LIMIT 1")
    fun getLastLog(): Flow<ExecutionLog?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ExecutionLog): Long

    @Query("DELETE FROM execution_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM execution_logs WHERE id NOT IN (SELECT id FROM execution_logs ORDER BY timestamp DESC LIMIT :keepLimit)")
    suspend fun trimLogs(keepLimit: Int = 1000)
}
