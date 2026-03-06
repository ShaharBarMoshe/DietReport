package com.diet.dietreport.auth

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.diet.dietreport.auth.data.AuthRepository
import com.diet.dietreport.auth.data.authDataStore
import com.diet.dietreport.settings.data.SettingsRepository
import com.diet.dietreport.settings.data.settingsDataStore
import kotlinx.coroutines.flow.first

object AuthViewModelFactory {

    /**
     * Set this in tests (before the Activity launches) to inject fake dependencies.
     */
    var testFactory: ViewModelProvider.Factory? = null

    fun create(application: Application): ViewModelProvider.Factory =
        testFactory ?: object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AuthViewModel(
                    CredentialManagerAuthService(),
                    AuthRepository(application.authDataStore),
                    isOnboardingComplete = {
                        SettingsRepository(application.settingsDataStore).isOnboardingComplete.first()
                    },
                ) as T
        }
}
