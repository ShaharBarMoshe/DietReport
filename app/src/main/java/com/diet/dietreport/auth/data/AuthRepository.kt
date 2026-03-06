package com.diet.dietreport.auth.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

class AuthRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_EMAIL = stringPreferencesKey("email")
        private val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
    }

    val user: Flow<User?> = dataStore.data.map { prefs ->
        val userId = prefs[KEY_USER_ID] ?: return@map null
        val email = prefs[KEY_EMAIL] ?: return@map null
        User(
            userId = userId,
            email = email,
            displayName = prefs[KEY_DISPLAY_NAME] ?: "",
        )
    }

    suspend fun saveUser(user: User) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = user.userId
            prefs[KEY_EMAIL] = user.email
            prefs[KEY_DISPLAY_NAME] = user.displayName
        }
    }

    suspend fun clearUser() {
        dataStore.edit { it.clear() }
    }
}
