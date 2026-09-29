package com.autopulse.automation.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class AutomationWithRules(
    @Embedded
    val automation: Automation,

    @Relation(
        parentColumn = "id",
        entityColumn = "automationId"
    )
    val triggers: List<Trigger>,

    @Relation(
        parentColumn = "id",
        entityColumn = "automationId"
    )
    val conditions: List<Condition>,

    @Relation(
        parentColumn = "id",
        entityColumn = "automationId"
    )
    val actions: List<Action>
)
