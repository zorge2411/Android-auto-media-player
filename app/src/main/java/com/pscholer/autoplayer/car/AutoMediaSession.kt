package com.pscholer.autoplayer.car

import android.content.Intent
import android.content.res.Configuration
import androidx.car.app.AppManager
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.pscholer.autoplayer.car.screens.RootScreen
import com.pscholer.autoplayer.car.surface.VideoSurfaceRenderer
import com.pscholer.autoplayer.di.AppEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AutoMediaSession : Session() {

    private lateinit var surfaceRenderer: VideoSurfaceRenderer

    override fun onCreateScreen(intent: Intent): Screen {
        val entryPoint = EntryPointAccessors.fromApplication(
            carContext.applicationContext,
            AppEntryPoint::class.java
        )

        val playerManager = entryPoint.playerManager()
        surfaceRenderer = VideoSurfaceRenderer(carContext, playerManager)

        lifecycleScope.launch {
            val pkg = carContext.getHostInfo()?.packageName ?: "unknown"
            val version = runCatching {
                carContext.packageManager.getPackageInfo(pkg, 0).versionName ?: "unknown"
            }.getOrDefault("unknown")
            entryPoint.preferencesManager().saveConnectionInfo(pkg, version, carContext.getCarAppApiLevel())
        }

        // The registered Surface sits behind every template rendered by this session.
        carContext.getCarService(AppManager::class.java)
            .setSurfaceCallback(surfaceRenderer)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                playerManager.videoSize.collectLatest { size ->
                    size?.let { surfaceRenderer.onVideoSizeChanged(it) }
                }
            }
        }

        return RootScreen(carContext)
    }

    override fun onCarConfigurationChanged(newConfiguration: Configuration) {
        super.onCarConfigurationChanged(newConfiguration)
        if (::surfaceRenderer.isInitialized) {
            surfaceRenderer.onConfigurationChanged()
        }
    }
}
