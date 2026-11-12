package com.example.vehiclemanager.core.domain

/** Counts shown in the delete confirmation before a vehicle is removed. */
data class VehicleDeletionPreview(
    val vehicle: Vehicle,
    val fuelCount: Int,
    val maintenanceCount: Int,
    val scheduleCount: Int,
)

/** A removed vehicle plus the history that was cascaded with it. */
data class DeletedVehicle(
    val vehicle: Vehicle,
    val fuelRecords: List<FuelRecord>,
    val maintenanceRecords: List<MaintenanceRecord>,
    val maintenanceSchedules: List<MaintenanceSchedule>,
)

/**
 * Vehicle deletion with the data needed to undo it. Android has no vehicle
 * delete screen in the original client; this repository backs the web-parity
 * delete + undo flow.
 */
interface VehicleDeletionRepository {
    suspend fun previewDeletion(vehicleId: Long): VehicleDeletionPreview?

    suspend fun deleteWithUndo(vehicle: Vehicle): DeletedVehicle

    /** Re-inserts the captured rows; fails if any original id is already taken. */
    suspend fun restore(deleted: DeletedVehicle)
}
