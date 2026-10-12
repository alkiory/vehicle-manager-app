package com.example.vehiclemanager.core.domain

import javax.inject.Inject

class GetDashboardSummaryUseCase @Inject constructor(
    private val estimateRemainingRange: EstimateRemainingRangeUseCase,
) {
    private val getUpcomingServices = GetUpcomingServicesUseCase()
    private val calculateVehicleStats = CalculateVehicleStatsUseCase()
    private val calculateFuelConsumption = CalculateFuelConsumptionUseCase()

    operator fun invoke(
        activeVehicle: Vehicle?,
        fuelRecords: List<FuelRecord>,
        maintenanceRecords: List<MaintenanceRecord>,
        schedules: List<MaintenanceSchedule>,
        currentDateMs: Long,
    ): DashboardSummary {
        val latestFuelRecord = fuelRecords.maxWithOrNull(
            compareBy<FuelRecord> { it.timestampMs }.thenBy { it.id },
        )
        val consumption = calculateFuelConsumption(fuelRecords)
        val nextRefuelEstimate = estimateRemainingRange(
            activeVehicle = activeVehicle,
            fuelRecords = fuelRecords,
            currentDateMs = currentDateMs,
        )
        val upcomingServices = if (activeVehicle == null) {
            emptyList()
        } else {
            getUpcomingServices(
                schedules = schedules,
                currentOdometerKm = activeVehicle.primaryOdometerKm,
                currentDateMs = currentDateMs,
            )
        }

        val stats = calculateVehicleStats(
            fuelRecords = fuelRecords,
            maintenanceRecords = maintenanceRecords,
            period = StatsPeriod.ALL_TIME,
            currentDateMs = currentDateMs,
            activeVehicleId = activeVehicle?.id,
        )

        return DashboardSummary(
            activeVehicle = activeVehicle,
            latestFuelRecord = latestFuelRecord,
            averageConsumptionLitersPer100KmX100 =
                (consumption as? FuelConsumptionResult.Calculated)?.litersPer100KmX100,
            costPerKmCentsX100 = stats.costPerKilometerCentsX100,
            nextRefuelEstimate = nextRefuelEstimate,
            maintenanceAlerts = upcomingServices.filter { it.status != ServiceStatus.OK },
            upcomingServices = upcomingServices,
        )
    }
}

data class DashboardSummary(
    val activeVehicle: Vehicle? = null,
    val latestFuelRecord: FuelRecord? = null,
    val averageConsumptionLitersPer100KmX100: Long? = null,
    val costPerKmCentsX100: Long? = null,
    val nextRefuelEstimate: NextRefuelEstimate = NextRefuelEstimate(),
    val maintenanceAlerts: List<UpcomingService> = emptyList(),
    val upcomingServices: List<UpcomingService> = emptyList(),
)
