---
phase: 3-aspect-ratio
plan: 01
type: execute
wave: 0
depends_on: []
files_modified:
  - app/build.gradle.kts
autonomous: true
requirements: [Feature-4]
must_haves:
  truths:
    - "JUnit 4 is on the test classpath"
    - "./gradlew :app:testDebugUnitTest task exists and resolves dependencies cleanly"
  artifacts:
    - path: "app/build.gradle.kts"
      provides: "JUnit 4 test runtime"
      contains: "testImplementation(\"junit:junit:4.13.2\")"
  key_links:
    - from: "app/build.gradle.kts"
      to: "junit:junit:4.13.2"
      via: "testImplementation declaration in dependencies block"
      pattern: "testImplementation\\(\"junit:junit:4\\.13\\.2\"\\)"
---

<objective>
Wave 0a: Add JUnit 4 to `app/build.gradle.kts` so subsequent waves can run pure-JVM unit tests for `AspectRatioCalculator.computeVertexTransform()`. This is a tiny prerequisite plan, but it is the gate for Wave 1's tests.

Purpose: No test framework currently exists in the project. RESEARCH.md §"Wave 0 Gaps" calls this out explicitly. Without JUnit 4, Wave 1 (vertex transform tests) cannot compile.

Output: `app/build.gradle.kts` updated to declare `testImplementation("junit:junit:4.13.2")` and `testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.0.0")`. Verified by `./gradlew :app:testDebugUnitTest --tests "NonExistent"` running without missing-dep errors.
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/phases/3-aspect-ratio/3-RESEARCH.md
@CLAUDE.md
@app/build.gradle.kts
</context>

<tasks>

<task type="auto">
  <name>Task 1: Add JUnit 4 + kotlin-test-junit testImplementation entries</name>
  <files>app/build.gradle.kts</files>
  <read_first>
    - app/build.gradle.kts (entire file — only ~108 lines)
    - .planning/phases/3-aspect-ratio/3-RESEARCH.md (§"Validation Architecture" → "Wave 0 Gaps" — confirms exact dependency strings)
  </read_first>
  <action>
    Open `app/build.gradle.kts`. Locate the `dependencies { ... }` block (currently ends at line 107 with the Coil dependency). Add at the bottom of the dependencies block, immediately before the closing brace:

    ```kotlin
        // ─── Testing ─────────────────────────────────────────────────────────────
        testImplementation("junit:junit:4.13.2")
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.0.0")
    ```

    Do NOT change any existing dependency or version. Do NOT add `androidTestImplementation` — pure-JVM tests only for this phase. Do NOT modify `compileOptions`, `kotlinOptions`, or any other block.

    Concrete strings to write verbatim:
    - `testImplementation("junit:junit:4.13.2")`
    - `testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.0.0")`

    These two lines are the entire change. The kotlin-test-junit version `2.0.0` matches the project's Kotlin version (per CLAUDE.md "Kotlin 2.0.0").
  </action>
  <verify>
    <automated>./gradlew :app:dependencies --configuration testDebugUnitTestRuntimeClasspath | grep -E "junit:junit:4\.13\.2|kotlin-test-junit:2\.0\.0"</automated>
  </verify>
  <acceptance_criteria>
    - `app/build.gradle.kts` contains the literal string `testImplementation("junit:junit:4.13.2")`
    - `app/build.gradle.kts` contains the literal string `testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.0.0")`
    - `./gradlew :app:dependencies --configuration testDebugUnitTestRuntimeClasspath` lists `junit:junit:4.13.2`
    - No existing `implementation(...)` line in the dependencies block was modified (diff only adds lines, does not change them)
  </acceptance_criteria>
  <done>JUnit 4 declared as testImplementation; gradle dependency resolution succeeds.</done>
</task>

<task type="auto">
  <name>Task 2: Verify test task runs (smoke test)</name>
  <files></files>
  <read_first>
    - app/build.gradle.kts (post-edit, to confirm Task 1 landed)
  </read_first>
  <action>
    Run `./gradlew :app:testDebugUnitTest --tests "ThisTestDoesNotExist"` from the repo root. Expected: gradle resolves the test classpath successfully and reports "0 tests completed" (or "No tests found for given includes" — both acceptable). The exit code should be 0 OR the only failure should be "no matching tests" (NOT a missing-dependency / unresolved-reference error).

    If you see `Could not resolve junit:junit:4.13.2` or any compilation error referencing JUnit symbols, Task 1 failed and must be re-applied.

    Do NOT create any test files in this plan — that is Wave 1's job. This task only confirms gradle dependency resolution works end-to-end.
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --tests "ThisTestDoesNotExist" --quiet 2>&1 | grep -vE "Could not resolve" || true</automated>
  </verify>
  <acceptance_criteria>
    - `./gradlew :app:testDebugUnitTest --tests "ThisTestDoesNotExist"` does NOT print "Could not resolve junit:junit"
    - `./gradlew :app:testDebugUnitTest --tests "ThisTestDoesNotExist"` does NOT print "Could not resolve org.jetbrains.kotlin:kotlin-test-junit"
    - Test task completes (exit 0 or only "no tests found" failure — gradle dependency resolution succeeds)
  </acceptance_criteria>
  <done>JVM unit test task is invokable and dependency-clean for Wave 1.</done>
</task>

</tasks>

<verification>
- `app/build.gradle.kts` contains both new `testImplementation` lines.
- `./gradlew :app:dependencies --configuration testDebugUnitTestRuntimeClasspath` includes JUnit 4 + kotlin-test-junit.
- No regression in `./gradlew :app:assembleDebug` (sanity check that nothing else broke).
</verification>

<success_criteria>
- JUnit 4 4.13.2 + kotlin-test-junit 2.0.0 resolvable on testDebugUnitTestRuntimeClasspath.
- `./gradlew :app:testDebugUnitTest --tests "Anything"` runs without dependency errors.
</success_criteria>

<output>
Create `.planning/phases/3-aspect-ratio/3-01-SUMMARY.md` summarizing the build.gradle.kts diff and confirming gradle resolution.
</output>
