package com.example.vehiclemanager.core.data.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vehiclemanager.core.domain.ReminderPreferencesRepository
import com.example.vehiclemanager.core.domain.notification.ReminderCheckTrigger
import com.example.vehiclemanager.core.domain.notification.RunReminderCheckUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * WorkManager worker that checks for due services and schedules notifications.
 */
@HiltWorker
class ServiceReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val runReminderCheckUseCase: RunReminderCheckUseCase,
    private val reminderPreferencesRepository: ReminderPreferencesRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            runReminderCheckUseCase.execute(ReminderCheckTrigger.SCHEDULED)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        } finally {
            runCatching {
                val prefs = reminderPreferencesRepository.reminderPreferences.first()
                ReminderScheduler.schedule(
                    context,
                    prefs.notificationTimeHours,
                    prefs.notificationTimeMinutes,
                    forceReschedule = true,
                )
            }
        }
    }

    companion object {
        fun schedule(context: Context, hours: Int = 9, minutes: Int = 0) {
            ReminderScheduler.schedule(context, hours, minutes, forceReschedule = true)
        }

        fun ensureScheduled(context: Context, hours: Int = 9, minutes: Int = 0) {
            ReminderScheduler.ensureScheduled(context, hours, minutes)
        }

        fun cancel(context: Context) {
            ReminderScheduler.cancel(context)
        }
    }
}
