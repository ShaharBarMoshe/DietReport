package com.diet.dietreport

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.diet.dietreport.auth.data.AuthRepository
import com.diet.dietreport.auth.data.User
import com.diet.dietreport.auth.data.authDataStore
import com.diet.dietreport.settings.data.settingsDataStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val TIMEOUT = 10_000L

@RunWith(AndroidJUnit4::class)
class PermissionDegradationTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        device.pressHome()
        SchedulerErrorBus.clear()

        // Pre-sign-in and mark onboarding complete so we start on HomeScreen
        runBlocking {
            context.authDataStore.edit { it.clear() }
            AuthRepository(context.authDataStore).saveUser(
                User("uid-perm-test", "perm@example.com", "Perm Test User")
            )
            // Mark onboarding complete so startup routes to Home
            context.settingsDataStore.edit { prefs ->
                prefs[androidx.datastore.preferences.core.booleanPreferencesKey("onboarding_complete")] = true
            }
        }

        // Revoke SCHEDULE_EXACT_ALARM before launch so onResume() detects it
        automation.executeShellCommand(
            "appops set ${context.packageName} SCHEDULE_EXACT_ALARM deny"
        ).close()
        Thread.sleep(500)
    }

    @After
    fun tearDown() {
        scenario?.close()
        scenario = null
        SchedulerErrorBus.clear()
        // Restore permission
        automation.executeShellCommand(
            "appops set ${context.packageName} SCHEDULE_EXACT_ALARM allow"
        ).close()
        Thread.sleep(500)
        runBlocking {
            context.authDataStore.edit { it.clear() }
            context.settingsDataStore.edit { it.clear() }
        }
    }

    @Test
    fun revokedExactAlarmPermission_showsWarningBannerOnHome_nocrash() {
        // 1. Launch app (signed in, onboarding complete)
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // 2. HomeScreen should appear
        assertTrue(
            "Expected HomeScreen",
            device.wait(Until.hasObject(By.res("home_screen")), TIMEOUT),
        )

        // 3. Warning banner should be visible (posted by onResume() when SCHEDULE_EXACT_ALARM denied)
        assertTrue(
            "Expected scheduler_error warning banner on HomeScreen",
            device.wait(Until.hasObject(By.res("scheduler_error")), TIMEOUT),
        )

        // 4. App has not crashed — assert HomeScreen is still displayed
        assertTrue(device.hasObject(By.res("home_screen")))
    }
}
