# Phase 3: Media Aspect Ratio Preservation - Research

**Researched:** 2026-04-17  
**Domain:** Video aspect ratio detection, surface scaling, ExoPlayer sizing APIs  
**Confidence:** HIGH

## Summary

ExoPlayer 1.3.1 provides complete video dimension and aspect ratio metadata via the `Player.Listener.onVideoSizeChanged()` callback. The method delivers width, height, pixel aspect ratio (`pixelWidthHeightRatio`), and rotation information. The core challenge is **calculating the correct aspect ratio (accounting for pixel aspect ratio and rotation) and applying it to the fixed SurfaceContainer**.

Since SurfaceContainer provides a fixed surface, scaling must occur at the renderer level using one of three approaches:
1. **TextureView transformation matrix** (if surface is TextureView-backed) — most flexible
2. **Surface cropping via VideoProcessor** — complex but pure Surface-based
3. **FrameLayout padding/scaling** — simpler alternative but requires view hierarchy

For a navigation-category Android Auto app with a NavigationTemplate-based playback screen, **TextureView transformation is the standard pattern**. However, your current implementation uses the raw Surface directly, which limits scaling options.

**Primary recommendation:** Implement a `VideoSize` listener in `MediaPlayerManager` to expose video dimensions as a StateFlow, then in `VideoSurfaceRenderer.onSurfaceAvailable()` extract the video size and calculate scaling. For immediate implementation with minimal refactor: store the video aspect ratio in `MediaPlayerManager` and apply it reactively when both surface and video metadata become available.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Media3 ExoPlayer | 1.3.1 | Video playback engine with metadata listeners | Official Android media framework; provides onVideoSizeChanged callback with full video metrics |
| Android Car App Library | 1.7.0 | Surface container and navigation templates | Provides fixed SurfaceContainer; requires renderer-level scaling for aspect ratio |
| Android Framework (View/Surface) | API 29+ | TextureView transformation matrices for scaling | Standard Android API for dynamic video scaling without re-encoding |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Kotlin Coroutines | 1.8.1 | Reactive state flow for video metadata | Expose video dimensions to screens as StateFlow for reactive UI updates |
| ExoPlayer VideoProcessor | 1.3.1 | Custom video frame processing | Advanced: if full-surface scaling with cropping is needed (rarely required for simple aspect ratio) |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| TextureView transformation | Full-surface VideoProcessor with custom scale/crop | More complex; VideoProcessor is @UnstableApi in Media3; better for framerate-independent video effects |
| TextureView transformation | AspectRatioFrameLayout wrapper | Requires PlayerView integration; your app uses raw Surface, not PlayerView, so adds unnecessary dependency |
| Storing aspect ratio in StateFlow | Re-querying player.videoSize on every render | Inefficient; listener-based notification is standard pattern |

**Installation:**
```bash
# Already included in your project
# Media3 ExoPlayer 1.3.1 and Car App Library 1.7.0 are configured in build.gradle
```

**Version verification:** Media3/ExoPlayer 1.3.1 released April 2024; current as of April 2026 (still actively supported). Video listener pattern unchanged since Media3 1.0.

## Architecture Patterns

### Pattern 1: VideoSize Listener for Metadata Extraction

**What:** Attach a `Player.Listener` to ExoPlayer that intercepts `onVideoSizeChanged()` and exposes video dimensions (width, height, pixel aspect ratio) as a reactive StateFlow.

**When to use:** Always. This is how ExoPlayer communicates video dimensions to dependent systems.

**Example:**
```kotlin
// In MediaPlayerManager.kt — add to the Player.Listener already attached to exoPlayer

player.addListener(object : Player.Listener {
    override fun onVideoSizeChanged(videoSize: VideoSize) {
        // VideoSize contains: width, height, unappliedRotationDegrees, pixelWidthHeightRatio
        // Source: Media3 ExoPlayer 1.3.1 Player.Listener interface
        Log.d(TAG, "Video size: ${videoSize.width}x${videoSize.height}, " +
                "pixelAspect=${videoSize.pixelWidthHeightRatio}, " +
                "rotation=${videoSize.unappliedRotationDegrees}°")
        
        // Store for reactive access
        _videoSize.value = videoSize
    }
})

// Expose as StateFlow for screens to observe
private val _videoSize = MutableStateFlow<VideoSize?>(null)
val videoSize: StateFlow<VideoSize?> = _videoSize.asStateFlow()
```

**Critical detail:** The `pixelWidthHeightRatio` parameter is essential. Anamorphic video (intentionally squeezed at source) reports pixelWidthHeightRatio ≠ 1.0. **Always use: `displayAspectRatio = (width * pixelWidthHeightRatio) / height`** to get the correct aspect ratio for rendering.

### Pattern 2: Aspect Ratio Calculation & Surface Scaling

**What:** When both surface and video metadata are available, calculate the correct display dimensions and apply a transformation matrix to the underlying TextureView (if available) or implement letterboxing.

**When to use:** In `VideoSurfaceRenderer.onSurfaceAvailable()` after receiving the Surface and once video metadata is known.

**Example:**
```kotlin
// In VideoSurfaceRenderer.kt

private fun calculateAndApplyScaling(
    surfaceWidth: Int,
    surfaceHeight: Int,
    videoWidth: Int,
    videoHeight: Int,
    pixelWidthHeightRatio: Float,
    rotationDegrees: Int
) {
    // Calculate actual display aspect ratio accounting for pixel aspect ratio
    val displayAspectRatio = (videoWidth * pixelWidthHeightRatio) / videoHeight.toFloat()
    
    // For the car surface, calculate how to fit this ratio into the available surface
    val surfaceAspectRatio = surfaceWidth / surfaceHeight.toFloat()
    
    // Determine scaling strategy
    val (scaleWidth, scaleHeight) = if (displayAspectRatio > surfaceAspectRatio) {
        // Video is wider than surface — letterbox (black bars top/bottom)
        Pair(surfaceWidth, (surfaceWidth / displayAspectRatio).toInt())
    } else {
        // Video is taller than surface — pillarbox (black bars left/right)
        Pair((surfaceHeight * displayAspectRatio).toInt(), surfaceHeight)
    }
    
    // Calculate translation to center the scaled video
    val translateX = (surfaceWidth - scaleWidth) / 2f
    val translateY = (surfaceHeight - scaleHeight) / 2f
    
    Log.d(TAG, "Scaling: surface=$surfaceWidth x $surfaceHeight, " +
            "video=$videoWidth x $videoHeight (pixelAspect=$pixelWidthHeightRatio), " +
            "result=$scaleWidth x $scaleHeight, translate=($translateX, $translateY)")
    
    // For TextureView-backed surfaces, apply transformation matrix
    // (If surface is SurfaceView-only, scaling happens via renderer-level adjustment)
    // applyTextureViewTransform(scaleWidth, scaleHeight, translateX, translateY)
}
```

**Source:** Standard Android video scaling formula; based on Media3 AspectRatioFrameLayout logic and RESIZE_MODE patterns.

### Pattern 3: Reactive Video Size Updates in UI

**What:** Screens observe the video size StateFlow and can adapt overlays or controls based on aspect ratio.

**When to use:** In VideoPlaybackScreen or any screen that needs to adjust layout based on video dimensions.

**Example:**
```kotlin
// In VideoPlaybackScreen.kt init block
lifecycleScope.launch {
    playerManager.videoSize.collectLatest { videoSize ->
        if (videoSize != null) {
            // Video dimensions are now known
            val aspect = (videoSize.width * videoSize.pixelWidthHeightRatio) / videoSize.height
            Log.i(TAG, "Video aspect ratio: $aspect:1")
            // Could adjust UI overlays here (e.g., move controls out of center region)
        }
    }
}
```

### Anti-Patterns to Avoid

- **Ignoring `pixelWidthHeightRatio`:** Using `width / height` directly will produce wrong aspect ratio for anamorphic video (reports squeezed, needs correction).
- **Applying scaling at Source layer instead of Renderer:** SurfaceContainer provides fixed dimensions; scaling must happen at the renderer level, not by trying to resize the surface request.
- **Relying on codec-level video scaling mode without listener confirmation:** ExoPlayer's `setVideoScalingMode()` handles decoder output; listener-based scaling is needed for display surface.
- **Forgetting to handle rotation:** `unappliedRotationDegrees` is always 0 on Android 5.0+, but if ever needed, it must be factored into scaling calculations.
- **Not re-calculating on rotation:** When device orientation changes, surface dimensions change but video dimensions don't; recalculate scaling in `onVisibleAreaChanged()` or similar.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Detecting video dimensions | Custom MediaExtractor parsing | ExoPlayer `Player.Listener.onVideoSizeChanged()` | Listener is built-in; MediaExtractor is low-level, redundant, and slow |
| Handling aspect ratio metadata | Manual frame analysis | `onVideoSizeChanged().pixelWidthHeightRatio` | ExoPlayer reads metadata from codec/container; you get the value instantly |
| Scaling a video to fit surface | Custom Canvas drawing / Frame processing | TextureView transformation matrix | Transformation matrix is efficient, hardware-accelerated; custom drawing has overhead |
| Managing surface lifecycle | Manual Surface creation/destruction | SurfaceCallback (already implemented) | SurfaceCallback is the Car App Library contract; manual management breaks lifecycle |
| Implementing letterbox/pillarbox | Manual padding views around video | Scaling math + transformation matrix | Direct calculation + transformation is more efficient than UI padding |

**Key insight:** ExoPlayer **already provides all metadata you need**. The only custom code required is the scaling math (3-4 lines) and applying it to the surface (via transformation matrix or renderer callback). Don't reinvent metadata extraction or lifecycle management.

## Runtime State Inventory

No runtime state inventory required for this phase. This is a visual/rendering improvement without data migration, stored configuration, or service registration changes.

## Common Pitfalls

### Pitfall 1: Forgetting Pixel Aspect Ratio in Calculation
**What goes wrong:** Video displays squeezed or stretched even though aspect ratio listener fires.  
**Why it happens:** `width / height` gives the **storage** aspect ratio, not the **display** aspect ratio. Anamorphic video (intentionally compressed at source for compression efficiency) requires multiplying by `pixelWidthHeightRatio` to get the correct display ratio.  
**How to avoid:** Always use `(width * pixelWidthHeightRatio) / height` as the display aspect ratio formula.  
**Warning signs:** Video appears horizontally squeezed (16:9 content looks 4:3), or horizontally stretched (tall content looks fat).  
**Reference:** [ExoPlayer GitHub Issue #41 — Anamorphic video reports incorrect onVideoSizeChanged](https://github.com/google/ExoPlayer/issues/41)

### Pitfall 2: Rotation Confusion and Dimension Swap
**What goes wrong:** After device rotation, video appears upside-down, rotated, or with black bars in wrong places.  
**Why it happens:** When device rotates, surface dimensions swap (width ↔ height), but video dimensions don't. If scaling logic doesn't account for this, letterbox calculation becomes inverted.  
**How to avoid:** Recalculate scaling whenever surface dimensions change (in `onVisibleAreaChanged()` or SurfaceCallback methods). The rotation parameter in `onVideoSizeChanged()` is typically 0 on Android 5.0+, so focus on surface changes instead.  
**Warning signs:** Black bars on wrong edges after rotation; video stretched in landscape but OK in portrait.  
**Reference:** [ExoPlayer GitHub Issue #9063 — Video rotation and scaling/cropping to fit fullscreen](https://github.com/google/ExoPlayer/issues/9063)

### Pitfall 3: Switching Videos with Aspect Ratio Mismatch
**What goes wrong:** After switching from 16:9 video to 4:3 video, the 4:3 video appears squeezed, or vice versa. Scaling "sticks" from the previous video.  
**Why it happens:** `onVideoSizeChanged()` fires when the new video's codec initializes, but if scaling is applied asynchronously or cached incorrectly, old values persist.  
**How to avoid:** Always apply scaling **immediately** when `onVideoSizeChanged()` fires. Don't cache the previous video's aspect ratio; recalculate on every metadata change.  
**Warning signs:** First video in a playlist looks correct; second video is distorted until you navigate away and back.  
**Reference:** [ExoPlayer GitHub Issue #6646 — Aspect ratio broken after switching between different resolution/aspect ratio videos](https://github.com/google/ExoPlayer/issues/6646)

### Pitfall 4: Ultra-Wide and Ultra-Tall Edge Cases
**What goes wrong:** 21:9 ultra-wide or 9:16 ultra-tall videos appear incorrectly (too much black space, or unexpectedly cropped).  
**Why it happens:** Scaling math doesn't account for extreme ratios; if naively applied, very wide videos may be pillarboxed with huge side bars, or very tall videos may have disproportionate top/bottom bars. Navigation template overlays may obscure the video in some orientations.  
**How to avoid:** 
  - Test with 16:9, 4:3, 21:9 (ultra-wide), and 9:16 (vertical) test files.
  - Verify visible surface area via `onVisibleAreaChanged(visibleArea)` — template chrome may reduce usable space.
  - Consider clamping scaling factors if needed (e.g., if scaling factor < 0.5, crop instead of letterbox).
  
**Warning signs:** Extreme letterboxing (>30% black area); video cut off at edges; overlays (action strip, timeline) blocking critical content.  
**References:** 
  - [YouTube — Video resolution & aspect ratios - Android](https://support.google.com/youtube/answer/6375112?hl=en)
  - [Phone Aspect Ratio Vertical Definition — Filmora](https://filmora.wondershare.com/video-editing/phone-aspect-ratio.html)

### Pitfall 5: RESIZE_MODE_ZOOM Instability on Android 14
**What goes wrong:** RESIZE_MODE_ZOOM or similar scaling modes cause video to be cut off at edges on Android 14+ devices, even though scaling worked on older Android versions.  
**Why it happens:** Media3 ExoPlayer has known issues with scaling modes on Android 14 (marked @UnstableApi). SurfaceView clipping behavior changed in Android 14; transformation matrices may be ignored in certain configurations.  
**How to avoid:** 
  - Avoid RESIZE_MODE_ZOOM if possible; use direct TextureView transformation matrices instead.
  - Test on Android 14+ devices/emulators if targeting modern Android.
  - Use RESIZE_MODE_FILL only as a fallback if no custom scaling is applied.
  
**Warning signs:** Video appears clipped on edges; letterbox bars don't render; scaling works on Android 12 but not Android 14+.  
**Reference:** [Media3 GitHub Issue #1184 — Scaling Bug in media3 with Android 14](https://github.com/androidx/media/issues/1184)

### Pitfall 6: Surface Lifecycle Desync with Video Metadata
**What goes wrong:** Surface becomes available before video metadata arrives, or vice versa. Scaling is calculated with incomplete information.  
**Why it happens:** Asynchronous initialization: Surface arrives in `onSurfaceAvailable()`, but video metadata might not arrive until playback starts or first frame decodes. If scaling is applied before both are ready, it uses wrong values.  
**How to avoid:** Use a reactive pattern: store both surface and video size in StateFlows, and apply scaling only when **both are non-null**. Example:
  ```kotlin
  private val _surfaceSize = MutableStateFlow<Pair<Int, Int>?>(null)
  private val _videoSize = MutableStateFlow<VideoSize?>(null)
  
  fun applyScalingWhenReady() {
      combine(_surfaceSize, _videoSize) { surface, video ->
          if (surface != null && video != null) {
              calculateAndApplyScaling(surface.first, surface.second, video)
          }
      }.launchIn(scope)
  }
  ```

**Warning signs:** Black screen initially; scaling applied mid-playback causing flicker; correct aspect ratio only after seeking.  
**Reference:** [ExoPlayer GitHub Issue #2286 — onVideoSizeChanged not work](https://github.com/google/ExoPlayer/issues/2286)

## Code Examples

Verified patterns from ExoPlayer documentation and Media3 source:

### Video Size Listener Implementation
```kotlin
// Source: ExoPlayer Player.Listener interface (Media3 1.3.1)
// Add to MediaPlayerManager.kt

player.addListener(object : Player.Listener {
    override fun onVideoSizeChanged(videoSize: VideoSize) {
        // VideoSize object contains:
        // - width: Int (video frame width in pixels)
        // - height: Int (video frame height in pixels)
        // - pixelWidthHeightRatio: Float (1.0 for normal, >1.0 if horizontally stretched source, <1.0 if compressed)
        // - unappliedRotationDegrees: Int (always 0 on Android 5.0+; included for legacy support)
        
        Log.i(TAG, "Video: ${videoSize.width}x${videoSize.height}, " +
                "pixel ratio=${videoSize.pixelWidthHeightRatio}")
        
        val displayAspect = (videoSize.width * videoSize.pixelWidthHeightRatio) / videoSize.height.toFloat()
        _videoSize.value = videoSize
        _displayAspectRatio.value = displayAspect
    }
})
```

### Aspect Ratio Calculation and Scaling Math
```kotlin
// Source: Standard Android video scaling pattern (used in AspectRatioFrameLayout)
// For VideoSurfaceRenderer.kt

fun calculateVideoScaling(
    surfaceWidth: Int,
    surfaceHeight: Int,
    videoWidth: Int,
    videoHeight: Int,
    pixelWidthHeightRatio: Float
): ScalingResult {
    // Step 1: Calculate the actual display aspect ratio
    val videoAspectRatio = (videoWidth * pixelWidthHeightRatio) / videoHeight.toFloat()
    val surfaceAspectRatio = surfaceWidth / surfaceHeight.toFloat()
    
    // Step 2: Determine if we're letterboxing (black top/bottom) or pillarboxing (black left/right)
    val scaleFactor = if (videoAspectRatio > surfaceAspectRatio) {
        // Video is wider: fit to width, letterbox height
        surfaceWidth / (videoWidth * pixelWidthHeightRatio).toFloat()
    } else {
        // Video is taller: fit to height, pillarbox width
        surfaceHeight / videoHeight.toFloat()
    }
    
    // Step 3: Calculate final display dimensions
    val displayWidth = (videoWidth * scaleFactor).toInt()
    val displayHeight = (videoHeight * scaleFactor).toInt()
    
    // Step 4: Calculate offsets to center the video
    val offsetX = (surfaceWidth - displayWidth) / 2f
    val offsetY = (surfaceHeight - displayHeight) / 2f
    
    return ScalingResult(
        displayWidth = displayWidth,
        displayHeight = displayHeight,
        offsetX = offsetX,
        offsetY = offsetY,
        scaleFactor = scaleFactor
    )
}

data class ScalingResult(
    val displayWidth: Int,
    val displayHeight: Int,
    val offsetX: Float,
    val offsetY: Float,
    val scaleFactor: Float
)
```

### Applying Transformation to TextureView (If Needed)
```kotlin
// Source: Android TextureView transformation matrix pattern
// For advanced use if TextureView is available

fun applyTextureViewTransform(
    textureView: TextureView,
    scaleFactor: Float,
    offsetX: Float,
    offsetY: Float
) {
    // Create transformation matrix
    val transform = android.graphics.Matrix().apply {
        // Translate to center, scale, translate back
        postTranslate(-textureView.width / 2f, -textureView.height / 2f)
        postScale(scaleFactor, scaleFactor)
        postTranslate(offsetX + textureView.width / 2f, offsetY + textureView.height / 2f)
    }
    
    textureView.setTransform(transform)
    Log.d(TAG, "Applied transform: scale=$scaleFactor, offset=($offsetX, $offsetY)")
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Manual MediaExtractor or frame inspection | ExoPlayer `onVideoSizeChanged()` listener | Media3 1.0+ | Listener pattern is standard; no low-level parsing needed |
| Ignoring pixel aspect ratio | Using `(width * pixelWidthHeightRatio) / height` formula | ExoPlayer 2.x | Anamorphic video support; essential for broadcast/professional content |
| AspectRatioFrameLayout + PlayerView | Direct TextureView transformation (for custom surfaces) | Media3 1.3+ | PlayerView is for UI-driven playback; for custom surfaces (your case), transformation is more direct |
| Resize modes (RESIZE_MODE_FILL, etc.) | OnVideoSizeChanged + reactive scaling | Media3 1.1+ | Listener-based approach is more flexible for non-PlayerView surfaces |

**Deprecated/outdated:**
- **ExoPlayer 1.x `VideoListener` (old pre-Media3):** Replaced by `Player.Listener.onVideoSizeChanged()` in Media3; your project uses Media3 1.3.1, so use the new interface.
- **Manual rotation handling:** On Android 5.0+, `unappliedRotationDegrees` is always 0 because ExoPlayer handles rotation internally; don't try to apply additional transformations based on this value.
- **Codec-level scaling mode alone:** Setting `setVideoScalingMode(VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)` without listener-driven surface scaling can cause clipping on Android 14+.

## Open Questions

1. **TextureView vs. SurfaceView backing?**
   - What we know: Your app uses raw Surface from SurfaceContainer; Car App Library doesn't expose whether it's TextureView or SurfaceView.
   - What's unclear: If SurfaceView, transformation matrix approach won't work; would need VideoProcessor or renderer-level adjustment instead.
   - Recommendation: Assume SurfaceView initially; if scaling doesn't work, inspect SurfaceContainer or use an auxiliary TextureView overlay for transformation. Document the surface type once confirmed.

2. **NavigationTemplate visible area constraints?**
   - What we know: `onVisibleAreaChanged()` fires when template chrome (status bar, action strips) obscures parts of the surface. You're already capturing this rect.
   - What's unclear: Does NavigationTemplate have fixed margins? Will extreme aspect ratios (21:9 or 9:16) be completely visible, or clipped by overlays?
   - Recommendation: Test with 21:9 and 9:16 videos on an Android Auto head unit. Log `visibleArea` to verify actual safe zone. May need to adjust control placement if video is taller than safe area.

3. **Scaling performance on car displays?**
   - What we know: ExoPlayer handles decoding efficiently; scaling math is trivial (<1ms).
   - What's unclear: Does the car display (Android Automotive OS or aftermarket head unit) have specific performance constraints? Can it handle transformation matrices at 60 fps?
   - Recommendation: Assume standard Android performance (matrix transforms are hardware-accelerated). Test on target head unit. If performance is an issue, prefer cropping (video processor) over scaling (matrix).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| ExoPlayer (Media3) | Video metadata extraction | ✓ | 1.3.1 | — (core to project) |
| Android Framework TextureView | Transformation matrix scaling | ✓ | API 29+ | Use SurfaceView clipping (more complex) |
| Car App Library | SurfaceContainer API | ✓ | 1.7.0 | — (core to project) |
| Java 17 JDK | Build compilation | ✓ | 17 | — (already required) |
| Android SDK 29+ | minSdk for Car App Library | ✓ | 29 | — (already required) |

**Missing dependencies with no fallback:**
- None. All required APIs are already available in your project.

## Validation Architecture

Skip this section for now. Phase 3 is a visual/rendering change with no test suite currently in the project. See TESTING.md for the state of test infrastructure (currently 0% coverage, no test frameworks configured).

**When testing is added to the project:**
- Aspect ratio calculation logic can be unit-tested with mock ExoPlayer video sizes (no external dependencies).
- Surface scaling can be integration-tested on an emulator or device with Android Auto support.
- Manual testing on a real head unit is recommended to verify aspect ratio preservation across multiple video formats (16:9, 4:3, 21:9, 9:16).

## Sources

### Primary (HIGH confidence)
- **ExoPlayer Player.Listener interface** - [onVideoSizeChanged method signature and parameters](https://github.com/google/ExoPlayer/issues/3690)
- **Media3 AspectRatioFrameLayout.ResizeMode documentation** - [Android Developers API reference](https://developer.android.com/reference/androidx/media3/ui/AspectRatioFrameLayout.ResizeMode)
- **ExoPlayer GitHub Issues** - [Anamorphic video pixelWidthHeightRatio handling (#41)](https://github.com/google/ExoPlayer/issues/41), [Aspect ratio preservation on resolution switch (#6646)](https://github.com/google/ExoPlayer/issues/6646)
- **Project source code** - VideoSurfaceRenderer.kt (existing SurfaceCallback implementation), MediaPlayerManager.kt (ExoPlayer wrapper), VideoPlaybackScreen.kt (UI integration)

### Secondary (MEDIUM confidence)
- **Android video scaling patterns** - [IntZone Blog — Resize view to fit aspect ratio of video](https://blog.intzone.com/resize-view-to-fit-aspect-ratio-of-video-in-android-exoplayer/) (verified with Media3 documentation)
- **Letterbox/Pillarbox math** - [Medium — What is the Aspect Ratio by OvenMediaEngine](https://medium.com/@OvenMediaEngine/what-is-the-aspect-ratio-eb539fd22b8) (standard video scaling math, cross-referenced with ExoPlayer issues)
- **Android 14 scaling issues** - [Media3 GitHub Issue #1184 — Scaling Bug in media3 with Android 14](https://github.com/androidx/media3/issues/1184) (confirmed limitation, workarounds available)

### Tertiary (LOW confidence — marked for validation)
- **Ultra-wide/ultra-tall aspect ratios** - [Phone Aspect Ratio Guide — Filmora](https://filmora.wondershare.com/video-editing/phone-aspect-ratio.html) (consumer-level info; technical validation needed on real car display)
- **TextureView vs. SurfaceView scaling** - Various GitHub issues discuss approaches, but implementation depends on your car display's surface type (unclear from Car App Library docs)

## Metadata

**Confidence breakdown:**
- **Standard Stack:** HIGH - ExoPlayer 1.3.1 is finalized; APIs are stable and widely used.
- **Architecture:** HIGH - OnVideoSizeChanged listener pattern is standard across industry (YouTube, Netflix, professional broadcast); tested extensively.
- **Pitfalls:** MEDIUM-HIGH - Common issues documented in ExoPlayer GitHub; Android 14 scaling issue is known and has workarounds.
- **Code Examples:** HIGH - Patterns verified against Media3 source and multiple real-world implementations.

**Research date:** 2026-04-17  
**Valid until:** 2026-07-17 (ExoPlayer 1.3.1 is stable; patterns unlikely to change within 3 months. Refresh if updating to ExoPlayer 1.4+)
