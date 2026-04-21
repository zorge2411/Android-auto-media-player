# Phase 2 Plan — Stop Other Audio Sources

**Phase:** 2-audio-focus  
**Goal:** Pause other apps' audio when playback starts  
**Status:** Verification + Documentation (no code changes needed)

## Objective

Verify that Media3's automatic audio focus handling works correctly in car context. Current implementation is correct; this phase documents and tests it.

## Success Criteria

- ✅ Audio focus configuration verified in MediaPlayerManager (lines 67-76)
- ✅ Other audio apps pause when playback starts (manual device test)
- ✅ Focus released cleanly on stop/pause
- ✅ Foreground service maintains focus while app backgrounded
- ✅ Documentation added for future team members

## Architecture Review

**Current Implementation:** CORRECT ✅

MediaPlayerManager.kt lines 61-70:
```kotlin
val player: ExoPlayer = ExoPlayer.Builder(context)
    .setAudioAttributes(
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)  // Video/movie content
            .setUsage(C.USAGE_MEDIA)                      // Standard media usage
            .build(),
        /* handleAudioFocus = */ true  // Let Media3 manage the full focus lifecycle
    )
    .setHandleAudioBecomingNoisy(true)  // Auto-pause on headphone unplug
    .build()
```

**What This Does:**
- `handleAudioFocus = true` → Media3 requests/releases focus automatically
- `AUDIO_CONTENT_TYPE_MOVIE` → Hints to system this is video content
- `USAGE_MEDIA` → Standard entertainment usage category
- `setHandleAudioBecomingNoisy` → Pause on headphone disconnect

**Why This Works:**
Media3 lifecycle:
1. `play()` called → ExoPlayer requests audio focus from AudioManager
2. Other apps receive focus loss → they pause/duck
3. Playback pauses → ExoPlayer releases focus
4. Foreground service active → focus maintained in background

## Tasks

### Task 2.1: Verify Configuration
**Effort:** Small (5 min)
**Status:** Already complete (configuration correct)
**Verification:** Code review confirms lines 67-76 match recommended pattern

### Task 2.2: Document Audio Focus Behavior
**Effort:** Small (10 min)
**File:** Add comment block to MediaPlayerManager above audio setup
**Content:**
```
// Audio Focus Lifecycle:
// - play() → ExoPlayer requests AUDIOFOCUS_GAIN
// - other apps receive AUDIOFOCUS_LOSS → they pause
// - pause() → ExoPlayer releases focus
// - Foreground service maintains focus while backgrounded
// - Headphone disconnect → auto-pause (handleAudioBecomingNoisy)
//
// No manual AudioManager calls needed; Media3 handles all transitions.
```

### Task 2.3: Manual Device Testing
**Effort:** Medium (20 min on device)
**Prerequisites:** Phase 1 complete (playback works)
**Test Steps:**
1. Start playback in Auto Player
2. Start Spotify/music app on same phone
3. Verify Spotify pauses automatically
4. Resume Spotify → Auto Player pauses
5. Test with podcast app, YouTube, etc.
6. Test while app backgrounded (car head unit display off)
7. Verify no audio artifacts or stuttering

**Pass Criteria:**
- Other audio pauses when playback starts ✓
- Other audio resumes when Auto Player pauses ✓
- No crashes or permission errors ✓
- Foreground service active during background playback ✓

### Task 2.4: Create Test Case (Optional)
**Effort:** Medium (25 min)
**File:** `app/src/androidTest/java/.../AudioFocusTest.kt`
**Scope:** Instrumented test verifying audio focus requests/releases
**Note:** Not critical; manual device test sufficient for MVP

## Critical Path

- Task 2.1 (verification) → Task 2.2 (documentation) → Task 2.3 (device test)

## Rollback

If audio focus breaks (other audio doesn't pause):
1. Verify `handleAudioFocus = true` still present
2. Check AudioAttributes are set correctly
3. Verify no overlapping manual AudioManager calls added elsewhere
4. Test on multiple device types (some car head units may override focus)

## Next Phase

After device testing passes, proceed to Phase 3: Media Aspect Ratio Preservation.

## Notes

- Audio focus is a system-level Android feature; car head units may have their own audio routing that overrides system focus
- Car-specific verification may require real Android Automotive head unit
- Current setup is industry-standard (same as YouTube, Spotify, etc.)
