package com.example.vehiclemanager.core.data.notification

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.vehiclemanager.core.domain.ReminderPreferencesRepository
import com.example.vehiclemanager.core.domain.notification.NotificationPermissionChecker
import com.example.vehiclemanager.core.domain.notification.NotificationPermissionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPermissionCheckerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderPreferencesRepository: ReminderPreferencesRepository,
) : NotificationPermissionChecker {

    private var currentActivity: Activity? = null

    fun attachActivity(activity: Activity?) {
        currentActivity = activity
    }

    override fun isPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun areNotificationsEnabled(): Boolean {
        val managerCompat = NotificationManagerCompat.from(context)
        if (!managerCompat.areNotificationsEnabled()) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = notificationManager?.getNotificationChannel("maintenance_reminders")
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
                return false
            }
        }
        return true
    }

    override fun shouldShowRationale(): Boolean {
        val activity = currentActivity ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        } else {
            false
        }
    }

    override fun hasAskedBefore(): Boolean {
        return runBlocking {
            runCatching {
                reminderPreferencesRepository.reminderPreferences.first().notificationPermissionAsked
            }.getOrDefault(false)
        }
    }

    override suspend fun setAskedBefore(asked: Boolean) {
        reminderPreferencesRepository.setNotificationPermissionAsked(asked)
    }

    override fun getPermissionStatus(apiLevel: Int): NotificationPermissionStatus {
        if (!areNotificationsEnabled()) {
            return NotificationPermissionStatus.APP_NOTIFICATIONS_DISABLED
        }
        if (apiLevel < Build.VERSION_CODES.TIRAMISU) {
            return NotificationPermissionStatus.GRANTED
        }
        if (isPermissionGranted()) {
            return NotificationPermissionStatus.GRANTED
        }
        if (shouldShowRationale()) {
            return NotificationPermissionStatus.RATIONALE_NEEDED
        }
        if (!hasAskedBefore()) {
            return NotificationPermissionStatus.NOT_ASKED
        }
        return NotificationPermissionStatus.PERMANENTLY_DENIED
    }
}
