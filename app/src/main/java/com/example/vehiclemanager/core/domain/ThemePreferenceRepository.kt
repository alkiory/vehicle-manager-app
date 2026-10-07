package com.example.vehiclemanager.core.domain

import kotlinx.coroutines.flow.StateFlow

interface ThemePreferenceRepository {
    val themeMode: StateFlow<ThemeMode>
    val isDarkTheme: StateFlow<Boolean>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDarkTheme(enabled: Boolean)
}
