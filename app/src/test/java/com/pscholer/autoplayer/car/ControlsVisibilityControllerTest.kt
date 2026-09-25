package com.pscholer.autoplayer.car

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ControlsVisibilityControllerTest {

    private fun TestScope.newController() = ControlsVisibilityController(backgroundScope)

    @Test
    fun startsVisible_hidesExactlyAfterTimeout() = runTest {
        val controller = newController()
        assertTrue(controller.visible.value)

        controller.onUserInteraction()
        advanceTimeBy(2_999)
        runCurrent()
        assertTrue(controller.visible.value)

        advanceTimeBy(1)
        runCurrent()
        assertFalse(controller.visible.value)
    }

    @Test
    fun secondInteraction_restartsTimer() = runTest {
        val controller = newController()
        controller.onUserInteraction()
        advanceTimeBy(2_000)
        controller.onUserInteraction()

        advanceTimeBy(2_999)
        runCurrent()
        assertTrue(controller.visible.value)

        advanceTimeBy(1)
        runCurrent()
        assertFalse(controller.visible.value)
    }

    @Test
    fun interactionWhileHidden_revealsImmediately_thenHidesAgain() = runTest {
        val controller = newController()
        controller.onUserInteraction()
        advanceTimeBy(3_000)
        runCurrent()
        assertFalse(controller.visible.value)

        controller.onUserInteraction()
        runCurrent()
        assertTrue(controller.visible.value)

        advanceTimeBy(3_000)
        runCurrent()
        assertFalse(controller.visible.value)
    }

    @Test
    fun cancel_stopsPendingHide() = runTest {
        val controller = newController()
        controller.onUserInteraction()
        controller.cancel()

        advanceTimeBy(10_000)
        runCurrent()
        assertTrue(controller.visible.value)
    }

    @Test
    fun visible_emitsDistinctValuesOnly() = runTest {
        val controller = newController()
        val emissions = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            controller.visible.collect { emissions += it }
        }

        controller.onUserInteraction()
        controller.onUserInteraction()
        advanceTimeBy(1_000)
        controller.onUserInteraction()
        advanceTimeBy(3_000)
        runCurrent()

        assertEquals(listOf(true, false), emissions)
    }
}
