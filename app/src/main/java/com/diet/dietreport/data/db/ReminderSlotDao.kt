package com.diet.dietreport.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderSlotDao {

    @Insert
    suspend fun insert(slot: ReminderSlot): Long

    @Query("SELECT * FROM reminder_slots WHERE scheduledAt >= :startMs AND scheduledAt < :endMs ORDER BY scheduledAt")
    suspend fun slotsForRange(startMs: Long, endMs: Long): List<ReminderSlot>

    @Query("SELECT * FROM reminder_slots WHERE scheduledAt >= :startMs AND scheduledAt < :endMs ORDER BY scheduledAt")
    fun slotsForRangeFlow(startMs: Long, endMs: Long): Flow<List<ReminderSlot>>

    @Query("SELECT * FROM reminder_slots WHERE id = :id")
    suspend fun getById(id: Long): ReminderSlot?

    @Query("UPDATE reminder_slots SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM reminder_slots WHERE scheduledAt >= :fromMs")
    suspend fun deleteFrom(fromMs: Long)
}
