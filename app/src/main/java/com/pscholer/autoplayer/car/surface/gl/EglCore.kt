package com.pscholer.autoplayer.car.surface.gl

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.util.Log
import android.view.Surface

/**
 * EGL14 context + window-surface lifecycle helper. Single instance per GLVideoPipeline.
 *
 * Lifecycle:
 *   1. initContext() — creates display, chooses config, creates context (one-time)
 *   2. createWindowSurface(carSurface) — creates window surface against Car App Surface
 *      (recreated on every onSurfaceAvailable; previous one destroyed first)
 *   3. makeCurrent() — binds context+surface to current thread (must be called from
 *      the GL thread before any GLES20 call)
 *   4. swapBuffers() — presents the rendered frame to the Car App Surface
 *   5. destroyWindowSurface() — on Car App onSurfaceDestroyed; KEEPS context alive
 *   6. release() — full teardown (display, context, surface) on pipeline destroy
 *
 * Source: RESEARCH.md Pattern 5; derived from Grafika EglCore.java (Apache 2.0).
 */
class EglCore {
    companion object { private const val TAG = "EglCore" }

    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var context: EGLContext = EGL14.EGL_NO_CONTEXT
    private var config: EGLConfig? = null
    private var windowSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    fun initContext() {
        check(display === EGL14.EGL_NO_DISPLAY) { "initContext called twice" }
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(display !== EGL14.EGL_NO_DISPLAY) {
            "eglGetDisplay returned EGL_NO_DISPLAY (err=0x${EGL14.eglGetError().toString(16)})"
        }
        val v = IntArray(2)
        check(EGL14.eglInitialize(display, v, 0, v, 1)) {
            "eglInitialize failed (err=0x${EGL14.eglGetError().toString(16)})"
        }
        Log.i(TAG, "EGL initialized v${v[0]}.${v[1]}")

        val attribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 0,
            EGL14.EGL_STENCIL_SIZE, 0,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val n = IntArray(1)
        check(EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, n, 0) && n[0] > 0) {
            "eglChooseConfig failed (err=0x${EGL14.eglGetError().toString(16)})"
        }
        config = configs[0]

        val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
        check(context !== EGL14.EGL_NO_CONTEXT) {
            "eglCreateContext failed (err=0x${EGL14.eglGetError().toString(16)})"
        }
    }

    /** Creates a window surface targeting the Car App Surface. Destroys any previous one first. */
    fun createWindowSurface(carAppSurface: Surface) {
        check(display !== EGL14.EGL_NO_DISPLAY) { "initContext not called" }
        destroyWindowSurface()
        val attribs = intArrayOf(EGL14.EGL_NONE)
        windowSurface = EGL14.eglCreateWindowSurface(display, config, carAppSurface, attribs, 0)
        check(windowSurface !== EGL14.EGL_NO_SURFACE) {
            "eglCreateWindowSurface failed on Car App Surface (err=0x${EGL14.eglGetError().toString(16)})"
        }
        Log.i(TAG, "Window surface created on Car App Surface")
    }

    /** Binds context+window surface to the calling thread. */
    fun makeCurrent() {
        check(EGL14.eglMakeCurrent(display, windowSurface, windowSurface, context)) {
            "eglMakeCurrent failed (err=0x${EGL14.eglGetError().toString(16)})"
        }
    }

    fun swapBuffers(): Boolean = EGL14.eglSwapBuffers(display, windowSurface)

    /** Tears down ONLY the window surface; keeps display+context alive for next attach. */
    fun destroyWindowSurface() {
        if (windowSurface !== EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(display, windowSurface)
            windowSurface = EGL14.EGL_NO_SURFACE
            Log.d(TAG, "Window surface destroyed (context preserved)")
        }
    }

    /** Full teardown: window surface + context + display. Idempotent. */
    fun release() {
        destroyWindowSurface()
        if (context !== EGL14.EGL_NO_CONTEXT) {
            EGL14.eglDestroyContext(display, context)
            context = EGL14.EGL_NO_CONTEXT
        }
        if (display !== EGL14.EGL_NO_DISPLAY) {
            EGL14.eglTerminate(display)
            display = EGL14.EGL_NO_DISPLAY
        }
        config = null
        Log.i(TAG, "EglCore released")
    }
}
