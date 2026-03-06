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
import com.diet.dietreport.meals.LogMealUiState
import com.diet.dietreport.meals.LogMealViewModel
import com.diet.dietreport.meals.LogMealViewModelFactory
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
class MealLoggingFlowTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val db = AppDatabase.getInstance(context)
    private var testSlotId = -1L
    private lateinit var testImageFile: File
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        device.pressHome()

        // Grant CAMERA permission
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("pm grant ${context.packageName} android.permission.CAMERA")
            .close()

        // Create a minimal 1×1 JPEG as the pre-loaded "captured" photo
        testImageFile = File(context.filesDir, "meals/test_meal.jpg")
            .also { it.parentFile?.mkdirs() }
        val bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
        testImageFile.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        bmp.recycle()

        // Insert a slot scheduled 10 min ago → within the 30-min success window
        val scheduledAt = System.currentTimeMillis() - 10 * 60_000L
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

        // Inject a ViewModel that starts with the photo already loaded (bypasses real camera)
        LogMealViewModelFactory.testFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LogMealViewModel(
                    slotId = testSlotId,
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

    @After
    fun tearDown() {
        scenario?.close()
        LogMealViewModelFactory.testFactory = null
        runBlocking { db.reminderSlotDao().deleteFrom(0L) }
        testImageFile.delete()
    }

    @Test
    fun mealLogging_happyFlow() {
        // Launch MainActivity with the notification deep-link intent
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("destination", "log_meal")
            putExtra("slot_id", testSlotId)
        }
        scenario = ActivityScenario.launch(intent)

        // 1. LogMealScreen is shown
        assertTrue(device.wait(Until.hasObject(By.res("log_meal_screen")), TIMEOUT))

        // 2. Photo preview is already visible (injected by testFactory)
        assertTrue(device.wait(Until.hasObject(By.res("photo_preview")), TIMEOUT))

        // 3. Tap Confirm
        device.findObject(By.res("confirm_button")).click()

        // 4. Wait for LogMealScreen to disappear (navigated away on confirm)
        assertTrue(device.wait(Until.gone(By.res("log_meal_screen")), TIMEOUT))

        // 5. Assert MealLog inserted with source = camera
        val logs = runBlocking { db.mealLogDao().logsForSlot(testSlotId) }
        assertEquals(1, logs.size)
        assertEquals(LogSource.CAMERA, logs[0].source)
        assertEquals(testImageFile.absolutePath, logs[0].photoPath)

        // 6. Assert slot status = success (logged within 30-min window)
        val slot = runBlocking { db.reminderSlotDao().getById(testSlotId) }
        assertEquals(SlotStatus.SUCCESS, slot?.status)

        // 7. App navigates to LockScreen (Phase 13: post-log lock period)
        assertTrue(device.wait(Until.hasObject(By.res("lock_screen")), TIMEOUT))
    }
}
