package com.example.vehiclemanager.feature.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vehiclemanager.R
import com.example.vehiclemanager.core.domain.ThemeMode
import com.example.vehiclemanager.core.ui.components.VehicleManagerAppBar
import com.example.vehiclemanager.core.ui.components.VehicleManagerScreen
import com.example.vehiclemanager.core.ui.theme.AppTheme
import com.example.vehiclemanager.feature.vehicles.VehicleBackupViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    backupViewModel: VehicleBackupViewModel = hiltViewModel(),
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val backupState by backupViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val json = backupState.exportJson
        if (uri != null && json != null) {
            scope.launch {
                writeBackup(context, uri, json)
                backupViewModel.consumeExportJson()
            }
        }
    }
    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = readBackup(context, uri)
                if (json == null) {
                    backupViewModel.importBackup("invalid")
                } else {
                    backupViewModel.importBackup(json)
                }
            }
        }
    }

    LaunchedEffect(backupState.exportJson) {
        if (backupState.exportJson != null) {
            createBackupLauncher.launch(context.getString(R.string.backup_export_filename))
        }
    }

    VehicleManagerScreen(
        topBar = { VehicleManagerAppBar(title = "Ajustes") },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.gradients.screenBackground)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Eyebrow + headline
            Text(
                text = "CONFIGURACIÓN",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Ajustes generales",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            // Appearance section
            SettingsCard(title = "Apariencia", icon = Icons.Default.Settings) {
                ThemeModeSelector(
                    currentMode = uiState.themeMode,
                    onSelectMode = viewModel::setThemeMode,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            // Reminder settings section
            SettingsCard(title = stringResource(R.string.reminder_settings_title), icon = Icons.Default.Notifications) {
                val prefs = uiState.reminderPreferences

                // Advance distance input
                OutlinedTextField(
                    value = if (prefs.advanceDistanceKm > 0) "${prefs.advanceDistanceKm}" else "",
                    onValueChange = { value ->
                        val km = value.toLongOrNull() ?: 0L
                        if (km >= 0) viewModel.setAdvanceDistanceKm(km)
                    },
                    label = { Text(stringResource(R.string.reminder_advance_distance_label)) },
                    suffix = { Text(text = stringResource(R.string.reminder_advance_distance_unit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    singleLine = true,
                )

                // Advance days input
                OutlinedTextField(
                    value = if (prefs.advanceDays > 0) "${prefs.advanceDays}" else "",
                    onValueChange = { value ->
                        val days = value.toIntOrNull() ?: 0
                        if (days >= 0) viewModel.setAdvanceDays(days)
                    },
                    label = { Text(stringResource(R.string.reminder_advance_days_label)) },
                    suffix = { Text(text = stringResource(R.string.reminder_advance_days_unit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    singleLine = true,
                )

                // Notification time
                Text(
                    text = stringResource(R.string.reminder_notification_time_label),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 16.dp),
                )
                TimePickerFieldDeckard(
                    initialHours = prefs.notificationTimeHours,
                    initialMinutes = prefs.notificationTimeMinutes,
                    onTimeSelected = { hours, minutes ->
                        viewModel.setNotificationTime(hours, minutes)
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )

                // Subtitle for notifications group
                Text(
                    text = "Notificaciones",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                )

                // Notification Toggles
                ReminderToggleRow(
                    icon = Icons.Default.Notifications,
                    label = stringResource(R.string.reminder_fuel_enabled),
                    checked = prefs.fuelNotificationsEnabled,
                    onCheckedChange = { viewModel.setFuelNotificationsEnabled(it) },
                )

                ReminderToggleRow(
                    icon = Icons.Default.Notifications,
                    label = stringResource(R.string.reminder_tire_pressure_enabled),
                    checked = prefs.tirePressureNotificationsEnabled,
                    onCheckedChange = { viewModel.setTirePressureNotificationsEnabled(it) },
                )

                ReminderToggleRow(
                    icon = Icons.Default.Vibration,
                    label = stringResource(R.string.reminder_vibrate_enabled),
                    checked = prefs.vibrateOnNotification,
                    onCheckedChange = { viewModel.setVibrateOnNotification(it) },
                )
            }

            // Data management section
            SettingsCard(title = stringResource(R.string.data_management_title), icon = Icons.Default.Backup) {
                BackupSection(
                    isBusy = backupState.isBusy,
                    onExport = backupViewModel::exportBackup,
                    onImport = {
                        openBackupLauncher.launch(arrayOf("application/json", "text/json"))
                    },
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            // Backup status feedback
            backupState.let { state ->
                when {
                    state.importCompleted -> StatusRow(
                        text = stringResource(R.string.backup_imported),
                        color = MaterialTheme.colorScheme.tertiary,
                        icon = Icons.Default.CheckCircle,
                    )
                    state.error != null -> Text(
                        text = stringResource(R.string.backup_error, state.error.orEmpty()),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // About section
            SettingsCard(title = "Acerca de", icon = Icons.Default.Info) {
                Text(
                    text = "Vehicle Manager v1.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = "Gestión de vehículos, combustible y mantenimiento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ReminderToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            content()
        }
    }
}

@Composable
private fun BackupSection(
    isBusy: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.backup_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (isBusy) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            Text(
                text = stringResource(R.string.backup_working),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            Button(
                onClick = onExport,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .semantics { contentDescription = "Exportar backup de datos" },
            ) {
                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(text = "  " + stringResource(R.string.export_backup))
            }
            OutlinedButton(
                onClick = onImport,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .semantics { contentDescription = "Importar backup de datos" },
            ) {
                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(text = "  " + stringResource(R.string.import_backup))
            }
        }
    }
}

@Composable
private fun StatusRow(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    icon: ImageVector,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun TimePickerFieldDeckard(
    initialHours: Int,
    initialMinutes: Int,
    onTimeSelected: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = String.format("%02d:%02d", initialHours, initialMinutes),
        onValueChange = {},
        label = { Text(text = stringResource(R.string.reminder_notification_time_label)) },
        readOnly = true,
        modifier = modifier.fillMaxWidth(),
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Seleccionar hora",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp),
            )
        },
    )
}

@Composable
private fun ThemeModeSelector(
    currentMode: ThemeMode,
    onSelectMode: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Tema de la aplicación",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeOptionChip(
                label = "Sistema",
                icon = Icons.Default.Settings,
                selected = currentMode == ThemeMode.SYSTEM,
                onClick = { onSelectMode(ThemeMode.SYSTEM) },
                modifier = Modifier.weight(1f),
            )
            ThemeOptionChip(
                label = "Claro",
                icon = Icons.Default.LightMode,
                selected = currentMode == ThemeMode.LIGHT,
                onClick = { onSelectMode(ThemeMode.LIGHT) },
                modifier = Modifier.weight(1f),
            )
            ThemeOptionChip(
                label = "Oscuro",
                icon = Icons.Default.DarkMode,
                selected = currentMode == ThemeMode.DARK,
                onClick = { onSelectMode(ThemeMode.DARK) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ThemeOptionChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

internal fun writeBackup(context: Context, uri: Uri, json: String) {
    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
        writer.write(json)
    }
}

internal fun readBackup(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
}.getOrNull()
