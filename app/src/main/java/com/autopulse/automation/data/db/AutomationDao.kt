package com.autopulse.automation.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.autopulse.automation.data.model.Action
import com.autopulse.automation.data.model.Automation
import com.autopulse.automation.data.model.AutomationWithRules
import com.autopulse.automation.data.model.Condition
import com.autopulse.automation.data.model.Trigger
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationDao {

    @Transaction
    @Query("SELECT * FROM automations ORDER BY updatedAt DESC")
    fun getAllAutomationsWithRules(): Flow<List<AutomationWithRules>>

    @Transaction
    @Query("SELECT * FROM automations WHERE isEnabled = 1")
    fun getEnabledAutomationsWithRules(): Flow<List<AutomationWithRules>>

    @Transaction
    @Query("SELECT * FROM automations WHERE isEnabled = 1")
    suspend fun getEnabledAutomationsList(): List<AutomationWithRules>

    @Transaction
    @Query("SELECT * FROM automations")
    suspend fun getAllAutomationsList(): List<AutomationWithRules>

    @Transaction
    @Query("SELECT * FROM automations WHERE id = :id")
    fun getAutomationWithRulesById(id: Long): Flow<AutomationWithRules?>

    @Transaction
    @Query("SELECT * FROM automations WHERE id = :id")
    suspend fun getAutomationWithRulesByIdDirect(id: Long): AutomationWithRules?

    @Query("SELECT COUNT(*) FROM automations")
    fun getAutomationsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM automations WHERE isEnabled = 1")
    fun getEnabledAutomationsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomation(automation: Automation): Long

    @Update
    suspend fun updateAutomation(automation: Automation)

    @Query("UPDATE automations SET isEnabled = :isEnabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setEnabled(id: Long, isEnabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteAutomation(automation: Automation)

    @Query("DELETE FROM automations WHERE id = :id")
    suspend fun deleteAutomationById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTriggers(triggers: List<Trigger>)

    @Query("DELETE FROM triggers WHERE automationId = :automationId")
    suspend fun deleteTriggersForAutomation(automationId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConditions(conditions: List<Condition>)

    @Query("DELETE FROM conditions WHERE automationId = :automationId")
    suspend fun deleteConditionsForAutomation(automationId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActions(actions: List<Action>)

    @Query("DELETE FROM actions WHERE automationId = :automationId")
    suspend fun deleteActionsForAutomation(automationId: Long)

    @Transaction
    suspend fun saveFullAutomation(
        automation: Automation,
        triggers: List<Trigger>,
        conditions: List<Condition>,
        actions: List<Action>
    ): Long {
        val automationId = if (automation.id == 0L) {
            insertAutomation(automation)
        } else {
            updateAutomation(automation.copy(updatedAt = System.currentTimeMillis()))
            deleteTriggersForAutomation(automation.id)
            deleteConditionsForAutomation(automation.id)
            deleteActionsForAutomation(automation.id)
            automation.id
        }

        insertTriggers(triggers.map { it.copy(automationId = automationId) })
        insertConditions(conditions.map { it.copy(automationId = automationId) })
        insertActions(actions.map { it.copy(automationId = automationId) })

        return automationId
    }
}
