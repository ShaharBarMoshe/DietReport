package com.diet.dietreport

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.datastore.preferences.core.edit
import com.diet.dietreport.data.db.AppDatabase
import com.diet.dietreport.settings.data.SettingsRepository
import com.diet.dietreport.settings.data.settingsDataStore
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

private const val TIMEOUT = 10_000L

@RunWith(AndroidJUnit4::class)
class HomeScreenFlowTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val db = AppDatabase.getInstance(context)
    private var failedSlotId = -1L
    private var pendingSlotId = -1L
    private var futureSlotId = -1L
    private lateinit var testImageFile: File
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        device.pressHome()

        runBlocking {
            SettingsRepository(context.settingsDataStore).markOnboardingComplete()
        }

        val now = System.currentTimeMillis()
        runBlocking {
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
            futureSlotId = db.reminderSlotDao().insert(
                ReminderSlot(
                    scheduledAt = now + 20 * 60_000L,
                    wakeStart = now - 60 * 60_000L,
                    wakeEnd = now + 2 * 60 * 60_000L,
                    bedtime = now + 4 * 60 * 60_000L,
                )
            )
        }

        testImageFile = File(context.filesDir, "meals/test_home_meal.jpg")
            .also { it.parentFile?.mkdirs() }
        val bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
        testImageFile.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 100, it) }
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

        // LockScreen timer expires quickly so the test returns to HomeScreen with SUCCESS status
        LockViewModelFactory.testFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LockViewModel(
                    slotId = capturedPendingSlotId,
                    slotDao = db.reminderSlotDao(),
                    mealLogDao = db.mealLogDao(),
                    durationMs = 300L,
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
            context.settingsDataStore.edit { it.clear() }
        }
        testImageFile.delete()
    }

    @Test
    fun homeScreen_showsSlots_logsFromHome_andShowsEmptyState() {
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // 1. HomeScreen is shown (already signed in)
        assertTrue(device.wait(Until.hasObject(By.res("home_screen")), TIMEOUT))

        // 2. Correct status chips are visible
        assertEquals("Failed", device.findObject(By.res("slot_chip_$failedSlotId")).text)
        assertEquals("Pending", device.findObject(By.res("slot_chip_$pendingSlotId")).text)
        assertEquals("Pending", device.findObject(By.res("slot_chip_$futureSlotId")).text)

        // 3. Tap the pending slot → navigate to LogMealScreen
        device.findObject(By.res("slot_row_$pendingSlotId")).click()
        assertTrue(device.wait(Until.hasObject(By.res("log_meal_screen")), TIMEOUT))

        // 4. Photo preview is pre-loaded; tap Confirm
        assertTrue(device.wait(Until.hasObject(By.res("photo_preview")), TIMEOUT))
        device.findObject(By.res("confirm_button")).click()

        // 5. Back on HomeScreen, slot chip updated to Success via live Flow
        assertTrue(device.wait(Until.hasObject(By.res("home_screen")), TIMEOUT))
        assertTrue(
            device.wait(
                Until.hasObject(By.res("slot_chip_$pendingSlotId").text("Success")),
                TIMEOUT,
            )
        )

        // 6. Clear DB → empty state appears via live Flow
        runBlocking { db.reminderSlotDao().deleteFrom(0L) }
        assertTrue(device.wait(Until.hasObject(By.res("empty_state")), TIMEOUT))
    }
}
