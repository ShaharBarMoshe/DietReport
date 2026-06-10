package com.diet.dietreport.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ReminderSlot::class, MealLog::class, UsageDay::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reminderSlotDao(): ReminderSlotDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun reportDao(): ReportDao
    abstract fun usageDayDao(): UsageDayDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /** V1→V2: Make meal_logs.reminderSlotId nullable for off-schedule meals. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE meal_logs_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        reminderSlotId INTEGER,
                        photoPath TEXT NOT NULL,
                        loggedAt INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        FOREIGN KEY (reminderSlotId) REFERENCES reminder_slots(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO meal_logs_new SELECT * FROM meal_logs")
                db.execSQL("DROP TABLE meal_logs")
                db.execSQL("ALTER TABLE meal_logs_new RENAME TO meal_logs")
                db.execSQL("CREATE INDEX index_meal_logs_reminderSlotId ON meal_logs(reminderSlotId)")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diet_report.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
    }
}
