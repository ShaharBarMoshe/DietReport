package com.diet.dietreport.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

object SlotStatus {
    const val PENDING = "pending"
    const val SUCCESS = "success"
    const val FAILED = "failed"
}

@Entity(tableName = "reminder_slots")
data class ReminderSlot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduledAt: Long,
    val wakeStart: Long,
    val wakeEnd: Long,
    val bedtime: Long,
    val status: String = SlotStatus.PENDING,
)
