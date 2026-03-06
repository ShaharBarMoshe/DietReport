package com.diet.dietreport.data.db

import androidx.room.Dao
import androidx.room.Query

private const val SUCCESS_WINDOW_MS = 30 * 60 * 1000L  // 30 minutes

@Dao
interface ReportDao {

    /**
     * Returns the percentage of reminder slots in [startMs, endMs) that have at least one
     * meal log recorded within 30 minutes of their scheduled time.
     * Returns 0 if no slots exist in the range.
     */
    @Query("""
        SELECT
            CASE WHEN COUNT(rs.id) = 0 THEN 0
            ELSE SUM(CASE WHEN (
                SELECT COUNT(*) FROM meal_logs ml
                WHERE ml.reminderSlotId = rs.id
                AND ml.loggedAt <= rs.scheduledAt + 1800000
            ) > 0 THEN 1 ELSE 0 END) * 100 / COUNT(rs.id)
            END
        FROM reminder_slots rs
        WHERE rs.scheduledAt >= :startMs AND rs.scheduledAt < :endMs
    """)
    suspend fun successPercentForRange(startMs: Long, endMs: Long): Int
}
