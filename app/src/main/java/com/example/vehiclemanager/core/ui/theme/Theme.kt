package com.example.vehiclemanager.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.vehiclemanager.core.domain.ThemeMode

@Composable
fun shouldUseDarkTheme(themeMode: ThemeMode): Boolean {
    return when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}

object AppTheme {
    val semanticColors: AppSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppSemanticColors.current

    val gradients: AppGradients
        @Composable
        @ReadOnlyComposable
        get() = LocalAppGradients.current
}

@Composable
fun VehicleManagerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = shouldUseDarkTheme(themeMode),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> {
            dynamicDarkColorScheme(context)
        }

        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicLightColorScheme(context)
        }

        darkTheme -> VehicleManagerDarkColors
        else -> VehicleManagerLightColors
    }

    val semanticColors = if (darkTheme) DarkAppSemanticColors else LightAppSemanticColors
    val gradients = if (darkTheme) DarkAppGradients else LightAppGradients

    CompositionLocalProvider(
        LocalAppSemanticColors provides semanticColors,
        LocalAppGradients provides gradients,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VehicleManagerTypography,
            shapes = VehicleManagerShapes,
            content = content,
        )
    }
}

@Preview(name = "Light theme", showBackground = true)
@Composable
private fun VehicleManagerLightThemePreview() {
    VehicleManagerTheme(darkTheme = false, dynamicColor = false) {
        Surface {
            Text(text = "Vehicle Manager")
        }
    }
}

@Preview(name = "Dark theme", showBackground = true)
@Composable
private fun VehicleManagerDarkThemePreview() {
    VehicleManagerTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            Text(text = "Vehicle Manager")
        }
    }
}
