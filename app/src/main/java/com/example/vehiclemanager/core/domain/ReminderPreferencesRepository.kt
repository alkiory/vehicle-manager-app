package com.example.vehiclemanager.core.domain

import com.example.vehiclemanager.core.domain.notification.ReminderCheckLog
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing reminder notification preferences.
 */
interface ReminderPreferencesRepository {
    /**
     * Current reminder preferences as a Flow.
     */
    val reminderPreferences: Flow<ReminderPreferences>

    /**
     * Last reminder check log.
     */
    val lastCheckLog: Flow<ReminderCheckLog?>

    /**
     * Last notified stages per reminder ID.
     */
    val lastNotifiedStages: Flow<Map<Long, StoredStageState>>

    /**
     * Save last reminder check log.
     */
    suspend fun saveLastCheckLog(log: ReminderCheckLog)

    /**
     * Save last notified stage state for a reminder.
     */
    suspend fun saveLastNotifiedStage(state: StoredStageState)

    /**
     * Reset last notified stage for a reminder (e.g. when edited, completed or rescheduled).
     */
    suspend fun resetLastNotifiedStage(scheduleId: Long)

    /**
     * Update advance distance notification preference.
     * @param km Distance in kilometers before service due date to show notification
     */
    suspend fun setAdvanceDistanceKm(km: Long)

    /**
     * Update advance days notification preference.
     * @param days Number of days before service due date to show notification
     */
    suspend fun setAdvanceDays(days: Int)

    /**
     * Update best notification time preference.
     * @param hours Hour (0-23)
     * @param minutes Minutes (0-59)
     */
    suspend fun setNotificationTime(hours: Int, minutes: Int)

    /**
     * Enable or disable fuel-related notifications.
     */
    suspend fun setFuelNotificationsEnabled(enabled: Boolean)

    /**
     * Enable or disable tire pressure notifications.
     */
    suspend fun setTirePressureNotificationsEnabled(enabled: Boolean)

    /**
     * Enable or disable vibration on notifications.
     */
    suspend fun setVibrateOnNotification(enabled: Boolean)

    /**
     * Set onboarding completed flag.
     */
    suspend fun setOnboardingCompleted(completed: Boolean)

    /**
     * Set notification permission asked flag.
     */
    suspend fun setNotificationPermissionAsked(asked: Boolean)

    /**
     * Update all preferences at once.
     */
    suspend fun updateAll(preferences: ReminderPreferences)
}
