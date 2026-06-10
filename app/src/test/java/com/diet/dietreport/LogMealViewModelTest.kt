package com.diet.dietreport

import com.diet.dietreport.data.db.LogSource
import com.diet.dietreport.data.db.MealLog
import com.diet.dietreport.data.db.MealLogDao
import com.diet.dietreport.data.db.ReminderSlot
import com.diet.dietreport.data.db.ReminderSlotDao
import com.diet.dietreport.data.db.SlotStatus
import com.diet.dietreport.meals.LogMealViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class LogMealViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var tempDir: File

    // Fake DAOs that record calls
    private val updatedStatuses = mutableListOf<Pair<Long, String>>()
    private val insertedLogs = mutableListOf<MealLog>()
    private var fakeSlot: ReminderSlot? = null

    private val fakeSlotDao = object : ReminderSlotDao {
        override suspend fun insert(slot: ReminderSlot): Long = 1L
        override suspend fun slotsForRange(startMs: Long, endMs: Long) = emptyList<ReminderSlot>()
        override fun slotsForRangeFlow(startMs: Long, endMs: Long): Flow<List<ReminderSlot>> = flowOf(emptyList())
        override suspend fun getById(id: Long) = fakeSlot
        override suspend fun updateStatus(id: Long, status: String) {
            updatedStatuses.add(id to status)
        }
        override suspend fun deleteFrom(fromMs: Long) {}
        override suspend fun markExpiredAsFailed(cutoffMs: Long) {}
    }

    private val fakeMealLogDao = object : MealLogDao {
        override suspend fun insert(log: MealLog): Long {
            insertedLogs.add(log)
            return 1L
        }
        override suspend fun logsForSlot(slotId: Long) = emptyList<MealLog>()
        override fun logsForSlotFlow(slotId: Long): Flow<List<MealLog>> = flowOf(emptyList())
        override suspend fun offScheduleLogsForRange(startMs: Long, endMs: Long) = emptyList<MealLog>()
        override fun allLogsFlow(): Flow<List<MealLog>> = flowOf(emptyList())
        override suspend fun deleteById(id: Long) {}
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        tempDir = File(System.getProperty("java.io.tmpdir"), "test_meals_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        updatedStatuses.clear()
        insertedLogs.clear()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        tempDir.deleteRecursively()
    }

    private fun createViewModel(slotId: Long) = LogMealViewModel(
        slotId = slotId,
        slotDao = fakeSlotDao,
        mealLogDao = fakeMealLogDao,
        filesDir = tempDir,
    )

    @Test
    fun confirm_onTime_setsStatusSuccess() = runTest {
        val now = System.currentTimeMillis()
        fakeSlot = ReminderSlot(
            id = 1L,
            scheduledAt = now - 5 * 60_000L, // 5 min ago (within 30-min window)
            wakeStart = now - 3_600_000L,
            wakeEnd = now + 3_600_000L,
            bedtime = now + 7_200_000L,
        )

        val vm = createViewModel(1L)
        vm.onPhotoCaptured("/fake/path.jpg", LogSource.CAMERA)
        vm.confirm()
        advanceUntilIdle()

        assertEquals(1, updatedStatuses.size)
        assertEquals(SlotStatus.SUCCESS, updatedStatuses[0].second)
        assertTrue(vm.uiState.value.isConfirmed)
    }

    @Test
    fun confirm_late_setsStatusFailed() = runTest {
        val now = System.currentTimeMillis()
        fakeSlot = ReminderSlot(
            id = 2L,
            scheduledAt = now - 45 * 60_000L, // 45 min ago (past 30-min window)
            wakeStart = now - 3_600_000L,
            wakeEnd = now + 3_600_000L,
            bedtime = now + 7_200_000L,
        )

        val vm = createViewModel(2L)
        vm.onPhotoCaptured("/fake/path.jpg", LogSource.CAMERA)
        vm.confirm()
        advanceUntilIdle()

        assertEquals(1, updatedStatuses.size)
        assertEquals(SlotStatus.FAILED, updatedStatuses[0].second)
    }

    @Test
    fun confirm_offSchedule_doesNotUpdateSlotStatus() = runTest {
        val vm = createViewModel(0L) // slotId=0 means off-schedule
        vm.onPhotoCaptured("/fake/path.jpg", LogSource.GALLERY)
        vm.confirm()
        advanceUntilIdle()

        assertEquals(0, updatedStatuses.size)
        assertEquals(1, insertedLogs.size)
        assertEquals(null, insertedLogs[0].reminderSlotId)
        assertTrue(vm.uiState.value.isConfirmed)
        assertTrue(vm.uiState.value.isOffSchedule)
    }

    @Test
    fun confirm_insertsCorrectMealLog() = runTest {
        val now = System.currentTimeMillis()
        fakeSlot = ReminderSlot(
            id = 3L,
            scheduledAt = now,
            wakeStart = now - 3_600_000L,
            wakeEnd = now + 3_600_000L,
            bedtime = now + 7_200_000L,
        )

        val vm = createViewModel(3L)
        vm.onPhotoCaptured("/test/photo.jpg", LogSource.GALLERY)
        vm.confirm()
        advanceUntilIdle()

        assertEquals(1, insertedLogs.size)
        val log = insertedLogs[0]
        assertEquals(3L, log.reminderSlotId)
        assertEquals("/test/photo.jpg", log.photoPath)
        assertEquals(LogSource.GALLERY, log.source)
    }

    @Test
    fun confirm_noPhoto_doesNothing() = runTest {
        val vm = createViewModel(1L)
        // Don't call onPhotoCaptured
        vm.confirm()
        advanceUntilIdle()

        assertEquals(0, insertedLogs.size)
        assertEquals(0, updatedStatuses.size)
    }

    @Test
    fun statusConstants_areLowercase() {
        assertEquals("pending", SlotStatus.PENDING)
        assertEquals("success", SlotStatus.SUCCESS)
        assertEquals("failed", SlotStatus.FAILED)
    }
}
