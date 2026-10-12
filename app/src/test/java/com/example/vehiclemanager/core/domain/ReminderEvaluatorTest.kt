package com.example.vehiclemanager.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReminderEvaluatorTest {

    private val today = LocalDate.of(2025, 6, 1)

    @Test
    fun dueByDateWithinLeadDays() {
        val item = ReminderItem(
            scheduleId = 1L,
            vehicleId = 1L,
            vehicleName = "Car",
            serviceTitle = "Oil Change",
            dueDate = LocalDate.of(2025, 6, 5), // 4 days from today
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
        assertEquals(ReminderEvaluationOutcome.DUE_BY_DATE, result.outcome)
    }

    @Test
    fun dueByKmWithinLeadKm() {
        val item = ReminderItem(
            scheduleId = 2L,
            vehicleId = 1L,
            vehicleName = "Car",
            serviceTitle = "Tire Rotation",
            dueDate = LocalDate.of(2025, 12, 31), // far in future
            dueOdometer = 10400L,
        )
        val result = ReminderEvaluator.evaluate(
            item = item,
            currentOdometer = 10000L, // threshold is 10400 - 500 = 9900. current (10000) >= 9900
            today = today,
            leadDays = 7,
            leadKm = 500L,
            fuelNotificationsEnabled = true,
            tirePressureNotificationsEnabled = false,
        )
        assertTrue(result.shouldNotify)
        assertEquals(ReminderEvaluationOutcome.DUE_BY_KM, result.outcome)
    }

    @Test
    fun notYetDue() {
        val item = ReminderItem(
            scheduleId = 3L,
            vehicleId = 1L,
            vehicleName = "Car",
            serviceTitle = "Inspection",
            dueDate = LocalDate.of(2025, 6, 20), // 19 days away (lead 7)
            dueOdometer = 20000L, // current 10000 (threshold 19500)
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
    fun alreadyNotifiedToday() {
        val item = ReminderItem(
            scheduleId = 4L,
            vehicleId = 1L,
            vehicleName = "Car",
            serviceTitle = "Oil Change",
            dueDate = LocalDate.of(2025, 6, 2),
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
            lastNotifiedDate = today,
        )
        assertFalse(result.shouldNotify)
        assertEquals(ReminderEvaluationOutcome.ALREADY_NOTIFIED_TODAY, result.outcome)
    }

    @Test
    fun toggledOffFuel() {
        val item = ReminderItem(
            scheduleId = 5L,
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
            fuelNotificationsEnabled = false, // toggled off
            tirePressureNotificationsEnabled = false,
        )
        assertFalse(result.shouldNotify)
        assertEquals(ReminderEvaluationOutcome.TOGGLED_OFF, result.outcome)
    }

    @Test
    fun toggledOffTirePressure() {
        val item = ReminderItem(
            scheduleId = 6L,
            vehicleId = 1L,
            vehicleName = "Car",
            serviceTitle = "Tire Pressure Check",
            dueDate = LocalDate.of(2025, 6, 2),
            dueOdometer = 10000L,
            isTirePressure = true,
        )
        val result = ReminderEvaluator.evaluate(
            item = item,
            currentOdometer = 10000L,
            today = today,
            leadDays = 7,
            leadKm = 500L,
            fuelNotificationsEnabled = true,
            tirePressureNotificationsEnabled = false, // toggled off
        )
        assertFalse(result.shouldNotify)
        assertEquals(ReminderEvaluationOutcome.TOGGLED_OFF, result.outcome)
    }
}
