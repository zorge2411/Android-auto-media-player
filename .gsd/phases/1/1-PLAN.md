---
phase: 1
plan: 1
wave: 1
---

# Plan 1.1: Refactor Video Scaling Logic

## Objective
Enable robust video scaling (Fit/Fill) on Android Auto by leveraging Media3 `Presentation` effects and cleaning up the transformation TODOs in the renderer.

## Context
- .gsd/SPEC.md
- .gsd/ARCHITECTURE.md
- [MediaPlayerManager.kt](file:///c:/Users/peter/AndroidStudioProjects/Android%20auto%20media%20player/app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt)
- [VideoSurfaceRenderer.kt](file:///c:/Users/peter/AndroidStudioProjects/Android%20auto%20media%20player/app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt)

## Tasks

<task type="auto">
  <name>Refactor MediaPlayerManager for Scaling Modes</name>
  <files>
    <file>app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt</file>
  </files>
  <action>
    - Add `enum class ScalingMode { FIT, FILL, STRETCH }`.
    - Add a `StateFlow<ScalingMode>` to track the current mode.
    - Update `applyPresentationEffect()` to use `Presentation.LAYOUT_SCALE_TO_FIT` or `Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROPPING` based on the mode.
    - Ensure `applyPresentationEffect()` is called whenever the mode changes.
  </action>
  <verify>Check that the file compiles and includes the new ScalingMode logic.</verify>
  <done>MediaPlayerManager has a reactive scaling mode and correctly maps it to Media3 Presentation constants.</done>
</task>

<task type="auto">
  <name>Finalize VideoSurfaceRenderer Logic</name>
  <files>
    <file>app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt</file>
  </files>
  <action>
    - Remove the stale TODO at line 126.
    - Ensure `onVideoSizeChanged` correctly propagates dimensions to the player manager if needed, or rely on the reactive flow already in place.
    - Verify that `onVisibleAreaChanged` correctly updates the `MediaPlayerManager`'s output dimensions.
  </action>
  <verify>Check that the TODO is removed and surface callbacks correctly trigger player updates.</verify>
  <done>VideoSurfaceRenderer is clean and correctly synchronizes car surface dimensions with the media player pipeline.</done>
</task>

## Success Criteria
- [ ] MediaPlayerManager supports switching between FIT and FILL modes.
- [ ] Surface dimensions are correctly synchronized between Android Auto and ExoPlayer.
- [ ] No placeholder TODOs remain in the core rendering pipeline.
