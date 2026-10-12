package com.example.vehiclemanager.feature.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.vehiclemanager.BuildConfig
import com.example.vehiclemanager.R
import com.example.vehiclemanager.core.domain.ThemeMode
import com.example.vehiclemanager.core.domain.notification.NotificationPermissionStatus
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
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkNotificationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            backupViewModel.consumeImportCompleted()
            backupViewModel.consumeError()
            viewModel.consumeSaveSuccessMessage()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    LaunchedEffect(uiState.triggerPermissionRequest) {
        if (uiState.triggerPermissionRequest) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            viewModel.consumeTriggerPermissionRequest()
        }
    }

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

    if (uiState.showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissRationale() },
            title = { Text("¿Activar notificaciones?") },
            text = { Text("Vehicle Manager te avisa de repostajes y servicios próximos. ¿Activar notificaciones?") },
            confirmButton = {
                TextButton(onClick = { viewModel.onAcceptRationale() }) {
                    Text("Activar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissRationale() }) {
                    Text("Ahora no")
                }
            },
        )
    }

    VehicleManagerScreen(
        topBar = { VehicleManagerAppBar(title = "Ajustes") },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.gradients.screenBackground),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 100.dp),
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
                        currentMode = uiState.draft.themeMode,
                        onSelectMode = viewModel::setThemeMode,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                // Reminder settings section
                SettingsCard(title = stringResource(R.string.reminder_settings_title), icon = Icons.Default.Notifications) {
                    val draft = uiState.draft
                    val errors = uiState.validationErrors

                    // Notification status card
                    NotificationStatusCard(
                        status = uiState.notificationPermissionStatus,
                        nextCheckTimeStr = uiState.nextCheckTimeStr,
                        onRequestPermission = {
                            if (uiState.notificationPermissionStatus == NotificationPermissionStatus.PERMANENTLY_DENIED ||
                                uiState.notificationPermissionStatus == NotificationPermissionStatus.APP_NOTIFICATIONS_DISABLED
                            ) {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        },
                        onSendTestNotification = viewModel::sendTestNotification,
                        modifier = Modifier.padding(top = 12.dp),
                    )

                    if (!uiState.isIgnoringBatteryOptimizations && !uiState.dismissBatteryHint) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Optimización de batería activa",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "El ahorro de energía del sistema puede retrasar la ejecución de los recordatorios automáticos.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Button(
                                        onClick = {
                                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(text = "Configurar", style = MaterialTheme.typography.labelMedium)
                                    }
                                    OutlinedButton(
                                        onClick = viewModel::dismissBatteryHint,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(text = "Ocultar", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }

                    // Advance distance input
                    ReminderNumericInputField(
                        valueText = draft.advanceDistanceKmText,
                        onValueChange = viewModel::setAdvanceDistanceKmText,
                        label = stringResource(R.string.reminder_advance_distance_label),
                        suffix = stringResource(R.string.reminder_advance_distance_unit),
                        errorMessage = errors.advanceDistanceError,
                        modifier = Modifier.padding(top = 12.dp),
                    )

                    // Advance days input
                    ReminderNumericInputField(
                        valueText = draft.advanceDaysText,
                        onValueChange = viewModel::setAdvanceDaysText,
                        label = stringResource(R.string.reminder_advance_days_label),
                        suffix = stringResource(R.string.reminder_advance_days_unit),
                        errorMessage = errors.advanceDaysError,
                        modifier = Modifier.padding(top = 12.dp),
                    )

                    Text(
                        text = "Regla de evaluación: Se notifica cuando el kilometraje restante es menor o igual a la distancia de antelación, o la fecha de vencimiento está a menos de los días indicados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                    )

                    // Notification time
                    Text(
                        text = stringResource(R.string.reminder_notification_time_label),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    TimePickerFieldDeckard(
                        initialHours = draft.notificationTimeHours,
                        initialMinutes = draft.notificationTimeMinutes,
                        onTimeSelected = viewModel::setNotificationTime,
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
                        checked = draft.fuelNotificationsEnabled,
                        onCheckedChange = viewModel::setFuelNotificationsEnabled,
                    )

                    ReminderToggleRow(
                        icon = Icons.Default.Notifications,
                        label = stringResource(R.string.reminder_tire_pressure_enabled),
                        checked = draft.tirePressureNotificationsEnabled,
                        onCheckedChange = viewModel::setTirePressureNotificationsEnabled,
                    )

                    ReminderToggleRow(
                        icon = Icons.Default.Vibration,
                        label = stringResource(R.string.reminder_vibrate_enabled),
                        checked = draft.vibrateOnNotification,
                        onCheckedChange = viewModel::setVibrateOnNotification,
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
                        text = "Vehicle Manager v${BuildConfig.VERSION_NAME}",
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

            // Sticky Bottom Save Bar
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    uiState.saveSuccessMessage?.let { msg ->
                        StatusRow(
                            text = msg,
                            color = MaterialTheme.colorScheme.tertiary,
                            icon = Icons.Default.CheckCircle,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    uiState.saveErrorMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Button(
                        onClick = { viewModel.saveSettings() },
                        enabled = uiState.isDirty && uiState.isValid && !uiState.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Guardar cambios de ajustes" },
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(text = "Guardar cambios")
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationStatusCard(
    status: NotificationPermissionStatus,
    nextCheckTimeStr: String?,
    onRequestPermission: () -> Unit,
    onSendTestNotification: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val (statusText, statusColor, descriptionText) = when (status) {
                NotificationPermissionStatus.GRANTED -> Triple(
                    "Notificaciones activadas",
                    MaterialTheme.colorScheme.tertiary,
                    "Recibirás alertas de mantenimiento y repostaje en tu dispositivo.",
                )
                NotificationPermissionStatus.APP_NOTIFICATIONS_DISABLED -> Triple(
                    "Notificaciones desactivadas en el sistema",
                    MaterialTheme.colorScheme.error,
                    "Las notificaciones están deshabilitadas en los ajustes de Android para Vehicle Manager.",
                )
                NotificationPermissionStatus.PERMANENTLY_DENIED -> Triple(
                    "Permiso denegado",
                    MaterialTheme.colorScheme.error,
                    "Has denegado el permiso de notificaciones. Puedes activarlo desde los ajustes del sistema.",
                )
                else -> Triple(
                    "Notificaciones desactivadas",
                    MaterialTheme.colorScheme.error,
                    "Vehicle Manager necesita permiso para enviarte recordatorios de servicios y repostajes.",
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (status == NotificationPermissionStatus.GRANTED) Icons.Default.CheckCircle else Icons.Default.NotificationsOff,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = descriptionText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (nextCheckTimeStr != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Próxima comprobación: $nextCheckTimeStr",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (status != NotificationPermissionStatus.GRANTED) {
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = "Permitir", style = MaterialTheme.typography.labelMedium)
                    }
                }
                OutlinedButton(
                    onClick = onSendTestNotification,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "Probar notificación", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun ReminderNumericInputField(
    valueText: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = valueText,
        onValueChange = { input ->
            val digitsOnly = input.filter { it.isDigit() }.take(6)
            onValueChange(digitsOnly)
        },
        label = { Text(text = label) },
        suffix = { Text(text = suffix) },
        isError = errorMessage != null,
        supportingText = errorMessage?.let { msg ->
            { Text(text = msg, color = MaterialTheme.colorScheme.error) }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
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
    val context = LocalContext.current
    val timePickerDialog = remember(context, initialHours, initialMinutes) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute -> onTimeSelected(hourOfDay, minute) },
            initialHours,
            initialMinutes,
            true,
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { timePickerDialog.show() },
    ) {
        OutlinedTextField(
            value = String.format("%02d:%02d", initialHours, initialMinutes),
            onValueChange = {},
            label = { Text(text = stringResource(R.string.reminder_notification_time_label)) },
            readOnly = true,
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Seleccionar hora",
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
        )
    }
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
