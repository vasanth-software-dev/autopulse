# AutoPulse — Native Android Automation Engine

A high-performance, original Android automation engine inspired by modern rule-based event systems. AutoPulse empowers you to build deterministic automation rules such as:

> **WHEN** an OLX lead/notification is received on **Phone 1**  
> ➔ Detect the notification & extract title/message  
> ➔ Start a background repeating task  
> ➔ Every **30 seconds** send a rich alert to **Telegram on Phone 2**  
> ➔ Continue repeating until manually stopped via the **STOP** button  

Phone 2 only requires standard Telegram to receive the continuous alerts.

---

## 🏗️ Architecture Overview

AutoPulse strictly decouples UI from execution logic via clean 10-layer architecture:

```
Android System Event (Notification Posted)
           │
           ▼
[NotificationListenerService] ────► [NotificationExtractor]
                                            │
                                            ▼
                                   [NotificationEventBus]
                                            │
                                            ▼
                                   [AutomationEngine]
                                            │
                      ┌─────────────────────┴─────────────────────┐
                      ▼                                           ▼
              [TriggerManager]                            [ConditionManager]
         (Package / App / Regex)                      (Text / Regex / Negation)
                      │                                           │
                      └─────────────────────┬─────────────────────┘
                                            ▼
                                     [CollisionCheck]
                            (IGNORE / RESTART / UPDATE)
                                            │
                                            ▼
                                     [ActionExecutor]
                                            │
                      ┌─────────────────────┴─────────────────────┐
                      ▼                                           ▼
         [AutomationExecutionService]                    [TelegramBotClient]
          (Foreground 30s Repeat Loop)                  (OkHttp + TLS 1.3 + HTML)
                      │                                           │
                      ▼                                           ▼
             Persistent Notification                      Telegram Servers
                 with [ STOP ]                           (api.telegram.org)
```

---

## 📱 Technology Stack

* **Language**: Kotlin 2.0.20
* **IDE & Tooling**: Android Studio (AGP 8.5.2, Gradle 8.7, targetSdk 35, minSdk 26)
* **UI**: Jetpack Compose + Material 3 (Consolidated Dark Automation Console Theme)
* **Database**: Room Database 2.6.1 with KSP (Transactional CRUD, cascade foreign keys, relations)
* **Async & Concurrency**: Kotlin Coroutines & SharedFlow Event Bus
* **Interception**: Native `NotificationListenerService` (zero polling, zero battery drain)
* **Continuous Repeat**: `AutomationExecutionService` (Foreground service with `dataSync`, partial `WakeLock`, and Doze fallback alarms)
* **Networking**: OkHttp 4.12.0 with exponential retry backoff & rate limit (`429`) parsing
* **Security**: `EncryptedSharedPreferences` (AES-256 GCM) with automatic token redaction in logs

---

## 🚀 Step-by-Step Setup Guide

### 1. Telegram Setup (Phone 2)
1. Open Telegram on Phone 2 (or desktop) and search for `@BotFather`.
2. Send `/newbot`, choose a name and username for your bot.
3. Copy the **HTTP API Bot Token** provided by BotFather.
4. Start a chat with your new bot and send `/start`.
5. Retrieve your Chat ID:
   * Message `@userinfobot` or forward any message to `@userinfobot` to get your numeric ID (e.g. `123456789`).
   * For private groups/channels, add the bot as admin and use the channel ID (e.g. `-100123456789`).

### 2. AutoPulse Configuration (Phone 1)
1. Open AutoPulse on Phone 1.
2. Navigate to **Telegram Settings** (paper plane icon in top bar):
   * Paste your **Bot Token** and **Chat ID**.
   * Click **Save Credentials Securely**.
   * Click **Test Connection** (`getMe`) and **Send Test** (`sendMessage`) to confirm Phone 2 receives the test message.
3. Navigate to **Permissions** (shield icon in top bar):
   * **Notification Access**: Click **ENABLE** and toggle AutoPulse ON in Android Settings.
   * **Post Notifications**: Click **ENABLE** to allow ongoing foreground task controls.
   * **Battery Optimization**: Click **ALLOW** to exempt AutoPulse from Android Doze mode.
   * Click **Run System Health Audit** to verify all green checks.

---

## ⚡ The OLX 30-Second Repeat Cycle

AutoPulse includes the pre-configured **OLX Lead Alert** template out of the box:

```json
{
  "name": "OLX Lead Alert",
  "enabled": true,
  "collisionStrategy": "IGNORE",
  "trigger": {
    "type": "NOTIFICATION_RECEIVED",
    "packageName": "com.olx.southasia",
    "appName": "OLX"
  },
  "conditions": [
    {
      "type": "TEXT_CONTAINS",
      "fieldToMatch": "ANY",
      "value": "lead"
    }
  ],
  "actions": [
    {
      "type": "START_REPEAT",
      "intervalSeconds": 30,
      "untilStopped": true
    },
    {
      "type": "SEND_TELEGRAM",
      "messageTemplate": "🔥 <b>NEW OLX LEAD</b>\n\n<b>Title:</b> {{notification_title}}\n<b>Message:</b> {{notification_text}}\n<b>Time:</b> {{timestamp}}"
    }
  ]
}
```

### How It Operates:
1. When OLX posts an inquiry/lead notification on Phone 1, `AutoPulseNotificationListenerService` captures the event.
2. `AutomationEngine` matches the rule and conditions.
3. `ActionExecutor` creates an active task in Room and launches `AutomationExecutionService`.
4. Phone 1 immediately sends the 1st Telegram message.
5. Phone 1 displays a persistent notification:
   `AutoPulse Running: OLX Lead Alert • Sent: 1 • [ STOP ]`
6. Every 30 seconds, Phone 1 wakes via CPU WakeLock and dispatches another Telegram alert.
7. **To STOP the repeating alerts**:
   * Click the **STOP** button directly on the Android notification, OR
   * Open AutoPulse, navigate to **Running Tasks**, and click **STOP**.
8. The task state updates to `STOPPED` in Room, the foreground loop terminates, and **no further Telegram messages are sent**.

---

## 🧪 Testing Without OLX

You do **not** need the OLX app installed to test the complete automation pipeline:
1. Open AutoPulse Dashboard.
2. In the **Quick Controls** section, click **Simulate Mock OLX Lead (Test Event)**.
3. AutoPulse dispatches a realistic simulated OLX lead notification into the event bus.
4. Watch the engine match the rule, start the 30s repeater, and verify the Telegram message arrives on Phone 2!

---

## 🔒 Security & Privacy

* **Zero Hard-Coded Credentials**: Secrets are never hardcoded in source code or Git.
* **Hardware-Backed Encryption**: Tokens are stored using Android Keystore AES-256 GCM (`EncryptedSharedPreferences`).
* **Secret Redaction**: All logs automatically mask bot tokens using regex filtering (`BOT_TOKEN_REDACTED`).
* **Cloud Backup Exclusion**: Sensitive XML preferences are explicitly excluded in `backup_rules.xml` and `data_extraction_rules.xml`.
