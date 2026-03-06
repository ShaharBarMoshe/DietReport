package com.diet.dietreport

import android.app.Application
import android.graphics.Bitmap
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.diet.dietreport.auth.data.AuthRepository
import com.diet.dietreport.auth.data.User
import com.diet.dietreport.auth.data.authDataStore
import com.diet.dietreport.data.db.AppDatabase
import com.diet.dietreport.data.db.LogSource
import com.diet.dietreport.data.db.ReminderSlot
import com.diet.dietreport.data.db.SlotStatus
import com.diet.dietreport.lock.LockViewModel
import com.diet.dietreport.lock.LockViewModelFactory
import com.diet.dietreport.meals.LogMealUiState
import com.diet.dietreport.meals.LogMealViewModel
import com.diet.dietreport.meals.LogMealViewModelFactory
import com.diet.dietreport.settings.data.SettingsRepository
import com.diet.dietreport.settings.data.settingsDataStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

private const val TIMEOUT = 10_000L

@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val db = AppDatabase.getInstance(context)
    private lateinit var testImageFile: File
    private var scenario: ActivityScenario<MainActivity>? = null

    private var successSlotId = -1L
    private var failedSlotId = -1L
    private var pendingSlotId = -1L

    private val screenshotDir = "/sdcard/dietreport_screenshots"

    @Before
    fun setUp() {
        device.pressHome()
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("mkdir -p $screenshotDir").close()

        runBlocking {
            context.authDataStore.edit { it.clear() }
            context.settingsDataStore.edit { it.clear() }
            db.reminderSlotDao().deleteFrom(0L)
        }

        val now = System.currentTimeMillis()
        runBlocking {
            successSlotId = db.reminderSlotDao().insert(
                ReminderSlot(
                    scheduledAt = now - 180 * 60_000L,
                    wakeStart = now - 240 * 60_000L,
                    wakeEnd = now - 150 * 60_000L,
                    bedtime = now + 4 * 60 * 60_000L,
                    status = SlotStatus.SUCCESS,
                )
            )
            failedSlotId = db.reminderSlotDao().insert(
                ReminderSlot(
                    scheduledAt = now - 70 * 60_000L,
                    wakeStart = now - 120 * 60_000L,
                    wakeEnd = now + 60 * 60_000L,
                    bedtime = now + 4 * 60 * 60_000L,
                    status = SlotStatus.FAILED,
                )
            )
            pendingSlotId = db.reminderSlotDao().insert(
                ReminderSlot(
                    scheduledAt = now - 10 * 60_000L,
                    wakeStart = now - 120 * 60_000L,
                    wakeEnd = now + 60 * 60_000L,
                    bedtime = now + 4 * 60 * 60_000L,
                )
            )
        }

        testImageFile = File(context.filesDir, "meals/test_screenshot.jpg")
            .also { it.parentFile?.mkdirs() }
        val bmp = Bitmap.createBitmap(320, 240, Bitmap.Config.RGB_565)
        bmp.eraseColor(android.graphics.Color.rgb(80, 140, 80))
        testImageFile.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bmp.recycle()

        val capturedPendingSlotId = pendingSlotId
        LogMealViewModelFactory.testFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LogMealViewModel(
                    slotId = capturedPendingSlotId,
                    slotDao = db.reminderSlotDao(),
                    mealLogDao = db.mealLogDao(),
                    filesDir = context.filesDir,
                    initialState = LogMealUiState(
                        photoPath = testImageFile.absolutePath,
                        photoSource = LogSource.CAMERA,
                    ),
                ) as T
        }

        // Long duration so lock screen stays visible for the screenshot
        LockViewModelFactory.testFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LockViewModel(
                    slotId = capturedPendingSlotId,
                    slotDao = db.reminderSlotDao(),
                    mealLogDao = db.mealLogDao(),
                    durationMs = 600_000L,
                ) as T
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        LogMealViewModelFactory.testFactory = null
        LockViewModelFactory.testFactory = null
        runBlocking {
            db.reminderSlotDao().deleteFrom(0L)
            context.authDataStore.edit { it.clear() }
            context.settingsDataStore.edit { it.clear() }
        }
        testImageFile.delete()
    }

    private fun screenshot(name: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p $screenshotDir/$name.png").close()
    }

    @Test
    fun captureAllScreens() {
        // --- 1. Sign-in screen (no auth seeded yet) ---
        scenario = ActivityScenario.launch(MainActivity::class.java)
        device.wait(Until.hasObject(By.res("sign_in_screen")), TIMEOUT)
        Thread.sleep(1500) // let any credential-manager bottom sheet animate in
        device.pressBack()  // dismiss it if present
        Thread.sleep(800)
        screenshot("01_sign_in")
        scenario?.close()
        scenario = null

        // --- Seed auth + onboarding for remaining screens ---
        runBlocking {
            AuthRepository(context.authDataStore).saveUser(
                User("uid-test", "test@example.com", "Test User")
            )
            SettingsRepository(context.settingsDataStore).markOnboardingComplete()
        }

        scenario = ActivityScenario.launch(MainActivity::class.java)

        // --- 2. Home screen ---
        device.wait(Until.hasObject(By.res("home_screen")), TIMEOUT)
        Thread.sleep(800)
        screenshot("02_home")

        // --- 3. Settings screen ---
        device.findObject(By.text("Settings")).click()
        device.wait(Until.hasObject(By.res("settings_screen")), TIMEOUT)
        Thread.sleep(600)
        screenshot("03_settings")

        // --- 4. Report screen ---
        device.findObject(By.text("Report")).click()
        device.wait(Until.hasObject(By.res("report_screen")), TIMEOUT)
        Thread.sleep(600)
        screenshot("04_report")

        // --- 5. Log meal screen (photo pre-loaded) ---
        device.findObject(By.text("Home")).click()
        device.wait(Until.hasObject(By.res("home_screen")), TIMEOUT)
        device.findObject(By.res("slot_row_$pendingSlotId")).click()
        device.wait(Until.hasObject(By.res("log_meal_screen")), TIMEOUT)
        device.wait(Until.hasObject(By.res("photo_preview")), TIMEOUT)
        Thread.sleep(600)
        screenshot("05_log_meal")

        // --- 6. Lock screen ---
        device.findObject(By.res("confirm_button")).click()
        device.wait(Until.hasObject(By.res("lock_screen")), TIMEOUT)
        Thread.sleep(1200)
        screenshot("06_lock")

        // Unlock cleanly to end test
        device.findObject(By.res("unlock_button")).click()
        device.wait(Until.hasObject(By.res("home_screen")), TIMEOUT)
    }
}
