package com.example.vehiclemanager.core.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AppScaffoldViewModel @Inject constructor(
    private val formDirtyStateHolder: FormDirtyStateHolder,
    private val navigationGuard: NavigationGuard,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppScaffoldUiState())
    val uiState: StateFlow<AppScaffoldUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            formDirtyStateHolder.dirtyTokens.collect { tokens ->
                _uiState.update { it.copy(hasDirtyForms = tokens.isNotEmpty()) }
            }
        }
    }

    /**
     * Called when the user chooses "Guardar" in the confirmation dialog.
     */
    fun confirmSave() {
        val navigate = _uiState.value.pendingDestination
        val activeGuard = navigationGuard.activeGuard.value
        if (activeGuard != null) {
            activeGuard.onSave(
                onSuccess = {
                    formDirtyStateHolder.clearAll()
                    _uiState.update { it.copy(hasDirtyForms = false, pendingDestination = null) }
                    activeGuard.onCloseForm()
                    navigate?.invoke()
                },
                onFailure = {
                    _uiState.update { it.copy(pendingDestination = null) }
                },
            )
        } else {
            formDirtyStateHolder.clearAll()
            _uiState.update { it.copy(hasDirtyForms = false, pendingDestination = null) }
            navigate?.invoke()
        }
    }

    /**
     * Called when the user chooses "Descartar" in the confirmation dialog.
     */
    fun confirmDiscard() {
        val navigate = _uiState.value.pendingDestination
        val activeGuard = navigationGuard.activeGuard.value
        activeGuard?.onDiscard()
        formDirtyStateHolder.clearAll()
        _uiState.update { it.copy(hasDirtyForms = false, pendingDestination = null) }
        activeGuard?.onCloseForm()
        navigate?.invoke()
    }

    /**
     * Called when the user chooses "Seguir editando" or dismisses the dialog.
     */
    fun dismiss() {
        _uiState.update { it.copy(pendingDestination = null) }
    }

    /**
     * Gate for primary-destination navigation: asks [NavigationGuard] or [FormDirtyStateHolder]
     * whether navigation can proceed directly or must display the unsaved changes dialog.
     */
    fun onPrimaryDestinationSelected(requestNavigation: () -> Unit) {
        val activeGuard = navigationGuard.activeGuard.value
        if (activeGuard != null) {
            if (activeGuard.isDirty()) {
                _uiState.update { it.copy(pendingDestination = requestNavigation) }
            } else {
                activeGuard.onCloseForm()
                requestNavigation()
            }
        } else if (_uiState.value.hasDirtyForms) {
            _uiState.update { it.copy(pendingDestination = requestNavigation) }
        } else {
            requestNavigation()
        }
    }
}

data class AppScaffoldUiState(
    val hasDirtyForms: Boolean = false,
    val pendingDestination: (() -> Unit)? = null,
)
