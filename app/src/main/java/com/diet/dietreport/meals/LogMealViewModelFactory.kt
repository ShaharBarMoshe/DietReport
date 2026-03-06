package com.diet.dietreport.meals

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.diet.dietreport.data.db.AppDatabase

object LogMealViewModelFactory {

    var testFactory: ViewModelProvider.Factory? = null

    fun create(context: Context, slotId: Long): ViewModelProvider.Factory =
        testFactory ?: object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LogMealViewModel(
                    slotId = slotId,
                    slotDao = AppDatabase.getInstance(context).reminderSlotDao(),
                    mealLogDao = AppDatabase.getInstance(context).mealLogDao(),
                    filesDir = context.filesDir,
                ) as T
        }
}
