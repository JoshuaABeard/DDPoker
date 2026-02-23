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

**Status:** APPROVED

**Reviewed by:** Claude Opus 4.6
**Date:** 2026-02-23

### Findings

#### Strengths

1. **Exact mirror of proven UI code.** The `handleAdvisorDoIt` implementation is a faithful reproduction of `DashboardAdvisor.actButton_`'s two-path logic: `Bet.doAI()` for practice mode, `pp.getAction(false)` + `playerActionPerformed` for WebSocket mode. The structure, guard checks (`pp.isHumanControlled()`, `pp.getPokerAI() != null`), and dispatch call are identical.

2. **Correct use of `SwingUtilities.invokeLater`.** The original code called `dispatchPlayerAction` which already uses `invokeLater`, but the new code needs EDT access for `context.getCurrentPhase()` and other Swing-adjacent state. Wrapping the entire block in `invokeLater` is correct and consistent with the `handleRebuy`/`handleAddon` patterns in the same file.

3. **Eliminates dead code.** The pre-existing `handActionToPokerGameAction` helper was previously unused. This fix gives it a purpose, and its mapping is identical to `DashboardAdvisor.toPokerGameAction`.

4. **Thorough null guards.** Every intermediate object (`main`, `context`, `game`, `hh`, `pp`, `aiAction`) is null-checked before use.

5. **Good comments.** The block comment explaining the two-path logic and why each path exists makes the intent clear for future maintainers.

#### Suggestions (Non-blocking)

1. **Silent no-op when neither branch matches.** If the current phase is not `Bet` AND `getPlayerActionListener()` is null, the `invokeLater` block silently does nothing but the HTTP response still says `accepted: true`. This matches the DashboardAdvisor behavior (the button simply does nothing in edge cases), and the `isBettingInputMode` guard above already ensures we are in a valid betting state. However, if desired for debuggability, you could log a warning when neither branch is taken. This is non-blocking since the scenario is unlikely in practice mode (the Bet phase should always be active when the input mode is a betting mode).

2. **`handActionToPokerGameAction` mapping for non-player actions.** The constants `ACTION_BLIND_BIG` (6), `ACTION_BLIND_SM` (7), `ACTION_ANTE` (8), `ACTION_WIN` (9), `ACTION_OVERBET` (10), and `ACTION_LOSE` (11) are not in the switch and fall through to the `default -> ACTION_FOLD` case. This is correct because `pp.getAction(false)` will never return these dealer/system actions from the AI recommendation path. The default-to-FOLD is a safe fallback consistent with `DashboardAdvisor.toPokerGameAction`. No change needed.

#### Required Changes (Blocking)

None.

### Verification

- **Tests:** 1608/1608 passed (confirmed in handoff)
- **Coverage:** N/A (dev-only source set, no production code changed)
- **Build:** Clean
- **Privacy:** SAFE - no private information, credentials, or secrets in the diff
- **Security:** SAFE - the handler is behind the existing `X-Control-Key` authentication in `BaseHandler`, and the `isBettingInputMode` guard prevents misuse outside valid betting states
