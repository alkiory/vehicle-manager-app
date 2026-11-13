package com.example.vehiclemanager.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OdometerTimelineTest {
    private data class Row(
        override val id: Long,
        override val timestampMs: Long,
        override val odometerKm: Long,
    ) : OdometerTimelineRecord

    @Test
    fun returnsTheClosestReadingsAroundTheSelectedDate() {
        val records = listOf(
            Row(id = 1, timestampMs = 1_000, odometerKm = 40_000),
            Row(id = 2, timestampMs = 3_000, odometerKm = 60_000),
        )

        assertEquals(
            OdometerNeighbors(previousOdometerKm = 40_000, nextOdometerKm = 60_000),
            chronologicalOdometerNeighbors(records, timestampMs = 2_000),
        )
    }

    @Test
    fun excludesTheEditedRecordFromItsOwnNeighbours() {
        val records = listOf(
            Row(id = 1, timestampMs = 1_000, odometerKm = 40_000),
            Row(id = 2, timestampMs = 2_000, odometerKm = 50_000),
            Row(id = 3, timestampMs = 3_000, odometerKm = 60_000),
        )

        assertEquals(
            OdometerNeighbors(previousOdometerKm = 40_000, nextOdometerKm = 60_000),
            chronologicalOdometerNeighbors(records, timestampMs = 2_000, excludeId = 2),
        )
    }

    @Test
    fun returnsNullBoundsWhenNoRecordExistsOnThatSide() {
        val records = listOf(Row(id = 1, timestampMs = 1_000, odometerKm = 40_000))

        val afterAll = chronologicalOdometerNeighbors(records, timestampMs = 2_000)
        assertEquals(40_000L, afterAll.previousOdometerKm)
        assertNull(afterAll.nextOdometerKm)

        val beforeAll = chronologicalOdometerNeighbors(records, timestampMs = 500)
        assertNull(beforeAll.previousOdometerKm)
        assertEquals(40_000L, beforeAll.nextOdometerKm)
    }
}
