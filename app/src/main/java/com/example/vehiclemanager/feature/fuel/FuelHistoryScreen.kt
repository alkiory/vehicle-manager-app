package com.example.vehiclemanager.feature.fuel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vehiclemanager.core.domain.FuelRecord
import com.example.vehiclemanager.core.ui.Formatters
import com.example.vehiclemanager.core.ui.components.VehicleManagerAppBar
import com.example.vehiclemanager.core.ui.components.VehicleManagerScreen
import com.example.vehiclemanager.core.ui.theme.AppIcons
import com.example.vehiclemanager.core.ui.theme.AppTheme
import java.text.DateFormat
import java.util.Date

@Composable
fun FuelHistoryScreen(
    onAddFuel: () -> Unit = {},
    onOpenDetail: (Long) -> Unit = {},
    viewModel: FuelHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    VehicleManagerScreen(
        topBar = {
            VehicleManagerAppBar(
                title = uiState.activeVehicle?.name?.let { "$it · Historial de combustible" } ?: "Historial de combustible",
            )
        },
        floatingActionButton = {
            if (uiState.activeVehicle != null) {
                FloatingActionButton(
                    onClick = onAddFuel,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Añadir repostaje")
                }
            }
        },
    ) {
        if (uiState.activeVehicle == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.gradients.screenBackground)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LocalGasStation,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Añade un vehículo para ver el historial.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        } else if (uiState.records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.gradients.screenBackground)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = AppIcons.IconFuelDrop,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Sin registros de combustible",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        text = "Pulsa el botón + para añadir tu primer repostaje.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.gradients.screenBackground),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.records, key = { it.id }) { record ->
                    FuelRecordItem(
                        record = record,
                        onClick = { onOpenDetail(record.id) },
                        onDelete = { viewModel.requestDelete(record) },
                    )
                }
            }
        }
    }

    uiState.recordPendingDeletion?.let { record ->
        AlertDialog(
            onDismissRequest = { if (!uiState.isDeleting) viewModel.dismissDelete() },
            title = { Text(text = "¿Eliminar este repostaje?") },
            text = {
                Text(
                    text = "Se eliminará el repostaje" +
                        (record.stationName?.let { " de $it" } ?: "") +
                        ". Esta acción no se puede deshacer.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDelete,
                    enabled = !uiState.isDeleting,
                ) {
                    Text(text = if (uiState.isDeleting) "Eliminando…" else "Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::dismissDelete,
                    enabled = !uiState.isDeleting,
                ) {
                    Text(text = "Cancelar")
                }
            },
        )
    }

    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::consumeError,
            title = { Text(text = "No se pudo eliminar el repostaje") },
            text = { Text(text = error) },
            confirmButton = {
                TextButton(onClick = viewModel::consumeError) { Text(text = "Cerrar") }
            },
        )
    }
}

@Composable
private fun FuelRecordItem(
    record: FuelRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateFormatter = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(AppTheme.gradients.fuelChip, CircleShape),
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.stationName ?: dateFormatter.format(Date(record.timestampMs)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${Formatters.formatLiters(record.litersX100)} · ${record.odometerKm} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatMoney(record.totalCostCents),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.semanticColors.cost,
                )
                Text(
                    text = if (record.isFullTank) "Lleno" else "Parcial",
                    color = if (record.isFullTank) {
                        AppTheme.semanticColors.fuel
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar repostaje",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
