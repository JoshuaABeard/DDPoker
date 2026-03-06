# Thin Client Separation Design

**Status:** APPROVED (2026-03-06)

## Goal

Make both clients (web and Java desktop) true thin clients with no poker knowledge. All game logic, data persistence, and business rules live on the server. The Java desktop client may embed the server for practice mode but must not use server-side code outside of that bootstrap.

## Current State

The web client is already a clean thin client. The Java desktop client (`poker` module) has significant leakage:

- **45+ files** import from `pokerengine.model` (server-side JPA entities)
- **7 files** query an embedded H2 database directly via the `db` module
- **`EmbeddedGameServer`** directly accesses Spring beans (`AuthService`, `JwtTokenProvider`) instead of REST
- **`TournamentProfileConverter`** imported from `pokergameserver` into client code
- **Maven Enforcer** uses `searchTransitive=false`, allowing all engine classes through transitively
- **Wildcard imports** from `server.*` and `gameserver.*` in multiple files

## Design

### 1. Protocol Model Records

Create pure Jackson-annotated records in `pokergameprotocol` to replace direct use of `pokerengine.model` JPA entities by the client.

| New Protocol Record | Replaces | Key Fields |
|---|---|---|
| `TournamentProfileData` | `TournamentProfile` | name, buyin, buyinChips, seats, levels (list of `BlindLevelData`), payouts, rebuys, addons, online settings, AI settings, timeout/boot settings |
| `BlindLevelData` | per-level data in `TournamentProfile` | level, smallBlind, bigBlind, ante, minutes, isBreak, gameType |
| `OnlineProfileData` | `OnlineProfile` | id, name, email, createDate, retired |
| `OnlineGameData` | `OnlineGame` | id, url, hostPlayer, mode, tournamentName, hostingType, startDate, endDate |
| `TournamentHistoryData` | `TournamentHistory` | gameId, place, tournamentName, tournamentType, startDate, endDate, buyin, rebuys, addons, prize, numPlayers, numRemaining, ended, numChips |

Server-side mappers in `pokergameserver` convert between JPA entities and these protocol records. The client uses protocol records exclusively. No migration of legacy binary profile files — old profiles are not supported.

### 2. Hand History Server Migration

Move all hand history and tournament results storage from the client-side H2 database to the embedded game server.

#### New JPA Entities (pokergameserver)

| Entity | Key Columns | Purpose |
|---|---|---|
| `HandHistoryEntity` | handId, gameId, tableId, handNumber, gameStyle, gameType, startDate, endDate, ante, smallBlind, bigBlind, communityCards (JSON list), communityCardsDealt | One row per hand |
| `HandPlayerEntity` | handId, playerId, playerName, seatNumber, startChips, endChips, holeCards (JSON list), preflopActions, flopActions, turnActions, riverActions (bitflags), cardsExposed | One row per player per hand |
| `HandActionEntity` | handId, playerId, sequence, round, actionType, amount, subAmount, allIn | One row per action |

Persistence hook: `ServerHand.storeHandHistory()` (currently a no-op) gets implemented to persist all three entities at hand completion.

#### New Protocol DTOs (pokergameprotocol)

| DTO | Purpose |
|---|---|
| `HandSummaryData` | Hand list display: handId, handNumber, tableId, holeCards, communityCards, startDate |
| `HandDetailData` | Full hand reconstruction: all entity fields + list of `HandPlayerDetailData` + list of `HandActionDetailData` |
| `HandPlayerDetailData` | Player in a hand: name, seat, cards, chips, action flags |
| `HandActionDetailData` | Single action: player, round, type, amount, allIn |
| `HandStatsData` | Aggregated statistics: handClass, count, winPct, losePct, avgBet, flopPct, showdownPct, etc. |
| `HandExportData` | Export-ready hand data (replaces `ImpExpHand`) |

#### New REST Endpoints (pokergameserver)

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/games/{gameId}/hands` | Paginated hand list (returns `HandSummaryData[]`) |
| `GET` | `/api/v1/games/{gameId}/hands/count` | Hand count for pagination |
| `GET` | `/api/v1/games/{gameId}/hands/{handId}` | Full hand detail (returns `HandDetailData`) |
| `GET` | `/api/v1/games/{gameId}/hands/{handId}/html` | Server-rendered HTML hand history |
| `GET` | `/api/v1/games/{gameId}/stats` | Hand statistics by hand class and round (returns `HandStatsData[]`) |
| `GET` | `/api/v1/games/{gameId}/hands/export` | Export hands in standard format (returns `HandExportData[]`) |

#### Tournament Results Endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/history/stats` | Aggregated win/loss/ROI |
| `DELETE` | `/api/v1/history/{id}` | Delete tournament history entry |
| `DELETE` | `/api/v1/history` | Delete all tournament history |

Tournament finish persistence happens server-side when the game ends (server already has all the data). The existing `/api/v1/history` endpoint serves tournament history reads.

#### Client DB Removal

- `PokerDatabase.java` — deleted entirely
- `PokerDatabaseProcs.java` — hand class calculation moves server-side
- `DatabaseQueryTableModel.java` — replaced with REST-backed table models
- `db` dependency removed from `poker/pom.xml`
- H2 database files (`~/.ddpoker/db/`) no longer created

### 3. Auth Cleanup

Replace direct Spring bean access in `EmbeddedGameServer` with REST calls.

**Current flow:**
```
getLocalUserJwt()
  -> context.getBean(AuthService.class)
  -> authService.register(username, password, email)
  -> context.getBean(JwtTokenProvider.class)
  -> jwtProvider.generateToken(username, profileId, ...)
```

**New flow:**
```
getLocalUserJwt()
  -> POST http://localhost:{port}/api/v1/auth/register
     body: RegisterRequest(username, password, email)
  -> on conflict: POST /api/v1/auth/login
     body: LoginRequest(username, password, false)
  -> extract JWT from LoginResponse.token()
```

Eliminates `AuthService`, `JwtTokenProvider`, `JwtKeyManager` imports from the client module.

### 4. TournamentProfileConverter & Model Mapping

- `TournamentProfileConverter` stays in `pokergameserver` (server-side: JPA entity -> GameConfig)
- New `GameConfigBuilder` in `pokergameprotocol` converts `TournamentProfileData` -> `GameConfig`
- Client uses `GameConfigBuilder` exclusively
- No migration of legacy binary profile files (product not yet fielded)
- Client saves/loads tournament profiles as JSON `TournamentProfileData` via Jackson

### 5. Enforcer & Dependency Cleanup

**Maven Enforcer:**
- Change `searchTransitive` from `false` to `true` in `poker/pom.xml`
- Enabled after all other work is done

**Dependency removal from `poker/pom.xml`:**
- Remove `db` module
- Remove `spring-boot-starter-data-jpa`
- Remove `h2` database
- Remove `spring-boot-starter-mail`
- Keep `pokergameserver` (embedded server bootstrap)
- Keep `pokergameprotocol` (client-server contract)

**Wildcard import cleanup:**
- `Lobby.java` — remove unused `server.*` wildcard
- `OnlineConfiguration.java` — remove `gameserver.*` and `server.*` wildcards
- `PokerShowdownPanel.java` — remove `server.*` wildcard

**Legacy test files:**
- Move `HandInfoFasterTest.java` and `HandUtilsTest.java` from `poker/src/test/` to `pokerengine/src/test/`

### 6. Client File Impact Map

**Deleted entirely:**
- `PokerDatabase.java`
- `PokerDatabaseProcs.java`
- `DatabaseQueryTableModel.java`

**Rewritten to use REST:**
- `HandHistoryPanel.java` — calls `GameServerRestClient` instead of `PokerDatabase`
- `HandHistoryDialog.java` — passes REST client context instead of SQL WHERE clauses
- `StatisticsViewer.java` — fetches stats and tournament history via REST
- `GameInfoDialog.java` — fetches hand history via REST
- `HistoryExportDialog.java` — fetches export data via REST
- `PlayerProfile.java` — tournament history CRUD via REST

**Rewritten to use protocol records:**
- `TournamentProfileDialog.java` — uses `TournamentProfileData`
- `TournamentProfileFormatter.java` — formats `TournamentProfileData`
- `TournamentOptions.java` — edits `TournamentProfileData`
- `BlindQuickSetupDialog.java` — uses `BlindLevelData`
- `PrizePoolDialog.java` — uses `TournamentProfileData`
- `PokerGame.java` — holds `TournamentProfileData`
- `OnlineConfiguration.java` — uses `GameConfigBuilder` + protocol types
- `FindGames.java` / `ListGames.java` — uses `OnlineGameData`
- `PlayerProfileOptions.java` — uses `OnlineProfileData`
- `RestartTournament.java` — uses `TournamentProfileData`
- `TournamentSummaryPanel.java` — uses `TournamentHistoryData`

**Auth cleanup:**
- `EmbeddedGameServer.java` — REST auth instead of direct bean access

**Converter swap:**
- `GameServerRestClient.java` — uses `GameConfigBuilder`

**Wildcard cleanup:**
- `Lobby.java`, `OnlineConfiguration.java`, `PokerShowdownPanel.java`

**Moved to other modules:**
- `HandInfoFasterTest.java` -> `pokerengine/src/test/`
- `HandUtilsTest.java` -> `pokerengine/src/test/`

**Unchanged (acceptable):**
- `EmbeddedServerConfig.java` — Spring wiring for embedded server
- `PracticeGameLauncher.java` — already uses REST
- `GameSaveManager.java` — already uses REST
- `WebSocketGameClient.java` — already clean
- `WebSocketTournamentDirector.java` — already clean
- Dev/src files — dev profile code, acceptable to use server internals
