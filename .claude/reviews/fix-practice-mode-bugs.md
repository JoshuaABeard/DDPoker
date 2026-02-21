# Review Request

## Review Request

**Branch:** fix-practice-mode-bugs
**Worktree:** ../DDPoker-fix-practice-mode-bugs
**Plan:** .claude/plans/fix-practice-mode-bugs.md
**Requested:** 2026-02-20

## Summary

Four bugs in the practice mode UI were fixed: an assertion failure in `HandInfo.categorize()` when hole cards are momentarily empty during all-in board runout; an NPE in `DashboardAdvisor` when `RemoteHoldemHand.getTable()` returns null; a level timer stuck at 15:00 because `GameClock` was never started; and a false-positive stuck detection in the smoke test (timeout too short for all-in runout).

## Files Changed

- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/dashboard/MyHand.java` — Bug 1: add `asViewedBy.getHandSorted().size() < 2` guard so pre-flop fallback is used when hole cards aren't yet available
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/dashboard/DashboardAdvisor.java` — Bug 2: add `hh.getTable() != null` guard to prevent NPE when `RemoteHoldemHand.getTable()` returns null
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java` — Bug 3: start `GameClock` in `onGameState()` and reset+conditionally-start in `onLevelChanged()`
- [ ] `.claude/scripts/test-practice-game.sh` — Bug 4: increase default `STUCK_TIMEOUT` from 15s to 45s

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** 149/149 passed (8 skipped integration, 11 skipped slow) — BUILD SUCCESS
- **Coverage:** Not measured (fast dev profile)
- **Build:** Clean

## Context & Decisions

**Bug 1 (MyHand):** The guard falls through to the existing pre-flop message path, which is the safe no-op behaviour. Adding a 4th OR clause to the existing check was the minimal-touch fix.

**Bug 2 (DashboardAdvisor):** Added `hh.getTable() != null` check before `!hh.isAllInShowdown()` because the NPE occurs inside `re.execute(p)` → `PokerAI.getPostFlopPositionCategory()` → `hhand.getTable().getButton()`. The "no advice" fallback is correct for online/practice mode where the AI advisor isn't meaningful.

**Bug 3 (WebSocketTournamentDirector):** `GameClock.setSecondsRemaining()` fires a "set" event but does NOT call `start()` — that's a separate step. In `onGameState()`, the clock is only started if not already running (idempotent). In `onLevelChanged()`, the clock is always reset (so countdown restarts for the new level) and started only if stopped. `GameClock.start()` already guards against double-start internally but the explicit check makes intent clear.

**Bug 4 (test script):** 45s allows for all-in board runout (15-20s) plus normal turn processing time with margin.

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
