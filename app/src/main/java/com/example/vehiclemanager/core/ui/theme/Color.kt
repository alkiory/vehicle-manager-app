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
    primaryContainer = Color(0xFFDCE5FF),
    onPrimaryContainer = Color(0xFF0B2A8A),
    inversePrimary = Color(0xFF7AA2FF),
    secondary = Color(0xFFE8590C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE3D6),
    onSecondaryContainer = Color(0xFF5A2108),
    tertiary = Color(0xFF0D9488),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCFF5EF),
    onTertiaryContainer = Color(0xFF0B3F3A),
    background = Color(0xFFF3F6FD),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE4EAF8),
    onSurfaceVariant = Color(0xFF5B6784),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFF),
    surfaceContainer = Color(0xFFF1F5FD),
    surfaceContainerHigh = Color(0xFFEAEFFB),
    surfaceContainerHighest = Color(0xFFE3E9F8),
    inverseSurface = Color(0xFF1B2540),
    inverseOnSurface = Color(0xFFF3F6FD),
    error = Color(0xFFD6284A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFD9E0),
    onErrorContainer = Color(0xFF5C0F25),
    outline = Color(0xFFD5DCEE),
    outlineVariant = Color(0xFFD5DCEE),
    scrim = Color(0xFF000000),
)

val VehicleManagerDarkColors = darkColorScheme(
    primary = Color(0xFF7AA2FF),
    onPrimary = Color(0xFF0A1B4D),
    primaryContainer = Color(0xFF2F49B8),
    onPrimaryContainer = Color(0xFFE6EAF5),
    inversePrimary = Color(0xFF2F5BEA),
    secondary = Color(0xFFFF7A59),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF4A2318),
    onSecondaryContainer = Color(0xFFFFD9CE),
    tertiary = Color(0xFF2DD4BF),
    onTertiary = Color(0xFF0A1120),
    tertiaryContainer = Color(0xFF0B3F3A),
    onTertiaryContainer = Color(0xFFBFF5EE),
    background = Color(0xFF0A1120),
    onBackground = Color(0xFFE6EAF5),
    surface = Color(0xFF131B2D),
    onSurface = Color(0xFFE6EAF5),
    surfaceVariant = Color(0xFF1B2540),
    onSurfaceVariant = Color(0xFF8D99B3),
    surfaceContainerLowest = Color(0xFF0A1120),
    surfaceContainerLow = Color(0xFF101828),
    surfaceContainer = Color(0xFF131B2D),
    surfaceContainerHigh = Color(0xFF18223A),
    surfaceContainerHighest = Color(0xFF1F2A46),
    inverseSurface = Color(0xFFE6EAF5),
    inverseOnSurface = Color(0xFF0A1120),
    error = Color(0xFFFF5C7A),
    onError = Color(0xFF0A1120),
    errorContainer = Color(0xFF4A1424),
    onErrorContainer = Color(0xFFFFD9E0),
    outline = Color(0xFF2B3652),
    outlineVariant = Color(0xFF1F2A46),
    scrim = Color(0xFF000000),
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
