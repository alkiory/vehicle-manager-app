package com.example.vehiclemanager.core.data.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderSchedulerTest {

    @Test
    fun calculateInitialDelayMs_whenTargetIsAfterCurrentTime_calculatesCorrectDelay() {
        // Given now is 08:00
        val now = ZonedDateTime.of(2026, 3, 10, 8, 0, 0, 0, ZoneId.of("Europe/Madrid"))
        // Target is 09:00
        val delay = ReminderScheduler.calculateInitialDelayMs(9, 0, now)
        assertEquals(Duration.ofHours(1).toMillis(), delay)
    }

    @Test
    fun calculateInitialDelayMs_whenTargetIsBeforeCurrentTime_calculatesNextDayDelay() {
        // Given now is 10:00
        val now = ZonedDateTime.of(2026, 3, 10, 10, 0, 0, 0, ZoneId.of("Europe/Madrid"))
        // Target is 09:00 -> should be 23 hours later
        val delay = ReminderScheduler.calculateInitialDelayMs(9, 0, now)
        assertEquals(Duration.ofHours(23).toMillis(), delay)
    }

    @Test
    fun calculateInitialDelayMs_whenTargetIsExactCurrentTime_calculates24HoursDelay() {
        val now = ZonedDateTime.of(2026, 3, 10, 9, 0, 0, 0, ZoneId.of("Europe/Madrid"))
        val delay = ReminderScheduler.calculateInitialDelayMs(9, 0, now)
        assertEquals(Duration.ofHours(24).toMillis(), delay)
    }

    @Test
    fun calculateInitialDelayMs_acrossMidnight_calculatesCorrectDelay() {
        // Given now is 23:30
        val now = ZonedDateTime.of(2026, 3, 10, 23, 30, 0, 0, ZoneId.of("Europe/Madrid"))
        // Target is 00:30
        val delay = ReminderScheduler.calculateInitialDelayMs(0, 30, now)
        assertEquals(Duration.ofMinutes(60).toMillis(), delay)
    }

    @Test
    fun calculateInitialDelayMs_acrossDstChange_calculatesDstSafeDelay() {
        val now = ZonedDateTime.of(2026, 3, 28, 23, 0, 0, 0, ZoneId.of("Europe/Madrid"))
        val delay = ReminderScheduler.calculateInitialDelayMs(9, 0, now)
        assertTrue(delay > 0)
    }
}
