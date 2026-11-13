package com.example.vehiclemanager.feature.fuel

import androidx.lifecycle.SavedStateHandle
import com.example.vehiclemanager.core.domain.ActiveVehicleRepository
import com.example.vehiclemanager.core.domain.FuelRecord
import com.example.vehiclemanager.core.domain.FuelRecordRepository
import com.example.vehiclemanager.core.domain.FuelType
import com.example.vehiclemanager.core.domain.Vehicle
import com.example.vehiclemanager.core.domain.VehicleRepository
import com.example.vehiclemanager.core.ui.navigation.FormDirtyStateHolder
import com.example.vehiclemanager.core.ui.navigation.NavigationGuard
import com.example.vehiclemanager.feature.fuel.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddFuelViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dirtyStateHolder = FormDirtyStateHolder()
    private val navigationGuard = NavigationGuard()

    @Test
    fun computesTotalFromLitersAndPrice() = runTest {
        val viewModel = createViewModel()

        viewModel.updateLiters("45.25")
        viewModel.updatePricePerLiter("1.79")

        // 45.25 L x 1.79 €/L = 80.9975 € → 81.00 (HALF_UP)
        assertEquals("81.00", viewModel.uiState.value.form.totalCost)
        assertEquals(FuelAmountField.TOTAL_COST, viewModel.uiState.value.form.autoFilledField)
    }

    @Test
    fun computesPriceFromLitersAndTotal() = runTest {
        val viewModel = createViewModel()

        viewModel.updateLiters("45.25")
        viewModel.updateTotalCost("81.00")

        assertEquals("1.79", viewModel.uiState.value.form.pricePerLiter)
        assertEquals(FuelAmountField.PRICE_PER_LITER, viewModel.uiState.value.form.autoFilledField)
    }

    @Test
    fun computesLitersFromPriceAndTotal() = runTest {
        val viewModel = createViewModel()

        viewModel.updatePricePerLiter("1.79")
        viewModel.updateTotalCost("80.99")

        assertEquals("45.25", viewModel.uiState.value.form.liters)
        assertEquals(FuelAmountField.LITERS, viewModel.uiState.value.form.autoFilledField)
    }

    @Test
    fun autoFilledValueRefreshesWhenAnInputChanges() = runTest {
        val viewModel = createViewModel()

        viewModel.updateLiters("45.25")
        viewModel.updatePricePerLiter("1.79")
        assertEquals("81.00", viewModel.uiState.value.form.totalCost)

        viewModel.updateLiters("50")
        assertEquals("89.50", viewModel.uiState.value.form.totalCost)
    }

    @Test
    fun autoFilledFieldClearsWhenAnInputBecomesInvalid() = runTest {
        val viewModel = createViewModel()

        viewModel.updateLiters("45.25")
        viewModel.updatePricePerLiter("1.79")
        assertEquals("81.00", viewModel.uiState.value.form.totalCost)

        viewModel.updatePricePerLiter("bad")
        assertEquals("", viewModel.uiState.value.form.totalCost)
        assertEquals(null, viewModel.uiState.value.form.autoFilledField)
    }

    @Test
    fun autoFillDoesNotOverwriteTheFieldTheUserIsTyping() = runTest {
        val viewModel = createViewModel()

        viewModel.updateLiters("45.25")
        viewModel.updatePricePerLiter("1.79")
        // The user rewrites total cost by hand: it must not be recomputed.
        viewModel.updateTotalCost("90.00")
        assertEquals("90.00", viewModel.uiState.value.form.totalCost)
    }

    @Test
    fun savingUsesTheAutoFilledTotalCost() = runTest {
        val recordRepository = FakeFuelRecordRepository()
        val viewModel = createViewModel(recordRepository = recordRepository)
        val vehicle = vehicle()

        viewModel.updateOdometer(vehicle.primaryOdometerKm.toString())
        viewModel.updateLiters("45.25")
        viewModel.updatePricePerLiter("1.79")
        viewModel.save()
        advanceUntilIdle()

        val inserted = recordRepository.inserted
        assertTrue(inserted != null)
        assertEquals(8_100L, inserted?.totalCostCents)
        assertEquals(4_525, inserted?.litersX100)
        assertEquals(179L, inserted?.pricePerLiterCents)
    }

    @Test
    fun formEditsMarkTheStateDirty() = runTest {
        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isDirty)
        viewModel.updateLiters("10")
        assertTrue(viewModel.uiState.value.isDirty)
    }

    @Test
    fun revertingEditsBackToInitialReturnsToClean() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDirty)
        viewModel.updateLiters("10")
        assertTrue(viewModel.uiState.value.isDirty)

        viewModel.updateLiters("")
        assertFalse(viewModel.uiState.value.isDirty)
    }

    @Test
    fun resetFormClearsDirtyState() = runTest {
        val viewModel = createViewModel()

        viewModel.updateLiters("10")
        assertTrue(viewModel.uiState.value.isDirty)

        viewModel.resetForm()
        assertFalse(viewModel.uiState.value.isDirty)
        assertEquals("", viewModel.uiState.value.form.liters)
    }

    @Test
    fun repeatedSaveCreatesOnlyOneRecord() = runTest {
        val recordRepository = FakeFuelRecordRepository()
        val viewModel = createViewModel(recordRepository = recordRepository)

        viewModel.updateOdometer("10000")
        viewModel.updateLiters("45.25")
        viewModel.updatePricePerLiter("1.79")
        viewModel.save()
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, recordRepository.insertCalls)
    }

    @Test
    fun editingUpdatesTheSameRecordAndExcludesItsOwnOdometer() = runTest {
        val record = FuelRecord(
            id = 42,
            vehicleId = 1,
            timestampMs = 100,
            odometerKm = 10_000,
            litersX100 = 4_000,
            pricePerLiterCents = 179,
            totalCostCents = 7_160,
            isFullTank = true,
            stationName = "Station",
            notes = null,
        )
        val earlier = record.copy(id = 41, timestampMs = 50, odometerKm = 9_000)
        val later = record.copy(id = 43, timestampMs = 200, odometerKm = 20_000)
        val recordRepository = FakeFuelRecordRepository(listOf(earlier, record, later))
        val viewModel = createViewModel(
            recordRepository = recordRepository,
            savedStateHandle = SavedStateHandle(mapOf("fuelRecordId" to 42L)),
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEditing)
        assertEquals("40.00", viewModel.uiState.value.form.liters)

        viewModel.updateOdometer("9500")
        viewModel.updateLiters("40.00")
        viewModel.updatePricePerLiter("2.00")
        viewModel.save()
        advanceUntilIdle()

        assertNull(recordRepository.inserted)
        assertEquals(42L, recordRepository.updated?.id)
        assertEquals(9_500L, recordRepository.updated?.odometerKm)
        assertEquals(1, recordRepository.updateCalls)
    }

    @Test
    fun backdatedRefuelBetweenNeighboursSavesWithoutLoweringTheVehicleOdometer() = runTest {
        val current = vehicle().copy(primaryOdometerKm = 12_000)
        val recordRepository = FakeFuelRecordRepository(
            listOf(
                fuelRecord(id = 1, timestampMs = 100, odometerKm = 9_000),
                fuelRecord(id = 2, timestampMs = 300, odometerKm = 12_000),
            ),
        )
        val vehicleRepository = FakeVehicleRepository(current)
        val viewModel = createViewModel(
            recordRepository = recordRepository,
            activeVehicle = current,
            vehicleRepository = vehicleRepository,
        )
        advanceUntilIdle()

        viewModel.updateTimestamp(200)
        assertEquals(9_000L, viewModel.uiState.value.previousOdometerKm)
        assertEquals(12_000L, viewModel.uiState.value.nextOdometerKm)

        viewModel.updateOdometer("11000")
        viewModel.updateLiters("40")
        viewModel.updatePricePerLiter("1.50")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, recordRepository.insertCalls)
        assertEquals(11_000L, recordRepository.inserted?.odometerKm)
        assertTrue(vehicleRepository.updated.isEmpty())
    }

    @Test
    fun odometerAboveTheNextChronologicalRefuelIsRejected() = runTest {
        val current = vehicle().copy(primaryOdometerKm = 12_000)
        val recordRepository = FakeFuelRecordRepository(
            listOf(
                fuelRecord(id = 1, timestampMs = 100, odometerKm = 9_000),
                fuelRecord(id = 2, timestampMs = 300, odometerKm = 12_000),
            ),
        )
        val viewModel = createViewModel(
            recordRepository = recordRepository,
            activeVehicle = current,
        )
        advanceUntilIdle()

        viewModel.updateTimestamp(200)
        viewModel.updateOdometer("13000")
        viewModel.updateLiters("40")
        viewModel.updatePricePerLiter("1.50")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(0, recordRepository.insertCalls)
        assertEquals("higher_than_next", viewModel.uiState.value.errors.odometer)
    }

    private fun createViewModel(
        recordRepository: FakeFuelRecordRepository = FakeFuelRecordRepository(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(mapOf("fuelRecordId" to null)),
        activeVehicle: Vehicle = vehicle(),
        vehicleRepository: FakeVehicleRepository = FakeVehicleRepository(activeVehicle),
    ): AddFuelViewModel = AddFuelViewModel(
        activeVehicleRepository = FakeActiveVehicleRepository(activeVehicle),
        fuelRecordRepository = recordRepository,
        vehicleRepository = vehicleRepository,
        formDirtyStateHolder = dirtyStateHolder,
        navigationGuard = navigationGuard,
        savedStateHandle = savedStateHandle,
    )

    private fun vehicle() = Vehicle(
        id = 1,
        name = "Test vehicle",
        make = "Make",
        model = "Model",
        year = 2024,
        licensePlate = "TEST",
        vin = null,
        fuelType = FuelType.PETROL,
        primaryOdometerKm = 10_000,
    )

    private class FakeActiveVehicleRepository(
        vehicle: Vehicle,
    ) : ActiveVehicleRepository {
        override val activeVehicle: StateFlow<Vehicle?> = MutableStateFlow(vehicle)
        override suspend fun setActiveVehicle(vehicleId: Long) = Unit
        override suspend fun clearActiveVehicle() = Unit
    }

    private class FakeFuelRecordRepository(
        initial: List<FuelRecord> = emptyList(),
    ) : FuelRecordRepository {
        private val records = MutableStateFlow(initial)
        var inserted: FuelRecord? = null
        var updated: FuelRecord? = null
        var insertCalls = 0
        var updateCalls = 0

        override fun observeFuelRecords(vehicleId: Long): Flow<List<FuelRecord>> = records
        override fun observeRecentPrices(): Flow<List<FuelRecord>> = records

        override suspend fun getFuelRecord(id: Long): FuelRecord? =
            records.value.firstOrNull { it.id == id }

        override suspend fun insertFuelRecord(record: FuelRecord): Long {
            insertCalls++
            inserted = record
            records.value = records.value + record.copy(id = 1)
            return 1
        }

        override suspend fun updateFuelRecord(record: FuelRecord) {
            updateCalls++
            updated = record
            records.value = records.value.map { if (it.id == record.id) record else it }
        }

        override suspend fun deleteFuelRecord(record: FuelRecord) {
            records.value = records.value.filterNot { it.id == record.id }
        }
    }

    private class FakeVehicleRepository(
        private val vehicle: Vehicle? = null,
    ) : VehicleRepository {
        val updated = mutableListOf<Vehicle>()

        override val vehicles: Flow<List<Vehicle>> = MutableStateFlow(emptyList())
        override suspend fun getVehicle(id: Long): Vehicle? = vehicle
        override suspend fun insertVehicle(vehicle: Vehicle): Long = 1
        override suspend fun updateVehicle(vehicle: Vehicle) {
            updated += vehicle
        }
        override suspend fun deleteVehicle(vehicle: Vehicle) = Unit
    }

    private fun fuelRecord(id: Long, timestampMs: Long, odometerKm: Long) = FuelRecord(
        id = id,
        vehicleId = 1,
        timestampMs = timestampMs,
        odometerKm = odometerKm,
        litersX100 = 4_000,
        pricePerLiterCents = 150,
        totalCostCents = 6_000,
        isFullTank = true,
        stationName = null,
        notes = null,
    )
}
