Status: complete

## Session Work Log (2026-02-20)

### Completed this session (commits 22934c57, 09e12f3f)

**Data quality / rendering gaps:**
- `GameStateSnapshot` — expanded from 6 to 15 fields: `dealerSeat`, `smallBlindSeat`,
  `bigBlindSeat`, `currentActorSeat`, `bettingRound`, `level`, `smallBlind`, `bigBlind`,
  `ante`
- `GameStateProjection` — populates all new fields from live table/hand/tournament
- `OutboundMessageConverter` — uses real data instead of hardcoded zeros/false
- `GameEventBroadcaster` — enriched `HandStarted` (real SB/BB seats), `HandCompleted`
  (winner data from `PotResolutionResult`), `LevelChanged` (actual blind amounts)
- `RemoteHoldemHand` — added SB/BB seat tracking fields + accessors
- `WebSocketTournamentDirector.onHandComplete` — credits winner chip counts, clears
  pot/bets; `onHandStarted` and `applyTableData` — set SB/BB seats on remote hand

**Missing events:**
- `GameEvent.PlayerEliminated` record added to sealed interface
- `ServerTournamentDirector.eliminateZeroChipPlayers` — publishes events with finish
  position; `handleGameOver` — publishes elimination events for final-hand busts
  (engine may bypass DONE→BEGIN transition when last player wins)
- `GameEventBroadcaster` — broadcasts `PLAYER_ELIMINATED` to clients
- `SwingEventBus` — exhaustive switch cases for `PlayerEliminated`
- `ServerHand` — added `getSmallBlindAmount/getBigBlindAmount/getAnteAmount` getters
- `ServerTournamentDirector.processTable` — publishes `PlayerActed(BLIND_SM/BB/ANTE)`
  after `HandStarted` so client chip counts update before first `ACTION_REQUIRED`

### Also completed this session (commits 61cc07c4, 33d4d584)

**Client-side correctness fixes:**
- `WebSocketTournamentDirector.onPlayerActed` — extended chipCount update condition to
  include `BLIND_SM/BLIND_BIG/ANTE` actions so all-in blind posting sets chipCount=0
- `WebSocketTournamentDirector.onGameComplete` — sets winner's `place=1` before
  triggering `PracticeGameOver`; guarded `context_.processPhase()` with null check for
  testability
- `WebSocketTournamentDirector.mapPokerGameActionToWsString` — added
  `PokerGame.ACTION_ALL_IN → "RAISE"` mapping (was falling through to FOLD); added
  warn log for truly unknown constants

**Tests added:**
- `WebSocketTournamentDirectorTest.blindAllInUpdatesChipCountToZero` — verifies
  `chipCount=0` is applied when blind posted all-in
- `WebSocketTournamentDirectorTest.gameCompleteSetWinnerPlaceToFirst` — verifies
  `place=1` is set on survivor when `GAME_COMPLETE` received
- `GameEventBroadcasterTest.playerEliminated_broadcastsToAllPlayers` and
  `broadcastsCorrectMessageType_playerEliminated`
- `ServerTournamentDirectorTest.playerEliminatedEventsPublished` and
  `blindPostingEventsPublished`

**Known remaining gaps:**
- `GAME_COMPLETE` sends empty standings (no per-player finish positions); lower priority
  since places are correctly set via `PLAYER_ELIMINATED` + `onGameComplete`
- `TIMER_UPDATE` never sent by server (client countdown NYI; not needed for practice mode)

---

# Fix Practice Mode — Server + Embedded Client

## Problem

After the M1–M6 server-hosted game engine work, practice mode (embedded Spring Boot
server inside the desktop Swing client) doesn't work reliably. The
`fix-embedded-autoconfiguration` branch contains initial fixes but the game still
gets stuck or misbehaves during multi-hand play.

## Approach

Use the **Game Control Server** (dev HTTP API at `src/dev/java/.../control/`) to
drive practice games programmatically — no human UI interaction needed. This lets
us reproduce issues deterministically, fix them, and verify fixes with an automated
test loop. The `GET /screenshot` endpoint provides visual verification without manual
testing. The control surface can be extended as needed during this work.

All fixes target fidelity with the original `TournamentDirector` design.

## Branch

Create a new worktree from main (which includes the merged `fix-embedded-autoconfiguration`):

```bash
git worktree add -b fix-practice-mode ../DDPoker-fix-practice-mode
```

## Completed (merged to main as 94e94513)

The following fixes were merged as part of `fix-embedded-autoconfiguration`:
- Embedded server autoconfiguration (`@Import` instead of `AutoConfiguration.imports`)
- `maven-shade-plugin` replacing `maven-assembly-plugin`
- `ServerHand.deal()` resetting `folded`/`allIn` flags between hands
- `mapPokerGameActionToWsString()` fixing `PokerGame.ACTION_*` vs `HandAction.ACTION_*`
- Rate-limiter `removePlayer()` clearing stale timestamps after valid actions
- Local table cleanup on first `GAME_STATE`
- Debug logging across the full action pipeline
- `desktop-client-testing.md` guide

Game successfully plays 70+ human actions end-to-end. Known remaining issue:
end-of-tournament detection (player elimination events not yet wired to client).

## Remaining Work

---

## Phase 1: Automated Test Harness

**Goal:** Create a reliable, repeatable test loop that plays a complete practice
game via the Game Control Server — no Swing interaction.

### 1.1 Create integration test script

Write a shell script (`.claude/scripts/test-practice-game.sh`) that:
1. Builds the desktop client with `-P dev` (shade plugin, dev control server)
2. Launches the JAR in the background with debug logging enabled
3. Waits for the control server to come up (`/health` polling)
4. Starts a 3-player practice game via `POST /game/start`
5. Polls `GET /state` in a loop:
   - `CHECK_BET` / `CHECK_RAISE` / `CALL_RAISE` → send FOLD (simplest valid action)
   - `DEAL` → send DEAL
   - `CONTINUE` / `CONTINUE_LOWER` → send CONTINUE
   - `REBUY_CHECK` → send DECLINE_REBUY
   - `QUITSAVE` / `NONE` → wait (AI acting or between states)
6. Detects game completion (game over phase or no state changes)
7. Reports success/failure with timing and hand count
8. Kills the Java process and cleans up

This script replaces the gitignored `test-practice.js` with something committed
and reproducible.

### 1.2 Add a stuck-detection timeout

If `/state` returns the same `inputMode` + `handNumber` for > 15 seconds, dump
the full state JSON to a log file and exit with failure. This catches the primary
symptom: "game stuck after action submitted."

---

## Phase 2: Diagnose Remaining Issues

**Goal:** Run the test harness, capture failures, identify root causes.

### 2.1 Run the test loop and categorize failures

Execute the test script multiple times (3–5 runs). Categorize outcomes:
- **Success**: Game completes all hands to game over
- **Stuck**: Timeout hit — capture the stuck state for diagnosis
- **Error**: HTTP 409/500 or WebSocket disconnect

### 2.2 Analyze stuck states

For each stuck occurrence, examine:
1. **Server logs** (`[ACTION-HUMAN]`, `[ROUTER]`, `[ServerHand]`) — was the action
   received? Was a pending future waiting?
2. **Client logs** (`[WS-SEND]`, `[WS-DIRECTOR]`) — was the action sent? What was
   the input mode?
3. **Control server state** — `inputMode`, `isHumanTurn`, `handNumber`, `availableActions`

Common root causes to look for (based on branch debugging history):

| Symptom | Likely Cause |
|---------|-------------|
| Action sent but server never receives | WebSocket message lost or malformed |
| Server receives action but no pending future | Race: `ACTION_REQUIRED` not yet processed when action arrives |
| Pending future exists but never completed | Action dispatched to wrong player ID or wrong game |
| `inputMode` stuck on `QUITSAVE` | Server waiting for human but client thinks it's AI turn |
| `RATE_LIMITED` in logs | Rate limiter not properly cleared |
| `OUT_OF_ORDER` in logs | Sequence number desync |

---

## Phase 3: Fix Issues

Fix what Phase 2 reproduces, plus obvious code defects found during analysis.
Items below are **probable issues** — only fix if reproduced or clearly defective.

### 3.1 ServerPlayerActionProvider — CompletableFuture race condition

**Problem:** The server sends `ACTION_REQUIRED` via WebSocket, then creates a
`CompletableFuture` to wait for the response. If the client responds before the
future is stored (fast network, same JVM for embedded), the action is lost —
`"no pending action for playerId"` in logs.

**Fix:** Create the `CompletableFuture` BEFORE broadcasting `ACTION_REQUIRED`.
The original `TournamentDirector` blocked synchronously waiting for input — the
future must be ready before the client can possibly respond.

**Original design alignment:** In the original design, `TournamentDirector` set
up the action listener, then waited. The server must do the same: prepare to
receive before asking.

**Fix if reproduced or if code inspection confirms the ordering bug.**

### 3.2 Action mapping completeness

**Problem:** `mapPokerGameActionToWsString()` default case falls through to FOLD
for unmapped action constants (e.g. ALL_IN). Silent data loss.

**Fix:** Map all `PokerGame.ACTION_*` constants explicitly. Log a warning for
truly unknown values instead of silently folding.

**Fix unconditionally — this is an obvious code defect.**

### 3.3 Sequence number desync on reconnect

**Problem:** Server resets `PlayerConnection.lastSequenceNumber = 0` on reconnect,
but client's `AtomicLong` keeps incrementing. First action after reconnect may be
rejected as `OUT_OF_ORDER`.

**Fix:** Either (a) server accepts any sequence > 0 after reconnect, or (b) client
resets its counter on reconnect. Option (a) is simpler and matches the original
design where there were no sequence numbers.

**Fix if reproduced.** Reconnect isn't expected during normal practice games.

### 3.4 DEAL/CONTINUE input modes not reaching client

**Problem:** The original design had explicit `TD.WaitForDeal` phases where the
client showed a "Deal" button. The server auto-completes these phases
(`ServerTournamentDirector` line ~180). If auto-deal is on, the client never sees
`DEAL` input mode — but the Game Control Server may need it for pacing.

**Fix:** Verify that the embedded server's auto-deal behavior matches the original
`TournamentDirector` auto-deal setting. If auto-deal is enabled (default for
practice), the server should auto-advance without waiting for client input. If
manual deal, the server must send a message that triggers `DEAL` input mode.

**Fix if reproduced.** Verification item, not necessarily a bug.

### 3.5 Game completion not reaching client

**Problem:** When the tournament ends (1 player left), `ServerTournamentDirector`
sends `GAME_COMPLETE`. The client's `WebSocketTournamentDirector.onGameComplete()`
must transition to the `PracticeGameOver` phase. If this message is lost or the
phase transition fails, the client sits forever.

**Fix:** Verify `GAME_COMPLETE` handling. Add a fallback: if the game state shows
only 1 player with chips and no action for 5 seconds, treat as game over.

**Fix if reproduced.** This was noted as the known remaining issue after the
autoconfiguration branch merge.

### 3.6 Thread safety in WebSocketTournamentDirector

**Problem:** Server messages arrive on the WebSocket thread but UI updates must
run on the EDT. The `onMessage()` dispatcher uses `SwingUtilities.invokeLater()`
for some operations but not all. If state is read from the WebSocket thread while
being modified on the EDT, data races occur.

**Fix:** Audit all `onMessage()` handlers. Ensure ALL state mutations go through
`SwingUtilities.invokeLater()`. The original `TournamentDirector` ran entirely on
a single thread — the new design must achieve equivalent safety.

**Fix if reproduced or if code audit reveals clear thread-safety violations.**

---

## Phase 4: Verify

### 4.1 Automated — full game completion (FOLD strategy)

The test script must complete 5 consecutive practice games (3-player, FOLD-only
strategy) without any stuck timeouts or errors.

### 4.2 Automated — varied strategies

Run the test script with CHECK/CALL instead of FOLD to exercise more betting paths.
Verify completion.

### 4.3 Screenshot verification

Capture `GET /screenshot` at key points (game start, mid-hand, game over) to verify
the Swing UI renders correctly without manual inspection.

### 4.4 Unit tests

`mvn test -P dev` — all existing tests pass, zero failures.

---

## Success Criteria

1. Automated test script plays a full practice game to completion via Game Control
   Server — no stuck states, no errors
2. All existing unit tests pass
3. Screenshots confirm the UI renders game state correctly
4. Debug logging confirms the action pipeline matches the original design:
   `ACTION_REQUIRED` → player acts → `PLAYER_ACTED` → next player, no gaps
5. Multiple consecutive games complete without H2 lock issues or resource leaks

## Files Likely Modified

- `ServerPlayerActionProvider.java` — CompletableFuture ordering (if reproduced)
- `WebSocketTournamentDirector.java` — action mapping fix (3.2), thread safety (if needed)
- `InboundMessageRouter.java` — sequence number handling (if reproduced)
- `.claude/scripts/test-practice-game.sh` — new test harness
- Test classes for confirmed fixes
