# Architecture

Technical reference for contributors and anyone curious about how DietReport is built.

---

## Overview

DietReport is a fully offline Android app. There is no backend, no network calls, and no user accounts. All data — meal logs, photos, settings — lives on the device.

```
┌─────────────────────────────────────────────┐
│                  UI Layer                   │
│   Jetpack Compose screens + ViewModels      │
├─────────────────────────────────────────────┤
│               Domain / Logic                │
│   SlotComputer · SettingsValidator          │
│   ReportMetrics · ReminderScheduler         │
├──────────────────────┬──────────────────────┤
│      Room DB         │    DataStore         │
│  MealLog · Reminder  │  SettingsRepository  │
│  Slot entities       │  (preferences)       │
├──────────────────────┴──────────────────────┤
│         Android Platform Services           │
│  AlarmManager · CameraX · NotificationMgr  │
└─────────────────────────────────────────────┘
```

---

## Tech Stack

| Area | Technology | Version |
|---|---|---|
| Language | Kotlin | 2.0.21 |
| UI | Jetpack Compose + Material Design 3 | Compose BOM 2024.09.00 |
| Architecture pattern | MVVM, single Activity | — |
| Navigation | Navigation Compose | 2.7.7 |
| Database | Room | — |
| Preferences | DataStore Preferences | — |
| Scheduling | AlarmManager (exact alarms) | — |
| Camera | CameraX | — |
| Min SDK | Android 15 | API 35 |
| Target SDK | Android 16 | API 36 |

---

## Package Structure

```
com.diet.dietreport/
├── MainActivity.kt               — single Activity, NavHost, bottom nav
├── AppError.kt                   — sealed class for all app-level errors
├── SchedulerErrorBus.kt          — SharedFlow for permission-error events
│
├── ui/theme/                     — Material 3 theme (Color, Type, Theme)
│
├── data/db/
│   ├── AppDatabase.kt            — Room database (v2+)
│   ├── MealLog.kt                — entity: one log per reminder slot
│   ├── MealLogDao.kt             — queries: insert, get by date/slot
│   ├── ReminderSlot.kt           — entity: scheduled alarm record
│   └── ReminderSlotDao.kt        — queries: insert, get today's slots
│
├── settings/
│   ├── data/
│   │   ├── SettingsRepository.kt — DataStore read/write wrapper
│   │   └── settingsDataStore.kt  — DataStore instance (application-scoped)
│   ├── SettingsScreen.kt
│   ├── SettingsViewModel.kt
│   ├── SettingsViewModelFactory.kt
│   └── SettingsValidator.kt      — pure validation functions
│
├── reminders/
│   ├── SlotComputer.kt           — pure: compute reminder times from schedule
│   ├── ReminderScheduler.kt      — schedules/cancels AlarmManager exact alarms
│   ├── NotificationHelper.kt     — creates notification channel; builds notifications
│   ├── ReminderReceiver.kt       — fired by AlarmManager; shows notification
│   ├── SnoozeReceiver.kt         — handles "Snooze" notification action
│   ├── BootReceiver.kt           — reschedules alarms after device reboot
│   ├── DailyRescheduleReceiver.kt— midnight receiver; rebuilds next day's slots
│   └── TimezoneChangeReceiver.kt — reschedules on timezone change
│
├── meals/
│   ├── HomeScreen.kt
│   ├── HomeViewModel.kt
│   ├── HomeViewModelFactory.kt
│   ├── LogMealScreen.kt          — CameraX preview + gallery picker
│   ├── LogMealViewModel.kt
│   ├── LogMealViewModelFactory.kt
│   ├── PhotoGalleryScreen.kt     — grid of all logged meal photos
│   ├── PhotoGalleryViewModel.kt
│   └── PhotoGalleryViewModelFactory.kt
│
├── lock/
│   ├── LockScreen.kt             — fullscreen 10-minute countdown
│   ├── LockViewModel.kt
│   └── LockViewModelFactory.kt
│
└── reports/
    ├── ReportScreen.kt
    ├── ReportViewModel.kt
    ├── ReportViewModelFactory.kt
    └── ReportMetrics.kt          — pure: compute stats from MealLog list
```

---

## Navigation

Single `NavHost` in `MainActivity`. All screens are Compose destinations.

```
HOME ──tap slot──► LOG_MEAL ──confirm──► LOCK ──done──► HOME
  │                   │                                   ▲
  │              ──retake──► (stays on LOG_MEAL)          │
  │                                                       │
  └──(first launch, no onboarding)──► SETTINGS ──save──►─┘

Bottom nav: HOME ↔ GALLERY ↔ SETTINGS ↔ REPORT
```

Routes are defined as string constants in `Routes` object in `MainActivity.kt`. Deep-link from notification `"Log meal"` action navigates directly to `LOG_MEAL/{slotId}`.

---

## Data Layer

### Room database

Two entities:

**`ReminderSlot`** — one row per scheduled alarm. Created when `ReminderScheduler` fires alarms each day.

| Column | Type | Notes |
|---|---|---|
| `id` | Long (PK, autoincrement) | — |
| `scheduledAt` | Long | Epoch millis of the alarm |
| `date` | String | `yyyy-MM-dd` for efficient day queries |

**`MealLog`** — one row per completed (or expired) logging attempt.

| Column | Type | Notes |
|---|---|---|
| `id` | Long (PK, autoincrement) | — |
| `slotId` | Long | FK → ReminderSlot.id (0 for extra meals) |
| `photoPath` | String | Absolute path to the saved photo file |
| `loggedAt` | Long | Epoch millis when the user tapped Confirm |
| `success` | Boolean | True if logged within the 30-minute window |
| `date` | String | `yyyy-MM-dd` for day queries |

### DataStore

User preferences stored as typed key-value pairs via DataStore Preferences. `SettingsRepository` exposes them as `Flow<T>` properties:

- `wakeTimeMinutes: Flow<Int>` — minutes since midnight
- `bedtimeMinutes: Flow<Int>` — minutes since midnight
- `firstMealDelayMinutes: Flow<Int>`
- `ringtone: Flow<String>`
- `activityMonitorEnabled: Flow<Boolean>`
- `isOnboardingComplete: Flow<Boolean>`

---

## Reminder Scheduling

`SlotComputer.computeSlots()` is a **pure function** — it takes wake time, delay, and bedtime in epoch millis and returns a list of `SlotTime` objects. No Android dependencies, fully unit-testable.

`ReminderScheduler` wraps `AlarmManager.setExactAndAllowWhileIdle()` and schedules one alarm per `SlotTime`. It also writes a `ReminderSlot` row to Room for each alarm so the Home screen can display it.

Scheduling is triggered from:
- `SettingsViewModel` on first save and on every subsequent save
- `BootReceiver` after device reboot
- `DailyRescheduleReceiver` at midnight each day
- `TimezoneChangeReceiver` on `ACTION_TIMEZONE_CHANGED`

---

## Error Handling

Errors are modeled as sealed subclasses of `AppError`:

```kotlin
sealed class AppError {
    data class SchedulerError(val message: String) : AppError()
    // extend here as needed
}
```

`SchedulerErrorBus` is an application-scoped `SharedFlow`. Components post errors to it; `MainActivity` collects it and propagates to ViewModels or shows banners.

Permission errors (exact alarm revoked, notifications disabled) are detected in `MainActivity.onResume()` and emitted on the bus so the Home screen can display a warning banner.

---

## Testing

| Layer | Tool | Location |
|---|---|---|
| Unit tests | JUnit 4 | `app/src/test/` |
| Instrumented / E2E | Compose Test + UI Automator | `app/src/androidTest/` |

Pure logic classes (`SlotComputer`, `SettingsValidator`, `ReportMetrics`) are covered by unit tests. Screen flows are covered by instrumented tests that launch `MainActivity` and drive the UI with UI Automator selectors.

Log tags follow the convention `DR/<Module>` (e.g. `DR/Reminder`, `DR/Meals`).

---

## Conventions

- **Log tags:** `DR/<Module>` — `d` for normal flow, `w` for recoverable issues, `e` with a `Throwable` for failures.
- **ProGuard:** `Log.d` calls are stripped in release builds via `-assumenosideeffects` in `proguard-rules.pro`.
- **minSdk 35:** no `Build.VERSION.SDK_INT` guards are needed anywhere in the codebase.
- **Kotlin Compose plugin:** bundled with Kotlin 2.0+ — do not add a separate `kotlinCompilerExtensionVersion`.
