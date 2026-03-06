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
import com.diet.dietreport.data.db.AppDatabase
import com.diet.dietreport.data.db.LogSource
import com.diet.dietreport.data.db.ReminderSlot
import com.diet.dietreport.data.db.SlotStatus
import com.diet.dietreport.lock.LockViewModel
import com.diet.dietreport.lock.LockViewModelFactory
import com.diet.dietreport.meals.LogMealUiState
import com.diet.dietreport.meals.LogMealViewModel
import com.diet.dietreport.meals.LogMealViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

private const val TIMEOUT = 15_000L

@RunWith(AndroidJUnit4::class)
class PhoneLockFlowTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val db = AppDatabase.getInstance(context)
    private lateinit var testImageFile: File
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        device.pressHome()

        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("pm grant ${context.packageName} android.permission.CAMERA")
            .close()

        testImageFile = File(context.filesDir, "meals/test_lock_meal.jpg")
            .also { it.parentFile?.mkdirs() }
        val bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
        testImageFile.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        bmp.recycle()
    }

    @After
    fun tearDown() {
        scenario?.close()
        LogMealViewModelFactory.testFactory = null
        LockViewModelFactory.testFactory = null
        runBlocking { db.reminderSlotDao().deleteFrom(0L) }
        testImageFile.delete()
    }

    // ─── Scenario A: explicit unlock button marks slot failed ───────────────

    @Test
    fun scenarioA_explicitUnlock_marksSlotFailed() {
        val slotId = insertPendingSlot()
        injectLogMealFactory(slotId)
        // Use default lock duration (10 min) — we'll tap unlock before it expires

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("destination", "log_meal")
            putExtra("slot_id", slotId)
        }
        scenario = ActivityScenario.launch(intent)

        // Navigate through LogMeal to LockScreen
        assertTrue(device.wait(Until.hasObject(By.res("log_meal_screen")), TIMEOUT))
        device.findObject(By.res("confirm_button")).click()
        assertTrue(device.wait(Until.hasObject(By.res("lock_screen")), TIMEOUT))

        // Assert countdown is visible and ≤ "10:00"
        val countdownObj = device.findObject(By.res("countdown_text"))
        assertNotNull(countdownObj)
        assertTrue(countdownObj.text <= "10:00")

        // Assert unlock button is visible
        assertTrue(device.hasObject(By.res("unlock_button")))

        // Tap unlock
        device.findObject(By.res("unlock_button")).click()

        // Should navigate to HomeScreen
        assertTrue(device.wait(Until.hasObject(By.res("home_screen")), TIMEOUT))

        // Slot status must be failed
        val slot = runBlocking { db.reminderSlotDao().getById(slotId) }
        assertEquals(SlotStatus.FAILED, slot?.status)
    }

    // ─── Scenario B: timer expiry does NOT change slot status ───────────────

    @Test
    fun scenarioB_timerExpiry_slotRemainsSuccess() {
        val slotId = insertPendingSlot()
        injectLogMealFactory(slotId)

        // Inject a LockViewModel with 3 s duration so we don't wait 10 min
        LockViewModelFactory.testFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LockViewModel(
                    slotId = slotId,
                    slotDao = db.reminderSlotDao(),
                    mealLogDao = db.mealLogDao(),
                    durationMs = 3_000L,
                ) as T
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("destination", "log_meal")
            putExtra("slot_id", slotId)
        }
        scenario = ActivityScenario.launch(intent)

        assertTrue(device.wait(Until.hasObject(By.res("log_meal_screen")), TIMEOUT))
        device.findObject(By.res("confirm_button")).click()
        assertTrue(device.wait(Until.hasObject(By.res("lock_screen")), TIMEOUT))

        // Wait for timer to expire (3 s) and navigate away automatically (up to 10 s)
        assertTrue(device.wait(Until.hasObject(By.res("home_screen")), 10_000L))

        // Slot status must still be success — timer expiry does NOT change it
        val slot = runBlocking { db.reminderSlotDao().getById(slotId) }
        assertEquals(SlotStatus.SUCCESS, slot?.status)
    }

    // ─── Scenario C: pressing Home marks slot failed ─────────────────────────

    @Test
    fun scenarioC_appBackgrounded_marksSlotFailed() {
        val slotId = insertPendingSlot()
        injectLogMealFactory(slotId)
        // Use default lock duration — long enough to not expire during the test

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("destination", "log_meal")
            putExtra("slot_id", slotId)
        }
        scenario = ActivityScenario.launch(intent)

        assertTrue(device.wait(Until.hasObject(By.res("log_meal_screen")), TIMEOUT))
        device.findObject(By.res("confirm_button")).click()
        assertTrue(device.wait(Until.hasObject(By.res("lock_screen")), TIMEOUT))

        // Press Home — triggers ON_STOP → onAppBackgrounded()
        device.pressHome()
        Thread.sleep(1_500L) // let the coroutine update the DB

        // Relaunch the app
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(
                "am start -n ${context.packageName}/.MainActivity"
            )
            .close()

        // Should show HomeScreen (LockScreen navigated away on backgrounding)
        assertTrue(device.wait(Until.hasObject(By.res("home_screen")), TIMEOUT))

        // Slot status must be failed
        val slot = runBlocking { db.reminderSlotDao().getById(slotId) }
        assertEquals(SlotStatus.FAILED, slot?.status)
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun insertPendingSlot(): Long {
        val scheduledAt = System.currentTimeMillis() - 10 * 60_000L
        return runBlocking {
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

    private fun injectLogMealFactory(slotId: Long) {
        LogMealViewModelFactory.testFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LogMealViewModel(
                    slotId = slotId,
                    slotDao = db.reminderSlotDao(),
                    mealLogDao = db.mealLogDao(),
                    filesDir = context.filesDir,
                    initialState = LogMealUiState(
                        photoPath = testImageFile.absolutePath,
                        photoSource = LogSource.CAMERA,
                    ),
                ) as T
        }
    }
}
