package com.example.vehiclemanager.feature.maintenance

import com.example.vehiclemanager.core.domain.ActiveVehicleRepository
import com.example.vehiclemanager.core.domain.FuelType
import com.example.vehiclemanager.core.domain.MaintenanceCategory
import com.example.vehiclemanager.core.domain.MaintenanceRecord
import com.example.vehiclemanager.core.domain.MaintenanceRecordRepository
import com.example.vehiclemanager.core.domain.Vehicle
import com.example.vehiclemanager.feature.fuel.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MaintenanceHistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun confirmedDeleteRemovesTheSelectedRecord() = runTest {
        val vehicle = vehicle()
        val record = record(vehicle.id)
        val records = MutableStateFlow(listOf(record))
        val repository = FakeMaintenanceRepository(records)
        val viewModel = MaintenanceHistoryViewModel(
            activeVehicleRepository = FakeActiveVehicleRepository(vehicle),
            maintenanceRecordRepository = repository,
        )

        assertEquals(vehicle, viewModel.uiState.first { it.activeVehicle != null }.activeVehicle)
        assertEquals(listOf(record), viewModel.uiState.first { it.records.isNotEmpty() }.records)

        viewModel.requestDelete(record)
        assertEquals(
            record,
            viewModel.uiState.first { it.recordPendingDeletion != null }.recordPendingDeletion,
        )

        viewModel.confirmDelete()

        assertEquals(emptyList<MaintenanceRecord>(), viewModel.uiState.first { it.records.isEmpty() }.records)
        assertEquals(listOf(record), repository.deletedRecords)
    }

    private fun vehicle() = Vehicle(
        id = 7,
        name = "Vehicle",
        make = "Make",
        model = "Model",
        year = 2024,
        licensePlate = "PLATE",
        vin = null,
        fuelType = FuelType.PETROL,
        primaryOdometerKm = 1_000,
    )

    private fun record(vehicleId: Long) = MaintenanceRecord(
        id = 1,
        vehicleId = vehicleId,
        title = "Oil change",
        category = MaintenanceCategory.OIL_CHANGE,
        costCents = 5_500,
        odometerKm = 1_100,
        timestampMs = 100,
        notes = null,
        performedBy = null,
    )

    private class FakeActiveVehicleRepository(vehicle: Vehicle) : ActiveVehicleRepository {
        override val activeVehicle: StateFlow<Vehicle?> = MutableStateFlow(vehicle)
        override suspend fun setActiveVehicle(vehicleId: Long) = Unit
        override suspend fun clearActiveVehicle() = Unit
    }

    private class FakeMaintenanceRepository(
        private val records: MutableStateFlow<List<MaintenanceRecord>>,
    ) : MaintenanceRecordRepository {
        val deletedRecords = mutableListOf<MaintenanceRecord>()

        override fun observeMaintenanceRecords(vehicleId: Long): Flow<List<MaintenanceRecord>> = records
        override suspend fun getMaintenanceRecord(id: Long): MaintenanceRecord? =
            records.value.firstOrNull { it.id == id }

        override suspend fun insertMaintenanceRecord(record: MaintenanceRecord): Long = record.id
        override suspend fun updateMaintenanceRecord(record: MaintenanceRecord) = Unit
        override suspend fun deleteMaintenanceRecord(record: MaintenanceRecord) {
            deletedRecords += record
            records.value = records.value.filterNot { it.id == record.id }
        }
    }
}
