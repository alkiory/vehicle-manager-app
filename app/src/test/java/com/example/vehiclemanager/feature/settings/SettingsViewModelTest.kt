package com.example.vehiclemanager.feature.settings

import androidx.test.core.app.ApplicationProvider
import com.example.vehiclemanager.core.data.notification.NotificationManagerImpl
import com.example.vehiclemanager.core.data.notification.NotificationRepository
import com.example.vehiclemanager.core.domain.GetDueServicesUseCase
import com.example.vehiclemanager.core.domain.MaintenanceSchedule
import com.example.vehiclemanager.core.domain.MaintenanceScheduleRepository
import com.example.vehiclemanager.core.domain.ReminderPreferences
import com.example.vehiclemanager.core.domain.ReminderPreferencesRepository
import com.example.vehiclemanager.core.domain.ThemeMode
import com.example.vehiclemanager.core.domain.ThemePreferenceRepository
import com.example.vehiclemanager.core.domain.notification.NotificationPermissionChecker
import com.example.vehiclemanager.core.domain.notification.NotificationPermissionStatus
import com.example.vehiclemanager.core.domain.notification.ReminderCheckLog
import com.example.vehiclemanager.core.domain.notification.RunReminderCheckUseCase
import com.example.vehiclemanager.core.ui.navigation.FormDirtyStateHolder
import com.example.vehiclemanager.core.ui.navigation.NavigationGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeThemeRepo: FakeThemePreferenceRepository
    private lateinit var fakeReminderRepo: FakeReminderPreferencesRepository
    private lateinit var fakePermissionChecker: FakeNotificationPermissionChecker
    private lateinit var notificationManager: NotificationManagerImpl
    private lateinit var navigationGuard: NavigationGuard
    private lateinit var formDirtyStateHolder: FormDirtyStateHolder
    private lateinit var runReminderCheckUseCase: RunReminderCheckUseCase
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeThemeRepo = FakeThemePreferenceRepository()
        fakeReminderRepo = FakeReminderPreferencesRepository()
        fakePermissionChecker = FakeNotificationPermissionChecker()
        notificationManager = NotificationManagerImpl(ApplicationProvider.getApplicationContext())
        navigationGuard = NavigationGuard()
        formDirtyStateHolder = FormDirtyStateHolder()

        val fakeScheduleRepo = FakeMaintenanceScheduleRepository()
        val getDueServicesUseCase = GetDueServicesUseCase(fakeScheduleRepo)
        val fakeNotificationRepo = FakeNotificationRepository()

        runReminderCheckUseCase = RunReminderCheckUseCase(
            context = ApplicationProvider.getApplicationContext(),
            getDueServicesUseCase = getDueServicesUseCase,
            notificationManager = notificationManager,
            notificationRepository = fakeNotificationRepo,
            reminderPreferencesRepository = fakeReminderRepo,
        )

        viewModel = SettingsViewModel(
            context = ApplicationProvider.getApplicationContext(),
            themePreferenceRepository = fakeThemeRepo,
            reminderPreferencesRepository = fakeReminderRepo,
            navigationGuard = navigationGuard,
            formDirtyStateHolder = formDirtyStateHolder,
            permissionChecker = fakePermissionChecker,
            notificationManager = notificationManager,
            runReminderCheckUseCase = runReminderCheckUseCase,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsFromRepository_isCleanAndValid() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isDirty)
        assertTrue(state.isValid)
        assertEquals("500", state.draft.advanceDistanceKmText)
        assertEquals("7", state.draft.advanceDaysText)
        assertEquals(ThemeMode.SYSTEM, state.draft.themeMode)
    }

    @Test
    fun editingDraft_marksDirty_andEnablesSaveWhenValid() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setAdvanceDistanceKmText("600")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isDirty)
        assertTrue(state.isValid)
        assertEquals("600", state.draft.advanceDistanceKmText)
    }

    @Test
    fun invalidInputs_showValidationErrors_andDisableSave() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setAdvanceDistanceKmText("0")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isDirty)
        assertFalse(state.isValid)
        assertNotNull(state.validationErrors.advanceDistanceError)

        viewModel.setAdvanceDaysText("")
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertFalse(updatedState.isValid)
        assertNotNull(updatedState.validationErrors.advanceDaysError)
    }

    @Test
    fun saveSettings_persistsToRepositories_resetsDirtyAndShowsFeedback() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setAdvanceDistanceKmText("800")
        viewModel.setAdvanceDaysText("14")
        viewModel.setThemeMode(ThemeMode.DARK)
        viewModel.setTirePressureNotificationsEnabled(true)
        testDispatcher.scheduler.advanceUntilIdle()

        var successCallbackCalled = false
        viewModel.saveSettings(onSuccess = { successCallbackCalled = true })
        testDispatcher.scheduler.advanceTimeBy(100)

        assertTrue(successCallbackCalled)
        assertEquals(800L, fakeReminderRepo.currentPrefs.advanceDistanceKm)
        assertEquals(14, fakeReminderRepo.currentPrefs.advanceDays)
        assertEquals(ThemeMode.DARK, fakeThemeRepo.themeMode.value)
        assertTrue(fakeReminderRepo.currentPrefs.tirePressureNotificationsEnabled)

        val state = viewModel.uiState.value
        assertFalse(state.isDirty)
        assertEquals("Cambios guardados", state.saveSuccessMessage)

        testDispatcher.scheduler.advanceTimeBy(3000)
        assertNull(viewModel.uiState.value.saveSuccessMessage)
    }

    @Test
    fun resetDraft_discardsEdits_restoresPersistedValues() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setAdvanceDistanceKmText("9999")
        viewModel.setThemeMode(ThemeMode.LIGHT)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDirty)

        viewModel.resetDraft()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isDirty)
        assertEquals("500", state.draft.advanceDistanceKmText)
        assertEquals(ThemeMode.SYSTEM, state.draft.themeMode)
    }

    @Test
    fun notificationPermission_granted_allowsTestNotification() = runTest {
        fakePermissionChecker.status = NotificationPermissionStatus.GRANTED
        fakePermissionChecker.isGranted = true
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendTestNotification()
        testDispatcher.scheduler.advanceTimeBy(100)

        assertEquals("Notificación de prueba enviada", viewModel.uiState.value.saveSuccessMessage)
    }

    @Test
    fun notificationPermission_notAsked_showsRationaleDialogOnTestNotification() = runTest {
        fakePermissionChecker.status = NotificationPermissionStatus.NOT_ASKED
        fakePermissionChecker.isGranted = false
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendTestNotification()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showRationaleDialog)
    }

    @Test
    fun runManualCheck_executesCheck_updatesLastLog_andShowsResultsDialog() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.runManualCheck()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showCheckResultsDialog)
        assertNotNull(state.lastCheckLog)

        viewModel.dismissCheckResultsDialog()
        assertFalse(viewModel.uiState.value.showCheckResultsDialog)
    }
}

private class FakeThemePreferenceRepository : ThemePreferenceRepository {
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    override val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        _isDarkTheme.value = (mode == ThemeMode.DARK)
    }

    override suspend fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        _themeMode.value = if (enabled) ThemeMode.DARK else ThemeMode.LIGHT
    }
}

private class FakeReminderPreferencesRepository : ReminderPreferencesRepository {
    private val _reminderPreferences = MutableStateFlow(ReminderPreferences())
    override val reminderPreferences: Flow<ReminderPreferences> = _reminderPreferences.asStateFlow()

    private val _lastCheckLog = MutableStateFlow<ReminderCheckLog?>(null)
    override val lastCheckLog: Flow<ReminderCheckLog?> = _lastCheckLog.asStateFlow()

    val currentPrefs: ReminderPreferences
        get() = _reminderPreferences.value

    override suspend fun saveLastCheckLog(log: ReminderCheckLog) {
        _lastCheckLog.value = log
    }

    override suspend fun setAdvanceDistanceKm(km: Long) {
        _reminderPreferences.value = _reminderPreferences.value.copy(advanceDistanceKm = km)
    }

    override suspend fun setAdvanceDays(days: Int) {
        _reminderPreferences.value = _reminderPreferences.value.copy(advanceDays = days)
    }

    override suspend fun setNotificationTime(hours: Int, minutes: Int) {
        _reminderPreferences.value = _reminderPreferences.value.copy(
            notificationTimeHours = hours,
            notificationTimeMinutes = minutes,
        )
    }

    override suspend fun setFuelNotificationsEnabled(enabled: Boolean) {
        _reminderPreferences.value = _reminderPreferences.value.copy(fuelNotificationsEnabled = enabled)
    }

    override suspend fun setTirePressureNotificationsEnabled(enabled: Boolean) {
        _reminderPreferences.value = _reminderPreferences.value.copy(tirePressureNotificationsEnabled = enabled)
    }

    override suspend fun setVibrateOnNotification(enabled: Boolean) {
        _reminderPreferences.value = _reminderPreferences.value.copy(vibrateOnNotification = enabled)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        _reminderPreferences.value = _reminderPreferences.value.copy(onboardingCompleted = completed)
    }

    override suspend fun setNotificationPermissionAsked(asked: Boolean) {
        _reminderPreferences.value = _reminderPreferences.value.copy(notificationPermissionAsked = asked)
    }

    override suspend fun updateAll(preferences: ReminderPreferences) {
        _reminderPreferences.value = preferences
    }
}

private class FakeNotificationPermissionChecker(
    var isGranted: Boolean = true,
    var notificationsEnabled: Boolean = true,
    var showRationale: Boolean = false,
    var askedBefore: Boolean = false,
    var status: NotificationPermissionStatus = NotificationPermissionStatus.GRANTED,
) : NotificationPermissionChecker {

    override fun isPermissionGranted(): Boolean = isGranted
    override fun areNotificationsEnabled(): Boolean = notificationsEnabled
    override fun shouldShowRationale(): Boolean = showRationale
    override fun hasAskedBefore(): Boolean = askedBefore

    override suspend fun setAskedBefore(asked: Boolean) {
        askedBefore = asked
    }

    override fun getPermissionStatus(apiLevel: Int): NotificationPermissionStatus = status
}

private class FakeMaintenanceScheduleRepository : MaintenanceScheduleRepository {
    override fun observeSchedules(vehicleId: Long): Flow<List<MaintenanceSchedule>> = flowOf(emptyList())
    override fun observeSchedulesForActiveVehicle(): Flow<List<MaintenanceSchedule>> = flowOf(emptyList())
    override suspend fun getSchedule(id: Long): MaintenanceSchedule? = null
    override suspend fun insertSchedule(schedule: MaintenanceSchedule): Long = 1L
    override suspend fun updateSchedule(schedule: MaintenanceSchedule) {}
    override suspend fun deleteSchedule(schedule: MaintenanceSchedule) {}
}

private class FakeNotificationRepository : NotificationRepository {
    private val notifications = mutableMapOf<Long, com.example.vehiclemanager.core.data.notification.NotificationScheduleEntity>()

    override fun observeAllNotifications(): Flow<List<com.example.vehiclemanager.core.data.notification.NotificationScheduleEntity>> {
        return flowOf(notifications.values.toList())
    }

    override fun observeNotificationsForVehicle(vehicleId: Long): Flow<List<com.example.vehiclemanager.core.data.notification.NotificationScheduleEntity>> {
        return flowOf(notifications.values.filter { it.vehicleId == vehicleId })
    }

    override suspend fun getNotification(id: Long): com.example.vehiclemanager.core.data.notification.NotificationScheduleEntity? {
        return notifications[id]
    }

    override suspend fun insertNotification(notification: com.example.vehiclemanager.core.data.notification.NotificationScheduleEntity): Long {
        notifications[notification.id] = notification
        return notification.id
    }

    override suspend fun updateNotification(notification: com.example.vehiclemanager.core.data.notification.NotificationScheduleEntity) {
        notifications[notification.id] = notification
    }

    override suspend fun deleteNotification(id: Long) {
        notifications.remove(id)
    }

    override suspend fun dismissNotification(id: Long) {
        val existing = notifications[id]
        if (existing != null) {
            notifications[id] = existing.copy(isDismissed = true)
        }
    }

    override suspend fun markAsTriggered(id: Long, triggeredDateMs: Long) {
        val existing = notifications[id]
        if (existing != null) {
            notifications[id] = existing.copy(isTriggered = true, triggeredDateMs = triggeredDateMs)
        }
    }

    override suspend fun getPreferences(): com.example.vehiclemanager.core.domain.NotificationPreferences? = null
    override suspend fun updatePreferences(preferences: com.example.vehiclemanager.core.domain.NotificationPreferences) {}
}
