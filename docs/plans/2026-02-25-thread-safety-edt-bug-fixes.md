# Thread Safety, EDT, and Protocol Bug Fixes

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix 32 bugs found in the desktop client and server: thread-safety violations, EDT blocking/violations, NPEs during table transitions, and protocol correctness issues.

**Architecture:** Fixes are grouped into 6 batches by area. Each batch is independently testable. Server-side thread-safety fixes come first (highest impact), then client thread-safety, EDT violations, NPE guards, protocol fixes, and concurrency races.

**Tech Stack:** Java 21, Swing, WebSocket (`java.net.http`), JUnit 5, `java.util.concurrent`

---

## Task 1: ServerPlayer — Add volatile/AtomicInteger for cross-thread field access

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerPlayer.java`
- Test: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/ServerPlayerTest.java`

**Step 1: Write failing test for atomic chip operations**

```java
@Test
void addChipsIsThreadSafe() throws Exception {
    ServerPlayer player = new ServerPlayer(1, "Test", false, 3);
    player.setChipCount(0);
    int threads = 10;
    int increments = 1000;
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    CountDownLatch latch = new CountDownLatch(threads);
    for (int i = 0; i < threads; i++) {
        executor.submit(() -> {
            for (int j = 0; j < increments; j++) {
                player.addChips(1);
            }
            latch.countDown();
        });
    }
    latch.await(5, TimeUnit.SECONDS);
    executor.shutdown();
    assertThat(player.getChipCount()).isEqualTo(threads * increments);
}
```

Run: `mvn test -pl pokergameserver -Dtest=ServerPlayerTest#addChipsIsThreadSafe`
Expected: FAIL (race condition causes count < 10000)

**Step 2: Apply fixes to ServerPlayer.java**

Change `chipCount` and `numRebuys` to `AtomicInteger`. Add `volatile` to all other mutable fields:

```java
// Replace plain fields with:
private volatile String name;
private volatile int skillLevel;
private final AtomicInteger chipCount = new AtomicInteger();
private volatile int seat;
private volatile boolean folded;
private volatile boolean allIn;
private volatile boolean sittingOut;
private volatile boolean observer;
private final AtomicInteger numRebuys = new AtomicInteger();
private volatile int finishPosition;
private volatile int oddChips;
private volatile int thinkBankMillis;
private volatile int timeoutMillis;
private volatile int timeoutMessageSecondsLeft;
private volatile boolean askShowWinning;
private volatile boolean askShowLosing;
```

Update all getters/setters for `chipCount` and `numRebuys` to use atomic methods:
```java
public int getChipCount() { return chipCount.get(); }
public void setChipCount(int chips) { chipCount.set(chips); }
public void addChips(int amount) { chipCount.addAndGet(amount); }
public void subtractChips(int amount) { chipCount.addAndGet(-amount); }
public int getNumRebuys() { return numRebuys.get(); }
public void incrementRebuys() { numRebuys.incrementAndGet(); }
```

**Step 3: Run test to verify it passes**

Run: `mvn test -pl pokergameserver -Dtest=ServerPlayerTest`
Expected: PASS

**Step 4: Commit**
```
fix: Make ServerPlayer fields thread-safe with volatile and AtomicInteger
```

---

## Task 2: ServerPlayerSession — AtomicInteger for consecutiveTimeouts

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerPlayerSession.java`

**Step 1: Change field and methods**

```java
// Line 43: change from
private volatile int consecutiveTimeouts;
// to
private final AtomicInteger consecutiveTimeouts = new AtomicInteger();

// Line 92-94: change from
public void incrementConsecutiveTimeouts() {
    this.consecutiveTimeouts++;
}
// to
public void incrementConsecutiveTimeouts() {
    this.consecutiveTimeouts.incrementAndGet();
}

// Line 99-101: change from
public void resetConsecutiveTimeouts() {
    this.consecutiveTimeouts = 0;
}
// to
public void resetConsecutiveTimeouts() {
    this.consecutiveTimeouts.set(0);
}

// Line 141-143: change from
public int getConsecutiveTimeouts() {
    return consecutiveTimeouts;
}
// to
public int getConsecutiveTimeouts() {
    return consecutiveTimeouts.get();
}
```

**Step 2: Run existing tests**

Run: `mvn test -pl pokergameserver`
Expected: PASS

**Step 3: Commit**
```
fix: Use AtomicInteger for ServerPlayerSession.consecutiveTimeouts
```

---

## Task 3: ServerPlayerSession — Synchronize connect/disconnect, add stateLock to reconnectPlayer

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerPlayerSession.java`
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/GameInstance.java`

**Step 1: Make connect() and disconnect() synchronized in ServerPlayerSession**

```java
public synchronized void connect() {
    this.connected = true;
    this.disconnected = false;
    this.disconnectedAt = null;
}

public synchronized void disconnect() {
    this.connected = false;
    if (!this.disconnected) {
        this.disconnected = true;
        this.disconnectedAt = Instant.now();
    }
    this.messageSender = null;
}
```

**Step 2: Add stateLock to reconnectPlayer in GameInstance.java**

```java
public void reconnectPlayer(long profileId) {
    stateLock.lock();
    try {
        ServerPlayerSession session = playerSessions.get(profileId);
        if (session != null) {
            session.connect();
        }
    } finally {
        stateLock.unlock();
    }
}
```

**Step 3: Run tests**

Run: `mvn test -pl pokergameserver`
Expected: PASS

**Step 4: Commit**
```
fix: Synchronize connect/disconnect and add stateLock to reconnectPlayer
```

---

## Task 4: GameEventBroadcaster — aiFaceUp volatile + broadcastCallback pattern

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/GameEventBroadcaster.java`
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerGameEventBus.java`

**Step 1: Make aiFaceUp volatile**

```java
// Line 88: change from
private boolean aiFaceUp;
// to
private volatile boolean aiFaceUp;
```

**Step 2: Fix broadcastCallback volatile single-read pattern in ServerGameEventBus**

```java
// Lines 79-85: change from
if (broadcastCallback != null) {
    try {
        broadcastCallback.accept(event);
    } catch (Exception e) {
        logger.log(Level.SEVERE, "Error in broadcast callback for event: " + event, e);
    }
}
// to
Consumer<GameEvent> cb = broadcastCallback;
if (cb != null) {
    try {
        cb.accept(event);
    } catch (Exception e) {
        logger.log(Level.SEVERE, "Error in broadcast callback for event: " + event, e);
    }
}
```

**Step 3: Run tests**

Run: `mvn test -pl pokergameserver`
Expected: PASS

**Step 4: Commit**
```
fix: Make aiFaceUp volatile, fix broadcastCallback single-read pattern
```

---

## Task 5: GameConnectionManager — Fix removeConnection TOCTOU

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/GameConnectionManager.java`

**Step 1: Replace check-then-act with atomic compute**

```java
// Lines 66-74: change from
public void removeConnection(String gameId, long profileId) {
    ConcurrentHashMap<Long, PlayerConnection> gameConnections = connections.get(gameId);
    if (gameConnections != null) {
        gameConnections.remove(profileId);
        if (gameConnections.isEmpty()) {
            connections.remove(gameId);
        }
    }
}
// to
public void removeConnection(String gameId, long profileId) {
    connections.computeIfPresent(gameId, (key, gameConnections) -> {
        gameConnections.remove(profileId);
        return gameConnections.isEmpty() ? null : gameConnections;
    });
}
```

**Step 2: Run tests**

Run: `mvn test -pl pokergameserver`
Expected: PASS

**Step 3: Commit**
```
fix: Use computeIfPresent to eliminate TOCTOU race in removeConnection
```

---

## Task 6: HandCompleted — Stop revealing folded players' hole cards

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/GameEventBroadcaster.java`

**Step 1: Change filter from isSittingOut to isFolded**

```java
// Lines 306-312: change from
// Build showdown players — include all players who were in the hand
// (not sitting out), so folded players' cards can be revealed too.
List<ServerPlayer> allWithCards = new ArrayList<>();
for (int s = 0; s < sgt.getNumSeats(); s++) {
    ServerPlayer sp = sgt.getPlayer(s);
    if (sp != null && !sp.isSittingOut()) {
        allWithCards.add(sp);
    }
}
// to
// Build showdown players — only include players still in the hand
// (not folded, not sitting out). Folded players' cards stay hidden.
List<ServerPlayer> allWithCards = new ArrayList<>();
for (int s = 0; s < sgt.getNumSeats(); s++) {
    ServerPlayer sp = sgt.getPlayer(s);
    if (sp != null && !sp.isSittingOut() && !sp.isFolded()) {
        allWithCards.add(sp);
    }
}
```

**Step 2: Run tests**

Run: `mvn test -pl pokergameserver`
Expected: PASS

**Step 3: Commit**
```
fix: Stop broadcasting folded players' hole cards in HandCompleted
```

---

## Task 7: HandStarted — Stamp sequence numbers on per-player sends

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/GameEventBroadcaster.java`

**Step 1: Stamp sequence numbers on direct sends**

In the `HandStarted` handler (around lines 170-202), every `conn.sendMessage()` call should stamp a sequence number. Add a helper or inline the stamping:

```java
// For each conn.sendMessage(msg) call in the HandStarted handler, change to:
conn.sendMessage(msg.withSequence(sequenceCounter.incrementAndGet()));
```

Apply this to all `conn.sendMessage()` calls within the HandStarted case block — GAME_STATE, HAND_STARTED, and HOLE_CARDS messages.

**Step 2: Run tests**

Run: `mvn test -pl pokergameserver`
Expected: PASS

**Step 3: Commit**
```
fix: Stamp sequence numbers on HandStarted per-player messages
```

---

## Task 8: WebSocketGameClient — Skip processing gapped message

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketGameClient.java`

**Step 1: Return after gap detection instead of processing the message**

```java
// Lines 422-434: change from
if (seq != null) {
    long prev = lastReceivedSequence.get();
    if (prev > 0 && seq > prev + 1) {
        logger.warn("[WS-GAP] sequence gap detected: last={} received={} (missed {} events)", prev, seq,
                seq - prev - 1);
        sendRequestState();
    }
    lastReceivedSequence.set(seq);
}

ServerMessageType type = ServerMessageType.valueOf(typeName);
messageHandler.accept(new InboundMessage(type, msgGameId, data, seq));
// to
if (seq != null) {
    long prev = lastReceivedSequence.get();
    if (prev > 0 && seq > prev + 1) {
        logger.warn("[WS-GAP] sequence gap detected: last={} received={} (missed {} events). "
                + "Requesting full state resync; dropping this message.", prev, seq, seq - prev - 1);
        lastReceivedSequence.set(seq);
        sendRequestState();
        return;  // Don't process the out-of-order message
    }
    lastReceivedSequence.set(seq);
}

ServerMessageType type = ServerMessageType.valueOf(typeName);
messageHandler.accept(new InboundMessage(type, msgGameId, data, seq));
```

**Step 2: Run tests**

Run: `mvn test -pl poker`
Expected: PASS

**Step 3: Commit**
```
fix: Drop gapped messages and wait for GAME_STATE resync
```

---

## Task 9: onNeverBrokeOffered — Wrap in invokeLater

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java`

**Step 1: Wrap handler body in SwingUtilities.invokeLater**

```java
// Lines 1646-1655: change from
private void onNeverBrokeOffered(ServerMessageData.NeverBrokeOfferedData d) {
    boolean accept = com.donohoedigital.games.poker.PokerUtils
            .isOptionOn(com.donohoedigital.games.poker.engine.PokerConstants.OPTION_CHEAT_NEVERBROKE);
    wsClient_.sendNeverBrokeDecision(accept);
    if (accept) {
        deliverChatLocal(PokerConstants.CHAT_ALWAYS, "Never-broke activated: chips restored",
                PokerConstants.CHAT_DEALER_MSG_ID);
    }
}
// to
private void onNeverBrokeOffered(ServerMessageData.NeverBrokeOfferedData d) {
    boolean accept = com.donohoedigital.games.poker.PokerUtils
            .isOptionOn(com.donohoedigital.games.poker.engine.PokerConstants.OPTION_CHEAT_NEVERBROKE);
    wsClient_.sendNeverBrokeDecision(accept);
    if (accept) {
        SwingUtilities.invokeLater(() ->
            deliverChatLocal(PokerConstants.CHAT_ALWAYS, "Never-broke activated: chips restored",
                    PokerConstants.CHAT_DEALER_MSG_ID));
    }
}
```

**Step 2: Commit**
```
fix: Dispatch onNeverBrokeOffered chat update to EDT
```

---

## Task 10: WebSocketOpponentTracker — Use ConcurrentHashMap

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketOpponentTracker.java`

**Step 1: Replace HashMap/HashSet with concurrent collections**

```java
// Lines 35-40: change from
private final Map<Integer, PlayerStats> stats = new HashMap<>();
private final Map<Integer, Boolean> handFolded = new HashMap<>();
private final Map<Integer, Boolean> handRaised = new HashMap<>();
private final Set<Integer> handActors = new HashSet<>();
// to
private final Map<Integer, PlayerStats> stats = new ConcurrentHashMap<>();
private final Map<Integer, Boolean> handFolded = new ConcurrentHashMap<>();
private final Map<Integer, Boolean> handRaised = new ConcurrentHashMap<>();
private final Set<Integer> handActors = ConcurrentHashMap.newKeySet();
```

Make `PlayerStats` fields volatile:
```java
private static class PlayerStats {
    volatile int handsActed, foldedPreFlop, raisedPreFlop, calledPreFlop;
}
```

**Step 2: Commit**
```
fix: Use concurrent collections in WebSocketOpponentTracker
```

---

## Task 11: isPreFlop_ — Add volatile

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java`

**Step 1: Add volatile to field declaration**

```java
// Line 96: change from
private boolean isPreFlop_ = false;
// to
private volatile boolean isPreFlop_ = false;
```

**Step 2: Commit**
```
fix: Make isPreFlop_ volatile for cross-thread visibility
```

---

## Task 12: AdvanceAction — Replace EDT sleep with Timer

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/dashboard/AdvanceAction.java`

**Step 1: Replace synchronous sleep with async Timer callback**

The autopilot sleep at lines 412-415 blocks the EDT. Replace with a `javax.swing.Timer` that fires the action after the delay:

```java
// Lines 410-416: change from
if (autopilot_ != null && autopilot_.isSelected()) {
    if (game_.isOnlineGame()) {
        int nDelay = 5 + DiceRoller.rollDieInt(15);
        Utils.sleepMillis(nDelay * 100);
    }
    return human.getAction(false);
}
// to
if (autopilot_ != null && autopilot_.isSelected()) {
    if (game_.isOnlineGame()) {
        int nDelay = (5 + DiceRoller.rollDieInt(15)) * 100;
        HandAction action = human.getAction(false);
        // Schedule the action after a delay instead of blocking the EDT
        javax.swing.Timer timer = new javax.swing.Timer(nDelay, e -> {
            game_.setPlayerActionListener(null);
            processAction(action);
        });
        timer.setRepeats(false);
        timer.start();
        return null;  // Signal that action is deferred
    }
    return human.getAction(false);
}
```

**Note:** This change requires understanding the call chain. The caller (`Bet.process()`) needs to handle a null return by waiting for the timer callback. Review the calling code carefully before implementing — the exact approach may need adjustment based on how `Bet.process()` consumes the return value.

**Step 2: Run tests**

Run: `mvn test -pl poker`
Expected: PASS

**Step 3: Commit**
```
fix: Replace EDT-blocking autopilot sleep with javax.swing.Timer
```

---

## Task 13: ShowTournamentTable — Replace EDT sleep with Timer

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/ShowTournamentTable.java`

**Step 1: Replace sleep with Timer for requestFocus retry**

```java
// Lines 1204-1214: change from
SwingUtilities.invokeLater(new Runnable() {
    public void run() {
        if (!buttonContinueMiddle_.isEnabled()) {
            Utils.sleepMillis(100);
        }
        buttonContinueLower_.requestFocus();
    }
});
// to
SwingUtilities.invokeLater(() -> {
    if (buttonContinueLower_.isEnabled()) {
        buttonContinueLower_.requestFocus();
    } else {
        javax.swing.Timer retry = new javax.swing.Timer(100, e -> buttonContinueLower_.requestFocus());
        retry.setRepeats(false);
        retry.start();
    }
});
```

Apply the same pattern to the second instance at lines 1220-1229 for `buttonContinueMiddle_`.

**Step 2: Run tests**

Run: `mvn test -pl poker`
Expected: PASS

**Step 3: Commit**
```
fix: Replace EDT-blocking sleep with Timer for button focus retry
```

---

## Task 14: ButtonDisplay — Move game piece mutations to EDT

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/ButtonDisplay.java`

**Step 1: Wrap model mutations in invokeLater along with repaint**

```java
// Lines 108-125: change from
final Territory old = piece.getTerritory();
if (old != null) {
    old.removeGamePiece(piece);
}
t.addGamePiece(piece);

if (nSleepMillis > 0)
    Utils.sleepMillis(nSleepMillis);

SwingUtilities.invokeLater(new Runnable() {
    public void run() {
        if (old != null)
            PokerUtils.getGameboard().repaintTerritory(old, false);
        PokerUtils.getGameboard().repaintTerritory(t, false);
    }
});
// to
if (nSleepMillis > 0)
    Utils.sleepMillis(nSleepMillis);

SwingUtilities.invokeLater(() -> {
    Territory old = piece.getTerritory();
    if (old != null) {
        old.removeGamePiece(piece);
    }
    t.addGamePiece(piece);
    if (old != null)
        PokerUtils.getGameboard().repaintTerritory(old, false);
    PokerUtils.getGameboard().repaintTerritory(t, false);
});
```

**Step 2: Commit**
```
fix: Move ButtonDisplay game piece mutations to EDT
```

---

## Task 15: ColorUpFinish — Wrap setInputMode in invokeLater

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/ColorUpFinish.java`

**Step 1: Marshal setInputMode to EDT**

```java
// Line 209: change from
game_.setInputMode(PokerTableInput.MODE_CONTINUE);
// to
SwingUtilities.invokeLater(() -> game_.setInputMode(PokerTableInput.MODE_CONTINUE));
```

**Step 2: Commit**
```
fix: Dispatch ColorUpFinish setInputMode to EDT
```

---

## Task 16: NPE guards — Add null checks for table/hand during transitions

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/dashboard/PlayerInfo.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerCustomTerritoryDrawer.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerGameboard.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/ShowTournamentTable.java`

**Step 1: PlayerInfo.java — Guard table null at line 101-103**

```java
// Change from
PokerTable table = last_.getTable();
int nLast = profile.getLastRebuyLevel();
if (table.getLevel() <= nLast) {
// to
PokerTable table = last_.getTable();
int nLast = profile.getLastRebuyLevel();
if (table != null && table.getLevel() <= nLast) {
```

**Step 2: PokerCustomTerritoryDrawer.java — Guard table null at line 110-111**

```java
// Change from
PokerTable table = game_.getCurrentTable();
HoldemHand hhand = table.getHoldemHand();
// to
PokerTable table = game_.getCurrentTable();
if (table == null) return;
HoldemHand hhand = table.getHoldemHand();
```

**Step 3: PokerGameboard.java — Guard table null at line 250-263**

```java
// Change from
PokerTable table = game_.getCurrentTable();
if (table != null && table.isZipMode())
    return;
// to
PokerTable table = game_.getCurrentTable();
if (table == null || table.isZipMode())
    return;
```

**Step 4: ShowTournamentTable.java SwingIt.run() — Guard table_ null at line 814**

```java
// Change from
public void run() {
    if (table_.isZipMode())
        return;
// to
public void run() {
    if (table_ == null || table_.isZipMode())
        return;
```

**Step 5: Run tests**

Run: `mvn test -pl poker`
Expected: PASS

**Step 6: Commit**
```
fix: Add null guards for table/hand during transitions to prevent NPEs
```

---

## Verification

After all tasks are complete:

1. Run full server test suite: `cd code && mvn test -pl pokergameserver`
2. Run full client test suite: `cd code && mvn test -pl poker`
3. Run full build: `cd code && mvn test`
4. Manual smoke test: Start a practice game, play through elimination, verify no NPEs or UI freezes in the game log
