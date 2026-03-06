package com.diet.dietreport.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.diet.dietreport.settings.data.SettingsRepository
import com.diet.dietreport.settings.data.settingsDataStore

object SettingsViewModelFactory {

    var testFactory: ViewModelProvider.Factory? = null

    fun create(application: Application): ViewModelProvider.Factory =
        testFactory ?: object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(SettingsRepository(application.settingsDataStore)) as T
        }
}
