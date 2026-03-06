package com.diet.dietreport.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.diet.dietreport.data.db.AppDatabase
import com.diet.dietreport.reminders.ReminderScheduler
import com.diet.dietreport.settings.data.SettingsRepository
import com.diet.dietreport.settings.data.settingsDataStore

object SettingsViewModelFactory {

    var testFactory: ViewModelProvider.Factory? = null

    fun create(application: Application): ViewModelProvider.Factory =
        testFactory ?: object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(
                    repository = SettingsRepository(application.settingsDataStore),
                    scheduler = ReminderScheduler(
                        application,
                        AppDatabase.getInstance(application).reminderSlotDao(),
                    ),
                ) as T
        }
}
