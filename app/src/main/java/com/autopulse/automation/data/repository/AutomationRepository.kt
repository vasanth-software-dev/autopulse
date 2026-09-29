package com.autopulse.automation.data.repository

import com.autopulse.automation.data.db.AutomationDao
import com.autopulse.automation.data.model.Action
import com.autopulse.automation.data.model.Automation
import com.autopulse.automation.data.model.AutomationWithRules
import com.autopulse.automation.data.model.Condition
import com.autopulse.automation.data.model.Trigger
import kotlinx.coroutines.flow.Flow

class AutomationRepository(private val dao: AutomationDao) {

    val allAutomations: Flow<List<AutomationWithRules>> = dao.getAllAutomationsWithRules()
    val enabledAutomations: Flow<List<AutomationWithRules>> = dao.getEnabledAutomationsWithRules()
    val totalCount: Flow<Int> = dao.getAutomationsCount()
    val enabledCount: Flow<Int> = dao.getEnabledAutomationsCount()

    fun getAutomationById(id: Long): Flow<AutomationWithRules?> = dao.getAutomationWithRulesById(id)

    suspend fun getAutomationByIdDirect(id: Long): AutomationWithRules? = dao.getAutomationWithRulesByIdDirect(id)

    suspend fun getEnabledAutomationsList(): List<AutomationWithRules> = dao.getEnabledAutomationsList()

    suspend fun setEnabled(id: Long, isEnabled: Boolean) {
        dao.setEnabled(id, isEnabled)
    }

    suspend fun saveAutomation(
        automation: Automation,
        triggers: List<Trigger>,
        conditions: List<Condition>,
        actions: List<Action>
    ): Long {
        return dao.saveFullAutomation(automation, triggers, conditions, actions)
    }

    suspend fun deleteAutomation(id: Long) {
        dao.deleteAutomationById(id)
    }
}
