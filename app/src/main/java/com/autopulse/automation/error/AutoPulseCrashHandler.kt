package com.autopulse.automation.error

import android.content.Context
import android.content.Intent
import android.util.Log
import java.lang.Thread.UncaughtExceptionHandler

class AutoPulseCrashHandler(
    private val context: Context,
    private val defaultHandler: UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
) : UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Log.e("AutoPulseCrashHandler", "Uncaught exception in thread ${thread.name}", throwable)

        val crashDetails = "${throwable.javaClass.name}: ${throwable.localizedMessage ?: "No message"}\n\n${throwable.stackTraceToString()}"

        // Persist crash details directly to SharedPreferences with synchronous commit()
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

        // Launch CrashActivity to display diagnostic details and prevent silent auto-close
        try {
            val intent = Intent(context, CrashActivity::class.java).apply {
                putExtra(CrashActivity.EXTRA_CRASH_INFO, crashDetails)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(10)
        } catch (t: Throwable) {
            Log.e("AutoPulseCrashHandler", "Failed to start CrashActivity", t)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        const val PREFS_NAME = "autopulse_crash_report"
        const val KEY_MESSAGE = "last_crash_message"
        const val KEY_STACKTRACE = "last_crash_stacktrace"
        const val KEY_TIMESTAMP = "last_crash_timestamp"

        fun install(context: Context) {
            val current = Thread.getDefaultUncaughtExceptionHandler()
            if (current !is AutoPulseCrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(
                    AutoPulseCrashHandler(context.applicationContext, current)
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

