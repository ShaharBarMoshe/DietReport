# DietReport — User Guide

A step-by-step guide to every screen and feature in the app.

---

## Table of Contents

1. [First Launch & Onboarding](#first-launch--onboarding)
2. [Home Screen](#home-screen)
3. [Logging a Meal](#logging-a-meal)
4. [Focus Lock Screen](#focus-lock-screen)
5. [Photo Gallery](#photo-gallery)
6. [Settings](#settings)
7. [Report](#report)
8. [Notifications & Reminders](#notifications--reminders)
9. [Tips & Best Practices](#tips--best-practices)

---

## First Launch & Onboarding

When you open DietReport for the first time, the app sends you straight to **Settings** to complete your daily schedule. There is no account required — everything stays on your device.

**What to fill in:**

| Field | What it means |
|---|---|
| Wake time | When you typically get out of bed (HH:MM) |
| Bedtime | When you go to sleep (HH:MM) |
| First-meal delay | Minutes after waking before the first reminder fires (0–180) |
| Ringtone | Sound played when a reminder notification arrives |

Once you tap **Save** the schedule is built and reminders start automatically. Subsequent launches go straight to the **Home** screen.

---

## Home Screen

The Home screen is your daily dashboard. It shows every meal reminder slot for today and its current status.

### Slot cards

| Status chip | What it means |
|---|---|
| **Pending** | The 30-minute logging window is open — tap the card to log now |
| **Success** | You logged a meal photo within the window |
| **Failed** | The window expired without a log, or you skipped the lock screen |

Tap any **Pending** card to open the Meal Logging screen for that slot.

### Next-meal banner

A banner at the top of the list shows the time of the next upcoming reminder so you always know when to expect the next prompt.

### Extra meal button

Tap **+ Log extra meal** to log a meal outside of a scheduled slot. Useful for unplanned snacks or off-schedule eating.

### Permission warning banner

If the exact-alarm or notification permission is revoked, a yellow warning banner appears at the top of the screen. Tap **Dismiss** to hide it temporarily; re-granting the permission in system settings clears it permanently.

---

## Logging a Meal

The Meal Logging screen opens when you tap a Pending slot or a **Log meal** notification action.

### Taking a photo

1. Grant camera permission when prompted (one-time).
2. The camera preview fills the screen.
3. Tap **Capture** to take a photo with the rear camera.
4. Review the photo — tap **Retake** to discard and reshoot, or **Confirm** to save.

### Picking from gallery

Tap **Gallery** at any point to open the system image picker and choose an existing photo instead of taking a new one.

### Timing rule

A log is recorded as **Success** only if you tap **Confirm** within **30 minutes** of the scheduled reminder time. After that window, the slot is automatically marked **Failed** regardless.

---

## Focus Lock Screen

After confirming a meal log, the app enters a 10-minute focus period designed to keep you off your phone while you eat.

- The screen goes fullscreen (system bars hidden).
- A countdown timer runs from **10:00**.
- The screen stays on for the entire countdown.
- When the timer reaches zero, the lock ends and the app returns to Home — the meal remains **Success**.

### Ending the lock early

Tap **Unlock (mark as failed)** or press the system Back button to cancel the countdown. This marks the meal as **Failed**. The same happens if you background the app (press Home, switch apps, or pull down the notification shade).

> This is intentional. The lock screen only benefits you if you actually stay on it.

---

## Photo Gallery

The **Photos** tab shows a scrollable grid of every meal photo you have logged, sorted with the most recent first.

- Tap any photo to view it full-size.
- Photos are stored locally on the device and are never uploaded anywhere.
- Deleting the app removes all photos.

---

## Settings

Reached at any time via the **Settings** tab.

### Schedule

| Field | Valid range | Notes |
|---|---|---|
| Wake time | HH:MM | Must be before bedtime |
| Bedtime | HH:MM | Must be after wake time |
| First-meal delay | 0–180 minutes | Offset before the first daily reminder |

**How reminders are spaced:** starting at wake time + first-meal delay, a reminder fires every **3 hours** until **2 hours before bedtime**. For example, wake at 07:00, delay 30 min, bedtime 23:00 → reminders at 07:30, 10:30, 13:30, 16:30, 19:30 (next slot 22:30 is skipped because it falls inside the 2-hour buffer).

### Notification Sound

| Option | Description |
|---|---|
| Default | System default notification tone |
| Alarm | Louder alarm-style tone |
| Notification | Soft notification chime |

### Activity Monitor

When enabled, the app reads your daily phone-usage patterns to automatically infer wake and bedtime, overriding the manual fields. Requires the **Usage Access** permission granted in System Settings → Privacy → Usage Access.

### Saving changes

Tap **Save** to validate and apply all values. A green confirmation card appears briefly on success. The reminder schedule is rebuilt immediately — you do not need to restart the app.

---

## Report

The Report screen shows your meal-logging statistics.

### Period selector

Toggle between **Weekly** (last 7 days) and **Monthly** (last 30 days).

### Metrics

| Section | What it shows |
|---|---|
| Overall Success | Your success percentage for the selected period with a progress bar |
| By Day | Per-day breakdown with individual success rate and mini bar |
| By Hour | Success rate grouped by the scheduled reminder hour |
| Time of Day | Three buckets — Morning (before 12:00), Afternoon (12:00–17:00), Evening (after 17:00) |

A meal counts as a success only if it was logged within the 30-minute window.

### Sharing

Tap the **Share** button to generate a plain-text summary and open the Android Sharesheet. Share to WhatsApp, email, or any app.

### Clearing data

A clear-data action is available on the Report screen if you want to reset your history.

---

## Notifications & Reminders

### How reminders work

The app uses `AlarmManager` exact alarms — not vague background jobs — so reminders fire at the precise scheduled time even when the device is in low-power mode (subject to Android's exact-alarm permission).

### Notification actions

Each reminder notification has two actions:

| Action | What happens |
|---|---|
| **Log meal** | Opens the Meal Logging screen for that slot directly |
| **Snooze** | Reschedules the same reminder for 10 minutes later |

### After device reboot

The reminder schedule is automatically restored by a boot receiver. You do not need to open the app after restarting your phone.

### After timezone change

If you travel to a different timezone, a receiver detects the change and rebuilds the schedule using your new local time.

### Exact-alarm permission

Android requires explicit user permission to schedule exact alarms. The app requests this on the first Settings save. If you deny it:
- Reminders may fire several minutes late due to battery-optimization batching.
- A warning banner appears on the Home screen.

To re-grant: **Settings → Apps → DietReport → Permissions → Alarms & reminders → Allow**.

---

## Tips & Best Practices

- **Grant exact alarms.** Late reminders defeat the purpose of timed tracking.
- **Use the Gallery option.** If you already photographed your meal with the regular camera app, pick the existing photo — you do not need to retake it.
- **Stay on the Lock screen.** The 10-minute focus period only marks a meal as Success if you see it through. Backgrounding the app cancels it.
- **Do not clear app data.** This erases your entire meal history and all settings.
- **Keep notifications enabled** for the DietReport channel in system settings, or you will not receive reminders.
