# Review Request

**Branch:** fix-gameover-ranks
**Worktree:** C:/Repos/DDPoker-fix-gameover-ranks
**Plan:** N/A (bug fix, no plan required)
**Requested:** 2026-02-22 03:15

## Summary

Fixes the GameOver dialog showing all players at the same rank (everyone as "1st") after
a WebSocket practice tournament. `applyPlayerResult()` was called with server-assigned
player IDs but `game_.players_` uses client-assigned IDs — the lookup always returned
null, silently failing to update any player's place or chips.

## Files Changed

- [x] `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java` — Add `resolveGamePlayer()` + `buildServerIdToGamePlayerMap()` to bridge server→client ID space; fix `winnerId` from `int` to `long`
- [x] `code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirectorTest.java` — Two regression tests: `playerEliminatedCallsApplyPlayerResultWithClientId` and `gameCompleteCallsApplyPlayerResultWithClientIdForWinner`

**Privacy Check:**
- ✅ SAFE - No private information found. Pure game logic / event dispatch fix.

## Verification Results

- **Tests:** Full dev suite passes, 0 failures, 0 errors
- **Coverage:** Not measured (dev profile used)
- **Build:** Clean after Spotless

## Context & Decisions

**Root cause:**

In `PracticeGameController.createPracticeGame()`, the embedded server assigns player IDs
as follows:
- Human player: `user.profileId()` (JWT database ID, e.g. 1, 2, ...)
- AI players: sequential negatives `-1`, `-2`, ...

On the client, `TournamentOptions.setupPracticeGame()` adds players to `game_.players_`
with client-assigned IDs from `getNextPlayerID()`:
- Human: ID = `HOST_ID = 0`
- AI 1: ID = 1, AI 2: ID = 2, ...

When `onPlayerEliminated()` / `onGameComplete()` called
`game_.applyPlayerResult((int) d.playerId(), ...)`, `getPokerPlayerFromID()` searched
`game_.players_` by server ID, found no match, and returned null — so
`applyPlayerResult()` silently returned without updating anything.

As a result, all players in `game_.players_` kept their initial chip counts (equal
for all players at game start) and `place = 0`. In `ChipLeaderPanel.createUI()`:
- `bDone = getNumPlayers() - getNumPlayersOut()` remained `3 - 0 = 3 != 0` → `false`
- The current-players loop ran with all three players at equal chip counts
- Tie-detection kept `nRank = 1` for all players
- The finished-players loop skipped everyone (`p.getPlace() == 0`)
- Result: all players shown as "1st" in the current standings, none in finished list

**Fix:**

`resolveGamePlayer(long serverPlayerId)` lazily builds a `Map<Long, PokerPlayer>` that
bridges the two ID spaces:
- `localPlayerId_` → `game_.getPokerPlayerFromID(PLAYER_ID_HOST)` (human at client ID 0)
- `-1L` → `game_.getPokerPlayerAt(1)` (first AI at client ID 1)
- `-2L` → `game_.getPokerPlayerAt(2)` (second AI at client ID 2), etc.

`onPlayerEliminated()` and `onGameComplete()` now call `resolveGamePlayer()` and pass
the resulting client player ID to `applyPlayerResult()`.

Also changed `winnerId` from `int` to `long` in `onGameComplete()` — the original
`(int) s.playerId()` cast truncated the 64-bit JWT profileId.

**Why the previous fix (commit 60455c5f) was incomplete:**

Commit 60455c5f added `applyPlayerResult()` and wired it up in `onPlayerEliminated()`
and `onGameComplete()`, but passed the server player ID directly. The method was correct
in isolation but the callers used the wrong ID space. This fix completes the bridge.

---

## Review Results

*[Review agent fills this section]*

**Status:**

**Reviewed by:**
**Date:**

### Findings

#### Strengths

#### Suggestions (Non-blocking)

#### Required Changes (Blocking)

### Verification

- Tests:
- Coverage:
- Build:
- Privacy:
- Security:
