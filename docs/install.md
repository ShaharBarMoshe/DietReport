# Installation Guide

How to build DietReport from source and install it on your Android device via USB.

---

## Requirements

| Requirement | Details |
|---|---|
| Android device | Android 15 (API 35) or higher |
| Computer | macOS, Linux, or Windows |
| Tools | Android Studio **or** the Android SDK platform-tools package |
| Cable | USB data cable (not charge-only) |

---

## Step 1 — Enable Developer Options on your phone

1. Open **Settings** on your Android device.
2. Scroll to **About phone** (or **About device**).
3. Tap **Build number** 7 times in a row until you see *"You are now a developer!"*
4. Go back to **Settings → System → Developer options** (location varies by manufacturer).
5. Enable **USB debugging**.

---

## Step 2 — Connect your phone

1. Plug your phone into your computer with the USB cable.
2. When prompted *"Allow USB debugging?"* on your phone, tap **Allow**.
   - Check *"Always allow from this computer"* to skip this prompt in future sessions.

---

## Step 3 — Verify the ADB connection

Open a terminal and run:

```bash
adb devices
```

Expected output:

```
List of devices attached
XXXXXXXX    device
```

> **Where is `adb`?**
> - Android Studio installs it at `~/Android/Sdk/platform-tools/adb`
> - Add it to your PATH permanently:
>   ```bash
>   # Add to ~/.bashrc or ~/.zshrc
>   export PATH="$HOME/Android/Sdk/platform-tools:$PATH"
>   ```

**If the device shows as `unauthorized`:** re-check the USB debugging prompt on your phone and tap Allow.

**If no device is listed:** try a different USB cable, a different port, or change the USB mode on your phone from *Charging only* to *File Transfer / MTP* (look in the notification shade after plugging in).

---

## Step 4 — Clone the repository

```bash
git clone https://github.com/ShaharBarMoshe/DietReport.git
cd DietReport
```

---

## Step 5 — Build the debug APK

```bash
./gradlew assembleDebug
```

The APK is output to:

```
app/build/outputs/apk/debug/app-debug.apk
```

---

## Step 6 — Install on your device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Successful output:

```
Performing Streamed Install
Success
```

**Reinstalling over an existing version:**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Step 7 — Launch the app

Open **DietReport** from your launcher, or launch directly from the terminal:

```bash
adb shell am start -n com.diet.dietreport/.MainActivity
```

---

## Permissions

The app requests the following permissions at runtime (not at install time):

| Permission | When prompted |
|---|---|
| Post notifications | First Settings save |
| Schedule exact alarms | First Settings save |
| Camera | Opening the meal-log camera for the first time |
| Read media images | Picking a photo from the gallery |

Grant all permissions when prompted. See the [User Guide](user-guide.md#exact-alarm-permission) for what happens if you deny exact alarms.

---

## Troubleshooting

### `adb: command not found`
Add the platform-tools directory to your PATH (see Step 3 above).

### `INSTALL_FAILED_UPDATE_INCOMPATIBLE`
Uninstall the existing version first, then install:
```bash
adb uninstall com.diet.dietreport
adb install app/build/outputs/apk/debug/app-debug.apk
```

### `INSTALL_FAILED_OLDER_SDK`
Your device is running Android older than 15 (API 35). The app requires Android 15 or higher.

### App crashes immediately after install
Check the crash log:
```bash
adb logcat -s "DR/*" AndroidRuntime
```

---

## Build commands reference

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing config)
./gradlew assembleRelease

# Build and install in one step
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Clean build artifacts
./gradlew clean
```
