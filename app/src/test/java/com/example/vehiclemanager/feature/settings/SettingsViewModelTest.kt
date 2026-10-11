package com.example.vehiclemanager.feature.settings

import com.example.vehiclemanager.core.domain.ReminderPreferences
import com.example.vehiclemanager.core.domain.ReminderPreferencesRepository
import com.example.vehiclemanager.core.domain.ThemeMode
import com.example.vehiclemanager.core.domain.ThemePreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeThemeRepo: FakeThemePreferenceRepository
    private lateinit var fakeReminderRepo: FakeReminderPreferencesRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeThemeRepo = FakeThemePreferenceRepository()
        fakeReminderRepo = FakeReminderPreferencesRepository()
        viewModel = SettingsViewModel(fakeThemeRepo, fakeReminderRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setAdvanceDistanceKm_updatesRepositoryAndState() = runTest {
        viewModel.setAdvanceDistanceKm(120L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(120L, fakeReminderRepo.currentPrefs.advanceDistanceKm)
    }

    @Test
    fun setAdvanceDays_updatesRepositoryAndState() = runTest {
        viewModel.setAdvanceDays(14)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(14, fakeReminderRepo.currentPrefs.advanceDays)
    }

    @Test
    fun setThemeMode_updatesRepositoryAndState() = runTest {
        viewModel.setThemeMode(ThemeMode.DARK)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ThemeMode.DARK, fakeThemeRepo.themeMode.value)
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

    val currentPrefs: ReminderPreferences
        get() = _reminderPreferences.value

    override suspend fun setAdvanceDistanceKm(km: Long) {
        _reminderPreferences.value = _reminderPreferences.value.copy(advanceDistanceKm = km)
    }

    override suspend fun setAdvanceDays(days: Int) {
        _reminderPreferences.value = _reminderPreferences.value.copy(advanceDays = days)
    }

    override suspend fun setNotificationTime(hours: Int, minutes: Int) {
        _reminderPreferences.value = _reminderPreferences.value.copy(
            notificationTimeHours = hours,
            notificationTimeMinutes = minutes
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

    override suspend fun updateAll(preferences: ReminderPreferences) {
        _reminderPreferences.value = preferences
    }
}
