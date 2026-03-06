package com.diet.dietreport

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.diet.dietreport.data.db.AppDatabase
import com.diet.dietreport.data.db.ReminderSlot
import com.diet.dietreport.data.db.SlotStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

private const val TIMEOUT = 10_000L

@RunWith(AndroidJUnit4::class)
class ReportFlowTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val db = AppDatabase.getInstance(context)
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        device.pressHome()

        runBlocking { db.reminderSlotDao().deleteFrom(0L) }

        // Seed 7 days × 5 slots at hours 8, 11, 14, 17, 20
        // Hours 8, 11, 14 → SUCCESS (21 total = 60% of 35)
        // Hours 17, 20 → PENDING (not success)
        runBlocking {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()
            for (daysAgo in 6 downTo 0) {
                for ((hour, status) in listOf(
                    8 to SlotStatus.SUCCESS,
                    11 to SlotStatus.SUCCESS,
                    14 to SlotStatus.SUCCESS,
                    17 to SlotStatus.PENDING,
                    20 to SlotStatus.PENDING,
                )) {
                    cal.timeInMillis = now
                    cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val scheduledAt = cal.timeInMillis
                    db.reminderSlotDao().insert(
                        ReminderSlot(
                            scheduledAt = scheduledAt,
                            wakeStart = scheduledAt - 60 * 60_000L,
                            wakeEnd = scheduledAt + 60 * 60_000L,
                            bedtime = scheduledAt + 2 * 60 * 60_000L,
                            status = status,
                        )
                    )
                }
            }
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        runBlocking { db.reminderSlotDao().deleteFrom(0L) }
    }

    @Test
    fun reportScreen_showsCorrectStats_andShareOpens() {
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Navigate to the Report tab
        assertTrue(device.wait(Until.hasObject(By.res("home_screen")), TIMEOUT))
        assertTrue(device.wait(Until.hasObject(By.text("Report")), TIMEOUT))
        device.findObject(By.text("Report")).click()
        assertTrue(device.wait(Until.hasObject(By.res("report_screen")), TIMEOUT))

        // Overall = 21/35 = 60%
        assertTrue(device.wait(Until.hasObject(By.res("overall_percent")), TIMEOUT))
        assertEquals("60%", device.findObject(By.res("overall_percent")).text)

        // Each day has 3/5 = 60%
        assertEquals("60%", device.findObject(By.res("day_row_0")).text)

        // Scroll down to reveal bucket rows (they may be off-screen in the verticalScroll column)
        device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 1.0f)

        // Morning bucket (hours 8, 11) > Evening bucket (hour 20)
        assertTrue(device.wait(Until.hasObject(By.res("bucket_morning_pct")), TIMEOUT))
        val morningPct = device.findObject(By.res("bucket_morning_pct")).text
            .removeSuffix("%").toInt()
        val eveningPct = device.findObject(By.res("bucket_evening_pct")).text
            .removeSuffix("%").toInt()
        assertTrue("Morning ($morningPct%) should be > Evening ($eveningPct%)", morningPct > eveningPct)

        // Tap Share → chooser appears as a new window (button is outside the scroll)
        assertTrue(device.wait(Until.hasObject(By.res("share_button")), TIMEOUT))
        device.findObject(By.res("share_button")).click()
        assertTrue("Share chooser did not appear", device.waitForWindowUpdate(null, TIMEOUT))
        device.pressBack()

        // Back on report screen
        assertTrue(device.wait(Until.hasObject(By.res("report_screen")), TIMEOUT))
    }
}
