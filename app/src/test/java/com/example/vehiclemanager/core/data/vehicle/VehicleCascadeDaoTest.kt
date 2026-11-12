package com.example.vehiclemanager.core.data.vehicle

import com.example.vehiclemanager.core.data.fuel.FuelRecordEntity
import com.example.vehiclemanager.core.data.maintenance.MaintenanceRecordEntity
import com.example.vehiclemanager.core.data.maintenance.MaintenanceScheduleEntity
import com.example.vehiclemanager.core.domain.FuelType
import com.example.vehiclemanager.core.domain.MaintenanceCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class VehicleCascadeDaoTest {
    @Test
    fun captureAndDeleteReturnsTheHistoryAndRemovesEveryRow() = runBlocking {
        val dao = FakeCascadeDao()
        dao.vehicles += vehicleEntity(1)
        dao.fuel += fuelEntity(10, 1)
        dao.fuel += fuelEntity(11, 1)
        dao.maintenance += maintenanceEntity(20, 1)
        dao.schedules += scheduleEntity(30, 1)

        val rows = dao.captureAndDelete(1)

        assertEquals(1L, rows?.vehicle?.id)
        assertEquals(listOf(10L, 11L), rows?.fuelRecords?.map { it.id })
        assertEquals(listOf(20L), rows?.maintenanceRecords?.map { it.id })
        assertEquals(listOf(30L), rows?.maintenanceSchedules?.map { it.id })
        assertTrue(dao.vehicles.isEmpty())
        assertTrue(dao.fuel.isEmpty())
        assertTrue(dao.maintenance.isEmpty())
        assertTrue(dao.schedules.isEmpty())
    }

    @Test
    fun restoreReinsertsTheRowsWithTheirOriginalIds() = runBlocking {
        val dao = FakeCascadeDao()
        dao.vehicles += vehicleEntity(1)
        dao.fuel += fuelEntity(10, 1)
        dao.maintenance += maintenanceEntity(20, 1)
        dao.schedules += scheduleEntity(30, 1)
        val rows = dao.captureAndDelete(1)!!

        dao.restore(rows)

        assertEquals(listOf(1L), dao.vehicles.map { it.id })
        assertEquals(listOf(10L), dao.fuel.map { it.id })
        assertEquals(listOf(20L), dao.maintenance.map { it.id })
        assertEquals(listOf(30L), dao.schedules.map { it.id })
        assertEquals(1L, dao.fuel.single().vehicleId)
    }

    @Test
    fun restoreAbortsWhenTheVehicleIdWasReused() = runBlocking {
        val dao = FakeCascadeDao()
        dao.vehicles += vehicleEntity(1)
        val rows = dao.captureAndDelete(1)!!
        dao.vehicles += vehicleEntity(1, name = "Replacement")

        try {
            dao.restore(rows)
            fail("Expected the restore to refuse a reused vehicle id")
        } catch (error: IllegalStateException) {
            assertTrue(error.message.orEmpty().contains("ya existe"))
        }

        assertEquals(1, dao.vehicles.size)
        assertEquals("Replacement", dao.vehicles.single().name)
        assertTrue(dao.fuel.isEmpty())
    }

    private fun vehicleEntity(id: Long, name: String = "Vehicle $id") = VehicleEntity(
        id = id,
        name = name,
        make = "Make",
        model = "Model",
        year = 2024,
        licensePlate = "PLATE-$id",
        vin = null,
        fuelType = FuelType.PETROL,
        primaryOdometerKm = 10_000,
    )

    private fun fuelEntity(id: Long, vehicleId: Long) = FuelRecordEntity(
        id = id,
        vehicleId = vehicleId,
        timestampMs = 1_000,
        odometerKm = 10_000,
        litersX100 = 4_000,
        pricePerLiterCents = 150,
        totalCostCents = 6_000,
        isFullTank = true,
        stationName = null,
        notes = null,
    )

    private fun maintenanceEntity(id: Long, vehicleId: Long) = MaintenanceRecordEntity(
        id = id,
        vehicleId = vehicleId,
        title = "Oil",
        category = MaintenanceCategory.OIL_CHANGE,
        costCents = 5_000,
        odometerKm = 10_000,
        timestampMs = 1_000,
        notes = null,
        performedBy = null,
    )

    private fun scheduleEntity(id: Long, vehicleId: Long) = MaintenanceScheduleEntity(
        id = id,
        vehicleId = vehicleId,
        serviceTitle = "Oil",
        intervalKm = 10_000,
        intervalMonths = null,
        lastPerformedKm = 10_000,
        lastPerformedDateMs = null,
    )

    /** In-memory stand-in for Room; the transaction semantics are Room's job. */
    private class FakeCascadeDao : VehicleCascadeDao() {
        val vehicles = mutableListOf<VehicleEntity>()
        val fuel = mutableListOf<FuelRecordEntity>()
        val maintenance = mutableListOf<MaintenanceRecordEntity>()
        val schedules = mutableListOf<MaintenanceScheduleEntity>()

        override suspend fun findVehicle(vehicleId: Long): VehicleEntity? =
            vehicles.firstOrNull { it.id == vehicleId }

        override suspend fun fuelRecordsFor(vehicleId: Long): List<FuelRecordEntity> =
            fuel.filter { it.vehicleId == vehicleId }

        override suspend fun maintenanceRecordsFor(vehicleId: Long): List<MaintenanceRecordEntity> =
            maintenance.filter { it.vehicleId == vehicleId }

        override suspend fun maintenanceSchedulesFor(
            vehicleId: Long,
        ): List<MaintenanceScheduleEntity> = schedules.filter { it.vehicleId == vehicleId }

        override suspend fun deleteVehicleById(vehicleId: Long) {
            vehicles.removeAll { it.id == vehicleId }
            fuel.removeAll { it.vehicleId == vehicleId }
            maintenance.removeAll { it.vehicleId == vehicleId }
            schedules.removeAll { it.vehicleId == vehicleId }
        }

        override suspend fun insertVehicle(vehicle: VehicleEntity): Long {
            check(vehicles.none { it.id == vehicle.id }) { "Vehicle id already exists" }
            vehicles += vehicle
            return vehicle.id
        }

        override suspend fun insertFuelRecords(records: List<FuelRecordEntity>) {
            check(records.none { row -> fuel.any { it.id == row.id } }) { "Fuel id already exists" }
            fuel += records
        }

        override suspend fun insertMaintenanceRecords(records: List<MaintenanceRecordEntity>) {
            check(records.none { row -> maintenance.any { it.id == row.id } }) {
                "Maintenance id already exists"
            }
            maintenance += records
        }

        override suspend fun insertMaintenanceSchedules(schedules: List<MaintenanceScheduleEntity>) {
            check(schedules.none { row -> this.schedules.any { it.id == row.id } }) {
                "Schedule id already exists"
            }
            this.schedules += schedules
        }
    }
}
