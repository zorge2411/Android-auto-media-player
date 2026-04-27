---
phase: 4
slug: smart-back-button-navigation
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-27
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 (existing Android project) |
| **Config file** | `app/src/test/` (existing) |
| **Quick run command** | `./gradlew testDebugUnitTest` |
| **Full suite command** | `./gradlew testDebugUnitTest` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew testDebugUnitTest`
- **After every plan wave:** Run `./gradlew testDebugUnitTest`
- **Before `/gsd:verify-work`:** Full suite must be green + manual device test
- **Max feedback latency:** 30 seconds (unit tests); device test required for behavioral verification

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 4-01-01 | 01 | 1 | Feature 2 | unit | `./gradlew testDebugUnitTest` | ❌ W0 | ⬜ pending |
| 4-01-02 | 01 | 1 | Feature 2 | manual | Device: press back from video | N/A | ⬜ pending |
| 4-02-01 | 02 | 2 | Feature 2 | unit | `./gradlew testDebugUnitTest` | ❌ W0 | ⬜ pending |
| 4-02-02 | 02 | 2 | Feature 2 | manual | Device: navigate deep, back, check scroll | N/A | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/com/pscholer/autoplayer/util/NavigationStackTest.kt` — unit tests for scroll index tracking
- [ ] `app/src/test/java/com/pscholer/autoplayer/car/screens/BrowseScreenScrollTest.kt` — test lastClickedIndex field tracking

*If none: "Existing infrastructure covers all phase requirements."*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Back from VideoPlaybackScreen stops playback and returns to Browse | Feature 2 AC1 | Requires Car App Library host + ExoPlayer running on device | Start video, press hardware back, verify playback stops and Browse screen shown |
| Scroll position restored after back from video | Feature 2 AC4 | GridTemplate scroll state only testable on real head unit | Browse to item #5+, tap video, press back, verify grid shows approximately same position |
| No navigation loops or crashes | Feature 2 UAT | Requires full navigation stack on device | Tap back repeatedly from all screens, verify no crash or loop |
| Stop button pops to Browse (not Root) | Feature 2 AC3 | Requires AA device | During playback, tap stop icon, verify BrowseScreen (not RootScreen) appears |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
