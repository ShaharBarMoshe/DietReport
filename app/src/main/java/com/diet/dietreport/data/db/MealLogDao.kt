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
}
