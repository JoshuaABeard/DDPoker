# M4: Desktop Client Adaptation

**Status:** DRAFT
**Created:** 2026-02-16
**Parent Plan:** `.claude/plans/SERVER-HOSTED-GAME-ENGINE.md`
**Depends On:** M1 (complete), M2 (complete), M3 (complete)
**Effort:** XL (5-7 weeks estimated)
**Absorbs:** M7 (Legacy P2P Removal) — old code deleted in this milestone, not deferred

---

## Context

M1-M3 built the complete `pokergameserver` module: 74 production Java files, 45 test files, and all server-side infrastructure for hosting poker games. The module provides GameInstanceManager, ServerTournamentDirector, JWT auth (RS256), REST API for game CRUD, WebSocket protocol with 27 server-to-client message types and 9 client-to-server message types, and card privacy enforcement.

The desktop client (`code/poker/`) currently runs games using a tightly coupled architecture:
- `PokerMain` (916 lines) extends `GameEngine` (Swing singleton) and manages P2P/TCP connections via `PokerConnectionServer`
- `TournamentDirector` (1,969 lines) extends `BasePhase` and drives the local game loop
- `OnlineManager` (2,335 lines) implements P2P message routing between host and clients
- `ShowTournamentTable` (2,609 lines) is the main game UI, reading directly from `PokerGame`, `PokerTable`, and `HoldemHand`
- `PokerGame` (2,296 lines) extends `Game` and implements `TournamentContext`

**What this plan does:** Embed the Spring Boot `pokergameserver` in the desktop client JVM. Replace `OnlineManager`, `TournamentDirector`, and the `Bet` phase with a WebSocket client that talks to the embedded server. Create thin view model classes (`RemotePokerTable`, `RemoteHoldemHand`) so the existing Swing UI reads game state from WebSocket messages instead of local game objects. Delete all P2P/TCP code, game logic phases, and unused game logic from the client.

**UI constraint:** The Swing client's look, feel, and workflows must remain identical. We are changing the plumbing, not the experience.

**Thinness constraint:** The desktop client must contain zero game logic after M4. No betting validation, no hand evaluation, no winner determination, no blind management, no table balancing, no AI decisions. The client is purely: display what the server tells it, send actions when the user clicks buttons. All logic that was previously in the `logic/` package, the game flow phases (`Bet`, `Deal`, `Showdown`, etc.), and the game engine methods in `PokerGame` moves to — or already lives in — the server. This minimizes the untestable Swing surface area.

---

## Key Architectural Decisions

These were decided during plan review and are not open for re-evaluation:

### 1. Thin View Models (Option A)

Create `RemotePokerTable extends PokerTable` and `RemoteHoldemHand extends HoldemHand` that override ~30 getters each. These are pure data containers — no poker logic. Populated from WebSocket messages. The UI reads them identically to the originals.

**Why:** Both classes are non-final with public constructors and non-final getters. The UI calls only getters (never mutating methods). This is the thinnest possible adaptation — no interface extraction, no adapter layer, no state synchronization.

### 2. No Parallel Paths — Delete Old Code in M4

The old P2P/TCP code (`OnlineManager`, `TournamentDirector`, `PokerConnectionServer`, `Bet` phase for online) is deleted as part of M4, not deferred to a separate M7 milestone. This keeps the codebase clean and avoids maintaining two code paths.

**Order:** Build the new WebSocket path first (Phases 4.1-4.4), verify it works, then delete old code (Phase 4.5).

### 3. Real JWT from Embedded Server

No `LocalAuthProvider` bypass. The embedded server generates real JWTs using the same `JwtService` as the standalone server. The desktop client gets a JWT via an in-JVM call (`EmbeddedGameServer.getLocalUserJwt()`), then uses it for all REST + WebSocket calls. Same auth code path everywhere.

### 4. Direct Action Routing — No PokerGame.playerActionPerformed Modification

`WebSocketTournamentDirector` registers a `PlayerActionListener` on `PokerGame` that sends actions directly to `WebSocketGameClient.sendAction()`. The `Bet` phase is not used in remote play. `ShowTournamentTable` continues to call `game_.playerActionPerformed(action, amount)` unchanged — the listener just goes to WebSocket instead of local engine.

**Action flow:**
```
ShowTournamentTable button click
  → game_.playerActionPerformed(action, amount)   [unchanged]
  → PlayerActionListener (set by WebSocketTournamentDirector)
  → WebSocketGameClient.sendAction(action, amount)
  → Server processes → broadcasts PLAYER_ACTED
  → WebSocketTournamentDirector receives → updates RemotePokerTable/RemoteHoldemHand → fires events
  → ShowTournamentTable re-renders                 [unchanged]
```

### 5. Full Multi-Table Support

No descoping to single table. `WebSocketTournamentDirector` manages multiple `RemotePokerTable` instances, one per table in the `GAME_STATE` message. Table switching uses existing `PokerGame.setCurrentTable()` which fires `PROP_CURRENT_TABLE` events.

### 6. Phase System Integration

`WebSocketTournamentDirector extends BasePhase implements Runnable, GameManager, ChatManager` replaces `TournamentDirector` in `gamedef.xml`. The phase chain is preserved:

```
TournamentOptions → InitializeTournamentGame → BeginTournamentGame (ShowTournamentTable)
  → WebSocketTournamentDirector (was TournamentDirector)
```

Update `gamedef.xml` line 421 to point to the new class. The phase gets `PokerGame` via `context_.getGame()`. Instead of running the poker engine locally, it connects to the WebSocket server and translates incoming messages into view model updates.

### 7. Server-Side TournamentProfile Conversion

The `TournamentProfileConverter` lives on the **server**, not the client. The server already depends on `pokerengine` (which has `TournamentProfile`). A new `POST /api/v1/games/practice` endpoint accepts `TournamentProfile` JSON directly, converts it to `GameConfig` internally, creates the game, adds AI players, auto-joins the caller, and returns a gameId. The client's `PracticeGameLauncher` becomes a single REST call (~30 lines).

**Why:** Keeps conversion logic server-side where it's easily testable. Reduces client code. The server owns its own data model conversion.

---

## Architecture: How It All Fits

```
PokerMain.init()
  |
  +--> GameEngine.init() (existing Swing framework startup)
  |
  +--> EmbeddedGameServer.start() (NEW - Spring Boot on random port)
  |       |
  |       +--> GameInstanceManager, ServerTournamentDirector
  |       +--> REST API at localhost:{port}/api/v1/...
  |       +--> WebSocket at ws://localhost:{port}/ws/games/{gameId}
  |       +--> JWT auth (same as standalone server)
  |
  +--> Swing UI initializes (existing flow)
  |
  +--> User creates game:
  |       +--> PracticeGameLauncher sends one REST call:
  |       |      POST /api/v1/games/practice (TournamentProfile JSON + JWT)
  |       |      Server converts → GameConfig, creates game, adds AIs, joins caller
  |       |      Returns gameId
  |       +--> Phase chain starts → WebSocketTournamentDirector.start()
  |       +--> WsTD connects WebSocket: ws://localhost:{port}/ws/games/{id}
  |       +--> Server sends CONNECTED with full GAME_STATE
  |       +--> WsTD creates RemotePokerTable/RemoteHoldemHand view models
  |       +--> ShowTournamentTable reads from view models (unchanged)
  |
  +--> Game play:
          +--> Server sends ACTION_REQUIRED via WebSocket
          +--> WsTD updates RemoteHoldemHand (current player = human)
          +--> WsTD fires PokerTableEvent → ShowTournamentTable shows buttons
          +--> User clicks Fold/Call/Raise
          +--> PlayerActionListener → WebSocketGameClient.sendAction()
          +--> Server processes → broadcasts PLAYER_ACTED
          +--> WsTD updates view models → fires events → UI re-renders
```

---

## Phase 4.1: Embed Spring Boot in Desktop Client

### Goal
Start the `pokergameserver` Spring Boot context inside the desktop client JVM, on a random port, with real JWT auth.

### New Files

**`code/poker/src/main/java/com/donohoedigital/games/poker/server/EmbeddedGameServer.java`** (~200 lines)

Manages the lifecycle of an embedded Spring Boot application context:
- Start Spring Boot programmatically on a background thread
- Find a random available port before startup
- Set embedded-mode properties (max 3 games, no action timeout, file-based H2 at `~/.ddpoker/games`)
- `getPort()`, `isRunning()`, `stop()`
- `getLocalUserJwt()` — generates a real JWT for the local user via in-JVM call to `JwtService`. Creates/looks up the local profile on first call.

**`code/poker/src/main/java/com/donohoedigital/games/poker/server/EmbeddedServerConfig.java`** (~80 lines)

Spring Boot configuration class for embedded mode:
- `@SpringBootApplication` scanning `pokergameserver` packages
- Standard `SecurityFilterChain` with JWT auth (same as standalone)
- `@Profile("embedded")` activation

**`code/poker/src/main/resources/application-embedded.properties`** (~15 lines)

```properties
server.port=0
game.server.max-concurrent-games=3
game.server.action-timeout-seconds=0
spring.datasource.url=jdbc:h2:file:${user.home}/.ddpoker/games
```

### Files to Modify

**`code/poker/pom.xml`**
- Add dependency on `pokergameserver`
- Add `spring-boot-starter-web`, `spring-boot-starter-websocket`, `spring-boot-starter-data-jpa`, `h2`
- Verify no Jackson or Log4j2 version conflicts

**`code/poker/src/main/java/com/donohoedigital/games/poker/PokerMain.java`**
- Add `EmbeddedGameServer` field
- After `super.init()`, start embedded server (during splash screen)
- On exit, call `embeddedServer.stop()`
- `getEmbeddedServer()` accessor

### Key Risk: Spring Boot + GameEngine Coexistence

- **Thread isolation:** Spring Boot runs on its own thread pool. Swing EDT remains Swing EDT.
- **Classloader:** Both share the same classloader. Spring Boot is designed for this.
- **Logging:** Both use Log4j2. `pokergameserver` pom already excludes `spring-boot-starter-logging`.
- **Startup time:** Spring Boot starts in ~1-2 seconds with lazy init. Acceptable during splash screen.
- **Memory:** May need to increase from `-Xmx512m` to `-Xmx768m`.

### Tests (TDD)

| Test | Description |
|------|-------------|
| `EmbeddedGameServerTest` | Server starts on random port, HTTP health check responds, stops cleanly, port released |
| `EmbeddedGameServerAuthTest` | `getLocalUserJwt()` returns valid JWT; REST endpoint reachable with that JWT |

---

## Phase 4.2: WebSocket Client + Thin View Models

### Goal
Create a Java WebSocket client and thin view model classes that the Swing UI reads from transparently.

### New Files

**`code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketGameClient.java`** (~350 lines)

JDK `java.net.http.HttpClient`-based WebSocket client:
- `connect(gameId, jwt)` — returns `CompletableFuture<Void>`
- `sendAction(action, amount)` — sends `PLAYER_ACTION` message
- `sendChat(message, tableChat)`, `sendSitOut()`, `sendAdminPause()`, `sendAdminResume()`
- `setMessageHandler(Consumer<ServerMessage>)` — called for each inbound message
- Auto-reconnect with exponential backoff on unexpected disconnect
- Inner `GameWebSocketListener implements WebSocket.Listener` that accumulates text frames and deserializes JSON

**`code/poker/src/main/java/com/donohoedigital/games/poker/online/RemotePokerTable.java`** (~250 lines)

Thin view model. Extends `PokerTable`, overrides ~15-20 getters to return simple stored fields:
- `getHoldemHand()` → returns `RemoteHoldemHand`
- `getNumOccupiedSeats()` → stored count
- `getPlayer(int seat)` → stored player array
- `getButton()` → stored button index
- `getName()`, `getNumber()`, `getSeats()`
- Update methods: `updateFromState(SeatData[], int button, ...)` — called by `WebSocketTournamentDirector`
- Fires `PokerTableEvent`s when state changes (same events the UI listens to)

**`code/poker/src/main/java/com/donohoedigital/games/poker/online/RemoteHoldemHand.java`** (~200 lines)

Thin view model. Extends `HoldemHand`, overrides ~10-15 getters:
- `getRound()` → stored round
- `getCommunity()` → stored community cards
- `getCurrentPlayer()` → stored current player
- `getCurrentPlayerIndex()` → stored index
- `getNumPlayers()` → stored count
- `getPlayerAt(int)` → stored player order
- `getTotalPotChipCount()` → stored pot total
- Update methods: `updateRound(BettingRound)`, `updateCommunity(Hand)`, `updateCurrentPlayer(int)`, etc.

### Tests (TDD)

| Test | Description |
|------|-------------|
| `WebSocketGameClientTest` | Connect, send action, receive messages, disconnect, reconnect on failure |
| `RemotePokerTableTest` | All overridden getters return correct stored values; events fire on update |
| `RemoteHoldemHandTest` | All overridden getters return correct stored values; round/community update |

---

## Phase 4.3: WebSocketTournamentDirector (Phase Replacement)

### Goal
Create the phase that replaces `TournamentDirector` — it connects to the WebSocket server, receives messages, and populates view models for the Swing UI.

### New Files

**`code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java`** (~600 lines)

The critical class. Extends `BasePhase implements Runnable, GameManager, ChatManager`.

**Lifecycle:**
- `start()`: Get `PokerGame` via `context_.getGame()`. Connect `WebSocketGameClient` to embedded server. Register as `PlayerActionListener`. Enter message receive loop.
- `run()`: Main loop — receive WebSocket messages, dispatch to handlers on Swing EDT.
- `finish()`: Disconnect WebSocket, clean up.

**Message handlers (all 27 server message types):**

| Message | Handler Action |
|---------|---------------|
| `CONNECTED` / `GAME_STATE` | Create `RemotePokerTable`/`RemoteHoldemHand` for each table. Add to `PokerGame`. Populate all player seats, chip counts, community cards, pots. |
| `HAND_STARTED` | Create new `RemoteHoldemHand` on the table. Set dealer, blinds. Fire `TYPE_NEW_HAND` event. |
| `HOLE_CARDS_DEALT` | Set local player's hole cards on `RemoteHoldemHand`. Fire card event. |
| `COMMUNITY_CARDS_DEALT` | Update community cards on `RemoteHoldemHand`. Fire community card event. |
| `ACTION_REQUIRED` | Update `RemoteHoldemHand` current player to local player. Store valid options. Fire event so UI shows buttons. |
| `PLAYER_ACTED` | Update player chip count, pot, bet. Fire `TYPE_PLAYER_ACTION` event. |
| `ACTION_TIMEOUT` | Apply auto-action to view model. Fire event. |
| `HAND_COMPLETE` | Update winners, show cards if showdown. Fire `TYPE_END_HAND` event. |
| `POT_AWARDED` | Update pot display. Fire event. |
| `SHOWDOWN_STARTED` | Fire showdown event. |
| `LEVEL_CHANGED` | Update blind level on `PokerGame`. Fire `PROP_CURRENT_LEVEL`. |
| `PLAYER_ELIMINATED` | Mark player eliminated. Fire event. |
| `GAME_COMPLETE` | Trigger results screen via phase chain. |
| `PLAYER_JOINED` / `PLAYER_LEFT` / `PLAYER_DISCONNECTED` | Update player list on view model. Fire events. |
| `PLAYER_REBUY` / `PLAYER_ADDON` | Update player chip count. Fire event. |
| `GAME_PAUSED` / `GAME_RESUMED` | Update game state. Fire events for pause overlay. |
| `PLAYER_KICKED` | Remove player from view model. Fire event. |
| `CHAT_MESSAGE` | Route to chat panel via `ChatManager`. |
| `ERROR` | Display error via existing dialog patterns. |
| `TIMER_UPDATE` | Update countdown display. |
| `REBUY_OFFERED` / `ADDON_OFFERED` | Show rebuy/addon dialog (existing UI). Send decision via WebSocket. |

**PlayerActionListener implementation:**
```java
// Set on PokerGame in start()
game.setPlayerActionListener((action, amount) -> {
    String wsAction = mapActionToString(action); // ACTION_FOLD → "FOLD", etc.
    webSocketClient.sendAction(wsAction, amount);
});
```

**GameManager implementation:**
- `getSaveLockObject()` → no-op object (saves handled by server)
- `getPhaseName()` → `"WebSocketTournamentDirector"`
- `cleanup()` → disconnect WebSocket

**ChatManager implementation:**
- Route chat messages to `WebSocketGameClient.sendChat()`

**Multi-table support:**
- `Map<Integer, RemotePokerTable>` — one per table from `GAME_STATE`
- Table switch events update `PokerGame.setCurrentTable()` → fires `PROP_CURRENT_TABLE`
- New tables from `PLAYER_JOINED`/state updates are added dynamically

### Files to Modify

**`code/poker/src/main/resources/config/poker/gamedef.xml`** (line 421)
- Change `TournamentDirector` phase class to `WebSocketTournamentDirector`
- Keep `ClientTournamentDirector` variant (line 424) pointed at WebSocket version too, or remove if unused

### Tests (TDD — most critical test class)

| Test | Description |
|------|-------------|
| `WebSocketTournamentDirectorTest` | Each of 27 message types correctly updates view models |
| `WsTD_HandFlowTest` | Full hand: HAND_STARTED → HOLE_CARDS → actions → COMMUNITY_CARDS → HAND_COMPLETE |
| `WsTD_ActionRequiredTest` | ACTION_REQUIRED sets correct state; button click → WebSocket sendAction |
| `WsTD_MultiTableTest` | GAME_STATE with 3 tables → 3 RemotePokerTables; table switch works |
| `WsTD_ReconnectTest` | Disconnect + reconnect → GAME_STATE rebuilds view models correctly |

---

## Phase 4.4: Practice Mode Integration

### Goal
Wire the complete flow: user creates a practice game → embedded server runs it → WebSocket drives UI.

### New Server Files

**`code/pokergameserver/.../api/PracticeGameController.java`** (~80 lines)

New REST endpoint on the server:
- `POST /api/v1/games/practice` — accepts `TournamentProfile` JSON body + JWT auth
- Converts `TournamentProfile` → `GameConfig` internally via `TournamentProfileConverter`
- Creates game, adds AI players, auto-joins the caller, starts the game
- Returns `{ "gameId": "..." }`

**`code/pokergameserver/.../api/TournamentProfileConverter.java`** (~150 lines)

Lives on the **server** (not client). Converts `TournamentProfile` → `GameConfig`:
- Map blind structure (all levels, antes, breaks)
- Map player count, starting chips, rebuy/addon rules
- Map game name, AI player configuration

### New Client Files

**`code/poker/src/main/java/com/donohoedigital/games/poker/server/GameServerRestClient.java`** (~100 lines)

JDK `java.net.http.HttpClient` wrapper for the game server REST API:
- `createPracticeGame(TournamentProfile, jwt)` → gameId (single call to `POST /api/v1/games/practice`)
- `listGames(status, jwt)` → `List<GameSummary>`

**`code/poker/src/main/java/com/donohoedigital/games/poker/server/PracticeGameLauncher.java`** (~30 lines)

Thin orchestrator — one REST call:
1. Get JWT via `EmbeddedGameServer.getLocalUserJwt()`
2. Call `restClient.createPracticeGame(profile, jwt)` → gameId
3. Store gameId and JWT for `WebSocketTournamentDirector` to use when the phase starts

### Files to Modify

**`code/poker/src/main/java/com/donohoedigital/games/poker/PokerGame.java`**
- Add `setWebSocketConfig(gameId, jwt, port)` — stores connection info for `WebSocketTournamentDirector`
- No other changes. `playerActionPerformed()` is unchanged.

**Game creation flow integration point** — The existing practice game flow goes through phases defined in `gamedef.xml`. The `InitializeTournamentGame` phase (or similar) creates the `PokerGame`. We insert `PracticeGameLauncher` here to create the game on the embedded server before the phase chain reaches `WebSocketTournamentDirector`.

### Tests (TDD)

| Test | Description |
|------|-------------|
| `TournamentProfileConverterTest` | **(Server test)** All TournamentProfile fields mapped correctly; blind structure round-trips |
| `PracticeGameControllerTest` | **(Server test)** POST /api/v1/games/practice creates game, adds AIs, returns gameId |
| `GameServerRestClientTest` | **(Client test)** HTTP call succeeds, errors handled, response deserialized |
| `PracticeGameIntegrationTest` | End-to-end: embedded server + launcher + WsTD + view models, AI-only game plays to completion |

---

## Phase 4.5: Legacy Code Removal & Client Gutting

### Goal
Delete all P2P/TCP code, game logic phases, the `logic/` package, and unused game logic from `PokerGame`. After this phase, the client contains zero poker logic — only UI display and WebSocket communication.

### Files to Delete

**P2P/TCP networking (replaced by WebSocket):**

| File | Lines | Replacement |
|------|-------|-------------|
| `OnlineManager.java` | 2,335 | `WebSocketGameClient` |
| `TournamentDirector.java` | 1,969 | `WebSocketTournamentDirector` |
| `ClientTournamentDirector.java` | ~200 | `WebSocketTournamentDirector` |
| `PokerConnectionServer` (inner class in PokerMain) | ~200 | Embedded Spring Boot server |
| P2P classes in `code/server/.../p2p/` | varies | N/A |

**Game logic phases (replaced by WsTD message handlers):**

| File | Lines | Why Delete |
|------|-------|------------|
| `Bet.java` | ~300 | Server validates betting; WsTD registers PlayerActionListener directly |
| `Deal.java` / `DealDisplay.java` / `DealCommunity.java` | varies | Server deals; WsTD handles HAND_STARTED/COMMUNITY_CARDS_DEALT |
| `Showdown.java` / `PreShowdown.java` | varies | Server handles showdown; WsTD handles HAND_COMPLETE |
| `CheckEndHand.java` | varies | Server determines hand end |
| `ColorUp.java` / `ColorUpFinish.java` | varies | Server handles color-up |
| `NewLevelActions.java` | varies | Server handles level changes |
| Other game flow phases | varies | All game orchestration is server-side |

**Note:** Some of these phases may have UI display helper methods used by `ShowTournamentTable` (e.g., `DealDisplay.syncCards()`, `Showdown.displayShowdown()`). If so, keep the **static display helper methods** but delete the phase logic. Determine during implementation by tracing references.

**`logic/` package (all server-side now):**

| File | Lines | Why Delete |
|------|-------|------------|
| `BetValidator.java` | varies | Server sends ACTION_REQUIRED with exact valid options |
| `HandOrchestrator.java` | varies | Server orchestrates hand phases |
| `ShowdownCalculator.java` | varies | Server evaluates winners |
| `DealingRules.java` | varies | Server deals cards |
| `GameOverChecker.java` | varies | Server determines game end |
| `ColorUpLogic.java` | varies | Server handles color-up |
| `LevelTransitionLogic.java` | varies | Server manages blind levels |
| `TableManager.java` | varies | Server balances tables |
| `OnlineCoordinator.java` | varies | Replaced by WebSocket protocol |

### Files to Gut

**`code/poker/src/main/java/com/donohoedigital/games/poker/PokerGame.java`** (~2,296 → ~500 lines)

Remove all game logic methods that are now server-side. **Keep only:**
- Table management: `getCurrentTable()`, `setCurrentTable()`, `getTables()`, `addTable()`, `removeTable()`, `getNumTables()`
- Property change infrastructure: `PROP_CURRENT_TABLE`, `PROP_CURRENT_LEVEL`, `PROP_PROFILE`, etc.
- Player action delegation: `playerActionPerformed()`, `setPlayerActionListener()`
- Profile/settings getters: `getProfile()`, `getLevel()`, `getHumanPlayer()`, `getSeats()`, `getAnte()`, `getSmallBlind()`, `getBigBlind()`
- WebSocket config: `setWebSocketConfig()`, `getWebSocketConfig()` (new)
- Input mode: `setInputMode()`, `getInputMode()`

**Delete from PokerGame:**
- `initTournament()`, `setupTournament()` — server creates tournaments
- `setupComputerPlayers()`, `fillSeats()` — server fills seats
- `assignTables()`, `processTableBalance()` — server balances tables
- `playerOut()` — server eliminates players
- `changeLevel()`, `nextLevel()`, `prevLevel()` — server manages levels
- `computeTotalChipsInPlay()` and related chip logic — server tracks chips
- All `TournamentContext` implementation methods that involve logic

### Files to Modify

**`code/poker/src/main/java/com/donohoedigital/games/poker/PokerMain.java`**
- Remove `PokerConnectionServer` inner class and TCP listener startup
- Remove P2P-related imports and fields
- The module becomes: Swing UI + WebSocket client + embedded server bootstrap

**`code/poker/src/main/java/com/donohoedigital/games/poker/ShowTournamentTable.java`**
- Remove direct `TournamentDirector` type cast in `TD()` — use `GameManager` interface
- Remove any `OnlineManager` references

**Other files** — Trace all references to deleted classes and update or remove. Determined during implementation by following compile errors after each deletion.

### Verification

- All remaining tests pass (tests for deleted classes are also deleted)
- `mvn test -P dev` across all modules
- Manual: launch PokerMain, create practice game, play to completion
- Verify no game logic remains in the `poker` module (grep for engine method calls)

---

## Phase 4.6: Save/Load via Event Sourcing

### Goal
Practice games auto-save via the embedded server's event store (H2 file at `~/.ddpoker/games`). In-progress games can be resumed on desktop restart.

### New Files

**`code/poker/src/main/java/com/donohoedigital/games/poker/server/GameResumeManager.java`** (~100 lines)

On desktop startup, queries embedded server for in-progress games:
- `getResumableGames(restClient, jwt)` → `List<GameSummary>` of IN_PROGRESS games
- `resumeGame(gameId)` → connect WebSocket; server sends `CONNECTED` with full state snapshot; `WebSocketTournamentDirector` rebuilds view models

### Files to Modify

**`code/poker/src/main/java/com/donohoedigital/games/poker/PokerMain.java`**
- After embedded server starts, call `GameResumeManager.getResumableGames()`
- If games found, show "Resume game?" prompt using existing dialog patterns

### Tests (TDD)

| Test | Description |
|------|-------------|
| `GameResumeManagerTest` | Queries games, skips COMPLETED, reconnects to IN_PROGRESS |
| `SaveLoadIntegrationTest` | Start game, play hands, shutdown, restart, resume, verify state |

---

## Implementation Order

| Order | Step | Depends On | Est. Effort |
|-------|------|------------|-------------|
| 1 | `EmbeddedServerConfig` + `EmbeddedGameServer` | — | M |
| 2 | `poker/pom.xml` + `PokerMain` embedded server startup | Step 1 | M |
| 3 | `WebSocketGameClient` | Step 2 | L |
| 4 | `RemotePokerTable` + `RemoteHoldemHand` | — (parallel with 3) | M |
| 5 | `TournamentProfileConverter` + `PracticeGameController` **(server-side)** | — (parallel with 3-4) | M |
| 6 | `GameServerRestClient` + `PracticeGameLauncher` **(client, ~130 lines total)** | Steps 2, 5 | S |
| 7 | `WebSocketTournamentDirector` — all 27 message handlers | Steps 3, 4 | **XL** |
| 8 | `gamedef.xml` update + integration testing | Steps 6, 7 | L |
| 9 | Legacy code removal + client gutting (Phase 4.5) | Step 8 verified working | L |
| 10 | `GameResumeManager` + save/load | Steps 2, 3, 7 | M |
| 11 | Full integration test: practice game end-to-end | All | L |

**Critical path:** 1 → 2 → 3 → 7 → 8 → 9 → Integration

`WebSocketTournamentDirector` (Step 7) is the hardest class. It must correctly map all 27 WebSocket message types to the right view model updates and fire the right `PokerTableEvent`s for the Swing UI. Study `SwingEventBus.java` and `ShowTournamentTable`'s event listeners carefully before implementing.

---

## Files Summary

### New Client Production Files (8)

| File | Est. Lines | Role |
|------|-----------|------|
| `poker/.../server/EmbeddedGameServer.java` | ~200 | Spring Boot lifecycle |
| `poker/.../server/EmbeddedServerConfig.java` | ~80 | Spring config |
| `poker/.../server/GameServerRestClient.java` | ~100 | HTTP wrapper (one method for practice, one for listing) |
| `poker/.../server/PracticeGameLauncher.java` | ~30 | One REST call to create practice game |
| `poker/.../server/GameResumeManager.java` | ~100 | Resume prompt on startup |
| `poker/.../online/WebSocketGameClient.java` | ~350 | WebSocket connection management |
| `poker/.../online/WebSocketTournamentDirector.java` | ~600 | Message → view model → event mapping (no logic) |
| `poker/.../online/RemotePokerTable.java` | ~250 | Thin view model — overridden getters |
| `poker/.../online/RemoteHoldemHand.java` | ~200 | Thin view model — overridden getters |

**Total new client code: ~1,910 lines** (zero game logic)

### New Server Production Files (2)

| File | Est. Lines | Role |
|------|-----------|------|
| `pokergameserver/.../api/PracticeGameController.java` | ~80 | `POST /api/v1/games/practice` endpoint |
| `pokergameserver/.../api/TournamentProfileConverter.java` | ~150 | TournamentProfile → GameConfig conversion |

### Modified Production Files (4)

| File | Change |
|------|--------|
| `code/poker/pom.xml` | Add `pokergameserver` + Spring Boot deps |
| `code/poker/.../PokerMain.java` | Start/stop `EmbeddedGameServer`; remove P2P code |
| `code/poker/.../PokerGame.java` | **Gutted**: remove ~1,800 lines of game logic; add `setWebSocketConfig()` |
| `code/poker/.../.../gamedef.xml` | Point TournamentDirector phase to `WebSocketTournamentDirector` |

### Deleted Production Files (~20+)

| Category | Files | Est. Lines Deleted |
|----------|-------|-------------------|
| P2P/TCP networking | `OnlineManager`, `TournamentDirector`, `ClientTournamentDirector`, `PokerConnectionServer`, P2P classes | ~5,000+ |
| Game flow phases | `Bet`, `Deal`, `DealCommunity`, `Showdown`, `PreShowdown`, `CheckEndHand`, `ColorUp`, `ColorUpFinish`, `NewLevelActions`, others | ~2,000+ |
| `logic/` package | `BetValidator`, `HandOrchestrator`, `ShowdownCalculator`, `DealingRules`, `GameOverChecker`, `ColorUpLogic`, `LevelTransitionLogic`, `TableManager`, `OnlineCoordinator` | ~1,000+ |
| PokerGame gutting | Removed methods (in-place) | ~1,800 |

**Total deleted: ~10,000+ lines of client-side game logic**

### New Test Files

**Client tests (~12):** `EmbeddedGameServerTest`, `EmbeddedGameServerAuthTest`, `WebSocketGameClientTest`, `RemotePokerTableTest`, `RemoteHoldemHandTest`, `GameServerRestClientTest`, `WebSocketTournamentDirectorTest`, `WsTD_HandFlowTest`, `WsTD_ActionRequiredTest`, `WsTD_MultiTableTest`, `WsTD_ReconnectTest`, `PracticeGameIntegrationTest`, `GameResumeManagerTest`, `SaveLoadIntegrationTest`

**Server tests (~2):** `TournamentProfileConverterTest`, `PracticeGameControllerTest`

### Net Impact

| Metric | Before M4 | After M4 |
|--------|-----------|----------|
| Client game logic | ~10,000+ lines | **0 lines** |
| Client new code | 0 | ~1,910 lines (all display/communication) |
| Client responsibilities | Game engine + UI + P2P networking | **UI display + WebSocket send/receive only** |

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| `WebSocketTournamentDirector` incorrectly maps messages to view model events | **High** | Medium | TDD with exhaustive per-message tests. Compare output to `SwingEventBus` behavior. |
| `RemotePokerTable`/`RemoteHoldemHand` missing getter overrides (UI reads un-overridden method that hits null engine state) | **High** | Medium | Audit all getter calls in `ShowTournamentTable` before implementing. Override every accessed getter. |
| Phase deletion removes UI display helpers that `ShowTournamentTable` still calls (e.g., `DealDisplay.syncCards()`, `Showdown.displayShowdown()`) | **High** | Medium | Trace all static method references from ShowTournamentTable before deleting any phase. Keep display helpers, delete phase logic. |
| `PokerGame` gutting removes a method that some unchanged UI code still calls | **High** | Medium | Compile after each method removal. Follow all compile errors. Keep any getter still referenced by UI. |
| Spring Boot + GameEngine Swing singleton conflicts | Medium | Low | Spring beans must not reference Swing singletons. Test startup in isolation first. |
| Spring Boot startup too slow for desktop feel | Medium | Low | Start during splash screen. Use lazy bean initialization. Target < 2s. |
| Jackson version conflict between Spring Boot and existing desktop deps | Low | Medium | Spring Boot BOM manages Jackson. Pin version if conflict occurs. |
| Memory pressure (Spring Boot + Swing in same JVM) | Low | Low | Increase `-Xmx` from 512m to 768m in jpackage config. |

---

## What is NOT in M4

1. **Community-hosted mode** — deferred to M6 (Game Discovery). M4 is practice mode only.
2. **Server-hosted mode** — connecting to remote servers, deferred to M6.
3. **Web client** — M5 (parallel, independent).
4. **M7 is absorbed** — Legacy P2P removal happens in Phase 4.5 of this plan.

---

## Critical Files to Read Before Implementation

**For Phase 4.2-4.3 (view models + WsTD):**
- `code/poker/src/main/java/.../ShowTournamentTable.java` — main game UI; audit every getter call on `PokerTable`/`HoldemHand` to ensure view model coverage
- `code/poker/src/main/java/.../online/SwingEventBus.java` — reference for how `GameEvent`s map to `PokerTableEvent`s; `WebSocketTournamentDirector` must fire the same events
- `code/pokergameserver/.../websocket/message/ServerMessageData.java` — the protocol contract; all 27 message types WsTD handles
- `code/poker/src/main/java/.../Bet.java` — understand the `PlayerActionListener` pattern that WsTD replaces

**For Phase 4.1 + 4.4 (embedded server + practice mode):**
- `code/poker/src/main/java/.../PokerMain.java` — central integration point
- `code/poker/src/main/resources/config/poker/gamedef.xml` — phase chain definitions

**For Phase 4.5 (gutting):**
- `code/poker/src/main/java/.../PokerGame.java` — identify which methods are still called by UI vs which are pure game logic to delete
- `code/poker/src/main/java/.../logic/` — entire package; verify nothing in it is called by UI before deleting
- All game flow phases (`Bet`, `Deal*`, `Showdown`, etc.) — check for static display helper methods that `ShowTournamentTable.sync()` calls (keep helpers, delete phase logic)

---

## Verification

```bash
# After all phases:
cd code && mvn test -pl poker -P dev           # Poker module tests pass
cd code && mvn test -pl pokergameserver -P dev  # Server tests still pass
cd code && mvn test -P dev                      # Full regression

# Manual: launch PokerMain, create practice game, play to tournament completion
# Verify: no visible difference in game flow vs old code path
```
