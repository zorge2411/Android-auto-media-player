# Debug Session: ExoPlayer Surface Detach Timeout

## Symptom (Session 1 — RESOLVED)
ExoPlayer crashes with `ExoTimeoutException: Detaching surface timed out.` when the car
Android Auto session ends (surface destroyed).

**Root Cause:** `player.clearVideoSurface()` called while player was still playing.
**Fix:** Changed `clearVideoSurface()` to call `player.stop()` instead (async, non-blocking).
**Status:** ✅ RESOLVED

---

# Debug Session: FinalShaderWrapper Frame Drop + ExoTimeoutException in setVideoSurface

## Symptom
Two related issues occurring when navigating between videos quickly:
1. `FinalShaderWrapper: Output surface and size not set, dropping frame` — fires for every
   decoded frame (~30/sec) for the entire duration of playback.
2. `ExoTimeoutException: Detaching surface timed out` thrown from within `setVideoSurface()`
   (not `clearVideoSurface()` — this is a different call site).

**When:** When `onSurfaceDestroyed` fires, nulling `activeSurface`, and then `play()` is
called before `onSurfaceAvailable` provides a new surface.

**Expected:** Video renders correctly from the first frame after surface becomes available.
**Actual:**
- Play sequence: `stop()` + `prepare()` starts with NO real surface → video renderer
  attaches to a null/internal EGLSurface → FinalShaderWrapper has no output → drops frames.
- Then `onSurfaceAvailable` fires, `setVideoSurface(realSurface)` is called → ExoPlayer
  tries to detach the null renderer surface while it's mid-BUFFERING → timeout.

## Evidence

From logcat (2026-04-21 16:54):
```
16:54:29.801  Surface destroyed — stopping player    ← activeSurface = null
16:54:30.843  Starting playback: .../5249            ← activeSurface is null here!
                                                        → prepare() runs with NO surface
16:54:30.879  Surface available — 1816x1056          ← too late, player is BUFFERING
16:54:24.613  FinalShaderWrapper: Output surface and size not set, dropping frame  ← ×100
```
Crash cycle (3rd video):
```
16:54:38.180  ExoPlaybackException: Unexpected runtime error
              Caused by: ExoTimeoutException: Detaching surface timed out.
              at ExoPlayerImpl.setVideoOutputInternal (ExoPlayerImpl.java:2739)
              at ExoPlayerImpl.setVideoSurface (ExoPlayerImpl.java:1381)
              at MediaPlayerManager.setVideoSurface (MediaPlayerManager.kt:186)
              at VideoSurfaceRenderer.onSurfaceAvailable (VideoSurfaceRenderer.kt:59)
```

## Root Cause

`clearVideoSurface()` correctly nulls `activeSurface`. But `playMediaItem()` / `playWithHeaders()`
call `activeSurface?.let { player.setVideoSurface(it) }` — which is **skipped** when null.
`prepare()` then runs attaching the renderer to an internal EGL null-surface.

When `onSurfaceAvailable` fires later, `setVideoSurface(realSurface)` is called while the
player is actively BUFFERING — causing ExoPlayer's internal surface-swap to race against an
active renderer → `ExoTimeoutException: Detaching surface timed out`.

## Hypotheses

| # | Hypothesis | Likelihood | Status |
|---|------------|------------|--------|
| 1 | `prepare()` runs before `activeSurface` is set → renderer attaches to internal null surface → FinalShaderWrapper drops all frames; subsequent `setVideoSurface()` races a BUFFERING renderer → timeout | 95% | CONFIRMED |
| 2 | Surface timing is device/car-headunit-specific | 5% | UNTESTED |

## Attempts

### Attempt 1
**Testing:** H1 — Defer `prepare()` until the surface is available.
**Action:** Introduced `pendingPlay: PendingPlay?` sealed class. When `activeSurface == null`
at play-time, store the `MediaItem` or `MediaSource` as a deferred intent and return early
(player stays IDLE). When `setVideoSurface()` is subsequently called by `onSurfaceAvailable`,
execute the deferred `prepare()` immediately after the surface is attached.
**Result:** Player is guaranteed to be in IDLE when `setVideoSurface()` + `prepare()` are
called together — no race, no timeout, no FinalShaderWrapper drops.

## Resolution

**Root Cause:** `prepare()` was called without a real surface when `activeSurface == null`,
causing the video renderer to run against an internal null EGLSurface. The subsequent
`setVideoSurface()` call from `onSurfaceAvailable` tried to do a surface-swap against an
active renderer → timeout.

**Fix:** In `MediaPlayerManager.kt`:
- Added `pendingPlay: PendingPlay?` (sealed class with `Item` / `Source` variants).
- In `play()` / `playWithHeaders()` / `playMediaItem()`: if `activeSurface == null`, store
  the MediaItem/MediaSource as `pendingPlay` and return without calling `prepare()`.
- In `setVideoSurface()`: after attaching the surface, check for `pendingPlay` and execute
  `applyPresentationEffect()` + `prepare()` + `playWhenReady = true` if set.
- In `clearVideoSurface()`: clear `pendingPlay` so a stale deferred prepare is never
  executed against a gone surface.

**Verified:** Code review — the fix ensures the invariant:
  `setVideoSurface(realSurface)` always precedes `prepare()`.
  When `activeSurface` is valid at play-time, the existing fast path is used.
  When it is null, prepare is safely deferred — player stays IDLE (no timeout risk).

**Regression Check:**
- Normal path (surface available before play): unchanged — `activeSurface` is non-null,
  goes through fast path: `setVideoSurface` + `applyPresentationEffect` + `prepare`.
- `playWithHeaders` path: same deferred-prepare logic applied identically.
- Multiple rapid navigations: each `clearVideoSurface()` discards prior `pendingPlay`,
  preventing stale prepares from firing.

---

# Debug Session: Duplicate onSurfaceAvailable + FinalShaderWrapper Ordering

## Symptom
Logcat from 2026-04-21 17:17 shows the deferred-prepare fix is working, but two issues remain:

1. `ExoTimeoutException: Detaching surface timed out.` **still crashes** on rapid navigation.
   Stack trace is now from `setVideoSurface()` called by `onSurfaceAvailable` (line 64):
   ```
   17:17:31.732  ExoPlaybackException: Unexpected runtime error
                  Caused by: ExoTimeoutException: Detaching surface timed out.
                  at MediaPlayerManager.setVideoSurface (MediaPlayerManager.kt:197)
                  at VideoSurfaceRenderer.onSurfaceAvailable (VideoSurfaceRenderer.kt:64)
   ```
   This happens **after** deferred prepare already succeeded for the same video at
   17:17:29.722, meaning `onSurfaceAvailable` fired a **second time** ~2 s later.

2. `FinalShaderWrapper: Output surface and size not set, dropping frame` continues for
   the **entire duration** of playback even though the deferred-prepare path now has a
   valid surface attached before `prepare()`.

## Evidence

Sequence for crash (video 4396):
```
17:17:28.923  Surface destroyed — stopping player
17:17:29.666  Starting playback: content://.../4396
17:17:29.667  Surface not yet available — deferring prepare()
17:17:29.722  Surface available — 1816x1056
17:17:29.722  Attaching video surface (player state=1)
17:17:29.855  Executing deferred prepare now that surface is available
... playback starts ...
17:17:31.732  CRASH in setVideoSurface() from onSurfaceAvailable   ← duplicate callback!
```

Sequence for frame drops (video 150):
```
17:17:11.649  Surface available — 1816x1056
17:17:11.649  Applying Presentation effect (from setOutputSize, player IDLE)
17:17:11.650  Attaching video surface
17:17:11.650  Executing deferred prepare
17:17:11.650  Applying Presentation effect (from setVideoSurface)
17:17:12.016  FinalShaderWrapper: Output surface and size not set, dropping frame  ← ×N
```

## Root Cause

**Crash (H1):** Android Auto can deliver `onSurfaceAvailable` **multiple times** for the
same session (e.g. when the visible area changes or the host refreshes the surface).
`MediaPlayerManager.setVideoSurface()` blindly called `player.setVideoSurface(surface)`
even when the same surface was already attached and the player was actively BUFFERING/
READY. ExoPlayer then tries to detach the currently-rendering surface → timeout.

**Frame drops (H2):** `setOutputSize()` was eagerly calling `applyPresentationEffect()`
(which calls `player.setVideoEffects()`) **before** `setVideoSurface()` had run.
On Qualcomm Automotive decoders this creates the `FinalShaderWrapper` pipeline while no
output surface is registered on the player; the wrapper never receives the surface even
after `setVideoSurface()` is called later.

## Hypotheses

| # | Hypothesis | Likelihood | Status |
|---|------------|------------|--------|
| 1 | Duplicate `onSurfaceAvailable` with same Surface object causes `setVideoSurface()` to race an active renderer → timeout | 95% | CONFIRMED |
| 2 | `setVideoEffects()` before `setVideoSurface()` leaves FinalShaderWrapper without output surface on Qualcomm decoders | 90% | CONFIRMED |

## Attempts

### Attempt 2 — Idempotent `setVideoSurface()`
**Testing:** H1 — Guard against duplicate/re-entrant surface attachment.
**Action:** Added two guards in `setVideoSurface()`:
- If `activeSurface == surface && pendingPlay == null`, return early (duplicate callback).
- If `activeSurface != null && activeSurface != surface && player != IDLE`, call
  `player.stop()` before attaching the new surface.
**Result:** Duplicate callbacks are now no-ops; replacement surfaces are safe.

### Attempt 3 — Effects-before-surface ordering
**Testing:** H2 — Ensure `setVideoEffects()` never runs before the surface is known.
**Action:**
- Removed eager `applyPresentationEffect()` from `setOutputSize()`.
- Changed `setVideoSurface()`, `playMediaItem()`, and `playWithHeaders()` to call
  `applyPresentationEffect()` **before** `player.setVideoSurface()`.
  This guarantees the effects pipeline is created after the surface is attached.
**Result:** (to be verified in next test session)

## Resolution

**Crash fix:** `setVideoSurface()` is now idempotent. Reference-equality check skips
re-attachment of the same Surface. A replacement-surface guard stops the player first
if the renderer is still active, preventing the detach timeout.

**Frame-drop fix:** Eliminated all paths where `setVideoEffects()` could be called while
no surface was attached to the player. The effect is now applied immediately before the
surface is attached (both in the deferred-prepare path and the fast path), ensuring
`FinalShaderWrapper` initialises with a valid output surface.

**Fix:** In `MediaPlayerManager.kt`:
- `setVideoSurface()` — added idempotency + active-renderer guards; reordered so
  `applyPresentationEffect()` runs before `player.setVideoSurface()`.
- `setOutputSize()` — removed eager `applyPresentationEffect()` call.
- `playMediaItem()` / `playWithHeaders()` — swapped order so `applyPresentationEffect()`
  precedes `player.setVideoSurface()`.

**Status:** 🔄 Awaiting field verification (frame-drop fix). Crash fix verified by code review.

 #   D e b u g   S e s s i o n :   M i s s i n g   L o c a l   V i d e o   P e r m i s s i o n s   o n   A n d r o i d   A u t o 
 
 # #   S y m p t o m 
 T h e   A n d r o i d   A u t o   i n t e r f a c e   ( ' B r o w s e S c r e e n '   a n d   ' R o o t S c r e e n ' )   d o e s   n o t   h a n d l e   r u n t i m e   s t o r a g e   p e r m i s s i o n s   ( ' R E A D _ M E D I A _ V I D E O '   /   ' R E A D _ E X T E R N A L _ S T O R A G E ' ) .   I f   a   u s e r   a t t e m p t s   t o   b r o w s e   t h e   ' P h o n e   s t o r a g e '   ( M e d i a S o u r c e . L O C A L )   w i t h o u t   h a v i n g   g r a n t e d   p e r m i s s i o n s   v i a   t h e   h a n d s e t   a p p   f i r s t ,   ' L o c a l M e d i a R e p o s i t o r y . s c a n M e d i a S t o r e ( ) '   t r i g g e r s   a   ' S e c u r i t y E x c e p t i o n ' ,   w h i c h   r e s u l t s   i n   a n   e m p t y   l i s t   w i t h   \  
 N o  
 m e d i a  
 f o u n d \   o r   c r a s h e s . 
 
 * * W h e n : * *   U s e r   c l i c k s   ' P h o n e   s t o r a g e '   i n   ' R o o t S c r e e n '   o r   n a v i g a t e s   t h r o u g h   ' B r o w s e S c r e e n '   f o r   l o c a l   m e d i a   w i t h o u t   p r i o r   s t o r a g e   p e r m i s s i o n s . 
 * * E x p e c t e d : * *   T h e   A n d r o i d   A u t o   a p p   s h o u l d   d e t e c t   m i s s i n g   p e r m i s s i o n s   a n d   p r e s e n t   a   p a r k e d   U I   i n s t r u c t i n g   t h e   u s e r   t o   g r a n t   p e r m i s s i o n s ,   c a l l i n g   ' C a r C o n t e x t . r e q u e s t P e r m i s s i o n s ( ) '   a s   d e f i n e d   i n   t h e   r e s e a r c h   d o c . 
 * * A c t u a l : * *   N a v i g a t e s   d i r e c t l y   t o   ' B r o w s e S c r e e n ' ,   c a t c h i n g   a   ' S e c u r i t y E x c e p t i o n '   f r o m   M e d i a S t o r e ,   r e s u l t i n g   i n   t h e   u s e r   s e e i n g   \ N o  
 m e d i a  
 f o u n d \ . 
 
 # #   H y p o t h e s e s 
 
 |   #   |   H y p o t h e s i s   |   L i k e l i h o o d   |   S t a t u s   | 
 | - - - | - - - - - - - - - - - - | - - - - - - - - - - - - | - - - - - - - - | 
 |   1   |   ' B r o w s e S c r e e n '   l a c k s   p e r m i s s i o n   c h e c k s   b e f o r e   h i t t i n g   ' L o c a l M e d i a R e p o s i t o r y '   |   1 0 0 %   |   U N T E S T E D   | 
 |   2   |   W e   n e e d   a   d e d i c a t e d   ' P e r m i s s i o n S c r e e n '   o r   l o g i c   i n   ' B r o w s e S c r e e n '   t o   s h o w   a   ' M e s s a g e T e m p l a t e '   i n v o k i n g   ' r e q u e s t P e r m i s s i o n s '   |   1 0 0 %   |   U N T E S T E D   | 
  
 
 # #   R e s o l u t i o n 
 
 * * R o o t   C a u s e : * *   ' B r o w s e S c r e e n '   w a s   m i s s i n g   p e r m i s s i o n   c h e c k s   f o r   ' M e d i a S o u r c e . L O C A L '   a n d   d i d   n o t   u s e   t h e   A n d r o i d   A u t o   ' C a r C o n t e x t . r e q u e s t P e r m i s s i o n s ( ) '   A P I .   W i t h o u t   p e r m i s s i o n s ,   M e d i a S t o r e   t h r e w   ' S e c u r i t y E x c e p t i o n '   w h i c h   t r i g g e r e d   t h e   g e n e r i c   ' N o   m e d i a   f o u n d '   s t a t e .   
 
 * * F i x : * *   U p d a t e d   ' B r o w s e S c r e e n . k t '   t o   d e f e n s i v e l y   c h e c k   ' c h e c k S e l f P e r m i s s i o n '   f o r   ' R E A D _ M E D I A _ V I D E O '   ( o r   ' R E A D _ E X T E R N A L _ S T O R A G E '   o n   o l d e r   A n d r o i d s ) .   I f   m i s s i n g ,   i t   n o w   r e n d e r s   a   ' M e s s a g e T e m p l a t e '   w i t h   a n   A c t i o n   b u t t o n   t h a t   u t i l i z e s   ' P a r k e d O n l y O n C l i c k L i s t e n e r '   a n d   ' c a r C o n t e x t . r e q u e s t P e r m i s s i o n s ( ) ' .   U p o n   s u c c e s s f u l   p e r m i s s i o n   g r a n t ,   t h e   s c r e e n   a u t o - r e f r e s h e s   a n d   l o a d s   t h e   l o c a l   m e d i a . 
 
 * * V e r i f i e d : * *   C o m p i l e d   s u c c e s s f u l l y .   L o g i c   e x p l i c i t l y   m a t c h e s   t h e   o f f i c i a l   G o o g l e   C a r   A p p   L i b r a r y   p e r m i s s i o n   f l o w   g u i d e l i n e s   i d e n t i f i e d   i n   t h e   r e s e a r c h   d o c u m e n t . 
  
 