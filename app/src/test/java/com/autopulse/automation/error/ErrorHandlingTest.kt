package com.autopulse.automation.error

import com.autopulse.automation.data.db.ExecutionLogDao
import com.autopulse.automation.data.model.ExecutionLog
import com.autopulse.automation.data.model.LogLevel
import com.autopulse.automation.data.repository.ExecutionLogRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ErrorHandlingTest {

    private val logDao: ExecutionLogDao = mockk(relaxed = true)
    private val logRepo = ExecutionLogRepository(logDao)

    @Test
    fun testSecretRedactionInLogRepository() = runTest {
        val capturedLog = slot<ExecutionLog>()
        coEvery { logDao.insertLog(capture(capturedLog)) } returns 1L

        val sensitiveMessage = "Attempted Telegram call with bot token 7123456789:AAHfk3x_abcdef1234567890ABCDEF12345 to chat 987654"
        logRepo.logInfo(sensitiveMessage)

        val savedLog = capturedLog.captured
        assertFalse(savedLog.message.contains("7123456789:AAHfk3x_abcdef1234567890ABCDEF12345"))
        assertTrue(savedLog.message.contains("BOT_TOKEN_REDACTED"))
    }

    @Test
    fun testAutoPulseErrorTypes() {
        val noInternet = AutoPulseError.NoInternet
        assertEquals("NET_001", noInternet.code)

        val invalidToken = AutoPulseError.InvalidBotToken
        assertEquals("TG_401", invalidToken.code)

        val rateLimit = AutoPulseError.RateLimited(retryAfterSeconds = 45)
        assertEquals("TG_429", rateLimit.code)
        assertTrue(rateLimit.recoverySuggestion.contains("45 seconds"))

        val collision = AutoPulseError.CollisionIgnored("OLX Alert", "task-123")
        assertEquals("EXEC_COLLISION", collision.code)
        assertTrue(collision.message.contains("OLX Alert"))
    }

    @Test
    fun testLogFilterLogic() {
        val logs = listOf(
            ExecutionLog(id = 1, level = LogLevel.INFO, message = "System started"),
            ExecutionLog(id = 2, level = LogLevel.ERROR, message = "Telegram delivery failed"),
            ExecutionLog(id = 3, level = LogLevel.SUCCESS, message = "Telegram message sent (Msg ID: 101)", automationName = "OLX Lead Alert"),
            ExecutionLog(id = 4, level = LogLevel.WARN, message = "Collision detected")
        )

        val errorsOnly = logs.filter { it.level == LogLevel.ERROR }
        assertEquals(1, errorsOnly.size)
        assertEquals(2L, errorsOnly[0].id)

        val searchOlx = logs.filter { it.automationName?.contains("OLX", ignoreCase = true) == true }
        assertEquals(1, searchOlx.size)
        assertEquals(3L, searchOlx[0].id)
    }
}
