---
phase: 1
slug: progress-slider-timeline-display
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-04-28
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 JVM unit tests (+ kotlinx-coroutines-test, added in 01-02) |
| **Config file** | app/build.gradle.kts — existing androidTestImplementation deps |
| **Quick run command** | `./gradlew :app:testDebugUnitTest` |
| **Full suite command** | `./gradlew connectedDebugAndroidTest` (requires device) |
| **Estimated runtime** | ~60 seconds (build only) |

Note: No test suite currently exists (per CLAUDE.md). All verification for Phase 1 is manual or build-time. The primary validation is: does the app compile and render correctly on a head unit?

---

## Sampling Rate

- **After every task commit:** Run `./gradlew build` (compile check)
- **After every plan wave:** Manual smoke test on device/emulator
- **Before `/gsd:verify-work`:** Full head-unit walkthrough
- **Max feedback latency:** 60 seconds (build) + manual test

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 1-01-01 | 01 | 1 | FEAT-1-AC4 formatter | unit | `./gradlew :app:testDebugUnitTest --tests "*TimeFormatterTest*"` | ❌ W0 (created in task) | ⬜ pending |
| 1-01-02 | 01 | 1 | FEAT-1-AC4 formatter fix | unit | `./gradlew :app:testDebugUnitTest --tests "*TimeFormatterTest*"` | ✅ after 1-01-01 | ⬜ pending |
| 1-01-03 | 01 | 1 | FEAT-1-AC5 1 Hz ticker | build | `./gradlew :app:assembleDebug` | ✅ existing | ⬜ pending |
| 1-02-01 | 02 | 1 | auto-hide timer | unit | `./gradlew :app:testDebugUnitTest --tests "*ControlsVisibilityControllerTest*"` | ❌ W0 (created in task) | ⬜ pending |
| 1-02-02 | 02 | 1 | touch reveal bridge | build | `./gradlew :app:assembleDebug` | ✅ existing | ⬜ pending |
| 1-02-03 | 02 | 1 | hidden state / screen wiring | build + unit | `./gradlew :app:assembleDebug :app:testDebugUnitTest` | ✅ existing | ⬜ pending |
| 1-03-01 | 03 | 2 | all (head unit) | manual | see 01-03 matrix | — | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements. No new test framework installation is needed. `./gradlew build` provides compile-time feedback after each task.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Controls auto-hide after 3s | UI-SPEC locked decision | Requires running app on head unit | Start playback, wait 3s, verify controls disappear |
| Tap-to-reveal | UI-SPEC locked decision | Requires SurfaceContainer touch input on device | Tap video surface, verify controls reappear |
| Timeline updates at 1Hz | ROADMAP UAT | Requires live playback | Play video, observe timeline text increments per second |
| No playback stutter on seek | ROADMAP UAT | ExoPlayer/codec behavior, device-dependent | Seek multiple times during playback, verify no audio/video glitch |
| onClick vs onScroll fallback | Car API level | API level varies by head unit | Test on both older (level < 5) and newer head units if available |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
