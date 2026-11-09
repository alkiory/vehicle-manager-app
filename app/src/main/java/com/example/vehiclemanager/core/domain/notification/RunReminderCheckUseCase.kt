package com.example.vehiclemanager.core.domain.notification

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.vehiclemanager.core.data.notification.NotificationManagerImpl
import com.example.vehiclemanager.core.domain.GetDueServicesUseCase
import com.example.vehiclemanager.core.domain.ReminderEvaluationOutcome
import com.example.vehiclemanager.core.domain.ReminderEvaluator
import com.example.vehiclemanager.core.domain.ReminderItem
import com.example.vehiclemanager.core.domain.ReminderPreferencesRepository
import com.example.vehiclemanager.core.domain.ReminderStage
import com.example.vehiclemanager.core.domain.ServiceStatus
import com.example.vehiclemanager.core.domain.StoredStageState
import com.example.vehiclemanager.core.domain.VehicleRepository
import com.example.vehiclemanager.core.domain.FuelRecordRepository
import com.example.vehiclemanager.core.domain.EstimateRemainingRangeUseCase
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
    private val reminderPreferencesRepository: ReminderPreferencesRepository,
    private val vehicleRepository: VehicleRepository,
    private val fuelRecordRepository: FuelRecordRepository,
    private val estimateRemainingRange: EstimateRemainingRangeUseCase,
) {
    suspend fun execute(trigger: ReminderCheckTrigger): ReminderCheckLog {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        val prefs = reminderPreferencesRepository.reminderPreferences.first()
        val lastNotifiedStages = reminderPreferencesRepository.lastNotifiedStages.first()
        val dueServices = getDueServicesUseCase.getDueServices().first()
        val vehicles = vehicleRepository.vehicles.first()
        val vehicleMap = vehicles.associateBy { id -> id.id }
        val today = LocalDate.now()

        val itemResults = mutableListOf<ReminderCheckItemResult>()
        var postedCount = 0

        // 1. Evaluate regular maintenance services across all vehicles
        dueServices.forEach { service ->
            val vehicle = vehicleMap[service.vehicleId]
            val currentOdometer = vehicle?.primaryOdometerKm ?: 0L

            val reminderItem = ReminderItem(
                scheduleId = service.scheduleId,
                vehicleId = service.vehicleId,
                vehicleName = service.vehicleName,
                serviceTitle = service.serviceTitle,
                dueDate = Instant.ofEpochMilli(service.dueDateMs).atZone(ZoneId.systemDefault()).toLocalDate(),
                dueOdometer = service.dueOdometer,
                isTirePressure = service.serviceTitle.contains("neumátic", ignoreCase = true) || service.serviceTitle.contains("tire", ignoreCase = true),
                isFuel = false,
            )

            val storedState = lastNotifiedStages[service.scheduleId]
            val lastNotifiedStage = storedState?.stage?.let { runCatching { ReminderStage.valueOf(it) }.getOrNull() }
            val targetDueDate = storedState?.targetDueDateMs?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
            val targetDueOdometer = storedState?.targetDueOdometer

            val evaluation = ReminderEvaluator.evaluate(
                item = reminderItem,
                currentOdometer = currentOdometer,
                today = today,
                leadDays = prefs.advanceDays,
                leadKm = prefs.advanceDistanceKm,
                fuelNotificationsEnabled = prefs.fuelNotificationsEnabled,
                tirePressureNotificationsEnabled = prefs.tirePressureNotificationsEnabled,
                lastNotifiedStage = lastNotifiedStage,
                lastNotifiedDueDate = targetDueDate,
                lastNotifiedDueOdometer = targetDueOdometer,
            )

            val dueInfoText = buildDueInfoText(service.dueDateMs, service.dueOdometer)
            var isNotified = false
            val resultReason: String

            if (evaluation.shouldNotify) {
                if (hasPermission) {
                    val notificationId = SERVICE_NOTIFICATION_PREFIX + service.scheduleId
                    notificationManager.showMaintenanceReminder(
                        notificationId = notificationId,
                        vehicleName = service.vehicleName,
                        serviceTitle = service.serviceTitle,
                        dueDateMs = service.dueDateMs,
                        dueOdometer = service.dueOdometer,
                        isOverdue = service.status == ServiceStatus.OVERDUE || evaluation.currentStage == ReminderStage.OVERDUE,
                        vibrateOnNotification = prefs.vibrateOnNotification,
                    )
                    isNotified = true
                    postedCount++
                    resultReason = "Notificación enviada (${evaluation.currentStage})"

                    reminderPreferencesRepository.saveLastNotifiedStage(
                        StoredStageState(
                            scheduleId = service.scheduleId,
                            stage = evaluation.currentStage!!.name,
                            targetDueDateMs = service.dueDateMs,
                            targetDueOdometer = service.dueOdometer,
                            timestampMs = System.currentTimeMillis(),
                        )
                    )
                } else {
                    resultReason = "Vencido/próximo pero falta el permiso de notificaciones"
                }
            } else {
                resultReason = when (evaluation.outcome) {
                    ReminderEvaluationOutcome.TOGGLED_OFF ->
                        "Notificaciones desactivadas para esta categoría"
                    ReminderEvaluationOutcome.ALREADY_NOTIFIED_FOR_STAGE ->
                        "Ya notificado para esta etapa"
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

        // 2. Evaluate fuel estimates for each vehicle if fuel notifications enabled
        if (prefs.fuelNotificationsEnabled) {
            for (vehicle in vehicles) {
                val fuelRecords = fuelRecordRepository.observeFuelRecords(vehicle.id).first()
                val estimate = estimateRemainingRange(vehicle, fuelRecords, System.currentTimeMillis())
                val estDateMs = estimate.estimatedNextRefuelDateMs
                val estKm = estimate.estimatedNextRefuelOdometerKm
                if (estDateMs != null && estKm != null) {
                    val scheduleId = FUEL_ESTIMATE_PREFIX + vehicle.id
                    val reminderItem = ReminderItem(
                        scheduleId = scheduleId,
                        vehicleId = vehicle.id,
                        vehicleName = vehicle.name,
                        serviceTitle = "Próximo repostaje estimado",
                        dueDate = Instant.ofEpochMilli(estDateMs).atZone(ZoneId.systemDefault()).toLocalDate(),
                        dueOdometer = estKm,
                        isFuel = true,
                    )

                    val storedState = lastNotifiedStages[scheduleId]
                    val lastNotifiedStage = storedState?.stage?.let { runCatching { ReminderStage.valueOf(it) }.getOrNull() }
                    val targetDueDate = storedState?.targetDueDateMs?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                    val targetDueOdometer = storedState?.targetDueOdometer

                    val evaluation = ReminderEvaluator.evaluate(
                        item = reminderItem,
                        currentOdometer = vehicle.primaryOdometerKm,
                        today = today,
                        leadDays = prefs.advanceDays,
                        leadKm = prefs.advanceDistanceKm,
                        fuelNotificationsEnabled = prefs.fuelNotificationsEnabled,
                        tirePressureNotificationsEnabled = prefs.tirePressureNotificationsEnabled,
                        lastNotifiedStage = lastNotifiedStage,
                        lastNotifiedDueDate = targetDueDate,
                        lastNotifiedDueOdometer = targetDueOdometer,
                    )

                    if (evaluation.shouldNotify) {
                        if (hasPermission) {
                            notificationManager.showFuelPriceAlert(
                                notificationId = scheduleId,
                                stationName = "Autonomía estimada próxima",
                                pricePerLiter = 0.0,
                                vehicleName = vehicle.name,
                                vibrateOnNotification = prefs.vibrateOnNotification,
                            )
                            postedCount++
                            reminderPreferencesRepository.saveLastNotifiedStage(
                                StoredStageState(
                                    scheduleId = scheduleId,
                                    stage = evaluation.currentStage!!.name,
                                    targetDueDateMs = estDateMs,
                                    targetDueOdometer = estKm,
                                    timestampMs = System.currentTimeMillis(),
                                )
                            )
                        }
                    }
                }
            }
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
                append("Vencido hace ${-daysUntil} days")
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
        private const val FUEL_ESTIMATE_PREFIX = 2000000L
        private const val TWENTY_SIX_HOURS_MS = 26 * 60 * 60 * 1000L
    }
}
