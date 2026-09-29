package com.autopulse.automation.engine

import com.autopulse.automation.data.model.MatchType
import com.autopulse.automation.data.model.Trigger
import com.autopulse.automation.data.model.TriggerType
import com.autopulse.automation.event.NotificationEvent

data class TriggerMatchResult(
    val isMatch: Boolean,
    val reason: String
)

object TriggerManager {

    /**
     * Evaluates if a NotificationEvent satisfies the criteria defined in the Trigger.
     */
    fun matches(trigger: Trigger, event: NotificationEvent): TriggerMatchResult {
        if (trigger.type != TriggerType.NOTIFICATION_RECEIVED) {
            return TriggerMatchResult(false, "Trigger type is not NOTIFICATION_RECEIVED (${trigger.type})")
        }

        // Check if trigger matches any application
        val targetPackage = trigger.packageName?.trim().orEmpty()
        val targetApp = trigger.appName?.trim().orEmpty()

        if (targetPackage.isBlank() && targetApp.isBlank()) {
            return TriggerMatchResult(true, "Trigger matches ANY application.")
        }

        // Match against Package Name or App Label
        return when (trigger.matchType) {
            MatchType.ANY -> {
                TriggerMatchResult(true, "Trigger configured to match ANY application.")
            }
            MatchType.EXACT -> {
                val packageMatches = targetPackage.isNotBlank() && event.packageName.equals(targetPackage, ignoreCase = true)
                val appMatches = targetApp.isNotBlank() && event.appName.equals(targetApp, ignoreCase = true)

                if (packageMatches || appMatches) {
                    TriggerMatchResult(true, "Exact match on application: ${event.appName} (${event.packageName})")
                } else {
                    TriggerMatchResult(false, "Exact match failed. Expected: '$targetPackage' / '$targetApp', Got: '${event.packageName}' / '${event.appName}'")
                }
            }
            MatchType.CONTAINS -> {
                val packageContains = targetPackage.isNotBlank() && event.packageName.contains(targetPackage, ignoreCase = true)
                val appContains = targetApp.isNotBlank() && event.appName.contains(targetApp, ignoreCase = true)

                if (packageContains || appContains) {
                    TriggerMatchResult(true, "Contains match on application: ${event.appName} (${event.packageName})")
                } else {
                    TriggerMatchResult(false, "Contains match failed for '$targetPackage' / '$targetApp'")
                }
            }
            MatchType.REGEX -> {
                try {
                    val pattern = if (targetPackage.isNotBlank()) targetPackage else targetApp
                    val regex = Regex(pattern, RegexOption.IGNORE_CASE)
                    val matches = regex.containsMatchIn(event.packageName) || regex.containsMatchIn(event.appName)

                    if (matches) {
                        TriggerMatchResult(true, "Regex matched on application: pattern '$pattern'")
                    } else {
                        TriggerMatchResult(false, "Regex did not match: pattern '$pattern'")
                    }
                } catch (e: Exception) {
                    TriggerMatchResult(false, "Invalid regex pattern: ${e.localizedMessage}")
                }
            }
        }
    }
}
