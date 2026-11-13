# Testing

Unit tests run on JVM via `./gradlew testDebugUnitTest`. Instrumented Android tests run via `./gradlew connectedDebugAndroidTest`.

## Test Coverage Requirements
- **Use Cases:** Test boundary conditions (insufficient data, zero distance, negative values, single refuel).
- **ViewModels:** Verify StateFlow emissions using standard coroutines test dispatchers (`MainDispatcherRule`).
- **DAOs & Migrations:** Verify SQLite database table creation, foreign key cascades, and migration step accuracy.
- **Odometer chronology:** Test backdated records between earlier and later readings, self-exclusion while editing, upper/lower bound errors, and that historical entries never lower the vehicle odometer.
- **Decimal input:** Test both point and Spanish comma decimals for prices, litres, totals, and maintenance costs, including HALF_UP rounding.

## Latest verification (2026-11-13)

Environment: Linux, JDK 21.0.13.

| Command | Result |
| --- | --- |
| `./gradlew :app:compileDebugKotlin` | **pass**, exit 0 |
| `./gradlew testDebugUnitTest` | **pass**, **161 tests**, 0 failures |

Bug found during verification: the first chronological-neighbours test compared `Int` literals with `Long?`, so JUnit used `assertEquals(Any?, Any?)`. The assertion was corrected to `Long` literals and the suite reran green.

## Backdated odometer and decimal comma coverage

- `OdometerTimelineTest` verifies the closest previous/next readings, editing self-exclusion, and missing bounds at either end of the history.
- `FuelFormTest` and `MaintenanceFormTest` verify the new `higher_than_next` validation and comma-decimal parsing.
- `AddFuelViewModelTest` and `AddMaintenanceViewModelTest` verify a backdated record between neighbours saves successfully, leaves `primaryOdometerKm` unchanged, and rejects a reading above the next chronological record.
