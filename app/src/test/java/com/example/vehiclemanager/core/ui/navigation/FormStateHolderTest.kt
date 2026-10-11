package com.example.vehiclemanager.core.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormStateHolderTest {

    private data class SampleForm(
        val name: String = "",
        val odometer: Long = 0L,
        val isFull: Boolean = false,
    )

    @Test
    fun `form is clean initially`() {
        val initial = SampleForm(name = "Initial", odometer = 100000L)
        val holder = FormStateHolder(initial)

        assertFalse(holder.isDirty)
        assertEquals(initial, holder.currentValue)
        assertEquals(initial, holder.initialValue)
    }

    @Test
    fun `form becomes dirty when value changes`() {
        val initial = SampleForm(name = "Initial", odometer = 100000L)
        val holder = FormStateHolder(initial)

        holder.update(initial.copy(name = "Updated"))

        assertTrue(holder.isDirty)
        assertEquals("Updated", holder.currentValue.name)
    }

    @Test
    fun `form returns to clean when value is reverted to initial`() {
        val initial = SampleForm(name = "Initial", odometer = 100000L)
        val holder = FormStateHolder(initial)

        holder.update(initial.copy(odometer = 105000L))
        assertTrue(holder.isDirty)

        holder.update(initial)
        assertFalse(holder.isDirty)
    }

    @Test
    fun `setInitial updates baseline and resets dirty state`() {
        val initial = SampleForm(name = "Initial", odometer = 100000L)
        val holder = FormStateHolder(initial)

        holder.update(initial.copy(name = "Typed"))
        assertTrue(holder.isDirty)

        val prefilled = SampleForm(name = "Prefilled", odometer = 120000L)
        holder.setInitial(prefilled, updateCurrent = true)

        assertFalse(holder.isDirty)
        assertEquals(prefilled, holder.initialValue)
        assertEquals(prefilled, holder.currentValue)
    }

    @Test
    fun `reset reverts currentValue back to initialValue`() {
        val initial = SampleForm(name = "Initial", odometer = 100000L)
        val holder = FormStateHolder(initial)

        holder.update(initial.copy(isFull = true))
        assertTrue(holder.isDirty)

        holder.reset()

        assertFalse(holder.isDirty)
        assertEquals(initial, holder.currentValue)
    }
}
