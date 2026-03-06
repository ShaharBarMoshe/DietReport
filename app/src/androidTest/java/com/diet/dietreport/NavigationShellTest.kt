package com.diet.dietreport

import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.diet.dietreport.auth.data.authDataStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

private const val NAV_TIMEOUT = 3_000L

@RunWith(AndroidJUnit4::class)
class NavigationShellTest {

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    // Clear DataStore so a stored user never auto-navigates away from sign_in
    private val clearDataStoreRule = object : ExternalResource() {
        override fun before() {
            device.pressHome() // ensure notification shade or other overlays are dismissed
            val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
            runBlocking { ctx.authDataStore.edit { it.clear() } }
        }
    }

    private val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(clearDataStoreRule).around(activityRule)

    @Test
    fun navigationShell_happyFlow() {
        // 1-2. SignInScreen is the start destination
        assertTrue(device.wait(Until.hasObject(By.res("sign_in_screen")), NAV_TIMEOUT))

        // 3. No runtime permissions needed for Phase 1

        // 4. Navigate to HomeScreen via bottom nav
        device.findObject(By.text("Home")).click()
        assertTrue(device.wait(Until.hasObject(By.res("home_screen")), NAV_TIMEOUT))

        // 5. Navigate to SettingsScreen via bottom nav
        device.findObject(By.text("Settings")).click()
        assertTrue(device.wait(Until.hasObject(By.res("settings_screen")), NAV_TIMEOUT))

        // 6. Navigate to ReportScreen via bottom nav
        device.findObject(By.text("Report")).click()
        assertTrue(device.wait(Until.hasObject(By.res("report_screen")), NAV_TIMEOUT))

        // 7. Press back → pops Report, sign_in (start destination) is displayed
        device.pressBack()
        assertTrue(device.wait(Until.hasObject(By.res("sign_in_screen")), NAV_TIMEOUT))
    }
}
