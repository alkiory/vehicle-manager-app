package com.example.vehiclemanager.feature.settings

import com.example.vehiclemanager.core.domain.ReminderPreferences
import com.example.vehiclemanager.core.domain.ReminderPreferencesRepository
import com.example.vehiclemanager.core.domain.ThemeMode
import com.example.vehiclemanager.core.domain.ThemePreferenceRepository
import com.example.vehiclemanager.core.ui.navigation.FormDirtyStateHolder
import com.example.vehiclemanager.core.ui.navigation.NavigationGuard
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeThemeRepo: FakeThemePreferenceRepository
    private lateinit var fakeReminderRepo: FakeReminderPreferencesRepository
    private lateinit var navigationGuard: NavigationGuard
    private lateinit var formDirtyStateHolder: FormDirtyStateHolder
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeThemeRepo = FakeThemePreferenceRepository()
        fakeReminderRepo = FakeReminderPreferencesRepository()
        navigationGuard = NavigationGuard()
        formDirtyStateHolder = FormDirtyStateHolder()
        viewModel = SettingsViewModel(
            themePreferenceRepository = fakeThemeRepo,
            reminderPreferencesRepository = fakeReminderRepo,
            navigationGuard = navigationGuard,
            formDirtyStateHolder = formDirtyStateHolder,
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
    fun repositoryRoundTrip_savesAndReadsBackUpdatedPreferences() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setAdvanceDistanceKmText("1200")
        viewModel.setNotificationTime(10, 30)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveSettings()
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedPrefs = fakeReminderRepo.currentPrefs
        assertEquals(1200L, updatedPrefs.advanceDistanceKm)
        assertEquals(10, updatedPrefs.notificationTimeHours)
        assertEquals(30, updatedPrefs.notificationTimeMinutes)
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

    override suspend fun updateAll(preferences: ReminderPreferences) {
        _reminderPreferences.value = preferences
    }
}
