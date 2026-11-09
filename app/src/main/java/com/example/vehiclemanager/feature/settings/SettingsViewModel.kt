package com.example.vehiclemanager.feature.settings

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.vehiclemanager.core.data.notification.NotificationManagerImpl
import com.example.vehiclemanager.core.data.notification.ReminderScheduler
import com.example.vehiclemanager.core.domain.ReminderPreferences
import com.example.vehiclemanager.core.domain.ReminderPreferencesRepository
import com.example.vehiclemanager.core.domain.ThemeMode
import com.example.vehiclemanager.core.domain.ThemePreferenceRepository
import com.example.vehiclemanager.core.domain.notification.NotificationPermissionChecker
import com.example.vehiclemanager.core.domain.notification.NotificationPermissionStatus
import com.example.vehiclemanager.core.domain.notification.ReminderCheckLog
import com.example.vehiclemanager.core.domain.notification.ReminderCheckTrigger
import com.example.vehiclemanager.core.domain.notification.RunReminderCheckUseCase
import com.example.vehiclemanager.core.ui.navigation.FormDirtyStateHolder
import com.example.vehiclemanager.core.ui.navigation.FormGuardHandler
import com.example.vehiclemanager.core.ui.navigation.NavigationGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SettingsDraft(
    val advanceDistanceKmText: String = "500",
    val advanceDaysText: String = "7",
    val notificationTimeHours: Int = 9,
    val notificationTimeMinutes: Int = 0,
    val fuelNotificationsEnabled: Boolean = true,
    val tirePressureNotificationsEnabled: Boolean = false,
    val vibrateOnNotification: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    val advanceDistanceKm: Long?
        get() = advanceDistanceKmText.trim().toLongOrNull()

    val advanceDays: Int?
        get() = advanceDaysText.trim().toIntOrNull()
}

data class SettingsValidationErrors(
    val advanceDistanceError: String? = null,
    val advanceDaysError: String? = null,
    val notificationTimeError: String? = null,
) {
    val hasErrors: Boolean
        get() = advanceDistanceError != null || advanceDaysError != null || notificationTimeError != null
}

data class SettingsUiState(
    val persistedThemeMode: ThemeMode = ThemeMode.SYSTEM,
    val persistedReminderPreferences: ReminderPreferences = ReminderPreferences(),
    val draft: SettingsDraft = SettingsDraft(),
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    val validationErrors: SettingsValidationErrors = SettingsValidationErrors(),
    val saveSuccessMessage: String? = null,
    val saveErrorMessage: String? = null,
    val notificationPermissionStatus: NotificationPermissionStatus = NotificationPermissionStatus.GRANTED,
    val showRationaleDialog: Boolean = false,
    val triggerPermissionRequest: Boolean = false,
    val nextCheckTimeStr: String? = null,
    val isCheckScheduled: Boolean = false,
    val lastCheckLog: ReminderCheckLog? = null,
    val isRunningManualCheck: Boolean = false,
    val showCheckResultsDialog: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = true,
    val dismissBatteryHint: Boolean = false,
) {
    val isValid: Boolean
        get() = !validationErrors.hasErrors

    val themeMode: ThemeMode
        get() = draft.themeMode

    val isDarkTheme: Boolean
        get() = draft.themeMode == ThemeMode.DARK

    val reminderPreferences: ReminderPreferences
        get() = ReminderPreferences(
            advanceDistanceKm = draft.advanceDistanceKm ?: 500L,
            advanceDays = draft.advanceDays ?: 7,
            notificationTimeHours = draft.notificationTimeHours,
            notificationTimeMinutes = draft.notificationTimeMinutes,
            fuelNotificationsEnabled = draft.fuelNotificationsEnabled,
            tirePressureNotificationsEnabled = draft.tirePressureNotificationsEnabled,
            vibrateOnNotification = draft.vibrateOnNotification,
            onboardingCompleted = persistedReminderPreferences.onboardingCompleted,
        )
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val themePreferenceRepository: ThemePreferenceRepository,
    private val reminderPreferencesRepository: ReminderPreferencesRepository,
    private val navigationGuard: NavigationGuard,
    private val formDirtyStateHolder: FormDirtyStateHolder,
    private val permissionChecker: NotificationPermissionChecker,
    private val notificationManager: NotificationManagerImpl,
    private val runReminderCheckUseCase: RunReminderCheckUseCase,
) : ViewModel() {

    private val dirtyToken = "settings_form"

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var initialSyncDone = false
    private var messageDismissJob: Job? = null

    private val guardHandler = object : FormGuardHandler {
        override fun isDirty(): Boolean = _uiState.value.isDirty

        override fun onSave(onSuccess: () -> Unit, onFailure: () -> Unit) {
            saveSettings(onSuccess = onSuccess, onFailure = onFailure)
        }

        override fun onDiscard() {
            resetDraft()
        }

        override fun onCloseForm() {
            // Settings is a top-level tab
        }
    }

    init {
        navigationGuard.registerGuard(guardHandler)
        checkNotificationPermission()
        checkBatteryOptimizations()
        loadNextCheckTime()
        viewModelScope.launch {
            reminderPreferencesRepository.lastCheckLog.collect { log ->
                _uiState.update { it.copy(lastCheckLog = log) }
            }
        }
        viewModelScope.launch {
            combine(
                themePreferenceRepository.themeMode,
                reminderPreferencesRepository.reminderPreferences,
            ) { themeMode, reminderPrefs ->
                themeMode to reminderPrefs
            }.collect { (persistedThemeMode, persistedPrefs) ->
                _uiState.update { state ->
                    val status = permissionChecker.getPermissionStatus()
                    val shouldShowLaunchRationale = (status == NotificationPermissionStatus.NOT_ASKED || status == NotificationPermissionStatus.RATIONALE_NEEDED) && !persistedPrefs.notificationPermissionAsked

                    if (!state.isDirty || !initialSyncDone) {
                        initialSyncDone = true
                        val syncedDraft = SettingsDraft(
                            advanceDistanceKmText = persistedPrefs.advanceDistanceKm.toString(),
                            advanceDaysText = persistedPrefs.advanceDays.toString(),
                            notificationTimeHours = persistedPrefs.notificationTimeHours,
                            notificationTimeMinutes = persistedPrefs.notificationTimeMinutes,
                            fuelNotificationsEnabled = persistedPrefs.fuelNotificationsEnabled,
                            tirePressureNotificationsEnabled = persistedPrefs.tirePressureNotificationsEnabled,
                            vibrateOnNotification = persistedPrefs.vibrateOnNotification,
                            themeMode = persistedThemeMode,
                        )
                        state.copy(
                            persistedThemeMode = persistedThemeMode,
                            persistedReminderPreferences = persistedPrefs,
                            draft = syncedDraft,
                            isDirty = false,
                            validationErrors = validateDraft(syncedDraft),
                            notificationPermissionStatus = status,
                            showRationaleDialog = shouldShowLaunchRationale,
                        )
                    } else {
                        val recomputedDirty = calculateIsDirty(state.draft, persistedThemeMode, persistedPrefs)
                        state.copy(
                            persistedThemeMode = persistedThemeMode,
                            persistedReminderPreferences = persistedPrefs,
                            isDirty = recomputedDirty,
                            notificationPermissionStatus = status,
                        )
                    }
                }
            }
        }
    }

    fun checkNotificationPermission() {
        val status = permissionChecker.getPermissionStatus()
        _uiState.update { it.copy(notificationPermissionStatus = status) }
    }

    fun checkBatteryOptimizations() {
        runCatching {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val isIgnoring = pm.isIgnoringBatteryOptimizations(context.packageName)
            _uiState.update { it.copy(isIgnoringBatteryOptimizations = isIgnoring) }
        }
    }

    fun dismissBatteryHint() {
        _uiState.update { it.copy(dismissBatteryHint = true) }
    }

    fun loadNextCheckTime() {
        viewModelScope.launch {
            runCatching {
                val workInfo = withContext(Dispatchers.IO) {
                    runCatching {
                        WorkManager.getInstance(context)
                            .getWorkInfosForUniqueWork(ReminderScheduler.WORK_NAME)
                            .get()
                            ?.firstOrNull()
                    }.getOrNull()
                }
                val nextTime = workInfo?.nextScheduleTimeMillis ?: 0L
                val isValidState = workInfo?.state == WorkInfo.State.ENQUEUED ||
                        workInfo?.state == WorkInfo.State.RUNNING
                val isValidNextTime = isValidState &&
                        nextTime > 0L &&
                        nextTime < Long.MAX_VALUE - 8640000000L &&
                        nextTime < 4102444800000L

                if (isValidNextTime) {
                    val formatted = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(nextTime))
                    _uiState.update { it.copy(nextCheckTimeStr = formatted, isCheckScheduled = true) }
                } else {
                    _uiState.update { it.copy(nextCheckTimeStr = null, isCheckScheduled = false) }
                }
            }
        }
    }

    fun rearmScheduler() {
        viewModelScope.launch {
            runCatching {
                val prefs = _uiState.value.reminderPreferences
                ReminderScheduler.schedule(context, prefs.notificationTimeHours, prefs.notificationTimeMinutes, forceReschedule = true)
                loadNextCheckTime()
            }
        }
    }

    fun runManualCheck() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningManualCheck = true) }
            runCatching {
                val log = runReminderCheckUseCase.execute(ReminderCheckTrigger.MANUAL)
                _uiState.update {
                    it.copy(
                        isRunningManualCheck = false,
                        showCheckResultsDialog = true,
                        lastCheckLog = log,
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isRunningManualCheck = false,
                        saveErrorMessage = e.message ?: "Error al comprobar los recordatorios",
                    )
                }
            }
        }
    }

    fun dismissCheckResultsDialog() {
        _uiState.update { it.copy(showCheckResultsDialog = false) }
    }

    fun onAcceptRationale() {
        _uiState.update {
            it.copy(
                showRationaleDialog = false,
                triggerPermissionRequest = true,
            )
        }
        viewModelScope.launch {
            permissionChecker.setAskedBefore(true)
        }
    }

    fun onDismissRationale() {
        _uiState.update { it.copy(showRationaleDialog = false) }
        viewModelScope.launch {
            permissionChecker.setAskedBefore(true)
        }
    }

    fun consumeTriggerPermissionRequest() {
        _uiState.update { it.copy(triggerPermissionRequest = false) }
    }

    fun onPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            permissionChecker.setAskedBefore(true)
            checkNotificationPermission()
        }
    }

    fun sendTestNotification() {
        val status = permissionChecker.getPermissionStatus()
        if (status == NotificationPermissionStatus.GRANTED) {
            notificationManager.showTestNotification(_uiState.value.draft.vibrateOnNotification)
            _uiState.update { it.copy(saveSuccessMessage = "Notificación de prueba enviada") }
            autoDismissSaveSuccessMessage()
        } else if (status == NotificationPermissionStatus.NOT_ASKED || status == NotificationPermissionStatus.RATIONALE_NEEDED) {
            _uiState.update { it.copy(showRationaleDialog = true) }
        } else {
            _uiState.update { it.copy(saveErrorMessage = "Activa el permiso de notificaciones desde los ajustes del sistema") }
        }
    }

    fun setAdvanceDistanceKmText(text: String) {
        val filtered = text.filter { it.isDigit() }.take(6)
        updateDraft { copy(advanceDistanceKmText = filtered) }
    }

    fun setAdvanceDaysText(text: String) {
        val filtered = text.filter { it.isDigit() }.take(4)
        updateDraft { copy(advanceDaysText = filtered) }
    }

    fun setNotificationTime(hours: Int, minutes: Int) {
        updateDraft { copy(notificationTimeHours = hours, notificationTimeMinutes = minutes) }
    }

    fun setFuelNotificationsEnabled(enabled: Boolean) {
        updateDraft { copy(fuelNotificationsEnabled = enabled) }
        if (enabled && _uiState.value.notificationPermissionStatus != NotificationPermissionStatus.GRANTED) {
            requestPermissionContextual()
        }
    }

    fun setTirePressureNotificationsEnabled(enabled: Boolean) {
        updateDraft { copy(tirePressureNotificationsEnabled = enabled) }
        if (enabled && _uiState.value.notificationPermissionStatus != NotificationPermissionStatus.GRANTED) {
            requestPermissionContextual()
        }
    }

    fun setVibrateOnNotification(enabled: Boolean) {
        updateDraft { copy(vibrateOnNotification = enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        updateDraft { copy(themeMode = mode) }
    }

    fun saveSettings(onSuccess: (() -> Unit)? = null, onFailure: (() -> Unit)? = null) {
        val currentState = _uiState.value
        val errors = validateDraft(currentState.draft)
        if (errors.hasErrors) {
            _uiState.update { it.copy(validationErrors = errors) }
            onFailure?.invoke()
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveErrorMessage = null) }
            runCatching {
                val draft = _uiState.value.draft
                val newPrefs = ReminderPreferences(
                    advanceDistanceKm = draft.advanceDistanceKm ?: 500L,
                    advanceDays = draft.advanceDays ?: 7,
                    notificationTimeHours = draft.notificationTimeHours,
                    notificationTimeMinutes = draft.notificationTimeMinutes,
                    fuelNotificationsEnabled = draft.fuelNotificationsEnabled,
                    tirePressureNotificationsEnabled = draft.tirePressureNotificationsEnabled,
                    vibrateOnNotification = draft.vibrateOnNotification,
                    onboardingCompleted = currentState.persistedReminderPreferences.onboardingCompleted,
                    notificationPermissionAsked = currentState.persistedReminderPreferences.notificationPermissionAsked,
                )
                reminderPreferencesRepository.updateAll(newPrefs)
                themePreferenceRepository.setThemeMode(draft.themeMode)

                // Reschedule reminders with new time (safely caught for tests)
                runCatching {
                    ReminderScheduler.schedule(context, draft.notificationTimeHours, draft.notificationTimeMinutes, forceReschedule = true)
                }
                loadNextCheckTime()
            }.onSuccess {
                formDirtyStateHolder.setDirty(dirtyToken, false)
                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        isDirty = false,
                        saveSuccessMessage = "Cambios guardados",
                        saveErrorMessage = null,
                    )
                }
                onSuccess?.invoke()
                autoDismissSaveSuccessMessage()
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        saveErrorMessage = throwable.message ?: "Error al guardar los cambios",
                    )
                }
                onFailure?.invoke()
            }
        }
    }

    fun resetDraft() {
        _uiState.update { state ->
            val persistedPrefs = state.persistedReminderPreferences
            val persistedThemeMode = state.persistedThemeMode
            val syncedDraft = SettingsDraft(
                advanceDistanceKmText = persistedPrefs.advanceDistanceKm.toString(),
                advanceDaysText = persistedPrefs.advanceDays.toString(),
                notificationTimeHours = persistedPrefs.notificationTimeHours,
                notificationTimeMinutes = persistedPrefs.notificationTimeMinutes,
                fuelNotificationsEnabled = persistedPrefs.fuelNotificationsEnabled,
                tirePressureNotificationsEnabled = persistedPrefs.tirePressureNotificationsEnabled,
                vibrateOnNotification = persistedPrefs.vibrateOnNotification,
                themeMode = persistedThemeMode,
            )
            formDirtyStateHolder.setDirty(dirtyToken, false)
            state.copy(
                draft = syncedDraft,
                isDirty = false,
                validationErrors = SettingsValidationErrors(),
            )
        }
    }

    fun consumeSaveSuccessMessage() {
        messageDismissJob?.cancel()
        _uiState.update { it.copy(saveSuccessMessage = null) }
    }

    override fun onCleared() {
        navigationGuard.unregisterGuard(guardHandler)
        formDirtyStateHolder.setDirty(dirtyToken, false)
        super.onCleared()
    }

    private fun requestPermissionContextual() {
        val status = permissionChecker.getPermissionStatus()
        if (status == NotificationPermissionStatus.NOT_ASKED || status == NotificationPermissionStatus.RATIONALE_NEEDED) {
            _uiState.update { it.copy(showRationaleDialog = true) }
        }
    }

    private fun updateDraft(transform: SettingsDraft.() -> SettingsDraft) {
        _uiState.update { state ->
            val newDraft = state.draft.transform()
            val newIsDirty = calculateIsDirty(newDraft, state.persistedThemeMode, state.persistedReminderPreferences)
            val newErrors = validateDraft(newDraft)
            formDirtyStateHolder.setDirty(dirtyToken, newIsDirty)
            state.copy(
                draft = newDraft,
                isDirty = newIsDirty,
                validationErrors = newErrors,
            )
        }
    }

    private fun calculateIsDirty(
        draft: SettingsDraft,
        persistedThemeMode: ThemeMode,
        persistedPrefs: ReminderPreferences,
    ): Boolean {
        if (draft.themeMode != persistedThemeMode) return true
        if (draft.advanceDistanceKmText != persistedPrefs.advanceDistanceKm.toString()) return true
        if (draft.advanceDaysText != persistedPrefs.advanceDays.toString()) return true
        if (draft.notificationTimeHours != persistedPrefs.notificationTimeHours) return true
        if (draft.notificationTimeMinutes != persistedPrefs.notificationTimeMinutes) return true
        if (draft.fuelNotificationsEnabled != persistedPrefs.fuelNotificationsEnabled) return true
        if (draft.tirePressureNotificationsEnabled != persistedPrefs.tirePressureNotificationsEnabled) return true
        if (draft.vibrateOnNotification != persistedPrefs.vibrateOnNotification) return true
        return false
    }

    private fun validateDraft(draft: SettingsDraft): SettingsValidationErrors {
        val dist = draft.advanceDistanceKm
        val distError = when {
            draft.advanceDistanceKmText.isBlank() || dist == null || dist <= 0L -> "Introduce una distancia mayor que 0"
            else -> null
        }

        val days = draft.advanceDays
        val daysError = when {
            draft.advanceDaysText.isBlank() || days == null || days <= 0 -> "Introduce un número de días mayor que 0"
            else -> null
        }

        val timeError = when {
            draft.notificationTimeHours !in 0..23 || draft.notificationTimeMinutes !in 0..59 -> "Hora no válida"
            else -> null
        }

        return SettingsValidationErrors(
            advanceDistanceError = distError,
            advanceDaysError = daysError,
            notificationTimeError = timeError,
        )
    }

    private fun autoDismissSaveSuccessMessage() {
        messageDismissJob?.cancel()
        messageDismissJob = viewModelScope.launch {
            delay(3000)
            _uiState.update { it.copy(saveSuccessMessage = null) }
        }
    }
}
