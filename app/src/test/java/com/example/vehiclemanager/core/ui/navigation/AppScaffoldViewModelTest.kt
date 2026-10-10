package com.example.vehiclemanager.core.ui.navigation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppScaffoldViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var formDirtyStateHolder: FormDirtyStateHolder
    private lateinit var navigationGuard: NavigationGuard
    private lateinit var viewModel: AppScaffoldViewModel

    private class MockFormGuardHandler(
        var dirty: Boolean = false,
        var saveShouldSucceed: Boolean = true,
    ) : FormGuardHandler {
        var saveCalled = false
        var discardCalled = false
        var closeCalled = false

        override fun isDirty(): Boolean = dirty

        override fun onSave(onSuccess: () -> Unit, onFailure: () -> Unit) {
            saveCalled = true
            if (saveShouldSucceed) onSuccess() else onFailure()
        }

        override fun onDiscard() {
            discardCalled = true
        }

        override fun onCloseForm() {
            closeCalled = true
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        formDirtyStateHolder = FormDirtyStateHolder()
        navigationGuard = NavigationGuard()
        viewModel = AppScaffoldViewModel(formDirtyStateHolder, navigationGuard)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `navigation proceeds immediately when no dirty form exists`() {
        var navigated = false

        viewModel.onPrimaryDestinationSelected { navigated = true }

        assertTrue(navigated)
        assertNull(viewModel.uiState.value.pendingDestination)
    }

    @Test
    fun `navigation is held when active guard is dirty`() {
        val handler = MockFormGuardHandler(dirty = true)
        navigationGuard.registerGuard(handler)

        var navigated = false
        viewModel.onPrimaryDestinationSelected { navigated = true }

        assertFalse(navigated)
        assertNotNull(viewModel.uiState.value.pendingDestination)
    }

    @Test
    fun `confirmSave executes save, closes form and navigates on success`() {
        val handler = MockFormGuardHandler(dirty = true, saveShouldSucceed = true)
        navigationGuard.registerGuard(handler)

        var navigated = false
        viewModel.onPrimaryDestinationSelected { navigated = true }

        viewModel.confirmSave()

        assertTrue(handler.saveCalled)
        assertTrue(handler.closeCalled)
        assertTrue(navigated)
        assertNull(viewModel.uiState.value.pendingDestination)
    }

    @Test
    fun `confirmSave cancels navigation and stays on form if save fails`() {
        val handler = MockFormGuardHandler(dirty = true, saveShouldSucceed = false)
        navigationGuard.registerGuard(handler)

        var navigated = false
        viewModel.onPrimaryDestinationSelected { navigated = true }

        viewModel.confirmSave()

        assertTrue(handler.saveCalled)
        assertFalse(handler.closeCalled)
        assertFalse(navigated)
        assertNull(viewModel.uiState.value.pendingDestination)
    }

    @Test
    fun `confirmDiscard discards draft, closes form and navigates`() {
        val handler = MockFormGuardHandler(dirty = true)
        navigationGuard.registerGuard(handler)

        var navigated = false
        viewModel.onPrimaryDestinationSelected { navigated = true }

        viewModel.confirmDiscard()

        assertTrue(handler.discardCalled)
        assertTrue(handler.closeCalled)
        assertTrue(navigated)
        assertNull(viewModel.uiState.value.pendingDestination)
    }

    @Test
    fun `dismiss cancels pending navigation without modifying form`() {
        val handler = MockFormGuardHandler(dirty = true)
        navigationGuard.registerGuard(handler)

        var navigated = false
        viewModel.onPrimaryDestinationSelected { navigated = true }

        viewModel.dismiss()

        assertFalse(handler.saveCalled)
        assertFalse(handler.discardCalled)
        assertFalse(handler.closeCalled)
        assertFalse(navigated)
        assertNull(viewModel.uiState.value.pendingDestination)
    }
}
