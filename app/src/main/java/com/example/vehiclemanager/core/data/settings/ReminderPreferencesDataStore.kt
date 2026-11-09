package com.example.vehiclemanager.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.vehiclemanager.core.domain.ReminderPreferences
import com.example.vehiclemanager.core.domain.notification.ReminderCheckLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reminderPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "reminder_preferences",
)

@Singleton
class ReminderPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object PreferencesKeys {
        val ADVANCE_DISTANCE_KM = longPreferencesKey("advance_distance_km")
        val ADVANCE_DAYS = intPreferencesKey("advance_days")
        val NOTIFICATION_TIME_HOURS = intPreferencesKey("notification_time_hours")
        val NOTIFICATION_TIME_MINUTES = intPreferencesKey("notification_time_minutes")
        val FUEL_NOTIFICATIONS_ENABLED = booleanPreferencesKey("fuel_notifications_enabled")
        val TIRE_PRESSURE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("tire_pressure_notifications_enabled")
        val VIBRATE_ON_NOTIFICATION = booleanPreferencesKey("vibrate_on_notification")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val NOTIFICATION_PERMISSION_ASKED = booleanPreferencesKey("notification_permission_asked")
        val LAST_CHECK_LOG_JSON = stringPreferencesKey("last_check_log_json")
    }

    val reminderPreferences: Flow<ReminderPreferences> = context.reminderPreferencesDataStore.data.map { preferences ->
        ReminderPreferences(
            advanceDistanceKm = preferences[PreferencesKeys.ADVANCE_DISTANCE_KM] ?: 500L,
            advanceDays = preferences[PreferencesKeys.ADVANCE_DAYS] ?: 7,
            notificationTimeHours = preferences[PreferencesKeys.NOTIFICATION_TIME_HOURS] ?: 9,
            notificationTimeMinutes = preferences[PreferencesKeys.NOTIFICATION_TIME_MINUTES] ?: 0,
            fuelNotificationsEnabled = preferences[PreferencesKeys.FUEL_NOTIFICATIONS_ENABLED] ?: true,
            tirePressureNotificationsEnabled = preferences[PreferencesKeys.TIRE_PRESSURE_NOTIFICATIONS_ENABLED] ?: false,
            vibrateOnNotification = preferences[PreferencesKeys.VIBRATE_ON_NOTIFICATION] ?: true,
            onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
            notificationPermissionAsked = preferences[PreferencesKeys.NOTIFICATION_PERMISSION_ASKED] ?: false,
        )
    }

    val lastCheckLog: Flow<ReminderCheckLog?> = context.reminderPreferencesDataStore.data.map { preferences ->
        val jsonStr = preferences[PreferencesKeys.LAST_CHECK_LOG_JSON] ?: return@map null
        runCatching { Json.decodeFromString<ReminderCheckLog>(jsonStr) }.getOrNull()
    }

    suspend fun saveLastCheckLog(log: ReminderCheckLog) {
        val jsonStr = Json.encodeToString(ReminderCheckLog.serializer(), log)
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_CHECK_LOG_JSON] = jsonStr
        }
    }

    suspend fun updateAdvanceDistanceKm(km: Long) {
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.ADVANCE_DISTANCE_KM] = km
        }
    }

    suspend fun updateAdvanceDays(days: Int) {
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.ADVANCE_DAYS] = days
        }
    }

    suspend fun updateNotificationTime(hours: Int, minutes: Int) {
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_TIME_HOURS] = hours
            preferences[PreferencesKeys.NOTIFICATION_TIME_MINUTES] = minutes
        }
    }

    suspend fun updateFuelNotificationsEnabled(enabled: Boolean) {
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.FUEL_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateTirePressureNotificationsEnabled(enabled: Boolean) {
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.TIRE_PRESSURE_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateVibrateOnNotification(enabled: Boolean) {
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.VIBRATE_ON_NOTIFICATION] = enabled
        }
    }

    suspend fun updateOnboardingCompleted(completed: Boolean) {
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun updateNotificationPermissionAsked(asked: Boolean) {
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_PERMISSION_ASKED] = asked
        }
    }

    suspend fun updateAll(preferences: ReminderPreferences) {
        context.reminderPreferencesDataStore.edit { prefs ->
            prefs[PreferencesKeys.ADVANCE_DISTANCE_KM] = preferences.advanceDistanceKm
            prefs[PreferencesKeys.ADVANCE_DAYS] = preferences.advanceDays
            prefs[PreferencesKeys.NOTIFICATION_TIME_HOURS] = preferences.notificationTimeHours
            prefs[PreferencesKeys.NOTIFICATION_TIME_MINUTES] = preferences.notificationTimeMinutes
            prefs[PreferencesKeys.FUEL_NOTIFICATIONS_ENABLED] = preferences.fuelNotificationsEnabled
            prefs[PreferencesKeys.TIRE_PRESSURE_NOTIFICATIONS_ENABLED] = preferences.tirePressureNotificationsEnabled
            prefs[PreferencesKeys.VIBRATE_ON_NOTIFICATION] = preferences.vibrateOnNotification
            prefs[PreferencesKeys.ONBOARDING_COMPLETED] = preferences.onboardingCompleted
            prefs[PreferencesKeys.NOTIFICATION_PERMISSION_ASKED] = preferences.notificationPermissionAsked
        }
    }
}
