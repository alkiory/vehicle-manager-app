package com.example.vehiclemanager.core.data.vehicle

import com.example.vehiclemanager.core.data.fuel.toDomain as fuelEntityToDomain
import com.example.vehiclemanager.core.data.fuel.toEntity as fuelToEntity
import com.example.vehiclemanager.core.data.maintenance.toDomain as maintenanceEntityToDomain
import com.example.vehiclemanager.core.data.maintenance.toEntity as maintenanceToEntity
import com.example.vehiclemanager.core.domain.DeletedVehicle
import com.example.vehiclemanager.core.domain.Vehicle
import com.example.vehiclemanager.core.domain.VehicleDeletionPreview
import com.example.vehiclemanager.core.domain.VehicleDeletionRepository
import javax.inject.Inject

class VehicleDeletionRepositoryImpl @Inject constructor(
    private val cascadeDao: VehicleCascadeDao,
) : VehicleDeletionRepository {
    override suspend fun previewDeletion(vehicleId: Long): VehicleDeletionPreview? {
        val vehicle = cascadeDao.findVehicle(vehicleId) ?: return null
        return VehicleDeletionPreview(
            vehicle = vehicle.toDomain(),
            fuelCount = cascadeDao.fuelRecordsFor(vehicleId).size,
            maintenanceCount = cascadeDao.maintenanceRecordsFor(vehicleId).size,
            scheduleCount = cascadeDao.maintenanceSchedulesFor(vehicleId).size,
        )
    }

    override suspend fun deleteWithUndo(vehicle: Vehicle): DeletedVehicle {
        val rows = cascadeDao.captureAndDelete(vehicle.id)
            ?: error("Vehicle ${vehicle.id} does not exist")
        return DeletedVehicle(
            vehicle = rows.vehicle.toDomain(),
            fuelRecords = rows.fuelRecords.map { it.fuelEntityToDomain() },
            maintenanceRecords = rows.maintenanceRecords.map { it.maintenanceEntityToDomain() },
            maintenanceSchedules = rows.maintenanceSchedules.map { it.maintenanceEntityToDomain() },
        )
    }

    override suspend fun restore(deleted: DeletedVehicle) {
        cascadeDao.restore(
            DeletedVehicleRows(
                vehicle = deleted.vehicle.toEntity(),
                fuelRecords = deleted.fuelRecords.map { it.fuelToEntity() },
                maintenanceRecords = deleted.maintenanceRecords.map { it.maintenanceToEntity() },
                maintenanceSchedules = deleted.maintenanceSchedules.map {
                    it.maintenanceToEntity()
                },
            ),
        )
    }
}
