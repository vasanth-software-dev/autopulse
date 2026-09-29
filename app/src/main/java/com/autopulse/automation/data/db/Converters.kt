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
    fun toTriggerType(value: String?): TriggerType? = value?.let { TriggerType.valueOf(it) }

    @TypeConverter
    fun fromMatchType(value: MatchType?): String? = value?.name

    @TypeConverter
    fun toMatchType(value: String?): MatchType? = value?.let { MatchType.valueOf(it) }

    @TypeConverter
    fun fromConditionType(value: ConditionType?): String? = value?.name

    @TypeConverter
    fun toConditionType(value: String?): ConditionType? = value?.let { ConditionType.valueOf(it) }

    @TypeConverter
    fun fromFieldToMatch(value: FieldToMatch?): String? = value?.name

    @TypeConverter
    fun toFieldToMatch(value: String?): FieldToMatch? = value?.let { FieldToMatch.valueOf(it) }

    @TypeConverter
    fun fromActionType(value: ActionType?): String? = value?.name

    @TypeConverter
    fun toActionType(value: String?): ActionType? = value?.let { ActionType.valueOf(it) }

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus?): String? = value?.name

    @TypeConverter
    fun toTaskStatus(value: String?): TaskStatus? = value?.let { TaskStatus.valueOf(it) }

    @TypeConverter
    fun fromLogLevel(value: LogLevel?): String? = value?.name

    @TypeConverter
    fun toLogLevel(value: String?): LogLevel? = value?.let { LogLevel.valueOf(it) }

    @TypeConverter
    fun fromCollisionStrategy(value: CollisionStrategy?): String? = value?.name

    @TypeConverter
    fun toCollisionStrategy(value: String?): CollisionStrategy? = value?.let { CollisionStrategy.valueOf(it) }
}
