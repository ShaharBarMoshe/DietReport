package com.diet.dietreport

import com.diet.dietreport.data.db.ReminderSlot
import com.diet.dietreport.data.db.SlotStatus
import com.diet.dietreport.reports.computeMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReportViewModelTest {

    private fun slotAt(hour: Int, status: String, daysAgo: Int = 0): ReminderSlot {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }
        return ReminderSlot(
            scheduledAt = cal.timeInMillis,
            wakeStart = cal.timeInMillis - 3_600_000L,
            wakeEnd = cal.timeInMillis + 3_600_000L,
            bedtime = cal.timeInMillis + 7_200_000L,
            status = status,
        )
    }

    @Test
    fun computeMetrics_emptyList_returnsZeros() {
        val data = computeMetrics(emptyList())
        assertEquals(0, data.overallPercent)
        assertEquals(0, data.overallTotal)
        assertEquals(3, data.buckets.size)
    }

    @Test
    fun computeMetrics_overallPercent() {
        val slots = listOf(
            slotAt(8, SlotStatus.SUCCESS),
            slotAt(11, SlotStatus.SUCCESS),
            slotAt(14, SlotStatus.FAILED),
            slotAt(17, SlotStatus.FAILED),
            slotAt(20, SlotStatus.PENDING),
        )
        val data = computeMetrics(slots)
        assertEquals(2, data.overallSuccess)
        assertEquals(5, data.overallTotal)
        assertEquals(40, data.overallPercent)
    }

    @Test
    fun computeMetrics_allSuccess() {
        val slots = (0 until 5).map { slotAt(8 + it, SlotStatus.SUCCESS) }
        val data = computeMetrics(slots)
        assertEquals(100, data.overallPercent)
    }

    @Test
    fun computeMetrics_byDayGrouping() {
        val slots = listOf(
            slotAt(8, SlotStatus.SUCCESS, daysAgo = 1),
            slotAt(11, SlotStatus.FAILED, daysAgo = 1),
            slotAt(8, SlotStatus.SUCCESS, daysAgo = 0),
        )
        val data = computeMetrics(slots)
        assertEquals(2, data.byDay.size)
        // day 0 = yesterday (daysAgo=1), day 1 = today (daysAgo=0), sorted ascending
        assertEquals(1, data.byDay[0].success)
        assertEquals(2, data.byDay[0].total)
        assertEquals(50, data.byDay[0].percent)
        assertEquals(1, data.byDay[1].success)
        assertEquals(1, data.byDay[1].total)
        assertEquals(100, data.byDay[1].percent)
    }

    @Test
    fun computeMetrics_byHour() {
        val slots = listOf(
            slotAt(8, SlotStatus.SUCCESS),
            slotAt(8, SlotStatus.SUCCESS),
            slotAt(11, SlotStatus.FAILED),
        )
        val data = computeMetrics(slots)
        assertEquals(2, data.byHour.size)
        assertEquals(8, data.byHour[0].hour)
        assertEquals(100, data.byHour[0].percent)
        assertEquals(11, data.byHour[1].hour)
        assertEquals(0, data.byHour[1].percent)
    }

    @Test
    fun computeMetrics_buckets_morningAfternoonEvening() {
        val slots = listOf(
            slotAt(8, SlotStatus.SUCCESS),   // morning
            slotAt(9, SlotStatus.SUCCESS),   // morning
            slotAt(14, SlotStatus.SUCCESS),  // afternoon
            slotAt(16, SlotStatus.FAILED),   // afternoon
            slotAt(20, SlotStatus.FAILED),   // evening
        )
        val data = computeMetrics(slots)
        val morning = data.buckets[0]
        val afternoon = data.buckets[1]
        val evening = data.buckets[2]
        assertEquals("Morning", morning.label)
        assertEquals(100, morning.percent)
        assertEquals("Afternoon", afternoon.label)
        assertEquals(50, afternoon.percent)
        assertEquals("Evening", evening.label)
        assertEquals(0, evening.percent)
    }

    @Test
    fun computeMetrics_morningBucketGreaterThanEvening() {
        val slots = listOf(
            slotAt(8, SlotStatus.SUCCESS),
            slotAt(20, SlotStatus.FAILED),
        )
        val data = computeMetrics(slots)
        assertTrue(data.buckets[0].percent > data.buckets[2].percent)
    }

    @Test
    fun computeMetrics_pendingNotCountedAsSuccess() {
        val slots = listOf(
            slotAt(8, SlotStatus.PENDING),
            slotAt(9, SlotStatus.PENDING),
        )
        val data = computeMetrics(slots)
        assertEquals(0, data.overallSuccess)
        assertEquals(0, data.overallPercent)
    }
}
