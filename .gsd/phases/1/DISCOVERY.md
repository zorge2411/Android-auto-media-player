# Discovery - Phase 1: Video Scaling & Aspect Ratios

## Objective
Identify how to correctly apply video transformations (scaling, cropping, fitting) using Media3 `Presentation` effects on a raw Android Auto `Surface`.

## Questions
1. What are the available `Presentation` layout modes in Media3 `1.5.1`?
2. How does `Presentation` interact with the `Surface` provided by `CarAppService`?
3. Is `AspectRatioCalculator` needed if `Presentation` handles letterboxing?
4. How to implement "Fill" (zoom to fill) vs "Fit" (letterbox) modes?

## Research Findings

### Media3 Presentation Effect
`androidx.media3.effect.Presentation` is used to define output dimensions and scaling behavior.
Common layout modes:
- `LAYOUT_SCALE_TO_FIT`: Scales input to fit within output dimensions while preserving aspect ratio. Adds letterboxing/pillarboxing.
- `LAYOUT_SCALE_TO_FIT_WITH_CROPPING`: Scales input to fill output dimensions while preserving aspect ratio by cropping edges.
- `LAYOUT_STRETCH_TO_FIT`: Stretches input to exactly match output dimensions (distorts aspect ratio).

### Android Auto Surface
`CarAppService` provide a `Surface` via `SurfaceCallback.onSurfaceAvailable(surfaceContainer)`.
Dimensions are in `surfaceContainer.width` and `height`.
Since it's a raw Surface, `videoScalingMode` (like `VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING`) on `ExoPlayer` is often ineffective because it requires a `SurfaceHolder` or `TextureView`.
The `Effect` API (specifically `Presentation`) is the recommended way to handle this in Media3 for raw Surfaces.

### Implementation Strategy
1. **Selection**: User might want a button to toggle between "Fit" and "Fill".
2. **Execution**: Update `MediaPlayerManager.applyPresentationEffect()` to accept a `layoutMode` parameter.
3. **Trigger**: Call `applyPresentationEffect()` when:
    - New video starts (onVideoSizeChanged).
    - Surface dimensions change (onVisibleAreaChanged).
    - User toggles the scaling mode.

## Proposed Conclusion
We should refactor `MediaPlayerManager` to support multiple scaling modes by leveraging `Presentation` layout constants. `AspectRatioCalculator` can be used if we need to manually compute UI overlays that align with the video content, but `Presentation` should handle the actual pixel rendering.

## Next Steps
- Implement `ScalingMode` enum in `MediaPlayerManager`.
- Update `applyPresentationEffect` to use the selected mode.
- Update `VideoPlaybackScreen` to allow toggling (optional but recommended for completeness).
