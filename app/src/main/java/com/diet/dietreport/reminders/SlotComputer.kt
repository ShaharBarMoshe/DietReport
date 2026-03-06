package com.diet.dietreport.reminders

data class SlotTime(
    val scheduledAt: Long,
    val wakeStart: Long,
    val wakeEnd: Long,
    val bedtime: Long,
)

object SlotComputer {
    const val INTERVAL_MS = 3L * 60 * 60 * 1000       // 3 hours
    const val STOP_BEFORE_BED_MS = 2L * 60 * 60 * 1000 // 2 hours

    /**
     * Computes reminder slots for a single day.
     *
     * @param wakeMs    Wake-up time in epoch millis.
     * @param delayMs   First-meal delay offset in millis.
     * @param bedtimeMs Bedtime in epoch millis.
     * @return Ordered list of slots from (wakeMs + delayMs) every 3h until (bedtimeMs - 2h).
     */
    fun computeSlots(wakeMs: Long, delayMs: Long, bedtimeMs: Long): List<SlotTime> {
        val cutoff = bedtimeMs - STOP_BEFORE_BED_MS
        val slots = mutableListOf<SlotTime>()
        var t = wakeMs + delayMs
        while (t <= cutoff) {
            slots += SlotTime(
                scheduledAt = t,
                wakeStart = wakeMs,
                wakeEnd = cutoff,
                bedtime = bedtimeMs,
            )
            t += INTERVAL_MS
        }
        return slots
    }
}
