package com.example.vehiclemanager.feature.vehicles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vehiclemanager.core.domain.Vehicle
import com.example.vehiclemanager.core.domain.VehicleDeletionPreview
import com.example.vehiclemanager.core.ui.components.VehicleManagerAppBar
import com.example.vehiclemanager.core.ui.components.VehicleManagerScreen
import com.example.vehiclemanager.core.ui.theme.AppIcons
import com.example.vehiclemanager.core.ui.theme.AppTheme

@Composable
fun VehiclesScreen(
    onAddVehicle: () -> Unit = {},
    onOpenVehicle: (Long) -> Unit = {},
    viewModel: VehicleBackupViewModel = hiltViewModel(),
) {
    val vehiclesState by viewModel.vehicles.collectAsState()
    val activeVehicleId by viewModel.activeVehicleId.collectAsState()
    val deletionPreview by viewModel.deletionPreview.collectAsState()
    val pendingDeletion by viewModel.pendingDeletion.collectAsState()
    val deletionError by viewModel.deletionError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(pendingDeletion) {
        val pending = pendingDeletion
        if (pending == null) {
            snackbarHostState.currentSnackbarData?.dismiss()
            return@LaunchedEffect
        }
        val result = snackbarHostState.showSnackbar(
            message = "Vehículo \"${pending.vehicle.name}\" eliminado.",
            actionLabel = "Deshacer",
            duration = SnackbarDuration.Indefinite,
        )
        when (result) {
            SnackbarResult.ActionPerformed -> viewModel.undoDelete()
            SnackbarResult.Dismissed -> viewModel.dismissUndo()
        }
    }

    LaunchedEffect(deletionError) {
        val message = deletionError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeDeletionError()
    }

    VehicleManagerScreen(
        topBar = { VehicleManagerAppBar(title = "Vehículos") },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddVehicle,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.semantics {
                    contentDescription = "Añadir vehículo"
                },
            ) {
                Icon(imageVector = AppIcons.ActionPlus, contentDescription = null)
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.gradients.screenBackground),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column {
                    Text(
                        text = "GESTIÓN",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Tus vehículos",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Gestiona la información y el historial de tu flota.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            val vehicles = vehiclesState
            if (vehicles == null) {
                item { VehicleListLoadingHint() }
            } else if (vehicles.isEmpty()) {
                item { VehicleListEmptyHint(onAddVehicle = onAddVehicle) }
            } else {
                items(vehicles, key = Vehicle::id) { vehicle ->
                    VehicleRow(
                        vehicle = vehicle,
                        isActive = vehicle.id == activeVehicleId,
                        onClick = { onOpenVehicle(vehicle.id) },
                        onActivate = { viewModel.setActiveVehicle(vehicle.id) },
                        onDelete = { viewModel.requestDelete(vehicle) },
                    )
                }
            }
        }
    }

    deletionPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text(text = "¿Eliminar ${preview.vehicle.name}?") },
            text = {
                Text(
                    text = deletionMessage(
                        preview = preview,
                        isActive = preview.vehicle.id == activeVehicleId,
                        remaining = (vehiclesState?.size ?: 1) - 1,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) {
                    Text(text = "Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text(text = "Cancelar")
                }
            },
        )
    }
}

private fun deletionMessage(
    preview: VehicleDeletionPreview,
    isActive: Boolean,
    remaining: Int,
): String {
    val records = buildList {
        if (preview.fuelCount > 0) {
            add("${preview.fuelCount} ${plural(preview.fuelCount, "repostaje", "repostajes")}")
        }
        if (preview.maintenanceCount > 0) {
            add("${preview.maintenanceCount} ${plural(preview.maintenanceCount, "servicio", "servicios")}")
        }
        if (preview.scheduleCount > 0) {
            add(
                "${preview.scheduleCount} " +
                    "${plural(preview.scheduleCount, "recordatorio", "recordatorios")} " +
                    "de mantenimiento",
            )
        }
    }
    val history = if (records.isEmpty()) {
        "No tiene repostajes ni servicios asociados."
    } else {
        "Se eliminarán también ${joinSpanish(records)} asociados a este vehículo."
    }
    val active = when {
        !isActive -> null
        remaining <= 0 -> "Es tu último vehículo, así que la aplicación quedará sin datos."
        else -> "Es el vehículo activo; al eliminarlo se activará otro de tus vehículos."
    }
    return listOfNotNull(history, active, "Esta acción no se puede deshacer.").joinToString(" ")
}

private fun plural(count: Int, singular: String, plural: String): String =
    if (count == 1) singular else plural

private fun joinSpanish(parts: List<String>): String = when (parts.size) {
    0 -> ""
    1 -> parts[0]
    else -> parts.dropLast(1).joinToString(", ") + " y " + parts.last()
}

@Composable
private fun VehicleListLoadingHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun VehicleListEmptyHint(onAddVehicle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.IconVehicleBadge,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = "Aún no hay vehículos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = "Añade tu primer vehículo para empezar a registrar repostajes y servicios.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            OutlinedButton(
                onClick = onAddVehicle,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Icon(imageVector = AppIcons.ActionPlus, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(text = "  Añadir vehículo")
            }
        }
    }
}

@Composable
private fun VehicleRow(
    vehicle: Vehicle,
    isActive: Boolean,
    onClick: () -> Unit,
    onActivate: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Editar ${vehicle.name}" },
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (isActive) AppTheme.gradients.vehicleCard else SolidColor(MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (isActive) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.forVehicleType(vehicle.vehicleType),
                        contentDescription = null,
                        tint = if (isActive) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                ) {
                    Text(
                        text = vehicle.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${vehicle.vehicleType.displayName} · ${vehicle.make} ${vehicle.model} · ${vehicle.primaryOdometerKm} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (isActive) {
                    StatusPill()
                } else {
                    OutlinedButton(
                        onClick = onActivate,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text(text = "Activar")
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.semantics {
                        contentDescription = "Eliminar ${vehicle.name}"
                    },
                ) {
                    Icon(
                        imageVector = AppIcons.ActionDelete,
                        contentDescription = null,
                        tint = if (isActive) {
                            Color.White.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill() {
    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(AppTheme.semanticColors.cost, CircleShape),
        )
        Text(
            text = "  Activo",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}
