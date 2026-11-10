package com.example.vehiclemanager.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vehiclemanager.R
import com.example.vehiclemanager.core.domain.FuelRecord
import com.example.vehiclemanager.core.domain.NextRefuelEstimate
import com.example.vehiclemanager.core.domain.TimelineEvent
import com.example.vehiclemanager.core.domain.UpcomingService
import com.example.vehiclemanager.core.ui.Formatters
import com.example.vehiclemanager.core.ui.components.VehicleManagerAppBar
import com.example.vehiclemanager.core.ui.components.VehicleManagerScreen
import com.example.vehiclemanager.core.ui.theme.AppIcons
import com.example.vehiclemanager.core.ui.theme.AppTheme
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    onAddFuel: () -> Unit = {},
    onAddMaintenance: () -> Unit = {},
    onViewAll: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var actionsExpanded by remember { mutableStateOf(false) }

    VehicleManagerScreen(
        topBar = {
            VehicleManagerAppBar(
                titleContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.vehiclemanagericon),
                            contentDescription = null,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Vehicle Manager",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            DashboardActionHub(
                expanded = actionsExpanded,
                onExpandedChange = { actionsExpanded = it },
                onAddFuel = onAddFuel,
                onAddMaintenance = onAddMaintenance,
            )
        },
    ) {
        DashboardContent(
            uiState = uiState,
            onViewAll = onViewAll,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vehicle = uiState.activeVehicle
    if (vehicle == null) {
        Box(
            modifier = modifier
                .background(AppTheme.gradients.screenBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = AppIcons.IconVehicleBadge,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Ningún vehículo activo",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = "Añade un vehículo para ver su consumo y estado de mantenimiento.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.background(AppTheme.gradients.screenBackground),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        state = rememberLazyListState(),
    ) {
        item { GreetingHeader() }
        item { VehicleHeroCard(vehicleName = vehicle.name, odometerKm = vehicle.primaryOdometerKm) }
        item { NextRefuelEstimateCard(estimate = uiState.nextRefuelEstimate) }
        item {
            QuickSummarySection(
                latestFuel = uiState.latestFuelRecord,
                consumptionX100 = uiState.averageConsumptionLitersPer100KmX100,
                costPerKmCentsX100 = uiState.costPerKmCentsX100,
                alerts = uiState.maintenanceAlerts,
                onViewAll = onViewAll,
            )
        }
        if (uiState.timelineEvents.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                TimelineSection(
                    events = uiState.timelineEvents,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun GreetingHeader() {
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Buenos días"
            in 12..19 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }
    Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Tu vehículo,",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "siempre en control.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.NavHome,
                contentDescription = "Perfil",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun VehicleHeroCard(vehicleName: String, odometerKm: Long) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = AppTheme.gradients.vehicleCard,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(20.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(50)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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
                Text(
                    text = vehicleName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = "Vehículo principal · $odometerKm km",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Icon(
                imageVector = AppIcons.IconVehicleBadge,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@Composable
private fun NextRefuelEstimateCard(
    estimate: NextRefuelEstimate,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, AppTheme.gradients.maintenanceCardBorder),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTheme.gradients.maintenanceCard)
                .padding(16.dp),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = AppIcons.IconFuelDrop,
                            contentDescription = null,
                            tint = AppTheme.semanticColors.maintenance,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Próximo repostaje estimado",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (estimate.estimatedRemainingRangeKm != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        if (estimate.averageConsumptionLPer100Km != null) {
                            Column {
                                Text(
                                    text = "Consumo medio",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f L/100 km", estimate.averageConsumptionLPer100Km),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Autonomía restante",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "${estimate.estimatedRemainingRangeKm} km",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        if (estimate.estimatedNextRefuelOdometerKm != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Próximo repostaje",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "${estimate.estimatedNextRefuelOdometerKm} km",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    if (estimate.estimatedNextRefuelDateMs != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val formattedDate = remember(estimate.estimatedNextRefuelDateMs) {
                            val instant = Instant.ofEpochMilli(estimate.estimatedNextRefuelDateMs)
                            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                                .withZone(ZoneId.systemDefault())
                            formatter.format(instant)
                        }
                        Text(
                            text = "Fecha estimada: $formattedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Text(
                        text = "Añade al menos dos repostajes para calcular la estimación.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickSummarySection(
    latestFuel: FuelRecord?,
    consumptionX100: Long?,
    costPerKmCentsX100: Long?,
    alerts: List<UpcomingService>,
    onViewAll: () -> Unit,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Resumen rápido", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Ver todo",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .clickable(onClickLabel = "Ver historial de combustible") { onViewAll() }
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickSummaryCard(
                modifier = Modifier.weight(1f),
                icon = AppIcons.IconFuelDrop,
                chipBrush = AppTheme.gradients.fuelChip,
                label = "Combustible",
                value = when {
                    latestFuel == null -> "Sin registros"
                    consumptionX100 == null -> "Sin datos suficientes"
                    else -> "${formatDecimalHundredths(consumptionX100)} L/100km"
                },
                caption = when {
                    latestFuel == null -> "Añade tu primer repostaje"
                    consumptionX100 == null -> "Se necesitan dos depósitos llenos"
                    else -> "Consumo medio"
                },
            )
            QuickSummaryCard(
                modifier = Modifier.weight(1f),
                icon = AppIcons.IconWrench,
                chipBrush = AppTheme.gradients.maintenanceChip,
                label = "Mantenimiento",
                value = if (alerts.isEmpty()) "Al día" else "${alerts.size} alerta(s)",
                caption = if (alerts.isEmpty()) "Sin alertas próximas" else alerts.first().schedule.serviceTitle,
            )
        }
        Box(modifier = Modifier.padding(top = 12.dp)) {
            QuickSummaryCard(
                modifier = Modifier.fillMaxWidth(),
                icon = AppIcons.IconTrending,
                chipBrush = AppTheme.gradients.barCurrent,
                label = "Coste por kilómetro",
                value = Formatters.formatCostPerKm(costPerKmCentsX100),
                caption = if (costPerKmCentsX100 != null) "Basado en coste total (combustible + mantenimiento)" else null,
                valueColor = AppTheme.semanticColors.cost,
            )
        }
    }
}

@Composable
private fun QuickSummaryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    chipBrush: Brush,
    label: String,
    value: String,
    caption: String?,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(chipBrush, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                modifier = Modifier.padding(top = 2.dp),
            )
            caption?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun DashboardActionHub(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddFuel: () -> Unit,
    onAddMaintenance: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (expanded) {
            ExtendedFloatingActionButton(
                onClick = {
                    onExpandedChange(false)
                    onAddFuel()
                },
                text = { Text(text = "Añadir repostaje") },
                icon = { Icon(imageVector = AppIcons.ActionFuel, contentDescription = null) },
                modifier = Modifier.semantics { contentDescription = "Añadir repostaje" },
            )
            ExtendedFloatingActionButton(
                onClick = {
                    onExpandedChange(false)
                    onAddMaintenance()
                },
                text = { Text(text = "Añadir servicio") },
                icon = { Icon(imageVector = AppIcons.ActionWrench, contentDescription = null) },
                modifier = Modifier.semantics { contentDescription = "Añadir servicio" },
            )
        }
        FloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.semantics { contentDescription = "Acciones rápidas" },
        ) {
            Icon(
                imageVector = if (expanded) AppIcons.ActionClose else AppIcons.ActionPlus,
                contentDescription = null,
            )
        }
    }
}

private fun formatDecimalHundredths(value: Long): String =
    "${value / 100}.${(value % 100).toString().padStart(2, '0')}"

@Composable
private fun TimelineSection(
    events: List<TimelineEvent>,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Column(modifier = modifier) {
        Text(
            text = "Historial Reciente",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            events.forEachIndexed { index, event ->
                TimelineItem(
                    event = event,
                    dateFormatter = dateFormatter,
                    timeFormatter = timeFormatter,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (index < events.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(
    event: TimelineEvent,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier,
) {
    val timestampMillis = event.timestampMs
    val dateStr = try {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestampMillis
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        sdf.format(cal.time)
    } catch (e: Exception) {
        "---"
    }
    val timeStr = try {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestampMillis
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        sdf.format(cal.time)
    } catch (e: Exception) {
        "--:--"
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Side bar with gradient
            val sideBarBrush = when (event) {
                is TimelineEvent.FuelRecordEvent -> AppTheme.gradients.fuelChip
                is TimelineEvent.MaintenanceRecordEvent -> AppTheme.gradients.maintenanceChip
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(sideBarBrush, CircleShape),
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Icon and content
            Column(modifier = Modifier.weight(1f)) {
                when (event) {
                    is TimelineEvent.FuelRecordEvent -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = AppIcons.IconFuelDrop,
                                contentDescription = "Repostaje",
                                tint = AppTheme.semanticColors.fuel,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = event.record.stationName ?: "Gasolinera",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${Formatters.formatLiters(event.record.litersX100)} · ${Formatters.formatMoney(event.record.totalCostCents)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (event.record.pricePerLiterCents > 0) {
                            Text(
                                text = Formatters.formatFuelPrice(event.record.pricePerLiterCents),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    is TimelineEvent.MaintenanceRecordEvent -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = AppIcons.IconWrench,
                                contentDescription = "Servicio",
                                tint = AppTheme.semanticColors.maintenance,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = event.record.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatMoney(event.record.costCents),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$dateStr · $timeStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Amount in semantic cost color
            Text(
                text = Formatters.formatMoney(when (event) {
                    is TimelineEvent.FuelRecordEvent -> event.record.totalCostCents
                    is TimelineEvent.MaintenanceRecordEvent -> event.record.costCents
                }),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AppTheme.semanticColors.cost,
                textAlign = TextAlign.End,
                modifier = Modifier.width(80.dp),
            )
        }
    }
}
