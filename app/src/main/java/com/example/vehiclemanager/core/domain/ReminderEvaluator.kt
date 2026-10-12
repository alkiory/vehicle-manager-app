package com.example.vehiclemanager.core.domain

import java.time.LocalDate

data class ReminderItem(
    val scheduleId: Long,
    val vehicleId: Long,
    val vehicleName: String,
    val serviceTitle: String,
    val dueDate: LocalDate?,
    val dueOdometer: Long?,
    val isTirePressure: Boolean = false,
    val isFuel: Boolean = false,
)

enum class ReminderEvaluationOutcome {
    DUE_BY_DATE,
    DUE_BY_KM,
    NOT_YET_DUE,
    ALREADY_NOTIFIED_TODAY,
    TOGGLED_OFF,
}

data class ReminderEvaluationResult(
    val scheduleId: Long,
    val outcome: ReminderEvaluationOutcome,
    val shouldNotify: Boolean,
)

object ReminderEvaluator {
    fun evaluate(
        item: ReminderItem,
        currentOdometer: Long,
        today: LocalDate,
        leadDays: Int,
        leadKm: Long,
        fuelNotificationsEnabled: Boolean,
        tirePressureNotificationsEnabled: Boolean,
        lastNotifiedDate: LocalDate? = null,
    ): ReminderEvaluationResult {
        // 1. Toggle-off check
        if (item.isFuel && !fuelNotificationsEnabled) {
            return ReminderEvaluationResult(item.scheduleId, ReminderEvaluationOutcome.TOGGLED_OFF, false)
        }
        if (item.isTirePressure && !tirePressureNotificationsEnabled) {
            return ReminderEvaluationResult(item.scheduleId, ReminderEvaluationOutcome.TOGGLED_OFF, false)
        }

        // 2. Already notified today check
        if (lastNotifiedDate != null && lastNotifiedDate == today) {
            return ReminderEvaluationResult(item.scheduleId, ReminderEvaluationOutcome.ALREADY_NOTIFIED_TODAY, false)
        }

        // 3. Date-based check
        val dueDate = item.dueDate
        if (dueDate != null) {
            val thresholdDate = today.plusDays(leadDays.toLong())
            if (!dueDate.isAfter(thresholdDate)) { // dueDate <= today + leadDays (includes overdue)
                return ReminderEvaluationResult(item.scheduleId, ReminderEvaluationOutcome.DUE_BY_DATE, true)
            }
        }

        // 4. Km-based check
        val dueOdometer = item.dueOdometer
        if (dueOdometer != null) {
            val thresholdOdometer = dueOdometer - leadKm
            if (currentOdometer >= thresholdOdometer) { // currentOdometer >= dueOdometer - leadKm (includes overdue)
                return ReminderEvaluationResult(item.scheduleId, ReminderEvaluationOutcome.DUE_BY_KM, true)
            }
        }

        return ReminderEvaluationResult(item.scheduleId, ReminderEvaluationOutcome.NOT_YET_DUE, false)
    }
}
