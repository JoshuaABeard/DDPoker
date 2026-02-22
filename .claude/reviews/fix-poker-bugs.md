# Review Request

**Branch:** fix-poker-bugs
**Worktree:** C:/Repos/DDPoker-fix-poker-bugs
**Plan:** N/A (bug fixes, no plan required)
**Requested:** 2026-02-21 16:15

## Summary

Fixes 4 bugs observed during manual gameplay testing of practice games (WebSocket mode). The bugs ranged from visual glitches (community board flashing, showdown cards hidden) to incorrect result display (winner shown as loser) and game state errors (eliminated players still shown). Changes span server game logic, client display, and the WebSocket message protocol.

## Files Changed

- [x] `code/pokergamecore/src/main/java/com/donohoedigital/games/poker/core/TournamentEngine.java` — Bug 1: removed `hand.isDone()` from sleep-disable condition in `handleCommunity()`
- [x] `code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/TournamentEngineTest.java` — Updated test that was verifying the old (buggy) sleep behavior
- [x] `code/poker/src/main/java/com/donohoedigital/games/poker/Showdown.java` — Bug 2: guard `player.getHandInfo()` when community has < 3 cards (pre-flop uncontested win)
- [x] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/GameStateProjection.java` — Bug 3: skip sitting-out (eliminated) players when building player state list
- [x] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/message/ServerMessageData.java` — Bug 4: add `List<ShowdownPlayerData> showdownPlayers` to `ShowdownStartedData`
- [x] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/GameEventBroadcaster.java` — Bug 4: populate showdown player cards in SHOWDOWN_STARTED broadcast
- [x] `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java` — Bug 4: set opponent cards + `setCardsExposed(true)` before firing `TYPE_DEALER_ACTION` in `onShowdownStarted()`
- [x] `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/GameStateProjectionTest.java` — Regression test: sitting-out player excluded from snapshot
- [x] `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/websocket/message/ServerMessageSerializationTest.java` — Regression test: ShowdownStartedData serializes showdownPlayers field

**Privacy Check:**
- ✅ SAFE - No private information found. All changes are game logic / protocol changes.

## Verification Results

- **Tests:** 1623/1624 passed (1 pre-existing failure in `WebSocketTournamentDirectorTest.actionTimeoutFiresPlayerActionEvent` — also fails on `main` branch)
- **Coverage:** Not measured (dev profile used)
- **Build:** Clean after `spotless:apply`

## Context & Decisions

**Bug 1 root cause:** `hand.isDone()` returns true when all players are all-in. This caused the COMMUNITY phase to skip the inter-street sleep, so FLOP/TURN/RIVER were broadcast in rapid succession and the board appeared to flash with different cards.

**Bug 2 root cause:** For uncontested pre-flop wins, `TournamentEngine.handleShowdown()` calls `hand.advanceRound()` which advances PRE_FLOP→FLOP and deals 3 community cards *server-side* without broadcasting them. The client community remains empty. `Showdown.displayShowdown()` then calls `player.getHandInfo()` → `PocketRanks.getInstance()` which throws with an empty community. Fix: guard the call site. We did NOT change `handleShowdown()` to use `setRound(SHOWDOWN)` (tried it — `setRound()` clears `playerBets` without calling `calcPots()` first, causing chip loss).

**Bug 3 root cause:** `eliminateZeroChipPlayers()` marks bust-out players `sittingOut=true` but does not clear their `allIn` flag. `deal()` skips sitting-out players, so the flag never resets. `GameStateProjection.forPlayer()` was iterating all non-null seats, including these players, producing `status=ALL_IN, chips=0` entries in the next hand's GAME_STATE.

**Bug 4 root cause:** `SHOWDOWN_STARTED` fires before `HAND_COMPLETE`. `displayShowdown()` runs on `SHOWDOWN_STARTED` (via `TYPE_DEALER_ACTION`). At that point, AI hole cards are not yet set (they arrive in `HAND_COMPLETE`). `bShowCards` evaluates to false for AI players. Fix: send showdown cards in `SHOWDOWN_STARTED` and expose them on the client before `displayShowdown()` runs.

**Wire format impact:** `ShowdownStartedData` gains a new `showdownPlayers` field. Old clients receiving a new-format message will silently ignore the new field (Jackson). The client `onShowdownStarted()` handler guards `d.showdownPlayers() != null`.

---

## Review Results

**Status:** NOTES

**Reviewed by:** Claude Opus 4.6 (review agent)
**Date:** 2026-02-21

### Findings

#### Strengths

- All four bugs are well-diagnosed with clear root cause analysis in the handoff.
- Bug 1 (TournamentEngine sleep): Minimal, correct fix. Removing `hand.isDone()` from the sleep-disable condition is the right call -- all-in runouts should still show community cards with pauses. Test updated to match new behavior.
- Bug 2 (Showdown pre-flop guard): Defensive guard on `hhand.getCommunitySorted().size() >= 3` prevents the NPE without changing server-side hand advancement logic. The decision not to use `setRound(SHOWDOWN)` (which clears `playerBets` prematurely) is well-documented.
- Bug 3 (GameStateProjection sitting-out): Clean one-line filter `!player.isSittingOut()` with a clear regression test.
- Bug 4 (Showdown cards): Sends cards in `SHOWDOWN_STARTED` and exposes them client-side before `displayShowdown()` runs. Wire format backward-compatible (Jackson ignores unknown fields; client null-checks `showdownPlayers`).
- Tests cover all four fixes: updated TournamentEngineTest, new GameStateProjectionTest, new ServerMessageSerializationTest, all passing.
- No scope creep -- changes are strictly limited to the four bugs.

#### Suggestions (Non-blocking)

1. **TypeScript `ShowdownStartedData` type not updated** (`code/web/lib/game/types.ts:328-330`): The Java `ShowdownStartedData` record gained a `showdownPlayers` field, but the TypeScript interface still only has `tableId`. The web client `gameReducer.ts` currently returns `state` unchanged for `SHOWDOWN_STARTED`, so this is not a runtime bug today. However, the type definition is now out of sync with the wire format. Should be updated for consistency and to support future web client showdown rendering.

2. **Test comment line break** (`TournamentEngineTest.java:1821-1823`): The comment has an odd line break mid-sentence (`"rapid\n        // community\n        // card flashing"`). This was likely auto-formatted by Spotless and is cosmetic only.

#### Required Changes (Blocking)

None. All changes are correct, well-tested, and properly scoped.

### Verification

- Tests: 1624 run, 1 failure (`WebSocketTournamentDirectorTest.actionTimeoutFiresPlayerActionEvent`) -- confirmed pre-existing on `main` branch (skipped when run individually on main). All new and modified tests pass.
- Coverage: Not measured (dev profile used, as noted in handoff).
- Build: Clean after Spotless formatting.
- Privacy: No private information (IPs, credentials, personal data) in any changed files. All changes are game logic and protocol.
- Security: Hole cards broadcast in `SHOWDOWN_STARTED` are only for non-folded players at showdown time -- this is public information. No card leak risk. Same pattern as existing `HAND_COMPLETE` broadcast.
