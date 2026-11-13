package com.example.vehiclemanager.core.domain

data class FuelRecord(
    override val id: Long = 0,
    val vehicleId: Long,
    override val timestampMs: Long,
    override val odometerKm: Long,
    val litersX100: Int,
    val pricePerLiterCents: Long,
    val totalCostCents: Long,
    val isFullTank: Boolean,
    val stationName: String?,
    val notes: String?,
) : OdometerTimelineRecord
