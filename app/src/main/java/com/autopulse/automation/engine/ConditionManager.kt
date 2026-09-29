package com.autopulse.automation.engine

import com.autopulse.automation.data.model.Condition
import com.autopulse.automation.data.model.ConditionType
import com.autopulse.automation.data.model.FieldToMatch
import com.autopulse.automation.event.NotificationEvent

data class ConditionEvaluationResult(
    val allPassed: Boolean,
    val matchedCount: Int,
    val totalCount: Int,
    val evaluationDetails: List<String>
)

object ConditionManager {

    /**
     * Evaluates a list of conditions against a NotificationEvent.
     * An empty condition list always passes.
     */
    fun evaluate(conditions: List<Condition>, event: NotificationEvent): ConditionEvaluationResult {
        if (conditions.isEmpty()) {
            return ConditionEvaluationResult(
                allPassed = true,
                matchedCount = 0,
                totalCount = 0,
                evaluationDetails = listOf("No conditions defined. Passed by default.")
            )
        }

        val details = mutableListOf<String>()
        var passedCount = 0

        for ((index, condition) in conditions.withIndex()) {
            val fieldText = extractFieldText(condition.fieldToMatch, event)
            val conditionValue = condition.value
            val ignoreCase = !condition.isCaseSensitive

            var conditionPassed = when (condition.type) {
                ConditionType.TEXT_CONTAINS -> {
                    fieldText.contains(conditionValue, ignoreCase = ignoreCase)
                }
                ConditionType.EXACT_MATCH -> {
                    fieldText.equals(conditionValue, ignoreCase = ignoreCase)
                }
                ConditionType.REGEX_MATCH -> {
                    try {
                        val options = if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
                        val regex = Regex(conditionValue, options)
                        regex.containsMatchIn(fieldText)
                    } catch (e: Exception) {
                        false
                    }
                }
                ConditionType.STARTS_WITH -> {
                    fieldText.startsWith(conditionValue, ignoreCase = ignoreCase)
                }
                ConditionType.ENDS_WITH -> {
                    fieldText.endsWith(conditionValue, ignoreCase = ignoreCase)
                }
            }

            // Apply negation if condition is inverted (e.g. "Does NOT contain")
            if (condition.isNegated) {
                conditionPassed = !conditionPassed
            }

            val prefix = if (condition.isNegated) "NOT " else ""
            val status = if (conditionPassed) "PASSED" else "FAILED"
            val detail = "Condition #${index + 1} [$status]: Field '${condition.fieldToMatch}' $prefix${condition.type} '$conditionValue' (Target: '${fieldText.take(30)}')"
            details.add(detail)

            if (conditionPassed) {
                passedCount++
            } else {
                // Short-circuit: All conditions must pass
                return ConditionEvaluationResult(
                    allPassed = false,
                    matchedCount = passedCount,
                    totalCount = conditions.size,
                    evaluationDetails = details
                )
            }
        }

        return ConditionEvaluationResult(
            allPassed = true,
            matchedCount = passedCount,
            totalCount = conditions.size,
            evaluationDetails = details
        )
    }

    private fun extractFieldText(field: FieldToMatch, event: NotificationEvent): String {
        return when (field) {
            FieldToMatch.NOTIFICATION_TITLE -> event.title
            FieldToMatch.NOTIFICATION_TEXT -> {
                if (event.bigText.isNotBlank()) event.bigText else event.text
            }
            FieldToMatch.PACKAGE_NAME -> event.packageName
            FieldToMatch.APP_NAME -> event.appName
            FieldToMatch.ANY -> event.combinedContent
        }
    }
}
