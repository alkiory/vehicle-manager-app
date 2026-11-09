package com.example.vehiclemanager.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderEvaluatorTest {

    private val today = LocalDate.of(2025, 6, 1)

    @Test
    fun notYetDue() {
        val item = ReminderItem(
            scheduleId = 1L,
            vehicleId = 1L,
            vehicleName = "Car 1",
            serviceTitle = "Inspection",
            dueDate = LocalDate.of(2025, 6, 20), // 19 days away (lead 7)
            dueOdometer = 20000L,
        )
        val result = ReminderEvaluator.evaluate(
            item = item,
            currentOdometer = 10000L,
            today = today,
            leadDays = 7,
            leadKm = 500L,
            fuelNotificationsEnabled = true,
            tirePressureNotificationsEnabled = false,
        )
        assertFalse(result.shouldNotify)
        assertEquals(ReminderEvaluationOutcome.NOT_YET_DUE, result.outcome)
    }

    @Test
    fun leadStage() {
        val item = ReminderItem(
            scheduleId = 2L,
            vehicleId = 1L,
            vehicleName = "Car 1",
            serviceTitle = "Oil Change",
            dueDate = LocalDate.of(2025, 6, 5), // 4 days away (lead 7)
            dueOdometer = 15000L,
        )
        val result = ReminderEvaluator.evaluate(
            item = item,
            currentOdometer = 10000L,
            today = today,
            leadDays = 7,
            leadKm = 500L,
            fuelNotificationsEnabled = true,
            tirePressureNotificationsEnabled = false,
        )
        assertTrue(result.shouldNotify)
        assertEquals(ReminderEvaluationOutcome.STAGE_LEAD, result.outcome)
        assertEquals(ReminderStage.LEAD, result.currentStage)
    }

    @Test
    fun dueStage() {
        val item = ReminderItem(
            scheduleId = 3L,
            vehicleId = 1L,
            vehicleName = "Car 1",
            serviceTitle = "Tire Rotation",
            dueDate = LocalDate.of(2025, 6, 1), // due today
            dueOdometer = 10000L,
        )
        val result = ReminderEvaluator.evaluate(
            item = item,
            currentOdometer = 10000L,
            today = today,
            leadDays = 7,
            leadKm = 500L,
            fuelNotificationsEnabled = true,
            tirePressureNotificationsEnabled = false,
        )
        assertTrue(result.shouldNotify)
        assertEquals(ReminderEvaluationOutcome.STAGE_DUE, result.outcome)
        assertEquals(ReminderStage.DUE, result.currentStage)
    }

    @Test
    fun overdueStage() {
        val item = ReminderItem(
            scheduleId = 4L,
            vehicleId = 1L,
            vehicleName = "Car 1",
            serviceTitle = "Brakes",
            dueDate = LocalDate.of(2025, 5, 25), // past due
            dueOdometer = 9000L, // current 10000 > 9000
        )
        val result = ReminderEvaluator.evaluate(
            item = item,
            currentOdometer = 10000L,
            today = today,
            leadDays = 7,
            leadKm = 500L,
            fuelNotificationsEnabled = true,
            tirePressureNotificationsEnabled = false,
        )
        assertTrue(result.shouldNotify)
        assertEquals(ReminderEvaluationOutcome.STAGE_OVERDUE, result.outcome)
        assertEquals(ReminderStage.OVERDUE, result.currentStage)
    }

    @Test
    fun stageSkippedCatchUp() {
        // App was closed during LEAD and DUE, now OVERDUE. Should notify OVERDUE once without skipping.
        val item = ReminderItem(
            scheduleId = 5L,
            vehicleId = 1L,
            vehicleName = "Car 1",
            serviceTitle = "Inspection",
            dueDate = LocalDate.of(2025, 5, 20),
            dueOdometer = 8000L,
        )
        val result = ReminderEvaluator.evaluate(
            item = item,
            currentOdometer = 10000L,
            today = today,
            leadDays = 7,
            leadKm = 500L,
            fuelNotificationsEnabled = true,
            tirePressureNotificationsEnabled = false,
            lastNotifiedStage = null,
        )
        assertTrue(result.shouldNotify)
        assertEquals(ReminderEvaluationOutcome.STAGE_OVERDUE, result.outcome)
    }

    @Test
    fun editedReminderResetsStage() {
        val itemOriginal = ReminderItem(
            scheduleId = 6L,
            vehicleId = 1L,
            vehicleName = "Car 1",
            serviceTitle = "Filter",
            dueDate = LocalDate.of(2025, 6, 3),
            dueOdometer = 15000L, // far away km so only date LEAD applies
        )
        // Already notified LEAD for original due date
        val result1 = ReminderEvaluator.evaluate(
            item = itemOriginal,
            currentOdometer = 10000L,
            today = today,
            leadDays = 7,
            leadKm = 500L,
            fuelNotificationsEnabled = true,
            tirePressureNotificationsEnabled = false,
            lastNotifiedStage = ReminderStage.LEAD,
            lastNotifiedDueDate = LocalDate.of(2025, 6, 3),
            lastNotifiedDueOdometer = 15000L,
        )
        assertFalse(result1.shouldNotify)

        // User edits reminder: dueDate changes to 2025, 6, 5 (still within lead days, target changed)
        val itemEdited = itemOriginal.copy(dueDate = LocalDate.of(2025, 6, 5))
        val result2 = ReminderEvaluator.evaluate(
            item = itemEdited,
            currentOdometer = 10000L,
            today = today,
            leadDays = 7,
            leadKm = 500L,
            fuelNotificationsEnabled = true,
            tirePressureNotificationsEnabled = false,
            lastNotifiedStage = ReminderStage.LEAD,
            lastNotifiedDueDate = LocalDate.of(2025, 6, 3), // old date
            lastNotifiedDueOdometer = 15000L,
        )
        assertTrue(result2.shouldNotify) // Resets because target changed
    }

    @Test
    fun twoVehiclesEvaluation() {
        val itemCar1 = ReminderItem(
            scheduleId = 10L,
            vehicleId = 1L,
            vehicleName = "Car 1",
            serviceTitle = "Service 1",
            dueDate = LocalDate.of(2025, 6, 3),
            dueOdometer = 10000L,
        )
        val itemCar2 = ReminderItem(
            scheduleId = 20L,
            vehicleId = 2L,
            vehicleName = "Car 2",
            serviceTitle = "Service 2",
            dueDate = LocalDate.of(2025, 6, 15),
            dueOdometer = 20000L,
        )

        val res1 = ReminderEvaluator.evaluate(itemCar1, 10000L, today, 7, 500L, true, false)
        val res2 = ReminderEvaluator.evaluate(itemCar2, 10000L, today, 7, 500L, true, false)

        assertTrue(res1.shouldNotify)
        assertFalse(res2.shouldNotify)
    }

    @Test
    fun midnightBoundaryAndTimeZoneChange() {
        val zdtUtc = ZonedDateTime.of(2025, 6, 1, 23, 59, 0, 0, ZoneId.of("UTC"))
        val zdtEt = zdtUtc.withZoneSameInstant(ZoneId.of("America/New_York"))
        val dateLocal = zdtEt.toLocalDate()

        val item = ReminderItem(
            scheduleId = 30L,
            vehicleId = 1L,
            vehicleName = "Car 1",
            serviceTitle = "Timezone Test",
            dueDate = dateLocal,
            dueOdometer = 15000L,
        )
        val result = ReminderEvaluator.evaluate(
            item = item,
            currentOdometer = 10000L,
            today = dateLocal,
            leadDays = 7,
            leadKm = 500L,
            fuelNotificationsEnabled = true,
            tirePressureNotificationsEnabled = false,
        )
        assertTrue(result.shouldNotify)
        assertEquals(ReminderEvaluationOutcome.STAGE_DUE, result.outcome)
    }

    @Test
    fun toggledOffFuel() {
        val item = ReminderItem(
            scheduleId = 40L,
            vehicleId = 1L,
            vehicleName = "Car",
            serviceTitle = "Fuel Check",
            dueDate = LocalDate.of(2025, 6, 2),
            dueOdometer = 10000L,
            isFuel = true,
        )
        val result = ReminderEvaluator.evaluate(
            item = item,
            currentOdometer = 10000L,
            today = today,
            leadDays = 7,
            leadKm = 500L,
            fuelNotificationsEnabled = false,
            tirePressureNotificationsEnabled = false,
        )
        assertFalse(result.shouldNotify)
        assertEquals(ReminderEvaluationOutcome.TOGGLED_OFF, result.outcome)
    }
}
