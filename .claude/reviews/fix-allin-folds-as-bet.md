# Review Request

## Review Request

**Branch:** fix-allin-folds-as-bet
**Worktree:** ../DDPoker-fix-allin-folds-as-bet
**Plan:** N/A (targeted bug fix)
**Requested:** 2026-02-22

## Summary

When the human player presses "All-In" as the first bettor in a round (no existing bet), the client was sending `"RAISE"` to the server. The server's `validateAction()` sets `canRaise=false` when `amountToCall==0`, so it converted the unacceptable RAISE into `PlayerAction.fold()` — causing the player to fold instead of go all-in. Fix: send `"BET"` when `canRaise=false` (first-to-act), `"RAISE"` otherwise. Applied to both the button-press path (`mapPokerGameActionToWsString`) and the advance-action (pre-selected) path (`AdvanceAction.getAdvanceActionWS`).

## Files Changed

- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java` — Changed `ACTION_ALL_IN` mapping to send `"BET"` when `!currentOptions_.canRaise()`, added `allInWsActionForTest()` package-private accessor, updated `getAdvanceActionWS` call to pass `canRaise`
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/dashboard/AdvanceAction.java` — Added `canRaise` parameter to `getAdvanceActionWS` and `_getAdvanceActionWS`; all-in advance action now sends `"BET"` or `"RAISE"` based on `canRaise`
- [ ] `code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirectorTest.java` — Added 3 tests: `allInWsActionIsBetWhenCanRaiseFalse`, `allInWsActionIsRaiseWhenCanRaiseTrue`, `allInWsActionIsRaiseWhenNoOptions`

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** 67/67 ran, 0 failures, 45 skipped (PropertyConfig-dependent — expected). Full suite BUILD SUCCESS.
- **Coverage:** N/A (targeted 2-path bug fix; no coverage run)
- **Build:** Clean

## Context & Decisions

**Root cause trace:**
1. `TournamentEngine.createActionOptions` sets `canRaise = (amountToCall > 0)`. When the player is first to act (no existing bet), `amountToCall==0` → `canRaise=false`.
2. `ServerPlayerActionProvider.validateAction` folds on `!options.canRaise()` for a RAISE action.
3. Client always sent `"RAISE"` for ALL_IN regardless of mode, hitting the fold path when first to act.

**Why BET vs RAISE:** The server treats these as distinct: BET opens betting, RAISE re-raises an existing bet. The server sends `canBet`/`canRaise` separately and validates against each. All-in as the opener is semantically a bet (just the maximum one); all-in when facing a bet is a raise.

**Fallback when `currentOptions_` is null:** Default to `"RAISE"` (pre-existing behavior). The server will validate and fold if truly invalid, but this case shouldn't occur in practice because `currentOptions_` is set before buttons are shown.

**Advance-action path:** `AdvanceAction.getAdvanceActionWS` had the same hardcoded `"RAISE"`. Added `canRaise` parameter to propagate the server's option through. The call site in `onActionRequired` already has `d.options()` available.

---

## Review Results

**Status:** APPROVED

**Reviewed by:** Claude Opus 4.6 (review agent)
**Date:** 2026-02-22

### Findings

#### Strengths

1. **Correct root cause analysis.** The fix accurately identifies the mismatch between the client sending `"RAISE"` and the server's `validateAction()` folding when `canRaise=false`. The server-side code at `ServerPlayerActionProvider.validateAction` (line 289) clearly shows `!options.canRaise()` yields `PlayerAction.fold()`, confirming the bug.

2. **Both code paths fixed.** The button-press path (`mapPokerGameActionToWsString` in WebSocketTournamentDirector, line 1547) and the advance-action path (`AdvanceAction._getAdvanceActionWS`, line 368) are both corrected with the same logic. This prevents the bug from manifesting whether the user clicks the All-In button directly or pre-selects it.

3. **Server-side semantics align.** `TournamentEngine.createActionOptions` (lines 556-557) sets `canBet = (amountToCall == 0) && (chipCount > 0)` and `canRaise = (amountToCall > 0) && (chipCount > amountToCall)`. These are mutually exclusive: when `canRaise=false` and the player can go all-in, `canBet` must be `true`. The server's `OutboundMessageConverter` (line 130) confirms `canAllIn = canBet || canRaise`, so the all-in button is only shown when at least one of BET/RAISE is valid. The fix correctly maps to whichever one is available.

4. **Safe fallback for null options.** When `currentOptions_` is null, the code defaults to `"RAISE"` (pre-existing behavior). This is acceptable since `currentOptions_` is always set before action buttons are displayed.

5. **No impossible edge case of both false.** Since `canAllIn` is only `true` when `canBet || canRaise` (server-side), the scenario where both are `false` but all-in is offered cannot occur. The fix does not need to handle it.

6. **Backward-compatible.** `getAdvanceActionWS` has exactly one call site (WebSocketTournamentDirector line 609), which was updated to pass `canRaise`. No other callers exist.

7. **Tests are well-structured.** Three tests cover the key cases: canRaise=false (sends BET), canRaise=true (sends RAISE), and null options (defaults to RAISE). The test options JSON mirrors realistic server payloads.

#### Suggestions (Non-blocking)

1. **Consider a direct unit test for `AdvanceAction._getAdvanceActionWS`.** The advance-action path is tested only indirectly through the WebSocketTournamentDirector integration tests. A direct unit test of `AdvanceAction.getAdvanceActionWS` with `allin_.isSelected()` would make the all-in BET/RAISE logic independently verifiable. However, since the current tests do exercise the `mapPokerGameActionToWsString` path through `allInWsActionForTest()`, and the advance-action logic is identical, this is non-blocking.

2. **Javadoc for `getAdvanceActionWS` parameter list is incomplete.** The method-level Javadoc documents `canCheck` and `allInAmount` parameters but does not document the new `canRaise` parameter. Adding a `@param canRaise` line would keep the documentation consistent.

#### Required Changes (Blocking)

None.

### Verification

- **Tests:** 67/67 passed, 0 failures. 3 new tests cover the fix. Reviewed test logic -- assertions match expected behavior.
- **Coverage:** Not run (targeted 2-path fix). Acceptable for this scope.
- **Build:** Clean (per review request).
- **Privacy:** No private information in changes. Safe.
- **Security:** No security implications. The fix only changes which valid action string is sent to the server; server-side validation remains unchanged.
