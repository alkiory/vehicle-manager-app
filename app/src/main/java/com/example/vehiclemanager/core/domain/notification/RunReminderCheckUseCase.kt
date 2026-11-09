package com.example.vehiclemanager.core.domain.notification

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.vehiclemanager.core.data.notification.NotificationManagerImpl
import com.example.vehiclemanager.core.data.notification.NotificationRepository
import com.example.vehiclemanager.core.data.notification.NotificationScheduleEntity
import com.example.vehiclemanager.core.domain.GetDueServicesUseCase
import com.example.vehiclemanager.core.domain.ReminderEvaluationOutcome
import com.example.vehiclemanager.core.domain.ReminderEvaluator
import com.example.vehiclemanager.core.domain.ReminderItem
import com.example.vehiclemanager.core.domain.ReminderPreferencesRepository
import com.example.vehiclemanager.core.domain.ServiceStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunReminderCheckUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getDueServicesUseCase: GetDueServicesUseCase,
    private val notificationManager: NotificationManagerImpl,
    private val notificationRepository: NotificationRepository,
    private val reminderPreferencesRepository: ReminderPreferencesRepository,
) {
    suspend fun execute(trigger: ReminderCheckTrigger): ReminderCheckLog {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        val prefs = reminderPreferencesRepository.reminderPreferences.first()
        val dueServices = getDueServicesUseCase.getDueServices().first()
        val today = LocalDate.now()

        val itemResults = mutableListOf<ReminderCheckItemResult>()
        var postedCount = 0

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
                currentOdometer = 0L,
                today = today,
                leadDays = prefs.advanceDays,
                leadKm = prefs.advanceDistanceKm,
                fuelNotificationsEnabled = prefs.fuelNotificationsEnabled,
                tirePressureNotificationsEnabled = prefs.tirePressureNotificationsEnabled,
                lastNotifiedDate = lastNotifiedDate,
            )

            val dueInfoText = buildDueInfoText(service.dueDateMs, service.dueOdometer)
            var isNotified = false
            val resultReason: String

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
                    isNotified = true
                    postedCount++
                    resultReason = "Notificación enviada"
                } else {
                    resultReason = "Vencido/próximo pero falta el permiso de notificaciones"
                }

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
            } else {
                resultReason = when (evaluation.outcome) {
                    ReminderEvaluationOutcome.TOGGLED_OFF ->
                        "Notificaciones desactivadas para esta categoría"
                    ReminderEvaluationOutcome.ALREADY_NOTIFIED_TODAY ->
                        "Ya notificado hoy"
                    ReminderEvaluationOutcome.NOT_YET_DUE ->
                        "No ha alcanzado la antelación necesaria"
                    else -> "No requiere notificación"
                }
            }

            itemResults.add(
                ReminderCheckItemResult(
                    scheduleId = service.scheduleId,
                    vehicleId = service.vehicleId,
                    vehicleName = service.vehicleName,
                    serviceTitle = service.serviceTitle,
                    dueInfo = dueInfoText,
                    isNotified = isNotified,
                    resultReason = resultReason,
                )
            )
        }

        val skipSummary = when {
            !hasPermission -> "Permiso de notificaciones deshabilitado"
            dueServices.isEmpty() -> "Sin servicios registrados o pendientes"
            else -> null
        }

        val log = ReminderCheckLog(
            timestampMs = System.currentTimeMillis(),
            trigger = trigger,
            evaluatedCount = dueServices.size,
            postedCount = postedCount,
            skipReasonSummary = skipSummary,
            itemResults = itemResults,
        )

        reminderPreferencesRepository.saveLastCheckLog(log)
        return log
    }

    suspend fun checkAndRunCatchUpIfNeeded(): ReminderCheckLog? {
        val lastLog = reminderPreferencesRepository.lastCheckLog.first()
        val now = System.currentTimeMillis()
        val isCatchUpNeeded = lastLog == null || (now - lastLog.timestampMs) >= TWENTY_SIX_HOURS_MS
        return if (isCatchUpNeeded) {
            execute(ReminderCheckTrigger.CATCH_UP)
        } else {
            null
        }
    }

    private fun buildDueInfoText(dueDateMs: Long, dueOdometer: Long): String {
        val now = System.currentTimeMillis()
        val daysUntil = (dueDateMs - now) / (24 * 60 * 60 * 1000)
        return buildString {
            if (daysUntil < 0) {
                append("Vencido hace ${-daysUntil} días")
            } else if (daysUntil == 0L) {
                append("Vence hoy")
            } else {
                append("Vence en $daysUntil días")
            }
            if (dueOdometer > 0) {
                append(" (${dueOdometer} km)")
            }
        }
    }

    companion object {
        private const val SERVICE_NOTIFICATION_PREFIX = 10000L
        private const val TWENTY_SIX_HOURS_MS = 26 * 60 * 60 * 1000L
    }
}
