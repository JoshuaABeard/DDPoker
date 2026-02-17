# M4: Desktop Client Adaptation

**Status:** DRAFT
**Created:** 2026-02-16
**Parent Plan:** `.claude/plans/SERVER-HOSTED-GAME-ENGINE.md`
**Depends On:** M1 (complete), M2 (complete), M3 (complete)
**Effort:** XL (5-7 weeks estimated)

---

## Context

M1-M3 built the complete `pokergameserver` module: 74 production Java files, 45 test files, and all server-side infrastructure for hosting poker games. The module provides GameInstanceManager, ServerTournamentDirector, JWT auth (RS256), REST API for game CRUD, WebSocket protocol with full message types (22 server-to-client, 9 client-to-server), and card privacy enforcement.

The desktop client (`code/poker/`) currently runs games using a tightly coupled architecture:
- `PokerMain` (916 lines) extends `GameEngine` (Swing singleton) and manages P2P/TCP connections via `PokerConnectionServer`
- `TournamentDirector` (1,969 lines) extends `BasePhase` and drives the local game loop
- `OnlineManager` (2,335 lines) implements P2P message routing between host and clients
- `ShowTournamentTable` (2,609 lines) is the main game UI, reading directly from `PokerGame`, `PokerTable`, and `HoldemHand`
- `PokerGame` (2,296 lines) extends `Game` and implements `TournamentContext`

**What this plan does:** Embed the Spring Boot `pokergameserver` in the desktop client JVM. Replace `OnlineManager` and `TournamentDirector` with a WebSocket client that talks to the embedded server. Create adapter classes so the existing Swing UI reads game state from WebSocket messages instead of local game objects.

**UI constraint:** The Swing client's look, feel, and workflows must remain identical. Existing users know this UI. We are changing the plumbing, not the experience.

---

## Key Design Decision: Incremental Adaptation vs Full Replacement

The master plan describes M4 (WebSocket adaptation) and M7 (P2P/TCP removal) as separate milestones. This plan follows that separation:

**M4 scope:** Add the WebSocket path alongside the existing code. Practice mode uses the embedded server + WebSocket. The old P2P code remains but is not used for new game modes. The Swing UI continues to read from `PokerGame`/`PokerTable`/`HoldemHand` objects — but these objects are now updated by WebSocket messages rather than by the local game engine directly.

**NOT in M4 scope:** Deleting `OnlineManager`, `TournamentDirector`, or the P2P infrastructure. That is M7. M4 adds the new path; M7 removes the old.

**Rationale:** This reduces risk dramatically. If the WebSocket path has bugs, the old code path still exists. It also means each file change is surgical — adding new classes and modifying PokerMain's startup, not gutting 10,000+ lines of existing code simultaneously.

---

## Architecture: How Embedded Server Fits

```
PokerMain.init()
  |
  +--> GameEngine.init() (existing Swing framework startup)
  |
  +--> EmbeddedGameServer.start() (NEW - Spring Boot on random port)
  |       |
  |       +--> GameInstanceManager
  |       +--> REST API at localhost:{port}/api/v1/...
  |       +--> WebSocket at ws://localhost:{port}/ws/games/{gameId}
  |       +--> Local auth (no JWT needed for practice mode)
  |
  +--> Swing UI initializes (existing flow unchanged)
  |
  +--> User creates game:
  |       +--> PracticeGameLauncher sends REST: POST /api/v1/games
  |       +--> WebSocketGameClient connects: ws://localhost:{port}/ws/games/{id}
  |       +--> GameStateAdapter receives CONNECTED message with full state
  |       +--> Adapter populates PokerGame/PokerTable/HoldemHand fields
  |       +--> Swing UI reads from these objects as always
  |
  +--> Game play:
          +--> Server sends ACTION_REQUIRED to WebSocket client
          +--> GameStateAdapter sets input mode on PokerGame (Swing EDT)
          +--> ShowTournamentTable shows betting buttons (existing UI)
          +--> User clicks Fold/Call/Raise
          +--> WebSocketGameClient sends PLAYER_ACTION to server
          +--> Server processes, broadcasts PLAYER_ACTED
          +--> GameStateAdapter updates PokerTable/HoldemHand, fires events
          +--> Swing UI re-renders (existing rendering code unchanged)
```

---

## Phase 4.1: Embed Spring Boot in Desktop Client

### Goal
Start the `pokergameserver` Spring Boot context inside the desktop client JVM, on a random port, with simplified auth for local play.

### New Files

**`code/poker/src/main/java/com/donohoedigital/games/poker/server/EmbeddedGameServer.java`** (~200 lines)

Manages the lifecycle of an embedded Spring Boot application context:
- Start Spring Boot programmatically on a background thread
- Find a random available port before startup
- Set embedded-mode properties (max 3 games, no action timeout, local auth mode, file-based H2 at `~/.ddpoker/games`)
- Expose `getPort()`, `isRunning()`, `stop()`, and `getGameInstanceManager()` (for direct in-JVM access)

**`code/poker/src/main/java/com/donohoedigital/games/poker/server/EmbeddedServerConfig.java`** (~80 lines)

Spring Boot configuration class for embedded mode:
- `@SpringBootApplication` scanning only `pokergameserver` packages
- `@Bean SecurityFilterChain` that permits all requests when `game.server.auth.mode=local` (no JWT needed for practice)

**`code/poker/src/main/resources/application-embedded.properties`** (~15 lines)

Spring properties activated in embedded mode:
- `server.port=0` (random port, overridden by EmbeddedGameServer at runtime)
- `game.server.max-concurrent-games=3`
- `game.server.action-timeout-seconds=0` (no timeout for practice)
- `game.server.auth.mode=local`
- `spring.datasource.url=jdbc:h2:file:${user.home}/.ddpoker/games`

### Files to Modify

**`code/poker/pom.xml`**
- Add dependency on `pokergameserver`
- Add `spring-boot-starter-web`, `spring-boot-starter-websocket`, `spring-boot-starter-data-jpa`, `h2`
- Verify no Jackson or Log4j2 version conflicts with existing deps

**`code/poker/src/main/java/com/donohoedigital/games/poker/PokerMain.java`**
- Add `EmbeddedGameServer` field
- After `super.init()`, start embedded server (during splash screen display)
- On application exit, call `embeddedServer.stop()`
- Add `getEmbeddedServer()` accessor

### Key Risk: Spring Boot + GameEngine Coexistence

`PokerMain` extends `GameEngine extends BaseApp`. `GameEngine` is a Swing singleton with static state. Spring Boot starts its own `ApplicationContext`. These must coexist:

- **Thread isolation:** Spring Boot runs on its own thread pool. Swing EDT remains Swing EDT. Spring beans must NOT call `GameEngine.getGameEngine()` or any static Swing singletons.
- **Classloader:** Both share the same classloader. Spring Boot is designed for this.
- **Logging:** Both use Log4j2. The `pokergameserver` pom already excludes `spring-boot-starter-logging`. Embedded server inherits the desktop client's Log4j2 config.
- **Startup time:** Spring Boot 3.5 starts in ~1-2 seconds with lazy init. Acceptable during splash screen.
- **Memory:** Spring Boot adds ~50-100MB heap. Desktop uses `-Xmx512m`. May need to increase to `-Xmx768m` in jpackage config.

### Tests (TDD — write before implementation)

| Test | Description |
|------|-------------|
| `EmbeddedGameServerTest` | Server starts on a random port, HTTP health check responds, stops cleanly, port is released after stop |
| `EmbeddedServerConfigTest` | Local auth mode: REST endpoint reachable without JWT, returns 200 not 401 |

---

## Phase 4.2: WebSocket Client for Desktop

### Goal
Create a Java WebSocket client (using JDK `java.net.http`) and a `GameStateAdapter` that bridges server messages to the Swing UI.

### New Files

**`code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketGameClient.java`** (~350 lines)

JDK `java.net.http.HttpClient`-based WebSocket client:
- `connect(gameId, token)` — returns `CompletableFuture<Void>`
- `sendAction(action, amount)` with monotonic sequence numbers
- `sendChat(message, tableChat)`, `sendSitOut()`, `sendAdminPause()`, `sendAdminResume()`
- `setMessageHandler(Consumer<ServerMessage>)` — called for each inbound message
- Auto-reconnect with exponential backoff on unexpected disconnect
- Inner `GameWebSocketListener implements WebSocket.Listener` that accumulates text frames and deserializes JSON

**`code/poker/src/main/java/com/donohoedigital/games/poker/online/GameStateAdapter.java`** (~500 lines)

The critical bridge class. Receives `ServerMessage` objects and updates existing `PokerGame`/`PokerTable`/`HoldemHand` state so the Swing UI sees changes without modification:

- `onMessage(ServerMessage)` — dispatches to Swing EDT via `SwingUtilities.invokeLater`
- Handlers for all 22 server message types:
  - `CONNECTED` / `GAME_STATE` — full state snapshot; populate PokerGame, PokerTable, all players
  - `HAND_STARTED` — reset hand state, set dealer/blinds, fire `TABLE_UPDATE` event
  - `HOLE_CARDS_DEALT` — set local player's hole cards on HoldemHand
  - `COMMUNITY_CARDS_DEALT` — update community cards on HoldemHand/PokerTable
  - `ACTION_REQUIRED` — call `game.setInputMode(...)` with correct options
  - `PLAYER_ACTED` — update pot, chip counts, fire events
  - `ACTION_TIMEOUT` — apply auto-action, update state
  - `HAND_COMPLETE` — show results, update chip counts
  - `LEVEL_CHANGED` — update blind level display
  - `PLAYER_ELIMINATED` — mark player eliminated, update standings
  - `GAME_COMPLETE` — trigger results screen
  - `POT_AWARDED`, `SHOWDOWN_STARTED` — visual feedback
  - `PLAYER_JOINED`, `PLAYER_LEFT`, `PLAYER_DISCONNECTED` — lobby updates
  - `GAME_PAUSED`, `GAME_RESUMED` — pause overlay
  - `PLAYER_KICKED` — remove player
  - `CHAT_MESSAGE` — route to chat panel
  - `ERROR` — display error dialog

**Why update existing objects, not create RemotePokerGame/RemotePokerTable?**

The master plan mentions `RemotePokerGame` and `RemotePokerTable`. After analysis, the better approach is to **update existing `PokerGame`/`PokerTable`/`HoldemHand` objects directly from WebSocket messages**:

1. `ShowTournamentTable` (2,609 lines) references these as concrete classes throughout — not interfaces. Creating adapters would require extracting interfaces from ~10,000 lines of existing code or extending them, both massive refactors.
2. `PokerGame` already has public setters and fires `PropertyChangeEvent`s the UI listens to.
3. The Swing UI is already event-driven. `GameStateAdapter` just needs to (1) update the state and (2) fire the right events. The UI code does not change.

**How ACTION_REQUIRED flows:**
1. Server sends `ACTION_REQUIRED` with `ActionOptionsData`
2. `GameStateAdapter.handleActionRequired()` calls `game.setInputMode(mode, hand, player)` on EDT
3. `ShowTournamentTable` sees input mode change, shows Fold/Check/Call/Raise buttons (existing code)
4. User clicks → existing `PlayerActionListener` fires → calls `webSocketClient.sendAction(action, amount)`
5. Server validates → broadcasts `PLAYER_ACTED` → adapter updates state → UI refreshes

### Files to Modify

**`code/poker/src/main/java/com/donohoedigital/games/poker/PokerGame.java`**
- Add `setGameStateAdapter(GameStateAdapter)` method
- In `playerActionPerformed()`, if adapter is active, route action to `webSocketClient.sendAction()` instead of local processing

**`code/poker/src/main/java/com/donohoedigital/games/poker/ShowTournamentTable.java`**
- Minimal change: if WebSocket mode active, `TD()` method returns a no-op `GameManager` stub rather than fetching from `context_.getGameManager()`
- All event listening and rendering code unchanged

### Tests (TDD)

| Test | Description |
|------|-------------|
| `WebSocketGameClientTest` | Connect, send action, receive messages, disconnect, reconnect on failure |
| `GameStateAdapterTest` | **Most critical.** Each of 22 message types correctly updates PokerGame state |
| `GameStateAdapter_HandFlowTest` | Full hand: HAND_STARTED → HOLE_CARDS → actions → COMMUNITY_CARDS → HAND_COMPLETE |
| `GameStateAdapter_ActionRequiredTest` | ACTION_REQUIRED sets correct input mode; correct options for fold/check/call/raise/all-in |

---

## Phase 4.3: Practice Mode Integration

### Goal
Wire up the complete flow so a user can start a practice game using the embedded server with AI opponents, connected via WebSocket. No visible change to the game creation UI.

### New Files

**`code/poker/src/main/java/com/donohoedigital/games/poker/server/TournamentProfileConverter.java`** (~150 lines)

Converts `TournamentProfile` (desktop game creation) to `GameConfig` (server API):
- Map blind structure (all levels, antes, breaks)
- Map player count, starting chips, rebuy/addon rules
- Map game name, description

**`code/poker/src/main/java/com/donohoedigital/games/poker/server/GameServerRestClient.java`** (~150 lines)

JDK `java.net.http.HttpClient` wrapper for the game server REST API:
- `createGame(GameConfig)` → returns gameId string
- `startGame(gameId)`
- `joinGame(gameId)`
- `listGames(status)` → `List<GameSummary>`

**`code/poker/src/main/java/com/donohoedigital/games/poker/server/PracticeGameLauncher.java`** (~200 lines)

Orchestrates practice game creation:
1. Convert `TournamentProfile` → `GameConfig` via `TournamentProfileConverter`
2. Create game via REST (`POST /api/v1/games`)
3. Connect WebSocket (`WebSocketGameClient.connect(gameId, "local")`)
4. Wire `GameStateAdapter` as message handler
5. Start game via REST (`POST /api/v1/games/{id}/start`)
6. Game now runs server-side with AI players; WebSocket messages drive UI

### Files to Modify

The existing practice game flow goes through phases defined in `gamedef.xml`. The key branching point is where `TournamentDirector` is launched:

**`code/poker/src/main/java/com/donohoedigital/games/poker/NewGame.java`** (or equivalent phase)
- If `PokerMain.getEmbeddedServer().isRunning()`, use `PracticeGameLauncher` instead of launching `TournamentDirector`
- This is the single branch point; all other game creation UI is unchanged

### Tests (TDD)

| Test | Description |
|------|-------------|
| `TournamentProfileConverterTest` | All TournamentProfile fields mapped correctly; blind structure round-trips |
| `GameServerRestClientTest` | HTTP calls succeed, errors handled, response deserialized correctly |
| `PracticeGameLauncherTest` | Full flow: TournamentProfile → GameConfig → REST create → WS connect → game starts |
| `PracticeGameIntegrationTest` | End-to-end: embedded server + launcher + adapter, AI-only game plays to completion |

---

## Phase 4.4: Save/Load via Event Sourcing

### Goal
Practice games auto-save via the embedded server's event store (H2 file at `~/.ddpoker/games`). In-progress games can be resumed on desktop restart.

### New Files

**`code/poker/src/main/java/com/donohoedigital/games/poker/server/GameResumeManager.java`** (~100 lines)

On desktop startup, queries embedded server for in-progress games:
- `getResumableGames(restClient)` → `List<GameSummary>` of IN_PROGRESS games
- `resumeGame(gameId, client, adapter)` → connect WebSocket; server sends `CONNECTED` with full state snapshot; adapter rebuilds `PokerGame` state

### Files to Modify

**`code/poker/src/main/java/com/donohoedigital/games/poker/PokerMain.java`**
- After embedded server starts, call `GameResumeManager.getResumableGames()`
- If games found, show a "Resume game?" prompt using existing dialog patterns

The existing save/load UI (`GameListPanel`) can surface embedded server games alongside legacy save files in a future cleanup (not M4 scope).

### Tests (TDD)

| Test | Description |
|------|-------------|
| `GameResumeManagerTest` | Queries games, skips COMPLETED games, reconnects to IN_PROGRESS |
| `SaveLoadIntegrationTest` | Start game, play hands, shutdown embedded server, restart, resume, verify state |

---

## Implementation Order (TDD — tests first)

| Order | Step | Depends On | Est. Effort |
|-------|------|------------|-------------|
| 1 | `EmbeddedServerConfig` + `LocalAuthProvider` | — | S |
| 2 | `EmbeddedGameServer` lifecycle | Step 1 | M |
| 3 | `poker/pom.xml` + `PokerMain` integration | Steps 1-2 | M |
| 4 | `WebSocketGameClient` | Step 3 | L |
| 5 | `TournamentProfileConverter` | — (parallel) | S |
| 6 | `GameServerRestClient` | Step 3 | M |
| 7 | `GameStateAdapter` — all 22 message handlers | Steps 4, 6 | **XL** |
| 8 | `PracticeGameLauncher` | Steps 4-7 | L |
| 9 | `PokerGame` + `ShowTournamentTable` routing | Steps 7-8 | L |
| 10 | `GameResumeManager` + save/load | Steps 3, 4, 7 | M |
| 11 | Integration test: full practice game end-to-end | All | L |

**Critical path:** 1 → 2 → 3 → 4 → 7 → 8 → 9 → Integration

`GameStateAdapter` (Step 7) is the hardest class. It must correctly map all 22 WebSocket message types to the right `PokerGame`/`PokerTable`/`HoldemHand` state updates and fire the right `PokerTableEvent`s for the Swing UI. Study `SwingEventBus.java` and `ShowTournamentTable`'s event listeners carefully before implementing.

---

## Files Summary

### New Production Files (10)

| File | Est. Lines |
|------|-----------|
| `poker/.../server/EmbeddedGameServer.java` | ~200 |
| `poker/.../server/EmbeddedServerConfig.java` | ~80 |
| `poker/.../server/LocalAuthProvider.java` | ~60 |
| `poker/.../server/GameServerRestClient.java` | ~150 |
| `poker/.../server/TournamentProfileConverter.java` | ~150 |
| `poker/.../server/PracticeGameLauncher.java` | ~200 |
| `poker/.../server/GameResumeManager.java` | ~100 |
| `poker/.../online/WebSocketGameClient.java` | ~350 |
| `poker/.../online/GameStateAdapter.java` | ~500 |
| `poker/src/main/resources/application-embedded.properties` | ~15 |

### Modified Production Files (4)

| File | Change |
|------|--------|
| `code/poker/pom.xml` | Add `pokergameserver` + Spring Boot deps |
| `code/poker/.../PokerMain.java` | Start/stop `EmbeddedGameServer` in init/shutdown |
| `code/poker/.../PokerGame.java` | Route player actions to WebSocket when adapter active |
| `code/poker/.../NewGame.java` | Branch to `PracticeGameLauncher` when embedded server running |

### New Test Files (~12)

`EmbeddedGameServerTest`, `EmbeddedServerConfigTest`, `WebSocketGameClientTest`, `TournamentProfileConverterTest`, `GameServerRestClientTest`, `GameStateAdapterTest`, `GameStateAdapter_HandFlowTest`, `GameStateAdapter_ActionRequiredTest`, `PracticeGameLauncherTest`, `GameResumeManagerTest`, `SaveLoadIntegrationTest`, `PracticeGameIntegrationTest`

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| `GameStateAdapter` incorrectly maps WebSocket state to Swing events | **High** | Medium | TDD with exhaustive per-message tests. Compare output to `SwingEventBus` behavior. |
| `PokerGame`/`PokerTable` public API insufficient for adapter updates | Medium | Medium | Audit all needed public setters before starting adapter. Add setters if needed (surgical). |
| Spring Boot + GameEngine Swing singleton conflicts | Medium | Low | Spring beans must not reference Swing singletons. Test startup in isolation first. |
| Spring Boot startup too slow for desktop feel | Medium | Low | Start during splash screen. Use lazy bean initialization. Target < 2s. |
| Jackson version conflict between Spring Boot and existing desktop deps | Low | Medium | Spring Boot BOM manages Jackson. Pin version if conflict occurs. |
| Embedded H2 vs existing HSQLDB usage | Low | Low | Different databases for different data. No conflict expected. |
| Memory pressure (Spring Boot + Swing in same JVM) | Low | Low | Increase `-Xmx` from 512m to 768m in jpackage config. Profile during testing. |

---

## Scope Reduction Option

If M4 proves too complex in full, the following minimum viable scope preserves the most value:

**Minimal M4:** Practice mode only, single table, AI opponents, no save/load.

Removes: Phase 4.4 (save/load), multi-table GameStateAdapter support, community-hosted mode.

Keeps: Phases 4.1-4.3 — embedded server startup, WebSocket client, GameStateAdapter for single-table hands, practice game creation.

Still XL effort (~3-4 weeks) but significantly lower risk.

---

## What is NOT in M4

1. **Community-hosted mode** — deferred to M6 (Game Discovery)
2. **Server-hosted mode** — connecting to remote servers, deferred to M6
3. **P2P/TCP code removal** — M7 (Legacy P2P Removal)
4. **Deleting `OnlineManager`, `TournamentDirector`** — M7
5. **Web client** — M5 (parallel, independent)

---

## Critical Files to Read Before Implementation

- `code/poker/src/main/java/com/donohoedigital/games/poker/PokerMain.java` — central integration point
- `code/poker/src/main/java/com/donohoedigital/games/poker/PokerGame.java` — state container the UI reads from; 2,296 lines
- `code/poker/src/main/java/com/donohoedigital/games/poker/online/SwingEventBus.java` — reference pattern for how GameEvents map to PokerTableEvents; GameStateAdapter must fire the same events
- `code/poker/src/main/java/com/donohoedigital/games/poker/ShowTournamentTable.java` — 2,609-line main game UI; understand its event listeners and state reads before implementing adapter
- `code/pokergameserver/src/main/java/.../websocket/message/ServerMessageData.java` — the protocol contract; all 22 message types GameStateAdapter must handle

---

## Verification

```bash
# After all phases:
cd code && mvn test -pl poker -P dev           # Existing poker tests still pass
cd code && mvn test -pl pokergameserver -P dev  # Server tests still pass
cd code && mvn test -P dev                      # Full regression

# Manual: launch PokerMain, create practice game, play to tournament completion
# Verify: no visible difference in game flow vs old code path
```
