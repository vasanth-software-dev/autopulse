package com.autopulse.automation.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.autopulse.automation.data.model.Action
import com.autopulse.automation.data.model.ActionType
import com.autopulse.automation.data.model.Automation
import com.autopulse.automation.data.model.CollisionStrategy
import com.autopulse.automation.data.model.Condition
import com.autopulse.automation.data.model.ConditionType
import com.autopulse.automation.data.model.ExecutionLog
import com.autopulse.automation.data.model.FieldToMatch
import com.autopulse.automation.data.model.LogLevel
import com.autopulse.automation.data.model.MatchType
import com.autopulse.automation.data.model.RepeatingTask
import com.autopulse.automation.data.model.Trigger
import com.autopulse.automation.data.model.TriggerType
import com.autopulse.automation.data.model.Variable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Automation::class,
        Trigger::class,
        Condition::class,
        Action::class,
        RepeatingTask::class,
        ExecutionLog::class,
        Variable::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AutoPulseDatabase : RoomDatabase() {

    abstract fun automationDao(): AutomationDao
    abstract fun repeatingTaskDao(): RepeatingTaskDao
    abstract fun executionLogDao(): ExecutionLogDao
    abstract fun variableDao(): VariableDao

    companion object {
        @Volatile
        private var INSTANCE: AutoPulseDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AutoPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AutoPulseDatabase::class.java,
                    "autopulse_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(AutoPulseDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AutoPulseDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialTemplates(database)
                    }
                }
            }

            private suspend fun populateInitialTemplates(db: AutoPulseDatabase) {
                val automationDao = db.automationDao()
                val logDao = db.executionLogDao()

                // Default Template: OLX Lead Alert
                val olxAutomation = Automation(
                    name = "OLX Lead Alert",
                    description = "Detects OLX buyer leads and repeats Telegram alerts every 30 seconds until stopped.",
                    isEnabled = true,
                    collisionStrategy = CollisionStrategy.IGNORE,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val triggers = listOf(
                    Trigger(
                        automationId = 0,
                        type = TriggerType.NOTIFICATION_RECEIVED,
                        packageName = "com.olx.southasia",
                        appName = "OLX",
                        matchType = MatchType.CONTAINS
                    )
                )

                val conditions = listOf(
                    Condition(
                        automationId = 0,
                        type = ConditionType.TEXT_CONTAINS,
                        fieldToMatch = FieldToMatch.ANY,
                        value = "lead",
                        isNegated = false,
                        isCaseSensitive = false
                    )
                )

                val actions = listOf(
                    Action(
                        automationId = 0,
                        orderIndex = 0,
                        type = ActionType.START_REPEAT,
                        payloadJson = """{"intervalSeconds":30,"untilStopped":true}"""
                    ),
                    Action(
                        automationId = 0,
                        orderIndex = 1,
                        type = ActionType.SEND_TELEGRAM,
                        payloadJson = """{"messageTemplate":"🔥 NEW OLX LEAD\n\nApp: {{app_name}}\nTitle: {{notification_title}}\nMessage: {{notification_text}}\nTime: {{timestamp}}"}"""
                    )
                )

                val createdId = automationDao.saveFullAutomation(olxAutomation, triggers, conditions, actions)

                logDao.insertLog(
                    ExecutionLog(
                        automationId = createdId,
                        automationName = "OLX Lead Alert",
                        level = LogLevel.INFO,
                        message = "System initialized with default 'OLX Lead Alert' automation template."
                    )
                )
            }
        }
    }
}
