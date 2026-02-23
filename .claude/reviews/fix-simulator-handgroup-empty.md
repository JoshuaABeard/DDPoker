# Review Request

**Branch:** fix/simulator-handgroup-empty (squash-merged to main)
**Plan:** N/A (bug fix)
**Requested:** 2026-02-23 09:45

## Summary

Fixed two production bugs uncovered by the scenario test suite, plus two test script bugs. `HandGroup(File, boolean)` never called `clearContents()`, leaving `pairs_`/`suited_`/`offsuit_` null for empty groups loaded from file, causing NPE in `expand()`. `HoldemSimulator.simulate()` computed `Math.log(0)` for empty groups, producing an invalid handCount. On the test side, `curl -f` swallowed 4xx response bodies, and tournament profile name extraction used the wrong JSON path.

## Files Changed

- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/HandGroup.java` - Call `clearContents()` at start of `read()` to ensure arrays initialized for empty groups
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/HoldemSimulator.java` - Skip groups with 0 hands in simulate loop (avoids `Math.log(0)`)
- [ ] `.claude/scripts/scenarios/lib.sh` - Change `curl -f` to `curl --fail-with-body` so 4xx response bodies are captured
- [ ] `.claude/scripts/scenarios/test-tournament-profile-editor.sh` - Fix name extraction: `o.profile?.name` not `o.name`

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** test-simulator PASS, test-tournament-profile-editor PASS (17/17), all other passing tests unaffected
- **Coverage:** Not measured (bug fixes only, no new code paths)
- **Build:** Clean

## Context & Decisions

**HandGroup NPE root cause:** `HandGroup(File, boolean)` calls `super(file, bFull)` → `load()` → `read()`. The `read()` method only calls `clearContents()` (via `parse()`) when the file has a "hands" key. Empty groups (classCount==0) are saved without a "hands" key by `write()`, so `pairs_` stays null. Fixed by calling `clearContents()` unconditionally at the start of `read()` — `parse()` calls it again when hands exist, which is harmless (idempotent reinit).

**HoldemSimulator log(0):** After the HandGroup NPE fix, empty groups return an empty HandList from `expand()`. Computing `handCount = (int)(pow(2,p) * (log(0) + 0.5) * 1000)` yields `Integer.MIN_VALUE`. Added `if (list.size() == 0) { continue; }` to skip empty groups cleanly.

**curl --fail-with-body:** curl 8.18.0 is available. This change makes all `api()` calls in test scripts capture error JSON for `|| true` callers, while still exiting non-zero for `|| die` callers — no behavior change for existing passing tests.

**Stale file:** A `handgroup.1001.dat` leftover from an interrupted earlier test was manually deleted. The HandGroup fix ensures future empty group files won't cause crashes.

---

## Review Results

**Status: APPROVED**

**Reviewed by:** Claude Opus 4.6
**Date:** 2026-02-23

### Findings

#### ✅ Strengths

**HandGroup.read() fix is correct and minimal.**
The root cause analysis is accurate. `HandGroup(File, boolean)` delegates to `super(file, bFull)` which calls `load()` → `read()`. The `read()` method called `super.read()` and then branched on whether a `"hands"` key was present. For empty groups saved without a `"hands"` key (because `write()` omits it when `classCount_ == 0`), `pairs_`/`suited_`/`offsuit_` were never initialized, leaving them null. Calling `clearContents()` unconditionally at the start of `read()` closes this gap cleanly (line 589 in `HandGroup.java`). The fix is surgical — one line added, nothing else touched.

The double-call concern (`parse()` calls `clearContents()` again for non-empty groups) is correctly assessed as harmless: `clearContents()` is idempotent (it always sets arrays to newly allocated zero-filled arrays and resets counters to zero). Calling it twice before data is loaded has no adverse effect.

**HoldemSimulator empty-group skip is correct and safe.**
`Math.log(0)` returns `-Infinity`, which when cast to `int` yields `Integer.MIN_VALUE`. Passing that as `handCount` to the sampling loop would produce undefined behavior (looping 2 billion iterations or an immediate underflow). The added guard (lines 103-107 in `HoldemSimulator.java`) short-circuits cleanly before that computation, increments `nNumDone` to keep the progress bar consistent, then calls `perc()` and `continue`. This matches the expected behavior exactly: an empty group simply contributes nothing to the simulation, no crash.

**curl --fail-with-body is the right tool for the job.**
The old `-f` / `--fail` flag causes curl to exit non-zero on 4xx/5xx responses and suppresses the response body, which makes test debugging significantly harder. `--fail-with-body` (available since curl 7.76.0; the environment has 8.18.0) exits non-zero on HTTP errors while still printing the response body. The switch does not change the success/failure semantics for any existing `|| die` call sites — they still get exit code propagation. It only improves the `|| true` callers by making the error JSON readable in logs. This is a clean, correct improvement to the testing infrastructure.

**test-tournament-profile-editor.sh JSON path fix is correct.**
The API response for a successful profile creation returns `{ "created": true, "profile": { "name": "..." } }`, not `{ "created": true, "name": "..." }`. The corrected expression `o.profile?.name||""` (line 31) matches the actual response structure. The optional-chaining (`?.`) is appropriate defensive coding since `profile` could be absent on failure paths, preventing a runtime error in the `jget` node snippet.

**Progress accounting for skipped groups is correct.**
When an empty group is skipped, `nNumDone++` and `perc()` are called before `continue`, so the progress bar advances normally. This mirrors the structure at the end of the normal (non-empty) path and avoids leaving the progress counter behind.

#### ⚠️ Suggestions (Non-blocking)

**No test for the empty-group file round-trip.**
`HandGroupTest.java` has thorough unit tests for in-memory construction and parse logic, but no test exercises the `HandGroup(File, boolean)` constructor path with an empty group. The bug — `pairs_` being null when `read()` is called on a file with no `"hands"` key — would not be caught by any existing test. A test that writes an empty `HandGroup` to a temp file and reads it back via `HandGroup(file, false)`, then asserts `expand()` returns an empty `HandList` without throwing, would pin this regression permanently. This is a suggestion, not a blocker, since the fix is clearly correct on inspection.

**HoldemSimulator: no test for the log(0) guard.**
Similarly, there is no unit test that feeds a simulate call a `HandGroup` with zero hands and verifies it is skipped without error. Given that `HoldemSimulator.simulate()` relies on file-system hand groups, a direct unit test may be difficult, but an integration-level test or a focused test using a mocked/injected list would be valuable to prevent regression.

**Comment on the `clearContents()` double-call is informative but could be clearer.**
The inline comment on line 589 (`// ensure pairs_/suited_/offsuit_ are initialized even for empty groups`) is accurate. It would be marginally clearer to note that `parse()` will call it again for non-empty groups and that the repetition is harmless, but this is a style preference and not required.

#### ❌ Required Changes (Blocking)

None.

### Verification

- **Tests:** Reported PASS for test-simulator and test-tournament-profile-editor (17/17). Existing tests unaffected. Accepted on author's attestation; no regressions evident from code inspection.
- **Coverage:** Not measured for this bug-fix PR; acceptable given the nature of the changes.
- **Build:** Reported clean.
- **Privacy:** SAFE — no private data present in any changed file.
- **Security:** No security concerns. The curl change actually improves auditability of error responses. No injection vectors introduced.
