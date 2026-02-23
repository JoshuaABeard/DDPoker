# Review Request

**Branch:** fix-broadcaster-table-index
**Worktree:** ../DDPoker-fix-broadcaster-table-index
**Plan:** N/A (bug fix, no plan needed)
**Requested:** 2026-02-22 18:50

## Summary

Fixes a bug where AI opponents' cards were never revealed at showdown. The root cause: commit `4d1e43de` made `table.getNumber()` return 1-based numbers, but `GameEventBroadcaster` still used `e.tableId()` (now 1-based) as a 0-based index into `getTournament().getTable()`. For any single-table game, the bounds check `1 < getNumTables()=1` was always false, so `showdownPlayers` was always an empty list — meaning `SHOWDOWN_STARTED` never carried card data, and `setCardsExposed(true)` was never called on opponents.

The same off-by-one also affected community cards, winner data, player name lookups in PLAYER_ACTED/PLAYER_JOINED, and chips-transferred player name lookups.

## Files Changed

- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/GameEventBroadcaster.java` — 8 call sites changed from `getTable(e.tableId())` to `getTable(e.tableId() - 1)`, bounds checks updated from `>= 0 && < N` to `> 0 && (- 1) < N`
- [ ] `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/websocket/GameEventBroadcasterTest.java` — 2 existing tests updated to use 1-based table numbers; 1 new regression test `showdownStarted_1basedTableId_looksUpTableAtIndex0` added

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** 610/610 passed (`mvn test -pl pokergameserver -P dev`)
- **Coverage:** Not checked (no new branches introduced)
- **Build:** Clean

## Context & Decisions

The 8 affected call sites all follow the exact same pattern: a bounds-guard followed by `getTable(e.tableId())`. The fix is uniform across all of them: subtract 1 from `e.tableId()` before using it as an index, and tighten the lower bound from `>= 0` to `> 0` (since valid 1-based table numbers start at 1).

The `PlayerAdded` event at line 368 (consolidation path) previously had no bounds check at all — this was also fixed, preventing a potential `IndexOutOfBoundsException` during multi-table consolidation.

The `ActionTimeout` handler (line 415) was NOT changed: it already iterates tables with a separate 0-based loop variable `t` and never uses `e.tableId()` as an index. It is correct.

---

## Review Results

**Status:** APPROVED

**Reviewed by:** Claude Opus 4.6 (review agent)
**Date:** 2026-02-22

### Findings

#### ✅ Strengths

- **Root cause correctly identified and fixed.** The `TournamentContext.getTable(int index)` interface explicitly documents a 0-based index parameter. Events are fired with `table.getNumber()` which returns 1-based numbers (set to `i + 1` in `ServerTournamentContext.createTables()`). The `e.tableId() - 1` conversion is the correct fix.

- **All 8 call sites fixed uniformly.** Every place that previously called `getTable(e.tableId())` now calls `getTable(e.tableId() - 1)` with consistent bounds checking. Verified by diff inspection that no `getTable(e.tableId())` calls remain.

- **Bounds check change from `>= 0` to `> 0` is correct.** Since `e.tableId()` is now known to be 1-based, the minimum valid value is 1 (not 0). The `> 0` lower bound correctly rejects invalid table IDs of 0 or below.

- **Pass-through of `e.tableId()` to outbound messages is correct.** The 1-based table number is preserved in outbound messages (lines 230, 248, 295, 302, 326, 378, 408, 466) since the client expects 1-based table numbers. Only the internal `getTable()` lookup is converted.

- **`PlayerAdded` now has a bounds check.** Previously this handler called `getTable(e.tableId())` without any guard, risking `IndexOutOfBoundsException`. The fix adds the same bounds-check pattern used by all other handlers.

- **`ActionTimeout` correctly left unchanged.** This handler uses a 0-based loop variable `t` for iteration and never indexes by `e.tableId()`. No change needed.

- **Existing test updates are correct.** `playerActed_withGameReference_populatesPlayerName` now creates a `ServerGameTable(1, ...)` (1-based number) and fires `PlayerActed(1, ...)` (1-based table ID). `playerAdded_withGame_includesPlayerName` now fires `PlayerAdded(1, ...)` and stubs `getNumTables()` and `getTable(0)` correctly.

- **New regression test directly validates the fix.** `showdownStarted_1basedTableId_looksUpTableAtIndex0` fires `ShowdownStarted(1)` against a single-table tournament and verifies `getTable(0)` is called, which is precisely the scenario that was broken.

#### ⚠️ Suggestions (Non-blocking)

- **Consider a helper method.** The pattern `e.tableId() > 0 && (e.tableId() - 1) < game.getTournament().getNumTables()` followed by `getTable(e.tableId() - 1)` is repeated 8 times. A small private helper like `getTableForEvent(int tableId)` returning `Optional<ServerGameTable>` would reduce duplication and the risk of future off-by-one errors. Not required for this fix since the current code is clear and consistent.

- **Additional regression test coverage.** The new test covers `ShowdownStarted` specifically. Other critical paths like `HandCompleted` (winner/showdown data) and `CommunityCardsDealt` follow the same pattern, so the single test is sufficient to validate the fix approach, but a parameterized test covering multiple event types would strengthen confidence.

#### ❌ Required Changes (Blocking)

None.

### Verification

- Tests: 24/24 passed (`GameEventBroadcasterTest`, confirmed by review agent)
- Coverage: N/A (no new branches; fix changes existing conditional logic)
- Build: Clean (confirmed by test run)
- Privacy: SAFE - No private information in changes
- Security: SAFE - No security-sensitive changes
