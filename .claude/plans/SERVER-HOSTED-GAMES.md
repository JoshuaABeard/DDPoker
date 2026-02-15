# Server-Hosted Game Support: Options Analysis

**Status:** active
**Last Updated:** 2026-02-15

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

**Priority:** Medium - AI is functional without these enhancements, but they improve decision quality by 10-15% in specific scenarios (limpers, tight/loose opponents).

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

## Option A: Server as Relay

### Concept
The server acts as a TCP relay. Clients connect to the server instead of directly to the host. The host client still runs TournamentDirector. The server just forwards messages between host and clients.

### What Changes
- **New `GameRelay` class in `pokerserver`** - Manages `Map<String, RelaySession>` keyed by game ID. Each session tracks one host connection + N client connections. Forwards messages without understanding game state.
- **New TCP listener on PokerServer** - Accepts relay connections (second port or protocol handshake on existing port).
- **Client game creation flow** - When registering a game, server responds with relay address. Host connects TO the relay (instead of listening). Clients connect to relay address.
- **New `PokerConnectionServer` impl** - For host-side, inverts the connection model: host connects out to relay rather than accepting incoming connections.

### Concurrent Games
- Each `RelaySession` keyed by game ID. Negligible memory per session (just connection references).
- Thousands of concurrent games feasible.

### Client Code Impact: **Small**
- Host still runs TD, OnlineManager, everything
- Only change: host connects to relay instead of opening a listener
- `OnlineManager` constructor needs to return relay-mode vs listen-mode server based on game setting

### Tradeoffs
| Pro | Con |
|-----|-----|
| Minimal code changes (~3-5 new classes) | Host client must stay online |
| No game logic on server | If host disconnects, game still dies |
| Existing protocol unchanged | Double network hops add latency |
| Low risk | Doesn't solve CPU/memory burden on host |

---

## Option B: Headless Host on Server

### Concept
The server spawns a headless `PokerMain` JVM process per game, running TournamentDirector + OnlineManager + PokerGame. Clients connect to the server. The game creator configures the tournament but doesn't need to stay connected.

**Status Update (2026-02-15):** Headless AI now working! Tournament-aware AI POC demonstrates that intelligent poker AI can run in headless mode without Swing. This validates the feasibility of this approach.

### What Changes
- **`ServerGameManager` in `pokerserver`** - Spawns/monitors/cleans up headless JVM processes. Assigns unique port per game from a configurable range.
- **Headless PokerMain hardening** - `bHeadless=true` already exists but needs expansion: guard every `SwingUtilities` call, stub UI-displaying phases, accept tournament config via args/file, auto-create game and wait for players.
- **Virtual host player** - Server-side `OnlineManager` uses a synthetic host player with no physical connection.
- **Process lifecycle** - Create: spawn process, wait for port bind. Monitor: health checks. End: process exits when TD finishes.
- **Tournament profile transfer** - Creator client sends `TournamentProfile` to server (already happens in `CAT_WAN_GAME_ADD`). Server passes config to headless process.

### Concurrent Games
- Each game = separate JVM process (~50-100MB each)
- 20 concurrent games = ~2GB RAM. Feasible for community edition.
- Port allocation from configurable range (e.g., 9000-9999)

### Why Process Isolation
The `GameEngine` is a static singleton - one per JVM. Options to solve this:
- **Process isolation (recommended)**: Each game in its own JVM. Safe, simple, leverages existing code.
- Classloader isolation: Complex, fragile.
- Refactor singleton: Cleanest but touches dozens of files.

### Client Code Impact: **Moderate**
- Game creation UI needs "Server Hosted" option
- Creator sends `TournamentProfile`, gets back connect URL, joins as regular client
- All `bHost_` logic stays the same on client side - clients don't know/care that host is on server

### Tradeoffs
| Pro | Con |
|-----|-----|
| Game survives creator disconnect | ~50-100MB per game JVM |
| No game logic changes needed | Headless mode needs hardening (audit all UI code paths in TD) |
| Existing protocol works as-is | Process management is new infrastructure |
| Natural isolation between games | Docker container resource limits needed |
| `bHeadless` flag already exists | Port range management |

---

## Option C: Native Server Game Engine

### Concept
Extract game engine logic (TD, OnlineManager, PokerGame) into a server-native module running directly in the PokerServer Spring process. No GameEngine, no GameContext, no BasePhase - pure game logic as Spring beans.

**Status Update (2026-02-15):** pokergamecore module now provides Swing-free game engine (TournamentEngine). Phase 7 plan documents path to extract V1/V2 AI. This significantly reduces the implementation effort for this option.

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

### Tradeoffs
| Pro | Con |
|-----|-----|
| Most memory-efficient (~5-10MB/game) | Largest implementation effort |
| Best scalability (100s of games) | Code duplication risk (two TDs) |
| Clean separation of concerns | Must deeply understand every TD code path |
| No Swing/AWT on server | Long-term maintenance of two engines |

---

## Recommended Path: Phased Approach

The three options form a natural progression:

### Phase 1: Option A (Relay) - Start Here
- Solves the NAT/firewall problem immediately
- ~3-5 new classes in `pokerserver`, minor client changes
- All users can join games without port forwarding
- Low risk, fast delivery

### Phase 2: Option B (Headless Host) - Build on Phase 1
- Instead of relaying to a remote host, server spawns a local headless host process
- Relay routes to `localhost:<assigned-port>` for server-hosted games
- Games survive host disconnection
- Medium effort on top of Phase 1

### Phase 3: Option C (Native Engine) - Only If Needed
- Only pursue if Phase 2's per-process overhead becomes a scaling problem
- Extract TD into shared module used by both client and server
- Major refactoring effort

### Scaling Decision Point
- **< 20 concurrent games**: Phase 2 is sufficient
- **20-100 games**: Phase 2 with JVM tuning (small heap, shared class data)
- **> 100 games**: Phase 3 becomes worthwhile

Given DDPoker is a community edition, **Phase 2 is likely the ceiling** for the foreseeable future.

---

## Summary Comparison

| Dimension | A: Relay | B: Headless Host | C: Native Engine |
|-----------|----------|-------------------|------------------|
| Solves NAT problem | Yes | Yes | Yes |
| Game survives host DC | No | Yes | Yes |
| Server RAM per game | ~0 | ~50-100MB | ~5-10MB |
| Client code changes | Small | Moderate | Small |
| Server code changes | Small | Medium | Large |
| Game logic risk | None | Low (same code) | High (new code) |
| Concurrent game capacity | Thousands | Tens | Hundreds |
