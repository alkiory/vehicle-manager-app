package com.example.vehiclemanager.core.data.vehicle

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.vehiclemanager.core.data.fuel.FuelRecordEntity
import com.example.vehiclemanager.core.data.maintenance.MaintenanceRecordEntity
import com.example.vehiclemanager.core.data.maintenance.MaintenanceScheduleEntity

/** Rows removed with a vehicle, kept in memory so the deletion can be undone. */
data class DeletedVehicleRows(
    val vehicle: VehicleEntity,
    val fuelRecords: List<FuelRecordEntity>,
    val maintenanceRecords: List<MaintenanceRecordEntity>,
    val maintenanceSchedules: List<MaintenanceScheduleEntity>,
)

/**
 * Capture/restore operations that span the vehicle and its child tables. The
 * [Transaction] methods keep the read and the cascading delete (or the whole
 * restore) atomic.
 */
@Dao
abstract class VehicleCascadeDao {
    @Query("SELECT * FROM vehicles WHERE id = :vehicleId")
    abstract suspend fun findVehicle(vehicleId: Long): VehicleEntity?

    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId ORDER BY id ASC")
    abstract suspend fun fuelRecordsFor(vehicleId: Long): List<FuelRecordEntity>

    @Query("SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId ORDER BY id ASC")
    abstract suspend fun maintenanceRecordsFor(vehicleId: Long): List<MaintenanceRecordEntity>

    @Query("SELECT * FROM maintenance_schedules WHERE vehicleId = :vehicleId ORDER BY id ASC")
    abstract suspend fun maintenanceSchedulesFor(vehicleId: Long): List<MaintenanceScheduleEntity>

    @Query("DELETE FROM vehicles WHERE id = :vehicleId")
    abstract suspend fun deleteVehicleById(vehicleId: Long)

    @Insert
    abstract suspend fun insertVehicle(vehicle: VehicleEntity): Long

    @Insert
    abstract suspend fun insertFuelRecords(records: List<FuelRecordEntity>)

    @Insert
    abstract suspend fun insertMaintenanceRecords(records: List<MaintenanceRecordEntity>)

    @Insert
    abstract suspend fun insertMaintenanceSchedules(schedules: List<MaintenanceScheduleEntity>)

    /** Captures the vehicle and its cascaded history, then deletes the vehicle. */
    @Transaction
    open suspend fun captureAndDelete(vehicleId: Long): DeletedVehicleRows? {
        val vehicle = findVehicle(vehicleId) ?: return null
        val rows = DeletedVehicleRows(
            vehicle = vehicle,
            fuelRecords = fuelRecordsFor(vehicleId),
            maintenanceRecords = maintenanceRecordsFor(vehicleId),
            maintenanceSchedules = maintenanceSchedulesFor(vehicleId),
        )
        deleteVehicleById(vehicleId)
        return rows
    }

    /** Re-inserts the rows with their original ids; aborts on any id conflict. */
    @Transaction
    open suspend fun restore(rows: DeletedVehicleRows) {
        check(findVehicle(rows.vehicle.id) == null) {
            "No se puede deshacer: el vehículo ${rows.vehicle.id} ya existe de nuevo."
        }
        insertVehicle(rows.vehicle)
        if (rows.fuelRecords.isNotEmpty()) insertFuelRecords(rows.fuelRecords)
        if (rows.maintenanceRecords.isNotEmpty()) insertMaintenanceRecords(rows.maintenanceRecords)
        if (rows.maintenanceSchedules.isNotEmpty()) {
            insertMaintenanceSchedules(rows.maintenanceSchedules)
        }
    }
}
