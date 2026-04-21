# Phase 3 Plan — Media Aspect Ratio Preservation

**Phase:** 3-aspect-ratio  
**Goal:** Display videos in native aspect ratio without distortion  
**Status:** Implementation + Testing

## Objective

Preserve video aspect ratio when rendering to Android Auto SurfaceContainer. Handle common formats (16:9, 4:3, 21:9, 9:16) and pixel aspect ratio edge cases.

## Success Criteria

- ✅ Video dimensions captured from ExoPlayer onVideoSizeChanged()
- ✅ Aspect ratio calculation includes pixel aspect ratio
- ✅ VideoSurfaceRenderer applies scaling transform
- ✅ Tested on 16:9, 4:3, 21:9, 9:16 formats
- ✅ No distortion or stretching visible
- ✅ Rotation handled correctly on device turn

## Architecture

**Flow:**
1. ExoPlayer playback starts → onVideoSizeChanged() fires
2. Capture width, height, pixelAspectRatio, rotation
3. Calculate target aspect ratio: `(width * pixelAspectRatio) / height`
4. VideoSurfaceRenderer applies transformation to SurfaceTexture
5. Video displays in correct proportions

**Key APIs:**
- `Player.Listener.onVideoSizeChanged()` — captures dimensions
- `VideoSurfaceRenderer` — applies TextureView scaling/transformation
- `StateFlow<VideoSize>` — reactive binding for dimension changes

## Tasks

### Task 3.1: Add VideoSize StateFlow to MediaPlayerManager
**Effort:** Small (15 min)
**Files:** `player/MediaPlayerManager.kt`

**Implementation:**
```kotlin
data class VideoSize(
    val width: Int,
    val height: Int,
    val pixelAspectRatio: Float,
    val rotationDegrees: Int
)

private val _videoSize = MutableStateFlow<VideoSize?>(null)
val videoSize: StateFlow<VideoSize?> = _videoSize.asStateFlow()

// In Player.Listener:
override fun onVideoSizeChanged(videoSize: VideoSize) {
    _videoSize.value = VideoSize(
        width = videoSize.width,
        height = videoSize.height,
        pixelAspectRatio = videoSize.pixelAspectRatio,
        rotationDegrees = videoSize.rotationDegrees
    )
}
```

**Pass Criteria:**
- ✅ VideoSize data class created
- ✅ StateFlow exposed from MediaPlayerManager
- ✅ onVideoSizeChanged() updates StateFlow
- ✅ Compilation succeeds

### Task 3.2: Implement Aspect Ratio Calculator
**Effort:** Small (20 min)
**Files:** Create `util/AspectRatioCalculator.kt` (NEW)

**Implementation:**
```kotlin
object AspectRatioCalculator {
    data class ScaledDimensions(
        val targetWidth: Float,
        val targetHeight: Float,
        val offsetX: Float,
        val offsetY: Float
    )

    fun calculateScaling(
        videoWidth: Int,
        videoHeight: Int,
        pixelAspectRatio: Float,
        containerWidth: Int,
        containerHeight: Int
    ): ScaledDimensions {
        val videoAspectRatio = (videoWidth * pixelAspectRatio) / videoHeight
        val containerAspectRatio = containerWidth.toFloat() / containerHeight

        val (targetWidth, targetHeight) = if (videoAspectRatio > containerAspectRatio) {
            // Video is wider than container — fit to width
            Pair(containerWidth.toFloat(), containerWidth / videoAspectRatio)
        } else {
            // Video is taller than container — fit to height
            Pair(containerHeight * videoAspectRatio, containerHeight.toFloat())
        }

        val offsetX = (containerWidth - targetWidth) / 2
        val offsetY = (containerHeight - targetHeight) / 2

        return ScaledDimensions(targetWidth, targetHeight, offsetX, offsetY)
    }
}
```

**Pass Criteria:**
- ✅ Calculator class created
- ✅ Handles 16:9, 4:3, 21:9, 9:16 test cases
- ✅ Includes pixel aspect ratio multiplication
- ✅ Compilation succeeds

### Task 3.3: Integrate Scaling into VideoSurfaceRenderer
**Effort:** Medium (30-40 min)
**Files:** `car/surface/VideoSurfaceRenderer.kt` (MODIFY)

**Implementation:**
- Add `mediaPlayerManager: MediaPlayerManager` as constructor param
- Collect from `mediaPlayerManager.videoSize` StateFlow
- On VideoSize change, calculate scaled dimensions
- Apply transformation to SurfaceTexture/TextureView
- Handle rotation (if supported by SurfaceContainer)

**Pattern:**
```kotlin
lifecycleScope.launch {
    mediaPlayerManager.videoSize.collectLatest { size ->
        if (size != null && activeSurfaceWidth > 0) {
            val scaled = AspectRatioCalculator.calculateScaling(
                size.width, size.height, size.pixelAspectRatio,
                activeSurfaceWidth, activeSurfaceHeight
            )
            applyScalingTransform(scaled)
        }
    }
}
```

**Pass Criteria:**
- ✅ VideoSize StateFlow collected
- ✅ Scaling applied on every dimension change
- ✅ No jank or visual artifacts during scaling
- ✅ Compilation succeeds

### Task 3.4: Manual Device Testing
**Effort:** Medium (25 min on device)
**Prerequisites:** Phases 1-2 complete, Phase 3 code changes implemented
**Test Matrix:**

| Format | Aspect Ratio | Test Video | Expected Result |
|--------|-------------|-----------|-----------------|
| 16:9 | 1.78:1 | Standard movie | Fills width, small top/bottom padding |
| 4:3 | 1.33:1 | Older content | Fills height, small left/right padding |
| 21:9 | 2.33:1 | Ultrawide | Fills width, large top/bottom padding |
| 9:16 | 0.56:1 | Vertical video | Fills height, large left/right padding |

**Test Steps:**
1. Load 16:9 video → verify fills width, pillarboxed top/bottom
2. Load 4:3 video → verify fills height, letterboxed left/right
3. Load 21:9 video → verify aspect preserved, not stretched
4. Load 9:16 video → verify tall format preserved
5. Switch between formats → verify scaling updates smoothly
6. Rotate device → verify scaling recalculates (if supported)

**Pass Criteria:**
- ✅ All formats display without distortion
- ✅ Scaling smooth and no jank
- ✅ No black areas outside video (or minimal, architecture-constrained)
- ✅ No crashes on aspect ratio changes

### Task 3.5: Edge Case Verification
**Effort:** Small (10 min)
**Checklist:**
- ✅ Very wide videos (21:9) don't exceed container bounds
- ✅ Very tall videos (9:16) don't exceed container bounds
- ✅ Pixel aspect ratio handled (anamorphic video)
- ✅ Rotation info stored but may not be actionable in Car App Library

## Critical Path

1. Task 3.1 (VideoSize StateFlow) → Task 3.3 (integrate scaling)
2. Task 3.2 (AspectRatioCalculator) → Task 3.3
3. All → Task 3.4 (device testing)

## Rollback

If aspect ratio breaks (video stretches):
1. Verify AspectRatioCalculator includes `pixelAspectRatio` multiplication
2. Check SurfaceContainer actual dimensions (may be different from Android display)
3. Verify onVideoSizeChanged() is firing (add logging)
4. Test with known 16:9 video as baseline

## Notes

- SurfaceContainer size may differ from phone display dimensions
- Car head unit display may impose additional constraints (safe area, overlay space)
- Rotation handling depends on Car App Library support (may be no-op)
- Pixel aspect ratio >1.0 is rare but occurs with anamorphic video

## Next Phase

After device testing passes, proceed to Phase 4: Smart Back Button Navigation.
