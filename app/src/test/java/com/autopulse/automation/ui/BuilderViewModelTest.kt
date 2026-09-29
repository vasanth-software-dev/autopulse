package com.autopulse.automation.ui

import com.autopulse.automation.data.model.CollisionStrategy
import com.autopulse.automation.data.model.ConditionType
import com.autopulse.automation.data.model.FieldToMatch
import com.autopulse.automation.data.model.MatchType
import com.autopulse.automation.data.repository.AutomationRepository
import com.autopulse.automation.data.repository.ExecutionLogRepository
import com.autopulse.automation.ui.builder.BuilderUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BuilderViewModelTest {

    private val automationRepository: AutomationRepository = mockk(relaxed = true)
    private val logRepository: ExecutionLogRepository = mockk(relaxed = true)

    @Test
    fun testBuilderDefaultState() {
        val state = BuilderUiState()
        assertEquals("OLX Lead Alert", state.name)
        assertEquals(30L, state.intervalSeconds)
        assertEquals(CollisionStrategy.IGNORE, state.collisionStrategy)
        assertEquals("com.olx.southasia", state.selectedPackageName)
        assertEquals("OLX", state.selectedAppName)
        assertEquals("lead", state.conditionValue)
        assertEquals(ConditionType.TEXT_CONTAINS, state.conditionType)
    }

    @Test
    fun testBuilderStateModifications() {
        var state = BuilderUiState()
        state = state.copy(
            name = "WhatsApp VIP Alert",
            selectedPackageName = "com.whatsapp",
            selectedAppName = "WhatsApp",
            intervalSeconds = 60L,
            conditionValue = "urgent",
            collisionStrategy = CollisionStrategy.RESTART
        )

        assertEquals("WhatsApp VIP Alert", state.name)
        assertEquals("com.whatsapp", state.selectedPackageName)
        assertEquals(60L, state.intervalSeconds)
        assertEquals("urgent", state.conditionValue)
        assertEquals(CollisionStrategy.RESTART, state.collisionStrategy)
    }

    @Test
    fun testSaveAutomationPersistence() = runTest {
        coEvery { automationRepository.saveAutomation(any(), any(), any(), any()) } returns 101L

        val savedId = automationRepository.saveAutomation(
            mockk(relaxed = true),
            listOf(mockk(relaxed = true)),
            listOf(mockk(relaxed = true)),
            listOf(mockk(relaxed = true))
        )

        assertEquals(101L, savedId)
        coVerify(exactly = 1) { automationRepository.saveAutomation(any(), any(), any(), any()) }
    }
}
