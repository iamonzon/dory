package com.iamonzon.dory.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.iamonzon.dory.algorithm.FsrsParameters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dory_settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val DESIRED_RETENTION = doublePreferencesKey("desired_retention")
        val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    }

    // --- Desired Retention ---

    fun observeDesiredRetention(): Flow<Double> =
        dataStore.data.map { prefs ->
            prefs[Keys.DESIRED_RETENTION] ?: FsrsParameters.DEFAULT_DESIRED_RETENTION
        }

    suspend fun getDesiredRetention(): Double =
        observeDesiredRetention().first()

    suspend fun setDesiredRetention(value: Double) {
        dataStore.edit { prefs ->
            prefs[Keys.DESIRED_RETENTION] = value
        }
    }

    // --- Notification Time ---

    fun observeNotificationHour(): Flow<Int> =
        dataStore.data.map { prefs -> prefs[Keys.NOTIFICATION_HOUR] ?: 9 }

    fun observeNotificationMinute(): Flow<Int> =
        dataStore.data.map { prefs -> prefs[Keys.NOTIFICATION_MINUTE] ?: 0 }

    suspend fun getNotificationHour(): Int =
        observeNotificationHour().first()

    suspend fun getNotificationMinute(): Int =
        observeNotificationMinute().first()

    suspend fun setNotificationTime(hour: Int, minute: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATION_HOUR] = hour
            prefs[Keys.NOTIFICATION_MINUTE] = minute
        }
    }

    // --- Onboarding ---

    fun observeHasCompletedOnboarding(): Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false
        }

    suspend fun getHasCompletedOnboarding(): Boolean =
        observeHasCompletedOnboarding().first()

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.HAS_COMPLETED_ONBOARDING] = completed
        }
    }
}
