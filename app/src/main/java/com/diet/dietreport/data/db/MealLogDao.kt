package com.diet.dietreport.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MealLogDao {

    @Insert
    suspend fun insert(log: MealLog): Long

    @Query("SELECT * FROM meal_logs WHERE reminderSlotId = :slotId ORDER BY loggedAt")
    suspend fun logsForSlot(slotId: Long): List<MealLog>

    @Query("SELECT * FROM meal_logs WHERE reminderSlotId = :slotId ORDER BY loggedAt")
    fun logsForSlotFlow(slotId: Long): Flow<List<MealLog>>

    @Query("SELECT * FROM meal_logs WHERE reminderSlotId IS NULL AND loggedAt >= :startMs AND loggedAt < :endMs ORDER BY loggedAt")
    suspend fun offScheduleLogsForRange(startMs: Long, endMs: Long): List<MealLog>

    @Query("SELECT * FROM meal_logs ORDER BY loggedAt DESC")
    fun allLogsFlow(): Flow<List<MealLog>>

    @Query("DELETE FROM meal_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        DELETE FROM meal_logs
        WHERE reminderSlotId IN (
            SELECT id FROM reminder_slots
            WHERE scheduledAt >= :startMs AND scheduledAt < :endMs
        )
    """)
    suspend fun deleteForSlotRange(startMs: Long, endMs: Long)
}
