package com.autopulse.automation.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "triggers",
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
data class Trigger(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val automationId: Long,
    val type: TriggerType = TriggerType.NOTIFICATION_RECEIVED,
    val packageName: String? = null,
    val appName: String? = null,
    val matchType: MatchType = MatchType.CONTAINS,
    val extraJson: String = "{}"
)
