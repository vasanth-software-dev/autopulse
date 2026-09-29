package com.autopulse.automation.error

import android.content.Context
import android.util.Log
import com.autopulse.automation.data.repository.ExecutionLogRepository
import java.lang.Thread.UncaughtExceptionHandler

class AutoPulseCrashHandler(
    private val context: Context,
    private val logRepository: ExecutionLogRepository,
    private val defaultHandler: UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
) : UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Log.e("AutoPulseCrashHandler", "Uncaught exception in thread ${thread.name}", throwable)

        // Persist crash details directly to SharedPreferences with synchronous commit()
        // This ensures the diagnostic info is recorded before process termination
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_MESSAGE, "${throwable.javaClass.simpleName}: ${throwable.localizedMessage ?: "No message"}")
                .putString(KEY_STACKTRACE, throwable.stackTraceToString())
                .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                .commit()
        } catch (t: Throwable) {
            Log.e("AutoPulseCrashHandler", "Failed to persist crash info to preferences", t)
        }

        defaultHandler?.uncaughtException(thread, throwable)
    }

    companion object {
        const val PREFS_NAME = "autopulse_crash_report"
        const val KEY_MESSAGE = "last_crash_message"
        const val KEY_STACKTRACE = "last_crash_stacktrace"
        const val KEY_TIMESTAMP = "last_crash_timestamp"

        fun install(context: Context, logRepository: ExecutionLogRepository) {
            val current = Thread.getDefaultUncaughtExceptionHandler()
            if (current !is AutoPulseCrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(
                    AutoPulseCrashHandler(context.applicationContext, logRepository, current)
                )
            }
        }

        fun getLastCrash(context: Context): String? {
            return try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val msg = prefs.getString(KEY_MESSAGE, null)
                val stack = prefs.getString(KEY_STACKTRACE, null)
                if (msg != null && stack != null) {
                    "$msg\n\n$stack"
                } else null
            } catch (t: Throwable) {
                null
            }
        }

        fun clearLastCrash(context: Context) {
            try {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
            } catch (_: Throwable) {}
        }
    }
}
