# DietReport

> An Android app that nags you to photograph your meals every 3 hours — because apparently the honor system wasn't working.

DietReport sends timed reminders throughout your waking hours, gives you a 30-minute window to snap a photo of whatever you're eating, locks your phone for 10 minutes so you actually eat without scrolling, and generates weekly/monthly success-rate reports so you can watch yourself fail with statistical precision.

Fully offline. No backend. No cloud sync. Your salad photos stay between you and your device.

---

## Features

- Configurable wake time, bedtime, and first-meal delay
- Reminders every 3 hours until 2 hours before bedtime (exact alarms — not "eventually")
- 30-minute logging window per reminder — camera or gallery
- 10-minute focus lock screen after logging (exits early = marked failed, no cheating)
- Photo gallery of all logged meals
- Weekly and monthly success-rate reports with sharing
- Survives reboots, timezone changes, and notification permission revocations
- Android 15+ required, no account required

---

## Documentation

| Document | Description |
|---|---|
| [User Guide](docs/user-guide.html) | Every screen explained — Home, logging, lock screen, gallery, settings, reports |
| [Installation Guide](docs/install.md) | Build from source and sideload via USB |
| [Architecture](docs/architecture.html) | Package structure, data layer, scheduling, error handling |

---

## Quick Start

**Requirements:** Android 15+ device, Android Studio or SDK platform-tools, USB cable.

```bash
git clone https://github.com/ShaharBarMoshe/DietReport.git
cd DietReport
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Full step-by-step instructions (enabling USB debugging, ADB setup, troubleshooting) are in the [Installation Guide](docs/install.md).

---

## Tech Stack

| Area | Technology |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material Design 3 |
| Architecture | MVVM, single Activity |
| Database | Room |
| Preferences | DataStore |
| Scheduling | AlarmManager (exact alarms) |
| Camera | CameraX |
| Min SDK | 35 (Android 15) |
| Target SDK | 36 (Android 16) |

---

## Project Structure

```
com.diet.dietreport/
├── meals/        — Home, LogMeal, PhotoGallery screens + ViewModels
├── lock/         — Focus lock screen
├── settings/     — Settings screen + DataStore repository
├── reminders/    — AlarmManager scheduling, notification, broadcast receivers
├── reports/      — Report screen + metrics computation
├── data/db/      — Room database, DAOs, entities
└── ui/theme/     — Material 3 theme
```

See [Architecture](docs/architecture.html) for the full breakdown.

---

## Building & Testing

```bash
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # build + install on connected device
./gradlew test                   # unit tests
./gradlew connectedAndroidTest   # instrumented tests (device required)
./gradlew lint
./gradlew clean
```
