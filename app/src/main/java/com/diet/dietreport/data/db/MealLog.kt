package com.diet.dietreport.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object LogSource {
    const val CAMERA = "camera"
    const val GALLERY = "gallery"
}

@Entity(
    tableName = "meal_logs",
    foreignKeys = [
        ForeignKey(
            entity = ReminderSlot::class,
            parentColumns = ["id"],
            childColumns = ["reminderSlotId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("reminderSlotId")],
)
data class MealLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderSlotId: Long,
    val photoPath: String,
    val loggedAt: Long,
    val source: String,
)
