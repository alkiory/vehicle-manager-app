package com.example.vehiclemanager.core.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.vehiclemanager.R
import com.example.vehiclemanager.core.ui.theme.AppIcons
import com.example.vehiclemanager.feature.dashboard.DashboardScreen
import com.example.vehiclemanager.feature.fuel.AddFuelScreen
import com.example.vehiclemanager.feature.fuel.FuelDetailScreen
import com.example.vehiclemanager.feature.fuel.FuelHistoryScreen
import com.example.vehiclemanager.feature.maintenance.AddMaintenanceScreen
import com.example.vehiclemanager.feature.maintenance.MaintenanceHistoryScreen
import com.example.vehiclemanager.feature.settings.SettingsScreen
import com.example.vehiclemanager.feature.statistics.StatisticsScreen
import com.example.vehiclemanager.feature.vehicles.AddEditVehicleScreen
import com.example.vehiclemanager.feature.vehicles.VehiclesScreen

private const val TABLET_BREAKPOINT_DP = 600

private data class PrimaryDestination(
    val label: String,
    val shortLabel: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val iconResId: Int? = null,
    val navigate: (NavHostController) -> Unit,
    val isSelected: (NavDestination?) -> Boolean,
)

private fun NavHostController.navigateTo(destination: Any) {
    when (destination) {
        DashboardRoute -> navigate(DashboardRoute) {
            popUpTo(graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        VehiclesRoute -> navigate(VehiclesRoute) {
            popUpTo(graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        FuelRoute -> navigate(FuelRoute) {
            popUpTo(graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        MaintenanceRoute -> navigate(MaintenanceRoute) {
            popUpTo(graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        StatsRoute -> navigate(StatsRoute) {
            popUpTo(graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        SettingsRoute -> navigate(SettingsRoute) {
            popUpTo(graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

private val primaryDestinations = listOf(
    PrimaryDestination(
        label = "Inicio",
        shortLabel = "Inicio",
        selectedIcon = AppIcons.NavHome,
        unselectedIcon = AppIcons.NavHome,
        iconResId = R.drawable.vehiclemanagericon,
        navigate = { it.navigateTo(DashboardRoute) },
        isSelected = { it?.hasRoute<DashboardRoute>() == true },
    ),
    PrimaryDestination(
        label = "Vehículos",
        shortLabel = "Vehículos",
        selectedIcon = AppIcons.NavVehicles,
        unselectedIcon = AppIcons.NavVehicles,
        navigate = { it.navigateTo(VehiclesRoute) },
        isSelected = { it?.hasRoute<VehiclesRoute>() == true },
    ),
    PrimaryDestination(
        label = "Estadísticas",
        shortLabel = "Estadísticas",
        selectedIcon = AppIcons.NavChart,
        unselectedIcon = AppIcons.NavChart,
        navigate = { it.navigateTo(StatsRoute) },
        isSelected = { it?.hasRoute<StatsRoute>() == true },
    ),
    PrimaryDestination(
        label = "Combustible",
        shortLabel = "Combustible",
        selectedIcon = AppIcons.NavFuel,
        unselectedIcon = AppIcons.NavFuel,
        navigate = { it.navigateTo(FuelRoute) },
        isSelected = { it?.hasRoute<FuelRoute>() == true },
    ),
    PrimaryDestination(
        label = "Servicios",
        shortLabel = "Servicios",
        selectedIcon = AppIcons.NavMaintenance,
        unselectedIcon = AppIcons.NavMaintenance,
        navigate = { it.navigateTo(MaintenanceRoute) },
        isSelected = { it?.hasRoute<MaintenanceRoute>() == true },
    ),
    PrimaryDestination(
        label = "Ajustes",
        shortLabel = "Ajustes",
        selectedIcon = AppIcons.NavSettings,
        unselectedIcon = AppIcons.NavSettings,
        navigate = { it.navigateTo(SettingsRoute) },
        isSelected = { it?.hasRoute<SettingsRoute>() == true },
    ),
)

@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    initialDestination: String? = null,
    navController: NavHostController = rememberNavController(),
    viewModel: AppScaffoldViewModel = hiltViewModel(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val scaffoldState by viewModel.uiState.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(initialDestination) {
        when (initialDestination) {
            "maintenance" -> navController.navigateTo(MaintenanceRoute)
            "fuel" -> navController.navigateTo(FuelRoute)
            "settings" -> navController.navigateTo(SettingsRoute)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= TABLET_BREAKPOINT_DP.dp
        val onDestinationSelected: (PrimaryDestination) -> Unit = { destination ->
            viewModel.onPrimaryDestinationSelected { destination.navigate(navController) }
        }

        if (useNavigationRail) {
            Row(modifier = Modifier.fillMaxSize()) {
                AppNavigationRail(
                    currentDestination = currentDestination,
                    onDestinationSelected = onDestinationSelected,
                )
                AppNavHost(
                    navController = navController,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    AppNavigationBar(
                        currentDestination = currentDestination,
                        onDestinationSelected = onDestinationSelected,
                    )
                },
            ) { paddingValues ->
                AppNavHost(
                    navController = navController,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }

    if (scaffoldState.pendingDestination != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text(text = stringResource(R.string.unsaved_changes_title)) },
            text = { Text(text = stringResource(R.string.unsaved_changes_message)) },
            confirmButton = {
                Button(onClick = viewModel::confirmSave) {
                    Text(text = stringResource(R.string.unsaved_changes_save))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = viewModel::confirmDiscard) {
                        Text(text = stringResource(R.string.unsaved_changes_discard))
                    }
                    TextButton(onClick = viewModel::dismiss) {
                        Text(text = stringResource(R.string.unsaved_changes_keep))
                    }
                }
            },
        )
    }
}

@Composable
private fun AppNavigationBar(
    currentDestination: NavDestination?,
    onDestinationSelected: (PrimaryDestination) -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val indicatorAlpha = if (isDark) 0.20f else 0.12f

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
    ) {
        primaryDestinations.forEach { destination ->
            val selected = destination.isSelected(currentDestination)
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationSelected(destination) },
                alwaysShowLabel = false,
                icon = {
                    val iconResId = destination.iconResId
                    if (iconResId != null) {
                        Icon(
                            painter = painterResource(id = iconResId),
                            contentDescription = destination.label,
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        Icon(
                            imageVector = destination.selectedIcon,
                            contentDescription = destination.label,
                            tint = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
                label = { },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = indicatorAlpha),
                ),
            )
        }
    }
}

@Composable
private fun AppNavigationRail(
    currentDestination: NavDestination?,
    onDestinationSelected: (PrimaryDestination) -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val indicatorAlpha = if (isDark) 0.20f else 0.12f

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        primaryDestinations.forEach { destination ->
            val selected = destination.isSelected(currentDestination)
            NavigationRailItem(
                selected = selected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    val iconResId = destination.iconResId
                    if (iconResId != null) {
                        Icon(
                            painter = painterResource(id = iconResId),
                            contentDescription = destination.label,
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        Icon(
                            imageVector = destination.selectedIcon,
                            contentDescription = destination.label,
                            tint = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
                label = {
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = indicatorAlpha),
                ),
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = DashboardRoute,
        modifier = modifier,
    ) {
        composable<DashboardRoute> {
            DashboardScreen(
                onAddFuel = { navController.navigate(AddFuelRoute()) },
                onAddMaintenance = { navController.navigate(AddEditMaintenanceRoute()) },
                onViewAll = { navController.navigate(FuelRoute) },
                onOpenFuel = { recordId -> navController.navigate(FuelDetailRoute(recordId)) },
                onOpenMaintenance = { recordId -> navController.navigate(AddEditMaintenanceRoute(recordId)) },
            )
        }
        composable<VehiclesRoute> {
            VehiclesScreen(
                onAddVehicle = { navController.navigate(AddEditVehicleRoute()) },
                onOpenVehicle = { vehicleId -> navController.navigate(AddEditVehicleRoute(vehicleId)) },
            )
        }
        composable<AddEditVehicleRoute> {
            AddEditVehicleScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable<FuelRoute> {
            FuelHistoryScreen(
                onAddFuel = { navController.navigate(AddFuelRoute()) },
                onOpenDetail = { recordId -> navController.navigate(FuelDetailRoute(recordId)) },
            )
        }
        composable<AddFuelRoute> {
            AddFuelScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable<FuelDetailRoute> {
            FuelDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onEdit = { recordId -> navController.navigate(AddFuelRoute(recordId)) },
            )
        }
        composable<MaintenanceRoute> {
            MaintenanceHistoryScreen(
                onAddMaintenance = { navController.navigate(AddEditMaintenanceRoute()) },
                onOpenEdit = { recordId -> navController.navigate(AddEditMaintenanceRoute(recordId)) },
            )
        }
        composable<AddEditMaintenanceRoute> {
            AddMaintenanceScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable<StatsRoute> { StatisticsScreen() }
        composable<SettingsRoute> { SettingsScreen() }
    }
}
