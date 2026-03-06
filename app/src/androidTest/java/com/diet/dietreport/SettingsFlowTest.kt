package com.diet.dietreport

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

private const val TIMEOUT = 5_000L

@RunWith(AndroidJUnit4::class)
class SettingsFlowTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val presetUser = User("uid-test", "test@example.com", "Test User")

    private val setupRule = object : ExternalResource() {
        override fun before() {
            device.pressHome() // ensure notification shade or other overlays are dismissed
            runBlocking {
                context.settingsDataStore.edit { it.clear() }
                AuthRepository(context.authDataStore).saveUser(presetUser)
            }
        }

        override fun after() {
            runBlocking { context.authDataStore.edit { it.clear() } }
        }
    }

    private val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(setupRule).around(activityRule)

    @Test
    fun settingsFlow_happyFlow() {
        // 1. App launches signed in → HomeScreen
        assertTrue(device.wait(Until.hasObject(By.res("home_screen")), TIMEOUT))

        // Navigate to SettingsScreen
        device.findObject(By.text("Settings")).click()
        assertTrue(device.wait(Until.hasObject(By.res("settings_screen")), TIMEOUT))

        // 2. Set wake time to 08:00 (non-default: default is 07:00)
        device.findObject(By.res("wake_time_field")).setText("08:00")

        // 3. Set bedtime to 22:00 (non-default: default is 23:00)
        device.findObject(By.res("bed_time_field")).setText("22:00")

        // 4. Set first-meal delay to 60 min (non-default: default is 30)
        device.findObject(By.res("delay_field")).setText("60")

        // 5. Select alarm ringtone (non-default: default is "default")
        device.findObject(By.res("ringtone_radio_alarm")).click()

        // 6. Tap Save
        device.findObject(By.res("save_button")).click()

        // 7. Assert success confirmation is shown
        assertTrue(device.wait(Until.hasObject(By.res("save_success")), TIMEOUT))

        // 8. Recreate activity (config-change simulation — NavController state is restored)
        activityRule.scenario.recreate()

        // After recreate the NavController restores back to SettingsScreen
        assertTrue(device.wait(Until.hasObject(By.res("settings_screen")), TIMEOUT))

        // 9-10. Assert all saved values are displayed correctly (all non-default proves persistence)
        assertEquals("08:00", device.findObject(By.res("wake_time_field")).text)
        assertEquals("22:00", device.findObject(By.res("bed_time_field")).text)
        assertEquals("60", device.findObject(By.res("delay_field")).text)
        assertTrue(device.findObject(By.res("ringtone_radio_alarm")).isChecked)
    }
}
