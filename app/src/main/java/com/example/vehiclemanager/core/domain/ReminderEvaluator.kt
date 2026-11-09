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

enum class ReminderStage {
    LEAD,
    DUE,
    OVERDUE,
}

enum class ReminderEvaluationOutcome {
    STAGE_LEAD,
    STAGE_DUE,
    STAGE_OVERDUE,
    NOT_YET_DUE,
    ALREADY_NOTIFIED_FOR_STAGE,
    TOGGLED_OFF,
}

data class ReminderEvaluationResult(
    val scheduleId: Long,
    val outcome: ReminderEvaluationOutcome,
    val shouldNotify: Boolean,
    val currentStage: ReminderStage? = null,
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
        lastNotifiedStage: ReminderStage? = null,
        lastNotifiedDueDate: LocalDate? = null,
        lastNotifiedDueOdometer: Long? = null,
    ): ReminderEvaluationResult {
        // 1. Toggle-off check
        if (item.isFuel && !fuelNotificationsEnabled) {
            return ReminderEvaluationResult(item.scheduleId, ReminderEvaluationOutcome.TOGGLED_OFF, false)
        }
        if (item.isTirePressure && !tirePressureNotificationsEnabled) {
            return ReminderEvaluationResult(item.scheduleId, ReminderEvaluationOutcome.TOGGLED_OFF, false)
        }

        // 2. Target change check (if target changed, reset lastNotifiedStage)
        val targetChanged = lastNotifiedDueDate != item.dueDate || lastNotifiedDueOdometer != item.dueOdometer
        val effectiveLastNotifiedStage = if (targetChanged) null else lastNotifiedStage

        // 3. Determine Date stage
        val dateStage = when {
            item.dueDate == null -> null
            item.dueDate.isBefore(today) -> ReminderStage.OVERDUE
            item.dueDate.isEqual(today) -> ReminderStage.DUE
            item.dueDate.isAfter(today) && !item.dueDate.isAfter(today.plusDays(leadDays.toLong())) -> ReminderStage.LEAD
            else -> null
        }

        // 4. Determine Odometer stage
        val kmStage = when {
            item.dueOdometer == null -> null
            currentOdometer > item.dueOdometer -> ReminderStage.OVERDUE
            currentOdometer == item.dueOdometer -> ReminderStage.DUE
            currentOdometer >= item.dueOdometer - leadKm -> ReminderStage.LEAD
            else -> null
        }

        // 5. Combine stages (most advanced wins: OVERDUE > DUE > LEAD)
        val stages = listOfNotNull(dateStage, kmStage)
        val currentStage = when {
            stages.contains(ReminderStage.OVERDUE) -> ReminderStage.OVERDUE
            stages.contains(ReminderStage.DUE) -> ReminderStage.DUE
            stages.contains(ReminderStage.LEAD) -> ReminderStage.LEAD
            else -> null
        }

        if (currentStage == null) {
            return ReminderEvaluationResult(item.scheduleId, ReminderEvaluationOutcome.NOT_YET_DUE, false, null)
        }

        // 6. Compare with effective last notified stage
        val shouldNotify = if (effectiveLastNotifiedStage == null) {
            true
        } else {
            currentStage.ordinal > effectiveLastNotifiedStage.ordinal
        }

        val outcome = if (shouldNotify) {
            when (currentStage) {
                ReminderStage.LEAD -> ReminderEvaluationOutcome.STAGE_LEAD
                ReminderStage.DUE -> ReminderEvaluationOutcome.STAGE_DUE
                ReminderStage.OVERDUE -> ReminderEvaluationOutcome.STAGE_OVERDUE
            }
        } else {
            ReminderEvaluationOutcome.ALREADY_NOTIFIED_FOR_STAGE
        }

        return ReminderEvaluationResult(
            scheduleId = item.scheduleId,
            outcome = outcome,
            shouldNotify = shouldNotify,
            currentStage = currentStage,
        )
    }
}
