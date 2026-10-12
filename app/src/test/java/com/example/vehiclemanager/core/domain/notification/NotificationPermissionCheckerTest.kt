package com.example.vehiclemanager.core.domain.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPermissionCheckerTest {

    private class FakeNotificationPermissionChecker(
        var isGranted: Boolean = false,
        var notificationsEnabled: Boolean = true,
        var showRationale: Boolean = false,
        var askedBefore: Boolean = false,
    ) : NotificationPermissionChecker {

        override fun isPermissionGranted(): Boolean = isGranted
        override fun areNotificationsEnabled(): Boolean = notificationsEnabled
        override fun shouldShowRationale(): Boolean = showRationale
        override fun hasAskedBefore(): Boolean = askedBefore

        override suspend fun setAskedBefore(asked: Boolean) {
            askedBefore = asked
        }

        override fun getPermissionStatus(apiLevel: Int): NotificationPermissionStatus {
            if (!areNotificationsEnabled()) {
                return NotificationPermissionStatus.APP_NOTIFICATIONS_DISABLED
            }
            if (apiLevel < 33) {
                return NotificationPermissionStatus.GRANTED
            }
            if (isPermissionGranted()) {
                return NotificationPermissionStatus.GRANTED
            }
            if (shouldShowRationale()) {
                return NotificationPermissionStatus.RATIONALE_NEEDED
            }
            if (!hasAskedBefore()) {
                return NotificationPermissionStatus.NOT_ASKED
            }
            return NotificationPermissionStatus.PERMANENTLY_DENIED
        }
    }

    @Test
    fun getPermissionStatus_appNotificationsDisabled_returnsAppNotificationsDisabled() {
        val checker = FakeNotificationPermissionChecker(notificationsEnabled = false)
        assertEquals(NotificationPermissionStatus.APP_NOTIFICATIONS_DISABLED, checker.getPermissionStatus(33))
        assertEquals(NotificationPermissionStatus.APP_NOTIFICATIONS_DISABLED, checker.getPermissionStatus(28))
    }

    @Test
    fun getPermissionStatus_belowApi33_returnsGranted() {
        val checker = FakeNotificationPermissionChecker(isGranted = false, notificationsEnabled = true)
        assertEquals(NotificationPermissionStatus.GRANTED, checker.getPermissionStatus(31))
    }

    @Test
    fun getPermissionStatus_api33AndPermissionGranted_returnsGranted() {
        val checker = FakeNotificationPermissionChecker(isGranted = true, notificationsEnabled = true)
        assertEquals(NotificationPermissionStatus.GRANTED, checker.getPermissionStatus(33))
    }

    @Test
    fun getPermissionStatus_api33NotAsked_returnsNotAsked() {
        val checker = FakeNotificationPermissionChecker(
            isGranted = false,
            notificationsEnabled = true,
            showRationale = false,
            askedBefore = false,
        )
        assertEquals(NotificationPermissionStatus.NOT_ASKED, checker.getPermissionStatus(33))
    }

    @Test
    fun getPermissionStatus_api33ShowRationale_returnsRationaleNeeded() {
        val checker = FakeNotificationPermissionChecker(
            isGranted = false,
            notificationsEnabled = true,
            showRationale = true,
            askedBefore = true,
        )
        assertEquals(NotificationPermissionStatus.RATIONALE_NEEDED, checker.getPermissionStatus(33))
    }

    @Test
    fun getPermissionStatus_api33AskedBeforeAndNoRationale_returnsPermanentlyDenied() {
        val checker = FakeNotificationPermissionChecker(
            isGranted = false,
            notificationsEnabled = true,
            showRationale = false,
            askedBefore = true,
        )
        assertEquals(NotificationPermissionStatus.PERMANENTLY_DENIED, checker.getPermissionStatus(33))
    }
}
