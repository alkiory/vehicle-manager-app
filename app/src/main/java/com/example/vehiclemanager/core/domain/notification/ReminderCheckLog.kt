package com.example.vehiclemanager.core.domain.notification

import kotlinx.serialization.Serializable

enum class ReminderCheckTrigger {
    SCHEDULED,
    CATCH_UP,
    MANUAL
}

@Serializable
data class ReminderCheckItemResult(
    val scheduleId: Long,
    val vehicleId: Long,
    val vehicleName: String,
    val serviceTitle: String,
    val dueInfo: String,
    val isNotified: Boolean,
    val resultReason: String,
)

@Serializable
data class ReminderCheckLog(
    val timestampMs: Long,
    val trigger: ReminderCheckTrigger,
    val evaluatedCount: Int,
    val postedCount: Int,
    val skipReasonSummary: String? = null,
    val itemResults: List<ReminderCheckItemResult> = emptyList(),
)
