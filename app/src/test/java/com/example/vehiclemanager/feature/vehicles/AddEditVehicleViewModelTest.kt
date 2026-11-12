package com.example.vehiclemanager.feature.vehicles

import androidx.lifecycle.SavedStateHandle
import com.example.vehiclemanager.core.domain.FuelType
import com.example.vehiclemanager.core.domain.Vehicle
import com.example.vehiclemanager.core.domain.VehicleRepository
import com.example.vehiclemanager.core.domain.VehicleType
import com.example.vehiclemanager.core.ui.navigation.FormDirtyStateHolder
import com.example.vehiclemanager.feature.fuel.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AddEditVehicleViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun selectedElectricBicycleTypeIsPersistedOnSave() = runTest {
        val repository = FakeVehicleRepository()
        val viewModel = AddEditVehicleViewModel(
            vehicleRepository = repository,
            formDirtyStateHolder = FormDirtyStateHolder(),
            savedStateHandle = SavedStateHandle(mapOf("vehicleId" to 1L)),
        )
        advanceUntilIdle()

        viewModel.updateName("E-bike")
        viewModel.updateMake("Example")
        viewModel.updateModel("Urban")
        viewModel.updateYear("2025")
        viewModel.updateLicensePlate("TEST")
        viewModel.updateFuelType(FuelType.ELECTRIC)
        viewModel.updateVehicleType(VehicleType.ELECTRIC_BICYCLE)
        viewModel.updateOdometer("1200")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(VehicleType.ELECTRIC_BICYCLE, repository.inserted?.vehicleType)
        assertEquals(FuelType.ELECTRIC, repository.inserted?.fuelType)
    }

    private class FakeVehicleRepository : VehicleRepository {
        var inserted: Vehicle? = null

        override val vehicles: Flow<List<Vehicle>> = flowOf(emptyList())

        override suspend fun getVehicle(id: Long): Vehicle? = null

        override suspend fun insertVehicle(vehicle: Vehicle): Long {
            inserted = vehicle
            return 1L
        }

        override suspend fun updateVehicle(vehicle: Vehicle) {
            inserted = vehicle
        }

        override suspend fun deleteVehicle(vehicle: Vehicle) = Unit
    }
}
