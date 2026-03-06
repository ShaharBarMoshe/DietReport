# Android Meal Tracking App — Plan

## Tech Stack

| Area | Choice |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material Design 3 + ViewModel |
| Architecture | MVVM, single Activity (`MainActivity`), Compose Navigation |
| Navigation | Navigation Compose (`NavHost` + `NavController`) |
| Local DB | Room |
| Preferences | DataStore |
| Scheduling | AlarmManager (exact alarms) + WorkManager |
| Auth | Google Sign-In via Credential Manager |
| Camera | CameraX + system gallery picker |
| Sharing | Android Sharesheet — `Intent.ACTION_SEND` |
| Min SDK | 35 (Android 15) |
| Target / Compile SDK | 36 (Android 16) |
| AGP | 8.13.2 |

> minSdk 35 — no version guards needed for `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `READ_MEDIA_IMAGES`, or dynamic color.

---

## Test Dependencies (add in Phase 1)

```toml
# libs.versions.toml
composeTest       = "1.7.0"       # matches Compose BOM 2024.09.00
uiAutomator       = "2.3.0"
testRunner        = "1.6.2"
```

```kotlin
// app/build.gradle.kts  (androidTestImplementation)
androidTestImplementation(platform(libs.androidx.compose.bom))
androidTestImplementation(libs.androidx.compose.ui.test.junit4)
androidTestImplementation(libs.androidx.test.runner)
androidTestImplementation(libs.androidx.uiautomator)
debugImplementation(libs.androidx.compose.ui.test.manifest)
```

E2E tests live in `app/src/androidTest/` and run on a device/emulator.
Each phase's happy-flow test must **pass** before the phase is considered done.

---

## Core Flow

```
Google Sign-In
    └─► Settings (waking hours, bedtime, ring tone, first-meal delay, activity monitor)
            └─► Reminder engine (every 3h, stops 2h before bed)
                    └─► Notification + ring
                            └─► 30-min logging window (Camera / Gallery)
                                    ├─► Success
                                    └─► Fail
                                            └─► Weekly / Monthly report
                                                        └─► Share (WhatsApp / Email)
```

---

## Current App State

```
MainActivity
└── DietReportTheme
        └── Scaffold  (placeholder — ready for NavHost)
```

- Compose + Material 3 theme in `ui/theme/` (Color, Type, Theme).
- Dynamic color on, edge-to-edge on.
- `NavHost` replaces the placeholder as phases are completed.

---

## Target Package Structure

```
com.diet.dietreport/
├── MainActivity.kt
├── ui/theme/
├── auth/
│   ├── SignInScreen.kt
│   └── AuthViewModel.kt
├── settings/
│   ├── SettingsScreen.kt
│   ├── SettingsViewModel.kt
│   └── data/SettingsRepository.kt      # DataStore
├── reminders/
│   ├── ReminderScheduler.kt            # AlarmManager logic
│   ├── ReminderReceiver.kt             # BroadcastReceiver
│   ├── BootReceiver.kt
│   └── data/ReminderRepository.kt
├── meals/
│   ├── HomeScreen.kt
│   ├── HomeViewModel.kt
│   ├── LogMealScreen.kt
│   ├── LogMealViewModel.kt
│   └── data/MealRepository.kt
├── reports/
│   ├── ReportScreen.kt
│   └── ReportViewModel.kt
└── data/db/                            # Room database, DAOs, entities
```

---

## Permissions

| Permission | Purpose |
|---|---|
| `SCHEDULE_EXACT_ALARM` | Precise reminder ring time |
| `POST_NOTIFICATIONS` | Notification display |
| `CAMERA` | In-app camera capture |
| `READ_MEDIA_IMAGES` | Gallery picker |
| `RECEIVE_BOOT_COMPLETED` | Reschedule alarms after reboot |
| `PACKAGE_USAGE_STATS` | Activity monitor (user grants in system Settings) |

---

## Out of Scope (MVP)

- Backend / cloud sync (app works fully offline)
- Weight or nutrition tracking
- Custom reminder intervals (fixed 3 h)

---

## Phases

Each phase must be **built, run on device/emulator, and all E2E tests passed** before starting the next.

---

### Phase 1 — Dependencies & Navigation Shell
**Goal:** app navigates between placeholder screens; skeleton compiles and runs.

**Tasks:**
- Add to `libs.versions.toml` + `app/build.gradle.kts`: Room, DataStore, WorkManager, CameraX, Credentials (Google Sign-In), and all test dependencies above
- Create placeholder `@Composable` screens: `SignInScreen`, `HomeScreen`, `SettingsScreen`, `LogMealScreen`, `ReportScreen`
- Wire `NavHost` in `MainActivity` with routes for all screens
- Add bottom navigation bar (Home, Settings, Report)

**E2E Test — `.`:**
```
1. Launch app
2. Assert SignInScreen is displayed
3. Grant any runtime permissions via UiAutomator
4. Navigate to HomeScreen via nav bar → assert HomeScreen is displayed
5. Navigate to SettingsScreen via nav bar → assert SettingsScreen is displayed
6. Navigate to ReportScreen via nav bar → assert ReportScreen is displayed
7. Press back → assert previous screen is displayed
```

**Done when:**
- [x] `NavigationShellTest` passes
- [x] No build errors or lint warnings on new files

---

### Phase 2 — Authentication (Google Sign-In)
**Goal:** user can sign in and sign out with a real Google account.

**Tasks:**
- Implement `SignInScreen` with "Sign in with Google" button
- `AuthViewModel` calls Credential Manager; stores `email`, `displayName`, `userId` in DataStore
- On success → navigate to `HomeScreen`; on first launch → navigate to `SettingsScreen` for onboarding
- Sign-out clears stored user data and returns to `SignInScreen`
- Protect all routes: redirect to `SignInScreen` if no user stored

**E2E Test — `AuthFlowTest`:**
```
1. Launch app with no stored user (fresh install / cleared data)
2. Assert SignInScreen is displayed
3. Inject a fake AuthViewModel that returns a pre-set user (bypass real Google dialog)
4. Tap "Sign in with Google"
5. Assert HomeScreen is displayed
6. Assert stored email matches injected user
7. Tap sign-out (from Settings or menu)
8. Assert SignInScreen is displayed
9. Assert stored user data is cleared
10. Press back → assert app stays on SignInScreen (not Home)
```

**Done when:**
- [x] `AuthFlowTest` passes
- [x] User info persists across app restarts (manual check)
- [x] Back press on Home does not return to Sign-In

---

### Phase 3 — Settings
**Goal:** user can configure all scheduling inputs; values persist and survive restart.

**Tasks:**
- `SettingsScreen` UI: time pickers for wake/bedtime, slider/input for first-meal delay (0–180 min), ringtone selector, activity-monitor toggle
- `SettingsRepository` reads/writes via DataStore
- `SettingsValidator`: wake < bedtime; delay in 0–180 min range; ring from allowed set — returns `Result<Settings>`
- Show inline validation errors; block save on invalid input

**E2E Test — `SettingsFlowTest`:**
```
1. Launch app signed in, navigate to SettingsScreen
2. Set wake time to 07:00
3. Set bedtime to 23:00
4. Set first-meal delay to 60 min
5. Select default ringtone
6. Tap Save
7. Assert success confirmation is shown
8. Kill and relaunch app
9. Navigate to SettingsScreen
10. Assert all five saved values are displayed correctly
```

**Unit tests:** `SettingsValidator` boundary values (wake == bedtime, delay = 0, delay = 180, delay = 181).

**Done when:**
- [x] `SettingsFlowTest` passes
- [x] `SettingsValidator` unit tests pass
- [x] Invalid input (wake after bedtime) shows inline error and blocks save

---

### Phase 4 — Room Database
**Goal:** all persistent data models exist and DAOs are tested.

**Entities:**
```
ReminderSlot   id, scheduled_at, wake_start, wake_end, bedtime, status (pending|success|failed)
MealLog        id, reminder_slot_id, photo_path, logged_at, source (camera|gallery)
UsageDay       date, inferred_wake, inferred_bedtime          ← activity monitor only
```

**Tasks:**
- Define entities, DAOs, and `AppDatabase` (Room)
- DAO methods: insert/query slots by date range, insert meal log, query logs by slot, aggregate success % by day/hour/bucket
- Write unit tests for all DAO queries using an in-memory Room database

**E2E Test — `DatabaseSmokeTest`** (instrumented, in-memory DB):
```
1. Insert 3 ReminderSlots for today
2. Query slots for today → assert 3 results
3. Insert a MealLog linked to slot[0] with logged_at = scheduled_at + 10 min
4. Query MealLog for slot[0] → assert 1 result, source matches
5. Query success % for today → assert 33 % (1 of 3)
6. Insert MealLog for slot[1] with logged_at = scheduled_at + 35 min (failed)
7. Query success % for today → assert still 33 % (late log doesn't count as success)
```

**Done when:**
- [x] `DatabaseSmokeTest` passes
- [x] All DAO unit tests pass
- [x] Database migration version set to 1

---

### Phase 5 — Reminder Engine
**Goal:** reminders fire at the correct times and persist across reboots.

**Tasks:**
- `ReminderScheduler`: compute slots (wake + delay, then every 3 h, stop 2 h before bed); schedule via `AlarmManager` exact alarms; persist each slot to Room
- `ReminderReceiver`: on alarm → fire high-priority notification + play ringtone; mark slot status
- `BootReceiver`: on `BOOT_COMPLETED` → reload unfinished slots from Room and reschedule
- Cancel and rebuild full schedule whenever Settings change
- Request `SCHEDULE_EXACT_ALARM` and `POST_NOTIFICATIONS` at runtime; handle denial gracefully

**E2E Test — `ReminderEngineTest`:**
```
1. Set wake = now, delay = 0 min, bed = now + 4h (forces a slot at "now")
2. Call ReminderScheduler.schedule()
3. Assert 1 ReminderSlot is inserted in DB with status = pending
4. Wait for notification to appear (UiAutomator, max 90 s)
5. Assert notification title/body matches expected slot time
6. Assert notification has "Log meal" and "Snooze" actions
```

**Unit tests:** slot-computation logic with fixed clock inputs (no AlarmManager dependency).

**Done when:**
- [x] `ReminderEngineTest` passes
- [x] Slot-computation unit tests pass
- [ ] Slots survive a device reboot and re-trigger (manual check)
- [ ] Changing wake time rebuilds schedule immediately (manual check)

---

### Phase 6 — Meal Logging
**Goal:** user can log a meal photo from notification or Home screen within the 30-min window.

**Tasks:**
- `LogMealScreen`: camera button (CameraX) and gallery button (`ACTION_GET_CONTENT`); confirm photo preview; save `MealLog` to Room
- Request `CAMERA` and `READ_MEDIA_IMAGES` at runtime
- Copy selected/captured image into app-controlled storage; validate MIME type and file size
- Success rule: `logged_at ≤ scheduled_at + 30 min` → `status = success`; else `status = failed`
- Notification "Log meal" action deep-links to `LogMealScreen` with `slotId`; validate `slotId` exists before navigating
- Snooze action reschedules alarm +10 min

**E2E Test — `MealLoggingFlowTest`:**
```
1. Insert a ReminderSlot with scheduled_at = now - 10 min, status = pending
2. Navigate to LogMealScreen with that slotId
3. Grant CAMERA permission via UiAutomator
4. Tap "Take photo" → assert camera preview opens
5. Simulate shutter (or inject a pre-loaded test image via fake CameraX)
6. Assert photo preview is shown in LogMealScreen
7. Tap Confirm
8. Assert MealLog is inserted in DB with source = camera
9. Assert slot status in DB is now success (logged_at within 30 min)
10. Assert HomeScreen shows the slot with "Success" chip
```

**Done when:**
- [x] `MealLoggingFlowTest` passes
- [ ] Gallery pick flow verified manually (gallery intent hard to automate)
- [ ] Logging after 30 min marks slot failed (manual check)
- [ ] Notification "Log meal" deep link navigates correctly (manual check)

---

### Phase 7 — Home Screen
**Goal:** user sees today's reminder slots with real-time status.

**Tasks:**
- `HomeScreen` lists today's `ReminderSlot` rows with status chips (Pending / Success / Failed)
- Tap a Pending slot → navigate to `LogMealScreen`
- `HomeViewModel` observes Room via `Flow`; list updates automatically when a log is saved
- Show empty state when no slots scheduled (e.g. outside waking hours)

**E2E Test — `HomeScreenFlowTest`:**
```
1. Seed DB with 3 slots for today: status = success, pending, failed
2. Launch HomeScreen
3. Assert 3 slot rows are visible
4. Assert slot[0] shows "Success" chip
5. Assert slot[1] shows "Pending" chip
6. Assert slot[2] shows "Failed" chip
7. Tap slot[1] (Pending) → assert LogMealScreen opens with correct slotId
8. Confirm a photo in LogMealScreen
9. Press back to HomeScreen
10. Assert slot[1] chip has updated to "Success" without restart
11. Clear DB → assert empty-state message is visible
```

**Done when:**
- [x] `HomeScreenFlowTest` passes
- [x] Live status update verified by test (step 10)

---

### Phase 8 — Activity Monitor (optional, can skip for MVP)
**Goal:** wake/sleep times are derived automatically from phone usage.

**Tasks:**
- Request `PACKAGE_USAGE_STATS`; direct user to system Settings if not granted; fall back to manual hours if denied
- Daily WorkManager job: query `UsageStatsManager` for last interaction in 19:00–07:00 window (= sleep start) and first interaction (= wake); persist to `UsageDay`
- Average last 3 `UsageDay` rows → write derived wake/bedtime to Settings; trigger reschedule
- When fewer than 3 days exist, use defaults (07:00–23:00)
- Toggle in SettingsScreen enables/disables this flow

**E2E Test — `ActivityMonitorTest`:**
```
1. Enable activity-monitor toggle in SettingsScreen
2. Inject 3 fake UsageDay rows: wake 07:30/07:00/06:30, bed 22:30/23:00/23:30
3. Trigger the WorkManager job directly in test
4. Read derived wake from DataStore → assert 07:00 (average)
5. Read derived bedtime from DataStore → assert 23:00 (average)
6. Assert ReminderScheduler was called with new wake/bedtime
7. Inject only 2 UsageDay rows
8. Trigger job again → assert fallback values 07:00/23:00 are used
```

**Done when:**
- [ ] `ActivityMonitorTest` passes
- [ ] Disabling the toggle stops the job and restores manual values (manual check)

---

### Phase 9 — Reports & Sharing
**Goal:** user can view weekly/monthly stats and share them.

**Tasks:**
- `ReportScreen`: period picker (Weekly / Monthly); display overall %, by-day list, by-hour breakdown, morning/afternoon/evening buckets
- `ReportViewModel` queries Room for the selected range; computes metrics
- Validate report params: start ≤ end; range ≤ 1 year
- Share button builds a text summary and opens Android Sharesheet (`Intent.ACTION_SEND`)

**E2E Test — `ReportFlowTest`:**
```
1. Seed DB with 7 days of slots (5 per day = 35 total); mark 21 as success (60 %)
   - Distribute: 3 success at 08:00, 4 at 11:00, 5 at 14:00 (morning bucket heavy)
2. Navigate to ReportScreen, select Weekly period for those 7 days
3. Assert overall % = 60 %
4. Assert by-day row for day[0] shows correct daily %
5. Assert by-hour row for 08:00 shows correct %
6. Assert Morning bucket % is higher than Evening bucket %
7. Tap Share → assert Sharesheet opens (UiAutomator: assertActivityExists)
8. Assert share text contains overall % and date range
```

**Unit tests:** `ReportViewModel` metric computation with a fake repository returning fixed data.

**Done when:**
- [x] `ReportFlowTest` passes
- [x] `ReportViewModel` unit tests pass
- [ ] Invalid date range shows inline error (manual check)

---

### Phase 10 — Error Handling & Logging
**Goal:** all failures are caught, reported to the user, and traceable via logs.

**Tasks:**

**Error handling:**
- Define a sealed `AppError` hierarchy covering: `AuthError`, `DatabaseError`, `SchedulerError`, `CameraError`, `NetworkError` (future-proof)
- Each ViewModel exposes a `uiState` with an `error: AppError?` field; screens show a `Snackbar` or inline message and a retry action where applicable
- Wrap all Room operations in `try/catch`; surface `DatabaseError` to ViewModel
- Wrap all AlarmManager scheduling in `try/catch`; surface `SchedulerError` if exact-alarm permission is missing
- Wrap CameraX and file-copy operations; surface `CameraError` on failure
- Never swallow exceptions silently — every `catch` either rethrows, maps to `AppError`, or logs

**Logging:**
- Use `android.util.Log` with a consistent tag per module (e.g. `"DR/Auth"`, `"DR/Reminder"`, `"DR/Meals"`)
- Log levels: `d` for lifecycle/flow events, `w` for recoverable issues, `e` for caught exceptions (include `throwable`)
- Strip `Log.d` calls in release builds via ProGuard rule: `-assumenosideeffects class android.util.Log { public static int d(...); }`
- Log key events: sign-in success/failure, settings saved, schedule built (slot count + times), alarm fired, meal logged (slotId + source + on-time/late), report generated

**Done when:**
- [x] All ViewModels surface errors via `uiState.error`; no unhandled exceptions in logcat during happy flow
- [x] Each module uses its own log tag; all key events appear in logcat during a full happy-flow run
- [x] `Log.d` calls absent from release build (verify with `./gradlew assembleRelease` + grep)

**E2E Test — `ErrorHandlingTest`:**
```
1. Inject a SettingsRepository that throws on save
2. Tap Save on SettingsScreen
3. Assert Snackbar/error message is visible (not a crash)
4. Inject a ReminderRepository that throws on insert
5. Call ReminderScheduler.schedule()
6. Assert HomeScreen shows scheduler-error warning (not a crash)
7. Inject a MealRepository that throws on MealLog insert
8. Confirm a photo in LogMealScreen
9. Assert error message is shown and slot status remains pending (not crashed/corrupted)
```

---

### Phase 11 — Polish & Edge Cases
**Goal:** app is stable across real-world conditions.

**Tasks:**
- Onboarding flow: first-launch guide through Settings before Home
- Handle timezone changes: recalculate and reschedule slots
- Handle DND / alarm permission revoked: show in-app warning, degrade gracefully
- Review all `contentDescription` on interactive Compose elements (accessibility)
- Final input-validation pass: URIs, deep-link extras, all SettingsValidator and ReportParamsValidator paths

**E2E Test — `OnboardingFlowTest`:**
```
1. Clear all app data (fresh install state)
2. Launch app → assert SignInScreen is displayed
3. Sign in (fake auth)
4. Assert SettingsScreen is displayed (first-launch onboarding, not HomeScreen)
5. Complete settings (wake, bedtime, delay, ring)
6. Tap Save → assert HomeScreen is displayed
7. Kill and relaunch app → assert HomeScreen is displayed directly (no onboarding repeat)
```

**E2E Test — `PermissionDegradationTest`:**
```
1. Launch app signed in with settings configured
2. Revoke SCHEDULE_EXACT_ALARM via UiAutomator shell command
3. Trigger schedule rebuild
4. Assert in-app warning banner is visible on HomeScreen
5. Assert no crash occurs
```

**Done when:**
- [x] `OnboardingFlowTest` passes
- [x] `PermissionDegradationTest` passes
- [ ] Timezone change reschedules correctly (manual check)
- [ ] No accessibility lint errors on interactive elements

---

### Phase 12 — Google Play Release Readiness
**Goal:** the app is production-hardened and uploadable to Google Play as a signed AAB.

**Tasks:**
- Enable R8 minification (`isMinifyEnabled = true`) and resource shrinking (`isShrinkResources = true`) in the release build type
- Configure release signing via `keystore.properties` (git-ignored); build does not fail if file is absent
- ProGuard rules: Room entities/enums, Kotlin coroutines, Credential Manager, CameraX, line numbers for crash reporting; strip `Log.d` from release
- Move `androidx.junit.ktx` from `implementation` → `androidTestImplementation` (was leaking into release APK)
- Add network security config — disallow all cleartext (HTTP) traffic
- Configure backup rules — exclude auth DataStore preferences and meal photos from cloud backup; allow photos in device-transfer
- Add `INTERNET` permission to manifest (required by Credential Manager for OAuth handshake)
- Add `keystore.properties.example` with keytool generation command; add `keystore.properties`, `*.jks`, `*.keystore` to `.gitignore`

**Manual steps (outside code):**
1. Generate release keystore: `keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias dietreport`
2. Create `keystore.properties` at project root (see `keystore.properties.example`)
3. Replace `YOUR_WEB_CLIENT_ID` in `CredentialManagerAuthService.kt` with your OAuth 2.0 Web Client ID from Google Cloud Console
4. In Play Console: complete Data Safety form, Privacy Policy URL, content rating, store listing assets (icon 512×512, feature graphic 1024×500, screenshots)

**Done when:**
- [x] `./gradlew assembleRelease` succeeds with R8 minification enabled (no ProGuard errors)
- [x] `./gradlew bundleRelease` produces a valid AAB
- [x] Release APK does not contain `Log.d` calls (verified by R8 `-assumenosideeffects` rule)
- [x] `keystore.properties` and `*.jks` are git-ignored
- [x] `android:networkSecurityConfig` set; no cleartext traffic permitted
- [x] Auth DataStore and meal photos excluded from cloud backup
- [x] `INTERNET` permission declared in manifest
- [ ] `keystore.properties` filled in with real keystore and signing succeeds (manual)
- [ ] `YOUR_WEB_CLIENT_ID` replaced with real OAuth client ID (manual)
- [ ] Data Safety form, Privacy Policy, and store listing completed in Play Console (manual)

---

### Phase 13 — Post-Log Phone Lock
**Goal:** after confirming a meal photo, the user is shown a fullscreen "lock" screen for 10 minutes; leaving early marks the meal as a failure.

**Background:** true device lock requires Device Admin (too invasive for a diet app). Instead, the app navigates to a fullscreen immersive `LockScreen` composable that occupies the entire display. The OS home/recents buttons still work (unavoidable), but when the user returns to the app the lock screen is still the top destination. Tapping the explicit "Unlock early" button — the only interactive element — marks the slot as failed and exits.

**Tasks:**
- Add `Routes.LOCK = "lock/{slotId}"` with `NavType.LongType` argument; register in `NavHost`
- Navigate from `LogMealScreen` to `LockScreen` immediately after a successful `MealLog` insert (replace the current navigate-back-to-home)
- `LockScreen` composable:
  - Fullscreen immersive (`WindowInsetsController` hides system bars; `systemBarsPadding` removed)
  - Countdown timer display (`MM:SS` format) counting down from 10:00
  - Explanatory message: "Your phone is locked. Tapping Unlock will mark this meal as FAILED."
  - Single "Unlock (mark as failed)" button, styled destructively (error color)
  - Screen stays on during countdown (`FLAG_KEEP_SCREEN_ON`)
- `LockViewModel(slotId, slotDao, mealLogDao, durationMs = 600_000L)`:
  - `uiState`: `remainingMs: Long`, `done: Boolean`, `error: AppError?`
  - Starts a `countDownTimer` coroutine on init; emits tick every second
  - `onUnlockEarly()`: cancels timer, updates slot status to `failed`, sets `done = true`
  - `onAppBackgrounded()`: same logic as `onUnlockEarly()` — idempotent, safe to call multiple times
  - Timer reaches zero: sets `done = true` (slot status unchanged — already `success`)
  - `LockViewModelFactory.testFactory` override for unit/instrumented tests
- Detect app backgrounding via `LifecycleEventObserver` inside `LockScreen`: on `Lifecycle.Event.ON_STOP` (user switches to another app, pulls down notification shade, or presses Home/Recents) → call `viewModel.onAppBackgrounded()` which cancels the timer and marks slot as failed; no additional permission required
- When the user returns to the app after being marked failed (`done = true`), the composable navigates to Home showing the failed state
- Back button on `LockScreen`: intercept with `BackHandler`; pressing back triggers the same "Unlock early" path (marks failed, navigates to Home)

**No new DB schema changes** — `status` field on `ReminderSlot` already supports `success` / `failed`.

**E2E Test — `PhoneLockFlowTest`:**
```
# Scenario A — explicit unlock button
1. Insert a ReminderSlot with scheduled_at = now - 5 min, status = pending
2. Navigate to LogMealScreen with that slotId; inject a pre-loaded test image via testFactory
3. Tap Confirm → assert LockScreen is displayed (testTag = "lock_screen")
4. Assert countdown text is visible and ≤ "10:00"
5. Assert "Unlock (mark as failed)" button is displayed
6. Tap "Unlock (mark as failed)"
7. Assert HomeScreen is displayed
8. Query DB → assert slot status = failed

# Scenario B — timer expiry (no interaction)
9. Insert a fresh ReminderSlot; navigate to LockScreen with short duration (3 s) via testFactory
10. Wait 5 s (UiAutomator)
11. Assert HomeScreen is displayed (timer expired automatically)
12. Query DB → assert slot status = success (timer expiry does NOT change status)

# Scenario C — app backgrounded (switches to another app)
13. Insert a fresh ReminderSlot; navigate to LockScreen
14. Press the Home button via UiAutomator (`UiDevice.pressHome()`)
15. Wait 1 s; relaunch app via UiAutomator
16. Assert HomeScreen is displayed (lock screen navigated away on re-foreground)
17. Query DB → assert slot status = failed
```

**Unit tests (`LockViewModelTest`):**
- Timer reaches zero → `done = true`, slot status unchanged (`success`)
- `onUnlockEarly()` → slot status set to `failed`, `done = true`
- `onAppBackgrounded()` → slot status set to `failed`, `done = true`
- `onUnlockEarly()` or `onAppBackgrounded()` called multiple times → idempotent (no duplicate DB writes)

**Done when:**
- [x] `PhoneLockFlowTest` (all three scenarios) passes on API 35 & 36
- [x] `LockViewModelTest` unit tests pass
- [ ] Back button on LockScreen marks slot failed and returns to Home (manual check)
- [ ] Switching to another app (Home, Recents, notification tap) marks slot failed (manual check)
- [ ] Returning to app after backgrounding shows HomeScreen with failed status (manual check)
