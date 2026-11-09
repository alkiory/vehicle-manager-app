package com.example.vehiclemanager.core.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.vehiclemanager.core.domain.ReminderPreferencesRepository
import com.example.vehiclemanager.core.domain.notification.RunReminderCheckUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderPreferencesRepository: ReminderPreferencesRepository

    @Inject
    lateinit var runReminderCheckUseCase: RunReminderCheckUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = reminderPreferencesRepository.reminderPreferences.first()
                    ReminderScheduler.ensureScheduled(
                        context,
                        prefs.notificationTimeHours,
                        prefs.notificationTimeMinutes,
                    )
                    runReminderCheckUseCase.checkAndRunCatchUpIfNeeded()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
