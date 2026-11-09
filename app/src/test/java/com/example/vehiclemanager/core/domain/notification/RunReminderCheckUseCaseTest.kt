package com.example.vehiclemanager.core.domain.notification

import androidx.test.core.app.ApplicationProvider
import com.example.vehiclemanager.core.data.notification.NotificationManagerImpl
import com.example.vehiclemanager.core.data.notification.NotificationScheduleEntity
import com.example.vehiclemanager.core.domain.GetDueServicesUseCase
import com.example.vehiclemanager.core.domain.MaintenanceSchedule
import com.example.vehiclemanager.core.domain.MaintenanceScheduleRepository
import com.example.vehiclemanager.core.domain.NotificationPreferences
import com.example.vehiclemanager.core.domain.ReminderPreferences
import com.example.vehiclemanager.core.domain.ReminderPreferencesRepository
import com.example.vehiclemanager.core.domain.StoredStageState
import com.example.vehiclemanager.core.domain.VehicleRepository
import com.example.vehiclemanager.core.domain.FuelRecordRepository
import com.example.vehiclemanager.core.domain.Vehicle
import com.example.vehiclemanager.core.domain.FuelRecord
import com.example.vehiclemanager.core.domain.EstimateRemainingRangeUseCase
import com.example.vehiclemanager.core.domain.CalculateFuelConsumptionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RunReminderCheckUseCaseTest {

    private lateinit var fakeReminderRepo: FakeReminderPreferencesRepository
    private lateinit var fakeScheduleRepo: FakeMaintenanceScheduleRepository
    private lateinit var fakeVehicleRepo: FakeVehicleRepository
    private lateinit var fakeFuelRepo: FakeFuelRecordRepository
    private lateinit var estimateRemainingRange: EstimateRemainingRangeUseCase
    private lateinit var notificationManager: NotificationManagerImpl
    private lateinit var useCase: RunReminderCheckUseCase

    @Before
    fun setUp() {
        fakeReminderRepo = FakeReminderPreferencesRepository()
        fakeScheduleRepo = FakeMaintenanceScheduleRepository()
        fakeVehicleRepo = FakeVehicleRepository()
        fakeFuelRepo = FakeFuelRecordRepository()
        estimateRemainingRange = EstimateRemainingRangeUseCase(CalculateFuelConsumptionUseCase())
        notificationManager = NotificationManagerImpl(ApplicationProvider.getApplicationContext())

        val getDueServicesUseCase = GetDueServicesUseCase(fakeScheduleRepo, fakeVehicleRepo, fakeFuelRepo)

        useCase = RunReminderCheckUseCase(
            context = ApplicationProvider.getApplicationContext(),
            getDueServicesUseCase = getDueServicesUseCase,
            notificationManager = notificationManager,
            reminderPreferencesRepository = fakeReminderRepo,
            vehicleRepository = fakeVehicleRepo,
            fuelRecordRepository = fakeFuelRepo,
            estimateRemainingRange = estimateRemainingRange,
        )
    }

    @Test
    fun execute_createsAndSavesCheckLog() = runTest {
        val log = useCase.execute(ReminderCheckTrigger.SCHEDULED)

        assertNotNull(log)
        assertEquals(ReminderCheckTrigger.SCHEDULED, log.trigger)
        assertEquals(log, fakeReminderRepo.lastLog)
    }

    @Test
    fun checkAndRunCatchUpIfNeeded_whenNoPreviousLog_runsCatchUp() = runTest {
        val catchUpLog = useCase.checkAndRunCatchUpIfNeeded()

        assertNotNull(catchUpLog)
        assertEquals(ReminderCheckTrigger.CATCH_UP, catchUpLog?.trigger)
    }

    @Test
    fun checkAndRunCatchUpIfNeeded_whenRecentLog_doesNotRunCatchUp() = runTest {
        val recentLog = ReminderCheckLog(
            timestampMs = System.currentTimeMillis() - (2 * 60 * 60 * 1000L), // 2 hours ago
            trigger = ReminderCheckTrigger.SCHEDULED,
            evaluatedCount = 1,
            postedCount = 0,
        )
        fakeReminderRepo.saveLastCheckLog(recentLog)

        val result = useCase.checkAndRunCatchUpIfNeeded()

        assertNull(result)
    }

    @Test
    fun checkAndRunCatchUpIfNeeded_whenOldLog_runsCatchUp() = runTest {
        val oldLog = ReminderCheckLog(
            timestampMs = System.currentTimeMillis() - (28 * 60 * 60 * 1000L), // 28 hours ago
            trigger = ReminderCheckTrigger.SCHEDULED,
            evaluatedCount = 1,
            postedCount = 0,
        )
        fakeReminderRepo.saveLastCheckLog(oldLog)

        val catchUpLog = useCase.checkAndRunCatchUpIfNeeded()

        assertNotNull(catchUpLog)
        assertEquals(ReminderCheckTrigger.CATCH_UP, catchUpLog?.trigger)
    }

    private class FakeReminderPreferencesRepository : ReminderPreferencesRepository {
        private val _reminderPreferences = MutableStateFlow(ReminderPreferences())
        override val reminderPreferences: Flow<ReminderPreferences> = _reminderPreferences.asStateFlow()

        private val _lastCheckLog = MutableStateFlow<ReminderCheckLog?>(null)
        override val lastCheckLog: Flow<ReminderCheckLog?> = _lastCheckLog.asStateFlow()

        private val _lastNotifiedStages = MutableStateFlow<Map<Long, StoredStageState>>(emptyMap())
        override val lastNotifiedStages: Flow<Map<Long, StoredStageState>> = _lastNotifiedStages.asStateFlow()

        val lastLog: ReminderCheckLog?
            get() = _lastCheckLog.value

        override suspend fun saveLastCheckLog(log: ReminderCheckLog) {
            _lastCheckLog.value = log
        }

        override suspend fun saveLastNotifiedStage(state: StoredStageState) {
            _lastNotifiedStages.value = _lastNotifiedStages.value + (state.scheduleId to state)
        }

        override suspend fun resetLastNotifiedStage(scheduleId: Long) {
            _lastNotifiedStages.value = _lastNotifiedStages.value - scheduleId
        }

        override suspend fun setAdvanceDistanceKm(km: Long) {}
        override suspend fun setAdvanceDays(days: Int) {}
        override suspend fun setNotificationTime(hours: Int, minutes: Int) {}
        override suspend fun setFuelNotificationsEnabled(enabled: Boolean) {}
        override suspend fun setTirePressureNotificationsEnabled(enabled: Boolean) {}
        override suspend fun setVibrateOnNotification(enabled: Boolean) {}
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun setNotificationPermissionAsked(asked: Boolean) {}
        override suspend fun updateAll(preferences: ReminderPreferences) {}
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
}
