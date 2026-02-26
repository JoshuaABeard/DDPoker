# Server-Hosted Game Support: Options Analysis

**Status:** COMPLETED
**Last Updated:** 2026-02-15

## Long-Term Vision: Pure Client-Server Architecture

**Strategic Goal:** Remove desktop-hosted mode entirely. All games run server-side. Support multiple client types (desktop, web, mobile) as pure UI renderers responding to server events.

**Current State:** Hub-and-spoke P2P model where one desktop client hosts the game engine and other clients connect directly to it.

**Target State:** Pure client-server where:
- **Server** hosts all games, runs all game logic, manages all state
- **Clients** (desktop, web, mobile) render UI, send player actions, receive game state updates
- No P2P hosting, no TournamentDirector in client, no desktop game engine

**Client Types:**
1. **Desktop (Java Swing)** - Current client, refactored to remove game logic
2. **Web (JavaScript/React/Vue)** - New browser-based client using same server API
3. **Mobile (future)** - Potential iOS/Android apps using same server API

**Migration Path:** Implement Option C (Native Server Game Engine), then deprecate desktop hosting code once feature parity achieved. Web client developed in parallel once server API stabilizes.

**Feature Parity Requirements:** All limitations S11-S21 must be resolved for server-hosted to fully replace desktop-hosted mode. See sections below for implementation roadmap.

## Recent Developments

### ✅ pokergamecore Module Complete (2026-02-15)

**Impact on Server-Hosted Games:** The pokergamecore module extraction is now complete with Swing-free AI validated. This significantly improves the feasibility of Options B and C.

**Completed Work:**
- ✅ **pokergamecore module** - Pure game logic with zero Swing dependencies
- ✅ **TournamentEngine** - Core game engine in pokergamecore (Swing-free)
- ✅ **TournamentContext interface** - Extended with blind query methods for AI
- ✅ **Tournament AI POC** - Proof-of-concept AI working in headless tests (10-50x faster than random)
- ✅ **Phase 7 plan created** - Roadmap for extracting V1/V2 AI to pokergamecore

**What This Enables:**
- **Option B (Headless Host):** Headless mode now has working AI - can run complete games without Swing
- **Option C (Native Engine):** Clear path to server-native AI (V1/V2 extraction planned in Phase 7)
- **Both options:** Reduced risk - architecture validated, AI feasibility proven

**See:**
- `.claude/plans/PHASE7-AI-EXTRACTION.md` - AI extraction roadmap
- `.claude/sessions/2026-02-15-tournament-ai-poc.md` - POC implementation details
- `.claude/reviews/main-tournament-ai-poc.md` - Code review (approved)

### ✅ V1 AI Algorithm Extraction Complete (2026-02-15)

**Impact on Server-Hosted Games:** V1 poker AI (~1,900 lines) successfully extracted from Swing-dependent code to pokergamecore module. Server-side AI is now functional with 100% behavioral parity to original desktop client AI.

**Completed Work:**
- ✅ **V1Algorithm** - Complete decision-making logic in pokergamecore (Swing-free)
- ✅ **ServerAIContext** - Server-side implementation of AIContext interface
- ✅ **Monte Carlo integration** - Hand strength and improvement odds calculations
- ✅ **Behavioral parity** - All blocking differences resolved, matches original V1Player exactly

**Known Server Limitations (Require Infrastructure):**

These are documented gaps that require server architecture enhancements:

| ID | Issue | Requirement | Impact |
|----|-------|-------------|--------|
| S11 | Limper detection data unavailable | Action history tracking per betting round in GameHand | AI uses conservative fold percentages (can't detect if raiser limped pre-flop) |
| S13 | Opponent profiling unavailable | Opponent modeling system tracking raise/bet frequency | AI uses neutral assumption (50% aggression) for all opponents |
| S14 | Custom AI personality creation unavailable | External strategy file loading or database-backed personality storage with REST API | Server AI limited to 10 built-in PlayerType profiles (playertype.0991-1000.dat), cannot create/load custom personalities |
| S15 | Host dashboard/monitoring unavailable | REST API for real-time game state queries (chip counts, hand progress, player actions) | No visual monitoring of server-hosted games; host is blind to game state unless players report issues |
| S16 | Mid-game management UI unavailable | REST API for pause/resume, kick player, add/remove AI, adjust clock | Cannot manage running games; must let them complete or kill process |
| S17 | Observer/spectator support unclear | Investigation of observer mode compatibility with headless operation | Unknown if observers can join server-hosted games; may require UI that doesn't exist headless |
| S18 | Error handling displays UI dialogs | TournamentDirector uses SwingUtilities/EngineUtils.displayInformationDialog | Errors may be invisible or crash headless process; need logging/monitoring instead |
| S19 | Save/load game state | Database-backed persistence or mounted volume for save files | Cannot save mid-game and resume later; games must complete in single session |
| S20 | Hand history export | API for downloading hand histories or database storage | Players cannot export/review hand histories after game |
| S21 | Tournament configuration UI | Tournament setup via network message (CAT_WAN_GAME_ADD already exists) | Limited to configuration options passable via network; advanced settings may be inaccessible |

**Implementation Notes for Phase 7C (Server AI Enhancements):**

When implementing server-hosted games (Option B or C), these gaps should be addressed:

1. **Action History Tracking (S11):**
   ```java
   // Add to GameHand or similar server class:
   private Map<Integer, Map<GamePlayerInfo, Integer>> actionHistory; // round -> player -> action

   // ServerAIContext.getLastActionInRound() would query this map
   // Enables limper detection: +20-30 fold percentage when opponent limped
   ```

2. **Opponent Profiling (S13):**
   ```java
   // Add opponent profile tracking:
   class OpponentProfile {
       private Map<Integer, ActionStats> frequencyByRound;
       void recordAction(int round, int action);
       int getRaiseFrequency(int round);
       int getBetFrequency(int round);
   }

   // ServerAIContext would query this system
   // Enables adaptive play: fold more vs tight, call more vs loose
   ```

3. **Custom AI Personality Creation (S14):**

   **Current State:** Server loads 10 built-in PlayerType profiles from JAR resources (playertype.0991-1000.dat). These are read-only and packaged at build time.

   **Desktop Capability:** Desktop client has `PlayerTypeManager` GUI that allows users to:
   - Create custom AI personalities with adjustable factors (aggression, tightness, bluff frequency, etc.)
   - Save custom profiles to local filesystem
   - Load and use custom profiles in games

   **Server Gap:** Server AI (`ServerStrategyProvider`) only loads from classpath resources. No support for:
   - External file loading from filesystem/mounted volumes
   - Database-backed personality storage
   - REST API for uploading/managing custom strategies

   **Implementation Options:**

   **Option 1: File-Based Loading (2-3h effort)**
   ```java
   // Add to ServerStrategyProvider:
   - Load from external directory (e.g., /var/ddpoker/strategies/)
   - Watch directory for new .dat files
   - Cache loaded strategies
   - Validation and error handling for malformed files
   ```

   **Option 2: Database + REST API (6-8h effort)**
   ```java
   // Add to pokerserver:
   - New table: ai_personalities (id, name, strategy_factors JSON, owner_id)
   - REST endpoints: POST /api/ai/personalities, GET /api/ai/personalities
   - Permission model (public vs private personalities)
   - ServerStrategyProvider loads from database instead of files
   ```

   **Option 3: Hybrid (3-4h effort)**
   ```java
   // Combine both:
   - Load built-in profiles from JAR (current behavior)
   - Load custom profiles from external directory
   - Precedence: external > JAR (allows overriding built-ins)
   ```

   **Recommended:** Option 3 (Hybrid) - Provides flexibility without requiring database infrastructure. Server admins can add custom profiles by dropping .dat files in mounted volume.

4. **Host Dashboard/Monitoring (S15):**

   **Current State:** Desktop host has full UI dashboard showing real-time game state (chip counts, hand progress, player actions, tournament clock, etc.). Server-hosted games run headless with no monitoring UI.

   **Implementation:**
   ```java
   // Add REST API endpoints to pokerserver:
   GET /api/games/{gameId}/state
   {
     "round": "flop",
     "pot": 1500,
     "players": [
       {"name": "Alice", "chips": 5000, "position": "BB", "status": "active"},
       {"name": "Bob", "chips": 3000, "position": "SB", "status": "folded"}
     ],
     "communityCards": ["AS", "KH", "QD"]
   }

   // TournamentDirector broadcasts state updates via OnlineMessage
   // PokerServer caches state and serves via REST API
   ```

   **Alternative:** Build admin web dashboard that consumes REST API for visual monitoring.

5. **Mid-Game Management (S16):**

   **Current State:** Desktop host can pause/resume, kick players, add/remove AI, adjust clock via GUI. Server-hosted has no management interface.

   **Implementation:**
   ```java
   // Add REST API endpoints:
   POST /api/games/{gameId}/pause
   POST /api/games/{gameId}/resume
   POST /api/games/{gameId}/kick/{playerId}
   POST /api/games/{gameId}/ai/add
   DELETE /api/games/{gameId}/ai/{aiId}
   PATCH /api/games/{gameId}/clock {"level": 5, "timeRemaining": 300}

   // TournamentDirector exposes management methods
   // PokerServer calls them via JMX or message queue
   ```

   **Security:** Require authentication, only game creator or admin can manage.

6. **Error Handling (S18):**

   **Current State:** TournamentDirector.run() catches exceptions and displays error dialog via SwingUtilities:
   ```java
   SwingUtilities.invokeLater(new Runnable() {
       public void run() {
           EngineUtils.displayInformationDialog(context_,
               Utils.fixHtmlTextFor15(PropertyConfig.getMessage("msg.tderror")));
           context_.restart();
       }
   });
   ```

   **Implementation:**
   ```java
   // Guard all UI calls in TournamentDirector:
   if (bHeadless_) {
       logger.error("Tournament error: " + e.getMessage(), e);
       // Send error message to all connected players
       onlineManager_.sendToAll(new OnlineMessage(CAT_ERROR, msg));
       // Optionally: POST error to REST API for monitoring
       notifyMonitoring(gameId, "error", e);
   } else {
       // Existing Swing dialog code
   }
   ```

   **Monitoring:** Errors posted to monitoring endpoint, admin dashboard shows alerts.

7. **Game Persistence (S19):**

   **Current State:** Desktop saves game state to local filesystem via `PokerDatabase.saveGame()`. Server-hosted in Docker has ephemeral filesystem.

   **Implementation Options:**

   **Option 1: Mounted Volume (simple)**
   ```yaml
   # docker-compose.yml
   volumes:
     - ./data/games:/var/ddpoker/games

   # ServerGameManager configures save path
   -Dddpoker.savePath=/var/ddpoker/games
   ```

   **Option 2: Database-Backed (robust)**
   ```java
   // New table: game_saves (game_id, save_data JSONB, timestamp)
   // PokerDatabase.saveGame() writes to database instead of file
   // PokerDatabase.loadGame() reads from database
   ```

   **Recommended:** Option 1 for MVP (simpler), Option 2 for production (more robust, supports backups/replication).

**Priority:**
- **S15 (Monitoring): High** - Critical for detecting and diagnosing issues with server-hosted games
- **S16 (Management): Medium** - Nice-to-have; can kill/restart process as workaround
- **S18 (Error Handling): High** - Critical for stability; errors must not crash headless process
- **S19 (Persistence): Medium** - Games can complete in single session; save/load is convenience
- **S17, S20, S21: Low** - Investigate during implementation; may work as-is or be non-critical

**Note:** S11, S13, S14 (AI enhancements) improve decision quality by 10-15% in specific scenarios but are not critical for MVP.

**See:**
- `.claude/plans/PHASE7B-V1-EXTRACTION.md` - V1 extraction plan
- `.claude/reviews/feature-v1-algorithm-extraction.md` - Complete review (approved)

---

## Context

Currently DDPoker uses a **hub-and-spoke P2P model** where one player (the "host") runs both the game client AND an embedded TCP server. All other players connect directly to the host's IP:port. This creates several problems:

- Host must have a publicly reachable IP (port forwarding / NAT traversal)
- Host client must stay online for the entire game duration
- Host bears all CPU/memory cost of running the game engine
- If host disconnects, the game dies for everyone

The goal is to allow the **server** to host games so clients all connect to the server instead of to each other. The server would support multiple concurrent games. This analysis assumes TCP conversion is already complete.

---

## Current Architecture (Key Points)

- **TournamentDirector** (`poker/.../online/TournamentDirector.java`) - Game engine running as a `Runnable` thread on the host. ~1000-line state machine in `_processTable()`. Extends `BasePhase` (requires `GameEngine`, `GameContext`). Uses `SwingUtilities` in error paths.
- **OnlineManager** (`poker/.../online/OnlineManager.java`) - Message routing hub. Depends on `PokerMain.getPokerMain()` singleton. Has `bHost_` flag. Routes all messages between players via `PokerConnectionServer`.
- **GameEngine** (`gameengine/.../GameEngine.java`) - **Static singleton** (`engine_` field). One per JVM. Entire framework assumes single game per process.
- **PokerMain** (`poker/.../PokerMain.java`) - Client entry point. Contains `PokerTCPServer` as private inner class. Has `bHeadless` constructor flag (partial headless support exists).
- **PokerServer** (`pokerserver/.../PokerServer.java`) - Central server (Spring, Docker). Currently only handles game registration/discovery, lobby chat, profiles, persistence. Does NOT depend on the `poker` (client) module.
- **Module dependencies**: `pokerserver` depends on `gameserver` + `pokernetwork`. The `poker` module depends on `gameengine` (Swing). These are intentionally separate.

---

## Target Architecture: Native Server Game Engine

### Concept
Extract game engine logic (TournamentDirector, OnlineManager, PokerGame) into a server-native module running directly in the PokerServer Spring process. No GameEngine, no GameContext, no BasePhase - pure game logic as Spring beans. All clients (desktop, web, mobile) connect to server and render UI based on server events.

**Status Update (2026-02-15):**
- ✅ pokergamecore module provides Swing-free game engine (TournamentEngine)
- ✅ V1 AI extraction complete, V2 AI extraction in progress
- ✅ Server-side AI validated and working
- 🎯 This is now the single target architecture (Options A & B removed from scope)

### What Changes
- **New module `pokergameengine`** - Extract pure game logic from `poker` module. New `ServerTournamentDirector` (not a `BasePhase`, just a `Runnable`). Uses `PokerGame`/`PokerTable`/`HoldemHand` from `pokerengine`.
- **New `ServerOnlineManager`** - No `PokerMain` dependency. Constructor-injected `PokerConnectionServer`. No "local player" concept. Re-implements message dispatch.
- **Refactor TD to remove UI coupling** - Replace `context_.processPhase()`, `SwingUtilities`, dialog calls with event dispatching that sends `OnlineMessage` to clients.
- **Spring-managed game registry** - `GameRegistry` singleton managing `Map<String, ServerGame>`, each with its own TD thread, OnlineManager, PokerGame, and player connections.

### Concurrent Games
- All games in same JVM, ~5-10MB per game (state only, no engine overhead)
- Hundreds of concurrent games feasible
- Shared thread pool for I/O

### Client Code Impact: **Small** (client side), **Large** (server side)
- Clients connect identically - don't know host is the server
- Server gains significant new code
- `pokerserver` gains dependency on `pokerengine` (already exists transitively)
- Must NOT add dependency on `poker` or `gameengine` (Swing)

### Benefits
- Most memory-efficient (~5-10MB per game)
- Best scalability (hundreds of concurrent games)
- Clean separation of concerns
- No Swing/AWT on server
- Enables multiple client types (desktop, web, mobile)

### Challenges
- Largest implementation effort
- Must deeply understand every TournamentDirector code path
- Risk of divergence between desktop and server game logic (mitigated by shared pokergamecore module)

---

## Feature Parity Roadmap: Path to Removing Desktop-Hosted Mode

**Goal:** Achieve full feature parity between server-hosted and desktop-hosted games, then deprecate and remove desktop hosting code.

### Phase 1: Resolve Critical Limitations (S11-S21)

All documented server limitations must be resolved before desktop hosting can be removed:

**High Priority (Blockers):**
- ✅ **S11:** Action history tracking for limper detection
- ✅ **S13:** Opponent profiling system
- ✅ **S15:** Host monitoring dashboard/API
- ✅ **S18:** Headless error handling (no UI dialogs)

**Medium Priority (Important):**
- ✅ **S14:** Custom AI personality upload/management
- ✅ **S16:** Mid-game management API (pause, kick, AI control)
- ✅ **S19:** Game persistence (save/load)
- ✅ **S20:** Hand history export/download

**Low Priority (Investigate):**
- ⚠️ **S17:** Observer/spectator mode compatibility
- ⚠️ **S21:** Full tournament configuration via API

### Phase 2: Server-Side Feature Additions

Beyond resolving gaps, server must gain new capabilities that desktop never had:

**Game Management:**
```java
// New REST API endpoints:
POST   /api/games/create              // Create new game
GET    /api/games                     // List available games
GET    /api/games/{id}                // Get game details
POST   /api/games/{id}/join           // Join game
DELETE /api/games/{id}/leave          // Leave game
POST   /api/games/{id}/start          // Start game (creator only)
GET    /api/games/{id}/state          // Real-time game state
POST   /api/games/{id}/action         // Submit player action
```

**Tournament Scheduling:**
```java
// Scheduled tournaments (desktop never supported this):
POST   /api/tournaments/schedule      // Schedule future tournament
GET    /api/tournaments/upcoming      // List scheduled events
POST   /api/tournaments/{id}/register // Pre-register for tournament
```

**User Profiles & Authentication:**
```java
// Desktop has local profiles; server needs account system:
POST   /api/auth/register
POST   /api/auth/login
GET    /api/users/{id}/profile
GET    /api/users/{id}/statistics     // Win/loss, hands played, etc.
GET    /api/users/{id}/history        // Hand history across all games
```

**Matchmaking & Lobby:**
```java
// Desktop has manual IP entry; server has lobby:
GET    /api/lobby/games               // Active games looking for players
GET    /api/lobby/players             // Players looking for games
POST   /api/lobby/quick-match         // Auto-match players
```

### Phase 3: Client Refactoring (Remove Game Logic)

Once server achieves feature parity, refactor desktop client to remove all game engine code:

**Code to Remove from `poker` module:**

```java
// Tournament management (moves to server):
- TournamentDirector.java              (~1000 lines)
- OnlineManager.java (host mode)       (~500 lines host-specific)
- PokerTCPServer.java                  (entire inner class)

// Game engine coupling:
- All GameContext/GameEngine references
- All BasePhase subclasses that run game logic
- PokerGame/PokerTable/HoldemHand mutation logic

// AI (already extracted to pokergamecore, but remove from client):
- V1Player.java, V2Player.java         (keep for testing, remove from production)
- RuleEngine.java                       (already duplicated in pokergamecore)

// P2P networking:
- PokerConnectionServer.java (listen mode)
- Direct player-to-player message routing
```

**Code to Keep/Refactor in `poker` module:**

```java
// Pure UI rendering:
- Dashboard panels (MyHand, MyTable, Odds, etc.)
- Card/chip rendering
- Table visualization
- Chat UI

// Client-side state (from server events):
- Read-only PokerGame/PokerTable/HoldemHand (built from server state)
- Local player action UI (bet slider, buttons)
- Sound effects, animations

// Networking (client mode only):
- PokerConnection.java (client side)
- OnlineMessage handling (receive only)
- Server connection management
```

**New Client Architecture:**

```
┌─────────────────────────────────────────────┐
│ Desktop Client (poker module)              │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ UI Layer (Swing)                    │   │
│  │  - Dashboards, tables, cards        │   │
│  │  - Player action input              │   │
│  │  - Animations, sounds               │   │
│  └──────────────┬──────────────────────┘   │
│                 │                           │
│  ┌──────────────▼──────────────────────┐   │
│  │ Event Handlers                      │   │
│  │  - Render game state from server    │   │
│  │  - Send player actions to server    │   │
│  │  - Update UI on server events       │   │
│  └──────────────┬──────────────────────┘   │
│                 │                           │
│  ┌──────────────▼──────────────────────┐   │
│  │ Server Connection (WebSocket/TCP)   │   │
│  │  - Subscribe to game events         │   │
│  │  - Send action commands             │   │
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│ Server (pokerserver module)                │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ REST API / WebSocket                │   │
│  │  - Game CRUD, player actions        │   │
│  │  - Real-time event streaming        │   │
│  └──────────────┬──────────────────────┘   │
│                 │                           │
│  ┌──────────────▼──────────────────────┐   │
│  │ Game Engine (pokergamecore)        │   │
│  │  - TournamentEngine                 │   │
│  │  - V1Algorithm, V2Algorithm AI      │   │
│  │  - Hand evaluation, betting logic   │   │
│  └──────────────┬──────────────────────┘   │
│                 │                           │
│  ┌──────────────▼──────────────────────┐   │
│  │ Persistence (Database)              │   │
│  │  - Game state, hand history         │   │
│  │  - User profiles, statistics        │   │
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

### Phase 4: Cutover (No Migration Needed)

**Note:** Since there are no users yet, backward compatibility is not a concern. Desktop-hosted mode can be removed immediately once server-hosted achieves feature parity.

**Cutover Plan:**
1. Complete Phases 1-3 (server feature parity + client refactor)
2. Remove desktop hosting code from client (TournamentDirector, PokerTCPServer, etc.)
3. Release as single mode: server-hosted only
4. No migration path needed
5. No dual-mode support needed

**Protocol Changes:**
- Can break/improve network protocol without backward compatibility concerns
- Can redesign OnlineMessage format for better efficiency/security
- Can switch from TCP to WebSocket without supporting both

**Simplified Timeline:**
- Build server-hosted features incrementally
- Test with internal users (no production traffic to worry about)
- When ready, switch client to server-only mode
- Remove desktop hosting code in same release

### Estimated Effort

| Phase | Work | Effort |
|-------|------|--------|
| Phase 1: Resolve S11-S21 | Implement all server limitations | 40-60h |
| Phase 2: Server features | API, scheduling, matchmaking, profiles | 60-80h |
| Phase 3: Client refactor | Remove game logic, event-driven UI | 40-60h |
| Phase 4: Cutover | Remove desktop hosting code (no migration needed) | 2-4h |
| **Total** | **Full feature parity + client refactor** | **142-204h** |

**Note:** No backward compatibility burden since there are no production users. This reduces complexity and allows protocol improvements without legacy support.

### Benefits of Pure Client-Server

1. **No NAT/firewall issues** - All connections to server, no P2P
2. **Games survive disconnects** - Server keeps running
3. **Better matchmaking** - Central lobby, quick-match
4. **Scheduled tournaments** - Pre-register, auto-start
5. **Cross-game statistics** - Track player performance over time
6. **Better anti-cheat** - Server validates all actions
7. **Easier updates** - Update server, all clients benefit
8. **Multiple client types** - Desktop, web, mobile all use same API
9. **Simpler client code** - No game engine complexity
10. **Better testing** - Server logic independent of UI

---

## Web Client Implementation

**Goal:** Browser-based poker client using same server API as desktop client.

### Architecture

```
┌─────────────────────────────────────────────┐
│ Web Client (Browser)                       │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ UI Layer (React/Vue/Svelte)         │   │
│  │  - Table view (SVG/Canvas)          │   │
│  │  - Card rendering                   │   │
│  │  - Bet slider, action buttons       │   │
│  │  - Chat, lobby, hand history        │   │
│  └──────────────┬──────────────────────┘   │
│                 │                           │
│  ┌──────────────▼──────────────────────┐   │
│  │ State Management (Redux/Zustand)    │   │
│  │  - Game state from server events    │   │
│  │  - Player actions                   │   │
│  │  - UI state (animations, etc)       │   │
│  └──────────────┬──────────────────────┘   │
│                 │                           │
│  ┌──────────────▼──────────────────────┐   │
│  │ API Client (WebSocket + REST)       │   │
│  │  - Subscribe to game events         │   │
│  │  - Send player actions              │   │
│  │  - Fetch game/user data             │   │
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
                    │
                    │ HTTPS/WSS
                    │
┌───────────────────▼─────────────────────────┐
│ Server (pokerserver)                       │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ REST API + WebSocket                │   │
│  │  - Same API for desktop & web       │   │
│  │  - Real-time game events            │   │
│  │  - Authentication, authorization    │   │
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

### Technology Stack (Web Client)

**Frontend Framework:**
- **React** (recommended) - Large ecosystem, good for complex UI
- **Vue** - Simpler learning curve, lighter weight
- **Svelte** - Excellent performance, less boilerplate

**State Management:**
- **Redux Toolkit** (React) - Standard for complex state
- **Zustand** (React) - Simpler alternative to Redux
- **Pinia** (Vue) - Official Vue state management

**UI Components:**
- **Custom SVG/Canvas** for table/cards (match desktop look)
- **Tailwind CSS** for styling
- **Framer Motion** or **React Spring** for animations

**Networking:**
- **WebSocket** for real-time game events (via Socket.IO or native WebSocket)
- **Fetch API** or **Axios** for REST calls
- **React Query** or **SWR** for data fetching/caching

**Build Tools:**
- **Vite** (fast dev server, modern bundler)
- **TypeScript** (type safety for API contracts)

### API Requirements for Web Client

The server API must support:

**WebSocket Events (Real-time):**
```typescript
// Client → Server
{
  type: 'game.action',
  gameId: 'uuid',
  action: { type: 'RAISE', amount: 100 }
}

{
  type: 'chat.message',
  gameId: 'uuid',
  message: 'gg'
}

// Server → Client
{
  type: 'game.state.update',
  gameId: 'uuid',
  state: {
    round: 'flop',
    pot: 1500,
    players: [...],
    communityCards: [...]
  }
}

{
  type: 'game.action.broadcast',
  gameId: 'uuid',
  player: 'Alice',
  action: { type: 'RAISE', amount: 100 }
}

{
  type: 'chat.message',
  gameId: 'uuid',
  from: 'Bob',
  message: 'gg'
}
```

**REST Endpoints (CRUD):**
```typescript
// Authentication
POST   /api/auth/register        { username, email, password }
POST   /api/auth/login           { username, password } → { token }
POST   /api/auth/logout          { token }

// Games
GET    /api/games                → [{ id, name, status, players }]
POST   /api/games                { tournament_config } → { id, join_url }
GET    /api/games/{id}           → { game details }
POST   /api/games/{id}/join      { token } → { success }
DELETE /api/games/{id}/leave     { token }
GET    /api/games/{id}/state     → { current game state }

// Tournaments
GET    /api/tournaments/upcoming → [{ id, name, start_time }]
POST   /api/tournaments/{id}/register

// Users
GET    /api/users/{id}/profile   → { username, avatar, stats }
GET    /api/users/{id}/history   → [{ game_id, result, hands }]

// Hand History
GET    /api/games/{id}/history   → [{ hand_number, actions, result }]
GET    /api/users/{id}/hands     → [{ game, hand, cards, outcome }]
```

### Web Client Implementation Phases

**Phase 1: Core Game UI (30-40h)**
- Table rendering (SVG poker table)
- Card rendering (SVG card images)
- Player positions, chip counts, bet amounts
- Action buttons (fold, call, raise with slider)
- Community cards, pot display

**Phase 2: Game Flow (20-30h)**
- WebSocket connection to server
- Receive and render game state updates
- Send player actions to server
- Animation: dealing cards, chips moving to pot
- Sound effects (bet, fold, win)

**Phase 3: Lobby & Matchmaking (15-20h)**
- Game list (available games)
- Create game UI (tournament configuration)
- Join game flow
- Quick-match button

**Phase 4: User Features (20-30h)**
- Login/registration
- User profile page
- Statistics dashboard
- Hand history viewer
- Settings (sound, animations, avatar)

**Phase 5: Chat & Social (10-15h)**
- In-game chat
- Lobby chat
- Emoji/reactions
- Player blocking/muting

**Phase 6: Polish & Responsive (15-20h)**
- Mobile responsive design
- Touch controls (drag slider on mobile)
- PWA support (install as app)
- Accessibility (keyboard controls, screen reader)
- Cross-browser testing

**Total Effort: 110-155h**

### Desktop vs Web Feature Parity

| Feature | Desktop (Swing) | Web (Browser) | Notes |
|---------|-----------------|---------------|-------|
| Join game | ✅ | ✅ | Same API |
| Play hands | ✅ | ✅ | Same game logic |
| Chat | ✅ | ✅ | Same WebSocket |
| Hand history | ✅ | ✅ | Same data |
| Statistics | ✅ | ✅ | Same backend |
| Create game | ✅ | ✅ | Same tournament config |
| AI personalities | ✅ | ✅ | Server-side AI |
| Offline play | ✅ | ❌ | Desktop can run local AI games |
| Native performance | ✅ | ⚠️ | Browser is slower but acceptable |
| Install required | ✅ | ❌ | Web is instant access |
| Cross-platform | ⚠️ | ✅ | Desktop is Java (works everywhere) but requires install; Web works everywhere with zero install |

**Advantages of Web Client:**
- **Zero install** - Play from any browser, no download
- **Auto-updates** - Server update = instant client update
- **Mobile-friendly** - Works on tablets/phones
- **Easier onboarding** - Share URL, instant play
- **Lower barrier to entry** - No Java runtime required

**Advantages of Desktop Client:**
- **Offline play** - Can play vs AI without server
- **Richer UI** - Full Swing capabilities, more complex UI possible
- **Better performance** - Native code is faster
- **Existing codebase** - Already built, tested, polished

**Recommended Strategy:**
1. Build server API with both clients in mind (RESTful + WebSocket)
2. Desktop client continues to work (refactored to use server API)
3. Web client built in parallel as secondary client
4. Both clients supported long-term (different use cases)
5. Eventually web client may become primary (easier onboarding)

---

## Local Server Embedding (Desktop Client Only)

**Goal:** Allow desktop client to optionally run the server locally for offline/LAN play while maintaining the same client architecture.

### Architecture

Desktop client supports three server connection modes:

```
┌─────────────────────────────────────────────┐
│ Mode 1: Remote Server (default)            │
│                                             │
│  Desktop Client                             │
│       │                                     │
│       │ HTTPS/WSS                           │
│       └──────────────────────────────►      │
│                                             │
│  Remote Server (ddpoker.com)                │
│   - Matchmaking, lobby                      │
│   - Scheduled tournaments                   │
│   - Cross-game statistics                   │
│   - Always online                           │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│ Mode 2: Local Embedded Server              │
│                                             │
│  Desktop Client                             │
│       │                                     │
│       │ launches embedded server            │
│       ▼                                     │
│  Embedded Server (localhost:9000)           │
│   - Same pokerserver code                   │
│   - Runs in-process or subprocess           │
│       ▲                                     │
│       │ localhost connection                │
│       │                                     │
│  Desktop Client connects to own server      │
│                                             │
│  Features:                                  │
│   - Offline play (vs AI)                    │
│   - LAN play (friends connect)              │
│   - No internet required                    │
│   - Full privacy (data local)               │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│ Mode 3: LAN Server Discovery               │
│                                             │
│  Desktop Client                             │
│       │                                     │
│       │ mDNS/Bonjour discovery              │
│       │                                     │
│  Discovers:                                 │
│   - Bob's Server (192.168.1.5:9000)         │
│   - Alice's Server (192.168.1.8:9000)       │
│       │                                     │
│       │ select and connect                  │
│       └──────────────────────────────►      │
│                                             │
│  Features:                                  │
│   - Join friend's local game                │
│   - No internet required                    │
│   - LAN-only (local network)                │
└─────────────────────────────────────────────┘
```

### Implementation: Hybrid Approach

Desktop client supports both embedded (in-process) and subprocess server modes. User chooses in settings; default is embedded for simplicity.

```java
public class PokerDesktopClient {
    private LocalServerEngine localServer;
    private ServerConnection serverConnection;

    public enum LocalServerMode {
        EMBEDDED,    // In-process (default, simpler)
        SUBPROCESS   // Separate JVM (advanced, more isolated)
    }

    public void hostLocalGame(TournamentConfig config, LocalServerMode mode) {
        localServer = (mode == LocalServerMode.EMBEDDED)
            ? new EmbeddedServerEngine()
            : new SubprocessServerEngine();

        // Configure and start
        localServer.configure(config);
        int port = localServer.start();  // Returns assigned port

        // Wait for server ready
        waitForServerHealthCheck("localhost", port);

        // Connect to own server (same API as remote)
        serverConnection = connectToServer("localhost", port);

        // Publish on LAN via mDNS (optional)
        if (config.allowLANJoin()) {
            LANDiscovery.publish(config.getGameName(), port);
        }
    }

    public void joinRemoteGame(String host, int port) {
        // Connect to remote server (same API)
        serverConnection = connectToServer(host, port);
    }

    public void discoverLANGames() {
        List<LANGameInfo> games = LANDiscovery.discover();
        showLANGamesDialog(games);  // Show in UI lobby
    }

    @Override
    public void shutdown() {
        if (localServer != null) {
            localServer.stop();
        }
        if (serverConnection != null) {
            serverConnection.close();
        }
    }
}

// Interface for local server engines
interface LocalServerEngine {
    void configure(TournamentConfig config);
    int start();  // Returns port
    void stop();
}

// Embedded: runs in same JVM
class EmbeddedServerEngine implements LocalServerEngine {
    private PokerServerApplication server;

    @Override
    public int start() {
        // Start Spring Boot server in background thread
        server = new PokerServerApplication();
        server.setEmbeddedMode(true);
        server.startOnPort(findAvailablePort());
        return server.getPort();
    }

    @Override
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }
}

// Subprocess: runs as separate JVM
class SubprocessServerEngine implements LocalServerEngine {
    private Process process;
    private int port;

    @Override
    public int start() {
        port = findAvailablePort();
        Path configFile = writeTempConfig(config);

        ProcessBuilder pb = new ProcessBuilder(
            "java", "-Xmx512m", "-jar", "lib/ddpoker-server.jar",
            "--port", String.valueOf(port),
            "--config", configFile.toString(),
            "--embedded"
        );
        process = pb.start();

        // Monitor server output
        monitorProcessOutput(process);

        return port;
    }

    @Override
    public void stop() {
        if (process != null && process.isAlive()) {
            process.destroy();
            process.waitFor(5, TimeUnit.SECONDS);
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
```

**Mode Selection (User Settings):**

```
┌────────────────────────────────────┐
│  Settings > Local Server           │
├────────────────────────────────────┤
│                                    │
│  Local Server Mode:                │
│                                    │
│  ◉ Embedded (Recommended)          │
│    Run server in same process      │
│    • Lower memory usage            │
│    • Simpler setup                 │
│    • Client crash = server crash   │
│                                    │
│  ○ Subprocess (Advanced)           │
│    Run server as separate process  │
│    • Process isolation             │
│    • Higher memory usage (~100MB)  │
│    • More stable (independent)     │
│                                    │
│  Default Port: [9000]              │
│  Auto-discover on LAN: ☑           │
│                                    │
│  [Save]  [Cancel]                  │
└────────────────────────────────────┘
```

**Why Hybrid?**

| Aspect | Embedded | Subprocess | Winner |
|--------|----------|------------|--------|
| Memory usage | Lower (~50MB total) | Higher (~150MB total) | Embedded |
| Setup complexity | Very simple | Simple | Embedded |
| Stability | Client crash = server crash | Independent processes | Subprocess |
| Debugging | Easier (single process) | Harder (IPC) | Embedded |
| Resource limits | Shared JVM heap | Separate heaps | Subprocess |
| Deployment | Single JAR | Two JARs | Embedded |

**Recommendation:**
- **Default: Embedded** - Simpler, lower overhead, works for 95% of use cases
- **Advanced: Subprocess** - For users who want process isolation or have stability concerns
- Both use same `pokerserver` codebase, just different deployment

### User Experience

**Main Menu:**

```
┌────────────────────────────────────┐
│        DD Poker                    │
├────────────────────────────────────┤
│                                    │
│  ┌──────────────────────────────┐ │
│  │  [Play Online]               │ │  ← Default: ddpoker.com
│  └──────────────────────────────┘ │
│                                    │
│  ┌──────────────────────────────┐ │
│  │  [Host Local Game]           │ │
│  │    • Play Offline (AI only)  │ │  ← Embedded server
│  │    • Open to LAN             │ │
│  └──────────────────────────────┘ │
│                                    │
│  ┌──────────────────────────────┐ │
│  │  [Join LAN Game]             │ │  ← mDNS discovery
│  │                              │ │
│  │    Bob's Game (192.168.1.5)  │ │
│  │    Alice's Game (.1.8)       │ │
│  │                              │ │
│  └──────────────────────────────┘ │
│                                    │
│  [Options]  [Quit]                 │
└────────────────────────────────────┘
```

**Local Game Hosting UI:**

```
┌────────────────────────────────────┐
│  Hosting Local Game                │
├────────────────────────────────────┤
│                                    │
│  Server Status: ● Running          │
│  Address: localhost:9000           │
│  LAN Address: 192.168.1.10:9000    │
│                                    │
│  Players Connected:                │
│    • You (host)                    │
│    • Alice (192.168.1.5)           │
│    • Bob (192.168.1.8)             │
│                                    │
│  Tournament: $1000 NL Hold'em      │
│  Status: Waiting for players       │
│                                    │
│  ┌──────────────────────────────┐ │
│  │  [Start Tournament]          │ │
│  └──────────────────────────────┘ │
│                                    │
│  ☐ Visible on LAN (mDNS)           │
│  ☑ Allow AI players                │
│                                    │
│  [Stop Server]                     │
└────────────────────────────────────┘
```

### Feature Matrix

| Feature | Remote Server | Local Embedded | LAN Server |
|---------|---------------|----------------|------------|
| Internet required | ✅ | ❌ | ❌ |
| Offline play (AI) | ❌ | ✅ | ✅ |
| Matchmaking | ✅ | ❌ | ❌ |
| Scheduled tournaments | ✅ | ❌ | ❌ |
| Cross-game statistics | ✅ | ✅ (local DB) | ✅ (local DB) |
| Friends can join | ✅ (anywhere) | ✅ (port forward) | ✅ (LAN only) |
| Game survives host DC | ✅ | ❌ | ❌ |
| Privacy (data local) | ❌ | ✅ | ✅ |
| No hosting cost | ❌ (server cost) | ✅ | ✅ |
| Setup complexity | Low | Very Low | Low |

### Implementation Effort

After server-side architecture is complete:

| Task | Effort | Notes |
|------|--------|-------|
| Embedded server engine wrapper | 5-8h | Adapt pokerserver for in-process use |
| Subprocess launcher | 2-3h | Launch server JAR, monitor lifecycle |
| LAN discovery (mDNS/Bonjour) | 3-5h | Publish/discover local games |
| Server mode UI (main menu) | 2-3h | Remote vs Local vs LAN options |
| Local game hosting UI | 2-3h | Server status, player list, controls |
| Configuration sync | 2-3h | Pass tournament config to local server |
| Testing (LAN scenarios) | 5-8h | Multi-client on same network |
| **Total** | **21-33h** | Restore offline/LAN capabilities |

### Benefits

1. **Best of both worlds:**
   - Online play: Central server, matchmaking, persistence
   - Offline play: No internet, AI practice, full privacy
   - LAN play: Friend's house, low latency, no internet

2. **Zero regression:**
   - Desktop users keep current offline functionality
   - Same architecture as remote (client-server)
   - No need for old P2P hosting code

3. **User flexibility:**
   - Choose remote server (easier, more features)
   - Choose local server (offline, privacy)
   - Choose based on context (home vs travel)

4. **Development benefits:**
   - Full stack testing locally
   - No dependency on remote server for development
   - Easier debugging (attach to local server)

### Implementation Timeline

**After server-side feature parity achieved:**

1. **Server API stabilized** (Phase 1-3 complete)
2. **Web client built** (validates API design)
3. **Desktop client refactored** (thin client using server API)
4. **Add local server embedding** (19-30h effort)
5. **Release:** Desktop supports both remote and local modes

**Result:**
- Web client: Remote server only (browser-based)
- Desktop client: Remote OR local (maximum flexibility)
- All use same server codebase (pokerserver module)

### Technical Notes

**Server Configuration Differences:**

```java
// Remote server config (production)
server.features.matchmaking=true
server.features.scheduled_tournaments=true
server.features.statistics=true
server.persistence.type=postgresql
server.security.auth_required=true

// Local embedded config
server.features.matchmaking=false       // No matchmaking offline
server.features.scheduled_tournaments=false
server.features.statistics=true         // Local SQLite DB
server.persistence.type=sqlite
server.persistence.path=~/.ddpoker/games.db
server.security.auth_required=false     // No auth for local
server.networking.bind=0.0.0.0          // Allow LAN connections
```

**Port Management:**

```java
// Avoid port conflicts
int port = findAvailablePort(9000, 9099);
if (port == -1) {
    throw new IOException("No available ports in range 9000-9099");
}
embeddedServer.startOnPort(port);
```

**LAN Discovery (mDNS):**

```java
// Publish using JmDNS library
ServiceInfo serviceInfo = ServiceInfo.create(
    "_ddpoker._tcp.local.",
    "My Poker Game",
    port,
    "path=/"
);
JmDNS.create().registerService(serviceInfo);

// Discover
JmDNS jmdns = JmDNS.create();
jmdns.addServiceListener("_ddpoker._tcp.local.", new ServiceListener() {
    @Override
    public void serviceAdded(ServiceEvent event) {
        // Found game: event.getInfo()
    }
});
```

---

## Implementation Summary

### Total Effort Breakdown

| Component | Effort | Priority |
|-----------|--------|----------|
| **Server-Side (Native Engine)** |
| Resolve S11-S21 limitations | 40-60h | High |
| Server features (API, matchmaking, etc.) | 60-80h | High |
| Client refactor (remove game logic) | 40-60h | High |
| Cutover (remove desktop hosting) | 2-4h | High |
| **Subtotal: Server-side** | **142-204h** | |
| **Web Client** |
| Core game UI | 30-40h | Medium |
| Game flow (WebSocket) | 20-30h | Medium |
| Lobby & matchmaking | 15-20h | Medium |
| User features | 20-30h | Medium |
| Chat & social | 10-15h | Low |
| Polish & responsive | 15-20h | Low |
| **Subtotal: Web client** | **110-155h** | |
| **Local Server Embedding** |
| Embedded server + subprocess | 7-11h | Medium |
| LAN discovery (mDNS) | 3-5h | Medium |
| UI for server modes | 4-6h | Medium |
| Testing | 5-8h | Medium |
| **Subtotal: Local embedding** | **19-30h** | |
| **Grand Total** | **271-389h** | |

### Implementation Order

1. **Server-side architecture** (142-204h) - Foundation for everything
2. **Web client** (110-155h) - Validates API design, provides web access
3. **Local embedding** (19-30h) - Restores desktop offline/LAN functionality

**No backward compatibility burden** - Can iterate freely, no production users yet.

### Success Criteria

- ✅ All games run on server (remote or local embedded)
- ✅ Desktop client is pure UI renderer
- ✅ Web client provides browser-based access
- ✅ Desktop client supports offline/LAN via local server
- ✅ All S11-S21 limitations resolved
- ✅ No P2P hosting code remains in client
- ✅ Protocol can be improved without legacy support
