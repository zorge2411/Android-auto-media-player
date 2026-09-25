---
phase: 01-progress-slider-timeline-display
plan: 03
type: verify
wave: 2
depends_on: ["01", "02"]
files_modified:
  - .planning/phases/01-progress-slider-timeline-display/01-UAT.md
autonomous: false
requirements: [FEAT-1-AC1, FEAT-1-AC2, FEAT-1-AC3, FEAT-1-AC4, FEAT-1-AC5, UI-SPEC-AUTOHIDE, UI-SPEC-REVEAL]
must_haves:
  truths:
    - "Every ROADMAP Phase 1 UAT bullet and every UI-SPEC auto-hide row maps to at least one row in 01-UAT.md"
    - "The host's Car App API level is recorded (Settings → 'Last AA Connection' debug card), because tap-to-reveal needs level >= 5"
    - "Each row is PASS / PARTIAL / FAIL with observed behaviour, head-unit model, AA app version and build SHA"
  artifacts:
    - path: ".planning/phases/01-progress-slider-timeline-display/01-UAT.md"
      provides: "Filled test matrix"
      contains: "FEAT-1-AC5"
---

<objective>
Wave 2: manual verification on a real head unit (or DHU). **autonomous: false.** Claude walks the user through the matrix and records results. Batch this session with the pending Phase 3 plan 03-07 and Phase 4 HUMAN-UAT, so one trip to the car covers all three.

Status outcome:
- `passed` → Phase 1 complete.
- `diagnosed` → `/gsd:plan-phase 1 --gaps`.
</objective>

<tasks>

<task type="checkpoint:human-verify">
  <name>Task 1: Run the Phase 1 matrix on the head unit</name>
  <action>
    Setup:
    - `./gradlew installDebug`
    - Connect to AA and note the head-unit model.
    - Open the phone's Settings screen → "Last AA Connection" card and record the host package, version and **Car API level**.
    - Use a local video of at least 2 minutes, plus one Plex or Jellyfin stream.

    Create `01-UAT.md` with these rows (same frontmatter format as 04-HUMAN-UAT.md):

    | # | Req | Steps | Expected |
    |---|-----|-------|----------|
    | 1 | FEAT-1-AC1/AC4 | Start playback | Timeline shows `00:00:00 / HH:MM:SS`, and the duration fills in once it's known |
    | 2 | FEAT-1-AC5 | Watch 10 s with the timeline visible | Counts up once per second; no skipped or doubled seconds |
    | 3 | FEAT-1-AC2/AC3 | Tap seek-forward ×3, then seek-back ×1 (parked) | Net +20 s, within ±2 s |
    | 4 | ROADMAP "no stutter" | Seek 5× quickly during playback | No audio glitch, no frozen frame longer than 1 s, no error toast |
    | 5 | UI-SPEC auto-hide | Wait 3 s after the last interaction | Timeline disappears. Record whether and when the host hides the action strips (host-controlled) |
    | 6 | UI-SPEC reveal (tap) | Tap the video surface once | Timeline reappears and hides again 3 s later. Expected only if Car API ≥ 5; otherwise mark N/A with the level |
    | 7 | UI-SPEC reveal (drag) | Drag a finger across the surface | Timeline reappears (onScroll, API 2) |
    | 8 | UI-SPEC toggle | Tap pause, then play | Icon swaps immediately each time; the timeline is shown and the timer restarts |
    | 9 | Todo closed | Look at the top-right strip | Stop button shows ✕ (ic_close), not pause; tapping it returns to Browse (Phase 4 AC3 regression) |
    | 10 | Regression | Hardware back during playback | Playback stops and you return to Browse (Phase 4 AC1) |
    | 11 | Regression | Pause for 20 s | Timeline stays frozen at the pause point; log shows no `invalidate()` spam |
    | 12 | Driving | (if safe or simulated) Drive mode | Host blocks button input; the app doesn't crash |

    Also capture:
    `adb logcat | grep -E "VideoSurfaceRenderer|VideoPlaybackScreen|IllegalStateException"`
    Any `IllegalStateException` from a template build = automatic FAIL.
  </action>
  <resume-signal>User reports results per row (e.g. "1 pass, 6 N/A api 4, …")</resume-signal>
  <done>01-UAT.md written with a status. For each FAIL row, add a Gaps section entry.</done>
</task>

</tasks>
