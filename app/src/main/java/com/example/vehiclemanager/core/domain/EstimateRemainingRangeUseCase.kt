package com.example.vehiclemanager.core.domain

import javax.inject.Inject
import kotlin.math.roundToInt

data class NextRefuelEstimate(
    val averageConsumptionLPer100Km: Double? = null,
    val estimatedRemainingRangeKm: Int? = null,
    val estimatedNextRefuelOdometerKm: Long? = null,
    val estimatedNextRefuelDateMs: Long? = null,
)

class EstimateRemainingRangeUseCase @Inject constructor(
    private val calculateFuelConsumption: CalculateFuelConsumptionUseCase,
) {
    operator fun invoke(
        activeVehicle: Vehicle?,
        fuelRecords: List<FuelRecord>,
        currentDateMs: Long,
    ): NextRefuelEstimate {
        val consumptionResult = calculateFuelConsumption(fuelRecords)
        if (consumptionResult !is FuelConsumptionResult.Calculated) {
            return NextRefuelEstimate()
        }

        val avgConsumption = consumptionResult.litersPer100KmX100 / 100.0
        if (avgConsumption <= 0.0) return NextRefuelEstimate()

        val sortedRecords = fuelRecords.sortedWith(
            compareBy<FuelRecord> { it.timestampMs }.thenBy { it.id },
        )
        val latestRecord = sortedRecords.lastOrNull() ?: return NextRefuelEstimate()

        val tankCapacityLiters = (latestRecord.litersX100 / 100.0).takeIf { it > 0 } ?: 50.0

        val remainingRangeKm = ((tankCapacityLiters / avgConsumption) * 100.0).roundToInt()
        val currentOdometer = activeVehicle?.primaryOdometerKm ?: latestRecord.odometerKm
        val nextRefuelOdometerKm = currentOdometer + (remainingRangeKm * 0.9).roundToInt()

        val kmPerDay = calculateAverageKmPerDay(sortedRecords)
        val estimatedNextRefuelDateMs = if (kmPerDay > 0) {
            val daysToNextRefuel = ((remainingRangeKm * 0.9) / kmPerDay).roundToInt()
            currentDateMs + (daysToNextRefuel * 86_400_000L)
        } else {
            null
        }

        return NextRefuelEstimate(
            averageConsumptionLPer100Km = avgConsumption,
            estimatedRemainingRangeKm = remainingRangeKm,
            estimatedNextRefuelOdometerKm = nextRefuelOdometerKm,
            estimatedNextRefuelDateMs = estimatedNextRefuelDateMs,
        )
    }

    private fun calculateAverageKmPerDay(records: List<FuelRecord>): Double {
        if (records.size < 2) return 30.0
        val first = records.first()
        val last = records.last()
        val totalDistance = last.odometerKm - first.odometerKm
        val totalDays = (last.timestampMs - first.timestampMs) / 86_400_000.0
        if (totalDistance <= 0 || totalDays <= 0) return 30.0
        return totalDistance / totalDays
    }
}
