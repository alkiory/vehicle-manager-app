package com.example.vehiclemanager.core.domain.notification

import androidx.test.core.app.ApplicationProvider
import com.example.vehiclemanager.core.data.notification.NotificationManagerImpl
import com.example.vehiclemanager.core.data.notification.NotificationRepository
import com.example.vehiclemanager.core.data.notification.NotificationScheduleEntity
import com.example.vehiclemanager.core.domain.GetDueServicesUseCase
import com.example.vehiclemanager.core.domain.MaintenanceSchedule
import com.example.vehiclemanager.core.domain.MaintenanceScheduleRepository
import com.example.vehiclemanager.core.domain.NotificationPreferences
import com.example.vehiclemanager.core.domain.ReminderPreferences
import com.example.vehiclemanager.core.domain.ReminderPreferencesRepository
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
    private lateinit var fakeNotificationRepo: FakeNotificationRepository
    private lateinit var notificationManager: NotificationManagerImpl
    private lateinit var useCase: RunReminderCheckUseCase

    @Before
    fun setUp() {
        fakeReminderRepo = FakeReminderPreferencesRepository()
        fakeScheduleRepo = FakeMaintenanceScheduleRepository()
        fakeNotificationRepo = FakeNotificationRepository()
        notificationManager = NotificationManagerImpl(ApplicationProvider.getApplicationContext())

        val getDueServicesUseCase = GetDueServicesUseCase(fakeScheduleRepo)

        useCase = RunReminderCheckUseCase(
            context = ApplicationProvider.getApplicationContext(),
            getDueServicesUseCase = getDueServicesUseCase,
            notificationManager = notificationManager,
            notificationRepository = fakeNotificationRepo,
            reminderPreferencesRepository = fakeReminderRepo,
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

        val lastLog: ReminderCheckLog?
            get() = _lastCheckLog.value

        override suspend fun saveLastCheckLog(log: ReminderCheckLog) {
            _lastCheckLog.value = log
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
        override suspend fun getSchedule(id: Long): MaintenanceSchedule? = null
        override suspend fun insertSchedule(schedule: MaintenanceSchedule): Long = 1L
        override suspend fun updateSchedule(schedule: MaintenanceSchedule) {}
        override suspend fun deleteSchedule(schedule: MaintenanceSchedule) {}
    }

    private class FakeNotificationRepository : NotificationRepository {
        private val notifications = mutableMapOf<Long, NotificationScheduleEntity>()

        override fun observeAllNotifications(): Flow<List<NotificationScheduleEntity>> = flowOf(notifications.values.toList())
        override fun observeNotificationsForVehicle(vehicleId: Long): Flow<List<NotificationScheduleEntity>> = flowOf(emptyList())
        override suspend fun getNotification(id: Long): NotificationScheduleEntity? = notifications[id]
        override suspend fun insertNotification(notification: NotificationScheduleEntity): Long {
            notifications[notification.id] = notification
            return notification.id
        }
        override suspend fun updateNotification(notification: NotificationScheduleEntity) {
            notifications[notification.id] = notification
        }
        override suspend fun dismissNotification(id: Long) {}
        override suspend fun markAsTriggered(id: Long, triggeredDateMs: Long) {}
        override suspend fun deleteNotification(id: Long) {}
        override suspend fun getPreferences(): NotificationPreferences? = null
        override suspend fun updatePreferences(preferences: NotificationPreferences) {}
    }
}
