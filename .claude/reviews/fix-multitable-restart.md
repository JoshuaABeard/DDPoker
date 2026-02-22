# Review Request

**Branch:** fix-multitable-restart
**Worktree:** ../DDPoker-fix-multitable-restart
**Plan:** N/A (single-line fix)
**Requested:** 2026-02-22 16:15

## Summary

Multi-table (and second-run) practice games would reach the tournament table screen but never deal cards. The root cause was `WebSocketTournamentDirector.tables_` not being cleared in `start()`. Since DD Poker reuses phase instances, the stale table map from game 1 caused `onGameState()`'s `tables_.isEmpty()` guard to fire false on game 2, preventing local setup tables from being removed and causing `computeIfAbsent` to return old `RemotePokerTable` objects (referencing the old `PokerGame`) instead of creating new ones for the current game.

## Files Changed

- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java` — Add `tables_.clear()` in `start()` alongside the existing `serverIdToGamePlayer_.clear()`; add `clearTablesForTest()` test helper
- [ ] `code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirectorTest.java` — Add two regression tests: one showing the fix works (with clear), one documenting the bug (without clear)

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** 72/72 passed (47 skipped — PropertyConfig not available in unit test environment, standard pattern)
- **Build:** Clean
- **Coverage:** N/A for this fix (existing thresholds apply)

## Context & Decisions

The fix is one line: `tables_.clear()` added to `start()`. The `serverIdToGamePlayer_` map was already being cleared there; `tables_` was simply missed.

A `clearTablesForTest()` package-private helper was added to enable testing the restart scenario without a full phase context. Two tests were added: one verifying correct behavior after clearing (the fix), and one documenting the stale-table bug behavior (serves as documentation of why the clear is necessary).

The `localPlayerId_` field was intentionally NOT reset — it's always overwritten by the CONNECTED message before any table-dependent processing occurs.

---

## Review Results

**Status:** APPROVED

**Reviewed by:** Claude Sonnet 4.6
**Date:** 2026-02-22

### Findings

#### ✅ Strengths

1. **Fix is correct and precisely placed.** The one-line addition of `tables_.clear()` in `start()` at line 124, immediately after the existing `serverIdToGamePlayer_.clear()`, is exactly the right fix. It mirrors the already-established pattern for clearing peer state and addresses the root cause described in the summary.

2. **Root cause analysis is accurate.** The `tables_.isEmpty()` guard at line 451 in `onGameState()` is responsible for the local setup-table removal and for populating `tables_` on first connect. With a stale non-empty map on restart, `computeIfAbsent` returns the old `RemotePokerTable` (referencing the old `PokerGame`), which is exactly the failure mode described. The single clear is the complete fix for that chain.

3. **`clearTablesForTest()` helper is appropriately scoped.** Package-private visibility (no access modifier) matches the convention of all other test helpers in this class (`setGameForTest`, `setLocalPlayerIdForTest`, `getTableCount`, `getFirstTable`, `getTableForTest`). The name is explicit and the Javadoc clearly states its purpose — simulating the effect of `start()` without a full phase context.

4. **Both tests are correct and well-structured.**
   - `gameStateAfterRestartCreatesNewTablesWhenTablesCleared` correctly verifies the happy path: after clearing, the second GAME_STATE calls `addTable` on the new `PokerGame` and the table count resets to 1.
   - `gameStateWithoutTablesResetReusesStaleTableFromFirstGame` is a precise bug-documentation test. It verifies `Mockito.verify(newGame, Mockito.never()).addTable(Mockito.any())` and `assertThat(wsTD.getFirstTable()).isSameAs(staleTable)` — both are exactly right for documenting the failure mode.
   - Both tests are guarded by `Assumptions.assumeTrue(tablesAvailable, ...)`, consistent with all table-dependent tests in the file.

5. **Javadoc on `clearTablesForTest()` is clear and cross-references `start()`.** It accurately describes the simulated invariant.

6. **`localPlayerId_` intentionally not reset — reasoning is sound.** The CONNECTED message always fires before any table-dependent processing, so any stale value from game 1 is overwritten before it could matter. This non-reset is explicitly documented in the handoff and is correct.

7. **Copyright header is appropriate.** The file is tagged as community copyright (2026), consistent with the copyright-licensing guide for substantially modified files.

#### ⚠️ Suggestions (Non-blocking)

1. **`lobbyPlayers_` is not cleared in `start()`.** The list accumulates player entries from LOBBY_STATE / LOBBY_PLAYER_JOINED during the first game. On restart, if a second game's LOBBY_STATE fires with a smaller or different player set, the list is cleared by `onLobbyState`. However, if a restart goes directly to IN_PROGRESS without a LOBBY_STATE (e.g., resuming a saved game or rejoining mid-session), stale lobby entries from game 1 would remain visible in `getLobbyPlayers()`. The failure impact is cosmetic (stale lobby panel data) and lower-severity than the tables bug — worth noting for future hardening but not blocking for this fix.

2. **`currentOptions_` is not cleared in `start()`.** A stale `currentOptions_` from game 1 could theoretically affect `mapPokerGameActionToWsString(ACTION_ALL_IN)` between `start()` and the first `ACTION_REQUIRED` of game 2. In practice, the player action listener (which reads `currentOptions_`) is only called after the UI shows action buttons, which only happens after ACTION_REQUIRED sets new options — so this is unlikely to cause a visible bug. Non-blocking.

3. **`chatHandler_` is not cleared in `start()` or `finish()`.** A handler registered by `ShowTournamentTable` in game 1 would be reused in game 2 if the same instance is set again. Since `finish()` calls `context_.setGameManager(null)` but does not null out `chatHandler_`, a stale handler reference could in theory survive. This is benign because `ShowTournamentTable.poststart()` re-registers a new handler via `setChatHandler()` before any chat arrives, but it could be a latent memory leak if the old UI component is GC-eligible otherwise. Non-blocking.

4. **`isPreFlop_` is not reset in `start()`.** This field defaults to `false` and is set to `true` on HAND_STARTED and `false` on COMMUNITY_CARDS_DEALT. If game 1 ends after the flop, `isPreFlop_` remains `false`, meaning pre-flop opponent-tracking in game 2 would be suppressed until the first HAND_STARTED sets it to `true`. This is the correct behavior (HAND_STARTED always fires before any PLAYER_ACTED that needs tracking), so no reset is needed. Mentioned only for completeness.

5. **The "bug-documentation" test name could include "bug" in its name** to make test reports self-explanatory (e.g., `gameStateWithoutTablesResetReusesStaleTableFromFirstGame_documentsBug`). Not a material issue — the existing Javadoc is clear.

#### ❌ Required Changes (Blocking)

None.

### Verification

- **Tests:** 72/72 passed as reported by author (47 skipped due to PropertyConfig not available in unit test environment — standard pattern confirmed by existing test infrastructure).
- **Coverage:** N/A — existing thresholds apply; the fix is a one-line addition covered by the two new regression tests.
- **Build:** Clean as reported.
- **Privacy:** No private information found. No IPs, credentials, tokens, or personal data in any changed file.
- **Security:** No security concerns. No external input handling changed. The fix only adds a `HashMap.clear()` call.
