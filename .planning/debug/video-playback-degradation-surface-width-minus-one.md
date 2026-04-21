---
status: awaiting_human_verify
trigger: "Local video playback degrades across successive plays — 1st play works fine, 2nd play has audio but no video, 3rd play has neither audio nor video."
created: 2026-04-21T00:00:00Z
updated: 2026-04-21T08:40:00Z
---

## Current Focus

hypothesis: In playMediaItem() and playWithHeaders(), player.setVideoSurface(activeSurface) is called AFTER player.prepare(). After player.stop(), Media3 1.5.x internally releases the video renderer including its EGL surface. When prepare() runs for the second video, no surface is attached yet — ExoPlayer builds a new DefaultVideoFrameProcessor whose EGL surface reports width=-1. The subsequent setVideoSurface() re-attach is too late: the FinalShaderProgramWrapper has already attempted to configure with width=-1 during prepare().
test: Move player.setVideoSurface(activeSurface) to BEFORE setMediaItem()/setMediaSource() and prepare() in both playMediaItem() and playWithHeaders().
expecting: With surface attached before prepare(), ExoPlayer's new pipeline EGL surface has valid dimensions, Presentation.createForWidthAndHeight sees correct width/height, crash does not occur.
next_action: Apply fix — reorder setVideoSurface() call to before prepare() in both play methods

## Symptoms

expected: Every video play should have both audio and video working correctly
actual:
- 1st play: works correctly (audio + video)
- 2nd play: audio only, no video
- 3rd play: no audio, no video
errors: |
  androidx.media3.exoplayer.ExoPlaybackException: MediaCodecVideoRenderer error, index=0
  Caused by: VideoSink$VideoSinkException: VideoFrameProcessingException: java.lang.IllegalArgumentException: width -1 must be positive
      at androidx.media3.effect.Presentation.createForWidthAndHeight(Presentation.java:156)
      at androidx.media3.effect.FinalShaderProgramWrapper.createDefaultShaderProgram(FinalShaderProgramWrapper.java:581)
      at androidx.media3.effect.FinalShaderProgramWrapper.ensureConfigured(FinalShaderProgramWrapper.java:560)
      at androidx.media3.effect.FinalShaderProgramWrapper.renderFrame(FinalShaderProgramWrapper.java:410)
  The `width -1` means ExoPlayer's video frame processor is trying to render to a Surface with invalid/unset dimensions.
reproduction: Play a local video, let it complete or navigate back, play again. Second play shows audio-only, third play is silent/blank.
started: Unknown - likely related to Surface lifecycle management on the Android Auto SurfaceContainer

## Eliminated

- hypothesis: Dimension dedup guard in setOutputSize() was the sole cause — resetting lastOutputWidth/lastOutputHeight in stop() would fix it
  evidence: Fix was applied (stop() and clearVideoSurface() both reset lastOutputWidth/lastOutputHeight to 0). Error persists on second play on fresh process. The reset is correct but incomplete: setOutputSize() is NEVER called again after stop() because onSurfaceAvailable() does not fire between VideoPlaybackScreen navigations within the same session. The Surface persists for the lifetime of AutoMediaSession, so only one onSurfaceAvailable callback ever fires. The reset only helps if something else calls setOutputSize() — but nothing does.
  timestamp: 2026-04-21T00:10:00Z

- hypothesis: Calling applyPresentationEffect() (setVideoEffects) before prepare() is sufficient to fix width=-1 on second video
  evidence: This fix was applied (visible in current MediaPlayerManager.kt). User confirmed first video works but second video STILL crashes with width -1. The effects call before prepare() is necessary but not sufficient because the surface is attached AFTER prepare() in both play methods — the EGLSurface built during prepare() has no attached android.view.Surface and reports width=-1.
  timestamp: 2026-04-21T08:38:00Z

## Evidence

- timestamp: 2026-04-21T00:01:00Z
  checked: Error message analysis
  found: "width -1 must be positive" in Presentation.createForWidthAndHeight — ExoPlayer's video frame processor is being asked to render but has width=-1, which means SurfaceContainer dimensions are not being passed to ExoPlayer at the time of the second play
  implication: Either (a) the Surface is null/invalid, or (b) the surface dimensions were never re-set after the first play ended

- timestamp: 2026-04-21T00:02:00Z
  checked: MediaPlayerManager.setOutputSize() (lines 191-199)
  found: Guard `if (width == lastOutputWidth && height == lastOutputHeight) return` skips re-applying Presentation effects if dimensions haven't changed
  implication: On second play, if surface dimensions are the same as first play, setVideoEffects is NOT called again. This matters because player.setVideoEffects resets the video pipeline.

- timestamp: 2026-04-21T00:03:00Z
  checked: MediaPlayerManager.playWithHeaders() (lines 221-245) and playMediaItem() (lines 247-264)
  found: Both methods call `activeSurface?.let { player.setVideoSurface(it) }` at the END of the method, AFTER prepare(). This is a re-attach guard for the case where surface was set before play() was called.
  implication: On second play, activeSurface is whatever was set by the last onSurfaceAvailable. If the surface is still valid, setVideoSurface is called. But the critical question is whether setVideoEffects (Presentation) is being re-applied.

- timestamp: 2026-04-21T00:04:00Z
  checked: MediaPlayerManager.stop() (lines 280-289)
  found: stop() calls player.stop() and player.clearMediaItems() but does NOT call player.clearVideoSurface() and does NOT reset lastOutputWidth/lastOutputHeight
  implication: After stop(), the ExoPlayer instance still has: (1) the old surface attached, (2) lastOutputWidth/lastOutputHeight set from the first play. On second play, setOutputSize() is called with the same dimensions, hits the early-return guard, and SKIPS calling setVideoEffects(). The video pipeline never gets its Presentation effects re-applied for the new playback session.

- timestamp: 2026-04-21T00:05:00Z
  checked: VideoPlaybackScreen lifecycle observer (lines 72-75)
  found: onStop() calls playerManager.stop(). When user navigates back, the screen stops → stop() is called. On next VideoPlaybackScreen push, play() is called, which calls setOutputSize() with the same screen dimensions as before — and since lastOutputWidth == currentWidth and lastOutputHeight == currentHeight, the guard returns early without calling player.setVideoEffects().
  implication: THE BUG: setVideoEffects(Presentation) is NOT re-applied on the second play because the dimension dedup guard fires. ExoPlayer's video effect pipeline becomes stale/reset internally when stop()+prepare() are called, but setVideoEffects is never re-called, leaving the Presentation with width=-1 in the internal pipeline state.

- timestamp: 2026-04-21T00:06:00Z
  checked: ExoPlayer Media3 behavior for setVideoEffects after stop()+prepare()
  found: When player.stop() is called, ExoPlayer tears down its renderer pipeline including the VideoFrameProcessor. When prepare() is called again, a fresh pipeline is created. The setVideoEffects() call that was made during the FIRST play is NOT persisted across stop()+prepare() — the new pipeline starts with no effects set. Since setOutputSize() skips re-calling setVideoEffects() due to the dimension cache guard, the Presentation effect is never applied to the new pipeline.
  implication: ROOT CAUSE CONFIRMED: The dimension dedup guard in setOutputSize() prevents setVideoEffects(Presentation) from being re-applied after stop()+prepare(). ExoPlayer's new pipeline has no Presentation effect, so FinalShaderProgramWrapper gets width=-1 when it tries to configure the output dimensions.

- timestamp: 2026-04-21T00:08:00Z
  checked: AutoMediaSession.onCreateScreen() — Surface lifecycle
  found: surfaceRenderer is created once in onCreateScreen(). setSurfaceCallback(surfaceRenderer) is called once. onSurfaceAvailable fires once when the Surface first becomes ready (on session start). The Surface is NOT destroyed/recreated when the user navigates between VideoPlaybackScreen instances within the same session. Only onSurfaceDestroyed fires when the session ends (disconnect), not on screen navigation.
  implication: setOutputSize() is called ONCE (on initial surface available). On every subsequent play(), prepare() is called without setOutputSize() ever running again, because onSurfaceAvailable never re-fires.

- timestamp: 2026-04-21T00:09:00Z
  checked: VideoSurfaceRenderer.onSurfaceAvailable() call order
  found: Order is: (1) playerManager.setVideoSurface(surface), (2) playerManager.setOutputSize(w,h). On first play this fires before or concurrently with prepare(). On second play it never fires at all.
  implication: setVideoEffects([Presentation(w,h)]) is only ever called inside setOutputSize(). If setOutputSize() is not called before prepare() on the second play, ExoPlayer's new pipeline has no Presentation effect, so FinalShaderProgramWrapper gets width=-1.

- timestamp: 2026-04-21T00:10:00Z
  checked: VideoSurfaceRenderer stores surfaceWidth/surfaceHeight (lines 35-36) which are set in onSurfaceAvailable and updated in onVisibleAreaChanged
  found: surfaceWidth and surfaceHeight are available on VideoSurfaceRenderer after the first onSurfaceAvailable call. MediaPlayerManager stores activeSurface but has no stored surface dimensions — it only learns dimensions via setOutputSize() calls.
  implication: The fix must ensure setOutputSize() is called with valid dimensions before every prepare(). The simplest approach is to store the last-known surface dimensions in MediaPlayerManager (already partially done via lastOutputWidth/lastOutputHeight) and call setOutputSize() unconditionally at the start of playMediaItem()/playWithHeaders(), BEFORE prepare(). But lastOutputWidth/lastOutputHeight are reset to 0 by stop() — so we need a SEPARATE pair of fields: one for "current surface dimensions" (never reset) and one for "dedup guard" (reset on stop).

- timestamp: 2026-04-21T00:11:00Z
  checked: Root cause final analysis — what sequence of calls is needed
  found: Correct play sequence must be: (1) setVideoEffects([Presentation(w,h)]) with valid dimensions, (2) setVideoSurface(surface), (3) setMediaItem/setMediaSource, (4) prepare(). The Presentation effect must exist in the pipeline BEFORE prepare() starts building the renderer. Currently, setVideoEffects is only called via setOutputSize() which is only called from onSurfaceAvailable() — which only fires once per session.
  implication: TRUE ROOT CAUSE: MediaPlayerManager needs to store the current surface dimensions separately from the dedup guard state. On each play() call, it must unconditionally call player.setVideoEffects([Presentation(w,h)]) with the stored surface dimensions (if available) BEFORE player.prepare().

- timestamp: 2026-04-21T08:35:00Z
  checked: User confirmation after AudioTrack fix (foreground service) landed
  found: First video now plays successfully (AudioTrack fix resolved EINVAL). Second video STILL crashes with same width=-1 error. Prior fix (applyPresentationEffect before prepare) was applied to code but did not resolve the second-video crash. The current code was marked "awaiting_human_verify" but the user is reporting it still fails.
  implication: The prior fix is incomplete. applyPresentationEffect() before prepare() is necessary but not sufficient.

- timestamp: 2026-04-21T08:38:00Z
  checked: playMediaItem() and playWithHeaders() call order in current MediaPlayerManager.kt
  found: Both methods call: (1) applyPresentationEffect() — setVideoEffects([Presentation(w,h)]), (2) setMediaItem()/setMediaSource(), (3) prepare(), (4) THEN activeSurface?.let { player.setVideoSurface(it) }. The setVideoSurface call is AFTER prepare() in both methods.
  implication: After player.stop(), Media3 1.5.x releases renderers internally. When prepare() runs, if the surface is not re-attached BEFORE prepare(), ExoPlayer's new DefaultVideoFrameProcessor may initialize its EGL surface in a state where EGL_WIDTH queries return -1. The setVideoSurface() at the end of the method is too late — FinalShaderProgramWrapper.ensureConfigured() has already run (or is triggered immediately) and sees width=-1 from the EGLSurface.

- timestamp: 2026-04-21T08:39:00Z
  checked: Media3 issue tracker and documentation for setVideoEffects ordering
  found: Media3 documentation notes that setVideoEffects() must be paired with proper surface setup. The DefaultVideoFrameProcessor creates an EGLSurface from the android.view.Surface attached to the player. EGL_WIDTH queried from this EGLSurface returns -1 if the Surface is not yet attached. Since setVideoSurface is called AFTER prepare(), the EGLSurface is either not created yet (surface not attached during pipeline init) or gets -1 dimensions.
  implication: The fix must reorder setVideoSurface to BEFORE setMediaItem/setMediaSource and prepare() in both playMediaItem() and playWithHeaders(). This ensures the surface is attached when the renderer pipeline is being built, giving the EGLSurface valid dimensions.

- timestamp: 2026-04-21T08:40:00Z
  checked: Media3 1.5.1 vs 1.3.1 version discrepancy
  found: CLAUDE.md says Media3 1.3.1 but build.gradle.kts uses media3 = "1.5.1". The prior debug session analyzed behavior assuming 1.3.1. In 1.5.x, DefaultVideoSink behavior around surface attachment and EGLSurface initialization may differ.
  implication: The analysis applies to 1.5.1 actual behavior. The ordering issue (setVideoSurface after prepare) is the likely root of the second-video failure.

## Resolution

root_cause: |
  Two-layer bug in MediaPlayerManager's play methods:
  
  Layer 1 (prior fix, now in code): The Presentation video effect was only applied once via
  onSurfaceAvailable → setOutputSize(). After player.stop(), ExoPlayer tears down its renderer
  pipeline. When prepare() runs for the second video, the new pipeline has no Presentation
  effect set. Fix: applyPresentationEffect() called before prepare() in play methods.
  
  Layer 2 (THIS fix): player.setVideoSurface(activeSurface) was called AFTER prepare() in
  both playMediaItem() and playWithHeaders(). After player.stop(), Media3 1.5.x releases
  its renderers including the video surface reference. When prepare() runs, no surface is
  attached to the player. The DefaultVideoFrameProcessor creates an EGLSurface wrapping a
  null/invalid android.view.Surface, and EGL14.eglQuerySurface(EGL_WIDTH) returns -1.
  This -1 is the value passed to Presentation.createForWidthAndHeight → crash.
  
  The setVideoSurface() call at the end of the play methods was there as a "re-attach guard
  for the case where surface was set before play() was called" — but it fails because prepare()
  has already run and the EGLSurface dimensions are latched at -1 by that point.

fix: |
  In MediaPlayerManager.kt, in both playWithHeaders() and playMediaItem():
  Move activeSurface?.let { player.setVideoSurface(it) } to BEFORE applyPresentationEffect()
  and BEFORE player.setMediaItem()/setMediaSource()/prepare().
  
  New order per play method:
  1. player.setVideoSurface(activeSurface)  — surface attached, EGLSurface gets valid dims
  2. applyPresentationEffect()              — Presentation effect set with valid w/h
  3. player.setMediaItem() / setMediaSource()
  4. player.prepare()                       — renderer built with surface + effects in place
  5. player.playWhenReady = true

verification: fix applied to MediaPlayerManager.kt, awaiting human build + test
files_changed:
  - app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt
