package com.example.vehiclemanager.core.domain

/**
 * Canonical vehicle types shared with the web client and backup schema v2.
 *
 * The stable identifier is the enum name; the display label and Material Symbol
 * name are the same values used by the web catalog.
 */
enum class VehicleType(
    val displayName: String,
    val materialSymbol: String,
) {
    CAR("Coche", "directions_car"),
    MOTORCYCLE("Motocicleta", "motorcycle"),
    TRUCK("Camión", "local_shipping"),
    VAN("Furgoneta", "airport_shuttle"),
    BUS("Autobús", "directions_bus"),
    ELECTRIC_BICYCLE("Bicicleta eléctrica", "electric_bike"),
    ;

    companion object {
        /** Exact backup identifier lookup: unknown values must stay rejected. */
        fun fromStored(value: String): VehicleType? = entries.firstOrNull { it.name == value }

        /** Local persistence/UI fallback for legacy or unknown stored values. */
        fun normalize(value: String?): VehicleType =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: CAR
    }
}
