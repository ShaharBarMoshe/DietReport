package com.diet.dietreport.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

open class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val KEY_WAKE_HOUR = intPreferencesKey("wake_hour")
        private val KEY_WAKE_MINUTE = intPreferencesKey("wake_minute")
        private val KEY_BED_HOUR = intPreferencesKey("bed_hour")
        private val KEY_BED_MINUTE = intPreferencesKey("bed_minute")
        private val KEY_DELAY = intPreferencesKey("first_meal_delay_minutes")
        private val KEY_RINGTONE = stringPreferencesKey("ringtone")
        private val KEY_ACTIVITY_MONITOR = booleanPreferencesKey("activity_monitor_enabled")
        private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    val settings: Flow<Settings> = dataStore.data.map { prefs ->
        Settings(
            wakeHour = prefs[KEY_WAKE_HOUR] ?: 7,
            wakeMinute = prefs[KEY_WAKE_MINUTE] ?: 0,
            bedHour = prefs[KEY_BED_HOUR] ?: 23,
            bedMinute = prefs[KEY_BED_MINUTE] ?: 0,
            firstMealDelayMinutes = prefs[KEY_DELAY] ?: 30,
            ringtone = prefs[KEY_RINGTONE] ?: "default",
            activityMonitorEnabled = prefs[KEY_ACTIVITY_MONITOR] ?: false,
        )
    }

    val isOnboardingComplete: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETE] ?: false
    }

    open suspend fun markOnboardingComplete() {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETE] = true }
    }

    open suspend fun save(settings: Settings) {
        dataStore.edit { prefs ->
            prefs[KEY_WAKE_HOUR] = settings.wakeHour
            prefs[KEY_WAKE_MINUTE] = settings.wakeMinute
            prefs[KEY_BED_HOUR] = settings.bedHour
            prefs[KEY_BED_MINUTE] = settings.bedMinute
            prefs[KEY_DELAY] = settings.firstMealDelayMinutes
            prefs[KEY_RINGTONE] = settings.ringtone
            prefs[KEY_ACTIVITY_MONITOR] = settings.activityMonitorEnabled
        }
    }
}
