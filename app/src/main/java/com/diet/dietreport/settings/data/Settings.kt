package com.diet.dietreport.settings.data

data class Settings(
    val wakeHour: Int = 7,
    val wakeMinute: Int = 0,
    val bedHour: Int = 23,
    val bedMinute: Int = 0,
    val firstMealDelayMinutes: Int = 30,
    val ringtone: String = "default",
    val activityMonitorEnabled: Boolean = false,
)
