package com.example.vehiclemanager.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

// Legacy gradient constants kept for compilation compatibility until screen restyling tasks
val HeroGradientStart = Color(0xFF2B4FA0)
val HeroGradientEnd = Color(0xFF4675DA)

val VehicleManagerLightColors = lightColorScheme(
    primary = Color(0xFF2F5BEA),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFE8590C),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF0D9488),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF3F6FD),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF1F8),
    onSurfaceVariant = Color(0xFF5B6784),
    error = Color(0xFFD6284A),
    outline = Color(0xFFD5DCEE),
)

val VehicleManagerDarkColors = darkColorScheme(
    primary = Color(0xFF7AA2FF),
    onPrimary = Color(0xFF0A1B4D),
    secondary = Color(0xFFFF7A59),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF2DD4BF),
    onTertiary = Color(0xFF0A1120),
    background = Color(0xFF0A1120),
    onBackground = Color(0xFFE6EAF5),
    surface = Color(0xFF131B2D),
    onSurface = Color(0xFFE6EAF5),
    surfaceVariant = Color(0xFF1A2437),
    onSurfaceVariant = Color(0xFF8D99B3),
    error = Color(0xFFFF5C7A),
    outline = Color(0xFF2B3652),
)

@Immutable
data class AppSemanticColors(
    val fuel: Color,
    val maintenance: Color,
    val cost: Color,
    val alert: Color,
)

val LightAppSemanticColors = AppSemanticColors(
    fuel = Color(0xFF0D9488),
    maintenance = Color(0xFFE8590C),
    cost = Color(0xFF16A34A),
    alert = Color(0xFFD6284A),
)

val DarkAppSemanticColors = AppSemanticColors(
    fuel = Color(0xFF2DD4BF),
    maintenance = Color(0xFFFF7A59),
    cost = Color(0xFF4ADE80),
    alert = Color(0xFFFF5C7A),
)

@Immutable
data class AppGradients(
    val vehicleCard: Brush,
    val maintenanceCard: Brush,
    val maintenanceCardBorder: Color,
    val maintenanceChip: Brush,
    val fuelChip: Brush,
    val screenBackground: Brush,
    val barCurrent: Brush,
    val barPrevious: Brush,
    val priceTrendFill: Brush,
)

val LightAppGradients = AppGradients(
    vehicleCard = Brush.linearGradient(listOf(Color(0xFF4C78F2), Color(0xFF2447C4))),
    maintenanceCard = Brush.linearGradient(listOf(Color(0xFFFFE3D6), Color(0xFFFFF4EE))),
    maintenanceCardBorder = Color(0xFFF5B9A1),
    maintenanceChip = Brush.linearGradient(listOf(Color(0xFFFF8A5C), Color(0xFFE8590C))),
    fuelChip = Brush.linearGradient(listOf(Color(0xFF2DD4BF), Color(0xFF0D9488))),
    screenBackground = Brush.verticalGradient(listOf(Color(0xFFF8FAFF), Color(0xFFE9EFFC))),
    barCurrent = Brush.verticalGradient(listOf(Color(0xFF4C78F2), Color(0xFF2447C4))),
    barPrevious = SolidColor(Color(0xFF4C78F2).copy(alpha = 0.35f)),
    priceTrendFill = Brush.verticalGradient(listOf(Color(0xFFD6284A).copy(alpha = 0.45f), Color(0xFFD6284A).copy(alpha = 0.0f))),
)

val DarkAppGradients = AppGradients(
    vehicleCard = Brush.linearGradient(listOf(Color(0xFF5B83F0), Color(0xFF2F49B8))),
    maintenanceCard = Brush.linearGradient(
        listOf(
            Color(0xFFFF7A59).copy(alpha = 0.28f),
            Color(0xFFFF7A59).copy(alpha = 0.06f)
        )
    ),
    maintenanceCardBorder = Color(0xFFFF7A59).copy(alpha = 0.45f),
    maintenanceChip = Brush.linearGradient(listOf(Color(0xFFFF7A59), Color(0xFFD9573A))),
    fuelChip = Brush.linearGradient(listOf(Color(0xFF2DD4BF), Color(0xFF0D9488))),
    screenBackground = Brush.verticalGradient(listOf(Color(0xFF0E1830), Color(0xFF0A1120))),
    barCurrent = Brush.verticalGradient(listOf(Color(0xFF5B83F0), Color(0xFF2F49B8))),
    barPrevious = SolidColor(Color(0xFF5B83F0).copy(alpha = 0.35f)),
    priceTrendFill = Brush.verticalGradient(listOf(Color(0xFFFF5C7A).copy(alpha = 0.45f), Color(0xFFFF5C7A).copy(alpha = 0.0f))),
)

val LocalAppSemanticColors = staticCompositionLocalOf {
    DarkAppSemanticColors
}

val LocalAppGradients = staticCompositionLocalOf {
    DarkAppGradients
}
