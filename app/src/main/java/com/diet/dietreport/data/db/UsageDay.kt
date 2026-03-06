package com.diet.dietreport.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_days")
data class UsageDay(
    @PrimaryKey val date: String,   // ISO date "YYYY-MM-DD"
    val inferredWake: Long,         // epoch millis
    val inferredBedtime: Long,      // epoch millis
)
