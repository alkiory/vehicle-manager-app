package com.example.vehiclemanager.core.data.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vehiclemanager.core.domain.GetDueServicesUseCase
import com.example.vehiclemanager.core.domain.ReminderEvaluator
import com.example.vehiclemanager.core.domain.ReminderItem
import com.example.vehiclemanager.core.domain.ReminderPreferencesRepository
import com.example.vehiclemanager.core.domain.ServiceStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * WorkManager worker that checks for due services and schedules notifications.
 */
@HiltWorker
class ServiceReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getDueServicesUseCase: GetDueServicesUseCase,
    private val notificationManager: NotificationManagerImpl,
    private val notificationRepository: NotificationRepository,
    private val reminderPreferencesRepository: ReminderPreferencesRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Check if POST_NOTIFICATIONS permission is missing; if so, skip posting silently
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            val prefs = reminderPreferencesRepository.reminderPreferences.first()
            val dueServices = getDueServicesUseCase.getDueServices().first()
            val today = LocalDate.now()

            dueServices.forEach { service ->
                val reminderItem = ReminderItem(
                    scheduleId = service.scheduleId,
                    vehicleId = service.vehicleId,
                    vehicleName = service.vehicleName,
                    serviceTitle = service.serviceTitle,
                    dueDate = Instant.ofEpochMilli(service.dueDateMs).atZone(ZoneId.systemDefault()).toLocalDate(),
                    dueOdometer = service.dueOdometer,
                )

                val existingNotification = notificationRepository.getNotification(service.scheduleId)
                val lastNotifiedDate = existingNotification?.triggeredDateMs?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                }

                val evaluation = ReminderEvaluator.evaluate(
                    item = reminderItem,
                    currentOdometer = 0L, // evaluated via use case / vehicle odometer
                    today = today,
                    leadDays = prefs.advanceDays,
                    leadKm = prefs.advanceDistanceKm,
                    fuelNotificationsEnabled = prefs.fuelNotificationsEnabled,
                    tirePressureNotificationsEnabled = prefs.tirePressureNotificationsEnabled,
                    lastNotifiedDate = lastNotifiedDate,
                )

                if (evaluation.shouldNotify && (existingNotification == null || !existingNotification.isTriggered)) {
                    if (hasPermission) {
                        val notificationId = SERVICE_NOTIFICATION_PREFIX + service.scheduleId
                        notificationManager.showMaintenanceReminder(
                            notificationId = notificationId,
                            vehicleName = service.vehicleName,
                            serviceTitle = service.serviceTitle,
                            dueDateMs = service.dueDateMs,
                            dueOdometer = service.dueOdometer,
                            isOverdue = service.status == ServiceStatus.OVERDUE,
                            vibrateOnNotification = prefs.vibrateOnNotification,
                        )
                    }

                    // Save notification record
                    val notificationEntity = NotificationScheduleEntity(
                        id = service.scheduleId,
                        vehicleId = service.vehicleId,
                        serviceTitle = service.serviceTitle,
                        scheduledDateMs = service.dueDateMs,
                        isTriggered = true,
                        triggeredDateMs = System.currentTimeMillis(),
                        maintenanceScheduleId = service.scheduleId,
                    )
                    notificationRepository.insertNotification(notificationEntity)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val SERVICE_NOTIFICATION_PREFIX = 10000L

        fun schedule(context: Context, hours: Int = 9, minutes: Int = 0) {
            ReminderScheduler.schedule(context, hours, minutes)
        }

        fun cancel(context: Context) {
            ReminderScheduler.cancel(context)
        }
    }
}
