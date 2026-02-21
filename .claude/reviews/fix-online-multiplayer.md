# Review Request

## Review Request

**Branch:** fix-online-multiplayer
**Worktree:** ../DDPoker-fix-online-multiplayer
**Plan:** N/A (code review findings)
**Requested:** 2026-02-20

## Summary

Seven bugs in the online multiplayer pipeline were fixed across three files: `WebSocketTournamentDirector.java` (client-side message handler), `GameEventBroadcaster.java` (server-side event→WebSocket), and `GameWebSocketHandler.java` (WebSocket connection lifecycle). These bugs were identified by code review comparing the online multiplayer path to the recently-fixed practice mode path. Tests were added for all fixes.

## Files Changed

- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java` — Fixes 1, 2, 4, 5, 6 (client), 7
- [ ] `code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirectorTest.java` — Tests for fixes 1, 2, 4, 5, 6 client; also adds GameClock mock to setUp so clock-calling tests work when PropertyConfig is available
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/GameEventBroadcaster.java` — Fix 6 (server): PlayerRemoved consolidation suppression + PlayerAdded name lookup
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/GameWebSocketHandler.java` — Fix 3: pass `game` to broadcaster in reconnect path
- [ ] `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/websocket/GameEventBroadcasterTest.java` — Tests for fixes 3 and 6 server

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** Full reactor BUILD SUCCESS, 0 failures, 0 errors across all modules
- **Coverage:** Not measured (dev profile)
- **Build:** Clean

## Context & Decisions

**Fix 1 (onPlayerActed chip count):** Removed the narrow guard (`chipCount > 0 || isFold || isBlindAnte`). The server is authoritative on chip counts; a player going all-in via BET/RAISE correctly returns `chipCount=0` which the guard previously skipped. The fix always applies the server-provided chip count unconditionally.

**Fix 2 (GameClock pause/unpause):** Added `game_.getGameClock().pause()` / `unpause()` calls in `onGamePaused` / `onGameResumed`. `GameClock.pause()` internally calls `stop()` and sets `bPaused_=true`; `unpause()` calls `start()` and clears the flag. This matches the semantics of the existing timer toggle in `ShowPokerNightTable`.

**Fix 3 (GameWebSocketHandler null-game broadcaster):** The reconnect path's `computeIfAbsent` was creating the broadcaster with the 3-arg constructor (null `game`). Changed to the 4-arg constructor passing `game`. This ensures `HandStarted` sends per-player GAME_STATE snapshots, `CommunityCards` resolves live cards, and `LevelChanged` resolves correct blind amounts — all of which are no-ops when `game==null`.

**Fix 4 (onGameCancelled navigation):** Added `context_.processPhase("PracticeGameOver")` with null guard, matching the pattern in `onGameComplete`. This navigates the client away from the table screen when the host cancels the game. The phase name "PracticeGameOver" is shared between practice and online modes since both use the same director; this may need a dedicated phase for M6 online.

**Fix 5 (deliverChatLocal):** Replaced the no-op stub with `chatHandler_.chatReceived(id, nType, sMessage)` guarded by `chatHandler_ != null`. This routes incoming server CHAT_MESSAGE events through the existing ChatHandler pathway to the chat panel.

**Fix 6 (table consolidation, server + client):**
- *Server (GameEventBroadcaster)*: For `PlayerRemoved`, checks if the player is still active in the tournament (`getFinishPosition() == 0`). If yes (consolidation), suppresses `PLAYER_LEFT` so clients don't show a permanent removal. Eliminated players (`finishPosition > 0`) still receive `PLAYER_LEFT` to clear their seat. For `PlayerAdded`, looks up the player's name from the game table so `PLAYER_JOINED` contains the correct name (was `""` before).
- *Client (WebSocketTournamentDirector)*: In `onPlayerJoined`, before adding a player to their new seat, scans all tables and clears their old seat (if any) to prevent the player appearing in two seats simultaneously. This is a no-op for new joins (no existing seat).

**Fix 7 (mapActionToWsString warn log):** Added `logger.warn(...)` to the `default` case matching the pattern already used in `mapPokerGameActionToWsString`. The default still yields `"FOLD"` as the safe fallback.

---

## Review Results

**Status:** APPROVED WITH SUGGESTIONS

**Reviewed by:** Claude Opus 4.6
**Date:** 2026-02-20

### Findings

#### ✅ Strengths

1. **Fix 1 (chip count) is correct and well-reasoned.** The server is authoritative on chip counts. Removing the narrow guard that skipped `chipCount=0` for BET/RAISE all-in scenarios is the right fix. The unconditional `player.setChipCount(d.chipCount())` is simpler, more correct, and eliminates an entire class of edge cases. The removal of the local `act` variable is clean since it was only used in the removed guard; `d.action()` is now called directly in the fold check.

2. **Fix 2 (GameClock pause/unpause) uses the correct API.** Verified that `GameClock.pause()` sets `bPaused_=true` then calls `stop()`, and `unpause()` clears `bPaused_` then calls `start()`. Using `pause()`/`unpause()` rather than raw `stop()`/`start()` is the right choice because it preserves the paused flag that UI code queries via `isPaused()`. This matches the semantics in `ShowPokerNightTable`.

3. **Fix 3 (reconnect-path broadcaster) is a critical one-line fix.** Passing `game` to the 4-arg constructor ensures the reconnect-path broadcaster has access to the game instance for resolving per-player GAME_STATE snapshots on HandStarted, community card lookups on CommunityCardsDealt, and blind amounts on LevelChanged. Without this, those branches all fall through to the `game == null` paths that produce empty/zero data.

4. **Fix 5 (deliverChatLocal) parameter mapping is correct.** The `ChatManager.deliverChatLocal(int nType, String sMessage, int id)` correctly maps to `ChatHandler.chatReceived(int fromPlayerID, int chatType, String message)` as `chatReceived(id, nType, sMessage)`. Verified against the `ChatHandler` interface and `ChatPanel.chatReceived()` implementation. The null guard prevents NPE when no chat panel is registered.

5. **Fix 6 consolidation detection logic is sound.** The `finishPosition == 0` check correctly identifies active (non-eliminated) players. In `ServerTournamentDirector`, `setFinishPosition(survivors + 1)` is called in the cleaning phase for eliminated players (0 chips), and the `PlayerRemoved` event for consolidation is published in the table-balancing code at line 591 where the player has NOT been eliminated. There is no race condition: the `PlayerRemoved` event is handled synchronously on the broadcaster, and `finishPosition` is set before any `PlayerRemoved` for eliminated players (elimination happens in the cleaning phase, consolidation happens after).

6. **Client-side seat-clearing logic in onPlayerJoined is defensive and correct.** The loop scans all tables and clears old seats where `(t != table || oldSeat != d.seatIndex())`, correctly handling both same-table consolidation (player moves to a different seat) and cross-table consolidation (player moves to a different table). The condition also correctly skips clearing when the player is already at the target seat (no-op for duplicate PLAYER_JOINED).

7. **Fix 7 is trivial and correct.** The warn log matches the existing pattern in `mapPokerGameActionToWsString`.

8. **Tests are thorough and well-structured.** Each fix has dedicated test coverage. The tests for fix 1 cover both BET and RAISE all-in scenarios. Fix 2 tests verify mock clock interactions. Fix 5 tests cover both the direct `deliverChatLocal` call and the end-to-end `CHAT_MESSAGE` dispatch path. Fix 6 tests cover both server-side (active suppression, eliminated pass-through, name lookup, null-game fallback) and client-side (seat clearing on consolidation). The GameClock mock addition to setUp is a sensible infrastructure improvement that prevents NPE in clock-dependent code paths.

#### ⚠️ Suggestions (Non-blocking)

1. **Fix 4 (PracticeGameOver for cancelled online games):** Using `"PracticeGameOver"` as the phase name for a cancelled online game works but is semantically misleading. The handoff already acknowledges this ("may need a dedicated phase for M6 online"). For M6, consider introducing a distinct `"OnlineGameCancelled"` phase or at minimum a shared `"GameOver"` phase (which already exists in `gamedef.xml` as an extension of `PracticeGameOver`). For now this is acceptable since both phases route to the same `GameOver` dialog class, but it could cause confusion if the phases diverge in the future.

2. **Fix 6 server: consider logging when consolidation suppresses PLAYER_LEFT.** The suppression is silent. Adding a debug-level log like `logger.debug("[BROADCAST] suppressing PLAYER_LEFT for consolidation playerId={}", e.playerId())` would aid debugging multi-table balancing issues without any runtime cost.

3. **Fix 6 client: the `findSeat` scan is O(tables * seats) per PLAYER_JOINED.** With the 10-seat max and typically 1-4 tables, this is negligible. But if the table count ever grows significantly (e.g., large online tournaments), this could be optimized with a player-to-seat index. Not needed now, just noting for future awareness.

4. **Test for fix 6 client: consider adding a cross-table consolidation test.** The current test (`playerJoinedClearsOldSeatBeforeAddingToNew`) covers same-table seat movement. A test with two tables where the player moves from table A to table B would exercise the cross-table path of the seat-clearing loop. This is a minor gap since the logic is straightforward, but would increase confidence.

#### ❌ Required Changes (Blocking)

None. All seven fixes are correct, complete, and well-tested.

### Verification

- **Tests:** PASS - Full reactor BUILD SUCCESS, 0 failures, 0 errors across all modules (as reported in handoff)
- **Coverage:** Not measured (dev profile) - acceptable for bug fixes
- **Build:** PASS - Clean build
- **Privacy:** PASS - No private information in changed files; no credentials, tokens, or PII
- **Security:** PASS - No security concerns; all changes are internal game-state plumbing with no external attack surface changes
