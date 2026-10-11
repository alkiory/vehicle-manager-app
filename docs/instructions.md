# Vehicle Manager — Master Product & Architecture Blueprint

Este documento es el **índice maestro y contrato arquitectónico** del proyecto.
Contiene únicamente el conocimiento **transversal** (stack, arquitectura, protocolo
del agente, convenciones). El detalle de cada épica vive en archivos separados
dentro de `/epics/epicXXX.md`.

> **Regla de oro para agentes:** carga este archivo siempre. Carga un `epicXXX.md`
> únicamente cuando la tarea activa pertenezca a esa épica. Nunca cargues varias
> épicas a la vez salvo que la tarea lo exija explícitamente.

---

## Índice de contenido

1. [System Architecture & Technical Stack](#1-system-architecture--technical-stack)
2. [Repository Memory & Directory Structure](#2-repository-memory--directory-structure)
3. [Agent State Machine & Lifecycle](#3-agent-state-machine--lifecycle)
4. [Índice de Épicas](#4-índice-de-épicas)
5. [Estado actual del proyecto](#5-estado-actual-del-proyecto)

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
* **Build System:** Gradle with Version Catalog (`../gradle/libs.versions.toml`)

### Architecture Rules

1. **Layer Separation:**
  * **Presentation Layer:** Jetpack Compose screens consuming state from `ViewModel`
    via `StateFlow<UiState>`. Zero business logic in Composables.
  * **Domain Layer:** Pure Kotlin modules containing Use Cases, Domain Models, and
    Value Objects. **Strictly no Android or Room imports**.
  * **Data Layer:** Room Entities, DAOs, Repositories, and Data Sources. Maps Room
    Entities to Domain Models via extension mappers.

2. **Data Integrity Standard:**
  * **Monetary Values:** Stored as `Long` minor units (e.g., cents) to eliminate
    floating-point inaccuracies.
  * **Fuel Quantities:** Stored as integer minor units (liters × 100, e.g., 45.25 L = `4525`).
  * **Odometer:** Stored as `Int` or `Long` in kilometers.

3. **Offline-First & Local Persistence Guarantee:**
  * Local SQLite database via Room and Android DataStore act as the canonical,
    permanent source of truth on the user's phone storage.
  * Zero remote network or backend dependencies. Data must survive app kills,
    process death, and full device reboots.

---

## 2. Repository Memory & Directory Structure
````
/
├── README.md
├── knowledge/
│ ├── product.md
│ ├── architecture.md
│ ├── domain.md
│ ├── database.md
│ ├── calculations.md
│ ├── conventions.md
│ └── testing.md
├── agent/
│ ├── CURRENT.md
│ ├── CHANGELOG.md
│ ├── MEMORY.md
│ └── BACKLOG.md
└── epics/
├── instructions.md ← este archivo
├── epic001.md
├── epic002.md
├── epic003.md
├── epic004.md
├── epic005.md
├── epic006.md
├── epic007.md
├── epic008.md
├── epic009.md
├── epic010.md
├── epic011.md
├── epic012.md
├── epic013.md
├── epic014.md
├── epic015.md
├── epic016.md
├── epic017.md
├── epic018.md
├── epic019.md
├── epic020.md
├── epic021.md
└── epic022.md
````


---

## 3. Agent State Machine & Lifecycle

Every task strictly follows the lifecycle state machine:

$$\text{BACKLOG} \longrightarrow \text{READY} \longrightarrow \text{IN\_PROGRESS} \longrightarrow \text{REVIEW} \longrightarrow \text{DONE}$$

*If blocked, state transitions to `BLOCKED` with mandatory `blocked_by` details
recorded in `../agent/CURRENT.md`.*

> **CRITICAL AGENT MEMORY MANDATE:**
> Upon starting and completing any task, the agent **MUST** explicitly update the
> memory files located inside the `../agent` folder (`../agent/CURRENT.md`,
> `../agent/CHANGELOG.md`, `../agent/MEMORY.md`, and `../agent/BACKLOG.md`).
> The repository files remain the single source of truth across development sessions.

---

## 4. Índice de Épicas

| Épica | Título | Estado | Archivo |
|-------|--------|--------|---------|
| EPIC-001 | Foundation & Architecture | 🟢 DONE | [epic001.md](./epic001.md) |
| EPIC-002 | Vehicle Management | 🟢 DONE | [epic002.md](./epic002.md) |
| EPIC-003 | Fuel Tracking & Consumption Engine | 🟢 DONE | [epic003.md](./epic003.md) |
| EPIC-004 | Maintenance & Service Tracking | 🟢 DONE | [epic004.md](./epic004.md) |
| EPIC-005 | Dashboard & Quick Action Hub | 🟢 DONE | [epic005.md](./epic005.md) |
| EPIC-006 | Statistics & Analytics Engine | 🟢 DONE | [epic006.md](./epic006.md) |
| EPIC-007 | Polish, Data Export & Refactoring | 🟢 DONE | [epic007.md](./epic007.md) |
| EPIC-008 | Visual Polish & Fluent Icon System | 🟢 DONE | [epic008.md](./epic008.md) |
| EPIC-009 | Advanced Vehicle Statistics & Insights | 🟢 DONE | [epic009.md](./epic009.md) |
| EPIC-010 | Notifications & Reminders | 🟢 DONE | [epic010.md](./epic010.md) |
| EPIC-011 | Enhanced Notification Settings | 🟢 DONE | [epic011.md](./epic011.md) |
| EPIC-012 | Custom App Icon & Animated Splash | 🟢 DONE | [epic012.md](./epic012.md) |
| EPIC-013 | Offline Persistence & Device Lifecycle | 🟢 DONE | [epic013.md](./epic013.md) |
| EPIC-014 | Estadísticas, Timeline Drivvo y Autonomía | 🟢 DONE | [epic014.md](./epic014.md) |
| EPIC-015 | UI/UX Layout Fixes & Overdue Tracking | 🟢 DONE | [epic015.md](./epic015.md) |
| EPIC-016 | Icono Adaptativo, Branding & Tema Automático | 🟢 DONE | [epic016.md](./epic016.md) |
| EPIC-017 | Permisos de Notificaciones & Onboarding | 🟢 DONE | [epic017.md](./epic017.md) |
| EPIC-018 | Predicción de Consumo & Próximo Repostaje | 🟢 DONE | [epic018.md](./epic018.md) |
| EPIC-019 | Actualización de Changelog & Memoria | 🟢 DONE | [epic019.md](./epic019.md) |
| EPIC-020 | _(pendiente de especificar)_ | 🟢 DONE | [epic020.md](./epic020.md) |
| EPIC-021 | _(pendiente de especificar)_ | 🟢 DONE  | [epic021.md](./epic021.md) |
| EPIC-022 | Persistent Settings, Reliable Notifications, Statistics Clarity and Changelog | 🟡 IN_PROGRESS | [epic022.md](./epic022.md) |

> Los `epicXXX.md` de épicas completadas se conservan como **registro histórico**.
> Los agentes pueden ignorarlos durante el desarrollo activo salvo que se investigue
> una regresión.

---

## 5. Estado actual del proyecto

* **Versión actual de la app:** `0.3.0`
* **Épica activa:** EPIC-022
* **Última épica cerrada:** EPIC-019 (documentación) — EPIC-020 y EPIC-021 cerradas también, ver `agent/CHANGELOG.md`
* **Próximo hito:** _(a definir por el mantenedor)_

### Convenciones de actualización

* Al **abrir** una épica nueva: crear `epics/epicXXX.md` desde la plantilla
  (ver `epics/_template.md` si existe, o replicar la estructura de `epic018.md`).
* Al **cerrar** una épica: marcar todas sus tareas como `🟢 DONE`, actualizar el
  estado en la tabla del §4, y registrar el cierre en `agent/CHANGELOG.md`.
* Al **añadir tareas** a una épica ya cerrada: **no reabrir la épica**. Crear una
  épica de seguimiento (`EPIC-0XX-followup-<nombre>`).

---

## 6. Relación con otras fuentes de verdad

* **`knowledge/`** — conocimiento técnico profundo (fórmulas, convenciones, arquitectura).
  Los `epicXXX.md` referencian estos archivos en lugar de duplicar contenido.
* **`agent/`** — estado vivo del agente. Se actualiza en cada transición de estado.
* **`README.md`** — visión de producto orientada al usuario final, no al desarrollador.
