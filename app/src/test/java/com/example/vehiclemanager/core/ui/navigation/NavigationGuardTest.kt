package com.example.vehiclemanager.core.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationGuardTest {

    private class TestGuardHandler(
        var dirty: Boolean = false,
        var saveSuccess: Boolean = true,
    ) : FormGuardHandler {
        var saveCalled = false
        var discardCalled = false
        var closeCalled = false

        override fun isDirty(): Boolean = dirty

        override fun onSave(onSuccess: () -> Unit, onFailure: () -> Unit) {
            saveCalled = true
            if (saveSuccess) onSuccess() else onFailure()
        }

        override fun onDiscard() {
            discardCalled = true
        }

        override fun onCloseForm() {
            closeCalled = true
        }
    }

    @Test
    fun `requestExit allows navigation when no guard is registered`() {
        val navigationGuard = NavigationGuard()
        var allowedExecuted = false
        var dialogShown = false

        val result = navigationGuard.requestExit(
            onShowConfirmation = { dialogShown = true },
            onAllowed = { allowedExecuted = true },
        )

        assertTrue(result)
        assertTrue(allowedExecuted)
        assertFalse(dialogShown)
    }

    @Test
    fun `requestExit closes form and allows navigation when guard is clean`() {
        val navigationGuard = NavigationGuard()
        val handler = TestGuardHandler(dirty = false)
        navigationGuard.registerGuard(handler)

        var allowedExecuted = false
        var dialogShown = false

        val result = navigationGuard.requestExit(
            onShowConfirmation = { dialogShown = true },
            onAllowed = { allowedExecuted = true },
        )

        assertTrue(result)
        assertTrue(allowedExecuted)
        assertTrue(handler.closeCalled)
        assertFalse(dialogShown)
    }

    @Test
    fun `requestExit holds navigation and shows confirmation when guard is dirty`() {
        val navigationGuard = NavigationGuard()
        val handler = TestGuardHandler(dirty = true)
        navigationGuard.registerGuard(handler)

        var allowedExecuted = false
        var dialogShown = false

        val result = navigationGuard.requestExit(
            onShowConfirmation = { dialogShown = true },
            onAllowed = { allowedExecuted = true },
        )

        assertFalse(result)
        assertFalse(allowedExecuted)
        assertFalse(handler.closeCalled)
        assertTrue(dialogShown)
    }

    @Test
    fun `register and unregister guard updates activeGuard flow`() {
        val navigationGuard = NavigationGuard()
        val handler = TestGuardHandler()

        navigationGuard.registerGuard(handler)
        assertSame(handler, navigationGuard.activeGuard.value)

        navigationGuard.unregisterGuard(handler)
        assertNull(navigationGuard.activeGuard.value)
    }
}
