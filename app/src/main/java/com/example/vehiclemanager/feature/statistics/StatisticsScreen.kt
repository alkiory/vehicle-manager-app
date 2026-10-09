package com.example.vehiclemanager.feature.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vehiclemanager.core.domain.MonthlyExpenditure
import com.example.vehiclemanager.core.domain.MonthlyFuelPrice
import com.example.vehiclemanager.core.domain.StatsPeriod
import com.example.vehiclemanager.core.domain.VehicleStats
import com.example.vehiclemanager.core.ui.components.VehicleManagerAppBar
import com.example.vehiclemanager.core.ui.components.VehicleManagerScreen
import com.example.vehiclemanager.core.ui.theme.AppTheme
import java.util.Locale

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    VehicleManagerScreen(
        topBar = {
            VehicleManagerAppBar(
                title = uiState.activeVehicle?.name?.let { "$it · Estadísticas" } ?: "Estadísticas",
            )
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
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Añade un vehículo para ver estadísticas.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        } else {
            StatisticsContent(
                stats = uiState.stats,
                selectedPeriod = uiState.selectedPeriod,
                onPeriodSelected = viewModel::selectPeriod,
            )
        }
    }
}

@Composable
private fun StatisticsContent(
    stats: VehicleStats,
    selectedPeriod: StatsPeriod,
    onPeriodSelected: (StatsPeriod) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.gradients.screenBackground),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = onPeriodSelected,
            )
        }
        item { SummaryCard(stats = stats) }
        item {
            MonthlyExpenseCard(
                values = stats.monthlyExpenditure,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            FuelPriceCard(
                values = stats.monthlyFuelPrice,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: StatsPeriod,
    onPeriodSelected: (StatsPeriod) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatsPeriod.entries.forEach { period ->
                val selected = period == selectedPeriod
                FilterChip(
                    selected = selected,
                    onClick = { onPeriodSelected(period) },
                    label = { Text(text = period.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = Color.Transparent,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        borderColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(stats: VehicleStats) {
    SummaryCardShell(
        icon = Icons.Default.AccountBalanceWallet,
        chipBrush = AppTheme.gradients.barCurrent,
        title = "Resumen",
    ) {
        SummaryMetricRow("Coste total", formatCents(stats.totalCostCents))
        SummaryDivider()
        SummaryMetricRow("Combustible", formatCents(stats.totalFuelCostCents))
        SummaryDivider()
        SummaryMetricRow("Mantenimiento", formatCents(stats.totalMaintenanceCostCents))
        SummaryDivider()
        SummaryMetricRow("Distancia recorrida", "${stats.totalDistanceKm} km")
        SummaryDivider()
        SummaryMetricRow(
            "Coste por kilómetro",
            stats.costPerKilometerCentsX100?.let { formatHundredthsCents(it) } ?: "No disponible",
            valueColor = AppTheme.semanticColors.cost,
        )
        SummaryDivider()
        SummaryMetricRow("Gasto mensual medio", formatCents(stats.averageMonthlySpendCents))
    }
}

@Composable
private fun SummaryMetricRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun SummaryDivider() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        ),
    ) {
        Spacer(modifier = Modifier.height(1.dp))
    }
}

@Composable
private fun MonthlyExpenseCard(values: List<MonthlyExpenditure>, modifier: Modifier = Modifier) {
    SummaryCardShell(
        icon = Icons.Default.TrendingUp,
        chipBrush = AppTheme.gradients.vehicleCard,
        title = "Gasto mensual",
        modifier = modifier,
    ) {
        if (values.isEmpty()) {
            Text(
                text = "Sin datos de gasto para este período.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            BarChart(
                values = values.map { it.totalCostCents },
                currentBarBrush = AppTheme.gradients.barCurrent,
                previousBarBrush = AppTheme.gradients.barPrevious,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(top = 12.dp)
                    .semantics { contentDescription = "Gráfico de gasto mensual" },
            )
            Text(
                text = values.joinToString("  ·  ") { "${it.month}/${it.year}: ${formatCents(it.totalCostCents)}" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun FuelPriceCard(values: List<MonthlyFuelPrice>, modifier: Modifier = Modifier) {
    SummaryCardShell(
        icon = Icons.Default.BarChart,
        chipBrush = AppTheme.gradients.fuelChip,
        title = "Tendencia del precio del combustible",
        modifier = modifier,
    ) {
        if (values.isEmpty()) {
            Text(
                text = "Sin datos de precio de combustible para este período.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            LineChart(
                values = values.map { it.averagePricePerLiterCents },
                lineColor = AppTheme.semanticColors.alert,
                fillBrush = AppTheme.gradients.priceTrendFill,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(top = 12.dp)
                    .semantics { contentDescription = "Gráfico de tendencia de precio del combustible" },
            )
            Text(
                text = values.joinToString("  ·  ") {
                    "${it.month}/${it.year}: ${formatCents(it.averagePricePerLiterCents)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SummaryCardShell(
    icon: ImageVector,
    chipBrush: Brush,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            content()
        }
    }
}

@Composable
private fun BarChart(
    values: List<Long>,
    currentBarBrush: Brush,
    previousBarBrush: Brush,
    modifier: Modifier = Modifier,
) {
    val maximum = remember(values) { values.maxOrNull()?.coerceAtLeast(1L) ?: 1L }
    Canvas(modifier = modifier) {
        val slotWidth = size.width / values.size.coerceAtLeast(1)
        val barWidth = slotWidth * 0.62f
        values.forEachIndexed { index, value ->
            val height = size.height * (value.toFloat() / maximum.toFloat())
            val brush = if (index == values.lastIndex) currentBarBrush else previousBarBrush
            drawRect(
                brush = brush,
                topLeft = Offset(index * slotWidth + (slotWidth - barWidth) / 2f, size.height - height),
                size = Size(barWidth, height),
            )
        }
    }
}

@Composable
private fun LineChart(
    values: List<Long>,
    lineColor: Color,
    fillBrush: Brush,
    modifier: Modifier = Modifier,
) {
    val maximum = remember(values) { values.maxOrNull()?.coerceAtLeast(1L) ?: 1L }
    val minimum = remember(values) { values.minOrNull() ?: 0L }
    Canvas(modifier = modifier) {
        if (values.size == 1) {
            val y = size.height / 2f
            drawCircle(color = lineColor, radius = 6.dp.toPx(), center = Offset(size.width / 2f, y))
        } else {
            val range = (maximum - minimum).coerceAtLeast(1L).toFloat()
            val step = size.width / (values.size - 1).toFloat()
            val points = values.mapIndexed { index, value ->
                Offset(
                    x = index * step,
                    y = size.height - ((value - minimum).toFloat() / range * size.height),
                )
            }

            // Draw filled gradient area under the line
            val fillPath = Path().apply {
                moveTo(points.first().x, size.height)
                lineTo(points.first().x, points.first().y)
                points.drop(1).forEach { point ->
                    lineTo(point.x, point.y)
                }
                lineTo(points.last().x, size.height)
                close()
            }
            drawPath(path = fillPath, brush = fillBrush)

            // Draw line
            points.zipWithNext().forEach { (start, end) ->
                drawLine(color = lineColor, start = start, end = end, strokeWidth = 4.dp.toPx())
            }

            // Draw endpoints/markers
            points.forEach { point ->
                drawCircle(color = lineColor, radius = 5.dp.toPx(), center = point)
            }
        }
    }
}

private val StatsPeriod.displayName: String
    get() = when (this) {
        StatsPeriod.LAST_30_DAYS -> "30 días"
        StatsPeriod.LAST_6_MONTHS -> "6 meses"
        StatsPeriod.YEAR_TO_DATE -> "Este año"
        StatsPeriod.ALL_TIME -> "Todo"
    }

private fun formatCents(cents: Long): String =
    String.format(Locale.US, "%.2f", cents / 100.0)

private fun formatHundredthsCents(value: Long): String =
    String.format(Locale.US, "%.2f", value / 100.0)
