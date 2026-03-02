# Client Parity Design — Desktop ↔ Web ↔ Central Server

**Created:** 2026-03-02
**Status:** Approved

---

## Goals

1. **Cross-play:** Java desktop client and web client can host and join the same online game and play together.
2. **Practice parity:** Both clients support solo practice games against AI (desktop offline via embedded server; web via central server).
3. **Feature parity:** Features that exist in the Java desktop client are available in the web client. Web-only features (simulator, advisor, charts) do not need to be mirrored on the desktop.

---

## Architecture Principles

### Central server is the only online host

For all online multiplayer games, both clients are consumers of the central game server (`pokergameserver`). Neither client runs infrastructure for online play.

```
Central Game Server (pokergameserver)
       │
       ├── REST API (/api/v1/*)
       │     ├── Web client (Next.js)
       │     └── Desktop client (Java Swing)
       │
       └── WebSocket (/ws/games/{gameId})
             ├── Web client (browser WebSocket)
             └── Desktop client (WebSocketGameClient)
```

### Embedded server is practice-only

The desktop client's `EmbeddedGameServer` runs the game engine locally for offline solo practice. It is never exposed to the network and is never used for online multiplayer. Community hosting (embedded server as external host) is deferred — not in scope.

```
EmbeddedGameServer  →  practice mode only (local, offline, no account required)
Central server      →  online mode only (multiplayer, authenticated)
```

### Tournament profiles are server-side for online play

Tournament profile templates belong to the user account, not the device. Both clients read and write templates via the central server's REST API (`/api/profile/templates`) when creating online games. For practice mode, the desktop uses local profiles (offline, no server required).

---

## Desktop Online Flow (Target State)

```
OnlineMenu
  ├── Host → TournamentOptions → [REST: createGame] → Lobby.Host → HostStart → game
  └── Join → FindGames → [REST: joinGame] → Lobby.Player → game
```

### What changes from current state

**`OnlineConfiguration.java` is gutted.** Its current job — start embedded server, detect public IP, expose ports, register with game list — is entirely wrong for this architecture. It becomes a simple Create Game confirmation screen: user selects a tournament profile (from server-side templates), clicks Create, REST `POST /api/v1/games` is called, and the flow navigates to `Lobby.Host`.

**`Lobby.Host`** connects to the central server WebSocket for the created game (same URL pattern as any joining player, with creator permissions). REST polling for the player list hits the central server, not localhost.

**JWT source** in the join and host flows must come from the logged-in user's central server session token — not from `EmbeddedGameServer.getLocalUserJwt()`, which is only valid for the embedded practice server.

**Login step** must be added before the user can enter the online flow. The user must be authenticated against the central server before creating or joining a game.

### What is unchanged

- `FindGames.java` → `Lobby.Player` structural flow (phase name bug fixed 2026-03-02)
- `WebSocketGameClient` — already implements the correct protocol for the central server
- `EmbeddedGameServer` — untouched, practice mode only
- `WebSocketTournamentDirector` — handles server messages for active gameplay, no changes needed

---

## Feature Gap Analysis

Gaps are assessed only in one direction: **features the Java desktop client has that the web client is missing.** Web-only features do not represent gaps.

### Tier 1 — Blocking cross-play

| Gap | Detail |
|-----|--------|
| No login step in desktop online flow | User has no authenticated central server session |
| Wrong JWT source in join/host flows | Uses embedded server's local JWT instead of user's central server token |
| `OnlineConfiguration` starts embedded server | Must be replaced with REST `createGame` to central server |
| `Lobby.Host` connects to embedded server WebSocket | Must connect to central server WebSocket |
| Full join path unverified | `Lobby.Player` → `InitializeOnlineGame` → `BeginOnlineGame` → gameplay → results never tested end-to-end |

### Tier 2 — Active plans ready to implement

| Desktop feature | Web client status |
|-----------------|-------------------|
| AI show winning hand (tournament-level setting) | Missing — desktop is adding it, web UI has no equivalent toggle |
| Tournament templates server-synced on desktop | Desktop uses local-only profiles; online flow must use server-side templates |
| Lobby chat panel in `FindGames` screen | Plan written, not yet implemented on desktop |
| Profile password validation on online profile switch | Plan written, not yet implemented on desktop |

Note: Lobby chat and profile password validation are desktop gaps (web already has equivalent), not web gaps.

### Tier 3 — Feature completeness verification

| Item | Detail |
|------|--------|
| Limit Hold'em selectable in web UI | Server supports it; web create-game form needs verification |
| Full tournament profile option coverage | Audit every desktop create-game option against web UI — some may be unexposed |

### Tier 4 — Major features (both clients)

| Feature | Detail |
|---------|--------|
| Omaha (PLO) | Neither client has it; shared `OmahaScorer` in `pokerengine`; full draft plan exists; 4–6 week effort |
| Game save/resume across server restarts | No plan yet; requires DB persistence of game state |

---

## Implementation Roadmap

### Phase 1 — Cross-Play Foundation
*Goal: desktop and web can host and join the same game and play a hand to completion.*

1. Add login step to desktop online flow
2. Fix JWT source in `FindGames` and online flow — use central server user token
3. Rework `OnlineConfiguration` — strip embedded server startup; replace with REST `createGame`
4. Fix `Lobby.Host` — connect to central server WebSocket
5. Audit and fix full join path (`Lobby.Player` → `InitializeOnlineGame` → `BeginOnlineGame` → gameplay → results)
6. End-to-end verification: web hosts, desktop joins, plays a complete hand
7. End-to-end verification: desktop creates game, web joins, plays a complete hand

### Phase 2 — Online Flow Polish
*Goal: close active gaps while cross-play is solid.*

1. Tournament templates — desktop online flow reads/writes from server-side templates
2. AI show winning hand — tournament setting backend + UI toggle on desktop and web
3. Lobby chat panel on desktop `FindGames` (plan already written)
4. Profile password validation on desktop (plan already written)

### Phase 3 — Feature Completeness
*Goal: ensure no desktop features are silently missing from web.*

1. Audit tournament profile option coverage between desktop and web
2. Verify Limit Hold'em selectable in web game creation
3. Game save/resume across server restarts (new plan needed)

### Phase 4 — Omaha
*Goal: PLO support on both clients simultaneously.*

Full implementation per existing draft plan (`2026-02-10-omaha-implementation.md`). Shared core algorithm in `pokerengine`, server-side dealing/evaluation, both client UIs updated. Blocked on Phase 1 completion.

---

## Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Online host | Central server only | Eliminates NAT traversal, port forwarding, IP detection; simpler mental model |
| Embedded server role | Practice mode only | Desktop's core value is offline capability; removing it would make desktop strictly inferior to web |
| Community hosting | Deferred | Will be designed separately when ready; embedded server is the likely mechanism |
| Tournament templates | Server-side for online, local for practice | Templates belong to the user account, not the device; practice is offline |
| Parity direction | Desktop → Web only | Web-only features (simulator, advisor, charts) do not need desktop equivalents |

---

## Bug Fixed During Design

- **`FindGames.java` lines 198, 235** — `processPhase("Lobby")` → `processPhase("Lobby.Player")` (fixed 2026-03-02)
- **`OnlineConfiguration.java` line 318** — `processPhase("Lobby")` → `processPhase("Lobby.Host")` (fixed 2026-03-02)

Both fixes are committed. The phase `"Lobby"` does not exist in `gamedef.xml`; `GameContext` silently swallowed the error, leaving the user stuck on `FindGames` with no feedback after a successful REST join call.
