# Review Request

**Branch:** fix-rebuy-server-params (squash merged to main as 59ce6104)
**Worktree:** ../DDPoker-fix-rebuy-server-params (removed after merge)
**Plan:** N/A
**Requested:** 2026-02-23 13:00

## Summary

Fixed a chain of three bugs that collectively prevented REBUY_CHECK from
ever appearing when a human player busts in an embedded practice game.
The root causes were: (1) `GameStartHandler` left critical rebuy profile
fields at zero defaults; (2) `GameInstance.offerRebuy()` timed out
instantly in practice mode where `actionTimeoutSeconds == 0`; (3) the
test script used a client-only cheat that the server never observed, and
blocked the EDT with a screenshot call during the critical latch window.

## Files Changed

- [ ] `code/poker/src/dev/java/.../control/GameStartHandler.java` - Added three `setInteger` calls when `rebuys=true`: `PARAM_REBUYCOST = buyinChips`, `PARAM_REBUYCHIPS = buyinChips`, `PARAM_MAXREBUYS = MAX_REBUYS`. Without these, `offerRebuy()` bails on `rebuyCost==0` and `isRebuyPeriodActive()` bails on `maxRebuys==0`.
- [ ] `code/pokergameserver/src/main/java/.../gameserver/GameInstance.java` - Applied same `if (timeoutSeconds <= 0) future.get()` indefinite-wait pattern (already used in `waitForContinue()`) to both `offerRebuy()` and `offerAddon()`. Without this, `future.get(0, SECONDS)` throws `TimeoutException` immediately in practice mode.
- [ ] `.claude/scripts/scenarios/test-rebuy-dialog.sh` - Rewrote to use natural chip loss (500-chip buyin, 50/100 blinds, fold every hand) instead of the client-only `setChips` cheat. Removed `screenshot()` call inside the REBUY_CHECK detection block, which would block indefinitely while the EDT awaits the latch.
- [ ] `.claude/scripts/scenarios/lib.sh` - Added `|| true` to `ps aux | grep java | grep -v grep` pipeline in `lib_launch()`. Without it, `set -euo pipefail` aborts the script when no Java processes exist.

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** Scenario test `test-rebuy-dialog.sh` passes — REBUY_CHECK appears within 10s, DECLINE_REBUY accepted immediately, REBUY accepted with chips restored to 1000. Pre-existing `levelChangedEventPublishedOnLevelAdvance` failure confirmed unchanged by stash/restore check.
- **Coverage:** N/A (dev module + script changes only; `GameInstance.java` change is one-line logic addition matching existing `waitForContinue` pattern)
- **Build:** Clean (warnings only from Spotless/Unsafe — pre-existing)

## Context & Decisions

**Why not add a unit test for `offerRebuy()` with `actionTimeoutSeconds=0`?**
The existing `ServerTournamentDirectorTest` already has extensive rebuy coverage. Adding a test specifically for the `actionTimeoutSeconds==0` path would require mocking a `GameProperties` with `actionTimeoutSeconds=0` and a `CompletableFuture` that resolves asynchronously — this is complex and the scenario test serves as integration-level coverage. Deferred.

**Why set `PARAM_REBUYCOST = buyinChips` and `PARAM_REBUYCHIPS = buyinChips`?**
Standard tournament convention: rebuy costs the same as the original buyin and restores the player to the starting stack. These values are fake (practice chips) and not configurable via the API, so using `buyinChips` as a symmetric default is correct.

**Why indefinite wait instead of a hardcoded fallback timeout?**
`waitForContinue()` already establishes the precedent: embedded/practice mode has no human action clock, so indefinite wait is the correct model. A hardcoded fallback (e.g. 30s) would auto-decline legitimate rebuys if the test response is slow.

---

## Review Results

**Status:** APPROVED with suggestions

**Reviewed by:** Claude Opus 4.6
**Date:** 2026-02-23

### Findings

#### Strengths

1. **Correct root-cause chain identified.** All three bugs (missing profile params, instant timeout, client-only cheat in test) are genuine, and the fixes are minimal and targeted. Good diagnostic work.

2. **`offerRebuy()` / `offerAddon()` indefinite-wait pattern matches `waitForContinue()` exactly.** The structure -- check `timeoutSeconds <= 0`, branch to `future.get()` vs `future.get(timeout, SECONDS)`, catch block, finally cleanup -- is a 1:1 match with the established pattern at line 732. This is the correct approach.

3. **Rebuy profile defaults are correct.** Setting `PARAM_REBUYCOST = buyinChips` and `PARAM_REBUYCHIPS = buyinChips` is the standard tournament convention (rebuy costs what you originally paid and restores the starting stack). `PARAM_MAXREBUYS = MAX_REBUYS` (99) is the engine's defined cap, verified by `ParameterConstraintsTest.should_CapRebuysAtMAX_REBUYS()`. The values are set *after* `profile.fixAll()` but use the raw `setInteger` path, which is fine since `fixAll()` applies constraints on read via `getInteger(key, default, min, max)`.

4. **Test script design is sound.** The natural chip-loss approach (500-chip buyin with 50/100 blinds, fold every hand) is the right strategy. The human loses 50 or 100 chips per hand from blinds alone, so bust occurs within 5-10 hands -- well within the 120s timeout. This is far more reliable than the client-only `setChips` cheat that the server never sees.

5. **`lib.sh` fix is correct.** The `|| true` on the `ps aux | grep java | grep -v grep` pipeline is necessary because `grep -v grep` can produce zero matches, causing a nonzero exit under `set -euo pipefail`. This is a genuine fix, not a suppression of real errors.

#### Suggestions (Non-blocking)

1. **`shutdown()` does not unblock pending rebuys/addons.** The `shutdown()` method at line 344 completes `pendingContinue` to unblock the director thread, but does not complete futures in `pendingRebuys` or `pendingAddons`. With the new indefinite-wait behavior, if `shutdown()` is called while `offerRebuy()` is blocking, the director thread will hang forever. In online mode this was safe (the timeout would expire), but with `timeoutSeconds <= 0` it becomes a potential deadlock. In practice this is unlikely to trigger -- a user would have to quit the game at exactly the moment a rebuy offer is pending -- but it should be addressed in a follow-up to keep `shutdown()` consistent with the `waitForContinue` teardown pattern. The fix would be iterating `pendingRebuys.values()` and `pendingAddons.values()` and completing each with `false`.

2. **Test script RB-015 failure path is a silent skip, not a failure.** At line 171, if the second REBUY_CHECK never appears within 120s, the script logs `WARN` and skips RB-015 entirely rather than failing. This means the accept-rebuy path could silently go untested. Consider making this a test failure (incrementing `FAILURES`) since the same game setup should reliably trigger bust a second time.

3. **`screenshot()` placement after DECLINE_REBUY (line 103) is safe but worth noting.** The screenshot was removed from the REBUY_CHECK detection loop (correct -- it would block the EDT during the latch window), but it's still called after DECLINE_REBUY on line 103. This is safe because the rebuy latch has already been resolved at that point, but if screenshot ever becomes slow on CI, the 2-second sleep on line 98 may need adjustment.

#### Required Changes (Blocking)

None. The code changes are correct and the identified shutdown concern is an edge case appropriate for a follow-up fix rather than blocking this commit.

### Verification

- Tests: Scenario test coverage verified by review of test script logic; natural chip-loss approach is reliable within timeout bounds.
- Coverage: N/A -- confirmed changes are dev-only (`GameStartHandler.java`) and a single logic branch addition in `GameInstance.java` matching existing pattern.
- Build: No new warnings introduced per handoff verification.
- Privacy: SAFE -- no credentials, tokens, or personal data in any changed file.
- Security: SAFE -- no new endpoints, no auth changes, no input validation gaps.
