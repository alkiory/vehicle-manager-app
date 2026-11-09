package com.example.vehiclemanager.core.data.maintenance

import com.example.vehiclemanager.core.domain.MaintenanceSchedule
import com.example.vehiclemanager.core.domain.ActiveVehicleRepository
import com.example.vehiclemanager.core.domain.Vehicle
import com.example.vehiclemanager.core.domain.ReminderPreferencesRepository
import com.example.vehiclemanager.core.domain.ReminderPreferences
import com.example.vehiclemanager.core.domain.StoredStageState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MaintenanceScheduleRepositoryTest {
    @Test
    fun entityMappingPreservesNullableIntervals() {
        val schedule = MaintenanceSchedule(
            id = 4,
            vehicleId = 8,
            serviceTitle = "Oil change",
            intervalKm = 10_000,
            intervalMonths = null,
            lastPerformedKm = 42_000,
            lastPerformedDateMs = null,
        )

        assertEquals(schedule, schedule.toEntity().toDomain())
    }

    @Test
    fun repositoryMapsVehicleScopedScheduleFlow() = runBlocking {
        val schedule = MaintenanceSchedule(
            id = 2,
            vehicleId = 12,
            serviceTitle = "Inspection",
            intervalKm = null,
            intervalMonths = 12,
            lastPerformedKm = null,
            lastPerformedDateMs = 100,
        )
        val vehicleStateFlow = MutableStateFlow<Vehicle?>(null)
        val fakeReminderRepo = object : ReminderPreferencesRepository {
            override val reminderPreferences: Flow<ReminderPreferences> = flowOf(ReminderPreferences())
            override val lastCheckLog = flowOf(null)
            override val lastNotifiedStages = flowOf(emptyMap<Long, StoredStageState>())
            override suspend fun saveLastCheckLog(log: com.example.vehiclemanager.core.domain.notification.ReminderCheckLog) {}
            override suspend fun saveLastNotifiedStage(state: StoredStageState) {}
            override suspend fun resetLastNotifiedStage(scheduleId: Long) {}
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

        val repository = MaintenanceScheduleRepositoryImpl(
            object : MaintenanceScheduleDao {
                override fun observeForVehicle(vehicleId: Long) = flowOf(listOf(schedule.toEntity()))
                override fun observeAll() = flowOf(listOf(schedule.toEntity()))
                override suspend fun findById(id: Long) = schedule.toEntity()
                override suspend fun findAll() = listOf(schedule.toEntity())
                override suspend fun deleteAll() = Unit
                override suspend fun insert(schedule: MaintenanceScheduleEntity) = schedule.id
                override suspend fun update(schedule: MaintenanceScheduleEntity) = Unit
                override suspend fun delete(schedule: MaintenanceScheduleEntity) = Unit
            },
            object : ActiveVehicleRepository {
                override val activeVehicle: kotlinx.coroutines.flow.StateFlow<Vehicle?> = vehicleStateFlow
                override suspend fun setActiveVehicle(vehicleId: Long) = Unit
                override suspend fun clearActiveVehicle() = Unit
            },
            fakeReminderRepo,
        )

        assertEquals(listOf(schedule), repository.observeSchedules(12).first())
    }
}
