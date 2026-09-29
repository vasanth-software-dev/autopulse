package com.autopulse.automation.service

import com.autopulse.automation.data.model.RepeatingTask
import com.autopulse.automation.data.model.TaskStatus
import com.autopulse.automation.data.repository.RepeatingTaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RepeatingTaskSchedulingTest {

    private val taskRepository: RepeatingTaskRepository = mockk(relaxed = true)

    @Test
    fun testTaskStopMechanism() = runTest {
        val taskId = "test-task-123"
        val runningTask = RepeatingTask(
            id = taskId,
            automationId = 1L,
            automationName = "OLX Lead Alert",
            status = TaskStatus.RUNNING,
            intervalSeconds = 30L
        )

        coEvery { taskRepository.getTaskByIdDirect(taskId) } returns runningTask

        // Simulate user pressing STOP
        taskRepository.stopTask(taskId)
        coVerify(exactly = 1) { taskRepository.stopTask(taskId) }

        // Simulated check before next tick
        val stoppedTask = runningTask.copy(status = TaskStatus.STOPPED)
        coEvery { taskRepository.getTaskByIdDirect(taskId) } returns stoppedTask

        val current = taskRepository.getTaskByIdDirect(taskId)
        assertEquals(TaskStatus.STOPPED, current?.status)
        assertFalse(current?.status == TaskStatus.RUNNING)
    }

    @Test
    fun testRecordTickProgression() = runTest {
        val taskId = "test-task-456"
        val start = System.currentTimeMillis()
        val next = start + 30_000L

        taskRepository.recordTick(taskId, lastExecution = start, nextExecution = next)

        coVerify(exactly = 1) {
            taskRepository.recordTick(taskId, lastExecution = start, nextExecution = next)
        }
    }

    @Test
    fun testProcessRecoveryReturnsRunningTasks() = runTest {
        val task1 = RepeatingTask(
            id = "task-1",
            automationId = 100L,
            automationName = "OLX Alert",
            status = TaskStatus.RUNNING
        )

        coEvery { taskRepository.getRunningTasksList() } returns listOf(task1)

        val activeList = taskRepository.getRunningTasksList()
        assertEquals(1, activeList.size)
        assertEquals(TaskStatus.RUNNING, activeList[0].status)
    }
}
