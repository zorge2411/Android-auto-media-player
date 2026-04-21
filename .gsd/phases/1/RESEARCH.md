# Research - Phase 1: Video Scaling & Aspect Ratios

## Summary
The goal of this phase is to ensure video rendering on Android Auto head units handles aspect ratios correctly, supporting both "Fit" (letterbox) and "Fill" (crop to fill) modes.

## Technical Findings

### Media3 Presentation Layouts
The `androidx.media3.effect.Presentation` class supports the following modes which are compatible with raw Surfaces:
- `LAYOUT_SCALE_TO_FIT`: Maps to standard letterboxing.
- `LAYOUT_SCALE_TO_FIT_WITH_CROPPING`: Maps to "Fill" mode where the video is zoomed to cover the entire surface while preserving aspect ratio.
- `LAYOUT_STRETCH_TO_FIT`: Maps to "Stretch" mode where aspect ratio is ignored.

### ExoPlayer Video Effects API
To apply these, we use `player.setVideoEffects(List<Effect>)`. 
**CRITICAL**: This must be called *before* `player.prepare()` if we want the pipeline to initialize with the correct buffers, or it will cause a restart of the video processor.

### Surface Management in Android Auto
The `Surface` from `CarAppService` is a raw hardware surface. Invariants:
1. It is created once per session but can be reused.
2. Dimensions are provided by the car host.
3. Aspect ratio of the car screen varies wildly (Portrait, Ultra-Wide, Standard 4:3).

## Recommendations
1. **Refactor MediaPlayerManager**: Add a `ScalingMode` enum and a method to update it.
2. **Dynamic Application**: Re-apply the `Presentation` effect in `setOutputSize` and whenever `ScalingMode` changes.
3. **UI Feedback**: Although not strictly required by the TODO, adding a toggle in `VideoPlaybackScreen` provides the best user experience.

## Verification Strategy
- **Simulated Screens**: Use various screen sizes in the Android Auto Desktop Head Unit (DHU) to verify letterboxing vs fill logic.
- **Log Validation**: Ensure `Projection` effects are being applied with correct width/height.
