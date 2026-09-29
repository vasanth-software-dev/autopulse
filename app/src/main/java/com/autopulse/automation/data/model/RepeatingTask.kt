package com.autopulse.automation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "repeating_tasks")
data class RepeatingTask(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val automationId: Long,
    val automationName: String,
    val status: TaskStatus = TaskStatus.RUNNING,
    val intervalSeconds: Long = 30L,
    val startTimestamp: Long = System.currentTimeMillis(),
    val lastExecutionTimestamp: Long = 0L,
    val nextExecutionTimestamp: Long = System.currentTimeMillis(),
    val executionCount: Int = 0,
    val leadTitle: String = "",
    val leadText: String = "",
    val leadPackage: String = "",
    val leadAppName: String = "",
    val customMessageTemplate: String? = null,
    val errorMessage: String? = null
)
