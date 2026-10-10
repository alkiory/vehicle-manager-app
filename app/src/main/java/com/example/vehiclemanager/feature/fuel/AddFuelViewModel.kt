package com.example.vehiclemanager.feature.fuel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vehiclemanager.core.domain.ActiveVehicleRepository
import com.example.vehiclemanager.core.domain.FuelRecord
import com.example.vehiclemanager.core.domain.FuelRecordRepository
import com.example.vehiclemanager.core.domain.Vehicle
import com.example.vehiclemanager.core.domain.VehicleRepository
import com.example.vehiclemanager.core.ui.navigation.FormDirtyStateHolder
import com.example.vehiclemanager.core.ui.navigation.FormGuardHandler
import com.example.vehiclemanager.core.ui.navigation.FormStateHolder
import com.example.vehiclemanager.core.ui.navigation.NavigationGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AddFuelViewModel @Inject constructor(
    private val activeVehicleRepository: ActiveVehicleRepository,
    private val fuelRecordRepository: FuelRecordRepository,
    private val vehicleRepository: VehicleRepository,
    private val formDirtyStateHolder: FormDirtyStateHolder,
    private val navigationGuard: NavigationGuard,
) : ViewModel() {
    private val dirtyToken = Any()
    private val formStateHolder = FormStateHolder(FuelFormData())
    private var onCloseFormListener: (() -> Unit)? = null

    private val _uiState = MutableStateFlow(AddFuelUiState())
    val uiState: StateFlow<AddFuelUiState> = _uiState.asStateFlow()

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

    private val activeVehicleRecords = activeVehicleRepository.activeVehicle
        .flatMapLatest { vehicle ->
            vehicle?.let { fuelRecordRepository.observeFuelRecords(it.id) }
                ?: flowOf(emptyList())
        }

    init {
        navigationGuard.registerGuard(guardHandler)
        viewModelScope.launch {
            combine(
                activeVehicleRepository.activeVehicle,
                activeVehicleRecords,
            ) { vehicle, records -> vehicle to records }
                .collect { (vehicle, records) ->
                    val previousOdometer = vehicle?.let { activeVehicle ->
                        maxOf(
                            activeVehicle.primaryOdometerKm,
                            records.maxOfOrNull(FuelRecord::odometerKm) ?: 0L,
                        )
                    }
                    _uiState.update { state ->
                        val shouldPrefillOdometer = !state.odometerManuallyEdited
                        var form = state.form
                        if (shouldPrefillOdometer && previousOdometer != null) {
                            form = form.copy(odometerKm = previousOdometer.toString())
                            formStateHolder.setInitial(form, updateCurrent = true)
                            formDirtyStateHolder.setDirty(dirtyToken, false)
                        }
                        state.copy(
                            isLoading = false,
                            activeVehicle = vehicle,
                            previousOdometerKm = previousOdometer,
                            form = form,
                            noActiveVehicle = vehicle == null,
                            isDirty = formStateHolder.isDirty,
                        )
                    }
                }
        }
    }

    fun setOnCloseFormListener(listener: (() -> Unit)?) {
        onCloseFormListener = listener
    }

    fun updateTimestamp(value: Long) = updateForm { copy(timestampMs = value) }

    fun updateOdometer(value: String) {
        _uiState.update { it.copy(odometerManuallyEdited = true) }
        updateForm { copy(odometerKm = value) }
    }

    fun updateLiters(value: String) = updateFuelAmounts(lastEditedField = FuelAmountField.LITERS, value = value)

    fun updatePricePerLiter(value: String) = updateFuelAmounts(lastEditedField = FuelAmountField.PRICE_PER_LITER, value = value)

    fun updateTotalCost(value: String) = updateFuelAmounts(lastEditedField = FuelAmountField.TOTAL_COST, value = value)

    fun updateFullTank(value: Boolean) = updateForm { copy(isFullTank = value) }

    fun updateStationName(value: String) = updateForm { copy(stationName = value) }

    fun updateNotes(value: String) = updateForm { copy(notes = value) }

    fun save() = saveWithCallbacks()

    fun saveWithCallbacks(
        onSuccess: (() -> Unit)? = null,
        onFailure: (() -> Unit)? = null,
    ) {
        val state = _uiState.value
        val errors = state.form.validate(state.previousOdometerKm)
        if (state.activeVehicle == null) {
            _uiState.update { it.copy(noActiveVehicle = true, saveError = "no_active_vehicle") }
            onFailure?.invoke()
            return
        }
        if (errors.hasErrors) {
            _uiState.update { it.copy(errors = errors) }
            onFailure?.invoke()
            return
        }

        val litersX100 = litersToX100(state.form.liters)
        val pricePerLiterCents = calculatePricePerLiterCents(state.form.pricePerLiter)
        val totalCostCents = calculateTotalCostCents(state.form.totalCost)
        if (litersX100 == null || pricePerLiterCents == null || totalCostCents == null) {
            _uiState.update {
                it.copy(
                    errors = FuelValidationErrors(
                        liters = "invalid",
                        pricePerLiter = "invalid",
                    ),
                )
            }
            onFailure?.invoke()
            return
        }

        val record = FuelRecord(
            vehicleId = state.activeVehicle.id,
            timestampMs = state.form.timestampMs,
            odometerKm = state.form.odometerKm.toLong(),
            litersX100 = litersX100,
            pricePerLiterCents = pricePerLiterCents,
            totalCostCents = totalCostCents,
            isFullTank = state.form.isFullTank,
            stationName = state.form.stationName.trim().ifEmpty { null },
            notes = state.form.notes.trim().ifEmpty { null },
        )

        val activeVehicle = state.activeVehicle
        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            runCatching {
                fuelRecordRepository.insertFuelRecord(record)
                if (record.odometerKm > activeVehicle.primaryOdometerKm) {
                    vehicleRepository.updateVehicle(
                        activeVehicle.copy(primaryOdometerKm = record.odometerKm),
                    )
                }
            }.onSuccess {
                resetForm()
                _uiState.update { it.copy(isSaving = false, saveCompleted = true) }
                onSuccess?.invoke()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveError = throwable.message ?: "save_failed",
                    )
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
                errors = FuelValidationErrors(),
                saveError = null,
                fuelAmountsEdited = emptySet(),
            )
        }
    }

    fun consumeSaveCompleted() {
        _uiState.update { it.copy(saveCompleted = false, isDirty = false) }
    }

    override fun onCleared() {
        navigationGuard.unregisterGuard(guardHandler)
        formDirtyStateHolder.setDirty(dirtyToken, false)
        super.onCleared()
    }

    private fun updateForm(transform: FuelFormData.() -> FuelFormData) {
        val updated = formStateHolder.currentValue.transform()
        formStateHolder.update(updated)
        val isDirty = formStateHolder.isDirty
        formDirtyStateHolder.setDirty(dirtyToken, isDirty)
        _uiState.update { state ->
            state.copy(
                form = updated,
                isDirty = isDirty,
                errors = FuelValidationErrors(),
                saveError = null,
            )
        }
    }

    private fun updateFuelAmounts(lastEditedField: FuelAmountField, value: String) {
        _uiState.update { state ->
            val edited = state.fuelAmountsEdited + lastEditedField
            val liters = if (lastEditedField == FuelAmountField.LITERS) value else state.form.liters
            val pricePerLiter = if (lastEditedField == FuelAmountField.PRICE_PER_LITER) value else state.form.pricePerLiter
            val totalCost = if (lastEditedField == FuelAmountField.TOTAL_COST) value else state.form.totalCost

            val missingField = edited.missingAmountField()
            val computed = when (missingField) {
                FuelAmountField.LITERS -> computeLiters(pricePerLiter, totalCost)
                FuelAmountField.PRICE_PER_LITER -> computePricePerLiterCents(liters, totalCost)
                FuelAmountField.TOTAL_COST -> computeTotalCostFromParts(liters, pricePerLiter)
                null -> null
            }

            var form = state.form.copy(
                liters = liters,
                pricePerLiter = pricePerLiter,
                totalCost = totalCost,
                autoFilledField = when {
                    computed != null -> missingField
                    state.form.autoFilledField != null -> state.form.autoFilledField
                    else -> null
                },
            )
            if (computed != null) {
                form = when (missingField) {
                    FuelAmountField.LITERS -> form.copy(liters = computed.toUserText())
                    FuelAmountField.PRICE_PER_LITER -> form.copy(pricePerLiter = computed.toUserText())
                    FuelAmountField.TOTAL_COST -> form.copy(totalCost = computed.toUserText())
                    null -> form
                }
            } else {
                val stale = form.autoFilledField
                if (stale != null && stale != lastEditedField) {
                    form = when (stale) {
                        FuelAmountField.LITERS -> form.copy(liters = "")
                        FuelAmountField.PRICE_PER_LITER -> form.copy(pricePerLiter = "")
                        FuelAmountField.TOTAL_COST -> form.copy(totalCost = "")
                        else -> form
                    }
                    form = form.copy(autoFilledField = null)
                }
            }

            formStateHolder.update(form)
            val isDirty = formStateHolder.isDirty
            formDirtyStateHolder.setDirty(dirtyToken, isDirty)

            state.copy(
                form = form,
                fuelAmountsEdited = edited,
                isDirty = isDirty,
                errors = FuelValidationErrors(),
                saveError = null,
            )
        }
    }
}

data class AddFuelUiState(
    val isLoading: Boolean = true,
    val activeVehicle: Vehicle? = null,
    val previousOdometerKm: Long? = null,
    val form: FuelFormData = FuelFormData(),
    val errors: FuelValidationErrors = FuelValidationErrors(),
    val odometerManuallyEdited: Boolean = false,
    val noActiveVehicle: Boolean = false,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val saveError: String? = null,
    /** Amount fields the user has typed into during this session. */
    val fuelAmountsEdited: Set<FuelAmountField> = emptySet(),
    /** True once the user has modified the form (drives the unsaved-changes guard). */
    val isDirty: Boolean = false,
)
