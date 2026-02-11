package com.airtel.usagetracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferences(private val context: Context) {

    companion object {
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        val SYNC_INTERVAL_HOURS = intPreferencesKey("sync_interval_hours")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        
        // Default values
        const val DEFAULT_SYNC_INTERVAL = 4 // hours
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_ONBOARDING_COMPLETED] ?: false
        }

    val syncIntervalHours: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[SYNC_INTERVAL_HOURS] ?: DEFAULT_SYNC_INTERVAL
        }

    val isAutoSyncEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AUTO_SYNC_ENABLED] ?: true
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setSyncInterval(hours: Int) {
        context.dataStore.edit { preferences ->
            preferences[SYNC_INTERVAL_HOURS] = hours
        }
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_SYNC_ENABLED] = enabled
        }
    }
}
