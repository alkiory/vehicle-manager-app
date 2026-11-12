package com.example.vehiclemanager.feature.vehicles

import com.example.vehiclemanager.core.domain.DeletedVehicle
import com.example.vehiclemanager.core.domain.ExportDatabaseUseCase
import com.example.vehiclemanager.core.domain.FuelType
import com.example.vehiclemanager.core.domain.ImportDatabaseUseCase
import com.example.vehiclemanager.core.domain.Vehicle
import com.example.vehiclemanager.core.domain.VehicleBackupRepository
import com.example.vehiclemanager.core.domain.VehicleBackupSnapshot
import com.example.vehiclemanager.core.domain.VehicleDeletionPreview
import com.example.vehiclemanager.core.domain.VehicleDeletionRepository
import com.example.vehiclemanager.core.domain.VehicleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleBackupViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val vehiclesFlow = MutableStateFlow<List<Vehicle>>(emptyList())
    private val activeVehicleFlow = MutableStateFlow<Vehicle?>(null)
    private val deletionRepository = FakeVehicleDeletionRepository()
    private var lastActivatedVehicleId: Long? = null

    private lateinit var viewModel: VehicleBackupViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = VehicleBackupViewModel(
            exportDatabase = ExportDatabaseUseCase(NoOpBackupRepository),
            importDatabase = ImportDatabaseUseCase(NoOpBackupRepository),
            vehicleRepository = FakeVehicleRepository(vehiclesFlow),
            activeVehicleRepository = FakeActiveVehicleRepository(
                activeVehicleFlow = activeVehicleFlow,
                onActivate = { id -> lastActivatedVehicleId = id },
            ),
            vehicleDeletionRepository = deletionRepository,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun vehiclesAreReemittedWhenTheRepositoryEmitsNewValues() = runTest {
        val collected = mutableListOf<List<Vehicle>?>()
        val job = launch(testDispatcher) { viewModel.vehicles.collect { collected.add(it) } }
        testDispatcher.scheduler.advanceUntilIdle()

        vehiclesFlow.value = listOf(vehicle(id = 1, odometerKm = 10_000))
        testDispatcher.scheduler.advanceUntilIdle()

        vehiclesFlow.value = listOf(vehicle(id = 1, odometerKm = 10_500))
        testDispatcher.scheduler.advanceUntilIdle()

        job.cancel()

        assertEquals(
            listOf<List<Vehicle>?>(
                null,
                emptyList(),
                listOf(vehicle(id = 1, odometerKm = 10_000)),
                listOf(vehicle(id = 1, odometerKm = 10_500)),
            ),
            collected,
        )
    }

    @Test
    fun activeVehicleIdMirrorsTheActiveVehicle() = runTest {
        val first = vehicle(id = 1)
        val second = vehicle(id = 2)
        activeVehicleFlow.value = first

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(first.id, viewModel.activeVehicleId.first { it != null })

        activeVehicleFlow.value = second
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(second.id, viewModel.activeVehicleId.first { it == second.id })
    }

    @Test
    fun setActiveVehicleDelegatesToTheRepository() = runTest {
        viewModel.setActiveVehicle(vehicleId = 2)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2L, lastActivatedVehicleId)
    }

    @Test
    fun activeVehicleIdStartsAsNullBeforeTheFirstEmission() = runTest {
        assertNull(viewModel.activeVehicleId.value)
    }

    @Test
    fun deletingAVehicleShowsThePreviewThenAnUndoWindowThatExpires() = runTest {
        val target = vehicle(id = 2)
        deletionRepository.preview = VehicleDeletionPreview(
            vehicle = target,
            fuelCount = 2,
            maintenanceCount = 1,
            scheduleCount = 0,
        )
        deletionRepository.deleted = DeletedVehicle(
            vehicle = target,
            fuelRecords = emptyList(),
            maintenanceRecords = emptyList(),
            maintenanceSchedules = emptyList(),
        )

        viewModel.requestDelete(target)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.deletionPreview.value?.fuelCount)

        viewModel.confirmDelete()
        // runCurrent, not advanceUntilIdle: the latter would also run the undo
        // timer and hide the banner before it can be asserted.
        testDispatcher.scheduler.runCurrent()
        assertNull(viewModel.deletionPreview.value)
        assertEquals(target.id, viewModel.pendingDeletion.value?.vehicle?.id)

        testDispatcher.scheduler.advanceTimeBy(8_001)
        testDispatcher.scheduler.runCurrent()
        assertNull(viewModel.pendingDeletion.value)
    }

    @Test
    fun undoRestoresTheCapturedRowsAndClearsTheBanner() = runTest {
        val target = vehicle(id = 2)
        val deleted = DeletedVehicle(
            vehicle = target,
            fuelRecords = emptyList(),
            maintenanceRecords = emptyList(),
            maintenanceSchedules = emptyList(),
        )
        deletionRepository.preview = VehicleDeletionPreview(target, 0, 0, 0)
        deletionRepository.deleted = deleted

        viewModel.requestDelete(target)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.confirmDelete()
        testDispatcher.scheduler.runCurrent()
        assertNotNull(viewModel.pendingDeletion.value)
        viewModel.undoDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(deleted), deletionRepository.restored)
        assertNull(viewModel.pendingDeletion.value)
    }

    @Test
    fun cancelDeleteClosesThePreviewWithoutDeleting() = runTest {
        val target = vehicle(id = 2)
        deletionRepository.preview = VehicleDeletionPreview(target, 0, 0, 0)

        viewModel.requestDelete(target)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.cancelDelete()

        assertNull(viewModel.deletionPreview.value)
        assertTrue(deletionRepository.restored.isEmpty())
        assertNull(viewModel.pendingDeletion.value)
    }

    @Test
    fun importBackup_succeedsAndAutoDismissesAfter3000ms() = runTest {
        val validJson = """{"schemaVersion":1,"activeVehicleId":null,"vehicles":[],"fuelRecords":[],"maintenanceRecords":[],"maintenanceSchedules":[]}"""
        viewModel.importBackup(validJson)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.importCompleted)

        testDispatcher.scheduler.advanceTimeBy(3_001)
        testDispatcher.scheduler.runCurrent()

        assertFalse(viewModel.uiState.value.importCompleted)
    }

    @Test
    fun importBackup_failureAutoDismissesErrorAfter3000ms() = runTest {
        val failingRepo = FailingBackupRepository()
        val failingViewModel = VehicleBackupViewModel(
            exportDatabase = ExportDatabaseUseCase(failingRepo),
            importDatabase = ImportDatabaseUseCase(failingRepo),
            vehicleRepository = FakeVehicleRepository(vehiclesFlow),
            activeVehicleRepository = FakeActiveVehicleRepository(activeVehicleFlow) { id -> lastActivatedVehicleId = id },
            vehicleDeletionRepository = deletionRepository,
        )

        failingViewModel.importBackup("invalid")
        testDispatcher.scheduler.runCurrent()

        assertNotNull(failingViewModel.uiState.value.error)

        testDispatcher.scheduler.advanceTimeBy(3_001)
        testDispatcher.scheduler.runCurrent()

        assertNull(failingViewModel.uiState.value.error)
    }

    private fun vehicle(
        id: Long,
        odometerKm: Long = 1_000,
    ) = Vehicle(
        id = id,
        name = "Vehicle $id",
        make = "Make",
        model = "Model",
        year = 2024,
        licensePlate = "PLATE-$id",
        vin = null,
        fuelType = FuelType.PETROL,
        primaryOdometerKm = odometerKm,
    )

    private class FakeVehicleRepository(
        private val vehicleFlow: Flow<List<Vehicle>>,
    ) : VehicleRepository {
        override val vehicles: Flow<List<Vehicle>> = vehicleFlow

        override suspend fun getVehicle(id: Long): Vehicle? = vehicleFlow.first()
            .firstOrNull { it.id == id }

        override suspend fun insertVehicle(vehicle: Vehicle): Long = vehicle.id

        override suspend fun updateVehicle(vehicle: Vehicle) = Unit

        override suspend fun deleteVehicle(vehicle: Vehicle) = Unit
    }

    private class FakeVehicleDeletionRepository : VehicleDeletionRepository {
        var preview: VehicleDeletionPreview? = null
        var deleted: DeletedVehicle? = null
        val restored = mutableListOf<DeletedVehicle>()

        override suspend fun previewDeletion(vehicleId: Long): VehicleDeletionPreview? = preview

        override suspend fun deleteWithUndo(vehicle: Vehicle): DeletedVehicle = deleted
            ?: DeletedVehicle(vehicle, emptyList(), emptyList(), emptyList())

        override suspend fun restore(deleted: DeletedVehicle) {
            restored += deleted
        }
    }

    private class FakeActiveVehicleRepository(
        private val activeVehicleFlow: MutableStateFlow<Vehicle?>,
        private val onActivate: (Long) -> Unit,
    ) : com.example.vehiclemanager.core.domain.ActiveVehicleRepository {
        override val activeVehicle: StateFlow<Vehicle?> = activeVehicleFlow

        override suspend fun setActiveVehicle(vehicleId: Long) = onActivate(vehicleId)

        override suspend fun clearActiveVehicle() {
            activeVehicleFlow.value = null
        }
    }

    private object NoOpBackupRepository : VehicleBackupRepository {
        override suspend fun readSnapshot() = VehicleBackupSnapshot(
            activeVehicleId = null,
            vehicles = emptyList(),
            fuelRecords = emptyList(),
            maintenanceRecords = emptyList(),
            maintenanceSchedules = emptyList(),
        )

        override suspend fun replaceSnapshot(snapshot: VehicleBackupSnapshot) = Unit
    }

    private class FailingBackupRepository : VehicleBackupRepository {
        override suspend fun readSnapshot(): VehicleBackupSnapshot {
            throw IllegalStateException("Corrupt backup file")
        }

        override suspend fun replaceSnapshot(snapshot: VehicleBackupSnapshot) {
            throw IllegalStateException("Corrupt backup file")
        }
    }
}
