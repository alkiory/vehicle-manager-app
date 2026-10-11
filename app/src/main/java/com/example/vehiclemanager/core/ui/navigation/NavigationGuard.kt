package com.example.vehiclemanager.core.ui.navigation

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Interface registered by active form screens to handle exit requests (saving, discarding, closing).
 */
interface FormGuardHandler {
    fun isDirty(): Boolean
    fun onSave(onSuccess: () -> Unit, onFailure: () -> Unit)
    fun onDiscard()
    fun onCloseForm()
}

/**
 * Central registry for active form guards.
 */
@Singleton
class NavigationGuard @Inject constructor() {
    private val _activeGuard = MutableStateFlow<FormGuardHandler?>(null)
    val activeGuard: StateFlow<FormGuardHandler?> = _activeGuard.asStateFlow()

    fun registerGuard(handler: FormGuardHandler) {
        _activeGuard.value = handler
    }

    fun unregisterGuard(handler: FormGuardHandler) {
        if (_activeGuard.value == handler) {
            _activeGuard.value = null
        }
    }

    fun clear() {
        _activeGuard.value = null
    }

    /**
     * Intercepts an exit request (e.g., bottom-nav tab click, back button, Cancel button).
     * Returns true if exit can proceed immediately, or false if exit is held (e.g. dialog shown).
     */
    fun requestExit(
        onShowConfirmation: () -> Unit,
        onAllowed: () -> Unit,
    ): Boolean {
        val guard = _activeGuard.value
        return if (guard == null) {
            onAllowed()
            true
        } else if (!guard.isDirty()) {
            guard.onCloseForm()
            onAllowed()
            true
        } else {
            onShowConfirmation()
            false
        }
    }
}
