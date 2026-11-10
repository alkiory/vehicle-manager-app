package com.example.vehiclemanager.core.domain

import javax.inject.Inject

/** Result of calculating consumption from full-tank fuel records. */
sealed interface FuelConsumptionResult {
    data class Calculated(
        val litersPer100KmX100: Long,
        val intervalsUsed: Int,
    ) : FuelConsumptionResult

    data object InsufficientData : FuelConsumptionResult
}

/**
 * Calculates fuel consumption across consecutive full-tank checkpoints.
 * Uses weighted average consumption across the most recent N intervals (default maxIntervals = 3).
 *
 * Partial refuels between two full tanks are included in the interval total.
 * Records are ordered by timestamp and ID before calculation.
 */
class CalculateFuelConsumptionUseCase @Inject constructor() {
    operator fun invoke(
        records: List<FuelRecord>,
        maxIntervals: Int = 3,
    ): FuelConsumptionResult {
        val orderedRecords = records.sortedWith(
            compareBy<FuelRecord> { it.timestampMs }.thenBy { it.id },
        )
        val fullTankIndexes = orderedRecords
            .mapIndexedNotNull { index, record -> index.takeIf { record.isFullTank } }

        if (fullTankIndexes.size < 2) return FuelConsumptionResult.InsufficientData

        val startIndex = fullTankIndexes.first()
        val endIndex = fullTankIndexes.last()
        val distanceKm = orderedRecords[endIndex].odometerKm - orderedRecords[startIndex].odometerKm
        if (distanceKm <= 0) return FuelConsumptionResult.InsufficientData

        val totalLitersX100 = orderedRecords
            .subList(startIndex + 1, endIndex + 1)
            .sumOf { it.litersX100.toLong() }
        if (totalLitersX100 <= 0) return FuelConsumptionResult.InsufficientData

        return FuelConsumptionResult.Calculated(
            litersPer100KmX100 = (totalLitersX100 * 100L) / distanceKm,
            intervalsUsed = fullTankIndexes.size - 1,
        )
    }


}
