# Review Request

**Branch:** fix-community-card-race
**Worktree:** ../DDPoker-fix-community-card-race
**Plan:** N/A (targeted fix)
**Requested:** 2026-02-22

## Summary

Fixes community cards "jumping between different sets" in all-in situations
during practice mode when playing multiple games. Root cause 2 (this fix):
the WebSocket handler thread sent an extra `GAME_STATE` message after
`startGame()` while the game thread (director) was already firing
`GAME_STATE + HAND_STARTED + COMMUNITY_CARDS_DEALT` through the same
`PlayerConnection`, causing race-induced out-of-order message delivery at
the client. The fix removes the extra `GAME_STATE` send and adds
`synchronized` to `PlayerConnection.sendMessage()`.

## Files Changed

- [x] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/GameWebSocketHandler.java` - Remove racy extra GAME_STATE send after startGame()
- [x] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/PlayerConnection.java` - Add `synchronized` to sendMessage()

**Privacy Check:**
- SAFE - No private information found

## Verification Results

- **Tests:** Existing tests pass (no new tests added; see findings)
- **Coverage:** Not re-measured; changes are deletions + a one-word modifier
- **Build:** Not run locally in review context

## Context & Decisions

The extra `GAME_STATE` send was originally added so the client's `tables_`
map would be populated before `ACTION_REQUIRED` arrived. The
`GameEventBroadcaster.HandStarted` handler now sends `GAME_STATE` before
`HAND_STARTED` on the game thread, making the extra send both redundant and
a race hazard.

---

## Review Results

**Status:** APPROVED WITH SUGGESTIONS

**Reviewed by:** Claude Sonnet 4.6
**Date:** 2026-02-22

---

### Findings

#### Strengths

**Correct root-cause diagnosis.** The race is real and the fix directly
eliminates it. The original code sent `GAME_STATE` from the WebSocket handler
thread immediately after `startGame()` returned — but `startGame()` submits
the director to a thread pool, so by the time the handler thread read the
game state snapshot the director was already executing and emitting events on
its own thread. Both threads shared the same `PlayerConnection`, and
`session.sendMessage()` is documented by Spring to be not thread-safe, making
interleaved sends possible.

**Correct ordering guarantee from the broadcaster.** Confirmed by reading
`GameEventBroadcaster.accept()` (`HandStarted` branch, lines 134-202): when
`game != null` (the auto-start path), the broadcaster iterates over
`connectionManager.getConnections(gameId)` and for each connection at the
starting table sends `GAME_STATE` then `HAND_STARTED` then `HOLE_CARDS` — all
in sequence, all on the game thread, with no intervening handler-thread sends
after the removal. The `tables_` map will be populated before
`ACTION_REQUIRED` arrives because `ServerPlayerActionProvider` calls the
`messageSender` callback (wired in `afterConnectionEstablished` at line 230)
only after the director has called all game-event callbacks including
`HandStarted`.

**Connection is in `connectionManager` before `startGame()`.** Lines 241 →
282 in the fixed handler: `connectionManager.addConnection(...)` is called at
line 241, which is before `startGame()` at line 282. So when the broadcaster's
`HandStarted` handler iterates `connectionManager.getConnections(gameId)`, the
player's connection is guaranteed to be present — the registration happens-before
the director thread begins executing.

**`synchronized` on `sendMessage()` is necessary and sufficient.** Spring's
`WebSocketSession.sendMessage()` is not thread-safe (per the JSR-356 spec and
Spring docs). With the extra `GAME_STATE` removed there are now only two
callers that can send to the same `PlayerConnection` from different threads
after start: the game thread via the broadcaster, and the handler thread on a
reconnect. The `synchronized` keyword on `sendMessage()` serializes these
cleanly. There is no caller that holds a lock and then calls `sendMessage()`,
so no deadlock risk.

**Surgical.** The diff is 10 lines removed, 8 lines added (comment). No
unrelated changes. The removal of an unused import was not needed because the
deleted code shared the same `GameStateSnapshot` type used by the reconnect
path (lines 308-321) which remains.

**Reconnect path is unaffected and correct.** The `else` branch (line 290)
that handles in-progress/paused reconnects still sends `GAME_STATE` directly.
This is right: in a reconnect the broadcaster is not about to fire
`GAME_STATE` again (the hand is already mid-flight), so the handler must seed
the client itself.

---

#### Suggestions (Non-blocking)

**1. No new test for the removed behavior or the ordering contract.**

The removed code had no test verifying that a `GAME_STATE` was sent from the
handler thread after `startGame()`. That's fine — it means removing it didn't
break any test. However, the new invariant ("broadcaster sends GAME_STATE
before HAND_STARTED so the client's `tables_` is populated") is important and
currently only verified by reading the broadcaster source. A unit test on
`GameEventBroadcaster` asserting the per-player `GAME_STATE` is emitted before
`HAND_STARTED` when `game != null` would make this contract explicit and catch
regressions if the broadcaster ordering is ever changed.

Example test sketch:
```java
// GameEventBroadcasterTest
@Test
void handStarted_sendsGameStateBeforeHandStarted_whenGameNotNull() {
    // Arrange: mock GameInstance, connectionManager with one connection,
    //          capture sendMessage() call order.
    // Act: broadcaster.accept(new GameEvent.HandStarted(1, 1))
    // Assert: GAME_STATE arrives before HAND_STARTED in captured order
}
```

This is a suggestion, not a blocker — the existing `autoStartsGameWhenOwnerConnectsWithPreAddedEntry` test covers the handler side, and the broadcaster ordering is deterministic single-threaded code.

**2. `LobbyWebSocketHandler.sendSafe()` has the same thread-safety gap.**

`LobbyWebSocketHandler` (line 287) calls `session.sendMessage()` directly
without synchronization. Lobby messages are currently only sent on the
Spring WebSocket dispatch thread so this is not an active race, but it is
inconsistent with the pattern now established in `PlayerConnection`. Consider
either a follow-up note in `.claude/learnings.md` or wrapping lobby sends in
a similar synchronized helper. Not a blocker for this fix.

**3. Check-then-act in `sendMessage()` is still present.**

```java
public synchronized void sendMessage(ServerMessage message) {
    if (!session.isOpen()) {   // check
        return;
    }
    // ... (gap)
    session.sendMessage(...);  // act
}
```

The `synchronized` on the outer method means both the check and the send now
occur while holding the `PlayerConnection` monitor, so there is no
check-then-act race between two `sendMessage()` callers. However, the session
can still be closed by the framework between `isOpen()` and `sendMessage()`
from an external thread (the framework's close handler). The existing
`IOException` catch (line 84-86) correctly handles this by discarding the
send. This is already the right approach — just noting it for completeness.

---

#### Required Changes (Blocking)

None.

---

### Verification

- **Tests:** The `autoStartsGameWhenOwnerConnectsWithPreAddedEntry` test
  (GameWebSocketHandlerTest line 176) verifies `startGame()` is called and
  the session is not closed, and does not assert a GAME_STATE is sent from
  the handler — consistent with the removal. `PlayerConnectionTest` covers
  `sendMessage()` behavior (open/closed session) but not concurrency. No new
  tests were added for this fix. Suggested addition in the non-blocking
  section above.
- **Coverage:** Not measured; the change is a net deletion of production code
  plus a one-word modifier, so coverage is unlikely to regress.
- **Build:** Not run in review context.
- **Privacy:** No private data in changed files.
- **Security:** No security concerns. The `synchronized` keyword on
  `sendMessage()` eliminates a potential interleaved-write vulnerability on
  the shared WebSocket session.
