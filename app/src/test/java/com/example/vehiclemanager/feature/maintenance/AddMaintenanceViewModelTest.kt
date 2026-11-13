package com.example.vehiclemanager.feature.maintenance

import androidx.lifecycle.SavedStateHandle
import com.example.vehiclemanager.core.domain.ActiveVehicleRepository
import com.example.vehiclemanager.core.domain.FuelType
import com.example.vehiclemanager.core.domain.MaintenanceCategory
import com.example.vehiclemanager.core.domain.MaintenanceRecord
import com.example.vehiclemanager.core.domain.MaintenanceRecordRepository
import com.example.vehiclemanager.core.domain.Vehicle
import com.example.vehiclemanager.core.domain.VehicleRepository
import com.example.vehiclemanager.core.ui.navigation.FormDirtyStateHolder
import com.example.vehiclemanager.core.ui.navigation.NavigationGuard
import com.example.vehiclemanager.feature.fuel.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddMaintenanceViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val navigationGuard = NavigationGuard()

    @Test
    fun savePersistsServiceAndAdvancesVehicleOdometer() = runTest {
        val vehicle = vehicle(10_000)
        val updatedVehicles = mutableListOf<Vehicle>()
        val recordRepository = FakeMaintenanceRepository()
        val viewModel = AddMaintenanceViewModel(
            activeVehicleRepository = FakeActiveVehicleRepository(vehicle),
            maintenanceRecordRepository = recordRepository,
            vehicleRepository = FakeVehicleRepository(updatedVehicles),
            formDirtyStateHolder = FormDirtyStateHolder(),
            navigationGuard = navigationGuard,
            savedStateHandle = SavedStateHandle(mapOf("recordId" to null)),
        )

        viewModel.updateTitle("Brake service")
        viewModel.updateCost("250.00")
        viewModel.updateOdometer("10500")
        viewModel.save()

        assertEquals("Brake service", recordRepository.inserted?.title)
        assertEquals(10_500L, updatedVehicles.single().primaryOdometerKm)
    }

    @Test
    fun repeatedSaveCreatesOnlyOneRecord() = runTest {
        val recordRepository = FakeMaintenanceRepository()
        val viewModel = AddMaintenanceViewModel(
            activeVehicleRepository = FakeActiveVehicleRepository(vehicle(10_000)),
            maintenanceRecordRepository = recordRepository,
            vehicleRepository = FakeVehicleRepository(mutableListOf()),
            formDirtyStateHolder = FormDirtyStateHolder(),
            navigationGuard = navigationGuard,
            savedStateHandle = SavedStateHandle(mapOf("recordId" to null)),
        )

        viewModel.updateTitle("Brake service")
        viewModel.updateCost("250.00")
        viewModel.updateOdometer("10500")
        viewModel.save()
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, recordRepository.insertCalls)
    }

    @Test
    fun editingUpdatesTheSameRecordId() = runTest {
        val record = MaintenanceRecord(
            id = 7,
            vehicleId = 1,
            title = "Oil change",
            category = MaintenanceCategory.OIL_CHANGE,
            costCents = 5_500,
            odometerKm = 10_100,
            timestampMs = 100,
            notes = null,
            performedBy = null,
        )
        val recordRepository = FakeMaintenanceRepository(listOf(record))
        val viewModel = AddMaintenanceViewModel(
            activeVehicleRepository = FakeActiveVehicleRepository(vehicle(10_000)),
            maintenanceRecordRepository = recordRepository,
            vehicleRepository = FakeVehicleRepository(mutableListOf(), vehicle(10_000)),
            formDirtyStateHolder = FormDirtyStateHolder(),
            navigationGuard = navigationGuard,
            savedStateHandle = SavedStateHandle(mapOf("recordId" to 7L)),
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEditing)
        viewModel.updateCost("60.00")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(0, recordRepository.insertCalls)
        assertEquals(7L, recordRepository.updated?.id)
        assertEquals(6_000L, recordRepository.updated?.costCents)
    }

    @Test
    fun formEditsMarkTheStateDirtyAndRegisterWithTheHolder() = runTest {
        val formDirtyStateHolder = FormDirtyStateHolder()
        val viewModel = AddMaintenanceViewModel(
            activeVehicleRepository = FakeActiveVehicleRepository(vehicle(10_000)),
            maintenanceRecordRepository = FakeMaintenanceRepository(),
            vehicleRepository = FakeVehicleRepository(mutableListOf()),
            formDirtyStateHolder = formDirtyStateHolder,
            navigationGuard = navigationGuard,
            savedStateHandle = SavedStateHandle(mapOf("recordId" to null)),
        )

        viewModel.updateTitle("Oil change")

        assertEquals(true, viewModel.uiState.value.isDirty)
        assertEquals(1, formDirtyStateHolder.dirtyTokens.value.size)
    }

    @Test
    fun prefilledStateIsCleanAndRevertingEditsReturnsToClean() = runTest {
        val formDirtyStateHolder = FormDirtyStateHolder()
        val viewModel = AddMaintenanceViewModel(
            activeVehicleRepository = FakeActiveVehicleRepository(vehicle(10_000)),
            maintenanceRecordRepository = FakeMaintenanceRepository(),
            vehicleRepository = FakeVehicleRepository(mutableListOf()),
            formDirtyStateHolder = formDirtyStateHolder,
            navigationGuard = navigationGuard,
            savedStateHandle = SavedStateHandle(mapOf("recordId" to null)),
        )

        assertFalse(viewModel.uiState.value.isDirty)

        viewModel.updateTitle("Tire rotation")
        assertTrue(viewModel.uiState.value.isDirty)

        viewModel.updateTitle("")
        assertFalse(viewModel.uiState.value.isDirty)
    }

    @Test
    fun resetFormClearsDirtyState() = runTest {
        val formDirtyStateHolder = FormDirtyStateHolder()
        val viewModel = AddMaintenanceViewModel(
            activeVehicleRepository = FakeActiveVehicleRepository(vehicle(10_000)),
            maintenanceRecordRepository = FakeMaintenanceRepository(),
            vehicleRepository = FakeVehicleRepository(mutableListOf()),
            formDirtyStateHolder = formDirtyStateHolder,
            navigationGuard = navigationGuard,
            savedStateHandle = SavedStateHandle(mapOf("recordId" to null)),
        )

        viewModel.updateTitle("Tire rotation")
        assertTrue(viewModel.uiState.value.isDirty)

        viewModel.resetForm()
        assertFalse(viewModel.uiState.value.isDirty)
        assertEquals("", viewModel.uiState.value.form.title)
    }

    @Test
    fun backdatedServiceBetweenNeighboursSavesWithoutLoweringTheVehicleOdometer() = runTest {
        val current = vehicle(12_000)
        val updatedVehicles = mutableListOf<Vehicle>()
        val recordRepository = FakeMaintenanceRepository(
            listOf(
                maintenanceRecord(id = 1, timestampMs = 100, odometerKm = 9_000),
                maintenanceRecord(id = 2, timestampMs = 300, odometerKm = 12_000),
            ),
        )
        val viewModel = AddMaintenanceViewModel(
            activeVehicleRepository = FakeActiveVehicleRepository(current),
            maintenanceRecordRepository = recordRepository,
            vehicleRepository = FakeVehicleRepository(updatedVehicles),
            formDirtyStateHolder = FormDirtyStateHolder(),
            navigationGuard = navigationGuard,
            savedStateHandle = SavedStateHandle(mapOf("recordId" to null)),
        )
        advanceUntilIdle()

        viewModel.updateTimestamp(200)
        assertEquals(9_000L, viewModel.uiState.value.previousOdometerKm)
        assertEquals(12_000L, viewModel.uiState.value.nextOdometerKm)

        viewModel.updateTitle("Backdated service")
        viewModel.updateCost("45,50")
        viewModel.updateOdometer("11000")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, recordRepository.insertCalls)
        assertEquals(11_000L, recordRepository.inserted?.odometerKm)
        assertEquals(4_550L, recordRepository.inserted?.costCents)
        assertTrue(updatedVehicles.isEmpty())
    }

    @Test
    fun odometerAboveTheNextChronologicalServiceIsRejected() = runTest {
        val current = vehicle(12_000)
        val recordRepository = FakeMaintenanceRepository(
            listOf(
                maintenanceRecord(id = 1, timestampMs = 100, odometerKm = 9_000),
                maintenanceRecord(id = 2, timestampMs = 300, odometerKm = 12_000),
            ),
        )
        val viewModel = AddMaintenanceViewModel(
            activeVehicleRepository = FakeActiveVehicleRepository(current),
            maintenanceRecordRepository = recordRepository,
            vehicleRepository = FakeVehicleRepository(mutableListOf()),
            formDirtyStateHolder = FormDirtyStateHolder(),
            navigationGuard = navigationGuard,
            savedStateHandle = SavedStateHandle(mapOf("recordId" to null)),
        )
        advanceUntilIdle()

        viewModel.updateTimestamp(200)
        viewModel.updateTitle("Too high")
        viewModel.updateCost("45.50")
        viewModel.updateOdometer("13000")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(0, recordRepository.insertCalls)
        assertEquals("higher_than_next", viewModel.uiState.value.errors.odometer)
    }

    private fun vehicle(odometerKm: Long) = Vehicle(
        id = 1,
        name = "Test vehicle",
        make = "Make",
        model = "Model",
        year = 2024,
        licensePlate = "TEST",
        vin = null,
        fuelType = FuelType.PETROL,
        primaryOdometerKm = odometerKm,
    )

    private class FakeActiveVehicleRepository(vehicle: Vehicle) : ActiveVehicleRepository {
        override val activeVehicle: StateFlow<Vehicle?> = MutableStateFlow(vehicle)
        override suspend fun setActiveVehicle(vehicleId: Long) = Unit
        override suspend fun clearActiveVehicle() = Unit
    }

    private class FakeMaintenanceRepository(
        initial: List<MaintenanceRecord> = emptyList(),
    ) : MaintenanceRecordRepository {
        private val records = MutableStateFlow(initial)
        var inserted: MaintenanceRecord? = null
        var updated: MaintenanceRecord? = null
        var insertCalls = 0
        override fun observeMaintenanceRecords(vehicleId: Long): Flow<List<MaintenanceRecord>> = records
        override suspend fun getMaintenanceRecord(id: Long): MaintenanceRecord? =
            records.value.firstOrNull { it.id == id }
        override suspend fun insertMaintenanceRecord(record: MaintenanceRecord): Long {
            insertCalls++
            inserted = record
            records.value = records.value + record.copy(id = 1)
            return 1
        }
        override suspend fun updateMaintenanceRecord(record: MaintenanceRecord) {
            updated = record
            records.value = records.value.map { if (it.id == record.id) record else it }
        }
        override suspend fun deleteMaintenanceRecord(record: MaintenanceRecord) = Unit
    }

    private fun maintenanceRecord(id: Long, timestampMs: Long, odometerKm: Long) =
        MaintenanceRecord(
            id = id,
            vehicleId = 1,
            title = "Service",
            category = MaintenanceCategory.GENERAL_INSPECTION,
            costCents = 5_000,
            odometerKm = odometerKm,
            timestampMs = timestampMs,
            notes = null,
            performedBy = null,
        )

    private class FakeVehicleRepository(
        private val updatedVehicles: MutableList<Vehicle>,
        private val existingVehicle: Vehicle? = null,
    ) : VehicleRepository {
        override val vehicles: Flow<List<Vehicle>> = MutableStateFlow(emptyList())
        override suspend fun getVehicle(id: Long): Vehicle? = existingVehicle
        override suspend fun insertVehicle(vehicle: Vehicle): Long = vehicle.id
        override suspend fun updateVehicle(vehicle: Vehicle) { updatedVehicles += vehicle }
        override suspend fun deleteVehicle(vehicle: Vehicle) = Unit
    }
}
