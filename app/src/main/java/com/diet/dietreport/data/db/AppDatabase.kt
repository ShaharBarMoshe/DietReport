package com.diet.dietreport.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ReminderSlot::class, MealLog::class, UsageDay::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reminderSlotDao(): ReminderSlotDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun reportDao(): ReportDao
    abstract fun usageDayDao(): UsageDayDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diet_report.db",
                ).build().also { INSTANCE = it }
            }
    }
}
