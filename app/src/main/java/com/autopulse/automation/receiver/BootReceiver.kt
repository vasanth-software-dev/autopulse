package com.autopulse.automation.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.autopulse.automation.AutoPulseApplication
import com.autopulse.automation.service.AutomationExecutionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (Intent.ACTION_BOOT_COMPLETED == action || Intent.ACTION_MY_PACKAGE_REPLACED == action) {
            Log.d(TAG, "Device booted or app replaced ($action). Inspecting tasks for recovery...")

            val app = context.applicationContext as? AutoPulseApplication ?: return
            val taskRepo = app.repeatingTaskRepository
            val logRepo = app.executionLogRepository

            CoroutineScope(Dispatchers.IO).launch {
                val runningTasks = taskRepo.getRunningTasksList()
                if (runningTasks.isNotEmpty()) {
                    logRepo.logInfo("Boot completed: Restoring ${runningTasks.size} active repeating task(s).")

                    val serviceIntent = Intent(context, AutomationExecutionService::class.java).apply {
                        this.action = AutomationExecutionService.ACTION_RECOVER_RUNNING_TASKS
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "AutoPulseBootReceiver"
    }
}
