package com.example.vehiclemanager.feature.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vehiclemanager.core.domain.ActiveVehicleRepository
import com.example.vehiclemanager.core.domain.DeletedVehicle
import com.example.vehiclemanager.core.domain.ExportDatabaseUseCase
import com.example.vehiclemanager.core.domain.ImportDatabaseUseCase
import com.example.vehiclemanager.core.domain.Vehicle
import com.example.vehiclemanager.core.domain.VehicleDeletionPreview
import com.example.vehiclemanager.core.domain.VehicleDeletionRepository
import com.example.vehiclemanager.core.domain.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class VehicleBackupViewModel @Inject constructor(
    private val exportDatabase: ExportDatabaseUseCase,
    private val importDatabase: ImportDatabaseUseCase,
    vehicleRepository: VehicleRepository,
    private val activeVehicleRepository: ActiveVehicleRepository,
    private val vehicleDeletionRepository: VehicleDeletionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(VehicleBackupUiState())
    val uiState: StateFlow<VehicleBackupUiState> = _uiState.asStateFlow()

    private var messageTimerJob: Job? = null
    private var undoJob: Job? = null

    private val _deletionPreview = MutableStateFlow<VehicleDeletionPreview?>(null)
    val deletionPreview: StateFlow<VehicleDeletionPreview?> = _deletionPreview.asStateFlow()

    private val _pendingDeletion = MutableStateFlow<DeletedVehicle?>(null)
    val pendingDeletion: StateFlow<DeletedVehicle?> = _pendingDeletion.asStateFlow()

    private val _deletionError = MutableStateFlow<String?>(null)
    val deletionError: StateFlow<String?> = _deletionError.asStateFlow()

    // `null` hasta la primera emisión de Room: evita mostrar el estado vacío antes de cargar.
    val vehicles: StateFlow<List<Vehicle>?> = vehicleRepository.vehicles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null,
        )

    val activeVehicleId: StateFlow<Long?> = activeVehicleRepository.activeVehicle
        .map { vehicle -> vehicle?.id }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null,
        )

    fun setActiveVehicle(vehicleId: Long) {
        viewModelScope.launch {
            activeVehicleRepository.setActiveVehicle(vehicleId)
        }
    }

    fun requestDelete(vehicle: Vehicle) {
        viewModelScope.launch {
            runCatching { vehicleDeletionRepository.previewDeletion(vehicle.id) }
                .onSuccess { preview ->
                    if (preview != null) _deletionPreview.value = preview
                }
                .onFailure {
                    _deletionError.value = "No se pudo preparar la eliminación"
                }
        }
    }

    fun cancelDelete() {
        _deletionPreview.value = null
    }

    fun confirmDelete() {
        val preview = _deletionPreview.value ?: return
        viewModelScope.launch {
            runCatching { vehicleDeletionRepository.deleteWithUndo(preview.vehicle) }
                .onSuccess { deleted ->
                    _deletionPreview.value = null
                    _pendingDeletion.value = deleted
                    undoJob?.cancel()
                    undoJob = viewModelScope.launch {
                        delay(UNDO_WINDOW_MS)
                        _pendingDeletion.value = null
                    }
                }
                .onFailure { error ->
                    _deletionPreview.value = null
                    _deletionError.value = error.message ?: "No se pudo eliminar el vehículo"
                }
        }
    }

    fun undoDelete() {
        val deleted = _pendingDeletion.value ?: return
        undoJob?.cancel()
        viewModelScope.launch {
            runCatching { vehicleDeletionRepository.restore(deleted) }
                .onSuccess { _pendingDeletion.value = null }
                .onFailure { error ->
                    _pendingDeletion.value = null
                    _deletionError.value = error.message ?: "No se pudo deshacer la eliminación"
                }
        }
    }

    fun dismissUndo() {
        undoJob?.cancel()
        _pendingDeletion.value = null
    }

    fun consumeDeletionError() {
        _deletionError.value = null
    }

    fun exportBackup() {
        messageTimerJob?.cancel()
        _uiState.update { it.copy(isBusy = true, error = null) }
        viewModelScope.launch {
            runCatching { exportDatabase() }
                .onSuccess { json -> _uiState.update { it.copy(isBusy = false, exportJson = json) } }
                .onFailure { error ->
                    _uiState.update { it.copy(isBusy = false, error = error.message ?: "Backup export failed") }
                    startAutoDismissTimer()
                }
        }
    }

    fun consumeExportJson() {
        _uiState.update { it.copy(exportJson = null) }
    }

    fun importBackup(json: String) {
        messageTimerJob?.cancel()
        _uiState.update { it.copy(isBusy = true, error = null, importCompleted = false) }
        viewModelScope.launch {
            runCatching { importDatabase(json) }
                .onSuccess {
                    _uiState.update { it.copy(isBusy = false, importCompleted = true) }
                    startAutoDismissTimer()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isBusy = false, error = error.message ?: "Backup import failed") }
                    startAutoDismissTimer()
                }
        }
    }

    private fun startAutoDismissTimer() {
        messageTimerJob?.cancel()
        messageTimerJob = viewModelScope.launch {
            delay(3_000)
            _uiState.update { it.copy(importCompleted = false, error = null) }
        }
    }

    fun consumeImportCompleted() {
        messageTimerJob?.cancel()
        _uiState.update { it.copy(importCompleted = false) }
    }

    fun consumeError() {
        messageTimerJob?.cancel()
        _uiState.update { it.copy(error = null) }
    }

    private companion object {
        const val UNDO_WINDOW_MS = 8_000L
    }
}

data class VehicleBackupUiState(
    val isBusy: Boolean = false,
    val exportJson: String? = null,
    val importCompleted: Boolean = false,
    val error: String? = null,
)
