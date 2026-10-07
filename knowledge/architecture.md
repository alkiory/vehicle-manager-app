# Architecture

Vehicle Manager uses Clean Architecture + MVI/MVVM with Hilt, Room, and DataStore.

## Key Layers & Components
- **Presentation:** Jetpack Compose screens consuming StateFlow UI states. `VehicleManagerTheme` dynamically listens to system dark theme changes via `isSystemInDarkTheme()`.
- **Domain:** Pure Kotlin Use Cases (`CalculateFuelConsumptionUseCase`, `EstimateRemainingRangeUseCase`, `GetUnifiedTimelineUseCase`, `CalculateVehicleStatsUseCase`). Zero Android imports.
- **Data:** Room Database v5 (`VehicleDatabase`) with explicit migrations (`MIGRATION_1_2` through `MIGRATION_4_5`) and DataStore repositories (`ThemePreferenceRepositoryImpl`, `ReminderPreferencesRepositoryImpl`).
- **Background Tasks:** `BootCompletedReceiver` listens to `ACTION_BOOT_COMPLETED` and reschedules `ServiceReminderWorker` via WorkManager.
