package com.pscholer.autoplayer.car.screens

import android.net.Uri
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MessageInfo
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.pscholer.autoplayer.R
import com.pscholer.autoplayer.car.ControlsVisibilityController
import com.pscholer.autoplayer.data.models.MediaItem
import com.pscholer.autoplayer.data.models.Favorite
import com.pscholer.autoplayer.di.AppEntryPoint
import com.pscholer.autoplayer.player.MediaPlayerManager
import com.pscholer.autoplayer.util.TimeFormatter
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Playback screen — the visual layer during video playback.
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  Car display                                                    │
 * │  ┌───────────────────────────────────────────────────────────┐  │
 * │  │  ExoPlayer video (rendered to Surface by VideoSurface-    │  │
 * │  │  Renderer — sits BEHIND all Car App Library templates)    │  │
 * │  │                                                           │  │
 * │  │  NavigationTemplate overlays minimal controls on top:     │  │
 * │  │    • ActionStrip (right edge): ⏮ ⏯ ⏭ icons              │  │
 * │  │    • MapActionStrip (top-right): [PAN] ♡ Favorite ✕ Stop  │  │
 * │  │    • MessageInfo: HH:MM:SS / HH:MM:SS timeline            │  │
 * │  │                                                           │  │
 * │  └───────────────────────────────────────────────────────────┘  │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * NavigationTemplate is the correct choice here because:
 * 1. It exposes the maximum visible Surface area of any template type.
 * 2. It provides ActionStrip + MapActionStrip for media controls.
 * 3. Its "map" region is fully transparent, letting video show through.
 *
 * The screen calls invalidate() whenever play/pause state changes so the
 * correct icon is displayed on the action strip.
 *
 * Auto-hide (Phase 1): the timeline is omitted from the template 3 s after the last user
 * interaction and returns on a surface touch or any button press. Both ActionStrips are
 * always set — NavigationTemplate.Builder.build() throws without an ActionStrip, and the host
 * already conceals the strips when idle. Action.PAN must stay in the map strip: without it
 * the host delivers no SurfaceCallback touch events. On touchscreens the host does not
 * display the PAN button.
 */
class VideoPlaybackScreen(
    carContext: CarContext,
    private val mediaItem: MediaItem
) : Screen(carContext) {

    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(
            carContext.applicationContext,
            AppEntryPoint::class.java
        )
    }
    private val playerManager: MediaPlayerManager by lazy { entryPoint.playerManager() }
    private val playbackRepository by lazy { entryPoint.playbackRepository() }
    private val favoriteRepository by lazy { entryPoint.favoriteRepository() }

    private var currentPositionMs = 0L
    private var currentDurationMs = 0L
    private var lastFormattedPosition = ""
    private var isFavorite = false
    private var lastIsPlaying: Boolean? = null
    private var controlsVisible = true
    private val controls = ControlsVisibilityController(lifecycleScope)

    init {
        // Back-navigation contract (Phase 4 — D-02, D-03):
        // The Car App Library's screen lifecycle fires onStop() when this screen is
        // popped (hardware back, Action.BACK, or screenManager.pop() from elsewhere).
        // We hook onStop to call playerManager.stop() — this satisfies FEAT-2-AC1
        // (back from video stops playback and returns to Browse) without an explicit
        // OnBackPressedCallback override. Do NOT call screenManager.pop() inside
        // onStop — that causes re-entrant double-pop / stack corruption (RESEARCH
        // Pitfall 2).
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                controls.cancel()
                playerManager.stop()
            }
        })

        // Start playback as soon as this screen is pushed
        lifecycleScope.launch {
            mediaItem.streamUrl?.let { url ->
                if (mediaItem.headers.isNotEmpty()) {
                    playerManager.playWithHeaders(
                        uri = url.toUri(),
                        headers = mediaItem.headers,
                        title = mediaItem.title,
                        source = mediaItem.source,
                        mediaId = mediaItem.id
                    )
                } else {
                    playerManager.play(
                        uri = url.toUri(),
                        title = mediaItem.title,
                        mimeType = mediaItem.mimeType,
                        source = mediaItem.source,
                        mediaId = mediaItem.id
                    )
                }

                playbackRepository.load(mediaItem.id)
                    ?.takeIf { it.isResumable() }
                    ?.let { playerManager.seekTo(it.positionMs) }
            }
            // Starts the initial 3 s auto-hide countdown
            controls.onUserInteraction()
        }

        lifecycleScope.launch {
            controls.visible.collect { visible ->
                controlsVisible = visible
                invalidate()
            }
        }

        // Surface touches (tap / drag / fling) reveal the controls and restart the timer
        lifecycleScope.launch {
            entryPoint.surfaceTouchEvents().taps.collect { controls.onUserInteraction() }
        }

        // Throttle invalidate() to once per second of wall-clock progress — the Car App
        // host IPC is expensive and can cause buffering if we invalidate on every tick.
        // While the timeline is hidden, position ticks do not invalidate at all; play/pause
        // changes always do, so the icon is never stale.
        lifecycleScope.launch {
            kotlinx.coroutines.flow.combine(
                playerManager.isPlaying,
                playerManager.positionMs,
                playerManager.durationMs
            ) { playing, pos, dur ->
                Triple(playing, pos, dur)
            }.collectLatest { (playing, pos, dur) ->
                val newTime = TimeFormatter.formatMillis(pos)
                val timelineChanged = newTime != lastFormattedPosition || currentDurationMs != dur
                val playingChanged = playing != lastIsPlaying
                lastFormattedPosition = newTime
                lastIsPlaying = playing
                currentPositionMs = pos
                currentDurationMs = dur
                if (playingChanged || (controlsVisible && timelineChanged)) {
                    invalidate()
                }
            }
        }

        // Check if video is favorited
        lifecycleScope.launch {
            isFavorite = favoriteRepository.isFavorite(mediaItem.id)
            invalidate()
        }

        // Handle playback errors safely
        lifecycleScope.launch {
            playerManager.playbackState.collectLatest { state ->
                if (state is MediaPlayerManager.PlaybackState.Error) {
                    androidx.car.app.CarToast.makeText(
                        carContext, 
                        "Playback error: ${state.message}", 
                        androidx.car.app.CarToast.LENGTH_LONG
                    ).show()
                    playerManager.stop()
                    screenManager.pop()
                }
            }
        }
    }

    override fun onGetTemplate(): Template {
        val isPlaying = playerManager.isPlaying.value
        val timelineText = buildTimelineText()

        // ── Right-edge action strip: primary playback controls ────────────────
        val playbackStrip = ActionStrip.Builder()
            .addAction(buildAction(R.drawable.ic_seek_back) { playerManager.seekBack() })
            .addAction(
                buildAction(
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                ) { playerManager.togglePlayPause() }
            )
            .addAction(buildAction(R.drawable.ic_seek_forward) { playerManager.seekForward() })
            .build()

        // ── Top-right map strip: secondary / navigation controls ──────────────
        val mapStrip = ActionStrip.Builder()
            // Required for SurfaceCallback touch events (tap-to-reveal); hidden on touchscreens
            .addAction(Action.PAN)
            .addAction(
                Action.Builder()
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(
                                carContext,
                                if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite
                            )
                        ).build()
                    )
                    .setOnClickListener {
                        controls.onUserInteraction()
                        lifecycleScope.launch {
                            if (isFavorite) {
                                favoriteRepository.remove(mediaItem.id)
                                isFavorite = false
                            } else {
                                favoriteRepository.add(
                                    Favorite(
                                        mediaId = mediaItem.id,
                                        source = mediaItem.source,
                                        mediaTitle = mediaItem.title
                                    )
                                )
                                isFavorite = true
                            }
                            invalidate()
                        }
                    }
                    .build()
            )
            // Stop button (Phase 4 — D-07, D-08): explicit stop-and-exit. Pops ONE
            // level (back to BrowseScreen, not RootScreen) so the user returns to the
            // browse context they came from. screenManager.pop() here triggers onStop
            // which already calls playerManager.stop() — the explicit stop() call in
            // the click listener is redundant-but-defensive (intentional belt+braces).
            .addAction(
                Action.Builder()
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, R.drawable.ic_close)
                        ).build()
                    )
                    .setOnClickListener {
                        controls.onUserInteraction()
                        playerManager.stop()
                        screenManager.pop()
                    }
                    .build()
            )
            .build()

        // ActionStrip is mandatory in every state (build() throws without one); hiding the
        // controls only drops the timeline.
        val builder = NavigationTemplate.Builder()
            .setActionStrip(playbackStrip)
            .setMapActionStrip(mapStrip)
        if (controlsVisible) {
            builder.setNavigationInfo(MessageInfo.Builder(timelineText).build())
        }
        return builder.build()
    }

    private fun buildAction(iconRes: Int, onClick: () -> Unit): Action =
        Action.Builder()
            .setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(carContext, iconRes)
                ).build()
            )
            .setOnClickListener {
                controls.onUserInteraction()
                onClick()
            }
            .build()

    private fun buildTimelineText(): String =
        TimeFormatter.formatTimeline(currentPositionMs, currentDurationMs)
}
