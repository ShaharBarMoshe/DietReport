package com.diet.dietreport

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

private const val TIMEOUT = 5_000L

@RunWith(AndroidJUnit4::class)
class AuthFlowTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val presetUser = User("uid-test", "test@example.com", "Test User")
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val setupRule = object : ExternalResource() {
        override fun before() {
            device.pressHome() // ensure notification shade or other overlays are dismissed
            runBlocking { context.authDataStore.edit { it.clear() } }
            AuthViewModelFactory.testFactory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AuthViewModel(
                        FakeAuthService(presetUser),
                        AuthRepository(context.authDataStore),
                    ) as T
            }
        }

        override fun after() {
            AuthViewModelFactory.testFactory = null
        }
    }

    // ActivityScenarioRule has no Espresso integration, unlike createAndroidComposeRule
    private val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(setupRule).around(activityRule)

    @Test
    fun authFlow_happyFlow() {
        // 1-2. Fresh install: SignInScreen is displayed
        assertTrue(device.wait(Until.hasObject(By.res("sign_in_screen")), TIMEOUT))

        // 4. Tap "Sign in with Google"
        device.findObject(By.desc("Sign in with Google")).click()

        // 5. Wait for async navigation then assert HomeScreen
        assertTrue(device.wait(Until.hasObject(By.res("home_screen")), TIMEOUT))

        // 6. Assert stored email matches injected user
        val storedUser = runBlocking { AuthRepository(context.authDataStore).user.first() }
        assertNotNull(storedUser)
        assertEquals(presetUser.email, storedUser?.email)

        // 7. Tap sign-out
        device.findObject(By.desc("Sign out")).click()

        // 8. Wait for async navigation then assert SignInScreen
        assertTrue(device.wait(Until.hasObject(By.res("sign_in_screen")), TIMEOUT))

        // 9. Assert stored user data is cleared
        val clearedUser = runBlocking { AuthRepository(context.authDataStore).user.first() }
        assertNull(clearedUser)

        // 10. Press back → BackHandler consumes it; app stays on SignInScreen
        device.pressBack()
        assertTrue(device.hasObject(By.res("sign_in_screen")))
    }
}
