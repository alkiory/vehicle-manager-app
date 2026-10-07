package com.example.vehiclemanager.core.data.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.vehiclemanager.core.domain.ThemeMode
import com.example.vehiclemanager.core.domain.ThemePreferenceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.themeDataStore by preferencesDataStore(
    name = "theme_preferences",
)

@Singleton
class ThemePreferenceRepositoryImpl @Inject constructor(
    @ApplicationContext
    private val context: Context,
) : ThemePreferenceRepository {

    override val themeMode: StateFlow<ThemeMode> = context.themeDataStore.data
        .map { preferences ->
            val modeStr = preferences[THEME_MODE_KEY]
            if (modeStr != null) {
                runCatching { ThemeMode.valueOf(modeStr) }.getOrDefault(ThemeMode.SYSTEM)
            } else {
                val oldDark = preferences[DARK_THEME_KEY]
                if (oldDark != null) {
                    if (oldDark) ThemeMode.DARK else ThemeMode.LIGHT
                } else {
                    ThemeMode.SYSTEM
                }
            }
        }
        .stateIn(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ThemeMode.SYSTEM,
        )

    override val isDarkTheme: StateFlow<Boolean> = themeMode
        .map { mode -> mode == ThemeMode.DARK }
        .stateIn(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = false,
        )

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }

    override suspend fun setDarkTheme(enabled: Boolean) {
        setThemeMode(if (enabled) ThemeMode.DARK else ThemeMode.LIGHT)
    }

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
    }
}
