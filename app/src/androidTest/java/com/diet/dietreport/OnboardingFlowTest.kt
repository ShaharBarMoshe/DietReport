package com.diet.dietreport

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.diet.dietreport.auth.AuthViewModel
import com.diet.dietreport.auth.AuthViewModelFactory
import com.diet.dietreport.auth.data.AuthRepository
import com.diet.dietreport.auth.data.User
import com.diet.dietreport.auth.data.authDataStore
import com.diet.dietreport.settings.data.SettingsRepository
import com.diet.dietreport.settings.data.settingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val TIMEOUT = 10_000L

@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val presetUser = User("uid-onboard", "onboard@example.com", "Onboard User")
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        device.pressHome()
        // Clear all persisted data for fresh-install state
        runBlocking {
            context.authDataStore.edit { it.clear() }
            context.settingsDataStore.edit { it.clear() }
        }
        // Inject fake auth so we bypass the real Google dialog
        AuthViewModelFactory.testFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AuthViewModel(
                    FakeAuthService(presetUser),
                    AuthRepository(context.authDataStore),
                    isOnboardingComplete = {
                        SettingsRepository(context.settingsDataStore).isOnboardingComplete.first()
                    },
                ) as T
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        scenario = null
        AuthViewModelFactory.testFactory = null
        runBlocking {
            context.authDataStore.edit { it.clear() }
            context.settingsDataStore.edit { it.clear() }
        }
    }

    @Test
    fun onboarding_firstSignIn_landOnSettings_thenHome_thenDirectHome() {
        // 1-2. Fresh install → SignInScreen
        scenario = ActivityScenario.launch(MainActivity::class.java)
        assertTrue(device.wait(Until.hasObject(By.res("sign_in_screen")), TIMEOUT))

        // 3. Sign in → should land on SettingsScreen (not HomeScreen)
        device.findObject(By.desc("Sign in with Google")).click()
        assertTrue(
            "Expected SettingsScreen after first sign-in",
            device.wait(Until.hasObject(By.res("settings_screen")), TIMEOUT),
        )
        assertTrue(
            "HomeScreen must NOT appear during onboarding",
            !device.hasObject(By.res("home_screen")),
        )

        // 4. Fill settings and tap Save → should navigate to HomeScreen
        // Fields already have defaults so we can save immediately
        device.findObject(By.res("save_button")).click()
        assertTrue(
            "Expected HomeScreen after saving settings during onboarding",
            device.wait(Until.hasObject(By.res("home_screen")), TIMEOUT),
        )

        // 5. Kill and relaunch → HomeScreen directly (onboarding complete)
        scenario?.close()
        scenario = null
        scenario = ActivityScenario.launch(MainActivity::class.java)
        assertTrue(
            "Expected HomeScreen on relaunch after onboarding complete",
            device.wait(Until.hasObject(By.res("home_screen")), TIMEOUT),
        )
        assertTrue(
            "SettingsScreen must NOT appear on subsequent launch",
            !device.hasObject(By.res("settings_screen")),
        )
    }
}
