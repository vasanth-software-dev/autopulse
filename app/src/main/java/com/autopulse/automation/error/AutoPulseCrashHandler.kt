package com.autopulse.automation.error

import android.util.Log
import com.autopulse.automation.data.repository.ExecutionLogRepository
import kotlinx.coroutines.runBlocking
import java.lang.Thread.UncaughtExceptionHandler

class AutoPulseCrashHandler(
    private val logRepository: ExecutionLogRepository,
    private val defaultHandler: UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
) : UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Log.e("AutoPulseCrashHandler", "Uncaught exception in thread ${thread.name}", throwable)

        try {
            runBlocking {
                logRepository.logError(
                    message = "CRASH: Uncaught exception in thread '${thread.name}': ${throwable.localizedMessage ?: "Unknown"}",
                    details = throwable.stackTraceToString().take(1000)
                )
            }
        } catch (_: Exception) {
            // Best effort logging during fatal crash
        }

        defaultHandler?.uncaughtException(thread, throwable)
    }

    companion object {
        fun install(logRepository: ExecutionLogRepository) {
            val current = Thread.getDefaultUncaughtExceptionHandler()
            if (current !is AutoPulseCrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(AutoPulseCrashHandler(logRepository, current))
            }
        }
    }
}
