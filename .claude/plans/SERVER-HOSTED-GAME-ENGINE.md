# Server-Hosted Game Engine: Comprehensive Implementation Plan

**Status:** DRAFT
**Created:** 2026-02-15
**Plan:** `.claude/plans/SERVER-HOSTED-GAME-ENGINE.md`

---

## Context

DDPoker currently uses a **hub-and-spoke P2P model** where one player (the "host") runs both the game client AND an embedded TCP server. Other players connect directly to the host's IP. This creates fundamental problems:

- Host must have a publicly reachable IP (port forwarding / NAT traversal)
- Host client must stay online for the entire game duration
- If host disconnects, the game dies for everyone
- Game state lives on the host's machine, making it impossible to build web or mobile clients
- No server-authoritative security (clients can potentially cheat)

**Goal:** Host the DDPoker game engine as a server-side process where:
- The server is the single source of truth for all game state
- Clients are thin: they send user input and render game state
- Existing desktop client, future web client, and mobile apps all connect via the same protocol
- **Unified protocol:** ALL game modes (practice, community-hosted, server-hosted) use the same REST + WebSocket protocol
- Desktop client embeds a Spring Boot game server for local/community play
- Server-hosted and community-hosted games are categorized differently in game discovery

---

## Architecture Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Server architecture | **Option C: Native Engine** | Game engine runs natively in the Spring Boot process. All games share one JVM (~5-10MB/game). Cleanest API boundary for multi-client. |
| Client-server protocol | **REST + WebSocket** | REST for game management (CRUD). WebSocket for real-time gameplay. Universal platform support. Spring Boot native support. |
| Authentication | **JWT + current profiles** | Keep existing online profile system. Add JWT token issuance on login. Tokens auth all API/WebSocket calls. |
| Web framework | **Next.js 16** (existing `code/web/`) | Already have React 19 + Tailwind 4 app with auth, routing, components. Extend it. |
| Persistence | **H2 + event sourcing** | Append-only game event log. Replay to rebuild state. Natural for poker's turn-by-turn flow. |
| Scale target | **10-50 concurrent games** | Community edition. Single server instance. H2 is sufficient. |
| Local/community hosting | **Embedded Spring Boot** | Desktop client embeds full Spring Boot game server. ALL game modes (practice, community, server) use WebSocket protocol. One code path. Same server code runs embedded or standalone. |

---

## Architecture Overview

**Key insight:** The same Spring Boot game server runs in two contexts:
1. **Standalone** (remote): Docker container, serves many players, many concurrent games
2. **Embedded** (desktop): Runs inside the desktop client JVM, serves local games

All clients — desktop Swing UI, web browser, future mobile — speak the same REST + WebSocket protocol. No P2P/TCP code path.

```
 ┌─────────────────────────────────────────────────────────────────────────┐
 │                     Game Server (Spring Boot)                          │
 │                                                                        │
 │  ┌──────────────┐  ┌─────────────┐  ┌────────────────────────────────┐ │
 │  │ REST API     │  │ WebSocket   │  │  GameInstanceManager           │ │
 │  │ (Game Mgmt)  │  │ (Gameplay)  │  │  ├── GameInstance (per game)   │ │
 │  └──────────────┘  └─────────────┘  │  │   ├── ServerTournamentDir  │ │
 │                                     │  │   ├── TournamentEngine     │ │
 │  ┌──────────────┐                   │  │   ├── PokerGame/Table      │ │
 │  │ JWT Auth     │                   │  │   ├── ServerAIProvider     │ │
 │  │ Profiles, DB │                   │  │   └── EventStore           │ │
 │  └──────────────┘                   │  └── GameInstance ...          │ │
 │                                     └────────────────────────────────┘ │
 └───────────────┬────────────────────────────────┬───────────────────────┘
                 │                                │
    Deployed as EITHER:                 Deployed as EITHER:
                 │                                │
   ┌─────────────┴──────────┐      ┌──────────────┴──────────────┐
   │  Standalone Server     │      │  Embedded in Desktop Client │
   │  (Docker, remote)      │      │  (localhost, in-JVM)        │
   │  - Multi-user          │      │  - Single user + AI         │
   │  - Full auth + DB      │      │  - Simplified auth          │
   │  - 10-50 games         │      │  - File-based H2             │
   │  - Server-hosted games │      │  - Practice + Community     │
   └────────────────────────┘      └─────────────────────────────┘
                 │                                │
    ┌────────────┼────────────┐         ┌─────────┴─────────┐
    │            │            │         │                   │
 ┌──┴──┐   ┌────┴───┐  ┌────┴───┐  ┌──┴────────────┐  ┌───┴──────────┐
 │ Web │   │ Mobile │  │Desktop │  │ Desktop Swing │  │ Remote       │
 │Next │   │(future)│  │ Client │  │ UI (embedded) │  │ players join │
 │ .js │   │        │  │(remote)│  │ connects to   │  │ via WebSocket│
 └─────┘   └────────┘  └────────┘  │ localhost WS  │  └──────────────┘
                                    └───────────────┘
```

**Three game modes, one protocol:**

| Mode | Server Location | Who Connects | Discovery |
|------|----------------|--------------|-----------|
| **Practice** | Embedded (localhost) | Local Swing UI only | Not listed |
| **Community-hosted** | Embedded (host's IP) | Host's Swing UI + remote players via WebSocket | Registered with WAN server, listed as "Community Hosted" |
| **Server-hosted** | Standalone (remote) | Any client via WebSocket | Listed as "Server Hosted" |

---

## Prerequisites (Must Complete Before This Plan)

### P1: Complete V2 AI Extraction ✅
- **Plan:** `.claude/plans/completed/PHASE7C-V2-EXTRACTION.md`
- **Why:** Server-hosted games need V2Algorithm in pokergamecore (Swing-free)
- **Status:** Complete (2026-02-16)

### P2: Complete Phase 7D - ServerAIProvider Integration ✅
- **Plan:** `.claude/plans/completed/PHASE7D-SERVER-AI-PROVIDER.md`
- **Why:** Server needs a working AI provider that maps skill levels to AI implementations
- **Status:** Complete (2026-02-16)
- **Depends on:** P1 ✅

### P3: TournamentEngine Full Integration ✅
- **What:** TournamentEngine (pokergamecore) must be fully integrated with TournamentDirector
- **Status:** Complete (2026-02-16). TournamentDirector.processTable() (line 638) calls engine_.processTable() on every cycle. Old _processTable() fully replaced. SwingEventBus and SwingPlayerActionProvider bridge the interfaces.
- **Evidence:** HeadlessGameRunnerTest proves pokergamecore runs complete single-table and multi-table tournaments without Swing.

---

## Milestone 1: Server Game Engine Foundation

**Goal:** Create the server-side module that can run a complete poker game without any Swing dependencies.

**Detailed Plan:** `.claude/plans/completed/M1-SERVER-GAME-ENGINE.md`

**Status:** COMPLETE (2026-02-16, in feature worktree `feature-m1-server-game-engine`, pending merge)

**Effort:** XL (largest single phase)

### Phase 1.1: New Module `pokergameserver`

Create a new Maven module that contains all server-side game hosting logic. This module is a **Spring Boot auto-configuration starter** that can be embedded in both:
- The standalone `pokerserver` (remote deployment)
- The desktop `poker` client (embedded local deployment)

**Module dependencies:**
```
pokergameserver
├── pokergamecore (TournamentEngine, PurePokerAI, events)
├── pokerengine (PokerGame, PokerTable, HoldemHand, TournamentProfile)
├── pokernetwork (OnlineMessage types, for compatibility)
├── spring-boot-starter-websocket
├── spring-boot-starter-web (embedded Tomcat/Jetty)
└── common (utils, config)
```

**Must NOT depend on:** `poker` (Swing client), `gameengine` (Swing framework), `gui`

**Spring Boot Auto-Configuration:** The module provides `@AutoConfiguration` so any Spring Boot application that includes it as a dependency automatically gets the game server (REST endpoints, WebSocket handlers, GameInstanceManager). This is what allows both `pokerserver` and the desktop client to embed the same game server with zero code duplication.

**New classes to create:**

| Class | Purpose |
|-------|---------|
| `GameInstance` | Encapsulates one running game: tournament context, player connections, state, thread |
| `GameInstanceManager` | Creates, tracks, cleans up `GameInstance` objects. Concurrent game registry. Thread pool management. |
| `ServerTournamentDirector` | Server-side equivalent of `TournamentDirector`. Not a `BasePhase`. Pure `Runnable`. Drives `TournamentEngine`. |
| `ServerOnlineManager` | Server-side message routing. No `PokerMain` dependency. Routes WebSocket messages to/from game instance. |
| `ServerPlayerSession` | Represents one connected player in a game instance. Tracks connection state, auth, reconnection tokens. |
| `GameStateProjection` | Builds per-player view of game state (hides other players' hole cards). |
| `GameEventStore` | Append-only event log. Writes game events to H2. Supports replay for crash recovery. |

**Key design decisions:**

1. **No GameEngine singleton:** `GameInstance` manages its own state. No static state anywhere. Each instance has its own `PokerGame`, `PokerTable`, etc.

2. **Thread model:** Each `GameInstance` runs its `ServerTournamentDirector` on a thread from a shared `ExecutorService`. The TD thread drives the game loop. Player input arrives via `CompletableFuture` from WebSocket handlers.

3. **Player action flow:**
   ```
   TD thread calls actionProvider.getAction(player, options)
     → ServerPlayerActionProvider checks if human or AI
       → AI: delegates to PurePokerAI (V1/V2Algorithm)
       → Human: posts ActionRequest to player's WebSocket
               waits on CompletableFuture with timeout
               → Player responds → completes future
               → Timeout → auto-fold
   ```

**Files to create:**
- `code/pokergameserver/pom.xml`
- `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/GameInstance.java`
- `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/GameInstanceManager.java`
- `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerTournamentDirector.java`
- `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerOnlineManager.java`
- `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerPlayerSession.java`
- `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/GameStateProjection.java`
- `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/GameEventStore.java`
- `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerPlayerActionProvider.java`

**Files to modify:**
- `code/pom.xml` - Add `pokergameserver` to module list
- `code/pokerserver/pom.xml` - Add dependency on `pokergameserver`

### Phase 1.2: ServerTournamentDirector Implementation

The heart of the server game engine. This is a clean rewrite of `TournamentDirector` that runs server-side without Swing.

**Current TournamentDirector analysis:**
- `code/poker/src/main/java/com/donohoedigital/games/poker/online/TournamentDirector.java`
- ~1000+ lines, extends `BasePhase` (Swing)
- Uses `SwingUtilities.invokeLater` for error handling
- Calls `context_.processPhase()` for UI transitions
- Contains `_processTable()` state machine (already extracted to `TournamentEngine`)
- Handles: dealing, betting, showdown, table balancing, level changes, breaks, rebuys, addons

**ServerTournamentDirector must:**
1. Use `TournamentEngine.processTable()` for core game loop (already extracted to pokergamecore)
2. Replace all Swing/UI calls with WebSocket message dispatching
3. Handle player timeouts (auto-fold/auto-check)
4. Support pause/resume controlled by game owner
5. Manage table balancing for multi-table tournaments
6. Handle rebuy/addon decisions (async via WebSocket)
7. Manage blind level progression using `TournamentProfile`

**Reuse from pokergamecore:**
- `TournamentEngine` - core state machine
- `GameEventBus` - event publishing
- `PlayerActionProvider` - player action interface
- `TableProcessResult` - processing outcomes
- `BettingRound`, `TableState` - state enums

### Phase 1.3: GameInstance Lifecycle

```
GameInstance States:
  CREATED → WAITING_FOR_PLAYERS → STARTING → IN_PROGRESS → PAUSED → IN_PROGRESS → COMPLETED
                                                                                  → CANCELLED
```

**GameInstance responsibilities:**
- Hold all state for one game (PokerGame, PokerTable[], players, settings)
- Create and manage ServerTournamentDirector thread
- Track connected player sessions (WebSocket connections)
- Handle player join/leave/reconnect during game
- Handle owner commands (pause, kick, cancel)
- Publish game lifecycle events to GameEventStore
- Clean up resources when game ends

**GameInstanceManager responsibilities:**
- `Map<String, GameInstance>` - concurrent game registry keyed by game ID
- Create new game instances from `TournamentProfile` config
- Monitor game health (detect stuck games, clean up finished ones)
- Enforce concurrent game limits (configurable, default 50)
- Provide game listing/filtering for lobby
- Thread pool management: `ScheduledThreadPoolExecutor` with configurable pool size

### Phase 1.4: Event Sourcing & Persistence

**Event types to store:**

| Event | Data |
|-------|------|
| `GAME_CREATED` | Game ID, profile, creator, timestamp |
| `PLAYER_JOINED` | Player ID, seat, stack |
| `PLAYER_LEFT` | Player ID, reason |
| `GAME_STARTED` | Start timestamp, player list |
| `HAND_STARTED` | Hand #, dealer seat, blinds posted |
| `CARDS_DEALT` | Hand ID, player hole cards (encrypted), community cards |
| `PLAYER_ACTION` | Player ID, action type, amount, pot after |
| `BETTING_ROUND_COMPLETE` | Round, pot amounts |
| `SHOWDOWN` | Winners, hands shown, pot distributions |
| `HAND_COMPLETE` | Hand #, final state |
| `LEVEL_CHANGED` | New level, blind amounts |
| `PLAYER_ELIMINATED` | Player ID, finish position |
| `GAME_PAUSED` / `GAME_RESUMED` | Timestamp, reason |
| `GAME_COMPLETED` | Final standings, prize distribution |

**H2 Schema:**
```sql
CREATE TABLE game_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_id VARCHAR(64) NOT NULL,
    sequence_number BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_data JSON NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    INDEX idx_game_events_game_id (game_id),
    UNIQUE INDEX idx_game_events_sequence (game_id, sequence_number)
);

CREATE TABLE game_instances (
    game_id VARCHAR(64) PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    profile_data JSON NOT NULL,
    owner_profile_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    hosting_type VARCHAR(20) NOT NULL DEFAULT 'SERVER',  -- 'SERVER' or 'COMMUNITY'
    FOREIGN KEY (owner_profile_id) REFERENCES wan_profile(ID)
);
```

**Crash recovery:** On server restart, scan `game_instances` for `IN_PROGRESS` games. For each, replay `game_events` to rebuild state. Resume game from last known state. Connected clients will need to reconnect (handled by WebSocket reconnection protocol).

---

## Milestone 2: Game API, Authentication & Database Persistence

**Goal:** REST API endpoints for game management, JWT-based authentication (RS256 asymmetric), profile CRUD, ban system, and H2 database persistence.

**Detailed Plan:** `.claude/plans/M2-GAME-API-AUTH-PERSISTENCE.md`

**Status:** DRAFT (plan complete, implementation pending)

**Effort:** L

**What M2 delivers:**
- **JWT authentication (RS256 asymmetric)** — WAN server signs with private key, all game servers validate with public key. Supports three auth modes: full (issuing), validate-only (community-hosted), and local (LAN/offline)
- **Profile management API** — registration, update, password change, retire (`/api/v1/profiles`)
- **Game management REST API** — CRUD and lifecycle at `/api/v1/games`
- **GameConfig model** — clean server-side tournament configuration (replaces legacy TournamentProfile for server use)
- **Ban system** — profile ID and email-based bans (no IP bans)
- **Database persistence** — H2 + Spring Data JPA for event store, game instances, and profiles

M1 implements the event store with in-memory storage. M2 adds `DatabaseGameEventStore` backed by H2, enabling persistent game history, simulation analysis across sessions, and crash recovery.

**See detailed plan for complete implementation steps, TDD methodology, and architecture decisions.**

---

## Milestone 3: WebSocket Game Protocol

**Goal:** Real-time bidirectional communication for gameplay.

**Effort:** XL

### Phase 3.1: WebSocket Infrastructure

**Spring WebSocket configuration:**
- Endpoint: `ws://server/ws/games/{gameId}`
- Auth: JWT token passed as query parameter or in initial CONNECT message
- Sub-protocol: JSON text frames
- Per-game rooms: each game is a WebSocket "room" managed by `GameInstance`

**New classes:**
- `code/pokerserver/src/main/java/.../server/websocket/GameWebSocketConfig.java` - Spring WebSocket config
- `code/pokerserver/src/main/java/.../server/websocket/GameWebSocketHandler.java` - Message routing
- `code/pokerserver/src/main/java/.../server/websocket/PlayerConnection.java` - Per-player connection wrapper

**Connection lifecycle:**
```
1. Client opens WebSocket to /ws/games/{gameId}?token=JWT
2. Server validates JWT, looks up GameInstance
3. Server sends CONNECTED message with game state snapshot
4. Client is now in the game room
5. Server pushes state updates as they occur
6. Client sends actions when it's their turn
7. On disconnect: server marks player as disconnected, starts reconnection timer
8. On reconnect: server sends full state snapshot, player resumes
```

### Phase 3.2: Server → Client Messages

All messages follow envelope format:
```json
{
  "type": "MESSAGE_TYPE",
  "gameId": "abc123",
  "timestamp": "2026-02-15T20:05:00Z",
  "data": { ... }
}
```

**Message types:**

#### Connection & State
```json
// CONNECTED - Sent on successful WebSocket connection
{
  "type": "CONNECTED",
  "data": {
    "playerId": 42,
    "gameState": { /* full state snapshot (see below) */ }
  }
}

// GAME_STATE - Full state snapshot (sent on connect and reconnect)
{
  "type": "GAME_STATE",
  "data": {
    "status": "IN_PROGRESS",
    "level": 3,
    "blinds": { "small": 100, "big": 200, "ante": 25 },
    "nextLevelIn": 847,  // seconds
    "tables": [{
      "tableId": 0,
      "seats": [
        {
          "seatIndex": 0,
          "playerId": 42,
          "playerName": "Alice",
          "chipCount": 4500,
          "status": "ACTIVE",          // ACTIVE, FOLDED, ALL_IN, SITTING_OUT, ELIMINATED
          "isDealer": false,
          "isSmallBlind": true,
          "isBigBlind": false,
          "currentBet": 100,
          "holeCards": ["Ah", "Kd"],   // ONLY included for the receiving player
          "isCurrentActor": false
        },
        {
          "seatIndex": 1,
          "playerId": 43,
          "playerName": "Bob",
          "chipCount": 5200,
          "status": "ACTIVE",
          "holeCards": null,            // hidden from other players
          "isCurrentActor": true
        }
      ],
      "communityCards": ["Js", "Tc", "4h"],
      "pots": [{ "amount": 600, "eligiblePlayers": [42, 43, 44] }],
      "currentRound": "FLOP",         // PREFLOP, FLOP, TURN, RIVER, SHOWDOWN
      "handNumber": 15
    }],
    "players": [
      { "playerId": 42, "name": "Alice", "chipCount": 4500, "tableId": 0, "seatIndex": 0, "finishPosition": null },
      { "playerId": 43, "name": "Bob", "chipCount": 5200, "tableId": 0, "seatIndex": 1, "finishPosition": null }
    ]
  }
}
```

#### Hand Flow
```json
// HAND_STARTED
{
  "type": "HAND_STARTED",
  "data": {
    "handNumber": 16,
    "dealerSeat": 2,
    "smallBlindSeat": 3,
    "bigBlindSeat": 4,
    "blindsPosted": [
      { "playerId": 44, "amount": 100, "type": "SMALL_BLIND" },
      { "playerId": 45, "amount": 200, "type": "BIG_BLIND" }
    ]
  }
}

// HOLE_CARDS_DEALT - Sent ONLY to the specific player
{
  "type": "HOLE_CARDS_DEALT",
  "data": {
    "cards": ["Ah", "Kd"]
  }
}

// COMMUNITY_CARDS_DEALT - Sent to all players
{
  "type": "COMMUNITY_CARDS_DEALT",
  "data": {
    "round": "FLOP",
    "cards": ["Js", "Tc", "4h"],
    "allCommunityCards": ["Js", "Tc", "4h"]
  }
}

// ACTION_REQUIRED - Sent ONLY to the player who needs to act
{
  "type": "ACTION_REQUIRED",
  "data": {
    "timeoutSeconds": 30,
    "options": {
      "canFold": true,
      "canCheck": false,
      "canCall": true,
      "callAmount": 200,
      "canBet": false,
      "canRaise": true,
      "minRaise": 400,
      "maxRaise": 4500,
      "canAllIn": true,
      "allInAmount": 4500
    }
  }
}

// PLAYER_ACTED - Sent to all players
{
  "type": "PLAYER_ACTED",
  "data": {
    "playerId": 42,
    "playerName": "Alice",
    "action": "RAISE",
    "amount": 600,
    "totalBet": 600,
    "chipCount": 3900,
    "potTotal": 1000
  }
}

// ACTION_TIMEOUT - Sent to all players when someone times out
{
  "type": "ACTION_TIMEOUT",
  "data": {
    "playerId": 42,
    "autoAction": "FOLD"
  }
}

// HAND_COMPLETE
{
  "type": "HAND_COMPLETE",
  "data": {
    "handNumber": 16,
    "winners": [
      {
        "playerId": 43,
        "amount": 1200,
        "hand": "Two Pair, Jacks and Tens",
        "cards": ["Jd", "Ts"],             // shown only at showdown
        "potIndex": 0
      }
    ],
    "showdownPlayers": [
      { "playerId": 43, "cards": ["Jd", "Ts"], "handDescription": "Two Pair" },
      { "playerId": 42, "cards": ["Ah", "Kd"], "handDescription": "Ace High" }
    ]
  }
}
```

#### Tournament Events
```json
// LEVEL_CHANGED
{
  "type": "LEVEL_CHANGED",
  "data": {
    "level": 4,
    "smallBlind": 200,
    "bigBlind": 400,
    "ante": 50,
    "nextLevelIn": 1200
  }
}

// PLAYER_ELIMINATED
{
  "type": "PLAYER_ELIMINATED",
  "data": {
    "playerId": 42,
    "playerName": "Alice",
    "finishPosition": 6,
    "handsPlayed": 47
  }
}

// REBUY_OFFERED - Sent ONLY to the eliminated player (if rebuys available)
{
  "type": "REBUY_OFFERED",
  "data": {
    "cost": 1000,
    "chips": 5000,
    "timeoutSeconds": 15
  }
}

// ADDON_OFFERED - Sent to eligible players during addon break
{
  "type": "ADDON_OFFERED",
  "data": {
    "cost": 1000,
    "chips": 3000,
    "timeoutSeconds": 15
  }
}

// GAME_COMPLETE
{
  "type": "GAME_COMPLETE",
  "data": {
    "standings": [
      { "position": 1, "playerId": 43, "playerName": "Bob", "prize": 5000 },
      { "position": 2, "playerId": 44, "playerName": "Charlie", "prize": 3000 }
    ],
    "totalHands": 127,
    "duration": 7200
  }
}
```

#### Admin/Management
```json
// PLAYER_JOINED / PLAYER_LEFT
{
  "type": "PLAYER_JOINED",
  "data": { "playerId": 46, "playerName": "Dave", "seatIndex": 5 }
}

// GAME_PAUSED / GAME_RESUMED
{
  "type": "GAME_PAUSED",
  "data": { "reason": "Owner paused the game", "pausedBy": "Alice" }
}

// PLAYER_KICKED
{
  "type": "PLAYER_KICKED",
  "data": { "playerId": 46, "playerName": "Dave", "reason": "Removed by host" }
}

// CHAT_MESSAGE
{
  "type": "CHAT_MESSAGE",
  "data": { "playerId": 42, "playerName": "Alice", "message": "Nice hand!", "tableChat": true }
}

// TIMER_UPDATE - Periodic countdown for current actor
{
  "type": "TIMER_UPDATE",
  "data": { "playerId": 43, "secondsRemaining": 15 }
}

// ERROR
{
  "type": "ERROR",
  "data": { "code": "INVALID_ACTION", "message": "It is not your turn to act" }
}
```

### Phase 3.3: Client → Server Messages

```json
// PLAYER_ACTION - Player submits their action
{
  "type": "PLAYER_ACTION",
  "data": {
    "action": "RAISE",    // FOLD, CHECK, CALL, BET, RAISE, ALL_IN
    "amount": 600         // required for BET and RAISE
  }
}

// REBUY_DECISION
{
  "type": "REBUY_DECISION",
  "data": { "accept": true }
}

// ADDON_DECISION
{
  "type": "ADDON_DECISION",
  "data": { "accept": true }
}

// CHAT
{
  "type": "CHAT",
  "data": { "message": "Nice hand!", "tableChat": true }
}

// SIT_OUT / COME_BACK
{
  "type": "SIT_OUT",
  "data": {}
}

// ADMIN_KICK (owner only)
{
  "type": "ADMIN_KICK",
  "data": { "playerId": 46 }
}

// ADMIN_PAUSE / ADMIN_RESUME (owner only)
{
  "type": "ADMIN_PAUSE",
  "data": {}
}
```

### Phase 3.4: Security Rules

| Rule | Implementation |
|------|----------------|
| Hole cards private | Server only sends `HOLE_CARDS_DEALT` to card owner. `GAME_STATE` omits other players' cards. Cards revealed only at `SHOWDOWN`. |
| Action validation | Server validates: correct player's turn, valid action type, valid amount (min/max raise, can't bet more than stack). Rejects invalid actions with ERROR. |
| Rate limiting | Max 1 action per second per player. Prevents spam. |
| Timeout | Configurable (default 30s). Auto-fold on timeout. Three consecutive timeouts = auto sit-out. |
| Anti-replay | Each action has a monotonic sequence number. Server rejects duplicate/out-of-order actions. |
| Owner validation | Admin commands (kick, pause, settings) verified against game owner profile ID from JWT. |
| Reconnection | Player has 2 minutes to reconnect. During disconnection, auto-fold on their turn. On reconnect, full state snapshot sent. |

---

## Milestone 4: Desktop Client Adaptation

**Goal:** Refactor the desktop client to embed a Spring Boot game server and use the WebSocket protocol for ALL game modes (practice, community-hosted, server-hosted).

**UI Constraint:** The desktop Swing client's look, feel, and workflows must remain as similar as possible to the current client. Existing users know this UI — we are changing the plumbing underneath, not the user experience. New options (e.g., "Server Hosted" toggle in game creation) are fine, but existing screens, button layouts, game flow, and visual style must be preserved. This constraint does NOT apply to the web client, mobile, or server — those are free to design as needed.

**Effort:** XL

### Phase 4.1: Embed Spring Boot in Desktop Client

The desktop client (`PokerMain`) will start an embedded Spring Boot application context that runs the game server locally. The Swing UI connects to this embedded server via `ws://localhost:{port}`.

**Approach:**
- Add `pokergameserver` as a dependency of the `poker` module
- On desktop startup, launch `SpringApplication` programmatically in a background thread
- Assign a random available port for the embedded server
- The Swing UI connects to `ws://localhost:{port}/ws/games/{gameId}` like any other client

**New classes:**
- `code/poker/src/main/java/.../poker/server/EmbeddedGameServer.java` - Manages Spring Boot lifecycle (start/stop)
- `code/poker/src/main/java/.../poker/server/EmbeddedServerConfig.java` - Spring config for embedded mode (simplified auth, in-memory storage)
- `code/poker/src/main/java/.../poker/online/WebSocketGameClient.java` - Java WebSocket client (uses JDK `java.net.http.HttpClient` WebSocket API)
- `code/poker/src/main/java/.../poker/online/GameStateAdapter.java` - Converts WebSocket JSON messages into `PokerGame`/`PokerTable` state that Swing UI can render

**Embedded server configuration differs from standalone:**

| Setting | Standalone (remote) | Embedded (desktop) |
|---------|--------------------|--------------------|
| Port | Configured (e.g., 8080) | Random available port |
| Auth | Full JWT + profiles | Simplified (local user auto-authenticated) |
| Database | H2 file-based (`/data/poker`) | H2 file-based (`~/.ddpoker/games`) |
| Max games | 50 | 1-3 (local use only) |
| Game discovery | Full WAN registration | Register with remote WAN server if community-hosted |
| Profiles | Full profile database | Single local profile |

**Startup sequence:**
```
1. PokerMain launches
2. EmbeddedGameServer.start() → Spring Boot starts on random port
3. Swing UI initializes (existing flow)
4. When user creates/joins game:
   - Practice/community: POST to localhost:{port}/api/v2/games
   - Server-hosted: POST to remote server URL
5. Connect WebSocket to appropriate server
6. Play game through WebSocket protocol
```

**Files to modify:**
- `code/poker/pom.xml` - Add `pokergameserver` dependency, add Spring Boot dependencies
- `code/poker/src/main/java/.../poker/PokerMain.java` - Start embedded server on init

### Phase 4.2: WebSocket Client Integration

Replace all direct game state access with WebSocket-mediated state.

**Key changes to existing code:**

1. **GameStateAdapter:**
   - The Swing UI currently reads from `PokerGame`, `PokerTable`, `HoldemHand` objects directly
   - `GameStateAdapter` implements these same interfaces (or wraps them) but populates state from WebSocket messages
   - When a `GAME_STATE`, `PLAYER_ACTED`, `HAND_STARTED`, etc. message arrives, the adapter updates its internal state and fires Swing events to trigger UI refresh

2. **Player action flow:**
   - Currently: Swing button click → local method call → `OnlineManager` → P2P message
   - New: Swing button click → WebSocket `PLAYER_ACTION` message → server validates → `PLAYER_ACTED` broadcast → UI updates
   - The existing Swing action buttons (fold, check, call, raise, bet slider) remain visually identical
   - Under the hood, they send JSON over WebSocket instead of calling local methods
   - Users should not notice any difference in the game flow

3. **OnlineManager removal:**
   - The existing `OnlineManager` with its P2P routing is **deleted** entirely
   - `WebSocketGameClient` replaces it for all game modes
   - No backward compatibility needed (no existing user base)

**The adapter pattern is key to preserving the UI.** Existing Swing components (`PokerGameboard`, `ShowTournamentTable`, `PlayerComponent`, etc.) read game state through `PokerGame`, `PokerTable`, and `HoldemHand` interfaces. The adapter classes below implement these same interfaces but are populated by WebSocket messages. This means the Swing UI code remains virtually unchanged — it reads the same interfaces, renders the same way, but the data source is now WebSocket instead of local game objects.

**New classes:**
- `code/poker/src/main/java/.../poker/online/WebSocketGameClient.java` - Manages WebSocket connection, sends/receives JSON messages
- `code/poker/src/main/java/.../poker/online/GameStateAdapter.java` - Translates WebSocket state updates to Swing-compatible game state objects
- `code/poker/src/main/java/.../poker/online/RemotePokerGame.java` - Implements `PokerGame`-compatible interface populated by WebSocket state
- `code/poker/src/main/java/.../poker/online/RemotePokerTable.java` - Implements `PokerTable`-compatible interface

### Phase 4.3: Game Mode Unification

**ALL game modes now use the same flow:**

| Mode | Server | URL | Auth | Discovery |
|------|--------|-----|------|-----------|
| **Practice** | Embedded (localhost) | `ws://localhost:{port}/ws/games/{id}` | Auto (local user) | Not listed anywhere |
| **Community-hosted** | Embedded (host's IP) | `ws://{host-ip}:{port}/ws/games/{id}` | JWT (registered profiles) | Registered with WAN server |
| **Server-hosted** | Standalone (remote) | `wss://{server}/ws/games/{id}` | JWT (registered profiles) | Listed by server |

**Practice mode specifics:**
- User creates game via `POST localhost:{port}/api/v2/games` with AI player config
- Connects via WebSocket
- AI players run on the embedded server
- No WAN registration
- No timeout on player actions (or very long timeout)
- Optional: event sourcing disabled for practice (faster, no persistence needed)

**Community-hosted mode specifics:**
- Same as practice but embedded server binds to `0.0.0.0:{port}` (not just localhost)
- Registers game with WAN server via existing `CAT_WAN_GAME_ADD` (or new REST call), providing WebSocket URL
- Other players' desktop/web clients connect to host's WebSocket endpoint
- Same NAT/port-forwarding requirement as current P2P model
- Host can optionally set game password

**P2P/TCP code removal:**
- The old `PokerConnectionServer`, `OnlineManager` P2P routing, and TCP-based game hosting code is **deleted** as part of this plan
- No user base exists, so there is no backward compatibility requirement
- All game modes use WebSocket exclusively

### Phase 4.4: Save/Load for Embedded Server

Practice games need save/load functionality (existing feature in desktop client).

**Approach:**
- The embedded server uses file-based H2 (`~/.ddpoker/games`) — same as standalone server
- Save: Already handled by event sourcing — the event log IS the save. Games persist automatically.
- Load: On desktop startup, embedded server reads `game_instances` table. Incomplete games can be resumed by replaying events.
- No separate save/load UI needed — games auto-persist. User can resume in-progress practice games from the lobby.
- Game history available via `GET /api/v2/games` on embedded server (same API as remote)

**File to create:**
- `code/poker/src/main/java/.../poker/server/GameSaveManager.java` - Handles resume of in-progress games on startup

---

## Milestone 5: Web Client Game UI

**Goal:** Build the poker game interface in the existing Next.js app.

**Effort:** XL

### Phase 5.1: WebSocket Client Library

**Create a TypeScript WebSocket client for the game protocol.**

**Files to create in `code/web/`:**
```
lib/game/
├── GameClient.ts              - WebSocket connection manager
├── GameProtocol.ts            - Message type definitions
├── GameState.ts               - Client-side game state store
├── useGameConnection.ts       - React hook for WebSocket lifecycle
├── useGameState.ts            - React hook for reactive game state
└── types.ts                   - TypeScript interfaces for all message types
```

**GameClient.ts:**
- Manages WebSocket connection lifecycle
- Handles JWT auth on connect
- Auto-reconnect with exponential backoff
- Message serialization/deserialization
- Event emitter pattern for message handling

**GameState.ts:**
- Client-side game state store (can use Zustand or React context)
- Updated by incoming WebSocket messages
- Provides reactive state to React components
- Handles optimistic updates (show action immediately, confirm from server)

### Phase 5.2: Game Table Component

**The core poker table UI rendered in React.**

```
components/game/
├── GameTable.tsx              - Main table layout (oval table, seats, community cards)
├── PlayerSeat.tsx             - Individual player seat (avatar, name, chips, cards, status)
├── CommunityCards.tsx         - Shared cards in center
├── PotDisplay.tsx             - Pot amount(s) display
├── ActionPanel.tsx            - Fold/Check/Call/Raise buttons + slider
├── HandHistory.tsx            - Scrollable hand log
├── ChatPanel.tsx              - In-game chat
├── TournamentInfo.tsx         - Level, blinds, timer, player count
├── Card.tsx                   - Individual card rendering (face up/down)
├── ChipStack.tsx              - Visual chip stack
└── TimerBar.tsx               - Action timeout countdown
```

**Design approach:**
- React components with Tailwind CSS for layout
- CSS transforms for card animations (deal, flip, slide)
- No Canvas/WebGL needed - HTML/CSS sufficient for poker
- Responsive design: works on desktop browsers and tablets
- Dark theme to match DD Poker aesthetic

### Phase 5.3: Game Management Pages

**Extend existing Next.js pages:**

```
app/
├── games/                     - NEW: Game lobby and management
│   ├── page.tsx               - Game list (available, in-progress, completed)
│   ├── create/
│   │   └── page.tsx           - Create new game form
│   ├── [gameId]/
│   │   ├── page.tsx           - Game play page (contains GameTable)
│   │   ├── lobby/
│   │   │   └── page.tsx       - Pre-game lobby (waiting for players)
│   │   ├── settings/
│   │   │   └── page.tsx       - Game settings (owner only)
│   │   └── results/
│   │       └── page.tsx       - Post-game results
```

**Game list page:**
- Server-hosted games: badge with server icon
- Community-hosted games: badge with user icon
- Filters: status (available, in-progress, completed), hosting type, player count
- Join button for available games
- Uses existing component patterns from `code/web/components/`

**Create game page:**
- Tournament profile configuration form
- Blind structure builder
- AI player configuration
- Private game option with password
- Submit creates game via REST API, redirects to lobby

**Game lobby page:**
- Shows connected players with seat selection
- Chat
- Owner controls: start, kick, change settings
- Auto-start countdown option

---

## Milestone 6: Game Discovery & Management

**Goal:** Unified game discovery that categorizes server-hosted vs community-hosted games.

**Effort:** M

### Phase 6.1: Unified Game Registry

**Modify existing WAN game infrastructure to include server-hosted games.**

Currently `WanGame` records in the database track community-hosted games. Extend to include server-hosted games.

**Approach:**
- Add `hosting_type` column to existing WAN game table (or use the new `game_instances` table)
- Server-hosted games: created via REST API, stored in `game_instances`
- Community-hosted games: registered via existing `CAT_WAN_GAME_ADD` protocol, stored in existing WAN tables
- Game listing API returns both, with `hostingType` field

**Modified files:**
- `code/pokerserver/src/main/java/.../server/service/WanGameService.java` (or equivalent) - Unified listing
- `code/pokernetwork/src/main/java/.../network/WanGame.java` - Add hosting type field

### Phase 6.2: Owner/Admin Privileges

**Game owner capabilities (same for both desktop and web):**

| Privilege | When Available | Implementation |
|-----------|---------------|----------------|
| Start game | WAITING_FOR_PLAYERS → IN_PROGRESS | REST: `POST /games/{id}/start` or WebSocket `ADMIN_START` |
| Pause/resume | IN_PROGRESS ↔ PAUSED | REST or WebSocket admin command |
| Kick player | Any time before COMPLETED | REST: `POST /games/{id}/kick` or WebSocket `ADMIN_KICK` |
| Cancel game | Any time before COMPLETED | REST: `DELETE /games/{id}` |
| Change settings | WAITING_FOR_PLAYERS only | REST: `PUT /games/{id}/settings` |
| Add/remove AI | WAITING_FOR_PLAYERS only | Part of settings update |
| Set password | WAITING_FOR_PLAYERS only | Part of settings update |

**Authorization:** All owner actions verified server-side by comparing JWT profile ID with game owner ID.

### Phase 6.3: Desktop Client Game List Updates

**Modify existing game listing UI in desktop client:**

- Currently: Shows WAN games from PokerServer
- New: Fetch from `GET /api/v2/games` REST endpoint on remote server
- Display format: Add column/badge for "Server Hosted" vs "Community Hosted"
- Filter: Allow filtering by hosting type
- Join flow (unified): All games joined via WebSocket — the only difference is the WebSocket URL
  - Server-hosted: `wss://{remote-server}/ws/games/{id}`
  - Community-hosted: `ws://{host-ip}:{port}/ws/games/{id}`

---

## Phase Dependencies & Ordering

```
Prerequisites:
  P1 (V2 AI extraction) ─────────────┐
  P2 (ServerAIProvider) ──────────────┤
  P3 (TournamentEngine integration) ──┤
                                      ▼
Milestone 1: Server Game Engine ──────┐
  Phase 1.1: pokergameserver module   │
  Phase 1.2: ServerTournamentDirector │
  Phase 1.3: GameInstance lifecycle   │
  Phase 1.4: Event sourcing          │
                                      ▼
Milestone 2: Game API & Auth ─────────┐
  Phase 2.1: JWT authentication       │ ← Can partially parallel with M1
  Phase 2.2: Game management REST API │
  Phase 2.3: TournamentProfile JSON   │
                                      ▼
Milestone 3: WebSocket Protocol ──────┐
  Phase 3.1: WebSocket infrastructure │
  Phase 3.2: Server→Client messages   │
  Phase 3.3: Client→Server messages   │
  Phase 3.4: Security rules           │
                                      ▼
          ┌───────────────────────────┴───────────────────────────┐
          ▼                                                       ▼
Milestone 4: Desktop Adaptation              Milestone 5: Web Client
  Phase 4.1: Embed Spring Boot                 Phase 5.1: WS client lib
  Phase 4.2: WebSocket client integration      Phase 5.2: Game table component
  Phase 4.3: Game mode unification             Phase 5.3: Game management pages
  Phase 4.4: Save/load for embedded
          │                                                       │
          └───────────────────────────┬───────────────────────────┘
                                      ▼
                          Milestone 6: Game Discovery
                            Phase 6.1: Unified registry
                            Phase 6.2: Owner privileges
                            Phase 6.3: Desktop list updates
```

**Milestones 4 and 5 can be developed in parallel** once Milestone 3 is complete. They share no code dependencies (Java vs TypeScript).

**Milestone 7 (P2P/TCP Code Removal)** can happen during or after Milestone 4, as the new WebSocket path replaces each piece of old code.

---

## Milestone 7: Legacy P2P/TCP Code Removal

**Goal:** Remove all P2P/TCP networking code that is replaced by the WebSocket protocol. No existing user base means no backward compatibility needed.

**Effort:** M

### Phase 7.1: Remove P2P Game Hosting Code

**Delete or gut the following:**
- `OnlineManager` - P2P message routing hub (entire class, replaced by `WebSocketGameClient`)
- `PokerConnectionServer` (inner class of `PokerMain`) - TCP listener for P2P connections
- `TournamentDirector` - The Swing-dependent game engine (replaced by `ServerTournamentDirector`)
- P2P connection classes in `code/server/src/main/java/com/donohoedigital/p2p/`
- TCP-specific message handling in `code/poker/src/main/java/.../poker/online/`

### Phase 7.2: Clean Up Networking Infrastructure

- Remove `PokerConnectionServer` / `Peer2PeerServer` / related TCP server code
- Remove P2P-specific message types from `OnlineMessage` (keep types used by WAN server communication if still needed for game registration)
- Remove `bHost_` flag and all host-vs-client branching in the desktop client
- Clean up `PokerMain` to remove TCP server startup code

### Phase 7.3: Simplify Desktop Client

- `PokerMain` becomes: start embedded Spring Boot server + launch Swing UI
- Remove all game logic from the `poker` module that now lives in `pokergameserver`
- The `poker` module becomes purely: Swing UI + WebSocket client + embedded server bootstrap
- Remove direct dependencies on `TournamentDirector`, `OnlineManager` from UI classes

**Risk:** Must ensure ALL functionality is covered by the WebSocket path before deleting old code. Run full test suite and manual testing of all game modes before cleanup.

---

## Effort Summary

| Milestone | Effort | Estimated Duration | Risk |
|-----------|--------|-------------------|------|
| Prerequisites (P1-P3) | L | Partially complete, ~2-4 weeks remaining | Medium (V2 extraction complexity) |
| M1: Server Game Engine | XL | 4-6 weeks | High (core architecture, most complex) |
| M2: Game API & Auth | L | 2-3 weeks | Low (standard Spring Boot patterns) |
| M3: WebSocket Protocol | XL | 3-4 weeks | Medium (real-time correctness, security) |
| M4: Desktop Adaptation | XL | 5-7 weeks | High (embedded server + Swing adapter + mode unification) |
| M5: Web Client | XL | 4-6 weeks | Medium (new UI, but well-defined protocol) |
| M6: Game Discovery | M | 1-2 weeks | Low (extends existing infrastructure) |
| M7: Legacy P2P Removal | M | 2-3 weeks | Medium (must verify all paths covered first) |
| **Total** | | **~23-35 weeks** | |

---

## Risk Register

| # | Risk | Impact | Likelihood | Mitigation |
|---|------|--------|------------|------------|
| R1 | TournamentEngine doesn't cover all TournamentDirector edge cases | High | Medium | Audit every TD code path. Build comprehensive integration tests before server rollout. |
| R2 | Game state synchronization bugs (client shows stale state) | High | Medium | Sequence numbers on all messages. Full state snapshot on reconnect. Automated protocol tests. |
| R3 | Desktop Swing adapter is too complex | Medium | Medium | Start with web client to prove the protocol. Apply lessons learned to desktop adapter. |
| R8 | Embedded Spring Boot startup too slow / heavy for desktop | Medium | Low | Spring Boot 3.x starts in 1-2 seconds. Can use lazy initialization. Desktop has plenty of RAM. Profile and optimize if needed. |
| R9 | Spring Boot embedded in Swing creates classloader/thread conflicts | Medium | Low | Spring Boot designed for embedding. Use separate class loader if needed. Test thoroughly on all platforms. |
| R4 | WebSocket scalability with 50 concurrent games | Low | Low | Each game has ~10 connections. 500 concurrent WebSockets is trivial for Spring Boot. |
| R5 | Crash recovery doesn't fully restore game state | Medium | Medium | Test crash recovery extensively. Event sourcing makes this deterministic - replay should be exact. |
| R6 | Concurrency bugs in GameInstanceManager | High | Medium | Thread-safe collections. Careful lock ordering. Stress tests with concurrent game creation/deletion. |
| R7 | V2 AI extraction takes longer than expected | Medium | Medium | V1 Algorithm + TournamentAI are fallback. Server can launch with simpler AI. |

---

## Testing Strategy

### Unit Tests (per milestone)
- **M1:** GameInstance lifecycle, ServerTournamentDirector with mock players, GameEventStore read/write/replay
- **M2:** JWT generation/validation, REST controller tests (Spring MockMvc), DTO conversion
- **M3:** WebSocket message serialization, GameStateProjection (ensure no card leakage), action validation
- **M4:** EmbeddedGameServer lifecycle, WebSocketGameClient connection/reconnect, GameStateAdapter, RemotePokerGame/Table adapters, save/load via event replay
- **M5:** React component tests, GameClient.ts unit tests, GameState store tests

### Integration Tests
- Full game lifecycle: create → join → start → play hands → complete (automated with AI players only)
- Mixed human + AI game: WebSocket client sends actions, verify game progresses correctly
- Crash recovery: kill server mid-game, restart, verify state restored, clients reconnect
- Concurrent games: run 10 games simultaneously, verify no cross-contamination
- Desktop + web mixed: desktop client and web client in same game
- Embedded server: verify Spring Boot starts/stops cleanly within desktop client JVM
- All three modes: practice (embedded, localhost), community-hosted (embedded, external), server-hosted (standalone)

### End-to-End Tests
- Manual testing with desktop client in practice mode (embedded server, AI opponents)
- Manual testing with desktop client in community-hosted mode (two desktop clients, one hosting)
- Manual testing with desktop client connecting to remote server-hosted game
- Manual testing with web browser playing full tournament on remote server
- Desktop + web in same community-hosted game (desktop hosts, web joins)
- Verify game discovery shows both server-hosted and community-hosted games
- Verify owner controls work from both desktop and web
- Save/load practice game via embedded server

### Security Tests
- Verify hole cards never leaked to wrong player (automated: intercept all WebSocket messages, scan for card data)
- Verify actions rejected when not player's turn
- Verify owner-only actions rejected for non-owners
- Verify JWT expiration and refresh flow

---

## Deployment Changes

### Standalone Server (Docker)
- `docker/Dockerfile` - Add `pokergameserver` jar to build
- `docker/docker-compose.yml` - Expose WebSocket port (same as HTTP, typically 443 with upgrade)
- No new containers needed - pokergameserver runs within pokerserver process

**Configuration (`code/pokerserver/src/main/resources/application.properties`):**
```properties
game.server.enabled=true
game.server.max-concurrent-games=50
game.server.action-timeout-seconds=30
game.server.reconnect-timeout-seconds=120
jwt.secret=<generated>
jwt.expiration-ms=86400000
```

### Embedded Server (Desktop Client)
The desktop client jar grows to include Spring Boot + pokergameserver dependencies. Expected increase: ~15-25MB.

**Embedded configuration (auto-detected via Spring profile `embedded`):**
```properties
# Activated when running inside desktop client
spring.profiles.active=embedded
server.port=0                              # Random available port
game.server.max-concurrent-games=3
game.server.action-timeout-seconds=0       # No timeout for practice
game.server.auth.mode=local                # Auto-authenticate local user
spring.datasource.url=jdbc:h2:file:~/.ddpoker/games  # File-based H2, same as server
```

---

## Future Extensions (Not in Scope)

- **Mobile app:** Same WebSocket protocol, different UI framework (React Native or Flutter)
- **Spectator mode:** Read-only WebSocket connections that see the table but can't act
- **Game replays:** Replay event log in web UI (event sourcing makes this straightforward)
- **Multi-server clustering:** Load balancing across multiple game server instances
- **Cash games (ring games):** Currently plan is tournament-only, matching existing DD Poker functionality
