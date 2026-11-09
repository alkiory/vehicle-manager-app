package com.example.vehiclemanager.core.data.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    const val WORK_NAME = "reminder-daily-check"

    fun calculateInitialDelayMs(
        notificationTimeHours: Int,
        notificationTimeMinutes: Int,
        now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())
    ): Long {
        var target = now.withHour(notificationTimeHours)
            .withMinute(notificationTimeMinutes)
            .withSecond(0)
            .withNano(0)
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return Duration.between(now, target).toMillis()
    }

    fun schedule(
        context: Context,
        notificationTimeHours: Int = 9,
        notificationTimeMinutes: Int = 0,
        forceReschedule: Boolean = true,
    ) {
        val delayMs = calculateInitialDelayMs(notificationTimeHours, notificationTimeMinutes)
        val workRequest = OneTimeWorkRequestBuilder<ServiceReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()

        val policy = if (forceReschedule) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                WORK_NAME,
                policy,
                workRequest,
            )
    }

    fun ensureScheduled(
        context: Context,
        notificationTimeHours: Int = 9,
        notificationTimeMinutes: Int = 0,
    ) {
        schedule(context, notificationTimeHours, notificationTimeMinutes, forceReschedule = false)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
