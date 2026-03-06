package com.diet.dietreport

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.diet.dietreport.data.db.AppDatabase
import com.diet.dietreport.data.db.ReminderSlot
import com.diet.dietreport.data.db.SlotStatus
import com.diet.dietreport.settings.data.SettingsRepository
import com.diet.dietreport.settings.data.settingsDataStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TIMEOUT = 10_000L

@RunWith(AndroidJUnit4::class)
class NextMealBannerTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val db = AppDatabase.getInstance(context)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        device.pressHome()
        runBlocking {
            db.reminderSlotDao().deleteFrom(0L)
            context.settingsDataStore.edit { it.clear() }
            SettingsRepository(context.settingsDataStore).markOnboardingComplete()
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        runBlocking {
            db.reminderSlotDao().deleteFrom(0L)
            context.settingsDataStore.edit { it.clear() }
        }
    }

    @Test
    fun futurePendingSlot_showsNextMealAtTime() {
        val now = System.currentTimeMillis()
        val scheduledAt = now + 90 * 60_000L // 90 min from now
        runBlocking {
            db.reminderSlotDao().insert(
                ReminderSlot(
                    scheduledAt = scheduledAt,
                    wakeStart = now - 60 * 60_000L,
                    wakeEnd = now + 4 * 60 * 60_000L,
                    bedtime = now + 8 * 60 * 60_000L,
                )
            )
        }

        scenario = ActivityScenario.launch(MainActivity::class.java)
        assertTrue(device.wait(Until.hasObject(By.res("home_screen")), TIMEOUT))

        // Banner is visible
        assertTrue(
            "Expected next_meal_banner to be visible",
            device.wait(Until.hasObject(By.res("next_meal_banner")), TIMEOUT),
        )

        // Banner text shows the scheduled time
        val expectedTime = timeFormat.format(Date(scheduledAt))
        assertTrue(
            "Expected banner text to contain 'Next meal at $expectedTime'",
            device.hasObject(By.res("next_meal_banner_text").textContains("Next meal at $expectedTime")),
        )
    }

    @Test
    fun pendingSlotWithinSuccessWindow_showsLogNowMessage() {
        val now = System.currentTimeMillis()
        val scheduledAt = now - 10 * 60_000L // 10 min ago (within 30-min window)
        runBlocking {
            db.reminderSlotDao().insert(
                ReminderSlot(
                    scheduledAt = scheduledAt,
                    wakeStart = now - 60 * 60_000L,
                    wakeEnd = now + 4 * 60 * 60_000L,
                    bedtime = now + 8 * 60 * 60_000L,
                )
            )
        }

        scenario = ActivityScenario.launch(MainActivity::class.java)
        assertTrue(device.wait(Until.hasObject(By.res("home_screen")), TIMEOUT))

        // Banner is visible
        assertTrue(
            "Expected next_meal_banner to be visible",
            device.wait(Until.hasObject(By.res("next_meal_banner")), TIMEOUT),
        )

        // Banner text shows urgent message
        assertTrue(
            "Expected banner text 'Log your meal now!'",
            device.hasObject(By.res("next_meal_banner_text").text("Log your meal now!")),
        )
    }

    @Test
    fun noPendingSlots_bannerIsHidden() {
        val now = System.currentTimeMillis()
        runBlocking {
            db.reminderSlotDao().insert(
                ReminderSlot(
                    scheduledAt = now - 60 * 60_000L,
                    wakeStart = now - 120 * 60_000L,
                    wakeEnd = now + 60 * 60_000L,
                    bedtime = now + 4 * 60 * 60_000L,
                    status = SlotStatus.SUCCESS,
                )
            )
            db.reminderSlotDao().insert(
                ReminderSlot(
                    scheduledAt = now - 30 * 60_000L,
                    wakeStart = now - 120 * 60_000L,
                    wakeEnd = now + 60 * 60_000L,
                    bedtime = now + 4 * 60 * 60_000L,
                    status = SlotStatus.FAILED,
                )
            )
        }

        scenario = ActivityScenario.launch(MainActivity::class.java)
        assertTrue(device.wait(Until.hasObject(By.res("home_screen")), TIMEOUT))
        Thread.sleep(500) // let UI settle

        // Banner should not be visible
        assertFalse(
            "Expected next_meal_banner to be hidden when no pending slots",
            device.hasObject(By.res("next_meal_banner")),
        )
    }
}
