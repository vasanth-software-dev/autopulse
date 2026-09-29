package com.autopulse.automation.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conditions",
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
data class Condition(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val automationId: Long,
    val type: ConditionType = ConditionType.TEXT_CONTAINS,
    val fieldToMatch: FieldToMatch = FieldToMatch.ANY,
    val value: String = "",
    val isNegated: Boolean = false,
    val isCaseSensitive: Boolean = false
)
