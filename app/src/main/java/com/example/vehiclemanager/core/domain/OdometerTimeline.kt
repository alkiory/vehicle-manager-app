package com.example.vehiclemanager.core.domain

import java.math.BigDecimal

/** A dated odometer reading that can be ordered against other records. */
interface OdometerTimelineRecord {
    val id: Long
    val timestampMs: Long
    val odometerKm: Long
}

/** The closest odometer readings surrounding a selected date. */
data class OdometerNeighbors(
    val previousOdometerKm: Long? = null,
    val nextOdometerKm: Long? = null,
)

/**
 * Finds the records immediately before and after [timestampMs].
 *
 * Records are ordered by timestamp and then id.
 * The record being edited is excluded so a correction never compares against
 * itself. A backdated record must sit between these two readings, regardless
 * of the vehicle's current odometer.
 */
fun chronologicalOdometerNeighbors(
    records: List<OdometerTimelineRecord>,
    timestampMs: Long,
    excludeId: Long? = null,
): OdometerNeighbors {
    val sorted = records
        .asSequence()
        .filter { excludeId == null || it.id != excludeId }
        .sortedWith(compareBy({ it.timestampMs }, { it.id }))
        .toList()

    var previousOdometerKm: Long? = null
    var nextOdometerKm: Long? = null
    for (record in sorted) {
        when {
            record.timestampMs < timestampMs -> previousOdometerKm = record.odometerKm
            record.timestampMs > timestampMs && nextOdometerKm == null ->
                nextOdometerKm = record.odometerKm
        }
    }
    return OdometerNeighbors(previousOdometerKm, nextOdometerKm)
}

/**
 * Parses a user-entered decimal, accepting Spanish decimal commas as well as
 * the canonical point (`1,475` and `6,7`).
 */
internal fun parseDecimalOrNull(value: String): BigDecimal? = value
    .trim()
    .takeIf(String::isNotEmpty)
    ?.replace(',', '.')
    ?.toBigDecimalOrNull()
