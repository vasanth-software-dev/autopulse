package com.autopulse.automation.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "actions",
    foreignKeys = [
        ForeignKey(
            entity = Automation::class,
            parentColumns = ["id"],
            childColumns = ["automationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("automationId")]
)
data class Action(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val automationId: Long,
    val orderIndex: Int = 0,
    val type: ActionType,
    val payloadJson: String = "{}"
)
