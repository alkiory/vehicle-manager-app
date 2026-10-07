# Testing

Unit tests run on JVM via `./gradlew testDebugUnitTest`. Instrumented Android tests run via `./gradlew connectedDebugAndroidTest`.

## Test Coverage Requirements
- **Use Cases:** Test boundary conditions (insufficient data, zero distance, negative values, single refuel).
- **ViewModels:** Verify StateFlow emissions using standard coroutines test dispatchers (`MainDispatcherRule`).
- **DAOs & Migrations:** Verify SQLite database table creation, foreign key cascades, and migration step accuracy.
