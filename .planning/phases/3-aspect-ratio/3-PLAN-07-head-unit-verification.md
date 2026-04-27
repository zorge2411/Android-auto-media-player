---
phase: 3-aspect-ratio
plan: 07
type: verify
wave: 3
depends_on: ["06"]
files_modified: []
autonomous: false
requirements: [Feature-4.1, Feature-4.2, Feature-4.3, Feature-4.4, Feature-4.5]
must_haves:
  truths:
    - "Phase 3 UAT criteria from ROADMAP.md are verified on a real Android Auto head unit, not an emulator"
    - "16:9 / 4:3 / 21:9 / 9:16 test assets all render with correct display aspect ratio (no squeeze, no stretch)"
    - "PLAY #2+ works without the historic ~2s Qualcomm-codec-cleanup hang (regression check from prior debug session)"
    - "Visible-area changes (template chrome appearing/disappearing) do not cause distortion or aspect ratio drift"
    - "Anamorphic content (PAR ≠ 1.0), if a test asset is available, displays at the correct DAR (not at the storage AR)"
    - "Result is captured in 3-07-UAT.md with status PASS / PARTIAL / FAIL per test row, plus head-unit model and build hash"
  artifacts:
    - path: ".planning/phases/3-aspect-ratio/3-07-UAT.md"
      provides: "Filled-in test matrix (16:9, 4:3, 21:9, 9:16, anamorphic, PLAY #2+, visible-area), PASS/FAIL per row, observed behavior, head-unit model + build SHA"
      contains: "16:9"
  key_links:
    - from: "Phase 3 ROADMAP UAT criteria"
      to: "3-07-UAT.md test rows"
      via: "Each ROADMAP UAT bullet maps to one or more test rows in the matrix"
      pattern: "Videos display in native aspect ratio"
---

<objective>
Wave 3 — final manual verification on a real Android Auto head unit. Plans 01–06 deliver the GL intermediary code path; this plan proves it actually works for the user. **autonomous: false** — only a human with a connected head unit can drive this. Claude's role is to walk the user through the matrix, record results in `3-07-UAT.md`, and surface any FAIL rows for follow-up gap planning.

Purpose:
- Close the loop on the Phase 3 goal: "Display videos in native aspect ratio without distortion".
- Provide empirical evidence the Option B pipeline is doing what RESEARCH.md claims it does, on the actual surface stack we couldn't fully simulate (the same surface stack that exposed the Media3 `setFrameRate -38` regression in the first place).
- Catch regressions in PLAY #2+ behavior (the 2s Qualcomm hang from the prior debug session) — this is the canary for the secondary issue Option B was meant to also resolve.

Output:
- `.planning/phases/3-aspect-ratio/3-07-UAT.md` — filled test matrix, head-unit model, build SHA, observed behaviors, PASS/PARTIAL/FAIL per row, screenshots or photos optional but encouraged.
- Either `status: passed` (all rows PASS — Phase 3 goal achieved) or `status: diagnosed` (one or more FAIL rows — `/gsd:plan-phase 3 --gaps` will pick this up next).
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/phases/3-aspect-ratio/3-RESEARCH.md
@.planning/ROADMAP.md
@.planning/REQUIREMENTS.md
@CLAUDE.md
@app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GLVideoPipeline.kt
@app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt
@app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt
</context>

<tasks>

<task type="manual">
  <name>Task 1: Build & install debug APK on head unit</name>
  <files></files>
  <read_first>
    - CLAUDE.md (build commands — "./gradlew installDebug" and adb start command)
    - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GLVideoPipeline.kt (confirm exists from Plan 06)
  </read_first>
  <action>
    1. Capture the build SHA: `git rev-parse --short HEAD` — record this in 3-07-UAT.md frontmatter as `build_sha`.
    2. Build & install the debug variant on the connected head unit (or phone in projection mode):
       ```
       ./gradlew installDebug
       ```
       Expected: BUILD SUCCESSFUL. APK installed as `com.pscholer.autoplayer.debug`.
    3. Start the app on the car display:
       ```
       adb shell am start -n com.pscholer.autoplayer.debug/.SettingsActivity
       ```
       (Settings activity is the LAUNCHER; navigate to playback from there per normal user flow.)
    4. Begin a logcat capture for the duration of the test session. Save to `.planning/phases/3-aspect-ratio/3-07-logcat.txt` (gitignored — not checked in):
       ```
       adb logcat -v time | grep -E "pscholer|AutoMedia|VideoSurface|GLVideoPipeline|MediaPlayerManager|FinalShaderWrapper|setFrameRate" > .planning/phases/3-aspect-ratio/3-07-logcat.txt &
       ```
       Keep this running across all subsequent tasks. Stop with `kill %1` (or `Ctrl+C` if foreground) when the matrix is complete.
    5. Record the head-unit model in 3-07-UAT.md frontmatter as `head_unit:` — exact OEM + model string from `adb shell getprop ro.product.model` and `getprop ro.product.manufacturer`.
  </action>
  <verify>
    <manual>
      - `./gradlew installDebug` exits 0
      - App icon visible on the head-unit launcher
      - Logcat is capturing (file size grows when you scroll the app)
    </manual>
  </verify>
  <acceptance_criteria>
    - `git rev-parse --short HEAD` recorded in 3-07-UAT.md
    - `adb shell pm list packages | grep com.pscholer.autoplayer.debug` returns the package
    - 3-07-logcat.txt exists and is non-empty after launching the app
    - 3-07-UAT.md frontmatter contains `head_unit:` and `build_sha:` fields
  </acceptance_criteria>
  <done>App is installed on the target head unit, logcat is capturing, and the build is identified for the matrix below.</done>
</task>

<task type="manual">
  <name>Task 2: Run the verification matrix — record PASS/FAIL per row</name>
  <files>.planning/phases/3-aspect-ratio/3-07-UAT.md</files>
  <read_first>
    - .planning/ROADMAP.md (Phase 3 UAT Criteria — exact bullets to verify)
    - .planning/REQUIREMENTS.md (Feature 4 acceptance criteria)
    - .planning/phases/3-aspect-ratio/3-RESEARCH.md (§"Validation Architecture" → "What CAN'T be unit tested" — the matrix below is the instrumented coverage)
  </read_first>
  <action>
    Create `.planning/phases/3-aspect-ratio/3-07-UAT.md` with this exact structure (fill in observations as you go). Each row in the matrix below MUST have a result.

    For each test row:
    1. Pick a sample asset matching the row's aspect ratio (use a local file, Plex, or Jellyfin source — sources are equivalent for surface rendering).
    2. Play the asset via the normal user flow (Settings → Browse → tap item).
    3. Observe what fills the screen. Take a photo of the head unit display if possible.
    4. Record the row outcome:
       - **PASS** — video shows in correct DAR; bars (if any) are on the expected edges; no distortion.
       - **PARTIAL** — works but with a caveat (e.g. minor letterbox on a "should-fill" case, off-by-a-few-pixels centering).
       - **FAIL** — wrong aspect ratio, distortion, black screen, freeze, or crash.

    Verification matrix (copy into 3-07-UAT.md):

    ```markdown
    ---
    phase: 3
    plan: 07
    status: pending          # update to "passed" / "diagnosed" / "partial" at end of session
    head_unit: <fill from Task 1>
    build_sha: <fill from Task 1>
    tested_at: <ISO timestamp>
    tested_by: <human name or "owner">
    ---

    # Phase 3 UAT — Aspect Ratio Preservation on Head Unit

    ## Test Matrix

    | # | Aspect | Source / Asset | Mode | Expected | Observed | Result | Notes |
    |---|--------|----------------|------|----------|----------|--------|-------|
    | 1 | 16:9   | <asset name>   | FIT  | Fills width, ≤2px letterbox if any (visible-area aspect dependent) | | | |
    | 2 | 4:3    | <asset name>   | FIT  | Pillarbox bars on left+right, video centered, no distortion | | | |
    | 3 | 21:9 (ultra-wide) | <asset name> | FIT | Letterbox bars top+bottom, video centered, no horizontal squeeze | | | |
    | 4 | 9:16 (vertical) | <asset name> | FIT | Strong pillarbox, video fully visible (not cropped), correct orientation | | | |
    | 5 | Anamorphic (PAR ≠ 1.0) | <asset name or N/A if no asset> | FIT | Display AR follows `width × PAR / height`, NOT `width / height` | | | |
    | 6 | 16:9 PLAY #2+ regression | Play any 16:9, stop, play another 16:9 | FIT | Second playback starts within ~1s; no 2s Qualcomm hang; logcat free of "Surface teardown timeout suppressed" spam | | | |
    | 7 | Visible-area change | Trigger template chrome (action strip toggle) mid-playback | FIT | Video re-letterboxes/pillarboxes correctly to new visible area; no flash, no distortion | | | |
    | 8 | FILL mode | 16:9 asset on FIT-mismatched container | FILL | EXPECTED N/A — phase-3 ships FIT only; `MediaPlayerManager.setScalingMode` is NOT plumbed end-to-end this phase (per Plan 06 Task 3 deferred-ideas note). Mark N/A unless a debug toggle has been added. If toggled: crops long axis, no bars, aspect preserved. | | N/A | Out of scope this phase — do NOT mark FAIL |
    | 9 | STRETCH mode | 16:9 asset | STRETCH | EXPECTED N/A — same as row 8. If toggled: fills container, intentional distortion, no bars. | | N/A | Out of scope this phase — do NOT mark FAIL |

    ## Logcat sanity checks

    Grep `3-07-logcat.txt` after the matrix is complete:

    - [ ] No occurrences of `FinalShaderWrapper: Output surface and size not set, dropping frame.` (the original Media3-effects regression — must be ZERO)
    - [ ] No occurrences of `IGraphicBufferProducer::setFrameRate(0.00) returned Function not implemented` (or if present, ONLY from non-pipeline code paths — annotate)
    - [ ] `GLVideoPipeline` logs show: `attach`, `setVideoSize`, `setVisibleArea` events firing in the expected order (attach before first setVideoSize)
    - [ ] `MediaPlayerManager` does NOT log `Surface teardown timeout suppressed (>1500ms)` between sequential playbacks (PLAY #2+ regression check)
    - [ ] **Feature 4.1 explicit check** — for each test asset in rows 1–5, `MediaPlayerManager` logs `onVideoSizeChanged` (or equivalent VideoSize listener log) with `width=`, `height=`, `pixelWidthHeightRatio=` values that match the asset's encoded metadata (use `mediainfo <file>` or `ffprobe -show_streams <file>` to get expected values; record both expected and observed in the matrix Notes column). This is the only way to catch a silent regression where the player reports a stale or wrong VideoSize.

    Record findings:

    ```
    [findings here — paste relevant grep results]
    ```

    ## Photos / Screenshots

    Optional but encouraged. If captured, place under `.planning/phases/3-aspect-ratio/3-07-photos/` (gitignored).

    ## Outcome

    Set `status:` in frontmatter:
    - `passed` — All rows PASS, all logcat sanity checks pass.
    - `partial` — Some rows PARTIAL, no FAIL. (Note: FILL/STRETCH rows 8–9 may legitimately be `N/A` if the debug toggle isn't wired; that's not a fail of FIT mode and Phase 3 goal — annotate as such.)
    - `diagnosed` — One or more FAIL rows. List each FAIL row with its row number and observed behavior in a `## Diagnosed Gaps` section. This becomes input to `/gsd:plan-phase 3 --gaps`.
    ```

    Use the project's existing UAT.md conventions if any of the other phases produced one — match that style if it differs from the above template.
  </action>
  <verify>
    <manual>
      - 3-07-UAT.md exists with all 9 test-matrix rows filled in (Result column not empty for any row that wasn't N/A)
      - Logcat sanity checklist all checked or annotated
      - Frontmatter status is one of passed / partial / diagnosed
    </manual>
  </verify>
  <acceptance_criteria>
    - `.planning/phases/3-aspect-ratio/3-07-UAT.md` exists
    - 3-07-UAT.md contains the literal strings `16:9`, `4:3`, `21:9`, `9:16`, and `PLAY #2+`
    - 3-07-UAT.md contains a frontmatter `status:` field set to `passed`, `partial`, or `diagnosed` (NOT `pending`)
    - For every row marked FAIL: the Notes column contains a one-sentence description of observed behavior
    - 3-07-UAT.md frontmatter contains both `head_unit:` and `build_sha:` populated (not placeholders)
  </acceptance_criteria>
  <done>Test matrix executed end-to-end, results recorded, status decided.</done>
</task>

<task type="manual">
  <name>Task 3: Surface gaps for follow-up (only if status != passed)</name>
  <files>.planning/phases/3-aspect-ratio/3-07-UAT.md</files>
  <read_first>
    - .planning/phases/3-aspect-ratio/3-07-UAT.md (post-Task-2)
    - .planning/phases/3-aspect-ratio/3-RESEARCH.md (Open Questions + Common Pitfalls — many FAIL modes are pre-anticipated there)
  </read_first>
  <action>
    If 3-07-UAT.md status is `passed`: skip this task — Phase 3 is done. Move on to Task 4.

    If status is `partial` or `diagnosed`:

    1. Add a `## Diagnosed Gaps` section to 3-07-UAT.md if not already present.
    2. For each FAIL or PARTIAL row, write one entry:
       ```
       ### Gap N — Row {row#}: {short title}

       **Observed:** {what happened}
       **Expected:** {what should have happened, copied from the Expected column}
       **Logcat snippet (if relevant):**
       ```
       <paste 3–10 lines of relevant log>
       ```
       **Hypothesis:** {best guess at root cause — reference RESEARCH.md sections if applicable}
       **Fix scope:** {one-line: where the fix likely lives — e.g. "AspectRatioCalculator.computeVertexTransform — rotation 270° branch", "GLVideoPipeline.onVisibleAreaChanged — debounce", etc.}
       ```
    3. The `## Diagnosed Gaps` section is the input contract for `/gsd:plan-phase 3 --gaps` — that command reads it and produces fix plans.
  </action>
  <verify>
    <manual>
      - If status is `passed`: this task is auto-passed, no entries needed.
      - Otherwise: every FAIL row has a corresponding `### Gap N` entry with all 4 fields (Observed, Expected, Hypothesis, Fix scope).
    </manual>
  </verify>
  <acceptance_criteria>
    - If 3-07-UAT.md frontmatter status is `passed`: 3-07-UAT.md does NOT contain `## Diagnosed Gaps` (clean exit).
    - If status is `partial` or `diagnosed`: 3-07-UAT.md contains `## Diagnosed Gaps` AND at least one `### Gap` entry.
    - For every entry under `## Diagnosed Gaps`: the body contains the strings `**Observed:**`, `**Expected:**`, `**Hypothesis:**`, `**Fix scope:**`.
  </acceptance_criteria>
  <done>Either Phase 3 is cleanly passed, or every gap is captured in a structured form ready for /gsd:plan-phase 3 --gaps.</done>
</task>

<task type="manual">
  <name>Task 4: Stop logcat capture, commit UAT artifact</name>
  <files>.planning/phases/3-aspect-ratio/3-07-UAT.md</files>
  <read_first>
    - .planning/phases/3-aspect-ratio/3-07-UAT.md
  </read_first>
  <action>
    1. Stop the background logcat capture started in Task 1 (`kill %1` or Ctrl+C).
    2. Confirm `.planning/phases/3-aspect-ratio/3-07-logcat.txt` is in `.gitignore` (or is small enough to skip — but matrix output is what we want committed, not raw log).
       ```
       grep -q "3-07-logcat" .gitignore || echo ".planning/phases/3-aspect-ratio/3-07-logcat.txt" >> .gitignore
       ```
    3. Stage and commit the UAT artifact only:
       ```
       git add .planning/phases/3-aspect-ratio/3-07-UAT.md .gitignore
       git commit -m "docs(phase-3): record head-unit UAT matrix (status: <status>)"
       ```
       Use the actual status value from the frontmatter in the commit message subject.
    4. Report the result back to the orchestrator (i.e. tell the human running this plan):
       - If status `passed`: "Phase 3 complete on head unit. Run `/gsd:progress` to advance."
       - If status `partial` or `diagnosed`: "Phase 3 has gaps. Run `/gsd:plan-phase 3 --gaps` to plan fixes."
  </action>
  <verify>
    <manual>
      - `git log -1 --oneline` shows the UAT commit
      - `git status` is clean (or only contains the gitignored logcat file)
      - The instruction echoed back to the human matches the UAT status
    </manual>
  </verify>
  <acceptance_criteria>
    - `git log -1 --oneline -- .planning/phases/3-aspect-ratio/3-07-UAT.md` returns a commit (the UAT was committed).
    - The most recent commit subject contains `docs(phase-3)` and `UAT`.
    - 3-07-logcat.txt path is in .gitignore (or the file is absent).
  </acceptance_criteria>
  <done>UAT artifact committed; next-step instruction handed back to the human.</done>
</task>

</tasks>

<verification>
- 3-07-UAT.md exists with full test matrix filled in.
- Frontmatter `status` is one of `passed` / `partial` / `diagnosed` (not `pending`).
- If `passed`: all 4 ROADMAP UAT criteria are checked off ("native aspect ratio", "no distortion", "real head unit", "no/minimal black bars").
- If not `passed`: every FAIL row has a `### Gap N` entry with Observed/Expected/Hypothesis/Fix-scope.
- Logcat sanity checks recorded.
- UAT artifact committed.
</verification>

<success_criteria>
- Phase 3 verified end-to-end on a real Android Auto head unit (not just the emulator — the prior Media3-effects bug was emulator-clean and head-unit-broken, so emulator-only verification is INSUFFICIENT).
- 16:9, 4:3, 21:9, 9:16 all render correctly (or every miss is documented as a gap).
- PLAY #2+ regression from the prior debug session does NOT recur.
- Result is machine-readable (frontmatter `status`) so `/gsd:progress` and `/gsd:plan-phase --gaps` can route off it.
</success_criteria>

<output>
Create `.planning/phases/3-aspect-ratio/3-07-SUMMARY.md` summarizing: head-unit model, build SHA, matrix outcome (X/9 PASS), any gap titles, and the next-step routing recommendation (continue / `--gaps` / re-test).
</output>
