package com.diet.dietreport.reports

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.diet.dietreport.data.db.AppDatabase

class ReportViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val repo = ReportRepository { start, end ->
        AppDatabase.getInstance(context).reminderSlotDao().slotsForRange(start, end)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ReportViewModel(repo) as T
}
