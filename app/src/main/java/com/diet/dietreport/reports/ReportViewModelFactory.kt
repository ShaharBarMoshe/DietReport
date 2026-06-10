package com.diet.dietreport.reports

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.diet.dietreport.data.db.AppDatabase

class ReportViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val db = AppDatabase.getInstance(context)
    private val repo = object : ReportRepository {
        override suspend fun slotsForRange(startMs: Long, endMs: Long) =
            db.reminderSlotDao().slotsForRange(startMs, endMs)
        override suspend fun offScheduleLogsForRange(startMs: Long, endMs: Long) =
            db.mealLogDao().offScheduleLogsForRange(startMs, endMs)
        override suspend fun clearLastWeek(startMs: Long, endMs: Long) {
            db.mealLogDao().deleteForSlotRange(startMs, endMs)
            db.reminderSlotDao().resetStatusForRange(startMs, endMs)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ReportViewModel(repo) as T
}
