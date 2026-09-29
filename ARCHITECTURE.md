# AutoPulse Architecture & Engineering Specification

This document details the internal design and interaction contracts across all 10 architectural layers of AutoPulse.

---

## The 10 Core Architectural Layers

### 1. UI Layer (`com.autopulse.automation.ui`)
* **Technology**: Jetpack Compose, Material 3, Navigation Compose.
* **Pattern**: MVVM (Model-View-ViewModel).
* **Screens**:
  * `DashboardScreen`: System health, running task count, quick controls, and simulated lead triggers.
  * `AutomationsScreen`: Configured rules list, instant enable/disable toggles, edit and delete actions.
  * `BuilderScreen`: Visual rule creator with presets, app scanning, condition operators, variable chips.
  * `TasksScreen`: Live 30s repeating task monitors with prominent **[ STOP ]** buttons.
  * `LogsScreen`: Monospace console with level filter chips (`INFO`, `SUCCESS`, `WARN`, `ERROR`), keyword search, and clipboard export.
  * `TelegramSettingsScreen`: Secure credential input with live `getMe` and `sendMessage` diagnostics.
  * `PermissionsScreen`: Deep-linking permission toggles and automated system health audits.

### 2. Automation Engine (`com.autopulse.automation.engine.AutomationEngine`)
* Coordinates reactive events from the `NotificationEventBus`.
* Loads enabled rules from Room database.
* Delegates source verification to `TriggerManager`.
* Delegates rule condition evaluation to `ConditionManager`.
* Resolves duplicate collisions (`IGNORE`, `RESTART`, `UPDATE_PAYLOAD`, `START_NEW`).
* Dispatches execution requests to `ActionExecutor`.

### 3. Trigger Manager (`com.autopulse.automation.engine.TriggerManager`)
* Verifies source package name and application label.
* Supports 4 match modes: `EXACT`, `CONTAINS`, `REGEX`, and `ANY`.
* Handles case-insensitive package and app label matching.

### 4. Condition Manager (`com.autopulse.automation.engine.ConditionManager`)
* Evaluates conditions against target fields: `NOTIFICATION_TITLE`, `NOTIFICATION_TEXT`, `PACKAGE_NAME`, `APP_NAME`, or `ANY`.
* Evaluates operators: `TEXT_CONTAINS`, `EXACT_MATCH`, `REGEX_MATCH`, `STARTS_WITH`, `ENDS_WITH`.
* Supports inverted logic (`isNegated = true`) and case-sensitivity switches.
* Enforces short-circuit AND logic across multiple conditions.

### 5. Action Executor (`com.autopulse.automation.engine.ActionExecutor`)
* Executes sorted, ordered action arrays:
  * `START_REPEAT`: Inserts a `RepeatingTask` into Room and signals `AutomationExecutionService`.
  * `SEND_TELEGRAM`: Interpolates template variables and dispatches payload via `TelegramBotClient`.
  * `STOP_REPEAT`: Stops running tasks for the associated automation ID.
  * `LOG_MESSAGE`: Writes custom message to execution logs.

### 6. Scheduler & Foreground Service (`com.autopulse.automation.service.AutomationExecutionService`)
* Runs as an Android Foreground Service with `foregroundServiceType="dataSync"`.
* Holds a partial `WakeLock` to prevent the device from entering deep sleep during intervals.
* Maintains the 30-second execution loop using `delay(30_000ms)` coupled with an `AlarmManager.setExactAndAllowWhileIdle()` fallback.
* Posts and updates the persistent notification with the live execution count, last run time, and **[ STOP ]** action.

### 7. Notification Listener (`com.autopulse.automation.service.AutoPulseNotificationListenerService`)
* Extends Android's `NotificationListenerService`.
* Intercepts `StatusBarNotification` events directly without polling.
* **Self-Loop Filter**: Filters out AutoPulse notifications to prevent feedback loops.
* **Deduplication**: 1500ms sliding debounce window on notification fingerprints.
* Dispatches domain `NotificationEvent` instances into `NotificationEventBus`.

### 8. Telegram Client (`com.autopulse.automation.telegram.TelegramBotClient`)
* OkHttp client with connection pooling and timeouts (15s connect, 20s read).
* Dispatches messages to `https://api.telegram.org/bot<TOKEN>/sendMessage`.
* Automatically retries transient network errors with exponential backoff (`attempt * 1500ms`).
* Parses error codes: `400` (Bad Request), `401` (Invalid Token), `403` (Blocked), `404` (Chat Not Found), `429` (Rate Limited with `retry_after`).
* Sanitizes input via `TelegramMessageFormatter.escapeHtml()` to prevent syntax errors.

### 9. Room Database (`com.autopulse.automation.data.db.AutoPulseDatabase`)
* SQLite abstraction using Android Room 2.6.1.
* Tables:
  * `automations`: Master rule definitions and collision strategies.
  * `triggers`: Cascade-deleted trigger entities.
  * `conditions`: Cascade-deleted rule condition entities.
  * `actions`: Ordered action definitions with JSON payloads.
  * `repeating_tasks`: Active background task tracking with timestamps, counts, and status.
  * `execution_logs`: Historical audit trail with automated 1000-entry trimming.
  * `variables`: Dynamic key-value store.
* Atomic transaction support via `saveFullAutomation()`.

### 10. Execution Logger (`com.autopulse.automation.data.repository.ExecutionLogRepository`)
* Centralized, thread-safe asynchronous logging.
* Automatically strips bot tokens using regex redaction (`BOT_TOKEN_REDACTED`).
* Categorizes events into `INFO`, `SUCCESS`, `WARN`, and `ERROR`.
* Reactive `Flow<List<ExecutionLog>>` stream for live UI console updates.

---

## 🛑 Stop System State Diagram

```
[ Active Repeating Task (RUNNING) ]
               │
               ▼
     User Clicks [ STOP ]
  (From Notification OR UI)
               │
               ├─────────────────────────────────────────────┐
               ▼                                             ▼
  1. Room DB State Update                      2. Foreground Service
  - Status marked STOPPED                      - Cancel active Coroutine Job
  - Error message cleared                      - Cancel Exact Alarm
                                               - Release WakeLock if last task
                                               - Stop Foreground Notification
                                                             │
                                                             ▼
                                                3. Zero Additional Telegram
                                                   Notifications Dispatched!
```

---

## 🔄 Crash & Reboot Recovery

* **Reboot**: Android broadcasts `ACTION_BOOT_COMPLETED` ➔ `BootReceiver` queries Room DB for any tasks with `status == RUNNING` ➔ Launches `AutomationExecutionService` to resume execution without data loss.
* **Uncaught Exceptions**: `AutoPulseCrashHandler` intercepts any thread panic, records the exception stack trace to `ExecutionLogRepository`, and flushes DB before terminating.
