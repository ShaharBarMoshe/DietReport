package com.diet.dietreport.settings

import com.diet.dietreport.settings.data.Settings

object SettingsValidator {

    val ALLOWED_RINGTONES = listOf("default", "alarm", "notification")

    fun validate(settings: Settings): Result<Settings> {
        val wakeMinutes = settings.wakeHour * 60 + settings.wakeMinute
        val bedMinutes = settings.bedHour * 60 + settings.bedMinute

        if (wakeMinutes >= bedMinutes) {
            return Result.failure(
                IllegalArgumentException("Wake time must be before bedtime")
            )
        }
        if (settings.firstMealDelayMinutes !in 0..180) {
            return Result.failure(
                IllegalArgumentException("Delay must be between 0 and 180 minutes")
            )
        }
        if (settings.ringtone !in ALLOWED_RINGTONES) {
            return Result.failure(
                IllegalArgumentException("Invalid ringtone selection")
            )
        }
        return Result.success(settings)
    }
}
