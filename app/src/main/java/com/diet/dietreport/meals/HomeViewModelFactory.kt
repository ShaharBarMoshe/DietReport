package com.diet.dietreport.meals

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.diet.dietreport.data.db.AppDatabase

class HomeViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val slotDao = AppDatabase.getInstance(context).reminderSlotDao()

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HomeViewModel(slotDao) as T
}
