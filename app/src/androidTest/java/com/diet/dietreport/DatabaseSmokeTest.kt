package com.diet.dietreport

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.diet.dietreport.data.db.AppDatabase
import com.diet.dietreport.data.db.LogSource
import com.diet.dietreport.data.db.MealLog
import com.diet.dietreport.data.db.ReminderSlot
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
class DatabaseSmokeTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun databaseSmoke_happyFlow() = runBlocking {
        val slotDao = db.reminderSlotDao()
        val logDao = db.mealLogDao()
        val reportDao = db.reportDao()

        // Compute start/end of today
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayStart = cal.timeInMillis
        val dayEnd = dayStart + 24L * 60 * 60 * 1000

        // Place slots at fixed times today (9 AM, noon, 3 PM) to avoid midnight edge cases
        val slot0At = dayStart + 9 * 3600_000L
        val slot1At = dayStart + 12 * 3600_000L
        val slot2At = dayStart + 15 * 3600_000L

        val makeSlot = { scheduledAt: Long ->
            ReminderSlot(
                scheduledAt = scheduledAt,
                wakeStart = dayStart + 7 * 3600_000L,
                wakeEnd = dayStart + 21 * 3600_000L,
                bedtime = dayStart + 23 * 3600_000L,
            )
        }

        // 1. Insert 3 ReminderSlots for today
        val slot0Id = slotDao.insert(makeSlot(slot0At))
        val slot1Id = slotDao.insert(makeSlot(slot1At))
        slotDao.insert(makeSlot(slot2At))

        // 2. Query slots for today → assert 3 results
        val slots = slotDao.slotsForRange(dayStart, dayEnd)
        assertEquals(3, slots.size)

        // 3. Insert MealLog for slot[0] with loggedAt = scheduledAt + 10 min (within window)
        logDao.insert(
            MealLog(
                reminderSlotId = slot0Id,
                photoPath = "/test/photo0.jpg",
                loggedAt = slot0At + 10 * 60_000L,
                source = LogSource.CAMERA,
            )
        )

        // 4. Query MealLog for slot[0] → assert 1 result, source matches
        val logs = logDao.logsForSlot(slot0Id)
        assertEquals(1, logs.size)
        assertEquals(LogSource.CAMERA, logs[0].source)

        // 5. Query success % for today → assert 33 % (1 of 3)
        assertEquals(33, reportDao.successPercentForRange(dayStart, dayEnd))

        // 6. Insert MealLog for slot[1] with loggedAt = scheduledAt + 35 min (outside 30-min window)
        logDao.insert(
            MealLog(
                reminderSlotId = slot1Id,
                photoPath = "/test/photo1.jpg",
                loggedAt = slot1At + 35 * 60_000L,
                source = LogSource.GALLERY,
            )
        )

        // 7. Query success % for today → still 33 % (late log doesn't count as success)
        assertEquals(33, reportDao.successPercentForRange(dayStart, dayEnd))
    }
}
