package com.autopulse.automation.data.model

enum class TriggerType {
    NOTIFICATION_RECEIVED,
    TIMER_TICK,
    MANUAL
}

enum class MatchType {
    EXACT,
    CONTAINS,
    REGEX,
    ANY
}

enum class ConditionType {
    TEXT_CONTAINS,
    EXACT_MATCH,
    REGEX_MATCH,
    STARTS_WITH,
    ENDS_WITH
}

enum class FieldToMatch {
    NOTIFICATION_TITLE,
    NOTIFICATION_TEXT,
    PACKAGE_NAME,
    APP_NAME,
    ANY
}

enum class ActionType {
    START_REPEAT,
    SEND_TELEGRAM,
    STOP_REPEAT,
    LOG_MESSAGE
}

enum class TaskStatus {
    RUNNING,
    STOPPED,
    FAILED,
    COMPLETED
}

enum class LogLevel {
    INFO,
    SUCCESS,
    WARN,
    ERROR
}

enum class CollisionStrategy {
    IGNORE,
    RESTART,
    START_NEW,
    UPDATE_PAYLOAD
}
