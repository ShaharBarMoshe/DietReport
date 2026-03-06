package com.diet.dietreport

import com.diet.dietreport.settings.SettingsValidator
import com.diet.dietreport.settings.data.Settings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsValidatorTest {

    private fun valid(
        wakeHour: Int = 7, wakeMinute: Int = 0,
        bedHour: Int = 23, bedMinute: Int = 0,
        delay: Int = 30,
        ringtone: String = "default",
    ) = Settings(wakeHour, wakeMinute, bedHour, bedMinute, delay, ringtone)

    @Test
    fun valid_settings_pass() {
        assertTrue(SettingsValidator.validate(valid()).isSuccess)
    }

    @Test
    fun wake_equals_bedtime_fails() {
        val result = SettingsValidator.validate(valid(wakeHour = 10, bedHour = 10))
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("Wake") == true)
    }

    @Test
    fun wake_after_bedtime_fails() {
        val result = SettingsValidator.validate(valid(wakeHour = 22, bedHour = 8))
        assertFalse(result.isSuccess)
    }

    @Test
    fun delay_zero_passes() {
        assertTrue(SettingsValidator.validate(valid(delay = 0)).isSuccess)
    }

    @Test
    fun delay_180_passes() {
        assertTrue(SettingsValidator.validate(valid(delay = 180)).isSuccess)
    }

    @Test
    fun delay_181_fails() {
        val result = SettingsValidator.validate(valid(delay = 181))
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("180") == true)
    }

    @Test
    fun delay_negative_fails() {
        assertFalse(SettingsValidator.validate(valid(delay = -1)).isSuccess)
    }

    @Test
    fun invalid_ringtone_fails() {
        val result = SettingsValidator.validate(valid(ringtone = "unknown"))
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("ringtone") == true)
    }

    @Test
    fun all_allowed_ringtones_pass() {
        SettingsValidator.ALLOWED_RINGTONES.forEach { ringtone ->
            assertTrue(
                "Expected $ringtone to be valid",
                SettingsValidator.validate(valid(ringtone = ringtone)).isSuccess,
            )
        }
    }
}
