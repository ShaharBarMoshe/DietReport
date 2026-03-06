package com.diet.dietreport

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.diet.dietreport.data.db.AppDatabase
import com.diet.dietreport.data.db.ReminderSlot
import com.diet.dietreport.data.db.SlotStatus
import com.diet.dietreport.lock.LockViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LockViewModelTest {

    private lateinit var db: AppDatabase
    private var slotId = -1L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

        val scheduledAt = System.currentTimeMillis() - 5 * 60_000L
        slotId = runBlocking {
            db.reminderSlotDao().insert(
                ReminderSlot(
                    scheduledAt = scheduledAt,
                    wakeStart = scheduledAt - 60_000L,
                    wakeEnd = scheduledAt + 2 * 60 * 60_000L,
                    bedtime = scheduledAt + 4 * 60 * 60_000L,
                    status = SlotStatus.SUCCESS,
                )
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun timerExpiry_doneBecomesTrueAndStatusUnchanged() {
        val vm = LockViewModel(
            slotId = slotId,
            slotDao = db.reminderSlotDao(),
            mealLogDao = db.mealLogDao(),
            durationMs = 200L, // expires quickly
        )

        // Wait for timer to expire and coroutine to complete
        Thread.sleep(2_000L)

        assertTrue(vm.uiState.value.done)
        assertNull(vm.uiState.value.error)
        // Status must still be SUCCESS — timer expiry does not change it
        val slot = runBlocking { db.reminderSlotDao().getById(slotId) }
        assertEquals(SlotStatus.SUCCESS, slot?.status)
    }

    @Test
    fun onUnlockEarly_setsSlotFailedAndDone() {
        val vm = LockViewModel(
            slotId = slotId,
            slotDao = db.reminderSlotDao(),
            mealLogDao = db.mealLogDao(),
            durationMs = 60_000L,
        )

        vm.onUnlockEarly()
        Thread.sleep(1_000L)

        assertTrue(vm.uiState.value.done)
        val slot = runBlocking { db.reminderSlotDao().getById(slotId) }
        assertEquals(SlotStatus.FAILED, slot?.status)
    }

    @Test
    fun onAppBackgrounded_setsSlotFailedAndDone() {
        val vm = LockViewModel(
            slotId = slotId,
            slotDao = db.reminderSlotDao(),
            mealLogDao = db.mealLogDao(),
            durationMs = 60_000L,
        )

        vm.onAppBackgrounded()
        Thread.sleep(1_000L)

        assertTrue(vm.uiState.value.done)
        val slot = runBlocking { db.reminderSlotDao().getById(slotId) }
        assertEquals(SlotStatus.FAILED, slot?.status)
    }

    @Test
    fun multipleCallsToMarkFailed_idempotent() {
        val vm = LockViewModel(
            slotId = slotId,
            slotDao = db.reminderSlotDao(),
            mealLogDao = db.mealLogDao(),
            durationMs = 60_000L,
        )

        // Call multiple times in quick succession
        vm.onUnlockEarly()
        vm.onUnlockEarly()
        vm.onAppBackgrounded()
        vm.onAppBackgrounded()
        vm.onUnlockEarly()

        Thread.sleep(1_000L)

        // done = true and no error (no duplicate DB write issues)
        assertTrue(vm.uiState.value.done)
        assertNull(vm.uiState.value.error)
        val slot = runBlocking { db.reminderSlotDao().getById(slotId) }
        assertEquals(SlotStatus.FAILED, slot?.status)
    }
}
