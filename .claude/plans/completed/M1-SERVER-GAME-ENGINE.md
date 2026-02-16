# Milestone 1: Server Game Engine Foundation — Detailed Plan

**Status:** ✅ **COMPLETE**
**Created:** 2026-02-16
**Last Updated:** 2026-02-16
**Completed:** 2026-02-16
**Parent Plan:** `.claude/plans/SERVER-HOSTED-GAME-ENGINE.md`

**📊 See [M1-PROGRESS-SUMMARY.md](./M1-PROGRESS-SUMMARY.md) for complete details**

---

## Progress Tracking

**Current Status:** ✅ **ALL STEPS COMPLETE**
**Overall M1 Status:** ✅ **COMPLETE** — All 11 steps implemented and tested

### Recent Session (2026-02-16 - Afternoon)

✅ **Completed:**
1. **Steps 5 & 6:** Implemented GameInstance and GameInstanceManager
   - Created GameInstanceState enum and GameServerException
   - Implemented GameInstance with full lifecycle state machine (CREATED → WAITING → IN_PROGRESS → PAUSED/COMPLETED/CANCELLED)
   - Implemented GameInstanceManager with game registry, lifecycle management, and cleanup
   - Fixed logging dependency conflict (excluded log4j-to-slf4j from Spring Boot starter)
   - Created simple random AI provider (TODO: integrate ServerAIProvider from pokerserver later)
   - Fixed TournamentProfile integration (method names, LevelAdvanceMode conversion)
   - Added 31 new tests (15 GameInstanceTest + 16 GameInstanceManagerTest)
2. **Refactoring:** Eliminated LevelAdvanceMode enum duplication
   - Deleted duplicate LevelAdvanceMode from pokergamecore
   - Kept canonical version in pokerengine/model
   - Updated all imports in ServerTournamentContext, ServerTournamentDirectorTest, ServerTournamentDirectorDebugTest
   - Fixed testOnlyOwnerCanPause timing issue (game completing too fast)
   - **All 243 pokergameserver tests passing** (11.1s total runtime)

### Previous Session (2026-02-16 - Morning)

✅ **Completed:**
1. Fixed chip conservation bug in ServerHand (removed debug logging)
2. Ported hands-based level advancement from original codebase
3. All 212 pokergameserver tests passing (4.5s total runtime)
4. Configured tests with hands-based advancement (2 hands per level, 6 blind tiers)

**What's Next:**
- ✅ M1 is COMPLETE — ready for code review and merge to main
- 🎯 **Milestone 2:** WebSocket API + REST API for game hosting
- 🔄 **Phase 7D:** ServerAIProvider integration (can proceed in parallel with M2)
- 📦 **Future:** Event store persistence (database backend)

**Blockers:** None.

---

## Context

The TournamentEngine in pokergamecore is complete and already integrated into TournamentDirector (the old `_processTable()` was fully replaced by `engine_.processTable()`). The HeadlessGameRunnerTest proves pokergamecore runs full tournaments — single-table and multi-table — without Swing dependencies.

**What exists today:**
- `TournamentEngine` — 826-line state machine handling all 18 table states
- `HeadlessGameRunnerTest` — proves the engine runs complete tournaments headlessly
- `SwingEventBus` / `SwingPlayerActionProvider` — desktop adapters (reference implementations)
- `GameEventBus`, `PlayerActionProvider`, `GameTable`, `GameHand`, `TournamentContext` — clean interfaces
- `ServerAIProvider` (skeleton) — Phase 7D will make this functional

**What this plan creates:**
A new `pokergameserver` Spring Boot auto-configuration module that hosts poker games server-side. The same module embeds in the desktop client (localhost) and runs standalone (remote server).

**Key insight from HeadlessGameRunnerTest:** The game loop is straightforward:
```
while (!gameOver) {
    for each table:
        result = engine.processTable(table, tournament, isHost, isOnline)
        apply state transitions from result
        handle phases (server-side, no Swing)
    consolidate tables (move players from emptied tables)
}
```
ServerTournamentDirector is this loop with real game objects, WebSocket broadcasting, and human player support.

---

## Prerequisites

| # | Prerequisite | Status | Notes |
|---|-------------|--------|-------|
| P1 | V2 AI Extraction | ✅ Complete | V1/V2 in pokergamecore |
| P2 | Phase 7D: ServerAIProvider | DRAFT | Plan ready, not started |
| P3 | TournamentEngine Integration | ✅ Complete | Engine fully integrated into TD |

**P3 update:** The master plan listed P3 as incomplete, but investigation shows TournamentDirector's `processTable()` (line 638) calls `engine_.processTable()` on every cycle. The old `_processTable()` is gone. P3 is done.

**Blocking dependency:** Phase 7D (ServerAIProvider) must be complete before Step 3 of this plan (ServerPlayerActionProvider delegates to ServerAIProvider for AI players). Steps 1-2 can proceed in parallel with Phase 7D.

---

## Current State Inventory

| Component | Location | Status |
|-----------|----------|--------|
| TournamentEngine | pokergamecore | ✅ Complete, 18 states |
| GameEventBus | pokergamecore | ✅ Simple pub/sub |
| PlayerActionProvider | pokergamecore | ✅ Interface defined |
| GameTable / GameHand / TournamentContext | pokergamecore | ✅ Interfaces defined |
| HeadlessGameTable / HeadlessPlayer / etc. | pokerserver (test) | ✅ Test implementations |
| PokerGame / PokerTable / HoldemHand | pokerengine | ✅ Production implementations |
| SwingEventBus | poker | ✅ Reference adapter |
| SwingPlayerActionProvider | poker | ✅ Reference adapter |
| ServerAIProvider | pokerserver | Skeleton (Phase 7D) |
| OtherTables consolidation | poker | In poker module (Swing dep chain) |

---

## Implementation Plan

### Step 1: Create `pokergameserver` Maven Module

**Goal:** New module with Spring Boot auto-configuration that provides game server capabilities to any Spring Boot application that includes it.

#### 1a. Module structure

```
code/pokergameserver/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/donohoedigital/games/poker/gameserver/
    │   │   ├── GameServerAutoConfiguration.java
    │   │   ├── GameServerProperties.java
    │   │   ├── GameInstance.java
    │   │   ├── GameInstanceManager.java
    │   │   ├── ServerTournamentDirector.java
    │   │   ├── ServerPlayerActionProvider.java
    │   │   ├── ServerPlayerSession.java
    │   │   ├── GameStateProjection.java
    │   │   └── event/
    │   │       ├── ServerGameEventBus.java
    │   │       └── GameEventStore.java
    │   └── resources/
    │       └── META-INF/
    │           └── spring/
    │               └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    └── test/
        └── java/com/donohoedigital/games/poker/gameserver/
```

#### 1b. pom.xml

```xml
<project>
    <parent>
        <groupId>com.donohoedigital.games.poker</groupId>
        <artifactId>ddpoker</artifactId>
    </parent>
    <artifactId>pokergameserver</artifactId>
    <name>DD Poker - Game Server</name>
    <description>Server-side game hosting for DD Poker</description>

    <dependencies>
        <!-- Core game logic (Swing-free) -->
        <dependency>
            <groupId>${project.groupId}</groupId>
            <artifactId>pokergamecore</artifactId>
        </dependency>

        <!-- Game engine types (PokerGame, PokerTable, HoldemHand) -->
        <dependency>
            <groupId>${project.groupId}</groupId>
            <artifactId>pokerengine</artifactId>
        </dependency>

        <!-- Spring Boot auto-configuration -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

**Must NOT depend on:** `poker` (Swing client), `gameengine` (Swing framework), `gui`, `gameserver` (legacy server framework), `pokerserver` (has Swing transitive deps via poker).

**Critical constraint:** This module is Swing-free. Unlike `pokerserver` which depends on `poker`, this module only depends on `pokergamecore` and `pokerengine`. The Maven Enforcer plugin should ban `javax.swing` and `java.awt` imports (same as pokergamecore).

**Dependency question:** pokerengine provides `PokerGame`, `PokerTable`, `HoldemHand` — the production implementations of the core interfaces. We need these for real game execution (not just the headless test stubs). Need to verify pokerengine itself has no Swing dependencies. If it does, we use the interfaces from pokergamecore and provide server-specific implementations.

#### 1c. Spring Boot auto-configuration

**File:** `GameServerAutoConfiguration.java`
```java
@AutoConfiguration
@EnableConfigurationProperties(GameServerProperties.class)
@ConditionalOnProperty(name = "game.server.enabled", havingValue = "true", matchIfMissing = true)
public class GameServerAutoConfiguration {

    @Bean
    public GameInstanceManager gameInstanceManager(GameServerProperties properties) {
        return new GameInstanceManager(properties);
    }
}
```

**File:** `GameServerProperties.java`
```java
@ConfigurationProperties(prefix = "game.server")
public record GameServerProperties(
    int maxConcurrentGames,       // default 50
    int actionTimeoutSeconds,     // default 30 (0 = no timeout)
    int reconnectTimeoutSeconds,  // default 120
    int threadPoolSize            // default 10
) {
    public GameServerProperties {
        if (maxConcurrentGames <= 0) maxConcurrentGames = 50;
        if (actionTimeoutSeconds < 0) actionTimeoutSeconds = 30;
        if (reconnectTimeoutSeconds < 0) reconnectTimeoutSeconds = 120;
        if (threadPoolSize <= 0) threadPoolSize = 10;
    }
}
```

**Auto-configuration registration:** `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` contains:
```
com.donohoedigital.games.poker.gameserver.GameServerAutoConfiguration
```

#### 1d. Register module in parent pom

**File:** `code/pom.xml` — add `pokergameserver` to `<modules>` list, positioned after `pokergamecore` and `pokerengine` but before `pokerserver` and `api`.

---

### Step 2: ServerTournamentDirector

**Goal:** Server-side game loop that drives TournamentEngine without Swing. This is the equivalent of TournamentDirector's `run()` method, but uses server-side adapters instead of Swing phases.

**File:** `ServerTournamentDirector.java`

**Design:** Follows the exact pattern proven by HeadlessGameRunnerTest, but with:
- Real `PokerGame`/`PokerTable`/`HoldemHand` objects (not headless stubs)
- `ServerGameEventBus` (broadcasts via callback, not Swing EDT)
- `ServerPlayerActionProvider` (routes to AI or waits for human input)
- Multi-table consolidation built in
- Pause/resume support
- Clean shutdown

```java
public class ServerTournamentDirector implements Runnable {
    private final TournamentEngine engine;
    private final TournamentContext tournament;
    private final ServerGameEventBus eventBus;
    private final ServerPlayerActionProvider actionProvider;
    private final GameServerProperties properties;

    private volatile boolean running;
    private volatile boolean paused;
    private volatile boolean shutdownRequested;

    // Callback for game lifecycle events (GameInstance listens)
    private final Consumer<GameLifecycleEvent> lifecycleCallback;
}
```

#### 2a. Main game loop

```java
@Override
public void run() {
    running = true;
    try {
        while (running && !shutdownRequested) {
            if (paused) {
                sleepMillis(SLEEP_MILLIS);
                continue;
            }

            boolean allSleep = processAllTables();

            if (tournament.isGameOver()) {
                handleGameOver();
                break;
            }

            if (allSleep) {
                sleepMillis(SLEEP_MILLIS);
            }
        }
    } catch (Exception e) {
        handleFatalError(e);
    } finally {
        running = false;
        lifecycleCallback.accept(GameLifecycleEvent.COMPLETED);
    }
}
```

#### 2b. Table processing (mirrors TD.process())

```java
private boolean processAllTables() {
    boolean allSleep = true;

    for (int i = 0; i < tournament.getNumTables(); i++) {
        GameTable table = tournament.getTable(i);

        if (table.getTableState() == TableState.GAME_OVER) continue;

        TableProcessResult result = engine.processTable(
            table, tournament, /*isHost=*/true, /*isOnline=*/true
        );

        applyResult(table, result);
        allSleep &= result.shouldSleep();
    }

    // Multi-table consolidation after processing all tables
    if (tournament.getNumTables() > 1) {
        consolidateTables();
    }

    return allSleep;
}
```

#### 2c. Result handling (replaces TD.processTable() post-engine logic)

In TournamentDirector, after `engine_.processTable()`, the result is copied to TDreturn, then:
1. Phases are run via `context_.processPhase()` (Swing UI phases)
2. State transitions are applied to the table
3. Events are broadcast via OnlineManager

ServerTournamentDirector replaces these with server-side equivalents:

```java
private void applyResult(GameTable table, TableProcessResult result) {
    // 1. Handle "phases" server-side (no Swing UI)
    if (result.phaseToRun() != null) {
        handleServerPhase(table, result.phaseToRun(), result.phaseParams());
    }

    // 2. Apply state transitions
    if (result.pendingState() != null) {
        table.setPendingTableState(result.pendingState());
        table.setTableState(TableState.PENDING);
    } else if (result.nextState() != null) {
        table.setTableState(result.nextState());
    }

    // 3. Broadcast state to connected clients (via GameInstance callback)
    eventBus.broadcastTableState(table);
}
```

#### 2d. Server-side phase handling

TournamentDirector runs Swing UI phases like "TD.WaitForDeal", "ShowTournamentTable", etc. On the server, these phases are either:
- **Auto-completed** (no UI needed — e.g., "TD.WaitForDeal" just proceeds)
- **Converted to WebSocket messages** (client handles display)
- **Timed delays** (simulate animation time for client rendering)

```java
private void handleServerPhase(GameTable table, String phase, Map<String, Object> params) {
    switch (phase) {
        case "TD.WaitForDeal":
            // Auto-deal: no manual button press needed on server
            table.setTableState(TableState.CHECK_END_HAND);
            break;
        case "ShowTournamentTable":
        case "TournamentShowdown":
            // Client renders these — just broadcast state and apply pending
            if (table.getPendingTableState() != null) {
                table.setTableState(table.getPendingTableState());
            }
            break;
        default:
            // Unknown phase — treat as auto-complete
            if (table.getPendingTableState() != null) {
                table.setTableState(table.getPendingTableState());
            }
            break;
    }
}
```

**Important:** This needs an audit of ALL phases the engine can request. Read through `TournamentEngine` to catalog every `phaseToRun()` value and determine the server-side handling for each.

#### 2e. Multi-table consolidation

The existing `OtherTables.consolidateTables()` in the poker module is ~200 lines of pure game logic but sits in a module with Swing transitive dependencies. For the server module, we have two options:

**Option A (Recommended): Extract consolidation to pokergamecore**
Move the core algorithm to pokergamecore as `TableConsolidator`. It operates purely on `GameTable` and `GamePlayerInfo` interfaces — no Swing types needed. Both TournamentDirector and ServerTournamentDirector call the same logic.

**Option B: Reimplement in pokergameserver**
Write a simpler version based on HeadlessGameRunnerTest's consolidation pattern. Simpler but duplicates logic.

Recommendation: **Option A** — one implementation, shared. The consolidation algorithm uses only `getNumOccupiedSeats()`, `getPlayer()`, seat management — all on the `GameTable` interface.

```java
// In pokergamecore:
public class TableConsolidator {
    public static void consolidate(TournamentContext tournament) {
        // Port from OtherTables.consolidateTables()
        // Uses GameTable interface only
    }
}
```

#### 2f. Pause/resume

```java
public void pause() {
    this.paused = true;
    eventBus.publishLifecycleEvent(GameLifecycleEvent.PAUSED);
}

public void resume() {
    this.paused = false;
    eventBus.publishLifecycleEvent(GameLifecycleEvent.RESUMED);
}

public void shutdown() {
    this.shutdownRequested = true;
}
```

---

### Step 3: ServerPlayerActionProvider

**Goal:** Routes action requests to AI (via ServerAIProvider from Phase 7D) or to human players (via callback that GameInstance connects to WebSocket).

**File:** `ServerPlayerActionProvider.java`

```java
public class ServerPlayerActionProvider implements PlayerActionProvider {
    private final PlayerActionProvider aiProvider;  // ServerAIProvider from Phase 7D
    private final Map<Integer, CompletableFuture<PlayerAction>> pendingActions;
    private final Consumer<ActionRequest> actionRequestCallback;
    private final int timeoutSeconds;
}
```

#### 3a. Action routing

```java
@Override
public PlayerAction getAction(GamePlayerInfo player, ActionOptions options) {
    if (player.isComputer()) {
        return aiProvider.getAction(player, options);
    }
    return getHumanAction(player, options);
}
```

#### 3b. Human action via CompletableFuture

```java
private PlayerAction getHumanAction(GamePlayerInfo player, ActionOptions options) {
    CompletableFuture<PlayerAction> future = new CompletableFuture<>();
    pendingActions.put(player.getID(), future);

    // Notify via callback (GameInstance sends WebSocket ACTION_REQUIRED)
    actionRequestCallback.accept(new ActionRequest(player, options));

    try {
        if (timeoutSeconds > 0) {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        }
        // No timeout (practice mode) — wait indefinitely
        return future.get();
    } catch (TimeoutException e) {
        // Auto-fold on timeout
        return options.canCheck() ? PlayerAction.check() : PlayerAction.fold();
    } catch (Exception e) {
        return PlayerAction.fold();
    } finally {
        pendingActions.remove(player.getID());
    }
}
```

#### 3c. Receiving human actions (called by GameInstance from WebSocket)

```java
public void submitAction(int playerId, PlayerAction action) {
    CompletableFuture<PlayerAction> future = pendingActions.get(playerId);
    if (future != null) {
        future.complete(action);
    }
}
```

#### 3d. Action validation

Before completing the future, validate the action:

```java
public void submitAction(int playerId, PlayerAction action, ActionOptions currentOptions) {
    CompletableFuture<PlayerAction> future = pendingActions.get(playerId);
    if (future == null) return; // Not this player's turn

    PlayerAction validated = validateAction(action, currentOptions);
    future.complete(validated);
}

private PlayerAction validateAction(PlayerAction action, ActionOptions options) {
    return switch (action.actionType()) {
        case FOLD -> options.canFold() ? action : PlayerAction.fold();
        case CHECK -> options.canCheck() ? action : PlayerAction.fold();
        case CALL -> options.canCall() ? action : PlayerAction.fold();
        case BET -> {
            if (!options.canBet()) yield PlayerAction.fold();
            int amount = Math.max(options.minBet(), Math.min(action.amount(), options.maxBet()));
            yield PlayerAction.bet(amount);
        }
        case RAISE -> {
            if (!options.canRaise()) yield PlayerAction.fold();
            int amount = Math.max(options.minRaise(), Math.min(action.amount(), options.maxRaise()));
            yield PlayerAction.raise(amount);
        }
        default -> PlayerAction.fold();
    };
}
```

**Design note:** TournamentEngine's `handleBetting()` already calls `actionProvider.getAction()` which blocks the engine thread until the action is received. This is the same pattern as SwingPlayerActionProvider (which blocks on a CountDownLatch). CompletableFuture is the server equivalent.

---

### Step 4: ServerGameEventBus

**Goal:** Event bus that broadcasts game events to listeners (GameInstance forwards to WebSocket clients) and optionally persists to event store.

**File:** `ServerGameEventBus.java`

```java
public class ServerGameEventBus extends GameEventBus {
    private final GameEventStore eventStore;  // Always present
    private Consumer<GameEvent> broadcastCallback;  // GameInstance sets this

    @Override
    public void publish(GameEvent event) {
        // 1. Persist to event store (always — event store is the authoritative log)
        eventStore.append(event);

        // 2. Notify in-process listeners (same as base GameEventBus)
        super.publish(event);

        // 3. Broadcast to connected clients
        if (broadcastCallback != null) {
            broadcastCallback.accept(event);
        }
    }

    public void broadcastTableState(GameTable table) {
        // Publish a synthetic event for table state changes
        publish(new GameEvent.TableStateChanged(
            table.getNumber(),
            table.getPreviousTableState(),
            table.getTableState()
        ));
    }
}
```

**Relationship to SwingEventBus:** SwingEventBus converts GameEvents to legacy PokerTableEvents and dispatches on Swing EDT. ServerGameEventBus skips that entirely — events go directly to the event store and WebSocket broadcast.

---

### Step 5: GameInstance

**Goal:** Encapsulates one running game. Owns the ServerTournamentDirector, player sessions, and game state. This is the primary unit of game hosting.

**File:** `GameInstance.java`

#### 5a. State machine

```
GameInstance States:
  CREATED → WAITING_FOR_PLAYERS → STARTING → IN_PROGRESS → PAUSED → IN_PROGRESS → COMPLETED
                                                                                   → CANCELLED
                                  WAITING_FOR_PLAYERS → CANCELLED
```

#### 5b. Core structure

```java
public class GameInstance {
    private final String gameId;
    private final long ownerProfileId;
    private final GameServerProperties properties;

    // Game state
    private volatile GameInstanceState state;
    private ServerTournamentContext tournament;  // Swing-free TournamentContext
    private ServerTournamentDirector director;
    private ServerPlayerActionProvider actionProvider;
    private ServerGameEventBus eventBus;

    // Player tracking
    private final Map<Integer, ServerPlayerSession> playerSessions;  // profileId → session
    private final ReentrantLock stateLock;

    // Threading
    private Future<?> directorFuture;  // From GameInstanceManager's executor
}
```

#### 5c. Game creation

```java
public static GameInstance create(String gameId, long ownerProfileId,
                                   TournamentProfile profile,
                                   GameServerProperties properties) {
    GameInstance instance = new GameInstance(gameId, ownerProfileId, properties);
    instance.state = GameInstanceState.CREATED;
    return instance;
}
```

ServerTournamentContext is created in `start()` after all players have joined, since we need the player list to distribute across tables.

#### 5d. Player management

```java
public void addPlayer(long profileId, String playerName, boolean isAI, int skillLevel) {
    if (state != GameInstanceState.WAITING_FOR_PLAYERS) {
        throw new IllegalStateException("Cannot add players in state: " + state);
    }
    // Create player, assign seat, track session
    ServerPlayerSession session = new ServerPlayerSession(profileId, playerName, isAI, skillLevel);
    playerSessions.put((int) profileId, session);
}

public void removePlayer(long profileId) {
    if (state == GameInstanceState.IN_PROGRESS) {
        // Mark as disconnected, don't remove
        ServerPlayerSession session = playerSessions.get((int) profileId);
        if (session != null) session.setDisconnected(true);
    } else {
        playerSessions.remove((int) profileId);
    }
}
```

#### 5e. Game start

```java
public void start(ExecutorService executor) {
    if (state != GameInstanceState.WAITING_FOR_PLAYERS) {
        throw new IllegalStateException("Cannot start in state: " + state);
    }

    // Initialize game objects
    GameEventStore eventStore = new GameEventStore(gameId);
    eventBus = new ServerGameEventBus(eventStore);
    actionProvider = new ServerPlayerActionProvider(
        createAIProvider(),
        this::onActionRequest,
        properties.actionTimeoutSeconds()
    );

    TournamentEngine engine = new TournamentEngine(eventBus, actionProvider);
    director = new ServerTournamentDirector(
        engine, pokerGame, eventBus, actionProvider, properties,
        this::onLifecycleEvent
    );

    state = GameInstanceState.IN_PROGRESS;
    directorFuture = executor.submit(director);
}
```

#### 5f. WebSocket integration point

GameInstance provides callbacks that the WebSocket layer (Milestone 3) will connect to:

```java
// Called when engine needs a human player's action
private void onActionRequest(ActionRequest request) {
    ServerPlayerSession session = playerSessions.get(request.player().getID());
    if (session != null && session.getMessageSender() != null) {
        session.getMessageSender().accept(
            new ActionRequiredMessage(request.options())
        );
    }
}

// Called by WebSocket handler when player submits action
public void onPlayerAction(int profileId, PlayerAction action) {
    actionProvider.submitAction(profileId, action);
}
```

---

### Step 6: GameInstanceManager

**Goal:** Registry and lifecycle manager for all GameInstances. Thread pool management.

**File:** `GameInstanceManager.java`

```java
@Component
public class GameInstanceManager {
    private final ConcurrentHashMap<String, GameInstance> games;
    private final ScheduledExecutorService executor;
    private final GameServerProperties properties;

    public GameInstanceManager(GameServerProperties properties) {
        this.properties = properties;
        this.games = new ConcurrentHashMap<>();
        this.executor = Executors.newScheduledThreadPool(properties.threadPoolSize());
    }
}
```

#### 6a. Game lifecycle

```java
public GameInstance createGame(long ownerProfileId, TournamentProfile profile) {
    if (games.size() >= properties.maxConcurrentGames()) {
        throw new GameServerException("Maximum concurrent games reached");
    }

    String gameId = generateGameId();
    GameInstance instance = GameInstance.create(gameId, ownerProfileId, profile, properties);
    games.put(gameId, instance);
    return instance;
}

public void startGame(String gameId, long requesterId) {
    GameInstance instance = getGameOrThrow(gameId);
    if (instance.getOwnerProfileId() != requesterId) {
        throw new GameServerException("Only the owner can start the game");
    }
    instance.start(executor);
}

public GameInstance getGame(String gameId) {
    return games.get(gameId);
}

public List<GameInstance> listGames(GameInstanceState statusFilter) {
    return games.values().stream()
        .filter(g -> statusFilter == null || g.getState() == statusFilter)
        .toList();
}
```

#### 6b. Cleanup

```java
@Scheduled(fixedRate = 60_000) // Every minute
public void cleanupCompletedGames() {
    games.entrySet().removeIf(entry -> {
        GameInstance game = entry.getValue();
        return game.getState() == GameInstanceState.COMPLETED
            && game.getCompletedAt() != null
            && game.getCompletedAt().isBefore(Instant.now().minus(Duration.ofHours(1)));
    });
}
```

---

### Step 7: ServerPlayerSession

**Goal:** Track one player's connection state within a game.

**File:** `ServerPlayerSession.java`

```java
public class ServerPlayerSession {
    private final long profileId;
    private final String playerName;
    private final boolean isAI;
    private final int skillLevel;

    private volatile boolean connected;
    private volatile boolean disconnected;
    private Instant disconnectedAt;
    private Consumer<Object> messageSender;  // WebSocket message callback (set when WS connects)
    private int consecutiveTimeouts;

    // Getters, connection management, timeout tracking
}
```

---

### Step 8: GameStateProjection

**Goal:** Build a per-player view of game state that hides other players' hole cards. Used when sending state to a specific client.

**File:** `GameStateProjection.java`

```java
public class GameStateProjection {

    /**
     * Create a state snapshot for a specific player.
     * Includes: all public information + this player's hole cards.
     * Excludes: other players' hole cards (except at showdown).
     */
    public static GameStateSnapshot forPlayer(GameInstance game, int playerId) {
        // Build table state, hiding cards appropriately
        // This class becomes critical in Milestone 3 (WebSocket messages)
        // For now, provide the data model and projection logic
    }
}
```

The full implementation of `GameStateSnapshot` (the DTO) will be completed in Milestone 3 when the WebSocket protocol is built. In M1, the projection logic handles the core security rule: **never leak hole cards to the wrong player**.

---

### Step 9: Event Store

**Goal:** Append-only event log for game events. The event store is always present — it is the authoritative record of every game action. It enables crash recovery, game replay, and simulation analysis.

**File:** `GameEventStore.java`

#### 9a. Core Implementation

```java
public class GameEventStore {
    private final String gameId;
    private final AtomicLong sequenceNumber;
    private final List<StoredEvent> events;  // In-memory for M1

    public void append(GameEvent event) {
        StoredEvent stored = new StoredEvent(
            gameId,
            sequenceNumber.incrementAndGet(),
            event.getClass().getSimpleName(),
            event,
            Instant.now()
        );
        events.add(stored);
    }

    public List<StoredEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public List<StoredEvent> getEventsSince(long afterSequence) {
        return events.stream()
            .filter(e -> e.sequenceNumber() > afterSequence)
            .toList();
    }
}
```

**The event store is NOT optional.** Every game always records events. This is critical for:
- Simulation testing and analysis (run thousands of games, query results)
- Crash recovery and game replay
- Hand history and game statistics
- Future database persistence (events are the source of truth)

#### 9b. Database persistence (deferred to Milestone 2)

The master plan specifies H2 database storage with a `game_events` table. For M1, the event store uses **in-memory storage** — events are collected and queryable within the game's lifetime but don't persist to disk.

H2 database persistence is completed in **Milestone 2** alongside the REST API. The event store interface is designed so that swapping in a database-backed implementation requires no changes to any other code.

**Design for database readiness:** The `GameEventStore` API (append, getEvents, getEventsSince) is storage-agnostic. In M2, a `DatabaseGameEventStore` implementation will write to H2 while keeping the same interface. This enables simulation testing with persistent results.

---

### Step 10: Server Game Object Implementations

**Goal:** Create Swing-free implementations of the four core game interfaces. These replace `PokerGame`, `PokerTable`, `PokerPlayer`, and `HoldemHand` for server-side use.

**Why rewrite instead of reuse:** Investigation confirmed PokerGame/PokerPlayer cannot be instantiated without Swing:
- `PokerGame` extends `Game` which requires `GameEngine` singleton (javax.swing.*, java.awt.*)
- `PokerPlayer` extends `GamePlayer` (javax.swing.*, javax.swing.event.*)
- `PokerTable` and `HoldemHand` are Swing-free in their own code but take PokerGame/PokerPlayer as constructor args
- The inheritance chain (`Game` → `GameContext` → `GameEngine`) is deeply entangled with Swing

**Approach:** Write clean server implementations that use composition, not inheritance. Port the essential game logic from the existing classes, dropping all UI/serialization/database concerns. Reference HoldemHand (~3,625 lines) but the server version needs only ~750-1,200 lines.

#### 10a. ServerPlayer (GamePlayerInfo)

**File:** `ServerPlayer.java` (~80 lines)

Replaces PokerPlayer. Simple data class — no GamePlayer inheritance.

```java
public class ServerPlayer implements GamePlayerInfo {
    private final int id;
    private final String name;
    private final boolean human;
    private int chipCount;
    private int seat;
    private boolean folded;
    private boolean allIn;
    private boolean sittingOut;
    private boolean observer;
    private int numRebuys;

    // Think bank for timed tournaments
    private int thinkBankMillis;
    private int timeoutMillis;
    private int timeoutMessageSecondsLeft;

    // Card-showing preferences (defaults for server)
    private boolean askShowWinning = false;
    private boolean askShowLosing = false;

    // GamePlayerInfo implementation — all direct field access
}
```

**What's dropped vs PokerPlayer:**
- No `GamePlayer` parent (no Swing PropertyChangeListener support)
- No `Hand` objects (cards managed by ServerHand)
- No `PlayerProfile` reference
- No save/load serialization

#### 10b. ServerGameTable (GameTable)

**File:** `ServerGameTable.java` (~250 lines)

Replaces PokerTable. Manages seat array and delegates hand creation.

```java
public class ServerGameTable implements GameTable {
    private final int tableNumber;
    private final int seats;            // Typically 10
    private final ServerPlayer[] players;  // Null = empty seat
    private final ServerTournamentContext tournament;

    private TableState tableState = TableState.NONE;
    private TableState pendingTableState;
    private TableState previousTableState;
    private String pendingPhase;

    private ServerHand currentHand;
    private int handNum;
    private int level;
    private int button = -1;           // Dealer button seat
    private int minChip;
    private int nextMinChip;

    // Wait list for online coordination
    private final List<ServerPlayer> waitList = new ArrayList<>();
    private final List<ServerPlayer> addedList = new ArrayList<>();

    // Timing
    private long lastStateChangeMillis;
    private int pauseMillis;
    private int autoDealDelay;
    private boolean autoDeal = true;
    private boolean zipMode;
}
```

**Key methods to implement:**

| Method | Logic | Source Reference |
|--------|-------|-----------------|
| `startNewHand()` | Advance button, increment hand#, create ServerHand, deal | PokerTable.startNewHand() |
| `simulateHand()` | Fast all-AI hand (random pot distribution) | PokerTable.simulateHand() |
| `getNextSeat(seat)` | Next occupied seat, wraps around | PokerTable.getNextSeat() |
| `getNextSeatAfterButton()` | First occupied seat after dealer | PokerTable.getNextSeatAfterButton() |
| `processAIRebuys()` / `processAIAddOns()` | Process AI rebuy/addon decisions | PokerTable.processAIRebuys() |
| `setButton()` | Deal for button (high card) | PokerTable.setButton() |
| `colorUp()` / `colorUpFinish()` | Chip denomination change | PokerTable low-priority (can stub initially) |
| Seat management | getPlayer, getNumOccupiedSeats, getSeat | Direct array access |

#### 10c. ServerHand (GameHand) — THE BIG ONE

**File:** `ServerHand.java` (~800-1,000 lines)

Replaces HoldemHand. This is the core poker hand state machine. Port logic from HoldemHand (3,625 lines), keeping only what the server needs.

**Internal state:**

```java
public class ServerHand implements GameHand {
    private final ServerGameTable table;
    private final Deck deck;                    // From pokerengine (Swing-free)

    // Cards
    private final Hand community = new Hand();
    private final Map<Integer, Hand> playerHands;  // playerId → hole cards

    // Betting state
    private BettingRound round = BettingRound.NONE;
    private List<ServerPlayer> playerOrder;
    private int currentPlayerIndex = -1;

    // Pots
    private final List<ServerPot> pots = new ArrayList<>();

    // Action history
    private final List<ServerHandAction> history = new ArrayList<>();

    // Blinds/antes for this hand
    private final int smallBlindAmount;
    private final int bigBlindAmount;
    private final int anteAmount;
    private int smallBlindSeat;
    private int bigBlindSeat;

    // Status tracking
    private boolean done;
    private boolean allInShowdown;
    private int potStatus;  // NO_POT_ACTION, CALLED_POT, RAISED_POT, RERAISED_POT

    // Resolution
    private List<ServerPlayer> preWinners;
    private List<ServerPlayer> preLosers;
}
```

**Logic to port by concern:**

**Dealing (~50 lines):**
- `deal()` — set player order, deal 2 hole cards each, post antes, post blinds
- Uses `Deck` from pokerengine (Swing-free)
- `advanceRound()` — burn + deal community cards (3 for flop, 1 for turn/river)

**Player ordering (~80 lines):**
- `setPlayerOrder(round)` — PRE_FLOP: start after BB; POST_FLOP: start after button
- `getCurrentPlayerWithInit()` — lazy init, skip folded/all-in
- `playerActed()` — advance to next eligible player

**Action processing (~100 lines):**
- `applyPlayerAction(player, action)` — convert PlayerAction to internal action, update pots
- Action methods: fold, check, call, bet, raise — update player state, add to history
- `addToPot(player, amount)` — deduct chips, add to current pot, recalculate side pots

**Pot management (~150 lines) — PORT CAREFULLY from HoldemHand.calcPots():**
- `ServerPot` inner class: chips, eligible players, winners
- `calcPots()` — side pot calculation when players go all-in at different levels
  - Sort players by total bet (lowest first)
  - Create new side pot at each all-in boundary
  - Distribute chips level-by-level
- This is the most complex piece — correctness is critical

**Bet calculation (~80 lines):**
- `getMinBet()` — big blind (doubled on turn/river for limit)
- `getMaxBet(player)` — player's stack (no-limit), pot (pot-limit), min bet (limit)
- `getMinRaise()` — at least the size of the previous bet/raise
- `getAmountToCall(player)` — current bet level minus player's current bet

**Completion detection (~60 lines):**
- `isDone()` — uncontested (1 player left) OR all players acted and pot is good
- `isPotGood()` — all non-folded players have matched the current bet (or are all-in)
- `getNumWithCards()` — count non-folded players

**Showdown resolution (~200 lines) — PORT from HoldemHand.resolvePot():**
- `preResolve()` — determine winners/losers before card exposure
- `resolve()` — for each pot: evaluate hands, find winner(s), distribute chips
- Hand evaluation via `HandInfoFaster` (in pokerengine, Swing-free — see Step 10f)
- Split pot handling: divide evenly, distribute odd chips by position
- `resolvePot(potIndex)` — find highest hand score, award chips

**GameHand interface query methods (~250 lines):**
- ~40 methods for V2 AI integration: betting history, player state, pot odds
- Most are simple lookups into history/state
- Port directly from HoldemHand's GameHand implementations

**What's dropped vs HoldemHand:**
- No `firePokerTableEvent()` calls (events go through ServerGameEventBus)
- No `PokerDatabase.storeHandHistory()` (event store handles persistence)
- No `recordHandInfo()` / player profile stats
- No `DataMarshal` serialization
- No `sDealPlayableHands_` testing feature
- No synchronized blocks (single-threaded per game instance)

#### 10d. ServerTournamentContext (TournamentContext)

**File:** `ServerTournamentContext.java` (~300 lines)

Replaces PokerGame. Manages tournament state: tables, players, levels, clock.

```java
public class ServerTournamentContext implements TournamentContext {
    private final List<ServerGameTable> tables;
    private final List<ServerPlayer> allPlayers;
    private final TournamentProfile profile;

    // Level management
    private int currentLevel;
    private long levelStartTimeMillis;
    private long totalPauseTimeMillis;

    // Tournament state
    private boolean gameOver;
    private boolean practice;  // vs online

    // Configuration from TournamentProfile
    private final int[] smallBlinds;   // Per level
    private final int[] bigBlinds;     // Per level
    private final int[] antes;         // Per level
    private final int[] levelMinutes;  // Duration per level
    private final int[] minChips;      // Chip denominations per level
    private final boolean[] breakLevels;  // Which levels are breaks
    private final int startingChips;
    private final int maxRebuys;
    private final int rebuyMaxLevel;
    private final boolean allowAddons;
}
```

**Key methods to implement:**

| Method | Logic | Source Reference |
|--------|-------|-----------------|
| `getSmallBlind(level)` / `getBigBlind(level)` / `getAnte(level)` | Array lookup from profile | PokerGame blind structure |
| `nextLevel()` | Increment level, update min chip | PokerGame.nextLevel() |
| `isLevelExpired()` | Compare elapsed time vs level duration | PokerGame clock logic |
| `isBreakLevel(level)` | Array lookup | TournamentProfile |
| `isOnePlayerLeft()` | Count players with chips > 0 | PokerGame.isOnePlayerLeft() |
| `isGameOver()` | One player left or explicit end | PokerGame |
| `advanceClock()` / `advanceClockBreak()` | Progress time-based levels | PokerGame clock |
| `isRebuyPeriodActive(player)` | Check level and rebuy count | PokerGame rebuy logic |
| `getNumTables()` / `getTable(i)` | List access | Direct |
| `getTimeoutForRound(round)` | Configurable per round | TournamentProfile or properties |

**Constructor from TournamentProfile:**
```java
public ServerTournamentContext(TournamentProfile profile,
                                List<ServerPlayer> players,
                                int numTables) {
    this.profile = profile;
    this.allPlayers = new ArrayList<>(players);
    this.startingChips = profile.getStartingChips();

    // Extract blind structure from profile
    int numLevels = profile.getNumLevels();
    this.smallBlinds = new int[numLevels];
    this.bigBlinds = new int[numLevels];
    this.antes = new int[numLevels];
    // ... populate from profile ...

    // Create and populate tables
    this.tables = createTables(players, numTables);
}
```

**What's dropped vs PokerGame:**
- No `Game` parent class (no GameContext, no GameEngine)
- No `DataMarshal` serialization — save/load is replaced by event sourcing (the event store IS the save; game state is rebuilt by replaying events). Players retain save/load capability via Milestone 4, Phase 4.4.
- No UI-related methods (territory, gameboard, etc.)
- No `GameEngine.getGameEngine()` singleton access
- No `PokerMain.getPokerMain()` access
- No `LanManager` / network registration — LAN discovery and WAN game listing are replaced by the REST API game registry in Milestone 6 (Game Discovery & Management).

#### 10e. ServerHandAction and ServerPot

**File:** `ServerHandAction.java` (~40 lines)

Simple record replacing HandAction (which extends DMArrayList — has DataMarshal serialization):

```java
public record ServerHandAction(
    ServerPlayer player,
    int round,
    int action,       // ACTION_FOLD, ACTION_CHECK, etc.
    int amount,
    int subAmount,    // Call portion for raises
    boolean allIn
) {
    // Action constants (same values as HandAction)
    public static final int ACTION_FOLD = 0;
    public static final int ACTION_CHECK = 1;
    // ... etc
}
```

**File:** `ServerPot.java` (~60 lines)

```java
public class ServerPot {
    private int chips;
    private final List<ServerPlayer> eligiblePlayers = new ArrayList<>();
    private final List<ServerPlayer> winners = new ArrayList<>();
    private int round;      // Round pot was created
    private int sideBet;    // All-in boundary amount

    public void addChips(ServerPlayer player, int amount) { ... }
    public boolean isPlayerEligible(ServerPlayer player) { ... }
    public boolean isOverbet() { return eligiblePlayers.size() == 1; }
}
```

#### 10f. Hand Evaluation Bridge

ServerHand needs to evaluate poker hands at showdown. Two Swing-free hand evaluators are available:

- **HandInfoFaster** (pokerengine, `com.donohoedigital.games.poker.engine.HandInfoFaster`) — ultra-fast hand scoring. No Swing dependencies. Used by AI code (V1Player, V2Player, ServerAIContext). Implements `HandScoreConstants`. Uses internal `SimpleCard`/`SimpleHand` for speed.
- **HandInfoFast** (pokergamecore, `com.donohoedigital.games.poker.core.ai.HandInfoFast`) — full-featured hand analysis with draw detection. No Swing dependencies (enforced by Maven Enforcer). Implements `HandScoreConstants`. Used by `PureHandPotential`.

Both are available since `pokergameserver` depends on `pokergamecore` and `pokerengine`.

**Decision: Use HandInfoFaster for showdown evaluation.** It's purpose-built for fast hand scoring and is already the standard evaluator used throughout the codebase for determining hand winners.

```java
// In ServerHand.resolve():
HandInfoFaster handInfo = new HandInfoFaster();
handInfo.getScore(playerHand, community);  // Populates score fields
int score = handInfo.getHandScore();       // Higher = better hand
```

Note: There is also a legacy `HandInfoFast` in the `poker` module — that one has indirect Swing dependencies and is NOT used here.

---

### Step 11: Tests

**All steps use test-driven development (TDD).** Tests are written BEFORE the implementation wherever practical. See the TDD section below for step-by-step test ordering.

#### 11a. ServerHandTest (written with Step 10c)

The most critical test class — validates all poker mechanics:
- **Dealing:** 2 cards per player, correct deck size after deal, community cards dealt correctly per round
- **Blind/ante posting:** Correct amounts deducted, correct pot size after posting
- **Action processing:** fold/check/call/bet/raise update player state and pot correctly
- **Pot management:** Side pots created when player goes all-in at different levels; odd chip distribution
- **Bet calculation:** getMinBet, getMaxBet, getMinRaise, getAmountToCall return correct values
- **Completion detection:** isDone() true when uncontested; isDone() true when all acted and pot good
- **Showdown resolution:** Correct winner(s) identified; split pots distributed; chips conserved
- **GameHand interface:** All ~46 interface methods return expected values

#### 11b. ServerTournamentContextTest (written with Step 10d)

- Blind structure: correct values per level
- Level progression: nextLevel() advances, isLevelExpired() triggers at correct time
- Player tracking: getNumPlayers(), isOnePlayerLeft(), isGameOver()
- Table management: getNumTables(), getTable()
- Break levels: correctly identified from profile
- Rebuy period: active during configured levels, inactive after

#### 11c. ServerGameTableTest (written with Step 10b)

- Seat management: add/remove players, getNumOccupiedSeats
- Button movement: advances correctly, skips empty seats
- startNewHand(): creates ServerHand, increments hand number
- getNextSeat/getNextSeatAfterButton: wraps correctly around table
- State transitions: tableState management

#### 11d. ServerPlayerTest (written with Step 10a)

- GamePlayerInfo interface contract: all getters return expected values
- Chip management: chipCount updates correctly
- State flags: folded, allIn, sittingOut

#### 11e. ServerPlayerActionProviderTest (written with Step 3)

- AI player actions route through aiProvider
- Human player actions via CompletableFuture
- Timeout produces auto-fold (or auto-check when possible)
- Action validation: rejects invalid amounts, clamps to valid range
- Concurrent action submissions (only one accepted)

#### 11f. ServerTournamentDirectorTest (written with Step 2)

- Single-table tournament with AI players completes
- Multi-table tournament with table consolidation completes
- Pause/resume works (game loop stops and resumes)
- Shutdown terminates cleanly
- Total chips conserved throughout

#### 11g. GameInstanceTest (written with Step 5)

- Lifecycle: CREATED → WAITING → IN_PROGRESS → COMPLETED
- Cannot add players after game starts
- Only owner can start/pause/cancel
- Player disconnect tracked, reconnect restores
- AI-only game runs to completion

#### 11h. GameInstanceManagerTest (written with Step 6)

- Create multiple games, list them
- Concurrent game limit enforced
- Cleanup removes completed games
- Game lookup by ID

#### 11i. GameStateProjectionTest (written with Step 8)

- Player's own cards included in their projection
- Other players' cards null (hidden)
- Showdown reveals winner's cards
- Community cards visible to all

#### 11j. Integration test: Full tournament lifecycle

Run a complete tournament through the server stack:
1. Create GameInstance with TournamentProfile
2. Add AI players
3. Start game
4. Game runs to completion (all AI)
5. Verify: winner has all chips, game state is COMPLETED, events logged

---

## Files Summary

### New Files

| File | Description |
|------|-------------|
| **Module Setup** | |
| `pokergameserver/pom.xml` | Module POM with Spring Boot starter deps |
| `GameServerAutoConfiguration.java` | Spring Boot auto-config |
| `GameServerProperties.java` | Configurable properties record |
| **Server Game Objects (Step 10)** | |
| `ServerPlayer.java` | GamePlayerInfo impl (~80 lines) |
| `ServerGameTable.java` | GameTable impl (~250 lines) |
| `ServerHand.java` | GameHand impl (~800-1,000 lines) — THE BIG ONE |
| `ServerTournamentContext.java` | TournamentContext impl (~300 lines) |
| `ServerHandAction.java` | Action record (~40 lines) |
| `ServerPot.java` | Pot tracking (~60 lines) |
| **Game Hosting Infrastructure** | |
| `ServerTournamentDirector.java` | Server-side game loop |
| `ServerPlayerActionProvider.java` | AI + human action routing |
| `ServerGameEventBus.java` | Event bus with persistence + broadcast |
| `ServerPlayerSession.java` | Per-player connection state |
| `GameStateProjection.java` | Per-player state view (card hiding) |
| `GameEventStore.java` | Append-only event log (in-memory for M1) |
| `GameInstance.java` | Single game encapsulation |
| `GameInstanceManager.java` | Game registry + lifecycle |
| **Supporting Types** | |
| `GameInstanceState.java` | Enum: CREATED, WAITING, IN_PROGRESS, etc. |
| `GameLifecycleEvent.java` | Enum: STARTED, PAUSED, RESUMED, COMPLETED |
| `ActionRequest.java` | Record: player + options (callback payload) |
| `GameServerException.java` | Runtime exception for game server errors |
| **Tests (10+ files)** | TDD — written before/alongside implementation |

### Modified Files

| File | Change |
|------|--------|
| `code/pom.xml` | Add `pokergameserver` to module list |

### Potentially Modified (During Implementation)

| File | Change | Condition |
|------|--------|-----------|
| `pokergamecore/.../TableConsolidator.java` | **New** — extracted from OtherTables | If Option A chosen for consolidation |
| `poker/.../OtherTables.java` | Delegate to TableConsolidator | If Option A chosen |

---

## Development Methodology: Test-Driven Development

**Tests are written BEFORE implementation for all steps.** Each step follows the red-green-refactor cycle:

1. **Red:** Write failing tests that define the expected behavior
2. **Green:** Write the minimum implementation to make tests pass
3. **Refactor:** Clean up while keeping tests green

### TDD Order by Step

The implementation order is designed so each step's tests can be written and run independently:

| Order | Step | TDD Approach |
|-------|------|-------------|
| 1 | Module setup | No tests — Maven config only |
| 2 | **ServerPlayer** (10a) | Write ServerPlayerTest first — simple data class, fast to TDD |
| 3 | **ServerPot** (10e) | Write ServerPotTest first — standalone pot logic |
| 4 | **ServerHandAction** (10e) | Write tests for record — trivial |
| 5 | **ServerHand** (10c) | Write ServerHandTest first — **most critical TDD target**. Test each concern independently: dealing, blind posting, action processing, pot calculation, isDone(), showdown. Build up incrementally. |
| 6 | **ServerGameTable** (10b) | Write ServerGameTableTest first — seat management, button, startNewHand (uses ServerHand) |
| 7 | **ServerTournamentContext** (10d) | Write ServerTournamentContextTest first — level progression, blinds, game-over detection |
| 8 | **ServerPlayerActionProvider** (3) | Write test first — mock AI provider, test CompletableFuture, timeout |
| 9 | **ServerGameEventBus** (4) | Write test first — verify events published and stored |
| 10 | **GameEventStore** (9) | Write test first — append, query, sequence numbers |
| 11 | **ServerTournamentDirector** (2) | Write test first — full game loop with server game objects. This is the integration point where all pieces connect. |
| 12 | **ServerPlayerSession** (7) | Write test first — connection state tracking |
| 13 | **GameStateProjection** (8) | Write test first — card hiding security |
| 14 | **GameInstance** (5) | Write test first — lifecycle state machine, start/pause/cancel |
| 15 | **GameInstanceManager** (6) | Write test first — registry, limits, cleanup |
| 16 | **Full integration test** (11j) | End-to-end: create game → add players → start → play to completion |

### TDD Notes for ServerHand

ServerHand is the largest and most complex class. TDD here by building up incrementally:

1. **Test dealing first:** Create hand, deal, verify each player has 2 cards, deck has correct remaining count
2. **Test blind posting:** After deal, verify blind amounts deducted, pot has correct total
3. **Test simple actions:** Single fold → hand done (uncontested). Check → player acted advances.
4. **Test call/bet/raise:** Verify pot updates, chip deductions, min/max constraints
5. **Test round advancement:** After all act, advanceRound → community cards dealt, new player order
6. **Test isDone():** Various scenarios (all fold to one, all check, bet-call sequences)
7. **Test side pots:** Player A all-in for less, B and C continue — verify correct pot split
8. **Test showdown:** Two players to showdown, higher hand wins correct amount
9. **Test split pot at showdown:** Identical hands split evenly, odd chip to correct player
10. **Test full hand lifecycle:** Deal → preflop actions → flop → actions → turn → actions → river → showdown → chips conserved

Each test should be self-contained and fast. Use fixed seeds for `Deck` shuffling where deterministic card sequences are needed.

---

## Implementation Order

| Order | Step | Description | Depends On | Est. Effort |
|-------|------|-------------|------------|-------------|
| 1 | **1** | Module setup + auto-config | None | S |
| 2 | **10a** | ServerPlayer | Step 1 | S |
| 3 | **10e** | ServerHandAction + ServerPot | Step 1 | S |
| 4 | **10c** | ServerHand (GameHand) | Steps 10a, 10e | **XL** |
| 5 | **10b** | ServerGameTable (GameTable) | Steps 10a, 10c | M |
| 6 | **10d** | ServerTournamentContext (TournamentContext) | Step 10b | L |
| 7 | **3** | ServerPlayerActionProvider | Step 1, Phase 7D | M |
| 8 | **4** | ServerGameEventBus | Step 1 | S |
| 9 | **9** | GameEventStore | Step 1 | S |
| 10 | **2** | ServerTournamentDirector | Steps 10d, 3, 4 | L |
| 11 | **7** | ServerPlayerSession | Step 1 | S |
| 12 | **8** | GameStateProjection | Step 10c | M |
| 13 | **5** | GameInstance | Steps 2, 3, 4, 10d | L |
| 14 | **6** | GameInstanceManager | Step 5 | M |
| 15 | **11j** | Full integration test | All above | M |

Steps 7-9 (ServerPlayerActionProvider, ServerGameEventBus, GameEventStore) can be built in parallel. Steps 11-12 (ServerPlayerSession, GameStateProjection) can also be built in parallel.

**The critical path is:** 1 → 10a → 10e → **10c (ServerHand)** → 10b → 10d → 2 → 5 → integration test

ServerHand (Step 10c) is the longest pole. Everything else flows from it.

---

## Key Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| ServerHand correctness (pot math, side pots, showdown) | **HIGH** | TDD with exhaustive test cases. Port logic line-by-line from HoldemHand. Verify chip conservation in every test. |
| Hand evaluation in pokergamecore is incomplete | LOW | **Resolved:** HandInfoFaster (pokerengine) is Swing-free and available. HandInfoFast (pokergamecore) also Swing-free. Both accessible from pokergameserver. Use HandInfoFaster for showdown scoring. |
| TournamentProfile parsing without ConfigManager | MEDIUM | TournamentProfile may use DD Poker's custom config system. May need a profile builder. |
| Multi-table consolidation extraction is complex | MEDIUM | Start simple (HeadlessGameRunnerTest pattern). Full OtherTables extraction can follow. |
| TournamentEngine phases are incomplete for server | MEDIUM | Audit all `phaseToRun()` values. Unknown phases auto-complete. |
| Thread safety in GameInstance | MEDIUM | Use explicit locks. Single director thread per game. CompletableFuture for cross-thread action passing. |
| pokerengine transitive Swing deps (via unused gamecommon) | LOW | Remove unused gamecommon dependency from pokerengine pom.xml. Add Enforcer ban in pokergameserver. |

---

## Verification

```bash
# After module setup (Step 1):
cd code && mvn compile -pl pokergameserver

# After core implementation (Steps 2-9):
cd code && mvn test -pl pokergameserver
cd code && mvn test -P dev   # Full regression

# After integration test (Step 11):
cd code && mvn verify -pl pokergameserver  # With coverage

# Enforcer check (no Swing dependencies):
cd code && mvn verify -pl pokergameserver  # Enforcer plugin validates
```

---

## Relationship to Next Milestones

This plan creates the **game engine foundation**. It does NOT include:
- REST API endpoints (Milestone 2)
- WebSocket protocol (Milestone 3)
- JWT authentication (Milestone 2)
- **H2 database persistence for event store** (Milestone 2) — M1 uses in-memory event store; M2 adds `DatabaseGameEventStore` with H2 backend for persistent game history, simulation analysis, and crash recovery
- Desktop client adaptation (Milestone 4)
- Web client (Milestone 5)

After M1, the game engine can run complete tournaments server-side with AI players. The in-memory event store captures all game events for testing and simulation within a session. Human players require the WebSocket protocol (M3) to send actions. The REST API (M2) provides game creation/management endpoints and adds database-backed event persistence. These milestones build on the foundation established here.
