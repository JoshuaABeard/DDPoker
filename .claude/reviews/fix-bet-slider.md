# Review Request

## Review Request

**Branch:** fix-bet-slider
**Worktree:** ../DDPoker-fix-bet-slider
**Plan:** N/A (targeted bug fix)
**Requested:** 2026-02-21

## Summary

The bet/raise slider in WebSocket practice mode was completely frozen after the first drag. Root cause: `RemotePokerTable.nMinChip_` defaulted to 0 because `addTable()` never called `setMinChip()`. This caused `chips % 0` (ArithmeticException) in `roundAmountMinChip`, which propagated before resetting the `bUpdating` re-entrancy guard, permanently locking out all subsequent slider events.

Two-part fix: set `minChip` on `RemotePokerTable` at creation, and add a defensive zero-guard in `roundAmountMinChip`.

## Files Changed

- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java` - Added `t.setMinChip(game_.getMinChip())` after `game_.addTable(t)` in `onGameState()`
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/PokerUtils.java` - Added `if (nMinChip <= 0) return chips;` guard in `roundAmountMinChip`
- [ ] `code/poker/src/test/java/com/donohoedigital/games/poker/PokerUtilsTest.java` - Added two tests covering zero-minChip and normal rounding
- [ ] `code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirectorTest.java` - Added `gameStateSetsMinChipOnRemoteTable` test

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** 86/86 passed (43 skipped — expected, PropertyConfig-dependent tests skip without full engine context)
- **Coverage:** N/A (targeted fix, no coverage run)
- **Build:** Clean

## Context & Decisions

**Why two fixes instead of one?**

The primary fix (`setMinChip` at creation) is the correct solution. The defensive guard in `roundAmountMinChip` is an independent safety net for any future caller that passes a table with `minChip == 0` — the function has no business dividing by zero regardless of how the table was set up. Both are needed: the first prevents the bug, the second prevents silent data corruption in any future misuse.

**`bUpdating` re-entrancy design:** The Swing `AmountPanel.stateChanged` uses `bUpdating` to prevent feedback loops when programmatically setting values. An uncaught exception between `bUpdating = true` and `bUpdating = false` permanently disables the panel. This is a pre-existing fragility in the original code — the fix targets the exception source rather than adding exception handling to the guard, keeping the change surgical.

---

## Review Results

**Status:** APPROVED

**Reviewed by:** Claude Opus 4.6
**Date:** 2026-02-21

### Findings

#### Strengths

1. **Correct root cause analysis.** The two-part fix precisely targets the actual bug: `RemotePokerTable.nMinChip_` defaulting to 0 because `onGameState()` never called `setMinChip()`, and the resulting `ArithmeticException` from `chips % 0` in `roundAmountMinChip`. The handoff document's explanation of how this permanently locks the `bUpdating` re-entrancy guard in `AmountPanel.stateChanged` (line 502-526 of `ShowTournamentTable.java`) is accurate and well-documented.

2. **Consistent with existing codebase patterns.** The `t.setMinChip(game_.getMinChip())` call in `WebSocketTournamentDirector.java:437` mirrors the exact same pattern used in `PokerGame.java:1131` and `PokerGame.java:1292` where normal (non-WebSocket) tables are created. This is the right fix in the right place.

3. **Defense-in-depth is justified.** The `if (nMinChip <= 0) return chips;` guard in `PokerUtils.roundAmountMinChip` (line 613-614) is a sound defensive measure. The function is `public static` and called from multiple locations (`ShowTournamentTable.java:523`, `Bet.java:402`, `HoldemHand.java:1528`, `HoldemHand.java:2754`). Protecting it against divide-by-zero is appropriate regardless of whether the caller correctly initializes the table's min chip.

4. **Surgical change.** The diff is 41 lines across 4 files: 1 production line in each of 2 files, plus well-targeted tests. No unrelated code was modified. No over-engineering.

5. **Good test coverage.** The `PokerUtilsTest` covers both the zero-guard edge case (which previously threw `ArithmeticException`) and the normal rounding behavior (exact, round-down, round-up at the half boundary). The `WebSocketTournamentDirectorTest.gameStateSetsMinChipOnRemoteTable` test verifies the primary fix by stubbing `mockGame.getMinChip()` to return 25 and asserting the table receives that value.

#### Suggestions (Non-blocking)

1. **Consider testing negative `nMinChip` values.** The guard uses `<= 0`, handling both zero and negative values, but the test only exercises `minChip == 0`. A single additional assertion like `Mockito.when(table.getMinChip()).thenReturn(-1)` followed by `assertThat(PokerUtils.roundAmountMinChip(table, 500)).isEqualTo(500)` would document the negative case. This is minor -- negative min chip values are unlikely in practice, and the guard clearly covers them.

#### Required Changes (Blocking)

None.

### Verification

- Tests: 86 passed, 0 failed, 43 skipped (PropertyConfig-dependent -- expected). Confirmed by reviewer.
- Coverage: N/A (targeted 2-line bug fix; no coverage regression risk)
- Build: Clean (BUILD SUCCESS, no warnings)
- Privacy: SAFE -- no private information, credentials, IPs, or personal data in the diff
- Security: SAFE -- no new attack surface, no user input handling changes, no serialization changes
