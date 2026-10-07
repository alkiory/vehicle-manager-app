# Conventions

Organize shared code under `core.data`, `core.domain`, and `core.ui`; organize user-facing functionality under `feature.*`. Keep business logic out of Composables.

## Design & UI Tokens
- **Adaptive Icon:** 108×108 dp canvas with `22dp` inset to guarantee full visibility inside the 66dp circular safe zone.
- **Branding Header:** App logo icon (28dp) + "Vehicle Manager" bold primary title in top app bar.
- **Color Scheme:** Light and dark Material 3 color schemes configured in `Color.kt` and `Theme.kt`, fully WCAG 2.1 AA compliant.
- **Theme Modes:** `SYSTEM`, `LIGHT`, `DARK` managed reactively via `ThemePreferenceRepository`.
