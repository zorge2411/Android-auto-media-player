package com.pscholer.autoplayer.car

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Auto-hide state machine for playback controls (Phase 1 UI-SPEC, locked decision).
 *
 * [visible] starts true. Every [onUserInteraction] makes it true and restarts the hide timer;
 * after [hideAfterMs] without interaction it becomes false. Position ticks must NOT call
 * [onUserInteraction] — only real user input (surface touches, strip button presses).
 *
 * Pure Kotlin (no Android types) so the timing is unit-testable with virtual time.
 */
class ControlsVisibilityController(
    private val scope: CoroutineScope,
    private val hideAfterMs: Long = 3_000L
) {
    private val _visible = MutableStateFlow(true)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    private var hideJob: Job? = null

    fun onUserInteraction() {
        _visible.value = true
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(hideAfterMs)
            _visible.value = false
        }
    }

    /** Cancels any pending hide; the current visibility is left unchanged. */
    fun cancel() {
        hideJob?.cancel()
        hideJob = null
    }
}
