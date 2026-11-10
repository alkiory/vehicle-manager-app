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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.vehiclemanager.core.ui.Formatters
import com.example.vehiclemanager.core.ui.components.VehicleManagerAppBar
import com.example.vehiclemanager.core.ui.components.VehicleManagerScreen
import com.example.vehiclemanager.core.ui.theme.AppTheme
import java.time.Month
import java.time.format.TextStyle
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
        if (stats.totalCostCents == 0L) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Sin datos en este periodo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                SummaryMetricRow(
                    label = "Coste total",
                    caption = "Combustible + mantenimiento",
                    value = Formatters.formatMoney(stats.totalCostCents),
                )
                SummaryDivider()
                SummaryMetricRow(
                    label = "Combustible",
                    caption = "Suma de repostajes",
                    value = Formatters.formatMoney(stats.totalFuelCostCents),
                )
                SummaryDivider()
                SummaryMetricRow(
                    label = "Mantenimiento",
                    caption = "Suma de servicios",
                    value = Formatters.formatMoney(stats.totalMaintenanceCostCents),
                )
                SummaryDivider()
                SummaryMetricRow(
                    label = "Distancia recorrida",
                    caption = "Del primer al último kilometraje",
                    value = if (stats.totalDistanceKm > 0) Formatters.formatDistance(stats.totalDistanceKm) else "—",
                    missingCaption = "Necesitas al menos 2 registros de kilometraje",
                )
                SummaryDivider()
                SummaryMetricRow(
                    label = "Coste por kilómetro",
                    caption = "Coste total ÷ distancia",
                    value = stats.costPerKilometerCentsX100?.let { Formatters.formatCostPerKm(it) } ?: "—",
                    missingCaption = "Necesitas al menos 2 registros con distancia",
                    valueColor = AppTheme.semanticColors.cost,
                )
                SummaryDivider()
                val monthsCount = stats.monthlyExpenditure.size
                val avgSpendCaption = when {
                    monthsCount <= 0 -> "Coste total ÷ meses"
                    monthsCount == 1 -> {
                        val m = stats.monthlyExpenditure.first()
                        val monthName = Month.of(m.month).getDisplayName(TextStyle.SHORT, Locale("es", "ES")).lowercase().removeSuffix(".")
                        "Coste total ÷ 1 mes ($monthName ${m.year % 100})"
                    }
                    else -> {
                        val first = stats.monthlyExpenditure.first()
                        val last = stats.monthlyExpenditure.last()
                        val firstName = Month.of(first.month).getDisplayName(TextStyle.SHORT, Locale("es", "ES")).lowercase().removeSuffix(".")
                        val lastName = Month.of(last.month).getDisplayName(TextStyle.SHORT, Locale("es", "ES")).lowercase().removeSuffix(".")
                        "Coste total ÷ $monthsCount meses ($firstName ${first.year % 100} – $lastName ${last.year % 100})"
                    }
                }
                SummaryMetricRow(
                    label = "Gasto mensual medio",
                    caption = avgSpendCaption,
                    value = Formatters.formatMonthlySpend(stats.averageMonthlySpendCents),
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricRow(
    label: String,
    caption: String,
    value: String,
    missingCaption: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                text = if (value == "—" && missingCaption != null) missingCaption else caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (value == "—") MaterialTheme.colorScheme.onSurfaceVariant else valueColor,
        )
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
        subtitle = "Combustible + mantenimiento, en € por mes",
    ) {
        if (values.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Sin datos en este periodo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val recentValues = values.takeLast(12)
            val maxCost = remember(recentValues) { recentValues.maxOfOrNull { it.totalCostCents }?.coerceAtLeast(1L) ?: 1L }

            val summaryDesc = "Gráfico de gasto mensual: " + recentValues.joinToString(", ") { exp ->
                val monthName = Month.of(exp.month).getDisplayName(TextStyle.FULL, Locale("es", "ES"))
                "$monthName de ${exp.year}, ${Formatters.formatMoney(exp.totalCostCents)}"
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .semantics { contentDescription = summaryDesc },
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    items(recentValues.size) { index ->
                        val item = recentValues[index]
                        val isCurrent = (index == recentValues.lastIndex)
                        val brush = if (isCurrent) AppTheme.gradients.barCurrent else AppTheme.gradients.barPrevious
                        val barHeightFraction = (item.totalCostCents.toFloat() / maxCost.toFloat()).coerceIn(0.05f, 1f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(60.dp),
                        ) {
                            Text(
                                text = Formatters.formatMoney(item.totalCostCents),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .height(120.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.75f)
                                        .height(120.dp * barHeightFraction)
                                        .background(brush, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val monthLabel = Month.of(item.month).getDisplayName(TextStyle.SHORT, Locale("es", "ES"))
                            Text(
                                text = "$monthLabel ${item.year % 100}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FuelPriceCard(values: List<MonthlyFuelPrice>, modifier: Modifier = Modifier) {
    SummaryCardShell(
        icon = Icons.Default.BarChart,
        chipBrush = AppTheme.gradients.fuelChip,
        title = "Precio del combustible",
        modifier = modifier,
        subtitle = "Precio medio por litro de cada mes, en €/L",
    ) {
        if (values.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Sin datos en este periodo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val recentValues = values.takeLast(12)

            val deltaText = if (recentValues.size >= 2) {
                val current = recentValues.last().exactPricePerLiter
                val previous = recentValues[recentValues.lastIndex - 1].exactPricePerLiter
                val diff = current - previous
                val pct = if (previous > 0.0) (diff / previous) * 100.0 else 0.0
                val sign = if (diff >= 0) "+" else ""
                String.format(Locale.US, "%s%.2f €/L (%s%.1f %%)", sign, diff, sign, pct)
            } else {
                null
            }

            val summaryDesc = "Gráfico de tendencia de precio del combustible: " + recentValues.joinToString(", ") { p ->
                val monthName = Month.of(p.month).getDisplayName(TextStyle.FULL, Locale("es", "ES"))
                "$monthName de ${p.year}, ${Formatters.formatFuelPrice(p.averagePricePerLiterCents)}"
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .semantics { contentDescription = summaryDesc },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Importe ÷ litros de cada mes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Escala ajustada al rango de precios",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (deltaText != null) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = deltaText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (recentValues.size == 1) {
                    val p = recentValues.first()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = Formatters.formatFuelPrice(p.averagePricePerLiterCents),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val monthLabel = Month.of(p.month).getDisplayName(TextStyle.SHORT, Locale("es", "ES"))
                            Text(
                                text = "$monthLabel ${p.year % 100}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    LineChartWithLabels(
                        values = recentValues,
                        lineColor = AppTheme.semanticColors.alert,
                        fillBrush = AppTheme.gradients.priceTrendFill,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LineChartWithLabels(
    values: List<MonthlyFuelPrice>,
    lineColor: Color,
    fillBrush: Brush,
    modifier: Modifier = Modifier,
) {
    val prices = values.map { it.averagePricePerLiterCents }
    val maxPrice = remember(prices) { prices.maxOrNull()?.coerceAtLeast(1L) ?: 1L }
    val minPrice = remember(prices) { prices.minOrNull() ?: 0L }
    val range = (maxPrice - minPrice).coerceAtLeast(1L).toFloat()

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = if (values.size > 1) size.width / (values.size - 1).toFloat() else size.width
                val points = values.mapIndexed { index, item ->
                    val normalized = if (range > 0f) (item.averagePricePerLiterCents - minPrice).toFloat() / range else 0.5f
                    val y = size.height - (normalized * (size.height - 24.dp.toPx()) + 12.dp.toPx())
                    val x = if (values.size > 1) index * step else size.width / 2f
                    Offset(x, y)
                }

                if (points.size >= 2) {
                    val fillPath = Path().apply {
                        moveTo(points.first().x, size.height)
                        lineTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, size.height)
                        close()
                    }
                    drawPath(path = fillPath, brush = fillBrush)

                    points.zipWithNext().forEach { (start, end) ->
                        drawLine(color = lineColor, start = start, end = end, strokeWidth = 3.dp.toPx())
                    }
                }

                points.forEach { point ->
                    drawCircle(color = lineColor, radius = 5.dp.toPx(), center = point)
                    drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = point)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            values.forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = Formatters.formatFuelPrice(item.averagePricePerLiterCents),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    val monthLabel = Month.of(item.month).getDisplayName(TextStyle.SHORT, Locale("es", "ES"))
                    Text(
                        text = "$monthLabel ${item.year % 100}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCardShell(
    icon: ImageVector,
    chipBrush: Brush,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
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
                Column(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            content()
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
