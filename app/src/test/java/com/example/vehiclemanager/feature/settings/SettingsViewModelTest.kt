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
import com.example.vehiclemanager.core.domain.StoredStageState
import com.example.vehiclemanager.core.domain.VehicleRepository
import com.example.vehiclemanager.core.domain.FuelRecordRepository
import com.example.vehiclemanager.core.domain.Vehicle
import com.example.vehiclemanager.core.domain.FuelRecord
import com.example.vehiclemanager.core.domain.EstimateRemainingRangeUseCase
import com.example.vehiclemanager.core.domain.CalculateFuelConsumptionUseCase
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
        val fakeVehicleRepo = FakeVehicleRepository()
        val fakeFuelRepo = FakeFuelRecordRepository()
        val estimateRemainingRange = EstimateRemainingRangeUseCase(CalculateFuelConsumptionUseCase())
        val getDueServicesUseCase = GetDueServicesUseCase(fakeScheduleRepo, fakeVehicleRepo, fakeFuelRepo)
        val fakeNotificationRepo = FakeNotificationRepository()

        runReminderCheckUseCase = RunReminderCheckUseCase(
            context = ApplicationProvider.getApplicationContext(),
            getDueServicesUseCase = getDueServicesUseCase,
            notificationManager = notificationManager,
            reminderPreferencesRepository = fakeReminderRepo,
            vehicleRepository = fakeVehicleRepo,
            fuelRecordRepository = fakeFuelRepo,
            estimateRemainingRange = estimateRemainingRange,
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
    fun initialState_loadsPreferences() = runTest {
        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals(500L, state.draft.advanceDistanceKm)
        assertEquals(7, state.draft.advanceDays)
        assertTrue(state.draft.fuelNotificationsEnabled)
        assertFalse(state.draft.tirePressureNotificationsEnabled)
        assertTrue(state.draft.vibrateOnNotification)
    }

    @Test
    fun updateAdvanceDistanceKm_updatesDraft() {
        viewModel.setAdvanceDistanceKmText("1000")
        assertEquals("1000", viewModel.uiState.value.draft.advanceDistanceKmText)
        assertTrue(viewModel.uiState.value.isDirty)
    }

    @Test
    fun updateAdvanceDays_updatesDraft() {
        viewModel.setAdvanceDaysText("14")
        assertEquals("14", viewModel.uiState.value.draft.advanceDaysText)
        assertTrue(viewModel.uiState.value.isDirty)
    }

    @Test
    fun updateFuelNotificationsEnabled_updatesDraft() {
        viewModel.setFuelNotificationsEnabled(false)
        assertFalse(viewModel.uiState.value.draft.fuelNotificationsEnabled)
        assertTrue(viewModel.uiState.value.isDirty)
    }

    @Test
    fun saveChanges_persistsPreferencesAndResetsDirty() = runTest {
        viewModel.setAdvanceDaysText("10")
        viewModel.saveSettings()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(10, fakeReminderRepo.currentPrefs.advanceDays)
        assertFalse(viewModel.uiState.value.isDirty)
    }

    private class FakeThemePreferenceRepository : ThemePreferenceRepository {
        private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
        override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

        private val _isDarkTheme = MutableStateFlow(false)
        override val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

        override suspend fun setThemeMode(mode: ThemeMode) {
            _themeMode.value = mode
            _isDarkTheme.value = mode == ThemeMode.DARK
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

        private val _lastNotifiedStages = MutableStateFlow<Map<Long, StoredStageState>>(emptyMap())
        override val lastNotifiedStages: Flow<Map<Long, StoredStageState>> = _lastNotifiedStages.asStateFlow()

        val currentPrefs: ReminderPreferences
            get() = _reminderPreferences.value

        override suspend fun saveLastCheckLog(log: ReminderCheckLog) {
            _lastCheckLog.value = log
        }

        override suspend fun saveLastNotifiedStage(state: StoredStageState) {
            _lastNotifiedStages.value = _lastNotifiedStages.value + (state.scheduleId to state)
        }

        override suspend fun resetLastNotifiedStage(scheduleId: Long) {
            _lastNotifiedStages.value = _lastNotifiedStages.value - scheduleId
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
        override fun observeAllSchedules(): Flow<List<MaintenanceSchedule>> = flowOf(emptyList())
        override suspend fun getSchedule(id: Long): MaintenanceSchedule? = null
        override suspend fun insertSchedule(schedule: MaintenanceSchedule): Long = 1L
        override suspend fun updateSchedule(schedule: MaintenanceSchedule) {}
        override suspend fun deleteSchedule(schedule: MaintenanceSchedule) {}
    }

    private class FakeVehicleRepository : VehicleRepository {
        override val vehicles: Flow<List<Vehicle>> = flowOf(emptyList())
        override suspend fun getVehicle(id: Long): Vehicle? = null
        override suspend fun insertVehicle(vehicle: Vehicle): Long = 1L
        override suspend fun updateVehicle(vehicle: Vehicle) {}
        override suspend fun deleteVehicle(vehicle: Vehicle) {}
    }

    private class FakeFuelRecordRepository : FuelRecordRepository {
        override fun observeFuelRecords(vehicleId: Long): Flow<List<FuelRecord>> = flowOf(emptyList())
        override fun observeRecentPrices(): Flow<List<FuelRecord>> = flowOf(emptyList())
        override suspend fun getFuelRecord(id: Long): FuelRecord? = null
        override suspend fun insertFuelRecord(record: FuelRecord): Long = 1L
        override suspend fun updateFuelRecord(record: FuelRecord) {}
        override suspend fun deleteFuelRecord(record: FuelRecord) {}
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
}
