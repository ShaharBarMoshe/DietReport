package com.diet.dietreport

import android.app.Application
import android.content.Intent
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
import com.diet.dietreport.auth.AuthViewModelFactory
import com.diet.dietreport.auth.data.AuthRepository
import com.diet.dietreport.auth.data.User
import com.diet.dietreport.auth.data.authDataStore
import com.diet.dietreport.data.db.AppDatabase
import com.diet.dietreport.data.db.LogSource
import com.diet.dietreport.data.db.MealLog
import com.diet.dietreport.data.db.MealLogDao
import com.diet.dietreport.data.db.ReminderSlot
import com.diet.dietreport.data.db.SlotStatus
import com.diet.dietreport.meals.LogMealUiState
import com.diet.dietreport.meals.LogMealViewModel
import com.diet.dietreport.meals.LogMealViewModelFactory
import com.diet.dietreport.settings.SettingsViewModel
import com.diet.dietreport.settings.SettingsViewModelFactory
import com.diet.dietreport.settings.data.Settings
import com.diet.dietreport.settings.data.SettingsRepository
import com.diet.dietreport.settings.data.settingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

private const val TIMEOUT = 10_000L

@RunWith(AndroidJUnit4::class)
class ErrorHandlingTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val db = AppDatabase.getInstance(context)
    private var scenario: ActivityScenario<MainActivity>? = null
    private var testSlotId = -1L
    private lateinit var testImageFile: File

    @Before
    fun setUp() {
        device.pressHome()
        SchedulerErrorBus.clear()

        // Grant CAMERA permission
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("pm grant ${context.packageName} android.permission.CAMERA")
            .close()

        // Pre-sign-in so tests start past the sign-in screen
        runBlocking {
            AuthRepository(context.authDataStore).saveUser(
                User("uid-error-test", "error@example.com", "Error Test User")
            )
        }

        // Create a minimal test image
        testImageFile = File(context.filesDir, "meals/test_error_meal.jpg")
            .also { it.parentFile?.mkdirs() }
        val bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
        testImageFile.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        bmp.recycle()

        // Insert a reminder slot (used by the MealLog error test)
        val scheduledAt = System.currentTimeMillis() - 5 * 60_000L
        testSlotId = runBlocking {
            db.reminderSlotDao().insert(
                ReminderSlot(
                    scheduledAt = scheduledAt,
                    wakeStart = scheduledAt - 60_000L,
                    wakeEnd = scheduledAt + 2 * 60 * 60_000L,
                    bedtime = scheduledAt + 4 * 60 * 60_000L,
                )
            )
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        scenario = null
        AuthViewModelFactory.testFactory = null
        SettingsViewModelFactory.testFactory = null
        LogMealViewModelFactory.testFactory = null
        SchedulerErrorBus.clear()
        runBlocking {
            db.reminderSlotDao().deleteFrom(0L)
            context.authDataStore.edit { it.clear() }
            context.settingsDataStore.edit { it.clear() }
        }
        testImageFile.delete()
    }

    // -------------------------------------------------------------------------
    // Test 1: SettingsRepository throws on save → SettingsScreen shows error card
    // -------------------------------------------------------------------------

    @Test
    fun settingsSaveError_showsErrorCard() {
        // Inject a SettingsViewModel backed by a repository that always throws on save
        val throwingRepo = object : SettingsRepository(context.settingsDataStore) {
            override suspend fun save(settings: Settings) =
                throw RuntimeException("Simulated settings save failure")
        }
        SettingsViewModelFactory.testFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(throwingRepo) as T
        }

        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Navigate to Settings screen
        assertTrue(device.wait(Until.hasObject(By.text("Settings")), TIMEOUT))
        device.findObject(By.text("Settings")).click()
        assertTrue(device.wait(Until.hasObject(By.res("settings_screen")), TIMEOUT))

        // Tap Save
        device.findObject(By.res("save_button")).click()

        // Assert error card is shown (not a crash)
        assertTrue(
            "Expected settings_error to appear",
            device.wait(Until.hasObject(By.res("settings_error")), TIMEOUT),
        )
    }

    // -------------------------------------------------------------------------
    // Test 2: SchedulerErrorBus post → HomeScreen shows scheduler error banner
    // -------------------------------------------------------------------------

    @Test
    fun schedulerError_showsErrorBannerOnHome() {
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Wait for HomeScreen
        assertTrue(device.wait(Until.hasObject(By.res("home_screen")), TIMEOUT))

        // Simulate a scheduler failure (as ReminderScheduler would do on exception)
        SchedulerErrorBus.post(AppError.SchedulerError("Simulated scheduler failure"))

        // Assert error banner is visible on HomeScreen (not a crash)
        assertTrue(
            "Expected scheduler_error banner to appear on HomeScreen",
            device.wait(Until.hasObject(By.res("scheduler_error")), TIMEOUT),
        )
    }

    // -------------------------------------------------------------------------
    // Test 3: MealLogDao throws on insert → LogMealScreen shows error, slot stays PENDING
    // -------------------------------------------------------------------------

    @Test
    fun mealLogInsertError_showsErrorAndSlotRemainingPending() {
        // Inject LogMealViewModel with a throwing MealLogDao + pre-loaded photo
        LogMealViewModelFactory.testFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LogMealViewModel(
                    slotId = testSlotId,
                    slotDao = db.reminderSlotDao(),
                    mealLogDao = ThrowingMealLogDao(),
                    filesDir = context.filesDir,
                    initialState = LogMealUiState(
                        photoPath = testImageFile.absolutePath,
                        photoSource = LogSource.CAMERA,
                    ),
                ) as T
        }

        // Launch with deep-link to log_meal
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("destination", "log_meal")
            putExtra("slot_id", testSlotId)
        }
        scenario = ActivityScenario.launch(intent)

        // Wait for LogMealScreen with photo preview
        assertTrue(device.wait(Until.hasObject(By.res("log_meal_screen")), TIMEOUT))
        assertTrue(device.wait(Until.hasObject(By.res("photo_preview")), TIMEOUT))

        // Tap Confirm
        device.findObject(By.res("confirm_button")).click()

        // Assert error message is shown (not a crash)
        assertTrue(
            "Expected log_meal_error to appear",
            device.wait(Until.hasObject(By.res("log_meal_error")), TIMEOUT),
        )

        // Assert slot status is still PENDING (insert threw before updateStatus could run)
        val slot = runBlocking { db.reminderSlotDao().getById(testSlotId) }
        assertEquals(SlotStatus.PENDING, slot?.status)

        // Assert no MealLog was inserted
        val logs = runBlocking { db.mealLogDao().logsForSlot(testSlotId) }
        assertTrue("No meal log should be inserted on failure", logs.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Fake DAO that throws on insert
    // -------------------------------------------------------------------------

    private class ThrowingMealLogDao : MealLogDao {
        override suspend fun insert(log: MealLog): Long =
            throw RuntimeException("Simulated MealLog insert failure")

        override suspend fun logsForSlot(slotId: Long): List<MealLog> = emptyList()

        override fun logsForSlotFlow(slotId: Long): Flow<List<MealLog>> = flowOf(emptyList())
    }
}
