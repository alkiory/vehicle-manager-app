package com.example.vehiclemanager.core.domain

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateVehicleStatsUseCaseTest {
    private val useCase = CalculateVehicleStatsUseCase()
    private val currentDate = epoch("2025-10-15")

    @Test
    fun epic022FixtureCalculatesCorrectTotalsAveragesAndWeightedPrice() {
        val fuel = listOf(
            fuel("2025-09-01", 1_000, 175, 5_000, litersX100 = 2_857, vehicleId = 1),
            fuel("2025-09-15", 1_500, 187, 3_000, litersX100 = 1_604, vehicleId = 1),
            fuel("2025-10-05", 2_448, 186, 14_000, litersX100 = 7_500, vehicleId = 1),
        )
        val maintenance = listOf(
            maintenance("2025-09-20", 1_500, 1_200, vehicleId = 1),
            maintenance("2025-10-10", 2_448, 463, vehicleId = 1),
        )

        val stats = useCase(fuel, maintenance, StatsPeriod.ALL_TIME, currentDate, activeVehicleId = 1)

        assertEquals(22_000L, stats.totalFuelCostCents)
        assertEquals(1_663L, stats.totalMaintenanceCostCents)
        assertEquals(23_663L, stats.totalCostCents)
        assertEquals(1_448L, stats.totalDistanceKm)
        assertEquals(1_634L, stats.costPerKilometerCentsX100) // 0.16 €/km
        assertEquals(11_832L, stats.averageMonthlySpendCents) // 118.32 €/mes (23663 / 2 months inclusive)
        assertEquals(listOf(9_200L, 14_463L), stats.monthlyExpenditure.map { it.totalCostCents })
        assertEquals(179L, stats.monthlyFuelPrice.find { it.month == 9 }?.averagePricePerLiterCents) // 1.79 €/L (weighted)
    }

    @Test
    fun activeVehicleFilteringIsRespectedWhenMultipleVehiclesPresent() {
        val fuel = listOf(
            fuel("2025-09-01", 1_000, 180, 5_000, vehicleId = 1),
            fuel("2025-09-02", 1_000, 180, 50_000, vehicleId = 2),
        )
        val stats = useCase(fuel, emptyList(), StatsPeriod.ALL_TIME, currentDate, activeVehicleId = 1)

        assertEquals(5_000L, stats.totalCostCents)
    }

    @Test
    fun zeroOrOneRecordProducesNullCostPerKilometer() {
        val zeroStats = useCase(emptyList(), emptyList(), StatsPeriod.ALL_TIME, currentDate)
        assertEquals(null, zeroStats.costPerKilometerCentsX100)

        val oneStats = useCase(
            listOf(fuel("2025-09-01", 1_000, 180, 5_000)),
            emptyList(),
            StatsPeriod.ALL_TIME,
            currentDate,
        )
        assertEquals(0L, oneStats.totalDistanceKm)
        assertEquals(null, oneStats.costPerKilometerCentsX100)
    }

    private fun fuel(
        date: String,
        odometerKm: Long,
        priceCents: Long,
        totalCostCents: Long,
        litersX100: Int = 300,
        vehicleId: Long = 1,
    ) = FuelRecord(
        id = date.hashCode().toLong(),
        vehicleId = vehicleId,
        timestampMs = epoch(date),
        odometerKm = odometerKm,
        litersX100 = litersX100,
        pricePerLiterCents = priceCents,
        totalCostCents = totalCostCents,
        isFullTank = true,
        stationName = null,
        notes = null,
    )

    private fun maintenance(
        date: String,
        odometerKm: Long,
        costCents: Long,
        vehicleId: Long = 1,
    ) = MaintenanceRecord(
        id = date.hashCode().toLong(),
        vehicleId = vehicleId,
        title = "Service",
        category = MaintenanceCategory.OTHER,
        costCents = costCents,
        odometerKm = odometerKm,
        timestampMs = epoch(date),
        notes = null,
        performedBy = null,
    )

    private fun epoch(date: String): Long = LocalDate.parse(date)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
}
