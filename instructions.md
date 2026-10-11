# Vehicle Manager — Master Product & Architecture Blueprint

This master specification document establishes the technical blueprint, architectural conventions, agent protocol, and detailed task breakdowns for building the **Vehicle Manager** native Android application.

---

## 1. System Architecture & Technical Stack

### Core Technology Stack

* **Language:** Kotlin 2.0+ (with Compose Compiler Gradle Plugin)
* **Target / Compile SDK:** 35 | **Min SDK:** 26 (Android 8.0)
* **UI Framework:** Jetpack Compose with Material Design 3
* **Iconography & Animation:** FluentUI System Icons & AndroidX Splash Screen API
* **Dependency Injection:** Hilt (`com.google.dagger:hilt-android`)
* **Local Persistence:** Room Database with KSP (`androidx.room`) & Preferences DataStore
* **Asynchronous Streams:** Coroutines + Kotlin Flow
* **Navigation:** Jetpack Navigation Compose (Type-Safe Navigation with Kotlinx Serialization)
* **Build System:** Gradle with Version Catalog (`gradle/libs.versions.toml`)

### Architecture Rules

1. **Layer Separation:**
   * **Presentation Layer:** Jetpack Compose screens consuming state from `ViewModel` via `StateFlow<UiState>`. Zero business logic in Composables.
   * **Domain Layer:** Pure Kotlin modules containing Use Cases, Domain Models, and Value Objects. **Strictly no Android or Room imports**.
   * **Data Layer:** Room Entities, DAOs, Repositories, and Data Sources. Maps Room Entities to Domain Models via extension mappers.

2. **Data Integrity Standard:**
   * **Monetary Values:** Stored as `Long` minor units (e.g., cents, pence) to eliminate floating-point inaccuracies.
   * **Fuel Quantities:** Stored as integer minor units (liters $\times 100$, e.g., 45.25 liters = `4525`).
   * **Odometer:** Stored as `Int` or `Long` in kilometers.

3. **Offline-First & Local Persistence Guarantee:**
   * Local SQLite database via Room and Android DataStore act as the canonical, permanent source of truth on the user's phone storage.
   * Zero remote network or backend dependencies. Data must survive app kills, process death, and full device reboots.

---

## 2. Repository Memory & Directory Structure


```

/
├── README.md
├── knowledge/
│   ├── product.md
│   ├── architecture.md
│   ├── domain.md
│   ├── database.md
│   ├── calculations.md
│   ├── conventions.md
│   └── testing.md
├── agent/
│   ├── CURRENT.md
│   ├── CHANGELOG.md
│   ├── MEMORY.md
│   └── BACKLOG.md
└── epics/
├── EPIC-001-foundation/
├── EPIC-002-vehicles/
├── EPIC-003-fuel/
├── EPIC-004-maintenance/
├── EPIC-005-dashboard/
├── EPIC-006-statistics/
├── EPIC-007-polish-release/
├── EPIC-008-fluent-icons-visual-enhancements/
├── EPIC-009-advanced-statistics/
├── EPIC-010-notifications-reminders/
├── EPIC-011-enhanced-notification-settings/
├── EPIC-012-app-icon-splash-animation/
└── EPIC-013-offline-persistence-and-device-lifecycle/

```

### Agent State Machine & Lifecycle

Every task strictly follows the lifecycle state machine:

$$\text{BACKLOG} \longrightarrow \text{READY} \longrightarrow \text{IN\_PROGRESS} \longrightarrow \text{REVIEW} \longrightarrow \text{DONE}$$

*If blocked, state transitions to `BLOCKED` with mandatory `blocked_by` details recorded in `agent/CURRENT.md`.*

> **CRITICAL AGENT MEMORY MANDATE:**
> Upon starting and completing any task, the agent **MUST** explicitly update the memory files located inside the `agent/` folder (`agent/CURRENT.md`, `agent/CHANGELOG.md`, `agent/MEMORY.md`, and `agent/BACKLOG.md`). The repository files remain the single source of truth across development sessions.

---

## 3. Detailed Epics & Task Specification

---

### EPIC-001 — Foundation & Architecture
* **TASK-001 — Project Foundation & Dependency Configuration:** `🟢 DONE`
* **TASK-002 — Base Design System & Material 3 Theme:** `🟢 DONE`
* **TASK-003 — Room Base Infrastructure & Converters:** `🟢 DONE`
* **TASK-004 — Navigation Skeleton & Main Scaffold:** `🟢 DONE`

---

### EPIC-002 — Vehicle Management
* **TASK-001 — Vehicle Domain Model & Data Layer:** `🟢 DONE`
* **TASK-002 — Vehicle Entry & Editing Flow:** `🟢 DONE`
* **TASK-003 — Active Vehicle State Management:** `🟢 DONE`

---

### EPIC-003 — Fuel Tracking & Consumption Engine
* **TASK-001 — Fuel Domain Model & Database Infrastructure:** `🟢 DONE`
* **TASK-002 — Refueling Entry UI & Validation:** `🟢 DONE`
* **TASK-003 — Fuel History List & Detail View:** `🟢 DONE`
* **TASK-004 — Fuel Consumption Engine:** `🟢 DONE`

---

### EPIC-004 — Maintenance & Service Tracking
* **TASK-001 — Maintenance Domain & Data Infrastructure:** `🟢 DONE`
* **TASK-002 — Service Record Entry & History UI:** `🟢 DONE`
* **TASK-003 — Service Reminders & Schedule Engine:** `🟢 DONE`

---

### EPIC-005 — Dashboard & Quick Action Hub
* **TASK-001 — Dashboard Aggregation Use Cases:** `🟢 DONE`
* **TASK-002 — Dashboard UI & Quick Action Floating Hub:** `🟢 DONE`

---

### EPIC-006 — Statistics & Analytics Engine
* **TASK-001 — Analytics Calculations Engine:** `🟢 DONE`
* **TASK-002 — Chart Visualizations Screen:** `🟢 DONE`

---

### EPIC-007 — Polish, Data Export & Refactoring Iteration
* **TASK-001 — JSON Backup & Restore Engine:** `🟢 DONE`
* **TASK-002 — Accessibility, Localization & UI Polish:** `🟢 DONE`
* **TASK-003 — Release Build Verification & Audit:** `🟢 DONE`
* **TASK-005 — Form Auto-Calculations, Unsaved Changes Guard, Backup Migration & Metric Cards Fix:** `🟢 DONE`

---

### EPIC-008 — Visual Polish & Fluent Icon System Integration
* **TASK-001 — FluentUI Color Icons Dependency & Theme Integration:** `🟢 DONE`
* **TASK-002 — App Navigation & Scaffold Visual Upgrade:** `🟢 DONE`
* **TASK-003 — Dashboard Cards & Floating Quick Action Styling:** `🟢 DONE`

---

### EPIC-009 — Advanced Vehicle Statistics & Insights
* **TASK-001 — Trend Prediction Engine:** `🟢 DONE`
* **TASK-002 — Cost Analysis by Vehicle & Period:** `🟢 DONE`
* **TASK-003 — Fuel Price Comparison Over Time:** `🟢 DONE`

---

### EPIC-010 — Notifications & Reminders
* **TASK-001 — Service Reminder Notifications:** `🟢 DONE`
* **TASK-002 — Fuel Price Alert System:** `🟢 DONE`
* **TASK-003 — Maintenance Reminder Integration:** `🟢 DONE`

---

### EPIC-011 — Enhanced Notification Settings

**Objective:** Allow user configuration of notification advance thresholds (km/days), preferred alert execution time, and toggle switches for specific alerts.

#### TASK-001 — Reminder Preferences UI & Data Layer
* **Status:** `🟢 DONE`
* **Objective:** Create domain model, DataStore persistence layer, Hilt bindings, and Settings UI section for reminder settings.
* **Requirements:**
  1. Implemented `ReminderPreferences` domain model (`advanceDistanceKm`, `advanceDays`, `notificationTimeHours`, `notificationTimeMinutes`, `fuelNotificationsEnabled`, `tirePressureNotificationsEnabled`, `vibrateOnNotification`).
  2. Implemented `ReminderPreferencesDataStore` and `ReminderPreferencesRepositoryImpl` bound via `ReminderDataModule`.
  3. Integrated "Recordatorios" section in `SettingsScreen` with numerical inputs for km/days, time display, and feature toggles.
* **Acceptance Criteria:**
  * Settings persist across app restarts via DataStore.
* **Verification:** `./gradlew assembleDebug testDebugUnitTest`

---

### EPIC-012 — Custom Application Icon & Animated Startup Screen

**Objective:** Design and implement a branded adaptive launcher icon and a modern, animated splash screen using the official AndroidX SplashScreen API and Compose animations.

#### TASK-001 — Adaptive App Launcher Icon Design
* **Status:** `🟢 DONE`
* **Objective:** Create vector-based adaptive launcher icon resources (`mipmap-hdpi`, `mipmap-xhdpi`, etc.) combining vehicle and fuel/service iconography with the app's visual identity.
* **Requirements:**
  1. Generated SVG/XML vector drawables for launcher icon foreground (`ic_launcher_foreground.xml`) and background (`ic_launcher_background.xml`).
  2. Implemented adaptive icon definitions (`res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`) with proper safe-zone margins.
  3. Updated `AndroidManifest.xml` to reference the new adaptive icons.
* **Acceptance Criteria:**
  * Launcher icon scales correctly across all density buckets without clipping in round, squircle, or teardrop launcher masks.
* **Verification:** `./gradlew assembleDebug`

*Implementation Notes:*
- Created vector drawables for ic_launcher_foreground.xml (vehicle silhouette + fuel drop iconography on blue circle)
- Created ic_launcher_background.xml (dark navy background)
- Created adaptive icon definitions in mipmap-anydpi-v26/
- Created fallback vector icons for all density buckets (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi, nodpi)
- Added Theme.SplashScreen configuration in themes.xml
- Added splash_background color
- Updated AndroidManifest.xml with android:icon, android:roundIcon, and splash screen theme
- Icon design combines vehicle + fuel drop for transportation/fuel theme

#### TASK-002 — Animated Startup Splash Screen Integration
* **Status:** `🟢 DONE`
* **Objective:** Implement the official `androidx.core:core-splashscreen` API with an animated branding transition into the main scaffold.
* **Requirements:**
  1. Add `androidx.core:core-splashscreen` dependency to `libs.versions.toml` and `app/build.gradle.kts`.
  2. Create `Theme.App.Starting` splash theme in `res/values/themes.xml` inheriting from `Theme.SplashScreen`.
  3. Call `installSplashScreen()` in `MainActivity.onCreate()` before `setContent`.
  4. Implement an animated entrance transition in Compose (brand icon scale/fade animation) that holds until `ActiveVehicleRepository` and initial Room state are hydrated.
* **Acceptance Criteria:**
  * Cold start displays smooth splash animation with zero white screen flash.
  * Transitions seamlessly into `AppScaffold` once initial data loading is ready.
* **Verification:** `./gradlew assembleDebug testDebugUnitTest`

*Implementation Notes:*
- Added `SplashScreenAnimation` composable with scale and fade animations
- Icon scales from 0 to 1 with FastOutSlowInEasing over 800ms
- Icon fades in with 200ms delay over 600ms
- Splash screen automatically dismissed after animation completes
- `installSplashScreen()` called before `setContent()` in MainActivity
- Theme.App.Starting configured in themes.xml with proper icon and background
- Uses ic_launcher_foreground drawable for splash icon

---

### EPIC-013 — Offline Local Storage & Device Lifecycle Persistence Verification

**Objective:** Ensure complete data persistence, integrity, and seamless restoration across device cold boots, process kills, system reboots, and offline operations without any backend server.

#### TASK-001 — Room DB & DataStore Cold Boot Persistence Audit
* **Status:** `🟢 DONE`
* **Objective:** Verify and guarantee that all SQLite database tables (vehicles, fuel records, maintenance schedules, notifications) and DataStore files survive full process terminations and system reboots.
* **Requirements:**
  1. Audit `VehicleDatabase` configuration: ensure write-ahead logging (WAL) is enabled for crash resistance.
  2. Ensure `BOOT_COMPLETED` broadcast receiver (`RECEIVE_BOOT_COMPLETED`) correctly reschedules `WorkManager` notification jobs (`ServiceReminderWorker`) when the phone is turned back on.
  3. Write Android instrumented tests (`androidTest`) simulating app process kill and verifying full state recovery.
* **Acceptance Criteria:**
  * App retains 100% of vehicles, refuel logs, maintenance records, and settings after process kill and device reboot simulations.
  * WorkManager automatically reschedules background reminder tasks upon boot.
* **Verification:** `./gradlew connectedDebugAndroidTest`

#### TASK-002 — Database Schema Auto-Migration & Schema Safety
* **Status:** `🟢 DONE`
* **Objective:** Configure Room schema export and automated migration strategy to prevent data loss during future app updates.
* **Requirements:**
  1. Verify `room.schemaLocation` generates schema JSON snapshots for all database versions.
  2. Implement explicit migration rules (`Migration(x, y)`) or fallback-to-destructive-migration guards to protect local user data during updates.
  3. Create an automated test (`MigrationTest`) verifying database migration integrity without corrupting existing records.
* **Acceptance Criteria:**
  * Database upgrades preserve existing local user records without SQLite schema mismatch crashes.
* **Verification:** `./gradlew testDebugUnitTest`

---

### EPIC-014 — Refactorización de Estadísticas, Timeline Drivvo y Autonomía

**Objetivo:** Corregir los cálculos de distancia recorrida y coste por km, unificar el feed histórico en la pantalla de Inicio al estilo Drivvo y agregar el estimado de km restantes para el próximo repostaje.

#### TASK-001 — Corrección del Cálculo de Distancia Recorrida y Coste por Km
* **Status:** `🟢 DONE`
* **Objective:** Refactorizar `CalculateVehicleStatsUseCase` para usar el odómetro inicial del vehículo como punto de referencia base cuando no existan suficientes repostajes.
* **Requirements:**
  1. Utilizar `Vehicle.initialOdometerKm` como baseline.
  2. $\text{Distancia Recorrida} = \text{Odómetro Máximo Actual} - \text{Odómetro Inicial}$.
  3. $\text{Coste por Km} = \frac{\text{Gasto Total (Combustible + Mantenimiento)}}{\text{Distancia Recorrida}}$. Si la distancia es $> 0$, retornar el valor exacto en centavos/km en lugar de `InsufficientData`.
* **Acceptance Criteria:**
  * Al ingresar un único repostaje que suba el odómetro (ej: de 188,107 km a 188,427 km), la distancia recorrida refleja inmediatamente `320 km` y el coste por km computa $\frac{50.00}{320} = 0.156\text{ €/km}$.
* **Verification:** `./gradlew testDebugUnitTest`

#### TASK-002 — Timeline Histórico Unificado en Inicio (Estilo Drivvo)
* **Status:** `🟢 DONE`
* **Objective:** Implementar un feed cronológico vertical en `DashboardScreen` que unifique eventos de repostaje, servicios y odómetro.
* **Requirements:**
  1. Crear `GetUnifiedTimelineUseCase` unificando `FuelRecord` y `MaintenanceRecord` ordenados por `timestampMs` descendente.
  2. Diseñar la línea de tiempo Compose (`TimelineItem`) con conector vertical e iconos temáticos:
     * Repostajes: Icono de gasolinera con importe, litros y precio/L.
     * Mantenimientos: Icono de servicio con taller y costo.

**Implementation Summary:**
- Created `GetUnifiedTimelineUseCase` in `core/domain` unifying fuel and maintenance records
- Timeline sorted by timestamp descending (most recent first)
- Includes `groupByMonth()` method for month-based grouping with spending totals
- Created sealed class `TimelineEvent` with `FuelRecordEvent` and `MaintenanceRecordEvent` subtypes
- Created `TimelineMonth` data class for month separators
- Updated `DashboardViewModel` to include timeline data
- Added `TimelineSection` composable to `DashboardScreen`
- Timeline items feature:
  - Vertical connector line for visual continuity
  - Thematic icons (fuel station for refuel, wrench for maintenance)
  - Event details (station name, liters, price/L for fuel; title, workshop for maintenance)
  - Date and time display
  - Cost displayed in primary color
- Month separators (e.g., "ENERO 2026") with total spent that month
- Empty state when no activities exist
  3. Agregar separadores de mes (ej. "AGOSTO 2026") con el total gastado en ese período.
* **Acceptance Criteria:**
  * La pantalla de Inicio muestra la línea de tiempo completa desplazable.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-003 — Estimador de Autonomía y Próximo Repostaje
* **Status:** `🟢 DONE`
* **Objective:** Calcular y mostrar en la tarjeta principal de Inicio los kilómetros estimados restantes antes del próximo repostaje.
* **Requirements:**
  1. Implementar `EstimateRemainingRangeUseCase`:
     $$\text{Autonomía} = \left(\frac{\text{Último nivel cargado (L)}}{\text{Consumo Promedio } (L/100\text{km})}\right) \times 100$$
  2. Si el consumo medio no está disponible, proyectar basado en el promedio histórico de km entre repostajes.
  3. Mostrar tarjeta en Inicio: *"Próximo repostaje estimado en X km"*.
* **Acceptance Criteria:**
  * La tarjeta de Inicio muestra el widget dinámico de autonomía restante.
* **Verification:** `./gradlew testDebugUnitTest`

---

### EPIC-015 — UI/UX Layout Fixes, Cost/Km Math Correction & Overdue Service Tracking

**Objetivo:** Corregir el cálculo de coste por kilómetro, convertir la barra de navegación inferior a estilo solo íconos, asegurar la integración del timeline unificado en el Dashboard, e implementar alertas de servicios vencidos.

#### TASK-001 — Corrección del Cálculo de Coste por Kilómetro
* **Status:** `🟢 DONE`
* **Objective:** Corregir el cálculo de coste por km para usar euros en lugar de centavos.
* **Requirements:**
  1. Fórmula correcta: $\text{Coste/Km} = \frac{\text{Total Cost (EUR)}}{\text{Total Distance (KM)}}$
  2. Ejemplo: 639.00 EUR / 1992 KM = **0.32 €/km** (no 32.07)
  3. La UI debe mostrar el sufijo de moneda apropiado (€/km).
* **Acceptance Criteria:**
  * El cálculo muestra valores reales en euros por kilómetro.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-002 — Barra de Navegación Inferior Solo Íconos
* **Status:** `🟢 DONE`
* **Objective:** Convertir la NavigationBar a layout solo íconos sin textos.
* **Requirements:**
  1. En `AppScaffold.kt`, usar `alwaysShowLabel = false` en `NavigationBarItem`.
  2. Eliminar labels o usar composables vacíos `label = { }`.
  3. Mantener `contentDescription` en todos los íconos para accesibilidad.
* **Acceptance Criteria:**
  * La barra de navegación inferior muestra solo íconos limpios y espaciados.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-003 — Integración del Timeline Unificado en Dashboard
* **Status:** `🟢 DONE`
* **Objective:** Asegurar que el TimelineSection se renderice explícitamente en DashboardScreen.
* **Requirements:**
  1. TimelineSection debajo de las tarjetas hero.
  2. Verificar que nuevos eventos aparecen instantáneamente en el feed.
  3. Tarjetas cronológicas con conector vertical.
* **Acceptance Criteria:**
  * La pantalla de Inicio muestra la línea de tiempo completa desplazable.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-004 — Alertas de Seguimiento de Servicios Vencidos
* **Status:** `🟢 DONE`
* **Objective:** Mostrar alertas de servicios vencidos y permitir registro rápido.
* **Requirements:**
  1. Si una fecha/odómetro de servicio programado ha pasado sin mantenimiento registrado, mostrar tarjeta de alerta "OVERDUE / PENDING REGISTRATION".
  2. Tarjeta de alerta en Dashboard con botón para registrar servicio completado.
  3. Notificación de seguimiento: "Did you complete your scheduled service? Tap to log it."
* **Acceptance Criteria:**
  * Servicios vencidos se muestran prominentemente en el Dashboard.
  * El usuario puede registrar un servicio completado directamente desde la alerta.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

---

### EPIC-016 — Corrección de Icono Adaptativo, Branding de Header y Tema Automático

**Objetivo:** Corregir el recorte del icono adaptativo detectado en dispositivos reales, reemplazar el título genérico "Resumen" por branding oficial (logo + nombre de app) y sincronizar el tema de la aplicación automáticamente con el sistema operativo.

#### TASK-001 — Corrección del Recorte del Icono Adaptativo en Launcher
* **Status:** `🟢 DONE`
* **Objective:** Corregir el recorte visual del icono detectado en dispositivo real (Pixel Launcher y similares), donde la parte inferior del diseño (parrilla/rueda con neón) queda cortada por la máscara circular del adaptive icon. Regresión sobre EPIC-012 TASK-001.
* **Requirements:**
  1. Auditar la configuración actual del adaptive icon: `mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml`, `ic_launcher_foreground.xml` y `ic_launcher_background.xml`.
  2. Reescalar y recentrar el `foreground` para que el diseño completo quepa dentro de la **safe zone de 66 × 66 dp** sobre el canvas de 108 × 108 dp.
  3. Añadir capa `monochrome` (`ic_launcher_monochrome.xml`) para *themed icons* de Android 13+ (API 33+), derivada de la paleta del logo.
  4. Regenerar todas las densidades fallback (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`, `nodpi`) mediante `File > New > Image Asset` en Android Studio.
  5. Validar el icono con al menos 4 máscaras: círculo, squircle, rounded square, teardrop.
  6. QA visual en 3 launchers reales: Pixel Launcher, Samsung One UI y Nova Launcher.
  7. Verificar coherencia con `Theme.App.Starting` (splash) definido en EPIC-012 TASK-002 — el splash debe usar el mismo asset corregido.
* **Acceptance Criteria:**
  * El diseño completo del icono (cúpula + parrilla + rueda con neón) es visible sin recorte en todas las máscaras y densidades.
  * Captura antes/después adjunta en el PR.
* **Verification:** `./gradlew assembleDebug`

#### TASK-002 — Header con Branding Oficial (Reemplazo del Título "Resumen")
* **Status:** `🟢 DONE`
* **Objective:** Sustituir el título genérico "Resumen" en la pantalla principal (`DashboardScreen`) por el logotipo reducido + nombre de la app, aprovechando la paleta cromática del logo.
* **Requirements:**
  1. Eliminar el texto "Resumen" del `TopAppBar` / header del Dashboard.
  2. Renderizar el logotipo (versión reducida) acompañado del nombre "Vehicle Manager" como elemento de branding.
  3. Derivar los design tokens del header (`primary`, `onPrimary`, `surface`, `surfaceVariant`) desde la paleta del logotipo.
  4. Verificar contraste AA (WCAG 2.1) en modo claro y oscuro.
  5. Revisar impacto en navegación por pestañas u otras pantallas donde "Resumen" aportaba contexto.
  6. Actualizar `knowledge/conventions.md` con los nuevos design tokens.
* **Acceptance Criteria:**
  * El header muestra el branding oficial sin texto genérico.
  * Contraste validado en ambos modos de tema.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-003 — Tema Automático Sincronizado con el Sistema Operativo
* **Status:** `🟢 DONE`
* **Objective:** Sincronizar el tema de la app (claro/oscuro) automáticamente con la configuración del sistema operativo, permitiendo override manual desde Ajustes.
* **Requirements:**
  1. Configurar `AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM` como default en `MainActivity` (o equivalente en `VehicleManagerApplication`).
  2. Aplicar `isSystemInDarkTheme()` en el `VehicleManagerTheme` de Compose.
  3. Añadir preferencia `themeMode` (`SYSTEM` / `LIGHT` / `DARK`) al `ReminderPreferencesDataStore` o crear `AppearancePreferencesDataStore` dedicado.
  4. Integrar sección "Apariencia" en `SettingsScreen` con selector: **Sistema (default) / Claro / Oscuro**.
  5. Garantizar recomposición reactiva al cambio de tema del sistema **sin reiniciar la app**.
  6. Auditar contraste AA (WCAG 2.1) en todas las pantallas principales (Dashboard, Fuel, Maintenance, Statistics, Settings) en ambos modos.
* **Acceptance Criteria:**
  * Al cambiar el tema del sistema con la app abierta, la UI se actualiza sin reinicio.
  * La preferencia persiste entre sesiones vía DataStore.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

---

### EPIC-017 — Permisos de Notificaciones y Onboarding de Recordatorios

**Objetivo:** Garantizar que el sistema de alertas/recordatorios (EPIC-010, EPIC-011) funcione end-to-end solicitando los permisos necesarios al usuario en el primer arranque y gestionando correctamente los estados de denegación.

#### TASK-001 — Flujo de Onboarding y Solicitud de Permiso `POST_NOTIFICATIONS`
* **Status:** `🟢 DONE`
* **Objective:** Implementar la solicitud del permiso `POST_NOTIFICATIONS` (Android 13+ / API 33+) con una pantalla de onboarding explicativa previa.
* **Requirements:**
  1. Crear pantalla de onboarding (Compose) que explique por qué se necesitan las notificaciones, con CTA "Activar recordatorios" y opción "Ahora no".
  2. Solicitar el permiso mediante `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`.
  3. Detectar versiones de Android < 13 (API < 33) donde el permiso está concedido por defecto al instalar, y saltar la solicitud si ya está concedido.
  4. Registrar el estado de onboarding en `ReminderPreferencesDataStore` (`onboardingCompleted: Boolean`).
* **Acceptance Criteria:**
  * En dispositivo limpio (fresh install), el onboarding aparece antes de configurar recordatorios.
  * El permiso se solicita correctamente en API 33+.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-002 — Gestión de Estados de Permiso y Pantalla en Ajustes
* **Status:** `🟢 DONE`
* **Objective:** Gestionar los casos de permiso denegado temporal y permanentemente, y mostrar el estado en la pantalla de Ajustes.
* **Requirements:**
  1. Caso **concedido:** programar recordatorios vía `ServiceReminderWorker` (WorkManager).
  2. Caso **denegado temporal:** mostrar banner persistente en `DashboardScreen` con acceso directo a Ajustes del sistema.
  3. Caso **denegado permanentemente** (`shouldShowRequestPermissionRationale == false` tras denegación): mostrar diálogo explicativo + deep link `Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)`.
  4. Crear sección en `SettingsScreen` > **Notificaciones** que muestre el estado del permiso con botones de acción.
  5. Documentar comportamiento específico por fabricante (Xiaomi/MIUI, Huawei/EMUI, Oppo/ColorOS) mediante guía mostrada en Ajustes.
* **Acceptance Criteria:**
  * El usuario puede diagnosticar y resolver la falta de permisos desde la propia app.
  * El banner solo aparece cuando el permiso está denegado.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-003 — Reagendado de Recordatorios tras Reinicio del Dispositivo
* **Status:** `🟢 DONE`
* **Objective:** Garantizar que los recordatorios configurados por el usuario se reprograman correctamente tras un reinicio del dispositivo. Relacionado con EPIC-013 TASK-001.
* **Requirements:**
  1. Verificar que el `BroadcastReceiver` de `BOOT_COMPLETED` está registrado con `RECEIVE_BOOT_COMPLETED`.
  2. Reencolar `ServiceReminderWorker` y las notificaciones de recordatorio al recibir el broadcast.
  3. Manejar correctamente zonas horarias y cambios de hora (DST) al reprogramar.
  4. Test manual documentado: configurar recordatorio a 1 día / 100 km / 09:00 → reiniciar dispositivo → verificar notificación a la hora esperada.
* **Acceptance Criteria:**
  * Los recordatorios sobreviven reinicios de dispositivo.
* **Verification:** `./gradlew connectedDebugAndroidTest`

---

### EPIC-018 — Predicción de Consumo y Estimador de Próximo Repostaje en Inicio

**Objetivo:** Verificar y mejorar el cálculo de consumo medio y la estimación de próximo repostaje mostrados en la pantalla principal tras cada repostaje. Complemento y validación de EPIC-014 TASK-003.

#### TASK-001 — Cálculo Robusto de Consumo Medio (L/100 km)
* **Status:** `🟢 DONE`
* **Objective:** Refactorizar el motor de cálculo de consumo para usar la media ponderada de los últimos N repostajes (default 3), evitando cifras erróneas con pocos datos.
* **Requirements:**
  1. Calcular consumo entre dos repostajes consecutivos: $\text{Consumo} = \frac{\text{Litros}}{\text{Odómetro}_2 - \text{Odómetro}_1} \times 100$.
  2. Calcular **consumo medio ponderado** sobre los últimos N repostajes (N configurable en Ajustes, default 3).
  3. Retornar estado `InsufficientData` cuando haya menos de 2 repostajes válidos.
  4. Rechazar o marcar como inválidos repostajes con odómetro no creciente.
  5. Añadir tests unitarios: 2 repostajes, N repostajes, división por cero, odómetro inconsistente, litros ≤ 0.
* **Acceptance Criteria:**
  * El consumo medio se recalcula automáticamente tras cada nuevo repostaje.
  * Los tests cubren todos los casos límite.
* **Verification:** `./gradlew testDebugUnitTest`

#### TASK-002 — Tarjeta de Estimación de Próximo Repostaje en Dashboard
* **Status:** `🟢 DONE`
* **Objective:** Mostrar de forma prominente en la pantalla principal el consumo medio, la autonomía restante estimada y los km/fecha del próximo repostaje.
* **Requirements:**
  1. Extender `EstimateRemainingRangeUseCase` (EPIC-014 TASK-003) para devolver una estructura completa:
    * `averageConsumptionLPer100Km: Double?`
    * `estimatedRemainingRangeKm: Int?`
    * `estimatedNextRefuelOdometerKm: Int?`
    * `estimatedNextRefuelDate: Long?`
  2. Fórmulas:
    * $\text{Autonomía} = \frac{\text{Capacidad Depósito (L)}}{\text{Consumo Medio}} \times 100$
    * $\text{Próximo Repostaje (km)} = \text{Odómetro Actual} + (\text{Autonomía} \times 0.9)$
    * $\text{Fecha Estimada} = \text{Hoy} + \frac{\text{Autonomía} \times 0.9}{\text{Km/día promedio}}$
  3. Mostrar tarjeta hero en `DashboardScreen` con los tres valores.
  4. Estado "Datos insuficientes" cuando no haya información suficiente (sin cifras erróneas).
  5. Localización de unidades (`L/100 km` ↔ `mpg`) según locale.
* **Acceptance Criteria:**
  * Tras registrar un repostaje con odómetro válido, la tarjeta se actualiza y muestra las tres estimaciones.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-003 — Tests de Integración del Motor Predictivo
* **Status:** `🟢 DONE`
* **Objective:** Cubrir con tests de integración el flujo completo: registrar repostaje → recalcular consumo → actualizar tarjeta en Dashboard.
* **Requirements:**
  1. Test de flujo end-to-end en `DashboardViewModel` con `FuelRepository` mockeado y `GetUnifiedTimelineUseCase` real.
  2. Test que verifique la emisión de `StateFlow<DashboardUiState>` con los nuevos campos de estimación.
  3. Test de cambio de unidades (locale ES ↔ US).
* **Acceptance Criteria:**
  * Cobertura ≥ 80% en el motor predictivo.
* **Verification:** `./gradlew testDebugUnitTest`

---

### EPIC-019 — Actualización de Changelog y Memoria del Proyecto

**Objetivo:** Mantener actualizados los documentos de trazabilidad del proyecto (`agent/CHANGELOG.md`, `agent/MEMORY.md`, `knowledge/*`) reflejando los cambios de las épicas EPIC-016 a EPIC-018.

#### TASK-001 — Actualización de `agent/CHANGELOG.md`
* **Status:** `🟢 DONE`
* **Objective:** Registrar las correcciones y mejoras de las épicas EPIC-016, EPIC-017 y EPIC-018 bajo la sección `[Unreleased]` o nueva versión.
* **Requirements:**
  1. Seguir el formato **Keep a Changelog** (secciones `Fixed`, `Added`, `Changed`, `Removed`).
  2. Agrupar entradas por épica con referencia cruzada (`EPIC-016`, `EPIC-017`, etc.).
  3. Incluir fecha y versión tentativa de release.
* **Acceptance Criteria:**
  * CHANGELOG refleja todos los cambios entregados.
* **Verification:** Revisión manual por par.

#### TASK-002 — Actualización de `knowledge/` (Conventions, Calculations, Architecture)
* **Status:** `🟢 DONE`
* **Objective:** Documentar las decisiones de diseño y fórmulas derivadas de las nuevas épicas.
* **Requirements:**
  1. `knowledge/conventions.md`: nuevos design tokens del header, reglas de theming dinámico, paleta derivada del logo.
  2. `knowledge/calculations.md`: fórmulas de consumo medio ponderado, autonomía y próximo repostaje.
  3. `knowledge/architecture.md`: flujo de solicitud y gestión de permisos de notificación, `AppearancePreferencesDataStore`, `BOOT_COMPLETED` reschedule.
  4. `knowledge/testing.md`: nuevos casos de test añadidos (permisos, theming, motor predictivo).
* **Acceptance Criteria:**
  * Documentación actualizada y coherente con el código entregado.
* **Verification:** Revisión manual por par.

#### TASK-003 — Actualización de `agent/MEMORY.md` y `agent/CURRENT.md`
* **Status:** `🟢 DONE`
* **Objective:** Reflejar el estado final de las nuevas épicas en la memoria persistente del agente.
* **Requirements:**
  1. Actualizar `agent/CURRENT.md` con el estado final de las tareas (`DONE` / `BLOCKED`).
  2. Actualizar `agent/MEMORY.md` con aprendizajes, decisiones arquitectónicas y posibles deudas técnicas pendientes (ej. marcar repostaje "parcial" en v2).
  3. Sincronizar `agent/BACKLOG.md` retirando las tareas completadas y añadiendo las diferidas a v2.
* **Acceptance Criteria:**
  * Los cuatro archivos de `agent/` quedan sincronizados con el estado del repositorio.
* **Verification:** Revisión manual por par.

---

## 📎 Notas de Integración

- **Relación con épicas existentes:**
  - EPIC-016 TASK-001 → regresión sobre EPIC-012 TASK-001.
  - EPIC-016 TASK-002 → extiende branding iniciado en EPIC-008.
  - EPIC-017 → completa EPIC-010 y EPIC-011 (notificaciones sin permisos).
  - EPIC-017 TASK-003 → refuerza EPIC-013 TASK-001 (persistencia y `BOOT_COMPLETED`).
  - EPIC-018 → valida y mejora EPIC-014 TASK-003 (estimador de autonomía).

- **Dependencias sugeridas de ejecución:**
  1. EPIC-016 TASK-001 (icono) — alto impacto visual, rápido.
  2. EPIC-016 TASK-002 (header) — alineado con el nuevo icono.
  3. EPIC-016 TASK-003 (theming) — base para validar el branding en ambos modos.
  4. EPIC-018 (motor predictivo) — núcleo funcional con tests.
  5. EPIC-017 (permisos) — crítico, mayor esfuerzo.
  6. EPIC-019 (documentación) — cierre de iteración.

- **Estado inicial sugerido:** todas las tareas arrancan en `BACKLOG` y transicionan por el lifecycle `BACKLOG → READY → IN_PROGRESS → REVIEW → DONE`, según el Agent State Machine definido en la sección 2 del blueprint.

### EPIC-020 — Midnight Warmth Redesign and UI Bug Fixes (v0.2.1)

**Objective:** Deliver the approved "Midnight Warmth" visual redesign (palette C with subtle gradients, dark and light modes) and fix the UI bugs reported in the QA report, releasing app version 0.2.1. The app is currently almost monochromatic (navy + periwinkle); every color must now carry a meaning.

**Color semantics:** primary (periwinkle/blue) = brand, active vehicle, selected navigation, spending bars. Coral = maintenance. Teal = fuel. Green = costs/savings. Pink-red = alerts and price increase.

**Global constraints:**
- Code, identifiers and comments in English. User-facing strings stay in Spanish (use string resources if the project already does).
- TASK-001 to TASK-006 are visual only: do not change domain, data, repository or navigation logic.
- TASK-007 and TASK-008 may touch UI state / ViewModel only as needed to fix the bug. TASK-009 touches build config and the About card.
- No hardcoded hex colors in screen composables. Only `MaterialTheme.colorScheme`, `LocalAppSemanticColors` and `LocalAppGradients`.
- Keep the Sistema / Claro / Oscuro theme selector working.
- Execute one task at a time. Do not start the next task without instruction.
- Root causes listed below are hypotheses based on the QA report. Inspect the code and confirm before fixing.

**Out of scope (log as separate tickets):**
- Per-vehicle accent color (not approved).
- Statistics shows cost per km as `18.22` while Dashboard shows `0.16 €/km` (looks like cents without unit).
- Prices shown as `¢/L` instead of `€/L`; missing currency symbols in Statistics.
- English strings in the Settings "Data management" card (Export backup, Import backup, description).

---

#### TASK-001 — Theme foundation: color schemes, semantic colors and gradients
* **Status:** `🟢 DONE`
* **Objective:** Create the single source of truth for colors and gradients so all later tasks only consume tokens.
* **Requirements:**
  1. Define `darkColorScheme` and `lightColorScheme`.
     Dark: background `#0A1120`, surface `#131B2D`, onSurface `#E6EAF5`, onSurfaceVariant `#8D99B3`, primary `#7AA2FF`, onPrimary `#0A1B4D`, secondary (coral) `#FF7A59`, tertiary (teal) `#2DD4BF`, error `#FF5C7A`, outline `#2B3652`.
     Light: background `#F3F6FD`, surface `#FFFFFF`, onSurface `#0F172A`, onSurfaceVariant `#5B6784`, primary `#2F5BEA`, onPrimary `#FFFFFF`, secondary (coral) `#E8590C`, tertiary (teal) `#0D9488`, error `#D6284A`, outline `#D5DCEE`.
     The two outline values are proposals: adjust them if borders look too strong or too faint.
  2. Create `AppSemanticColors` (fuel, maintenance, cost, alert) with a `CompositionLocal`.
     Dark: fuel `#2DD4BF`, maintenance `#FF7A59`, cost `#4ADE80`, alert `#FF5C7A`.
     Light: fuel `#0D9488`, maintenance `#E8590C`, cost `#16A34A`, alert `#D6284A`.
  3. Create `AppGradients` (Material 3 `ColorScheme` cannot hold gradients) with a `CompositionLocal`, one dark and one light instance, built with `Brush`:
     Dark:
    - vehicleCard: linear 135°, `#5B83F0` → `#2F49B8`
    - maintenanceCard: linear 135°, `#FF7A59` at 28% alpha → 6% alpha. Border `#FF7A59` at 45% alpha.
    - maintenanceChip: linear 135°, `#FF7A59` → `#D9573A`
    - fuelChip (also history side bar, vertical): `#2DD4BF` → `#0D9488`
    - screenBackground: vertical, `#0E1830` → `#0A1120`
    - barCurrent: vertical, `#5B83F0` → `#2F49B8`
    - barPrevious: solid `#5B83F0` at 35% alpha
    - priceTrendFill: vertical, `#FF5C7A` at 45% alpha → 0%
      Light:
    - vehicleCard: linear 135°, `#4C78F2` → `#2447C4`
    - maintenanceCard: linear 135°, `#FFE3D6` → `#FFF4EE`. Border `#F5B9A1`.
    - maintenanceChip: linear 135°, `#FF8A5C` → `#E8590C`
    - fuelChip: `#2DD4BF` → `#0D9488`
    - screenBackground: vertical, `#F8FAFF` → `#E9EFFC`
    - barCurrent: vertical, `#4C78F2` → `#2447C4`
    - barPrevious: solid `#4C78F2` at 35% alpha
    - priceTrendFill: vertical, `#D6284A` at 45% alpha → 0%
  4. Provide both locals inside the app theme composable, switching with the resolved dark/light mode (including "Sistema"). Expose simple accessors, e.g. `AppTheme.gradients` and `AppTheme.semanticColors`.
  5. Usage rules for later tasks: gradients only on the active vehicle card, maintenance card, screen background, icon chips, current-month bar, price-trend fill and history side bar. Lists, history cards, form fields and dialogs stay flat on `surface`. Text on the vehicle card is white; verify contrast at both ends of each gradient (4.5:1 normal text, 3:1 large text).
* **Acceptance Criteria:**
  * Project compiles; switching Sistema / Claro / Oscuro updates colors and gradients.
  * All hex values live only in theme files.
  * No visible regression on existing screens other than the new base colors.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-002 — Window insets and compact top/bottom bars (QA bugs: top bar and bottom bar waste space)
* **Status:** `🟢 DONE`
* **Objective:** Recover vertical space for content on every tab and fix the navigation bar layout.
* **QA evidence (screenshots):**
  - Top: the "Vehicle Manager" header (logo + title) sits in a tall band with a large empty area before the first card (blue vehicle card). It should only be as tall as the title text.
  - Bottom: on the Dashboard ("Historial Reciente" with the "+" FAB and the GAR item) the navigation bar (5 icons, Home selected) has an extra band of unused space around it. Reported as visible on all tabs.
  - Also observed in the app screenshots: the selected-tab indicator is clipped at the left screen edge on Dashboard and at the right edge on Settings, and the FAB overlaps the last history card.
* **Requirements:**
  1. Audit the shared Scaffold / TopAppBar / NavigationBar for duplicated inset handling. Likely causes (verify): `enableEdgeToEdge` combined with a Scaffold that already applies insets, status-bar or navigation-bar padding applied twice, fixed large bar heights, extra Spacers.
  2. Top bar: height wraps the title; status-bar inset applied once; content starts immediately below.
  3. Bottom bar: icon-only, compact content height (target about 56–64dp plus the system navigation inset, confirm visually), no extra band.
  4. Navigation items evenly distributed; the selected indicator is never clipped. Indicator color: primary at 20% alpha (dark) / 12% alpha (light); selected icon in primary.
  5. Every screen uses the Scaffold `innerPadding`; scrollable content gets bottom `contentPadding` = bar height + FAB clearance so the last item is never hidden.
  6. Apply through the shared scaffold so all six tabs benefit.
* **Acceptance Criteria:**
  * No empty band above the title or below the icons on any tab; visible content area is larger than before.
  * Indicator fully visible on all tabs; FAB never covers the last list item.
  * Verified with gesture navigation and 3-button navigation, on a small and a large screen.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-003 — Dashboard restyle
* **Status:** `🟢 DONE`
* **Objective:** Apply the approved design to the Dashboard.
* **Requirements:**
  1. Active vehicle card: `vehicleCard` gradient, white text, car icon at the right.
  2. "Próximo repostaje estimado" card: `maintenanceCard` gradient and border.
  3. Quick summary cards: fuel icon chip with `fuelChip`, maintenance icon chip with `maintenanceChip`, white icons, `surface` card background.
  4. Cost per km in the semantic cost color.
  5. Recent history: flat `surface` cards, vertical side bar with `fuelChip` gradient, amount in cost color.
  6. Screen background: `screenBackground`.
* **Acceptance Criteria:**
  * Matches the approved design in dark and light.
  * No hex colors in the screen file.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-004 — Statistics and Vehicles restyle
* **Status:** `🟢 DONE`
* **Objective:** Apply the design to Statistics and Vehicles.
* **Requirements:**
  1. Statistics: icon chips of each card use the gradient chips (summary, monthly spending, price trend).
  2. Monthly spending: current month uses `barCurrent`, previous months use `barPrevious`.
  3. Fuel price trend: line in alert color with `priceTrendFill` below it; keep the end points.
  4. Period filters (30 días, 6 meses, Este año, Todo): selected in primary, others outlined.
  5. Vehicles: active vehicle card with `vehicleCard` gradient; inactive vehicles flat `surface`; "Activo" chip and "Activar" button using theme colors.
* **Acceptance Criteria:**
  * Bars and line follow the rules above in dark and light.
  * No logic changes.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-005 — Fuel, Maintenance and form screens restyle
* **Status:** `🟢 DONE`
* **Objective:** Apply semantic colors to the remaining screens.
* **Requirements:**
  1. Fuel screen: teal accents on icons and side bars.
  2. Maintenance screen: coral accents on icons and side bars.
  3. Forms (new fuel entry, new service, new vehicle): flat `surface`, focus and primary button in primary.
  4. Screen background gradient on both list screens.
* **Acceptance Criteria:**
  * Consistent color meaning across screens, dark and light.
  * Forms stay flat (no gradients).
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-006 — Settings layout fixes and restyle (QA bug: text breaks the UI)
* **Status:** `🟢 DONE`
* **Objective:** Fix the broken rows in the Reminders block and restyle Settings.
* **QA evidence (screenshots):** In "Recordatorios" there are three toggle rows: "Notificación de repostaje" (bell icon, on), "Notificación de presión de neumáticos" (check icon, off) and "Vibrar al notificar" (vibrate icon, on). The tire-pressure label runs into its switch and pushes it past the card edge, so the three switches do not share the same right alignment. Also observed: the "km antes" / "días antes" suffixes sit at the top right of the fields instead of being vertically centered, and the two input fields have different widths.
* **Requirements:**
  1. Row layout: icon, text with `weight(1f)` that wraps to two lines if needed, switch fixed at the end with a horizontal gap. All switches aligned at the same right edge.
  2. Add a subtitle (e.g. "Notificaciones") above the toggle group inside "Recordatorios", because the three toggles are notification settings (QA suggestion).
  3. Use the bell icon for both notification toggles; keep the vibrate icon for "Vibrar al notificar".
  4. Switch OFF state: track with enough contrast against the card (currently the tire-pressure switch is almost invisible).
  5. Align the "km antes" / "días antes" suffixes with the field center and use the same field width.
  6. Appearance theme selector and cards restyled with the new tokens; cards flat on `surface`.
* **Acceptance Criteria:**
  * No text overlaps or pushes a control on small screens or with large system font sizes.
  * Switches aligned; OFF state clearly visible in dark and light.
  * Notifications subtitle visible.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-007 — Fix reversed digits in reminder numeric inputs (QA bug)
* **Status:** `🟢 DONE`
* **Objective:** Make the "Distancia de antelación" and "Días de antelación" inputs behave like normal numeric fields.
* **QA evidence:** Typing 1, then 2, then 0 shows `201`. After the first digit the caret jumps in front of it, so each new digit is inserted before the previous ones. Same behavior in both inputs.
* **Requirements:**
  1. Probable cause (verify): the TextField is bound directly to state that is reformatted or round-tripped (String ↔ Int, or a persisted Flow) on every keystroke, which recreates the value and resets the selection to position 0.
  2. Keep a local editing state (`TextFieldValue` or `rememberSaveable` String) separate from the persisted value; accept digits only and use a numeric keyboard.
  3. Persist on debounce, IME Done, or focus loss, not through a per-keystroke round trip. Sync from the persisted value only when it differs from what the user is editing and the field is not focused.
  4. Handle empty input without crashing; keep the current validation rules.
  5. Add a unit test for the parse/persist logic and a Compose UI test if the project already has UI tests.
* **Acceptance Criteria:**
  * Typing `120` shows `120`; the caret stays at the end; editing in the middle and deleting work.
  * The value persists after leaving and reopening Settings.
  * Same result in both inputs.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-008 — Auto-dismiss save/import feedback (QA bug)
* **Status:** `🟢 DONE`
* **Objective:** Success messages must disappear and must not leak across tabs.
* **QA evidence:** After saving a change or importing a configuration, the "changes saved" / "import completed successfully" notification stays on screen. It should disappear after 3 seconds. It also keeps showing when the user navigates between tabs.
* **Requirements:**
  1. Find how the message is held. Probable cause (verify): a nullable message in persistent UI state or in a ViewModel shared across tabs that is never cleared.
  2. Model feedback as a one-shot event (Channel / SharedFlow) or clear it explicitly once shown.
  3. Auto-dismiss after 3000 ms with a custom timer (the default Short Snackbar lasts longer). A new message replaces the previous one and restarts the timer.
  4. Clear any visible message when the screen leaves composition or the user switches tabs; it must not reappear on return or after rotation.
  5. Apply to save and import, and to export if it shows a message.
  6. Unit test with virtual time that the message is cleared after 3000 ms.
* **Acceptance Criteria:**
  * Message disappears after about 3 seconds.
  * Not visible after switching tabs and coming back; not re-emitted after rotation.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-009 — App version 0.2.1 (QA bug)
* **Status:** `🟢 DONE`
* **Objective:** The About card must show the real app version.
* **QA evidence:** After two releases, Settings → "Acerca de" still shows `Vehicle Manager v1.0`. The project is not at v1 yet; the correct version is `0.2.1` (the `.1` because this epic fixes the reported bugs).
* **Requirements:**
  1. In the app module Gradle file, set `versionName` to `0.2.1` and increment `versionCode` by one over the current value.
  2. Remove the hardcoded "v1.0" and read the version from `BuildConfig.VERSION_NAME` (enable `buildFeatures.buildConfig` if needed) or from `PackageManager`. Display `Vehicle Manager v<version>`.
  3. Add a 0.2.1 entry to `agent/CHANGELOG.md` listing the fixes in this epic.
* **Acceptance Criteria:**
  * About shows `Vehicle Manager v0.2.1`.
  * A future version bump only requires changing Gradle.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

### EPIC-021 — Form Dismissal and Unsaved Changes Guard (v0.2.2)

**Objective:** Fix the behavior of the "new fuel entry" and "new service" forms when the user leaves them. Today, if the user opens either form, switches to another tab and comes back, the form is still open. Expected behavior: leaving a form without touching anything closes it; leaving after modifying any input asks whether to save the changes.

**Behavior matrix (applies to both forms):**

| Exit trigger | Form untouched | Form modified |
|---|---|---|
| Tap on another bottom-nav tab | Close form, navigate | Show confirmation dialog, hold navigation until the user decides |
| System back / back gesture | Close form (back to the list) | Show confirmation dialog |
| In-form close / cancel control | Close form | Show confirmation dialog |
| Return to the tab later | List is shown (form closed) | List is shown (form closed, draft already resolved by the dialog) |
| Rotation / app sent to background and back | Form and draft preserved | Form and draft preserved (not an exit) |

**Confirmation dialog (user-facing strings in Spanish):**
- Title: "¿Guardar cambios?"
- Message: "Has modificado el formulario. ¿Quieres guardarlo antes de salir?"
- Buttons:
  - "Guardar": validate and save. If validation fails, stay in the form and show the field errors, and cancel the pending navigation. If it succeeds, close the form and continue the pending navigation.
  - "Descartar": drop the draft, close the form, continue the pending navigation.
  - "Seguir editando": dismiss the dialog, stay in the form, cancel the pending navigation.
- Tapping outside the dialog or pressing back while it is shown behaves like "Seguir editando".

**Definition of "modified":** any user-editable field differs from its initial value. Values prefilled by the app (default date and time, suggested odometer, default fuel type, etc.) count as the initial value. Typing something and then erasing it back to the initial value makes the form clean again.

**Global constraints:**
- Code, identifiers and comments in English. User-facing strings stay in Spanish (use string resources if the project already does).
- Do not change domain rules, Room entities, DAOs or repository behavior. Save logic is reused as is.
- Use the existing theme tokens (no hardcoded colors); the dialog uses Material 3 `AlertDialog` styled with the current theme.
- Build one reusable mechanism and apply it to both forms; do not duplicate logic per screen.
- Root causes below are hypotheses. Inspect the code and confirm before fixing.
- Execute one task at a time. Do not start the next task without instruction.

**Out of scope:**
- The "new vehicle" form (the mechanism must be reusable so it can be applied later).
- Draft persistence across app restarts or process death.

---

#### TASK-001 — Reusable unsaved-changes guard (dirty tracking, dialog, navigation interception)
* **Status:** `🟢 DONE`
* **Objective:** Provide a single, tested mechanism that tracks whether a form is modified, intercepts every exit path and drives the confirmation dialog.
* **Requirements:**
  1. Inspect how the forms are currently shown and where the "form is open" state lives. Probable cause of the bug (verify): the flag lives in a ViewModel, `rememberSaveable` or a navigation back-stack entry that is saved and restored when switching tabs (`saveState` / `restoreState` on the bottom navigation), so the form reappears on return.
  2. Create a form-state holder (plain class or small ViewModel helper) that keeps the initial values, the current values, an `isDirty` derived state and a `reset()` method. `isDirty` compares the current values against the initial ones, not "has the user typed".
  3. Create a shared guard composable or modifier (e.g. `UnsavedChangesGuard`) that:
    - registers a `BackHandler` that is enabled only while the form is open and dirty,
    - exposes a `requestExit(onAllowed: () -> Unit)` function: if not dirty it runs `onAllowed` immediately, if dirty it shows the dialog and keeps `onAllowed` as the pending action,
    - renders the confirmation dialog with the three actions described in the epic.
  4. Intercept bottom-navigation clicks: before navigating, the navigation bar asks the active form (if any) through a shared hook, for example a `NavigationGuard` held in a ViewModel or `CompositionLocal` that the form registers and unregisters. The navigation happens only after the guard allows it. Do not hardcode screen-specific checks inside the navigation bar.
  5. State of the dialog and of the pending action must survive rotation.
  6. Add unit tests for the form-state holder (dirty/clean transitions, typing then reverting, prefilled defaults) and for the guard logic (allowed when clean, held when dirty, Guardar / Descartar / Seguir editando outcomes).
* **Acceptance Criteria:**
  * The guard can be attached to any form with a few lines.
  * Unit tests cover the transitions in the behavior matrix.
  * No visual or behavioral change in the app yet (forms are wired in the next tasks).
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-002 — Apply to the new fuel entry form
* **Status:** `🟢 DONE`
* **Objective:** Fix the stale form and add the unsaved-changes prompt in the fuel screen.
* **Requirements:**
  1. Wire the form to the guard from TASK-001: all exit paths (bottom-nav tab change, system back / gesture, in-form close or cancel control) go through `requestExit`.
  2. Fix the stale form: when the user leaves the Fuel tab with a clean form, the form closes and the list is shown on return. Make sure the "form open" state is cleared on exit, and that the navigation `saveState` / `restoreState` configuration no longer restores an open form.
  3. "Guardar" in the dialog runs the same validation and save path as the form's own save button. On validation failure stay in the form and cancel the navigation.
  4. After saving or discarding, reopening the form shows empty fields with the app defaults (no leftover draft).
  5. Rotation and background/foreground keep the open form and its draft intact.
* **Acceptance Criteria:**
  * Open form, switch tab, come back: list is shown, no form.
  * Open form, type any value, switch tab: dialog appears; each of the three actions behaves as described in the matrix.
  * Open form, type and erase a value, switch tab: no dialog.
  * Back gesture follows the same rules.
  * Existing save behavior is unchanged.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-003 — Apply to the new service (maintenance) form
* **Status:** `🟢 DONE`
* **Objective:** Same behavior for the Maintenance screen, reusing the TASK-001 mechanism.
* **Requirements:**
  1. Wire the service form to the guard; all exit paths use `requestExit`.
  2. Fix the stale form on tab return, same as TASK-002.
  3. Confirm the dialog behaves identically (same component, same strings).
  4. Check the optional fields specific to this form (service type, reminder by km or by date, notes) so that default values count as initial values and do not mark the form as modified.
* **Acceptance Criteria:**
  * Same acceptance checks as TASK-002, on the Maintenance tab.
  * No duplicated guard logic between the two screens.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-004 — Regression check and version 0.2.2
* **Status:** `READY`
* **Objective:** Close the epic with a regression pass and the version bump.
* **Requirements:**
  1. Walk through the behavior matrix on both forms, with gesture navigation and 3-button navigation, and fix anything that deviates.
  2. Check that the dialog looks right in dark and light and does not overlap the bottom navigation or system bars.
  3. Check the rest of the tabs: switching between tabs without any form open must be unaffected (no dialog, no delay).
  4. In the app module Gradle file, set `versionName` to `0.2.2` and increment `versionCode` by one over the current value. The About card must keep reading the version from `BuildConfig`, not from a hardcoded string.
  5. Add a 0.2.2 entry to `agent/CHANGELOG.md` summarizing the fix.
* **Acceptance Criteria:**
  * All behavior-matrix cases pass on both forms.
  * About shows `Vehicle Manager v0.2.2`.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

### EPIC-022 — Persistent Settings, Reliable Notifications, Statistics Clarity and Changelog (v0.3.0)

**Objective:** Fix four trust problems found while testing the installed APK: (1) Settings edits are shown while navigating but are lost when the app is closed; (2) the app never asks for notification permission and there is no way to know whether reminders will actually fire; (3) Statistics shows numbers without units or explanation, and some values do not match their calculation; (4) there is no changelog for traceability (the public landing page links to it).

**Global constraints:**
- Code, identifiers and comments in English. User-facing strings stay in Spanish.
- No Room schema changes or migrations. If one turns out to be unavoidable, stop and report instead of doing it.
- Reuse the existing `UnsavedChangesGuard`, the 3-second feedback mechanism and the theme tokens (`MaterialTheme.colorScheme`, `LocalAppSemanticColors`, `LocalAppGradients`). No hardcoded colors.
- Keep the current decimal separator convention. Only fix units, labels and rounding.
- Root causes below are hypotheses. Inspect the code and confirm before fixing; record confirmed causes in `agent/MEMORY.md`.
- Execute one task at a time. Do not start the next task without instruction.

**Out of scope:** per-vehicle accent color, redesign of any screen not named here, exporting settings inside the backup.

---

#### TASK-001 — Persist settings with an explicit "Guardar cambios" button
* **Status:** `READY`
* **Objective:** Settings edits must survive closing and reopening the app, and must only be applied when the user saves.
* **Bug evidence:** In Settings the user edits an input (e.g. antelación distance), switches tab and the edited value is still visible, but after leaving the app and coming back the default values appear again. The edit lived only in in-memory state.
* **Requirements:**
  1. Find where settings are stored (DataStore, SharedPreferences, Room or none) and where the Settings screen state comes from. Probable causes (verify): the screen binds inputs to a ViewModel `MutableStateFlow` that is never written to storage; or storage is written but the defaults are re-applied on app start and overwrite the stored values; or the write happens on a scope that is cancelled.
  2. Settings fields in scope: antelación distance (km), antelación days, best time to notify, the three toggles (repostaje, presión de neumáticos, vibrar al notificar) and the app theme (Sistema / Claro / Oscuro). Export / Import backup are immediate actions and are not part of the draft.
  3. Introduce a draft model: the screen edits a draft copy; the persisted settings change only when the user taps "Guardar cambios". Defaults apply only when nothing is stored yet (first run); never overwrite stored values with defaults at startup.
  4. Add a "Guardar cambios" button at the bottom of the Settings content, always reachable above the bottom navigation (sticky, with correct insets and IME padding). It is enabled only when the draft differs from the persisted values and all fields are valid (numbers > 0 where required, valid time). Invalid fields show an inline error and keep the button disabled.
  5. On success: write to storage (suspend, awaited), show the existing 3-second success message ("Cambios guardados"), and the button goes back to disabled.
  6. Reuse `UnsavedChangesGuard`: leaving Settings (tab change, back) with a modified draft shows the same dialog (Guardar / Descartar / Seguir editando). A clean draft leaves silently.
  7. Rotation keeps the draft. Persisted values are loaded once at screen creation and re-synced only when the draft is clean.
  8. The theme selector is part of the draft like the rest, and applies when saved.
  9. Unit tests: repository round trip (write, read back, defaults only when empty), draft dirty/clean transitions, validation rules.
* **Acceptance Criteria:**
  * Edit values, save, kill the app, reopen: the saved values are shown.
  * Edit values without saving, switch tab: the guard dialog appears.
  * Save button disabled when nothing changed or when a field is invalid.
  * Fresh install shows the defaults (500 km is only a hint; keep the current default values).
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-002 — Notification permission flow and visible notification status
* **Status:** `BACKLOG`
* **Objective:** The app must request the notification permission, handle denial correctly and let the user check that notifications work.
* **Bug evidence:** After installing the APK the app never asks for notification permission.
* **Requirements:**
  1. Declare `POST_NOTIFICATIONS` in the manifest (required on Android 13 / API 33+). Create the notification channels at app start (required from API 26).
  2. First-launch request: on API 33+, if the permission is not granted and has not been asked before, show a short rationale dialog in Spanish ("Vehicle Manager te avisa de repostajes y servicios próximos. ¿Activar notificaciones?") with "Activar" (launches the system permission request) and "Ahora no". Persist that it was asked so it is not repeated automatically.
  3. Contextual request: when the user turns ON a notification toggle in Settings, or saves with a notification enabled, and the permission is missing, request it.
  4. Denied permanently (the system no longer shows the dialog): the "Permitir" action opens the app notification settings (`Settings.ACTION_APP_NOTIFICATION_SETTINGS`).
  5. Add a notification status card inside the "Recordatorios" block of Settings showing: permission state ("Activadas" / "Desactivadas"), a "Permitir" button when off, and an "Enviar notificación de prueba" button that posts a real notification through the same channel and style used for reminders (it triggers the permission request if missing). Also detect app-level notifications disabled (`areNotificationsEnabled`) and a channel set to importance NONE.
  6. Re-evaluate the state on `ON_RESUME`, since the user may change it in system settings.
  7. Tapping a notification opens the app (PendingIntent with `FLAG_IMMUTABLE`) on the relevant tab.
  8. The notification small icon must be a monochrome vector; text in Spanish.
  9. Unit tests for the permission-state logic (API levels, asked flag, permanent denial) behind an interface so it is testable.
* **Acceptance Criteria:**
  * Fresh install on Android 13+ shows the rationale and then the system permission dialog.
  * Test notification arrives with the permission granted; with it denied the card explains how to enable it.
  * No crash on API below 33.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-003 — Reminder scheduling audit and reliability
* **Status:** `BACKLOG`
* **Objective:** Guarantee, by design and by tests, that reminders fire when their conditions are met.
* **Requirements:**
  1. Audit and document in `agent/MEMORY.md` ("Reminder pipeline"): what schedules reminders today (WorkManager, AlarmManager or nothing), what triggers a date-based reminder, what triggers a km-based reminder, where lead time (days, km) and best notification time are read from, and the gaps found.
  2. Implement or fix a single `ReminderScheduler`: one daily check at the configured "Mejor hora para notificar" (default 09:00), reading the persisted settings from TASK-001. Prefer WorkManager (no special permission). Do not add `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` unless the current implementation already depends on them; timing is approximate and may drift a few minutes under Doze.
  3. Reschedule when settings are saved, after device reboot, after app update and when the time zone or system time changes.
  4. Put the due-evaluation in a pure function (inputs: reminders, current odometer, today, lead days, lead km; output: notifications to show) with unit tests: due by date, due by km, not yet due, already notified (no duplicate for the same reminder on the same day), toggles off.
  5. Respect the toggles: fuel notification, tire pressure notification. Vibrate toggle: Android 8+ cannot change a channel's vibration after creation, so use two channels (vibrating and silent) and choose by the toggle.
  6. If the permission is missing, skip posting without crashing and keep the status card (TASK-002) accurate.
  7. In the status card show "Próxima comprobación: <date and time>" read from the scheduled work (when the API exposes it), so the user can see the scheduler is armed. If the app is subject to battery optimization, show a dismissible hint with a button that opens the system battery settings. Do not request the exemption automatically.
  8. Add a short info text under the lead-time inputs explaining how reminders are evaluated (date-based on the due date minus lead days; km-based when the odometer gets within the lead distance), matching what the code really does.
* **Acceptance Criteria:**
  * Unit tests cover every case in requirement 4.
  * Saving settings changes the next scheduled check; the status card shows it.
  * After reboot the check is still scheduled.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-004 — Statistics calculation audit, units and shared formatting
* **Status:** `BACKLOG`
* **Objective:** Every number in Statistics (and the Dashboard values derived from the same data) must match a documented formula, be rounded correctly and carry its unit.
* **Findings from screenshots (verify each one):**
  * Statistics shows cost per km as `16.34` (with data: 236.63 total, 1448 km) while the real value is 0.1634 €/km. It is expressed in cents without a unit. Earlier data (152.00 total, 834 km) showed `18.22`, the same issue.
  * Dashboard shows `0.16 €/km` captioned "Basado en historial de combustible" while Statistics shows a total-cost-based value under the same name. With the earlier data a fuel-only value would be 140 / 834 = 0.168, which should display as 0.17, not 0.16. Two different metrics share one name, and one of them may be truncated instead of rounded.
  * "Gasto mensual medio" shows `118.31`, but 236.63 / 2 months = 118.315, which rounds (HALF_UP) to 118.32. This is a floating-point artifact. Money must not be summed and divided as raw `Double`.
  * Fuel price shows `¢/L` on the Dashboard history; it must be `€/L`.
  * No currency symbol anywhere in Statistics.
* **Requirements:**
  1. Document the formulas actually implemented, then align them to these definitions (confirm "distance" against the code and document it):
    - Period filter: 30 días = last 30 days, 6 meses = last 6 months, Este año = from Jan 1, Todo = all records.
    - Fuel total = sum of fuel entry costs in the period. Maintenance total = sum of service costs. Total cost = fuel + maintenance.
    - Distance = highest odometer value minus lowest odometer value among the records in the period of the active vehicle.
    - Cost per km = total cost / distance, in €/km, null when distance is 0.
    - Average monthly cost = total cost / number of calendar months from the first to the last month with records in the period (inclusive).
    - Monthly price (chart) = weighted average: total fuel cost / total liters of that month. Not the simple mean of entry prices.
  2. All statistics must use only the active vehicle's records. Verify and add a test with two vehicles.
  3. Money math with `BigDecimal` (`BigDecimal.valueOf` per Double value, then sum), rounding `RoundingMode.HALF_UP`, scale 2. Never truncate. Do not migrate the database.
  4. Make Dashboard and Statistics use the same cost-per-km use case. The Dashboard keeps the label "Coste por kilómetro" with the total-based definition for the whole history, and its caption must say so (replace "Basado en historial de combustible").
  5. Create one `Formatters` object (money with €, price per liter `€/L`, cost per km `€/km`, distance `km`, monthly `€/mes`) and replace ad-hoc formatting across Statistics, Dashboard and fuel history. Fix the `¢/L` unit everywhere.
  6. Edge cases: no records, one record, distance 0, a single month. Return null/empty results; never show `Infinity`, `NaN` or `0.00 €/km` for missing data.
  7. Unit tests with this fixture (from the screenshots): September has fuel 50.00 € / 28.57 L and 30.00 € / 16.04 L plus maintenance 12.00 €; October has fuel 140.00 € in total and maintenance 4.63 €. Expected: monthly totals [92.00, 144.63]; fuel 220.00; maintenance 16.63; total 236.63; average monthly 118.32; September price 1.79 €/L (weighted: 80 / 44.61), which differs from the simple mean 1.81; with distance 1448 km, cost per km 0.16 €/km.
* **Acceptance Criteria:**
  * All the findings above are fixed or explained in `agent/MEMORY.md` if they turn out not to apply.
  * Tests above pass; the same inputs always give the same text on Dashboard and Statistics.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-005 — Statistics UI: explain every value
* **Status:** `BACKLOG`
* **Objective:** The user must know exactly what each number and each chart represents.
* **Requirements:**
  1. "Resumen" rows: each row shows its label, a short muted caption and the value with its unit, using `Formatters`. Captions (adjust the wording to match the real formulas from TASK-004):
    - Coste total: "Combustible + mantenimiento" → `236.63 €`
    - Combustible: "Suma de repostajes" → `220.00 €`
    - Mantenimiento: "Suma de servicios" → `16.63 €`
    - Distancia recorrida: caption describing the real formula (e.g. "Del primer al último kilometraje registrado") → `1448 km`
    - Coste por kilómetro: "Coste total ÷ distancia" → `0.16 €/km`
    - Gasto mensual medio: "Coste total ÷ meses" → `118.32 €/mes`
      When a value cannot be computed show "—" and a caption explaining what is missing (e.g. "Necesitas al menos 2 registros de kilometraje").
  2. "Gasto mensual" chart:
    - Title "Gasto mensual" and subtitle "Combustible + mantenimiento, en € por mes".
    - Replace the text line `9/2026: 92.00 · 10/2026: 144.63` with a month label under each bar (short Spanish month, plus the year when it changes) and the value above each bar (e.g. `92 €` or `144.63 €`).
    - Bars are zero-based with heights proportional to the value. Current month uses `barCurrent`, previous months `barPrevious`.
    - With many months, show the most recent 12 and let the chart scroll horizontally.
  3. "Tendencia del precio del combustible" chart:
    - Title "Precio del combustible" and subtitle "Precio medio por litro de cada mes, en €/L". A small caption states the method ("Importe ÷ litros de cada mes").
    - Replace the text line `9/2026: 1.79 · 10/2026: 1.82` with a value label and month label on each point.
    - The vertical scale is adjusted to the price range, so a small change looks steep. Add the caption "Escala ajustada al rango de precios" and a delta chip such as `+0.03 €/L (+1.7 %)` computed from the unrounded values.
    - One month: show a single point, no line. No data: empty state.
  4. Empty state for every card when the period has no data ("Sin datos en este periodo").
  5. Accessibility: each chart has a `contentDescription` summarizing its values and units (e.g. "Gasto mensual: septiembre 2026, 92 euros; octubre 2026, 144,63 euros").
  6. Visual style follows the approved design tokens; no new colors.
* **Acceptance Criteria:**
  * No number on the screen appears without a unit or an explanatory caption.
  * Changing the period filter updates summary, bars and price line consistently.
  * Visible in dark and light, no text clipping at large font sizes.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`

#### TASK-006 — CHANGELOG.md, version 0.3.0 and final regression
* **Status:** `BACKLOG`
* **Objective:** Create the public changelog and release-ready versioning.
* **Requirements:**
  1. Before creating the file, search the repository (README, docs, any landing folder) for links to the changelog and use that exact filename and casing (GitHub URLs are case-sensitive). If none is found, create `CHANGELOG.md` at the repository root. Report the final path.
  2. Format: Keep a Changelog 1.1.0 and Semantic Versioning (still 0.x). Written in Spanish because the audience is app users and it is linked from the Spanish landing page. Sections per version: Añadido / Cambiado / Corregido. Newest version first, with an `[Unreleased]` section on top and comparison links at the bottom if tags exist.
  3. Backfill from `git log`, tags and `agent/CHANGELOG.md`. Do not invent entries; if dates cannot be determined, use the commit date. Use these known entries:
    - 0.2.2: Añadido: aviso de cambios sin guardar al salir de los formularios de nuevo repostaje y nuevo servicio. Corregido: los formularios ya no siguen abiertos al volver desde otra pestaña.
    - 0.2.1: Cambiado: rediseño visual con la paleta "Medianoche con calor" y degradados, en modo claro y oscuro. Corregido: dígitos invertidos en los campos de antelación, textos que rompían la interfaz en Ajustes, mensajes de guardado e importación que no desaparecían, barras superior e inferior más compactas, versión mostrada en "Acerca de".
    - Earlier releases: only what git history supports.
  4. Add the 0.3.0 entry for this epic: settings persistence with "Guardar cambios", notification permission and status/test notification, reliable reminder scheduling, clarified Statistics (units, captions, calculation fixes), `¢/L` → `€/L`, and the new changelog.
  5. Set `versionName` to `0.3.0` and increase `versionCode` by one. The About card keeps reading the version from `BuildConfig`.
  6. Process rule from now on: every task that changes behavior adds a line to `[Unreleased]` in `CHANGELOG.md`, in addition to `agent/CHANGELOG.md`.
  7. Final regression pass: Settings save, permission flow on a fresh install (Android 13+), test notification, Statistics in all four period filters, dark and light.
* **Acceptance Criteria:**
  * `CHANGELOG.md` exists with the correct filename, valid structure and the entries above.
  * About shows `Vehicle Manager v0.3.0`.
* **Verification:** `./gradlew testDebugUnitTest assembleDebug`