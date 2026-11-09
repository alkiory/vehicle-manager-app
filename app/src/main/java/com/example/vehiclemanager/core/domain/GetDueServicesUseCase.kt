package com.example.vehiclemanager.core.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * Use case to retrieve maintenance schedules that are due or upcoming.
 */
class GetDueServicesUseCase @Inject constructor(
    private val maintenanceScheduleRepository: MaintenanceScheduleRepository,
    private val vehicleRepository: VehicleRepository,
    private val fuelRecordRepository: FuelRecordRepository,
) {
    /**
     * Returns a flow of due service notifications for all vehicles.
     */
    fun getDueServices(): Flow<List<DueServiceNotification>> {
        return combine(
            maintenanceScheduleRepository.observeAllSchedules(),
            vehicleRepository.vehicles,
            fuelRecordRepository.observeRecentPrices()
        ) { schedules, vehicles, _ ->
            val vehicleMap = vehicles.associateBy { it.id }
            schedules.mapNotNull { schedule ->
                val vehicle = vehicleMap[schedule.vehicleId]
                val currentOdometer = vehicle?.primaryOdometerKm ?: 0L
                
                DueServiceNotification(
                    scheduleId = schedule.id,
                    vehicleId = schedule.vehicleId,
                    vehicleName = vehicle?.name ?: "Vehículo desconocido",
                    serviceTitle = schedule.serviceTitle,
                    dueDateMs = calculateNextDueDate(schedule, currentOdometer),
                    dueOdometer = calculateNextDueOdometer(schedule, currentOdometer),
                    status = determineServiceStatus(schedule, currentOdometer),
                )
            }.sortedWith(
                compareBy<DueServiceNotification> { it.status }
                    .thenBy { it.dueDateMs }
            )
        }
    }

    fun calculateNextDueDate(
        schedule: MaintenanceSchedule,
        currentOdometer: Long,
    ): Long {
        if (schedule.intervalMonths != null && schedule.lastPerformedDateMs != null) {
            val lastDate = Instant.ofEpochMilli(schedule.lastPerformedDateMs)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val nextDate = lastDate.plusMonths(schedule.intervalMonths.toLong())
            return nextDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        }
        if (schedule.lastPerformedDateMs != null) {
            return schedule.lastPerformedDateMs + (30L * 24 * 60 * 60 * 1000)
        }
        return System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
    }

    fun calculateNextDueOdometer(
        schedule: MaintenanceSchedule,
        currentOdometer: Long,
    ): Long {
        if (schedule.intervalKm != null && schedule.lastPerformedKm != null) {
            return schedule.lastPerformedKm + schedule.intervalKm
        }
        if (schedule.intervalKm != null) {
            return currentOdometer + schedule.intervalKm
        }
        return currentOdometer + 10000L
    }

    fun determineServiceStatus(
        schedule: MaintenanceSchedule,
        currentOdometer: Long,
    ): ServiceStatus {
        val nextDueOdometer = calculateNextDueOdometer(schedule, currentOdometer)
        val nextDueDate = calculateNextDueDate(schedule, currentOdometer)
        val now = System.currentTimeMillis()

        val kmOverdue = currentOdometer - nextDueOdometer
        val daysOverdue = (now - nextDueDate) / (24 * 60 * 60 * 1000)

        return when {
            kmOverdue > 0 || daysOverdue > 0 -> ServiceStatus.OVERDUE
            kmOverdue > -1000 || daysOverdue > -7 -> ServiceStatus.DUE_SOON
            else -> ServiceStatus.OK
        }
    }
}

/**
 * Represents a service that is due or upcoming.
 */
data class DueServiceNotification(
    val scheduleId: Long,
    val vehicleId: Long,
    val vehicleName: String,
    val serviceTitle: String,
    val dueDateMs: Long,
    val dueOdometer: Long,
    val status: ServiceStatus,
)

/**
 * Service status for notifications.
 */
enum class ServiceStatus {
    /** Service is overdue and needs immediate attention */
    OVERDUE,
    /** Service is due soon (within 7 days or 1000km) */
    DUE_SOON,
    /** Service is not yet due */
    OK,
}
