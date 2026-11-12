package com.example.vehiclemanager.feature.maintenance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vehiclemanager.core.domain.ActiveVehicleRepository
import com.example.vehiclemanager.core.domain.MaintenanceCategory
import com.example.vehiclemanager.core.domain.MaintenanceRecord
import com.example.vehiclemanager.core.domain.MaintenanceRecordRepository
import com.example.vehiclemanager.core.domain.Vehicle
import com.example.vehiclemanager.core.domain.VehicleRepository
import com.example.vehiclemanager.core.ui.navigation.FormDirtyStateHolder
import com.example.vehiclemanager.core.ui.navigation.FormGuardHandler
import com.example.vehiclemanager.core.ui.navigation.FormStateHolder
import com.example.vehiclemanager.core.ui.navigation.NavigationGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AddMaintenanceViewModel @Inject constructor(
    private val activeVehicleRepository: ActiveVehicleRepository,
    private val maintenanceRecordRepository: MaintenanceRecordRepository,
    private val vehicleRepository: VehicleRepository,
    private val formDirtyStateHolder: FormDirtyStateHolder,
    private val navigationGuard: NavigationGuard,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val recordId = savedStateHandle.get<Long>(RECORD_ID_KEY) ?: 0L
    private val dirtyToken = Any()
    private val formStateHolder = FormStateHolder(MaintenanceFormData())
    private var onCloseFormListener: (() -> Unit)? = null

    private val _uiState = MutableStateFlow(
        AddMaintenanceUiState(isEditing = recordId != 0L, recordId = recordId),
    )
    val uiState: StateFlow<AddMaintenanceUiState> = _uiState.asStateFlow()

    private val guardHandler = object : FormGuardHandler {
        override fun isDirty(): Boolean = formStateHolder.isDirty

        override fun onSave(onSuccess: () -> Unit, onFailure: () -> Unit) {
            saveWithCallbacks(onSuccess, onFailure)
        }

        override fun onDiscard() {
            resetForm()
        }

        override fun onCloseForm() {
            onCloseFormListener?.invoke()
        }
    }

    init {
        navigationGuard.registerGuard(guardHandler)
        if (recordId != 0L) loadRecord(recordId) else startCreateMode()
    }

    fun setOnCloseFormListener(listener: (() -> Unit)?) {
        onCloseFormListener = listener
    }

    override fun onCleared() {
        navigationGuard.unregisterGuard(guardHandler)
        formDirtyStateHolder.setDirty(dirtyToken, false)
        super.onCleared()
    }

    fun updateTitle(value: String) = updateForm { copy(title = value) }

    fun updateCategory(value: MaintenanceCategory) = updateForm { copy(category = value) }

    fun updateCost(value: String) = updateForm { copy(cost = value) }

    fun updateOdometer(value: String) {
        _uiState.update { it.copy(odometerManuallyEdited = true) }
        updateForm { copy(odometerKm = value) }
    }

    fun updateTimestamp(value: Long) = updateForm { copy(timestampMs = value) }

    fun updateNotes(value: String) = updateForm { copy(notes = value) }

    fun updatePerformedBy(value: String) = updateForm { copy(performedBy = value) }

    fun save() = saveWithCallbacks()

    fun saveWithCallbacks(
        onSuccess: (() -> Unit)? = null,
        onFailure: (() -> Unit)? = null,
    ) {
        val state = _uiState.value
        if (state.isSaving || state.saveCompleted) return

        val errors = state.form.validate(state.previousOdometerKm)
        val costCents = maintenanceCostToCents(state.form.cost)
        val odometerKm = state.form.odometerKm.toLongOrNull()
        val completeErrors = errors.copy(
            cost = errors.cost ?: if (costCents == null) "invalid" else null,
            odometer = errors.odometer ?: if (odometerKm == null) "invalid" else null,
        )

        if (completeErrors.hasErrors) {
            _uiState.update { it.copy(errors = completeErrors) }
            onFailure?.invoke()
            return
        }

        val activeVehicle = state.activeVehicle
        if (activeVehicle == null) {
            _uiState.update { it.copy(noActiveVehicle = true, saveError = "no_active_vehicle") }
            onFailure?.invoke()
            return
        }

        val serviceOdometerKm = odometerKm ?: run {
            onFailure?.invoke()
            return
        }

        val record = MaintenanceRecord(
            id = state.recordId,
            vehicleId = activeVehicle.id,
            title = state.form.title.trim(),
            category = state.form.category,
            costCents = costCents ?: run {
                onFailure?.invoke()
                return
            },
            odometerKm = serviceOdometerKm,
            timestampMs = state.form.timestampMs,
            notes = state.form.notes.trim().ifEmpty { null },
            performedBy = state.form.performedBy.trim().ifEmpty { null },
        )

        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            runCatching {
                if (record.id == 0L) {
                    maintenanceRecordRepository.insertMaintenanceRecord(record)
                } else {
                    maintenanceRecordRepository.updateMaintenanceRecord(record)
                }
                if (serviceOdometerKm > activeVehicle.primaryOdometerKm) {
                    vehicleRepository.updateVehicle(
                        activeVehicle.copy(primaryOdometerKm = serviceOdometerKm),
                    )
                }
            }.onSuccess {
                resetForm()
                _uiState.update { it.copy(isSaving = false, saveCompleted = true) }
                onSuccess?.invoke()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isSaving = false, saveError = throwable.message ?: "save_failed")
                }
                onFailure?.invoke()
            }
        }
    }

    fun resetForm() {
        formStateHolder.reset()
        formDirtyStateHolder.setDirty(dirtyToken, false)
        _uiState.update { state ->
            state.copy(
                form = formStateHolder.currentValue,
                isDirty = false,
                errors = MaintenanceValidationErrors(),
                saveError = null,
            )
        }
    }

    fun consumeSaveCompleted() = _uiState.update { it.copy(saveCompleted = false, isDirty = false) }

    private fun startCreateMode() {
        viewModelScope.launch {
            activeVehicleRepository.activeVehicle.collect { vehicle ->
                _uiState.update { state ->
                    val shouldPrefill = !state.odometerManuallyEdited
                    var form = state.form
                    if (shouldPrefill && vehicle != null) {
                        form = form.copy(odometerKm = vehicle.primaryOdometerKm.toString())
                        formStateHolder.setInitial(form, updateCurrent = true)
                        formDirtyStateHolder.setDirty(dirtyToken, false)
                    }
                    state.copy(
                        isLoading = false,
                        activeVehicle = vehicle,
                        previousOdometerKm = vehicle?.primaryOdometerKm,
                        form = form,
                        noActiveVehicle = vehicle == null,
                        isDirty = formStateHolder.isDirty,
                    )
                }
            }
        }
    }

    private fun loadRecord(id: Long) {
        viewModelScope.launch {
            val record = maintenanceRecordRepository.getMaintenanceRecord(id)
            if (record == null) {
                _uiState.update { it.copy(isLoading = false, loadError = "record_not_found") }
                return@launch
            }
            // Keep the record's own vehicle even if another vehicle is active now.
            val vehicle = vehicleRepository.getVehicle(record.vehicleId)
            val initialForm = record.toFormData()
            formStateHolder.setInitial(initialForm, updateCurrent = true)
            formDirtyStateHolder.setDirty(dirtyToken, false)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    activeVehicle = vehicle,
                    recordId = record.id,
                    previousOdometerKm = vehicle?.primaryOdometerKm,
                    form = initialForm,
                    noActiveVehicle = vehicle == null,
                    isDirty = false,
                )
            }
        }
    }

    private fun updateForm(transform: MaintenanceFormData.() -> MaintenanceFormData) {
        val updated = formStateHolder.currentValue.transform()
        formStateHolder.update(updated)
        val isDirty = formStateHolder.isDirty
        formDirtyStateHolder.setDirty(dirtyToken, isDirty)
        _uiState.update { state ->
            state.copy(
                form = updated,
                errors = MaintenanceValidationErrors(),
                saveError = null,
                isDirty = isDirty,
            )
        }
    }

    private companion object {
        const val RECORD_ID_KEY = "recordId"
    }
}

data class AddMaintenanceUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val activeVehicle: Vehicle? = null,
    val previousOdometerKm: Long? = null,
    val recordId: Long = 0,
    val form: MaintenanceFormData = MaintenanceFormData(),
    val errors: MaintenanceValidationErrors = MaintenanceValidationErrors(),
    val odometerManuallyEdited: Boolean = false,
    val noActiveVehicle: Boolean = false,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val loadError: String? = null,
    val saveError: String? = null,
    /** True once the user has modified the form (drives the unsaved-changes guard). */
    val isDirty: Boolean = false,
)

private fun MaintenanceRecord.toFormData() = MaintenanceFormData(
    title = title,
    category = category,
    cost = "${costCents / 100}.${(costCents % 100).toString().padStart(2, '0')}",
    odometerKm = odometerKm.toString(),
    timestampMs = timestampMs,
    notes = notes.orEmpty(),
    performedBy = performedBy.orEmpty(),
)
