package com.example.vehiclemanager.core.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * State holder for forms that calculates dirty state by comparing current values
 * against initial values.
 */
class FormStateHolder<T>(initialValue: T) {
    var initialValue: T = initialValue
        private set

    var currentValue: T by mutableStateOf(initialValue)
        private set

    val isDirty: Boolean
        get() = currentValue != initialValue

    fun update(value: T) {
        currentValue = value
    }

    fun setInitial(value: T, updateCurrent: Boolean = true) {
        initialValue = value
        if (updateCurrent) {
            currentValue = value
        }
    }

    fun reset() {
        currentValue = initialValue
    }
}
