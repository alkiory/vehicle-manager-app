package com.example.vehiclemanager

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.vehiclemanager.core.data.notification.ReminderScheduler
import com.example.vehiclemanager.core.domain.ReminderPreferencesRepository
import com.example.vehiclemanager.core.domain.notification.RunReminderCheckUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class VehicleApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var reminderPreferencesRepository: ReminderPreferencesRepository

    @Inject
    lateinit var runReminderCheckUseCase: RunReminderCheckUseCase

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val prefs = reminderPreferencesRepository.reminderPreferences.first()
                ReminderScheduler.ensureScheduled(
                    this@VehicleApplication,
                    prefs.notificationTimeHours,
                    prefs.notificationTimeMinutes,
                )
                runReminderCheckUseCase.checkAndRunCatchUpIfNeeded()
            }
        }
    }
}
