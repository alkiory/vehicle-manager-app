package com.example.vehiclemanager.core.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class CalculateVehicleStatsUseCase @Inject constructor() {
    operator fun invoke(
        fuelRecords: List<FuelRecord>,
        maintenanceRecords: List<MaintenanceRecord>,
        period: StatsPeriod,
        currentDateMs: Long,
        activeVehicleId: Long? = null,
    ): VehicleStats {
        val startDateMs = period.startDateMs(currentDateMs)
        val filteredFuel = fuelRecords.filter {
            (activeVehicleId == null || it.vehicleId == activeVehicleId) &&
            it.timestampMs.isInPeriod(startDateMs, currentDateMs)
        }
        val filteredMaintenance = maintenanceRecords.filter {
            (activeVehicleId == null || it.vehicleId == activeVehicleId) &&
            it.timestampMs.isInPeriod(startDateMs, currentDateMs)
        }

        val totalFuelCostCents = filteredFuel.sumOf { it.totalCostCents }
        val totalMaintenanceCostCents = filteredMaintenance.sumOf { it.costCents }
        val totalCostCents = totalFuelCostCents + totalMaintenanceCostCents

        val odometers = (filteredFuel.map { it.odometerKm } + filteredMaintenance.map { it.odometerKm })
        val totalDistanceKm = if (odometers.size < 2) {
            0L
        } else {
            (odometers.maxOrNull()!! - odometers.minOrNull()!!).coerceAtLeast(0L)
        }

        val costPerKilometerCentsX100 = if (totalDistanceKm <= 0 || totalCostCents <= 0) {
            null
        } else {
            BigDecimal.valueOf(totalCostCents)
                .multiply(BigDecimal(100L))
                .divide(BigDecimal(totalDistanceKm), 0, RoundingMode.HALF_UP)
                .toLong()
        }

        val allMonths = (filteredFuel.map { it.timestampMs } + filteredMaintenance.map { it.timestampMs })
            .map { it.toYearMonth() }
            .distinct()
            .sorted()

        val monthsCount = if (allMonths.isEmpty()) {
            0
        } else {
            val first = allMonths.first()
            val last = allMonths.last()
            ChronoUnit.MONTHS.between(first, last).toInt() + 1
        }

        val averageMonthlySpendCents = if (monthsCount <= 0 || totalCostCents <= 0) {
            0L
        } else {
            BigDecimal.valueOf(totalCostCents)
                .divide(BigDecimal(monthsCount), 0, RoundingMode.HALF_UP)
                .toLong()
        }

        return VehicleStats(
            period = period,
            totalFuelCostCents = totalFuelCostCents,
            totalMaintenanceCostCents = totalMaintenanceCostCents,
            totalCostCents = totalCostCents,
            costPerKilometerCentsX100 = costPerKilometerCentsX100,
            averageMonthlySpendCents = averageMonthlySpendCents,
            totalDistanceKm = totalDistanceKm,
            monthlyExpenditure = buildMonthlyExpenditure(filteredFuel, filteredMaintenance),
            monthlyFuelPrice = buildMonthlyFuelPrice(filteredFuel),
        )
    }

    private fun buildMonthlyExpenditure(
        fuelRecords: List<FuelRecord>,
        maintenanceRecords: List<MaintenanceRecord>,
    ): List<MonthlyExpenditure> {
        val amounts = mutableMapOf<YearMonth, Long>()
        fuelRecords.forEach { record ->
            amounts[record.timestampMs.toYearMonth()] =
                (amounts[record.timestampMs.toYearMonth()] ?: 0L) + record.totalCostCents
        }
        maintenanceRecords.forEach { record ->
            amounts[record.timestampMs.toYearMonth()] =
                (amounts[record.timestampMs.toYearMonth()] ?: 0L) + record.costCents
        }
        return amounts.toSortedMap().map { (month, amount) ->
            MonthlyExpenditure(month.year, month.monthValue, amount)
        }
    }

    private fun buildMonthlyFuelPrice(records: List<FuelRecord>): List<MonthlyFuelPrice> = records
        .groupBy { it.timestampMs.toYearMonth() }
        .toSortedMap()
        .map { (month, monthRecords) ->
            val totalCostBigDecimal = monthRecords.sumOf { BigDecimal.valueOf(it.totalCostCents) }
            val totalLitersBigDecimal = monthRecords.sumOf {
                BigDecimal.valueOf(it.litersX100.toLong()).divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
            }
            val weightedPrice = if (totalLitersBigDecimal.compareTo(BigDecimal.ZERO) <= 0) {
                0L
            } else {
                totalCostBigDecimal
                    .divide(totalLitersBigDecimal, 0, RoundingMode.HALF_UP)
                    .toLong()
            }
            val exactPrice = if (totalLitersBigDecimal.compareTo(BigDecimal.ZERO) <= 0) {
                0.0
            } else {
                totalCostBigDecimal.divide(totalLitersBigDecimal, 4, RoundingMode.HALF_UP)
                    .divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
                    .toDouble()
            }
            MonthlyFuelPrice(month.year, month.monthValue, weightedPrice, exactPrice)
        }
}

enum class StatsPeriod {
    LAST_30_DAYS,
    LAST_6_MONTHS,
    YEAR_TO_DATE,
    ALL_TIME,
}

data class VehicleStats(
    val period: StatsPeriod,
    val totalFuelCostCents: Long = 0,
    val totalMaintenanceCostCents: Long = 0,
    val totalCostCents: Long = 0,
    val costPerKilometerCentsX100: Long? = null,
    val averageMonthlySpendCents: Long = 0,
    val totalDistanceKm: Long = 0,
    val monthlyExpenditure: List<MonthlyExpenditure> = emptyList(),
    val monthlyFuelPrice: List<MonthlyFuelPrice> = emptyList(),
)

data class MonthlyExpenditure(
    val year: Int,
    val month: Int,
    val totalCostCents: Long,
)

data class MonthlyFuelPrice(
    val year: Int,
    val month: Int,
    val averagePricePerLiterCents: Long,
    val exactPricePerLiter: Double = 0.0,
)

fun StatsPeriod.startDateMs(currentDateMs: Long): Long? = when (this) {
    StatsPeriod.LAST_30_DAYS -> currentDateMs.minusDaysSafely(30)
    StatsPeriod.LAST_6_MONTHS -> currentDateMs.minusMonthsSafely(6)
    StatsPeriod.YEAR_TO_DATE -> Instant.ofEpochMilli(currentDateMs)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .withDayOfYear(1)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
    StatsPeriod.ALL_TIME -> null
}

private fun Long.isInPeriod(startDateMs: Long?, endDateMs: Long): Boolean =
    (startDateMs == null || this >= startDateMs) && this <= endDateMs

private fun Long.toYearMonth(): YearMonth = YearMonth.from(
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC),
)

private fun Long.minusDaysSafely(days: Long): Long = runCatching {
    Instant.ofEpochMilli(this).minus(days, ChronoUnit.DAYS).toEpochMilli()
}.getOrElse { Long.MIN_VALUE }

private fun Long.minusMonthsSafely(months: Long): Long = runCatching {
    Instant.ofEpochMilli(this)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .minusMonths(months)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
}.getOrElse { Long.MIN_VALUE }
