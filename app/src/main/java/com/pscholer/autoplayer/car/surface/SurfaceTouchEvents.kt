package com.pscholer.autoplayer.car.surface

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges touch input on the car Surface (received by the session-owned [VideoSurfaceRenderer])
 * to screens, which only reach shared objects through AppEntryPoint.
 *
 * The host delivers these callbacks only while the map ActionStrip contains Action.PAN.
 */
@Singleton
class SurfaceTouchEvents @Inject constructor() {
    private val _taps = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val taps: SharedFlow<Unit> = _taps.asSharedFlow()

    fun emit() {
        _taps.tryEmit(Unit)
    }
}
