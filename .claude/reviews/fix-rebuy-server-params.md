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
