package com.diet.dietreport.lock

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.diet.dietreport.data.db.AppDatabase

object LockViewModelFactory {

    var testFactory: ViewModelProvider.Factory? = null

    fun create(context: Context, slotId: Long): ViewModelProvider.Factory =
        testFactory ?: object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(context)
                return LockViewModel(
                    slotId = slotId,
                    slotDao = db.reminderSlotDao(),
                    mealLogDao = db.mealLogDao(),
                ) as T
            }
        }
}
