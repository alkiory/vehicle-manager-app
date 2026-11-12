package com.example.vehiclemanager.feature.fuel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.vehiclemanager.core.domain.FuelRecord
import com.example.vehiclemanager.core.domain.FuelRecordRepository
import com.example.vehiclemanager.core.domain.Vehicle
import com.example.vehiclemanager.core.domain.VehicleRepository
import com.example.vehiclemanager.core.ui.Formatters
import com.example.vehiclemanager.core.ui.components.VehicleManagerAppBar
import com.example.vehiclemanager.core.ui.components.VehicleManagerPrimaryButton
import com.example.vehiclemanager.core.ui.components.VehicleManagerScreen
import com.example.vehiclemanager.core.ui.navigation.FuelDetailRoute
import com.example.vehiclemanager.core.ui.theme.AppIcons
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun FuelDetailScreen(
    onNavigateBack: () -> Unit,
    onEdit: (Long) -> Unit = {},
    viewModel: FuelDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }
    val record = uiState.record

    VehicleManagerScreen(
        topBar = {
            VehicleManagerAppBar(
                title = "Detalles del repostaje",
                actions = {
                    if (record != null) {
                        TextButton(onClick = { onEdit(record.id) }) {
                            Text(text = "Editar")
                        }
                    }
                    TextButton(onClick = onNavigateBack) {
                        Text(text = "Volver")
                    }
                },
            )
        },
    ) {
        when {
            uiState.isLoading -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            }
            record == null -> Text(
                text = "Registro de combustible no encontrado.",
                modifier = Modifier.padding(24.dp),
            )
            else -> FuelDetailContent(
                record = record,
                vehicle = uiState.vehicle,
                isDeleting = uiState.isDeleting,
                onEdit = { onEdit(record.id) },
                onDelete = { confirmDelete = true },
            )
        }
    }

    if (confirmDelete && record != null) {
        val dateFormatter = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
        AlertDialog(
            onDismissRequest = { if (!uiState.isDeleting) confirmDelete = false },
            title = { Text(text = "¿Eliminar este repostaje?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Se eliminará el repostaje del " +
                            "${dateFormatter.format(Date(record.timestampMs))}" +
                            (record.stationName?.let { " en $it" } ?: "") +
                            ". Esta acción no se puede deshacer.",
                    )
                    uiState.deleteError?.let { error ->
                        Text(text = error, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecord(onDeleted = onNavigateBack)
                    },
                    enabled = !uiState.isDeleting,
                ) {
                    Text(text = if (uiState.isDeleting) "Eliminando…" else "Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmDelete = false },
                    enabled = !uiState.isDeleting,
                ) {
                    Text(text = "Cancelar")
                }
            },
        )
    }
}

@Composable
private fun FuelDetailContent(
    record: FuelRecord,
    vehicle: Vehicle?,
    isDeleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateFormatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VehicleContextCard(vehicle)
        Text(
            text = dateFormatter.format(Date(record.timestampMs)),
            style = MaterialTheme.typography.headlineSmall,
        )
        DetailRow("Cantidad", Formatters.formatLiters(record.litersX100))
        DetailRow("Precio por litro", Formatters.formatFuelPrice(record.pricePerLiterCents))
        DetailRow("Coste total", Formatters.formatMoney(record.totalCostCents))
        DetailRow("Odómetro", "${record.odometerKm} km")
        DetailRow("Estado del depósito", if (record.isFullTank) "Depósito lleno" else "Repostaje parcial")
        record.stationName?.let { DetailRow("Gasolinera", it) }
        record.notes?.let { DetailRow("Notas", it) }

        Spacer(modifier = Modifier.height(8.dp))
        VehicleManagerPrimaryButton(
            text = "Editar repostaje",
            onClick = onEdit,
            enabled = !isDeleting,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onDelete,
            enabled = !isDeleting,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Text(text = "Eliminar repostaje")
        }
    }
}

@Composable
private fun VehicleContextCard(vehicle: Vehicle?) {
    val presentation = fuelVehiclePresentation(vehicle)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        ListItem(
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (presentation.isMissing) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            shape = RoundedCornerShape(10.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = presentation.icon,
                        contentDescription = presentation.iconDescription,
                        tint = if (presentation.isMissing) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
            headlineContent = {
                Text(
                    text = presentation.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (presentation.isMissing) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            },
            supportingContent = {
                Text(
                    text = presentation.typeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
        )
    }
}

internal data class FuelVehiclePresentation(
    val icon: ImageVector,
    val iconDescription: String,
    val name: String,
    val typeLabel: String,
    val isMissing: Boolean,
)

internal fun fuelVehiclePresentation(vehicle: Vehicle?): FuelVehiclePresentation =
    if (vehicle == null) {
        FuelVehiclePresentation(
            icon = AppIcons.IconVehicleBadge,
            iconDescription = "Vehículo no disponible",
            name = "Vehículo no disponible",
            typeLabel = "El vehículo asociado a este repostaje ya no existe.",
            isMissing = true,
        )
    } else {
        FuelVehiclePresentation(
            icon = AppIcons.forVehicleType(vehicle.vehicleType),
            iconDescription = vehicle.vehicleType.displayName,
            name = vehicle.name,
            typeLabel = vehicle.vehicleType.displayName,
            isMissing = false,
        )
    }

@Composable
private fun DetailRow(label: String, value: String) {
    ListItem(
        headlineContent = { Text(text = label) },
        supportingContent = { Text(text = value) },
    )
}

@HiltViewModel
class FuelDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fuelRecordRepository: FuelRecordRepository,
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<FuelDetailRoute>()
    private val _uiState = MutableStateFlow(FuelDetailUiState())
    val uiState: StateFlow<FuelDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val initialRecord = fuelRecordRepository.getFuelRecord(route.fuelRecordId)
            if (initialRecord == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            // Resolve the record's own vehicle. Never fall back to the active vehicle.
            val vehicle = vehicleRepository.getVehicle(initialRecord.vehicleId)
            _uiState.update { it.copy(isLoading = false, record = initialRecord, vehicle = vehicle) }
            fuelRecordRepository.observeFuelRecords(initialRecord.vehicleId).collect { records ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        record = records.firstOrNull { it.id == route.fuelRecordId },
                    )
                }
            }
        }
    }

    fun deleteRecord(onDeleted: () -> Unit) {
        val record = _uiState.value.record ?: return
        if (_uiState.value.isDeleting) return

        _uiState.update { it.copy(isDeleting = true, deleteError = null) }
        viewModelScope.launch {
            runCatching { fuelRecordRepository.deleteFuelRecord(record) }
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false) }
                    onDeleted()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            deleteError = throwable.message ?: "delete_failed",
                        )
                    }
                }
        }
    }
}

data class FuelDetailUiState(
    val isLoading: Boolean = true,
    val record: FuelRecord? = null,
    val vehicle: Vehicle? = null,
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
)
