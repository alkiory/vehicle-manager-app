package com.example.vehiclemanager.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EstimateRemainingRangeUseCaseTest {
    private val useCase = EstimateRemainingRangeUseCase(CalculateFuelConsumptionUseCase())

    @Test
    fun returnsNullEstimateWhenInsufficientData() {
        val result = useCase(
            activeVehicle = testVehicle(),
            fuelRecords = emptyList(),
            currentDateMs = 1_000_000L,
        )

        assertNull(result.averageConsumptionLPer100Km)
        assertNull(result.estimatedRemainingRangeKm)
        assertNull(result.estimatedNextRefuelOdometerKm)
    }

    @Test
    fun calculatesEstimateCorrectlyWithValidRecords() {
        val records = listOf(
            record(id = 1, timestampMs = 0L, odometerKm = 10_000, litersX100 = 5_000, fullTank = true),
            record(id = 2, timestampMs = 10 * 86_400_000L, odometerKm = 10_500, litersX100 = 3_000, fullTank = true), // 30L for 500km = 6.0 L/100km
        )

        val result = useCase(
            activeVehicle = testVehicle(primaryOdometerKm = 10_500),
            fuelRecords = records,
            currentDateMs = 10 * 86_400_000L,
        )

        assertEquals(6.0, result.averageConsumptionLPer100Km!!, 0.01)
        // Tank capacity 30L / 6.0 * 100 = 500 km range
        assertEquals(500, result.estimatedRemainingRangeKm)
        // 10500 + 500 * 0.9 = 10950 km
        assertEquals(10_950L, result.estimatedNextRefuelOdometerKm)
        assertNotNull(result.estimatedNextRefuelDateMs)
    }

    private fun testVehicle(primaryOdometerKm: Long = 10_000) = Vehicle(
        id = 1,
        name = "Test Car",
        make = "Toyota",
        model = "Corolla",
        year = 2020,
        licensePlate = "ABC-1234",
        vin = null,
        fuelType = FuelType.PETROL,
        primaryOdometerKm = primaryOdometerKm,
    )

    private fun record(
        id: Long,
        timestampMs: Long,
        odometerKm: Long,
        litersX100: Int,
        fullTank: Boolean,
    ) = FuelRecord(
        id = id,
        vehicleId = 1,
        timestampMs = timestampMs,
        odometerKm = odometerKm,
        litersX100 = litersX100,
        pricePerLiterCents = 180,
        totalCostCents = 0,
        isFullTank = fullTank,
        stationName = null,
        notes = null,
    )
}
