package com.diet.dietreport

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.diet.dietreport.data.db.AppDatabase
import com.diet.dietreport.data.db.SlotStatus
import com.diet.dietreport.reminders.NotificationHelper
import com.diet.dietreport.reminders.ReminderScheduler
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

private const val NOTIF_TIMEOUT = 90_000L

@RunWith(AndroidJUnit4::class)
class ReminderEngineTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private lateinit var db: AppDatabase
    private lateinit var scheduler: ReminderScheduler

    @Before
    fun setUp() {
        // Grant POST_NOTIFICATIONS (runtime permission — grantable via pm)
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(
                "pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS"
            ).close()

        // Grant SCHEDULE_EXACT_ALARM via appops so setExactAndAllowWhileIdle works in tests
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.executeShellCommand("appops set ${context.packageName} SCHEDULE_EXACT_ALARM allow").close()
        uiAutomation.executeShellCommand("appops set --uid ${context.packageName} SCHEDULE_EXACT_ALARM allow").close()
        Thread.sleep(500) // let appops grant propagate

        // Create notification channel so the notification can be shown
        NotificationHelper.createChannel(context)

        // Isolated in-memory DB — scheduler writes here; ReminderReceiver uses intent extras only
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scheduler = ReminderScheduler(context, db.reminderSlotDao())
    }

    @After
    fun tearDown() {
        db.close()
        device.pressBack() // close notification shade so subsequent tests see the activity
    }

    @Test
    fun reminderEngine_happyFlow() = runBlocking {
        val now = System.currentTimeMillis()

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayStart = cal.timeInMillis
        val dayEnd = dayStart + 24L * 60 * 60 * 1000

        // 1. Set wake = now, delay = 0, bed = now + 4h → forces exactly 1 slot at "now"
        val wakeMs = now
        val delayMs = 0L
        val bedtimeMs = now + 4L * 60 * 60 * 1000

        // 2. Schedule slots
        scheduler.scheduleForDay(wakeMs, delayMs, bedtimeMs, fromMs = now)

        // 3. Assert 1 ReminderSlot inserted with status = pending
        val slots = db.reminderSlotDao().slotsForRange(dayStart, dayEnd)
        assertEquals(1, slots.size)
        assertEquals(SlotStatus.PENDING, slots[0].status)

        // 4. Open notification shade and wait for the reminder notification (max 90s)
        device.wakeUp()
        device.openNotification()
        assertTrue(
            "Expected reminder notification within ${NOTIF_TIMEOUT / 1000}s",
            device.wait(Until.hasObject(By.text("Time to log your meal")), NOTIF_TIMEOUT),
        )

        // 5. Assert notification title is visible
        assertTrue(device.hasObject(By.text("Time to log your meal")))

        // 6. Assert "Log meal" and "Snooze" action buttons are present
        assertTrue(device.hasObject(By.text("Log meal")))
        assertTrue(device.hasObject(By.text("Snooze")))
    }
}
