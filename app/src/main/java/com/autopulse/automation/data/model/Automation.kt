package com.autopulse.automation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "automations")
data class Automation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val isEnabled: Boolean = true,
    val collisionStrategy: CollisionStrategy = CollisionStrategy.IGNORE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
