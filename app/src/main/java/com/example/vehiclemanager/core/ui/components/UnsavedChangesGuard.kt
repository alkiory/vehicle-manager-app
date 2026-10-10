package com.example.vehiclemanager.core.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.vehiclemanager.R

/** Holds whether the unsaved-changes confirmation dialog is visible and stores pending actions. */
class UnsavedChangesState {
    var isConfirmationVisible: Boolean by mutableStateOf(false)
        private set

    var pendingAction: (() -> Unit)? = null
        private set

    fun requestConfirmation(onPending: (() -> Unit)? = null) {
        pendingAction = onPending
        isConfirmationVisible = true
    }

    fun dismiss() {
        isConfirmationVisible = false
        pendingAction = null
    }

    fun runPendingAction() {
        val action = pendingAction
        dismiss()
        action?.invoke()
    }
}

/** Remembers an [UnsavedChangesState] across recompositions and rotation. */
@Composable
fun rememberUnsavedChangesState(): UnsavedChangesState {
    val state = remember { UnsavedChangesState() }
    var isVisibleSaveable by rememberSaveable { mutableStateOf(false) }

    if (isVisibleSaveable != state.isConfirmationVisible) {
        if (isVisibleSaveable) {
            state.requestConfirmation(state.pendingAction)
        } else {
            state.dismiss()
        }
    }

    return state
}

/**
 * Shared guard for form screens: shows the unsaved-changes confirmation dialog
 * (Guardar / Descartar / Seguir editando) when requested and intercepts the system back
 * gesture while the form is dirty.
 */
@Composable
fun UnsavedChangesGuard(
    state: UnsavedChangesState,
    isDirty: Boolean,
    onSave: (() -> Unit)? = null,
    onDiscard: () -> Unit,
) {
    if (state.isConfirmationVisible) {
        AlertDialog(
            onDismissRequest = state::dismiss,
            title = { Text(text = stringResource(R.string.unsaved_changes_title)) },
            text = { Text(text = stringResource(R.string.unsaved_changes_message)) },
            confirmButton = {
                if (onSave != null) {
                    Button(onClick = {
                        val pending = state.pendingAction
                        state.dismiss()
                        onSave()
                        pending?.invoke()
                    }) {
                        Text(text = stringResource(R.string.unsaved_changes_save))
                    }
                } else {
                    Button(onClick = {
                        state.runPendingAction()
                        onDiscard()
                    }) {
                        Text(text = stringResource(R.string.unsaved_changes_discard))
                    }
                }
            },
            dismissButton = {
                if (onSave != null) {
                    Row {
                        TextButton(onClick = {
                            state.runPendingAction()
                            onDiscard()
                        }) {
                            Text(text = stringResource(R.string.unsaved_changes_discard))
                        }
                        TextButton(onClick = state::dismiss) {
                            Text(text = stringResource(R.string.unsaved_changes_keep))
                        }
                    }
                } else {
                    TextButton(onClick = state::dismiss) {
                        Text(text = stringResource(R.string.unsaved_changes_keep))
                    }
                }
            },
        )
    }
    if (isDirty) {
        BackHandler {
            state.requestConfirmation()
        }
    }
}
