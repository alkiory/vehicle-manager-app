package com.example.vehiclemanager.feature.fuel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Motorcycle
import androidx.lifecycle.SavedStateHandle
import com.example.vehiclemanager.core.domain.FuelRecord
import com.example.vehiclemanager.core.domain.FuelRecordRepository
import com.example.vehiclemanager.core.domain.FuelType
import com.example.vehiclemanager.core.domain.Vehicle
import com.example.vehiclemanager.core.domain.VehicleRepository
import com.example.vehiclemanager.core.domain.VehicleType
import com.example.vehiclemanager.core.ui.theme.AppIcons
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FuelDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun associatesTheRecordWithItsOwnVehicle() = runTest {
        val record = record(vehicleId = 7)
        val vehicle = vehicle(id = 7, name = "Weekend car", type = VehicleType.CAR)
        val vehicles = FakeVehicleRepository(listOf(vehicle))
        val viewModel = createViewModel(record, vehicles)

        advanceUntilIdle()

        assertEquals(listOf(7L), vehicles.requestedIds)
        assertEquals(vehicle, viewModel.uiState.value.vehicle)
    }

    @Test
    fun presentationUsesTheCanonicalVehicleIconAndTypeLabel() = runTest {
        val record = record(vehicleId = 7)
        val vehicle = vehicle(id = 7, name = "City bike", type = VehicleType.MOTORCYCLE)
        val viewModel = createViewModel(record, FakeVehicleRepository(listOf(vehicle)))

        advanceUntilIdle()

        val presentation = fuelVehiclePresentation(viewModel.uiState.value.vehicle)
        assertEquals("City bike", presentation.name)
        assertEquals("Motocicleta", presentation.typeLabel)
        assertEquals(Icons.Filled.Motorcycle, presentation.icon)
        assertEquals("Motocicleta", presentation.iconDescription)
        assertFalse(presentation.isMissing)
    }

    @Test
    fun usesTheRecordVehicleEvenWhenAnotherVehicleIsActive() = runTest {
        val activeVehicle = vehicle(id = 1, name = "Daily car", type = VehicleType.CAR)
        val recordVehicle = vehicle(id = 7, name = "Work van", type = VehicleType.VAN)
        val record = record(vehicleId = recordVehicle.id)
        val vehicles = FakeVehicleRepository(listOf(activeVehicle, recordVehicle))
        val viewModel = createViewModel(record, vehicles)

        advanceUntilIdle()

        assertEquals(listOf(recordVehicle.id), vehicles.requestedIds)
        assertEquals(recordVehicle, viewModel.uiState.value.vehicle)
        assertEquals("Furgoneta", fuelVehiclePresentation(viewModel.uiState.value.vehicle).typeLabel)
    }

    @Test
    fun missingVehicleReferenceUsesAGracefulFallback() = runTest {
        val record = record(vehicleId = 99)
        val viewModel = createViewModel(record, FakeVehicleRepository(emptyList()))

        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.record)
        assertNull(viewModel.uiState.value.vehicle)
        val presentation = fuelVehiclePresentation(viewModel.uiState.value.vehicle)
        assertTrue(presentation.isMissing)
        assertEquals("Vehículo no disponible", presentation.name)
        assertEquals("El vehículo asociado a este repostaje ya no existe.", presentation.typeLabel)
        assertEquals(AppIcons.IconVehicleBadge, presentation.icon)
    }

    private fun createViewModel(
        record: FuelRecord,
        vehicles: FakeVehicleRepository,
    ): FuelDetailViewModel = FuelDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("fuelRecordId" to record.id)),
        fuelRecordRepository = FakeFuelRecordRepository(listOf(record)),
        vehicleRepository = vehicles,
    )

    private fun record(vehicleId: Long) = FuelRecord(
        id = 42,
        vehicleId = vehicleId,
        timestampMs = 100,
        odometerKm = 10_000,
        litersX100 = 4_000,
        pricePerLiterCents = 179,
        totalCostCents = 7_160,
        isFullTank = true,
        stationName = "Station",
        notes = null,
    )

    private fun vehicle(
        id: Long,
        name: String,
        type: VehicleType,
    ) = Vehicle(
        id = id,
        name = name,
        make = "Make",
        model = "Model",
        year = 2024,
        licensePlate = "PLATE-$id",
        vin = null,
        fuelType = FuelType.PETROL,
        primaryOdometerKm = 10_000,
        vehicleType = type,
    )

    private class FakeFuelRecordRepository(
        initial: List<FuelRecord>,
    ) : FuelRecordRepository {
        private val records = MutableStateFlow(initial)

        override fun observeFuelRecords(vehicleId: Long): Flow<List<FuelRecord>> = records

        override fun observeRecentPrices(): Flow<List<FuelRecord>> = records

        override suspend fun getFuelRecord(id: Long): FuelRecord? =
            records.value.firstOrNull { it.id == id }

        override suspend fun insertFuelRecord(record: FuelRecord): Long = record.id

        override suspend fun updateFuelRecord(record: FuelRecord) = Unit

        override suspend fun deleteFuelRecord(record: FuelRecord) = Unit
    }

    private class FakeVehicleRepository(
        private val availableVehicles: List<Vehicle>,
    ) : VehicleRepository {
        val requestedIds = mutableListOf<Long>()
        override val vehicles: Flow<List<Vehicle>> = MutableStateFlow(availableVehicles)

        override suspend fun getVehicle(id: Long): Vehicle? {
            requestedIds += id
            return availableVehicles.firstOrNull { it.id == id }
        }

        override suspend fun insertVehicle(vehicle: Vehicle): Long = vehicle.id

        override suspend fun updateVehicle(vehicle: Vehicle) = Unit

        override suspend fun deleteVehicle(vehicle: Vehicle) = Unit
    }
}
