package com.autopulse.automation.telegram

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TelegramBotClientTest {

    private val client = TelegramBotClient()

    @Test
    fun testEmptyTokenReturnsFailureImmediately() = runTest {
        val result = client.sendMessage(
            botToken = "",
            chatId = "123456",
            text = "Test message"
        )

        assertTrue(result is TelegramResult.Failure)
        val failure = result as TelegramResult.Failure
        assertEquals(401, failure.errorCode)
        assertFalse(failure.isRetryable)
    }

    @Test
    fun testEmptyChatIdReturnsFailureImmediately() = runTest {
        val result = client.sendMessage(
            botToken = "123456:ABC-DEF",
            chatId = "   ",
            text = "Test message"
        )

        assertTrue(result is TelegramResult.Failure)
        val failure = result as TelegramResult.Failure
        assertEquals(400, failure.errorCode)
        assertFalse(failure.isRetryable)
    }

    @Test
    fun testEmptyTokenInGetMeReturnsFailure() = runTest {
        val result = client.getMe("")
        assertTrue(result is TelegramResult.Failure)
        val failure = result as TelegramResult.Failure
        assertEquals(401, failure.errorCode)
    }
}
