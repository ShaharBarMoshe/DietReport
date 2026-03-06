# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug APK on connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Run a single unit test class
./gradlew test --tests "com.diet.dietreport.ExampleUnitTest"

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Clean build
./gradlew clean
```

## Workflow

- **Always read `plan.md` before implementing** — it defines phases, data models, and done-when criteria
- Each phase must have its E2E test passing before starting the next
- **Always ask the user for explicit permission before starting each new phase** — summarize what the phase will do and wait for confirmation before writing any code
- Use the `mobile-android-design` skill for any UI/Compose work

## Skills

When designing or implementing UI, use the **`mobile-android-design`** skill located at `.agents/skills/mobile-android-design/SKILL.md`. It covers Material Design 3, Jetpack Compose patterns, theming, navigation, and adaptive layouts.

## Architecture

This is an Android app (Kotlin 2.0.21, minSdk 35, targetSdk 36) using **Jetpack Compose** with **Material Design 3**.

- **Single Activity** (`MainActivity`) — sets up `DietReportTheme` and edge-to-edge; all UI is Compose
- **Compose Navigation** — `NavHost` + `NavController` replace the placeholder Scaffold as phases are completed
- **MVVM** — ViewModels per screen; state flows down, events flow up
- **Theme** lives in `ui/theme/` (`Theme.kt`, `Color.kt`, `Type.kt`); dynamic color is always on (minSdk 35 guarantees Android 12+ support)

**Current state:** pre-Phase 1 — `MainActivity` shows a placeholder `Scaffold` with no `NavHost` yet. No Room, DataStore, WorkManager, CameraX, or Credentials dependencies added yet.

**Target package structure** (populated phase by phase):
```
com.diet.dietreport/
├── MainActivity.kt
├── ui/theme/
├── auth/          (SignInScreen, AuthViewModel)
├── settings/      (SettingsScreen, SettingsViewModel, data/SettingsRepository)
├── reminders/     (ReminderScheduler, ReminderReceiver, BootReceiver, data/ReminderRepository)
├── meals/         (HomeScreen, HomeViewModel, LogMealScreen, LogMealViewModel, data/MealRepository)
├── reports/       (ReportScreen, ReportViewModel)
└── data/db/       (Room AppDatabase, DAOs, entities: ReminderSlot, MealLog, UsageDay)
```

## Key Files

- `app/src/main/java/com/diet/dietreport/` — all Kotlin source
- `app/src/main/java/com/diet/dietreport/ui/theme/` — Material 3 theme (Color, Type, Theme)
- `gradle/libs.versions.toml` — version catalog for all dependencies
- `app/build.gradle.kts` — app-level build config (SDK versions, Compose enabled)
- `plan.md` — full feature plan and implementation order

## Conventions

- Log tags: `DR/<Module>` (e.g. `DR/Auth`, `DR/Reminder`, `DR/Meals`) — `d` for flow, `w` for recoverable, `e` with throwable
- Error handling: map failures to sealed `AppError` subclasses; surface via `uiState.error`; never swallow silently
- minSdk 35 — no `Build.VERSION.SDK_INT` guards needed anywhere

## Testing

```bash
# Run a single instrumented test class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.diet.dietreport.NavigationShellTest
```

- E2E tests: Compose Test + UI Automator, live in `app/src/androidTest/`
- Unit tests for pure logic (validators, slot computation, report metrics): `app/src/test/`

## Gotchas

- `kotlin-compose` plugin is bundled with Kotlin 2.0+ — do NOT add a separate `kotlinCompilerExtensionVersion`
- ProGuard strips `Log.d` in release via `-assumenosideeffects class android.util.Log { public static int d(...); }` — add to `proguard-rules.pro` if missing
- Android Studio default "Android" view hides project-root files (CLAUDE.md, plan.md) — switch to "Project Files" view

## Dependencies

Managed via `gradle/libs.versions.toml` version catalog:
- Compose BOM 2024.09.00 (ui, material3, ui-tooling)
- Navigation Compose 2.7.7
- Activity Compose 1.9.2
- Compose compiler plugin via `kotlin-compose` (bundled with Kotlin 2.0.21 — no separate version needed)

**To add in Phase 1:** Room, DataStore, WorkManager, CameraX, Credentials (Google Sign-In), and test dependencies (composeTest 1.7.0, uiAutomator 2.3.0, testRunner 1.6.2). See `plan.md` Phase 1 for exact catalog entries.
