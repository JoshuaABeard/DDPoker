# Review Request

**Branch:** fix-dashboard-updates
**Worktree:** C:/Repos/DDPoker-fix-dashboard-updates
**Plan:** N/A (bug fixes, no plan required)
**Requested:** 2026-02-21 17:20

## Summary

Fixes 3 bugs that prevented dashboard panels from updating during WebSocket
practice games. All three stem from timing/event-dispatch mismatches between
the legacy local-engine event system and the WebSocket-based remote mode.

## Files Changed

- [x] `code/poker/src/main/java/com/donohoedigital/games/poker/dashboard/MyHand.java` — Bug 1: skip timer-based `clear()` on `TYPE_NEW_HAND` for remote tables
- [x] `code/poker/src/main/java/com/donohoedigital/games/poker/dashboard/Odds.java` — Bug 2: use `hhand.getCurrentPlayer()` instead of `hhand.getPlayerAt(event.getNew())`
- [x] `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java` — Bug 2: capture old player index before `updateCurrentPlayer()` and fire `TYPE_CURRENT_PLAYER_CHANGED` with correct `(oldIndex, newIndex)` constructor
- [x] `code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirectorTest.java` — Regression test: `TYPE_CURRENT_PLAYER_CHANGED` event carries correct `getNew()` when local player is not at index 0
- [x] `code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/TournamentEngineTest.java` — chore: Spotless reformatted a comment (no logic change)

**Privacy Check:**
- ✅ SAFE - No private information found. All changes are game logic / event dispatch fixes.

## Verification Results

- **Tests:** Full dev suite passes, 0 failures
- **Coverage:** Not measured (dev profile used)
- **Build:** Clean after Spotless

## Context & Decisions

**Bug 1 root cause (MyHand blank after new hand):**
`TYPE_NEW_HAND` and `TYPE_DEALER_ACTION` fire in the same `SwingUtilities.invokeLater()` task in `onHandStarted()`. `TYPE_DEALER_ACTION` queues a `SWING_SYNC` via `invokeLater()`. `TYPE_NEW_HAND` registers a timer task (delay=0) that blocks via `invokeAndWait()` to call `clear()`. The EDT queue order becomes: [SWING_SYNC, clear()]. Result: SWING_SYNC updates MyHand correctly, then clear() erases it.

Fix: skip `clear()` when `event.getTable().isRemoteTable()` is true. SWING_SYNC from `TYPE_DEALER_ACTION` will populate the correct preflop state. Legacy local-engine behavior is unchanged (clear() still runs there; the delay between TYPE_NEW_HAND and the next dealer phase is long enough that clearing is appropriate UX).

**Bug 2 root cause (Odds panels never update):**
`RemotePokerTable.fireEvent(int type)` constructs a `PokerTableEvent(type, this)` with `nOne_=nTwo_=0`. In `onActionRequired()`, the code called `table.fireEvent(TYPE_CURRENT_PLAYER_CHANGED)`, so `event.getNew()` always returned 0. `Odds.tableEventOccurred()` calls `hhand.getPlayerAt(event.getNew())`, which was always player at index 0, regardless of who the human was. Panels only updated if the human happened to be at hand index 0.

Same bug affected `ShowTournamentTable.tableEventOccurred()` which calls `hhand.getPlayerAt(event.getOld())` and `hhand.getPlayerAt(event.getNew())` for seat repainting on player change.

Fix 1 (WebSocketTournamentDirector): Capture `oldPlayerIndex = hand.getCurrentPlayerIndex()` before `updateCurrentPlayer(playerIndex)` and fire with `new PokerTableEvent(TYPE_CURRENT_PLAYER_CHANGED, table, oldPlayerIndex, playerIndex)`. ShowTournamentTable's seat-repaint handler now also gets correct indices.

Fix 2 (Odds.java): Change `hhand.getPlayerAt(event.getNew())` to `hhand.getCurrentPlayer()`. This is more resilient regardless of how the event is fired (defends against future callers too).

**Why not fix ShowTournamentTable separately?**
With Fix 1, `ShowTournamentTable` gets correct indices in the event. No additional change needed there.

**Note on Rank, CheatDash, AdvanceAction:**
- Rank subscribes to `TYPE_NEW_HAND` and `TYPE_PLAYER_CHIPS_CHANGED`, both of which fire correctly in WebSocket mode. Rank should work.
- CheatDash toggles save to prefs and trigger `PREFS_CHANGED` → `SWING_SYNC`. Functional. "AI Face Up" is a known limitation: AI hole cards are only sent at showdown via `HAND_COMPLETE`, not during the hand.
- AdvanceAction clears buttons on `TYPE_NEW_HAND`/`TYPE_DEALER_ACTION` and is populated when the human acts. Should work.

---

## Review Results

**Status:** APPROVED

**Reviewed by:** Claude Opus 4.6 (review agent)
**Date:** 2026-02-21

### Findings

#### Strengths

1. **Correct root cause analysis and minimal fixes.** Both bugs are well-understood and the fixes address the root causes directly. The handoff document explains the EDT queue ordering issue (Bug 1) and the zero-default event indices issue (Bug 2) clearly.

2. **Defense in depth for Odds.java.** Changing `hhand.getPlayerAt(event.getNew())` to `hhand.getCurrentPlayer()` is a good defensive change. Even though the WebSocketTournamentDirector fix now provides correct indices, `Odds` no longer depends on event indices at all, making it resilient to any future caller that fires `TYPE_CURRENT_PLAYER_CHANGED` without proper indices.

3. **Consistent with existing codebase patterns.** The `WebSocketTournamentDirector` fix mirrors exactly how `HoldemHand.java:1162` fires the same event type: `new PokerTableEvent(TYPE_CURRENT_PLAYER_CHANGED, table_, nOld, nNew)`. The constructor signature `(int, PokerTable, int, int)` at `PokerTableEvent.java:227` is correct.

4. **Good regression test.** `actionRequiredFiresCurrentPlayerChangedWithCorrectIndices` sets up a two-player scenario where the local player is at hand index 1 (not 0), which is the exact failure case. It verifies both `getNew()` (playerIndex=1) and `getOld()` (NO_CURRENT_PLAYER=-999), confirming the old index is captured before `updateCurrentPlayer()` mutates it.

5. **No scope creep.** Changes are surgical -- only the files needed are touched. The `ShowTournamentTable` consumer is correctly left unchanged since it now receives correct indices from the event. The `TournamentEngineTest.java` change is just a Spotless reformat of a comment.

6. **Legacy behavior preserved.** The `MyHand.java` fix guards on `isRemoteTable()`, so local-engine games still get the `clear()` call with its appropriate UX timing.

#### Suggestions (Non-blocking)

1. **No test for Bug 1 (MyHand clear skip).** The `MyHand.java` change lacks a direct unit test. This is understandable -- `MyHand` is a Swing `DashboardItem` that extends a UI component and would require extensive mocking of the rendering pipeline. The fix is straightforward (one condition added) and the risk is low. Consider adding a test if `MyHand` is ever refactored to be more testable.

#### Required Changes (Blocking)

None.

### Verification

- Tests: PASS -- `WebSocketTournamentDirectorTest`: 58 run, 0 failures, 0 errors (42 skipped due to headless/environment constraints, expected)
- Coverage: Not measured (dev profile); acceptable for bug fixes with regression test
- Build: PASS
- Privacy: SAFE -- No private data, credentials, IPs, or personal information in any changed file
- Security: SAFE -- No new endpoints, no user input handling changes, no authentication changes
