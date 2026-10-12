package com.example.vehiclemanager.core.domain.notification

import android.os.Build

enum class NotificationPermissionStatus {
    GRANTED,
    NOT_ASKED,
    RATIONALE_NEEDED,
    PERMANENTLY_DENIED,
    APP_NOTIFICATIONS_DISABLED,
}

interface NotificationPermissionChecker {
    fun isPermissionGranted(): Boolean
    fun areNotificationsEnabled(): Boolean
    fun shouldShowRationale(): Boolean
    fun hasAskedBefore(): Boolean
    suspend fun setAskedBefore(asked: Boolean)
    fun getPermissionStatus(apiLevel: Int = Build.VERSION.SDK_INT): NotificationPermissionStatus
}
