package com.diet.dietreport

import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.diet.dietreport.settings.data.SettingsRepository
import com.diet.dietreport.settings.data.settingsDataStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

private const val NAV_TIMEOUT = 5_000L

@RunWith(AndroidJUnit4::class)
class NavigationShellTest {

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val setupRule = object : ExternalResource() {
        override fun before() {
            device.pressHome()
            val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
            runBlocking {
                // Mark onboarding complete so the app starts on HomeScreen
                SettingsRepository(ctx.settingsDataStore).markOnboardingComplete()
            }
        }

        override fun after() {
            val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
            runBlocking { ctx.settingsDataStore.edit { it.clear() } }
        }
    }

    private val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(setupRule).around(activityRule)

    @Test
    fun navigationShell_happyFlow() {
        // 1. HomeScreen is the start destination
        assertTrue(device.wait(Until.hasObject(By.res("home_screen")), NAV_TIMEOUT))

        // 2. Navigate to SettingsScreen via bottom nav
        device.findObject(By.text("Settings")).click()
        assertTrue(device.wait(Until.hasObject(By.res("settings_screen")), NAV_TIMEOUT))

        // 3. Navigate to ReportScreen via bottom nav
        device.findObject(By.text("Report")).click()
        assertTrue(device.wait(Until.hasObject(By.res("report_screen")), NAV_TIMEOUT))

        // 4. Press back → pops Report, HomeScreen (start destination) is displayed
        device.pressBack()
        assertTrue(device.wait(Until.hasObject(By.res("home_screen")), NAV_TIMEOUT))
    }
}
