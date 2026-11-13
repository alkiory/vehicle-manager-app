package com.example.vehiclemanager.core.domain

data class MaintenanceRecord(
    override val id: Long = 0,
    val vehicleId: Long,
    val title: String,
    val category: MaintenanceCategory,
    val costCents: Long,
    override val odometerKm: Long,
    override val timestampMs: Long,
    val notes: String?,
    val performedBy: String?,
) : OdometerTimelineRecord
