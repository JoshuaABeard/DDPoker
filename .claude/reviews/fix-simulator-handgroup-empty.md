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

*[Review agent fills this section]*

**Status:**

**Reviewed by:**
**Date:**

### Findings

#### ✅ Strengths

#### ⚠️ Suggestions (Non-blocking)

#### ❌ Required Changes (Blocking)

### Verification

- Tests:
- Coverage:
- Build:
- Privacy:
- Security:
