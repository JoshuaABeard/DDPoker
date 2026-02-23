# Review Request

**Branch:** fix-advisor-do-it
**Worktree:** ../DDPoker-fix-advisor-do-it
**Plan:** N/A (bug-fix branch)
**Requested:** 2026-02-23

## Summary

Fixed `ADVISOR_DO_IT` in the dev control server (`ActionHandler.handleAdvisorDoIt`): the action was accepted (HTTP 200) but did not advance the game state. The bug: `handleAdvisorDoIt` dispatched `ACTION_CALL` unconditionally instead of executing the advisor's actual AI recommendation. The fix mirrors `DashboardAdvisor.actButton_` exactly — in practice mode the current `Bet` phase's `doAI()` is called; in WebSocket mode `pp.getAction(false)` is used to obtain and dispatch the real `HandAction`.

## Files Changed

- [ ] `code/poker/src/dev/java/com/donohoedigital/games/poker/control/ActionHandler.java` — Fix: replace hardcoded `ACTION_CALL` dispatch with two-path logic mirroring `DashboardAdvisor.actButton_`. In practice mode (`Bet` phase active): delegate to `bet.doAI()`. In WebSocket mode (`playerActionListener != null`): get the human player's AI recommendation via `pp.getAction(false)` and dispatch it. Add imports for `Phase`, `Bet`, `HoldemHand`.

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** 1608/1608 passed (`mvn test -P dev` in code/)
- **Coverage:** Not run (dev-only file; no new production paths)
- **Build:** Clean
- **Scenario test:** `test-advisor-do-it.sh` passes — ADVISOR_DO_IT accepted, advisor recommends "Fold", hole cards change to new hand confirming the fold executed and a new hand was dealt.

## Context & Decisions

### Why the original implementation was wrong

The original code dispatched `ACTION_CALL` with amount 0 unconditionally, with a comment claiming that `Bet.doAI()` causes `PokerPlayer.getAction()` to return FOLD silently. This was incorrect — `Bet.doAI()` works correctly in practice mode, and the comment was written before `cheat.aifaceup=true` was understood to be required. The scenario test confirms that when `cheat.aifaceup=true` is set (as the test script does), `pp.getPokerAI()` is non-null and the advisor path works.

### Why mirror DashboardAdvisor.actButton_ exactly

The UI "Do It" button has worked correctly for both practice and WebSocket modes. Reusing its two-path logic is the safest, most correct approach. The `handActionToPokerGameAction` helper was already present in `ActionHandler` (unused, at line 297 in the original), confirming that this was the intended fix all along.

### The pre-existing `handActionToPokerGameAction` method

`ActionHandler` already contained an unused `handActionToPokerGameAction` static helper. This fix uses it in the WebSocket branch, eliminating the dead code.

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
