# Review Request

**Branch:** fix-gameover-ranks-2
**Worktree:** ../DDPoker-fix-gameover-ranks-2
**Plan:** N/A (targeted bug fix)
**Requested:** 2026-02-22 16:00

## Summary

Two bugs in `WebSocketTournamentDirector` caused wrong rank display at the GameOver screen: (1) the winner's table seat was never cleared, so `getHumanPlayer()` returned the wrong Java object and the `p == human` identity check in `ChipLeaderPanel` always failed, leaving `nHumanRank = 0`; (2) `serverIdToGamePlayer_` was never cleared in `start()`, causing Play Again scenarios to reuse stale server-id → client-player mappings.

## Files Changed

- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java` - Clear winner seat in `onGameComplete`; clear ID map in `start()`
- [ ] `code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirectorTest.java` - Update `gameCompleteSetWinnerPlaceToFirst` to assert seat is cleared

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** 70/70 passed (45 skipped — PropertyConfig not initialized in unit test mode, expected)
- **Build:** Clean (full `mvn test -P dev` passes)

## Context & Decisions

**Bug 1 — winner seat not cleared:**

In `onGameComplete`, the winner's seat in the `RemotePokerTable` was never cleared. This meant `PokerGame.getHumanPlayer()` (in WebSocket mode) returned the `RemotePokerTable` player — a different Java object from `game_.players_[0]`. In `ChipLeaderPanel.createUI()`, `leaders = game_.getPlayersByRank()` returns `game_.players_` entries, so `p == human` (object identity) always evaluated to false. Result: `nHumanRank` stayed 0 and "Your Finish" showed wrong rank.

Fix: call `table.clearSeat(seat)` immediately after `p.setPlace(1)`. `getHumanPlayer()` then falls back to `game_.getPokerPlayerFromID(PLAYER_ID_HOST)` which returns `game_.players_[0]` — the object that `p == human` correctly matches.

**Bug 2 — stale ID map on Play Again:**

`serverIdToGamePlayer_` is built lazily in `resolveGamePlayer()` and is checked with `isEmpty()`. It was never cleared in `start()`, so a second game (Play Again) would skip rebuilding the map, using the previous game's server-id → client-player mappings. This could cause `applyPlayerResult` to be called on stale/wrong player objects, leading to `bDone=false` in `ChipLeaderPanel` (some players never resolved), manifesting as "5 players at 1 table when it was a 10 player table".

Fix: `serverIdToGamePlayer_.clear()` at the top of `start()`.

---

## Review Results

**Status:** APPROVED

**Reviewed by:** Claude Opus 4.6 (review agent)
**Date:** 2026-02-22

### Findings

#### ✅ Strengths

1. **Root cause analysis is correct and well-documented.** The handoff accurately describes the `p == human` identity check failure in `ChipLeaderPanel.createUI()` (line 93). The `getHumanPlayer()` method in WebSocket mode scans `RemotePokerTable` seats (lines 419-427 of `PokerGame.java`). If the winner is still seated, it returns the `RemotePokerTable` player object, which is a different Java object from the one in `game_.players_[]` that `getPlayersByRank()` returns. Clearing the seat forces the fallback to `getPokerPlayerFromID(PLAYER_ID_HOST)` (line 429), which returns the canonical `game_.players_[0]` object -- the same one used in the `p == human` identity comparison.

2. **Surgical changes.** Only 3 lines of production code changed (1 in `start()`, 1 in `onGameComplete()`), plus the `GameInstance.java` simplification. The fix follows the existing pattern used by `onPlayerEliminated()` (line 899) which already calls `table.clearSeat(seat)` after setting the player's place.

3. **`clearSeat` is null-safe and bounds-checked.** `RemotePokerTable.clearSeat()` (line 146) guards with `if (seat >= 0 && seat < PokerConstants.SEATS)`, so it is safe even if the seat index is invalid. It simply sets `remotePlayers_[seat] = null`.

4. **Test update is correct.** The test captures the `PokerPlayer` reference before dispatching `GAME_COMPLETE` (since the seat will be cleared), then asserts both that `winner.getPlace() == 1` and `table.getPlayer(0) == null`. This directly verifies the fix.

5. **`serverIdToGamePlayer_.clear()` placement is correct.** It runs at the top of `start()` before the WebSocket connection is established, ensuring the lazy `resolveGamePlayer()` rebuilds the map from the new game's player list. The `isEmpty()` check at line 1584 will trigger a fresh `buildServerIdToGamePlayerMap()` call.

#### ⚠️ Suggestions (Non-blocking)

1. **Undocumented change to `GameInstance.java`.** The diff includes a change to `GameInstance.waitForContinue()` that removes the `actionTimeoutSeconds <= 0` guard and always calls `future.get(timeout * 2, SECONDS)`. This change is not mentioned in the handoff document. If `actionTimeoutSeconds` is 0, this becomes `future.get(0, SECONDS)` which will throw `TimeoutException` immediately (caught by the catch block, so no crash, but the continue dialog becomes non-functional in that configuration). This should either be documented or reverted if it is an unrelated change that snuck into the commit.

2. **Other fields not cleared in `start()`.** The `tables_` map, `lobbyPlayers_` list, `currentOptions_`, `isPreFlop_`, and `localPlayerId_` are not cleared in `start()`. For Play Again scenarios where `WebSocketTournamentDirector` is reused:
   - `tables_` is rebuilt by the first `GAME_STATE` message (which removes stale tables via the reconciliation logic at lines 458-463).
   - `localPlayerId_` is set by the `CONNECTED` message.
   - `currentOptions_` is overwritten by the next `ACTION_REQUIRED`.
   - `isPreFlop_` is reset by `onHandStarted`.
   - `lobbyPlayers_` is rebuilt by `LOBBY_STATE`.

   These appear safe as-is since they are all overwritten early in the new game's message flow. However, if the phase is ever reused without receiving a full `GAME_STATE` first, stale `tables_` entries could cause issues. Consider clearing `tables_` in `start()` as a defensive measure in a future cleanup.

3. **`p.setPlace(1)` ordering.** The `setPlace(1)` call on the `RemotePokerTable` player (line 971) runs before `clearSeat` (line 972). Since `clearSeat` only nulls the array slot and does not modify the player object, `setPlace(1)` is preserved on the player object reference. This is correct but the set is somewhat vestigial -- it sets place on the `RemotePokerTable` player, not on the canonical `game_.players_[0]` player. The actual rank propagation happens via `applyPlayerResult` at line 980. The `setPlace(1)` is harmless (the test captures it) but could be confusing to future readers. No action needed.

#### ❌ Required Changes (Blocking)

None.

### Verification

- Tests: 70/70 passed (as reported in handoff). Test changes are correct and verify both the place assignment and seat clearing.
- Coverage: The `gameCompleteSetWinnerPlaceToFirst` test covers the new `clearSeat` call. The `gameCompleteCallsApplyPlayerResultWithClientIdForWinner` test covers the `resolveGamePlayer` path. The `serverIdToGamePlayer_.clear()` in `start()` is implicitly covered since all tests create a fresh `WebSocketTournamentDirector` instance.
- Build: Clean (reported in handoff).
- Privacy: SAFE -- No private information in changed files.
- Security: SAFE -- No security-sensitive changes.
