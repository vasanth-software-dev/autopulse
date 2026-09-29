package com.autopulse.automation.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "execution_logs",
    indices = [
        Index("timestamp"),
        Index("automationId"),
        Index("taskId")
    ]
)
data class ExecutionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val automationId: Long? = null,
    val automationName: String? = null,
    val taskId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val message: String,
    val detailsJson: String? = null
)
