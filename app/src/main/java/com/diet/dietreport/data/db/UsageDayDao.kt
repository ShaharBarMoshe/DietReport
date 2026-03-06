package com.diet.dietreport.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UsageDayDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(day: UsageDay)

    @Query("SELECT * FROM usage_days ORDER BY date DESC LIMIT :limit")
    suspend fun recentDays(limit: Int): List<UsageDay>
}
