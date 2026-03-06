package com.diet.dietreport

import com.diet.dietreport.reminders.SlotComputer
import org.junit.Assert.assertEquals
import org.junit.Test

class SlotComputerTest {

    // Fixed epoch anchor — avoids real-time dependency
    private val T = 1_700_000_000_000L
    private val H = 3_600_000L  // 1 hour in ms
    private val M = 60_000L     // 1 minute in ms

    @Test
    fun noDelay_wake7_bed23_produces5Slots() {
        // cutoff = 23H - 2H = 21H
        // slots: 7H, 10H, 13H, 16H, 19H  (19H+3H=22H > 21H → stop)
        val slots = SlotComputer.computeSlots(T + 7 * H, 0L, T + 23 * H)
        assertEquals(5, slots.size)
        assertEquals(T + 7 * H, slots[0].scheduledAt)
        assertEquals(T + 10 * H, slots[1].scheduledAt)
        assertEquals(T + 13 * H, slots[2].scheduledAt)
        assertEquals(T + 16 * H, slots[3].scheduledAt)
        assertEquals(T + 19 * H, slots[4].scheduledAt)
    }

    @Test
    fun delay60min_shiftsFirstSlot() {
        // wake=7H, delay=60min → first slot = 8H
        // cutoff=21H; slots: 8H, 11H, 14H, 17H, 20H (20+3=23 > 21 → stop)
        val slots = SlotComputer.computeSlots(T + 7 * H, 60 * M, T + 23 * H)
        assertEquals(5, slots.size)
        assertEquals(T + 8 * H, slots[0].scheduledAt)
        assertEquals(T + 20 * H, slots[4].scheduledAt)
    }

    @Test
    fun wakeAndBed4hApart_delay0_produces1Slot() {
        // wake=T, bed=T+4H, cutoff=T+2H
        // first=T+0=T ≤ T+2H → slot; next=T+3H > T+2H → stop
        val slots = SlotComputer.computeSlots(T, 0L, T + 4 * H)
        assertEquals(1, slots.size)
        assertEquals(T, slots[0].scheduledAt)
    }

    @Test
    fun slotAtExactCutoffIsIncluded() {
        // first slot = cutoff exactly → should be included
        // wake=21H, delay=0, bed=23H → cutoff=21H, first=21H ≤ 21H → 1 slot
        val slots = SlotComputer.computeSlots(T + 21 * H, 0L, T + 23 * H)
        assertEquals(1, slots.size)
        assertEquals(T + 21 * H, slots[0].scheduledAt)
    }

    @Test
    fun delayPushesFirstSlotBeyondCutoff_noSlots() {
        // wake=20H, delay=2H → first=22H; cutoff=21H → 22H > 21H → no slots
        val slots = SlotComputer.computeSlots(T + 20 * H, 2 * H, T + 23 * H)
        assertEquals(0, slots.size)
    }

    @Test
    fun wakeSameBedtime_noSlots() {
        // wake=bed=10H → cutoff=8H, first=10H > 8H → no slots
        val slots = SlotComputer.computeSlots(T + 10 * H, 0L, T + 10 * H)
        assertEquals(0, slots.size)
    }

    @Test
    fun slotsHaveCorrectMetadata() {
        val wake = T + 7 * H
        val bed = T + 23 * H
        val cutoff = bed - SlotComputer.STOP_BEFORE_BED_MS
        val slots = SlotComputer.computeSlots(wake, 0L, bed)
        slots.forEach {
            assertEquals(wake, it.wakeStart)
            assertEquals(cutoff, it.wakeEnd)
            assertEquals(bed, it.bedtime)
        }
    }
}
