package com.autopulse.automation.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "variables",
    indices = [Index(value = ["key"], unique = true)]
)
data class Variable(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val value: String,
    val description: String = "",
    val isSystem: Boolean = false
)
