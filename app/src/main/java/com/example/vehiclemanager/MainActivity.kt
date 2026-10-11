package com.example.vehiclemanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.vehiclemanager.core.domain.ThemePreferenceRepository
import com.example.vehiclemanager.core.ui.navigation.AppScaffold
import com.example.vehiclemanager.core.ui.theme.VehicleManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var themePreferenceRepository: ThemePreferenceRepository

    private val navigationDestinationState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        navigationDestinationState.value = intent?.getStringExtra("navigationDestination")

        setContent {
            val themeMode by themePreferenceRepository.themeMode.collectAsState()
            VehicleManagerTheme(themeMode = themeMode) {
                VehicleManagerApp(initialDestination = navigationDestinationState.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val dest = intent.getStringExtra("navigationDestination")
        if (dest != null) {
            navigationDestinationState.value = dest
        }
    }
}

@Composable
private fun VehicleManagerApp(initialDestination: String? = null) {
    AppScaffold(initialDestination = initialDestination)
}
