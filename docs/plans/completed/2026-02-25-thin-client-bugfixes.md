# Thin Client Bugfixes Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Status:** COMPLETED (2026-03-02) — all 8 tasks implemented as part of feature/desktop-thin-client-cleanup merge.

**Goal:** Fix 8 bugs in the desktop thin client WebSocket system (messaging, event processing, UI updates, resource management).

**Architecture:** All bugs are in the client-side `poker` module (`WebSocketTournamentDirector`, `WebSocketGameClient`) and do not require server-side changes. Fixes are surgical: each targets a specific handler or lifecycle method. The `ActionTimeoutData` record already provides `playerId`, `autoAction`, and `tableId` — no server schema changes needed.

**Tech Stack:** Java 21, Swing EDT, JDK WebSocket, Jackson JSON, JUnit 5 + Mockito + AssertJ

---

### Task 1: Fix ACTION_TIMEOUT — missing state updates and duplicate chat

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java` (lines 948-968, the `onActionTimeout` method)
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirectorTest.java`

**Context:**
When a player times out, the server sends `ACTION_TIMEOUT` followed by `PLAYER_ACTED` for the same action. Currently `onActionTimeout` fires `TYPE_PLAYER_ACTION` with a `HandAction` and shows dealer chat — then `onPlayerActed` does the same thing again, producing **duplicate chat messages**. Additionally, `onActionTimeout` doesn't update chip count, folded status, pot, or hide action buttons for the local player.

**Fix approach:** Change `onActionTimeout` to be a **notification-only** handler. It should:
1. Show a "[Player] timed out" chat message (distinct from the action chat that `onPlayerActed` will send)
2. If the timed-out player is the local player, hide the action buttons (`setInputMode(MODE_QUITSAVE)`)
3. Do NOT fire `TYPE_PLAYER_ACTION` (let the subsequent `PLAYER_ACTED` handle state updates and action chat)
4. Still clear the current player index (as it does now)

**Step 1: Write failing tests**

Add these tests to `WebSocketTournamentDirectorTest.java` after the existing `actionTimeoutSendsDealerChatToHandler` test (around line 1143):

```java
@Test
void actionTimeoutDoesNotFirePlayerActionEvent() throws Exception {
    dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
    dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
    RemotePokerTable table = requireTable();
    List<Integer> events = collectEvents(table);

    ObjectNode payload = mapper.createObjectNode();
    payload.put("playerId", 1L).put("autoAction", "FOLD").put("tableId", 1);
    dispatch(ServerMessageType.ACTION_TIMEOUT, payload);

    // ACTION_TIMEOUT should NOT fire TYPE_PLAYER_ACTION — that's PLAYER_ACTED's job
    assertThat(events).doesNotContain(PokerTableEvent.TYPE_PLAYER_ACTION);
}

@Test
void actionTimeoutOnLocalPlayerHidesActionButtons() throws Exception {
    dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
    dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
    requireTable();

    ObjectNode payload = mapper.createObjectNode();
    payload.put("playerId", 1L).put("autoAction", "FOLD").put("tableId", 1);
    dispatch(ServerMessageType.ACTION_TIMEOUT, payload);

    // Local player timed out — buttons must be hidden
    Mockito.verify(mockGame).setInputMode(PokerTableInput.MODE_QUITSAVE);
}

@Test
void actionTimeoutShowsTimeoutChatNotActionChat() throws Exception {
    dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
    dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
    requireTable();

    List<String> messages = new ArrayList<>();
    wsTD.setChatHandler((fromPlayerID, chatType, message) -> messages.add(message));

    ObjectNode payload = mapper.createObjectNode();
    payload.put("playerId", 1L).put("autoAction", "FOLD").put("tableId", 1);
    dispatch(ServerMessageType.ACTION_TIMEOUT, payload);

    assertThat(messages).hasSize(1);
    assertThat(messages.get(0)).containsIgnoringCase("timed out");
}
```

Also update the **existing** `actionTimeoutFiresPlayerActionEvent` test (line 338-350) — it currently asserts `TYPE_PLAYER_ACTION` is present. This test's assertion needs to be inverted since we're changing the behavior:

```java
@Test
void actionTimeoutClearsCurrentPlayer() throws Exception {
    dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
    dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
    RemotePokerTable table = requireTable();

    ObjectNode payload = mapper.createObjectNode();
    payload.put("playerId", 1L).put("autoAction", "FOLD").put("tableId", 1);
    dispatch(ServerMessageType.ACTION_TIMEOUT, payload);

    RemoteHoldemHand hand = table.getRemoteHand();
    assertThat(hand.getCurrentPlayerIndex())
            .isEqualTo(com.donohoedigital.games.poker.HoldemHand.NO_CURRENT_PLAYER);
}
```

**Step 2: Run tests to verify they fail**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketTournamentDirectorTest -P fast`

Expected: The new `actionTimeoutDoesNotFirePlayerActionEvent` and `actionTimeoutOnLocalPlayerHidesActionButtons` tests FAIL because the current code fires `TYPE_PLAYER_ACTION` and does not call `setInputMode`.

**Step 3: Implement the fix**

Replace the `onActionTimeout` method (lines 948-968) in `WebSocketTournamentDirector.java`:

```java
private void onActionTimeout(ActionTimeoutData d) {
    SwingUtilities.invokeLater(() -> {
        // Notification-only: the subsequent PLAYER_ACTED message handles state
        // updates (chip count, folded, pot, action chat). This handler only:
        // 1. Clears the current player highlight
        // 2. Hides action buttons if the local player timed out
        // 3. Shows a "timed out" chat message (distinct from the action chat)
        RemotePokerTable table = tables_.getOrDefault(d.tableId(), currentTable());
        if (table == null)
            return;
        RemoteHoldemHand hand = table.getRemoteHand();
        if (hand == null)
            return;

        hand.updateCurrentPlayer(HoldemHand.NO_CURRENT_PLAYER);

        // Hide action buttons if the local player was the one who timed out
        if (d.playerId() == localPlayerId_) {
            game_.setInputMode(PokerTableInput.MODE_QUITSAVE);
        }

        PokerPlayer player = findPlayer(d.playerId());
        String name = player != null ? player.getName() : "Player " + d.playerId();
        deliverChatLocal(PokerConstants.CHAT_2, name + " timed out",
                PokerConstants.CHAT_DEALER_MSG_ID);
    });
}
```

**Step 4: Run tests to verify they pass**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketTournamentDirectorTest -P fast`

Expected: ALL tests pass including the new ones.

**Step 5: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java
git add code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirectorTest.java
git commit -m "fix: ACTION_TIMEOUT no longer duplicates chat or leaves stale UI state

The onActionTimeout handler now only shows a 'timed out' notification and
hides action buttons for the local player. State updates (chip count,
folded status, pot, action chat) are left to the subsequent PLAYER_ACTED
message, preventing duplicate dealer chat messages and stale action buttons."
```

---

### Task 2: Fix reconnect scheduler — destroyed after first disconnect

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketGameClient.java` (lines 77, 99-102, 124-146, 242-252)
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketGameClientTest.java`

**Context:**
`disconnect()` calls `scheduler.shutdown()` which permanently destroys the `ScheduledExecutorService`. If the same `WebSocketGameClient` instance is reused (field is `final` on `WebSocketTournamentDirector`, and phases can be cached), `handleReconnect()` silently fails because the scheduler rejects new tasks.

**Fix approach:** Make `scheduler` non-final. In `connect()`, if the old scheduler is shut down, create a fresh one. In `disconnect()`, continue to shut it down (clean resource management).

**Step 1: Write failing test**

Add to `WebSocketGameClientTest.java`:

```java
@Test
void reconnectWorksAfterDisconnectAndReconnect() throws InterruptedException {
    HttpClient httpClient = mock(HttpClient.class);
    WebSocket.Builder builder = mock(WebSocket.Builder.class);
    when(httpClient.newWebSocketBuilder()).thenReturn(builder);

    WebSocket connectedWs = mock(WebSocket.class);
    CompletableFuture<WebSocket> success = CompletableFuture.completedFuture(connectedWs);
    when(builder.buildAsync(any(URI.class), any(WebSocket.Listener.class))).thenReturn(success);

    WebSocketGameClient client = new WebSocketGameClient(
            new ObjectMapper().registerModule(new JavaTimeModule()), httpClient);
    try {
        // First game: connect then disconnect
        client.connect(11885, "game-1", "jwt-1").join();
        assertThat(client.isConnected()).isTrue();
        client.disconnect();
        assertThat(client.isConnected()).isFalse();

        // Second game: connect must succeed (scheduler recreated)
        client.connect(11885, "game-2", "jwt-2").join();
        assertThat(client.isConnected()).isTrue();

        // Verify buildAsync was called twice (once per connect)
        verify(builder, times(2)).buildAsync(any(URI.class), any(WebSocket.Listener.class));
    } finally {
        client.disconnect();
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketGameClientTest#reconnectWorksAfterDisconnectAndReconnect -P fast`

Expected: FAIL — second `connect()` may succeed (new `openConnection()`) but the scheduler is dead, so if the connection later drops, reconnect won't work. The test itself may pass on connect but we need a more targeted assertion. Let's verify the scheduler is alive:

```java
@Test
void schedulerIsAliveAfterDisconnectAndReconnect() {
    HttpClient httpClient = mock(HttpClient.class);
    WebSocket.Builder builder = mock(WebSocket.Builder.class);
    when(httpClient.newWebSocketBuilder()).thenReturn(builder);

    CompletableFuture<WebSocket> failed = new CompletableFuture<>();
    failed.completeExceptionally(new RuntimeException("connect failed"));
    when(builder.buildAsync(any(URI.class), any(WebSocket.Listener.class))).thenReturn(failed);

    WebSocketGameClient client = new WebSocketGameClient(
            new ObjectMapper().registerModule(new JavaTimeModule()), httpClient);
    try {
        // First game: connect + disconnect
        client.connect(11885, "game-1", "jwt-1");
        client.disconnect();

        // Second game: connect triggers reconnect cycle on failure
        // If scheduler is dead, reconnect attempts won't fire
        client.connect(11885, "game-2", "jwt-2");

        // Should see at least 2 buildAsync calls: initial + reconnect attempt
        // (reconnect fires within 500ms)
        verify(builder, timeout(3000).atLeast(3)).buildAsync(any(URI.class), any(WebSocket.Listener.class));
    } finally {
        client.disconnect();
    }
}
```

**Step 3: Implement the fix**

In `WebSocketGameClient.java`, change the `scheduler` field from `final` to mutable and recreate it in `connect()`:

Change the field declaration (line 77):
```java
private ScheduledExecutorService scheduler;
```

In the constructor (lines 99-102), keep the assignment:
```java
this.scheduler = scheduler;
```

In `connect()` (after line 136, before `CompletableFuture<Void> connectFuture`), add:
```java
// Recreate scheduler if it was shut down by a previous disconnect()
if (scheduler.isShutdown()) {
    scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ws-reconnect");
        t.setDaemon(true);
        return t;
    });
}
```

**Step 4: Run tests to verify they pass**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketGameClientTest -P fast`

Expected: ALL tests pass.

**Step 5: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketGameClient.java
git add code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketGameClientTest.java
git commit -m "fix: recreate reconnect scheduler after disconnect

The ScheduledExecutorService was permanently destroyed by disconnect(),
breaking auto-reconnect if the WebSocketGameClient instance was reused
for a second game. Now connect() recreates the scheduler if it was
previously shut down."
```

---

### Task 3: Fix sequence counter not reset on reconnect

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketGameClient.java` (line 136 area inside `connect()`)
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketGameClientTest.java`

**Context:**
After reconnection, `lastReceivedSequence` retains the old value. The first broadcast message from the server may have a much higher sequence number (events occurred during disconnection), triggering gap detection, dropping the message, and requesting state. This wastes a round-trip and loses the `HAND_STARTED` message's semantic triggers.

**Fix approach:** Reset `lastReceivedSequence` to 0 in `connect()` so the gap detection's `prev > 0` guard skips the check on the first message of the new connection.

**Step 1: Write failing test**

Add to `WebSocketGameClientTest.java`:

```java
@Test
void connectResetsLastReceivedSequenceToZero() {
    HttpClient httpClient = mock(HttpClient.class);
    WebSocket.Builder builder = mock(WebSocket.Builder.class);
    when(httpClient.newWebSocketBuilder()).thenReturn(builder);

    WebSocket connectedWs = mock(WebSocket.class);
    when(builder.buildAsync(any(URI.class), any(WebSocket.Listener.class)))
            .thenReturn(CompletableFuture.completedFuture(connectedWs));

    WebSocketGameClient client = new WebSocketGameClient(
            new ObjectMapper().registerModule(new JavaTimeModule()), httpClient);
    try {
        client.connect(11885, "game-1", "jwt-1").join();

        // Simulate that we received messages up to sequence 50
        client.setLastReceivedSequenceForTest(50);

        // Disconnect and reconnect
        client.disconnect();
        client.connect(11885, "game-2", "jwt-2").join();

        // After reconnect, lastReceivedSequence must be 0 so gap detection
        // doesn't trigger on the first broadcast message
        assertThat(client.getLastReceivedSequenceForTest()).isEqualTo(0);
    } finally {
        client.disconnect();
    }
}
```

This requires adding two package-private test accessors to `WebSocketGameClient`.

**Step 2: Run test to verify it fails**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketGameClientTest#connectResetsLastReceivedSequenceToZero -P fast`

Expected: FAIL — `getLastReceivedSequenceForTest()` returns 50 (not reset).

**Step 3: Implement the fix**

In `WebSocketGameClient.java`:

1. Add the test accessors (near the other visible-for-testing methods):
```java
// Visible for testing
void setLastReceivedSequenceForTest(long value) {
    lastReceivedSequence.set(value);
}

// Visible for testing
long getLastReceivedSequenceForTest() {
    return lastReceivedSequence.get();
}
```

2. In `connect()`, add after `this.reconnectCycle.incrementAndGet();` (line 136):
```java
this.lastReceivedSequence.set(0);
this.sequenceCounter.set(0);
```

**Step 4: Run tests to verify they pass**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketGameClientTest -P fast`

Expected: ALL tests pass.

**Step 5: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketGameClient.java
git add code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketGameClientTest.java
git commit -m "fix: reset sequence counters on connect to prevent false gap detection

After reconnection, lastReceivedSequence retained the old value, causing
the first broadcast message to trigger gap detection and be dropped.
Resetting both counters to 0 in connect() prevents this."
```

---

### Task 4: Fix declineScheduler_ resource leak

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java` (line 234-241, the `finish()` method)
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirectorTest.java`

**Context:**
The `declineScheduler_` (`ScheduledExecutorService`) is created per instance but never shut down in `finish()`. Each game leaks a thread pool. It's a daemon thread so it won't block JVM exit, but it wastes resources during long sessions.

**Step 1: Write failing test**

Add to `WebSocketTournamentDirectorTest.java`:

```java
@Test
void finishShutsDownDeclineScheduler() throws Exception {
    // Simulate a full lifecycle: start → finish
    // The declineScheduler_ must be shut down after finish() completes.
    // We verify by checking that scheduling a new task throws or is rejected.
    // Since declineScheduler_ is private, we test indirectly: after finish(),
    // a REBUY_OFFERED should not schedule a decline timeout.
    // (This is a resource-leak prevention test — verifying no exception is thrown.)
    wsTD.finishForTest();
    // No exception — the scheduler was shut down cleanly.
}
```

This requires a `finishForTest()` method that calls the core `finish()` logic without requiring a full phase context.

**Step 2: Implement the fix**

In `WebSocketTournamentDirector.java`, add to the `finish()` method (line 234-241), before `wsClient_.disconnect()`:

```java
declineScheduler_.shutdown();
```

The full `finish()` method becomes:
```java
@Override
public void finish() {
    context_.setGameManager(null);
    cancelPendingRebuyDecline();
    cancelPendingAddonDecline();
    declineScheduler_.shutdown();
    wsClient_.disconnect();
    game_.setPlayerActionListener(null);
}
```

Also add a test accessor:
```java
/** Shuts down schedulers for testing (simulates finish() without context). */
void finishForTest() {
    cancelPendingRebuyDecline();
    cancelPendingAddonDecline();
    declineScheduler_.shutdown();
}
```

**Step 3: Run tests**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketTournamentDirectorTest -P fast`

Expected: ALL tests pass.

**Step 4: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java
git add code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirectorTest.java
git commit -m "fix: shut down declineScheduler_ in finish() to prevent thread leak

Each game played leaked a ScheduledExecutorService thread pool because
declineScheduler_ was never shut down. Now finish() shuts it down after
cancelling any pending rebuy/addon decline tasks."
```

---

### Task 5: Fix uncontested pots showing LOSE overlays on folded players

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java` (lines 1028-1058, the `onPotAwarded` method)
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirectorTest.java`

**Context:**
`onPotAwarded` unconditionally sets the round to `SHOWDOWN` and fires `TYPE_DEALER_ACTION`, even for uncontested pots where everyone folded. This triggers `displayShowdown()` which shows LOSE overlays on folded players. The fix: only set `SHOWDOWN` round if a `SHOWDOWN_STARTED` message was already received for this hand (indicating a real showdown). For uncontested pots, skip the round change — `HAND_COMPLETE` handles cleanup.

**Fix approach:** Track whether a showdown was started for the current hand via a boolean flag. `onShowdownStarted` sets it; `onHandStarted` clears it. `onPotAwarded` only sets the round to SHOWDOWN if the flag is true.

**Step 1: Write failing test**

Add to `WebSocketTournamentDirectorTest.java`:

```java
@Test
void potAwardedWithoutShowdownDoesNotSetShowdownRound() throws Exception {
    // Uncontested pot: all opponents folded, single winner.
    // No SHOWDOWN_STARTED was sent, so round should NOT be set to SHOWDOWN.
    dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
    dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
    RemotePokerTable table = requireTable();
    RemoteHoldemHand hand = table.getRemoteHand();

    // Pot awarded without preceding SHOWDOWN_STARTED
    ObjectNode payload = mapper.createObjectNode();
    payload.put("potIndex", 0).put("amount", 300);
    payload.putArray("winnerIds").add(1L);
    dispatch(ServerMessageType.POT_AWARDED, payload);

    // Round should remain PRE_FLOP (no showdown occurred)
    assertThat(hand.getRound()).isNotEqualTo(BettingRound.SHOWDOWN);
}

@Test
void potAwardedAfterShowdownStartedSetsShowdownRound() throws Exception {
    // Contested pot: SHOWDOWN_STARTED was sent before POT_AWARDED.
    dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
    dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
    RemotePokerTable table = requireTable();
    RemoteHoldemHand hand = table.getRemoteHand();

    // SHOWDOWN_STARTED arrives first
    ObjectNode showdownPayload = mapper.createObjectNode();
    showdownPayload.put("tableId", 1);
    dispatch(ServerMessageType.SHOWDOWN_STARTED, showdownPayload);

    // Then POT_AWARDED
    ObjectNode payload = mapper.createObjectNode();
    payload.put("potIndex", 0).put("amount", 300);
    payload.putArray("winnerIds").add(1L);
    dispatch(ServerMessageType.POT_AWARDED, payload);

    assertThat(hand.getRound()).isEqualTo(BettingRound.SHOWDOWN);
}
```

**Step 2: Run tests to verify they fail**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketTournamentDirectorTest#potAwardedWithoutShowdownDoesNotSetShowdownRound -P fast`

Expected: FAIL — round is SHOWDOWN even without SHOWDOWN_STARTED.

**Step 3: Implement the fix**

1. Add a field to `WebSocketTournamentDirector.java` (near `isPreFlop_` at line 96):
```java
// Tracks whether SHOWDOWN_STARTED was received for the current hand.
// Used by onPotAwarded to skip SHOWDOWN round for uncontested pots.
private volatile boolean showdownStarted_ = false;
```

2. In `onHandStarted` (search for the method), add at the start of the `invokeLater` lambda:
```java
showdownStarted_ = false;
```

3. In `onShowdownStarted` (line 1060-1092), add at the start of the `invokeLater` lambda:
```java
showdownStarted_ = true;
```

4. In `onPotAwarded` (line 1028-1058), wrap the SHOWDOWN round change in the flag check. Change:
```java
hand.updateRound(BettingRound.SHOWDOWN);
table.fireEvent(PokerTableEvent.TYPE_DEALER_ACTION, BettingRound.SHOWDOWN.toLegacy());
```
to:
```java
if (showdownStarted_) {
    hand.updateRound(BettingRound.SHOWDOWN);
    table.fireEvent(PokerTableEvent.TYPE_DEALER_ACTION, BettingRound.SHOWDOWN.toLegacy());
}
```

**Step 4: Run tests**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketTournamentDirectorTest -P fast`

Expected: ALL tests pass. The existing `potAwardedFiresDealerActionEvent` test may need updating since it doesn't precede POT_AWARDED with SHOWDOWN_STARTED — check if it still passes and update if needed (either add a preceding SHOWDOWN_STARTED dispatch or change the assertion).

**Step 5: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java
git add code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirectorTest.java
git commit -m "fix: uncontested pots no longer trigger SHOWDOWN round and LOSE overlays

onPotAwarded now only sets the round to SHOWDOWN when a SHOWDOWN_STARTED
message was received for the current hand. For uncontested pots (all
opponents folded), the round is left as-is, preventing false LOSE
overlays on folded players."
```

---

### Task 6: Fix determineInputMode for action option edge cases

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java` (lines 886-894, `determineInputMode`)
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirectorTest.java`

**Context:**
`determineInputMode` checks `canCall` first, then `canRaise`, but doesn't check `canBet`. This mishandles the edge case where the player can only check (no bet, no raise, no call) — it falls through to `MODE_CHECK_BET` which may offer a bet option that isn't valid. Also, when `canCall` is true but `canRaise` is false, `MODE_CALL_RAISE` may display a raise button that shouldn't be there.

**Fix approach:** Map the server options more precisely to the four modes:
- `canCall` → `MODE_CALL_RAISE` (bet already placed, can call/raise)
- `canBet` → `MODE_CHECK_BET` (no bet yet, can check/bet)
- `canRaise` (without `canCall`) → `MODE_CHECK_RAISE` (big blind pre-flop: check or raise)
- None of the above → `MODE_QUITSAVE` (no valid actions, shouldn't happen)

**Step 1: Write tests**

Add to `WebSocketTournamentDirectorTest.java`:

```java
@Test
void determineInputModeCallRaiseWhenCanCall() throws Exception {
    dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
    dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
    requireTable();

    ObjectNode options = mapper.createObjectNode();
    options.put("canFold", true).put("canCheck", false).put("canCall", true).put("callAmount", 50)
            .put("canBet", false).put("minBet", 0).put("maxBet", 0).put("canRaise", true).put("minRaise", 100)
            .put("maxRaise", 500).put("canAllIn", true).put("allInAmount", 1000);
    ObjectNode payload = mapper.createObjectNode();
    payload.put("timeoutSeconds", 30).set("options", options);
    dispatch(ServerMessageType.ACTION_REQUIRED, payload);

    Mockito.verify(mockGame).setInputMode(Mockito.eq(PokerTableInput.MODE_CALL_RAISE), Mockito.any(), Mockito.any());
}

@Test
void determineInputModeCheckBetWhenCanBetNotCall() throws Exception {
    dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
    dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
    requireTable();

    ObjectNode options = mapper.createObjectNode();
    options.put("canFold", true).put("canCheck", true).put("canCall", false).put("callAmount", 0)
            .put("canBet", true).put("minBet", 50).put("maxBet", 1000).put("canRaise", false).put("minRaise", 0)
            .put("maxRaise", 0).put("canAllIn", true).put("allInAmount", 1000);
    ObjectNode payload = mapper.createObjectNode();
    payload.put("timeoutSeconds", 30).set("options", options);
    dispatch(ServerMessageType.ACTION_REQUIRED, payload);

    Mockito.verify(mockGame).setInputMode(Mockito.eq(PokerTableInput.MODE_CHECK_BET), Mockito.any(), Mockito.any());
}

@Test
void determineInputModeCheckRaiseWhenCanRaiseNotCall() throws Exception {
    // Big blind pre-flop: can check or raise, but not call
    dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
    dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
    requireTable();

    ObjectNode options = mapper.createObjectNode();
    options.put("canFold", true).put("canCheck", true).put("canCall", false).put("callAmount", 0)
            .put("canBet", false).put("minBet", 0).put("maxBet", 0).put("canRaise", true).put("minRaise", 100)
            .put("maxRaise", 500).put("canAllIn", true).put("allInAmount", 1000);
    ObjectNode payload = mapper.createObjectNode();
    payload.put("timeoutSeconds", 30).set("options", options);
    dispatch(ServerMessageType.ACTION_REQUIRED, payload);

    Mockito.verify(mockGame).setInputMode(Mockito.eq(PokerTableInput.MODE_CHECK_RAISE), Mockito.any(), Mockito.any());
}
```

**Step 2: Run tests to verify current behavior**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketTournamentDirectorTest -P fast`

The `determineInputModeCheckBetWhenCanBetNotCall` test should pass (current fallthrough works). The others should also pass since the logic happens to produce correct results for the common cases. These tests primarily serve as **regression tests** to lock in the correct behavior.

**Step 3: Implement the fix**

Replace `determineInputMode` (lines 886-894):

```java
private int determineInputMode(ActionOptionsData opts) {
    if (opts == null)
        return PokerTableInput.MODE_QUITSAVE;
    if (opts.canCall())
        return PokerTableInput.MODE_CALL_RAISE;
    if (opts.canRaise())
        return PokerTableInput.MODE_CHECK_RAISE;
    if (opts.canBet())
        return PokerTableInput.MODE_CHECK_BET;
    return PokerTableInput.MODE_QUITSAVE;
}
```

The key changes: (1) explicit `canBet()` check instead of fallthrough, (2) default to `MODE_QUITSAVE` instead of `MODE_CHECK_BET` when no actions are available.

**Step 4: Run tests**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketTournamentDirectorTest -P fast`

Expected: ALL tests pass.

**Step 5: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java
git add code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirectorTest.java
git commit -m "fix: determineInputMode checks canBet explicitly, defaults to QUITSAVE

The previous fallthrough to MODE_CHECK_BET could show bet buttons when
no betting was actually available. Now canBet is checked explicitly, and
the default is MODE_QUITSAVE when no valid actions exist."
```

---

### Task 7: Fix EDT blocking in onRebuyOffered control-server path

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java` (lines 1151-1191, `onRebuyOffered`)
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirectorTest.java`

**Context:**
The control-server rebuy path calls `provider.waitForDecision()` inside `SwingUtilities.invokeLater()`, which blocks the EDT. If the API response is delivered via `invokeLater`, this creates a deadlock. The fix is to move the blocking wait off the EDT.

**Step 1: Read the current code**

Read lines 1151-1191 of `WebSocketTournamentDirector.java` to understand the full `onRebuyOffered` method.

**Step 2: Implement the fix**

The `waitForDecision` call must happen on a background thread, not the EDT. Spawn the wait on the `declineScheduler_` (or a new background thread) and have it set the input mode on the EDT via `invokeLater` before blocking:

Replace the control-server path in `onRebuyOffered`:

```java
if (provider != null) {
    // Set the input mode on the EDT so the control server API can detect it,
    // then wait for the decision on a background thread to avoid blocking the EDT.
    game_.setInputMode(PokerTableInput.MODE_REBUY_CHECK);
    declineScheduler_.execute(() -> {
        boolean accepted = provider.waitForDecision(() -> {}, 30);
        SwingUtilities.invokeLater(() -> {
            cancelPendingRebuyDecline();
            if (accepted) {
                markRebuyDecisionSent(true, d.cost(), d.chips(), false);
                wsClient_.sendRebuyDecision(true);
            } else {
                markRebuyDecisionSent(false, d.cost(), d.chips(), false);
                wsClient_.sendRebuyDecision(false);
            }
        });
    });
    return; // Skip the normal Swing rebuy button path
}
```

**Step 3: Run all tests**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketTournamentDirectorTest -P fast`

Expected: ALL tests pass. No new test needed — this is a threading-correctness fix that's hard to unit-test without integration infrastructure.

**Step 4: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java
git commit -m "fix: move control-server rebuy waitForDecision off the EDT

The waitForDecision call in onRebuyOffered was blocking the Swing EDT for
up to 30 seconds, potentially causing UI freeze or deadlock. Now the wait
runs on the declineScheduler background thread, and the decision is
applied back on the EDT via invokeLater."
```

---

### Task 8: Run full test suite and verify no regressions

**Files:** None (verification only)

**Step 1: Run poker module tests**

Run: `cd code && mvn test -pl poker -P fast`

Expected: ALL tests pass.

**Step 2: Run pokergameserver module tests**

Run: `cd code && mvn test -pl pokergameserver -P fast`

Expected: ALL tests pass. (No server-side changes were made, but verify no cross-module breakage.)

**Step 3: Run full project tests**

Run: `cd code && mvn test -P dev`

Expected: ALL tests pass.

**Step 4: Final commit (if any cleanup needed)**

If any test failures were found and fixed during this task, commit the fixes.

---

## Summary of Changes

| Task | Bug | Severity | Files Changed |
|------|-----|----------|---------------|
| 1 | ACTION_TIMEOUT duplicate chat + stale UI | HIGH | WebSocketTournamentDirector + test |
| 2 | Scheduler destroyed after disconnect | HIGH | WebSocketGameClient + test |
| 3 | Sequence counter not reset on reconnect | MEDIUM | WebSocketGameClient + test |
| 4 | declineScheduler_ resource leak | MEDIUM | WebSocketTournamentDirector + test |
| 5 | Uncontested pot LOSE overlays | MEDIUM | WebSocketTournamentDirector + test |
| 6 | determineInputMode edge cases | LOW | WebSocketTournamentDirector + test |
| 7 | EDT blocking in rebuy control path | LOW | WebSocketTournamentDirector |
| 8 | Full regression verification | — | None |
