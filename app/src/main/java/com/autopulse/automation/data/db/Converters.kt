package com.autopulse.automation.data.db

import androidx.room.TypeConverter
import com.autopulse.automation.data.model.ActionType
import com.autopulse.automation.data.model.CollisionStrategy
import com.autopulse.automation.data.model.ConditionType
import com.autopulse.automation.data.model.FieldToMatch
import com.autopulse.automation.data.model.LogLevel
import com.autopulse.automation.data.model.MatchType
import com.autopulse.automation.data.model.TaskStatus
import com.autopulse.automation.data.model.TriggerType

class Converters {
    @TypeConverter
    fun fromTriggerType(value: TriggerType?): String? = value?.name

    @TypeConverter
    fun toTriggerType(value: String?): TriggerType? = value?.let { runCatching { TriggerType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromMatchType(value: MatchType?): String? = value?.name

    @TypeConverter
    fun toMatchType(value: String?): MatchType? = value?.let { runCatching { MatchType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromConditionType(value: ConditionType?): String? = value?.name

    @TypeConverter
    fun toConditionType(value: String?): ConditionType? = value?.let { runCatching { ConditionType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromFieldToMatch(value: FieldToMatch?): String? = value?.name

    @TypeConverter
    fun toFieldToMatch(value: String?): FieldToMatch? = value?.let { runCatching { FieldToMatch.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromActionType(value: ActionType?): String? = value?.name

    @TypeConverter
    fun toActionType(value: String?): ActionType? = value?.let { runCatching { ActionType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus?): String? = value?.name

    @TypeConverter
    fun toTaskStatus(value: String?): TaskStatus? = value?.let { runCatching { TaskStatus.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromLogLevel(value: LogLevel?): String? = value?.name

    @TypeConverter
    fun toLogLevel(value: String?): LogLevel? = value?.let { runCatching { LogLevel.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromCollisionStrategy(value: CollisionStrategy?): String? = value?.name

    @TypeConverter
    fun toCollisionStrategy(value: String?): CollisionStrategy? = value?.let { runCatching { CollisionStrategy.valueOf(it) }.getOrNull() }
}
