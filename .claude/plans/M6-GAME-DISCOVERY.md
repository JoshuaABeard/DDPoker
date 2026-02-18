# M6: Game Discovery & Management

**Status:** PLANNED
**Created:** 2026-02-17
**Parent Plan:** `.claude/plans/SERVER-HOSTED-GAME-ENGINE.md`
**Depends On:** M1 ✅, M2 ✅, M3 ✅, M4 ✅
**Effort:** L (2–3 weeks)

---

## Context

M1–M4 built a complete server-side game engine and replaced the desktop client's P2P plumbing with WebSocket. What's missing is the game lobby: how players discover games, how community hosts register their games, how owners manage games before they start, and how stale listings get cleaned up.

**Decisions (locked):**
- Single `game_instances` table for both SERVER and COMMUNITY game types — already has `hosting_type` column
- Community-hosted games register via REST (no legacy TCP `CAT_WAN_GAME_ADD`)
- REST for pre-game owner actions (start, settings, kick, cancel); WebSocket for mid-game runtime (already in M3)
- Private games visible in lobby with lock icon; password validated on join
- Web lobby UI (list, create, join pages) lives in M5; M6 delivers backend + desktop UI only
- No backward compatibility with `wan_game` table or legacy `api` module `GameController`
- **WAN server manages SERVER-hosted games only.** For COMMUNITY games, the WAN server's role is discovery metadata only (register, list, heartbeat, cancel listing). Lobby management (kick, settings, game-start broadcast) is handled by the community host's embedded server and is not the WAN server's concern.
- **COMMUNITY game owner operations supported from WAN:** `DELETE` (cancel listing) and `POST /start` (update DB status to IN_PROGRESS for accurate listing). All other owner operations (`kick`, `settings`) return `422 Not Applicable` for COMMUNITY games.
- **`POST /api/v1/games/{id}/join` is a gatekeeper only.** It validates the password and returns the WebSocket URL. It does NOT increment `playerCount`. Player count is updated when the WebSocket connects and `GameInstance.addPlayer()` fires.
- **`TournamentSummaryPanel` is replaced** with a new `GameInfoPanel` built around `GameSummary`. No adapter to the old `TournamentProfile` model. `GameInfoPanel` becomes the canonical game detail component used in the desktop lobby and referenced by M5 (web) for its game detail view.

---

## What Already Exists (Do Not Re-Implement)

| File | Status | Notes |
|------|--------|-------|
| `GameInstanceEntity` | Exists | Has `hosting_type` — needs 3 new columns |
| `GameInstanceRepository` | Exists | Has filter methods — needs 3 new query methods |
| `GameController` (`pokergameserver`) | Exists | At `/api/v1/games` — extend, don't replace |
| `GameService` | Exists | Has createGame, joinGame (no password), startGame (no auth) — extend |
| `GameSummary` | Exists | Thin record — replace with richer version |
| `ServerMessageType` | Exists | Has 27 types — add 7 lobby types |
| `GameEventBroadcaster` | Exists | Add lobby broadcast methods |
| `GameWebSocketHandler` | Exists | Add lobby-state handling |
| `GameServerProperties` | Exists | Record — add 3 new cleanup config fields |
| `api/GameController` | Exists | Legacy — delete it |

---

## Phase 6.1: Schema — Add Three Columns to `game_instances`

### `GameInstanceEntity` — Add Fields

**File:** `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/persistence/entity/GameInstanceEntity.java`

Add three JPA fields with getters/setters:

```java
// WebSocket URL. For SERVER games: auto-derived from server base URL + gameId
// (e.g. "wss://play.ddpoker.com/ws/games/{gameId}"). For COMMUNITY games:
// supplied by the host at registration time.
@Column(name = "ws_url", length = 255)
private String wsUrl;

// BCrypt hash of the join password. Null if the game is public.
@Column(name = "password_hash", length = 255)
private String passwordHash;

// Timestamp of last heartbeat. Null for SERVER games (server is always alive).
// COMMUNITY games must send a heartbeat every ≤60 s or be marked CANCELLED.
@Column(name = "last_heartbeat")
private Instant lastHeartbeat;
```

Schema update is handled automatically by Hibernate (`ddl-auto=update`). No migration file needed for H2.

### `GameInstanceRepository` — Add Three Query Methods

**File:** `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/persistence/repository/GameInstanceRepository.java`

```java
/**
 * Find COMMUNITY games whose last heartbeat is older than the given cutoff,
 * or that have never sent a heartbeat (null). Used for stale-game cleanup.
 */
@Query("SELECT g FROM GameInstanceEntity g WHERE g.hostingType = 'COMMUNITY' " +
       "AND g.status IN ('WAITING_FOR_PLAYERS', 'IN_PROGRESS') " +
       "AND (g.lastHeartbeat IS NULL OR g.lastHeartbeat < :cutoff)")
List<GameInstanceEntity> findStaleCommmunityGames(@Param("cutoff") Instant cutoff);

/**
 * Find SERVER games that have been in WAITING_FOR_PLAYERS longer than the
 * given cutoff (abandoned lobbies).
 */
@Query("SELECT g FROM GameInstanceEntity g WHERE g.hostingType = 'SERVER' " +
       "AND g.status = 'WAITING_FOR_PLAYERS' AND g.createdAt < :cutoff")
List<GameInstanceEntity> findAbandonedServerLobbies(@Param("cutoff") Instant cutoff);

/**
 * Find games in terminal states (COMPLETED, CANCELLED) older than the
 * retention cutoff. These are safe to delete.
 */
@Query("SELECT g FROM GameInstanceEntity g WHERE g.status IN ('COMPLETED', 'CANCELLED') " +
       "AND g.completedAt < :cutoff")
List<GameInstanceEntity> findExpiredGames(@Param("cutoff") Instant cutoff);

/**
 * Update the last_heartbeat timestamp for a community-hosted game.
 */
@Modifying(clearAutomatically = true)
@Query("UPDATE GameInstanceEntity g SET g.lastHeartbeat = :now WHERE g.gameId = :gameId")
void updateHeartbeat(@Param("gameId") String gameId, @Param("now") Instant now);
```

---

## Phase 6.2: Game Discovery API

### 6.2.1 Replace `GameSummary` DTO

**File:** `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/dto/GameSummary.java`

Replace the existing thin record with a richer one. All callers (GameService.listGames, GameController) must be updated to match.

```java
/**
 * Public game summary returned by the lobby listing API.
 *
 * wsUrl is null for private games when the caller is not authenticated.
 * For authenticated callers who have successfully joined, wsUrl is included
 * in GameJoinResponse instead.
 */
public record GameSummary(
    String gameId,
    String name,
    String hostingType,    // "SERVER" | "COMMUNITY"
    String status,         // GameInstanceState name
    String ownerName,
    int playerCount,
    int maxPlayers,
    boolean isPrivate,     // true if passwordHash is non-null
    String wsUrl,          // null for private games in list view
    BlindsSummary blinds,  // from GameConfig
    Instant createdAt,
    Instant startedAt
) {}
```

**New nested record** (same file or inner class):
```java
public record BlindsSummary(int smallBlind, int bigBlind, int ante) {}
```

**GameService.toSummary helper** — centralize entity → DTO mapping:
```java
private GameSummary toSummary(GameInstanceEntity e) {
    boolean isPrivate = e.getPasswordHash() != null;
    // wsUrl omitted from listing for private games; included in join response
    String wsUrl = isPrivate ? null : e.getWsUrl();
    BlindsSummary blinds = parseBlinds(e.getProfileData());
    return new GameSummary(e.getGameId(), e.getName(), e.getHostingType(),
        e.getStatus().name(), e.getOwnerName(), e.getPlayerCount(), e.getMaxPlayers(),
        isPrivate, wsUrl, blinds, e.getCreatedAt(), e.getStartedAt());
}
```

### 6.2.2 Add New DTOs

Create these files in `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/dto/`:

**`GameListResponse.java`**
```java
/** Paginated lobby listing response. */
public record GameListResponse(
    List<GameSummary> games,
    int total,
    int page,
    int pageSize
) {}
```

**`CommunityGameRegisterRequest.java`**
```java
/** Request body for POST /api/v1/games/community. */
public record CommunityGameRegisterRequest(
    @NotBlank String name,
    @NotBlank String wsUrl,           // Full WebSocket URL host provides
    @Valid GameConfig profile,
    String password                   // Optional; null = public game
) {}
```

**`GameJoinRequest.java`**
```java
/** Request body for POST /api/v1/games/{id}/join. */
public record GameJoinRequest(
    String password    // null or omitted for public games
) {}
```

**`GameJoinResponse.java`**
```java
/** Returned after a successful join. Contains the WS URL to connect to. */
public record GameJoinResponse(
    String wsUrl,       // Full WebSocket URL for the game
    String gameId
) {}
```

**`GameSettingsRequest.java`**
```java
/** Request body for PUT /api/v1/games/{id}/settings. Null fields = no change. */
public record GameSettingsRequest(
    String name,
    Integer maxPlayers,
    GameConfig profile,
    String password     // Empty string = remove password; null = no change
) {}
```

**`KickRequest.java`**
```java
/** Request body for POST /api/v1/games/{id}/kick. */
public record KickRequest(
    @NotNull Long profileId
) {}
```

### 6.2.3 Extend `GameService`

**File:** `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/service/GameService.java`

Add the following methods. The existing `createGame`, `joinGame` (no password), `startGame` (no auth check), `getGameState`, `listGames` must be updated or replaced inline:

```java
/**
 * Paginated game listing with optional filters.
 *
 * @param statuses    comma-separated status names to include
 *                    (default: "WAITING_FOR_PLAYERS,IN_PROGRESS")
 * @param hostingType "SERVER", "COMMUNITY", or null for all
 * @param search      substring match against game name and owner name (case-insensitive)
 * @param page        zero-based page index
 * @param pageSize    items per page (max 100)
 * @return paginated response
 */
public GameListResponse listGames(List<GameInstanceState> statuses,
                                  String hostingType,
                                  String search,
                                  int page,
                                  int pageSize);

/**
 * Register a community-hosted game. The host is responsible for running
 * a Spring Boot game server and providing its WebSocket URL. The host
 * must send a heartbeat at least every 60 s.
 *
 * @param ownerProfileId JWT-authenticated caller's profile ID
 * @param ownerName      caller's display name
 * @param request        registration payload
 * @return newly created game summary
 */
public GameSummary registerCommunityGame(Long ownerProfileId,
                                         String ownerName,
                                         CommunityGameRegisterRequest request);

/**
 * Join gate: validates password (if private) and returns the WebSocket URL.
 * Does NOT increment playerCount — that happens when the WebSocket connects
 * and GameInstance.addPlayer() fires. Two players validating simultaneously
 * for the last seat is resolved at WebSocket-connect time; the second is
 * rejected by GameInstance (already full), which is acceptable.
 *
 * @param gameId   target game
 * @param password supplied password (null for public games)
 * @return join response containing the WebSocket URL
 * @throws GameServerException(GAME_NOT_FOUND)     if gameId invalid
 * @throws GameServerException(WRONG_PASSWORD)     if password mismatch
 * @throws GameServerException(GAME_NOT_JOINABLE)  if status != WAITING_FOR_PLAYERS
 */
public GameJoinResponse joinGame(String gameId, String password);

/**
 * Record a heartbeat from the community host.
 * Only the game owner may call this.
 *
 * @throws GameServerException(GAME_NOT_FOUND)       if gameId invalid
 * @throws GameServerException(NOT_GAME_OWNER)       if requester != owner
 * @throws GameServerException(WRONG_HOSTING_TYPE)   if game is SERVER-hosted
 */
public void heartbeat(String gameId, Long requesterProfileId);

/**
 * Start the game (WAITING_FOR_PLAYERS → IN_PROGRESS).
 * Only the game owner may start.
 *
 * SERVER games: updates DB status, triggers GameInstance.start() on the
 * thread pool, broadcasts LOBBY_GAME_STARTING to all connected WS clients.
 *
 * COMMUNITY games: updates DB status to IN_PROGRESS only (keeps the discovery
 * listing accurate). No WebSocket broadcast — community clients are connected
 * to the host's embedded server, not the WAN server.
 *
 * @throws GameServerException(GAME_NOT_FOUND)       if gameId invalid
 * @throws GameServerException(NOT_GAME_OWNER)       if requester != owner
 * @throws GameServerException(GAME_ALREADY_STARTED) if not in WAITING_FOR_PLAYERS
 */
public GameSummary startGame(String gameId, Long requesterProfileId);

/**
 * Update pre-game settings. SERVER games only.
 * Updates DB record and broadcasts LOBBY_SETTINGS_CHANGED to WS clients.
 *
 * @throws GameServerException(GAME_NOT_FOUND)        if gameId invalid
 * @throws GameServerException(NOT_GAME_OWNER)        if requester != owner
 * @throws GameServerException(GAME_NOT_IN_LOBBY)     if not WAITING_FOR_PLAYERS
 * @throws GameServerException(NOT_APPLICABLE)        if COMMUNITY game
 */
public GameSummary updateSettings(String gameId, Long requesterProfileId,
                                  GameSettingsRequest request);

/**
 * Kick a player from the lobby (pre-game). SERVER games only.
 * For mid-game kicks use the WebSocket ADMIN_KICK message (already in M3).
 * Broadcasts LOBBY_PLAYER_KICKED to WS clients on success.
 *
 * @throws GameServerException(GAME_NOT_FOUND)    if gameId invalid
 * @throws GameServerException(NOT_GAME_OWNER)    if requester != owner
 * @throws GameServerException(GAME_NOT_IN_LOBBY) if not WAITING_FOR_PLAYERS
 * @throws GameServerException(PLAYER_NOT_FOUND)  if profileId not in lobby
 * @throws GameServerException(NOT_APPLICABLE)    if COMMUNITY game
 */
public void kickFromLobby(String gameId, Long requesterProfileId, Long targetProfileId);

/**
 * Cancel a game. Valid at any point before COMPLETED.
 * Broadcasts GAME_CANCELLED to all connected clients.
 * Marks game as CANCELLED and sets completedAt.
 *
 * @throws GameServerException(GAME_NOT_FOUND)   if gameId invalid
 * @throws GameServerException(NOT_GAME_OWNER)   if requester != owner
 * @throws GameServerException(GAME_COMPLETED)   if already in terminal state
 */
public void cancelGame(String gameId, Long requesterProfileId);
```

**Error codes** — add to `GameServerException` or use existing mechanism:
`GAME_NOT_FOUND`, `GAME_FULL`, `WRONG_PASSWORD`, `GAME_NOT_JOINABLE`, `NOT_GAME_OWNER`, `WRONG_HOSTING_TYPE`, `GAME_ALREADY_STARTED`, `GAME_NOT_IN_LOBBY`, `PLAYER_NOT_FOUND`, `GAME_COMPLETED`

**Password handling** — use BCrypt (already a project dependency via Spring Security):
```java
// Registration/settings: hash before storing
entity.setPasswordHash(passwordEncoder.encode(rawPassword));

// Join: verify
if (!passwordEncoder.matches(suppliedPassword, entity.getPasswordHash())) {
    throw new GameServerException(ErrorCode.WRONG_PASSWORD, "Incorrect password");
}
```

**ws_url for SERVER games** — derive at create time from server base URL + gameId:
```java
String wsUrl = serverBaseUrl + "/ws/games/" + gameId;
entity.setWsUrl(wsUrl);
```

`serverBaseUrl` comes from a new config property (see Phase 6.5).

### 6.2.4 Extend `GameController`

**File:** `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/controller/GameController.java`

Replace the existing implementations and add new endpoints. Final endpoint set:

```
GET    /api/v1/games                    List games (filterable, paginated)
GET    /api/v1/games/{id}               Get single game summary
POST   /api/v1/games                    Create server-hosted game (authenticated)
POST   /api/v1/games/community          Register community-hosted game (authenticated)
POST   /api/v1/games/{id}/join          Join game, returns wsUrl (authenticated)
POST   /api/v1/games/{id}/heartbeat     Community host keepalive (owner only)
POST   /api/v1/games/{id}/start         Start game (owner only)
PUT    /api/v1/games/{id}/settings      Update pre-game settings (owner only)
POST   /api/v1/games/{id}/kick          Kick from lobby (owner only)
DELETE /api/v1/games/{id}               Cancel game (owner only)
```

**GET /api/v1/games** — query parameters:
- `status` (String, default `WAITING_FOR_PLAYERS,IN_PROGRESS`) — comma-separated `GameInstanceState` names
- `hostingType` (String, optional) — `SERVER` or `COMMUNITY`
- `search` (String, optional) — case-insensitive substring match on name or ownerName
- `page` (int, default `0`)
- `pageSize` (int, default `50`, max `100`)

Response: `GameListResponse`

**GET /api/v1/games/{id}** — Response: `GameSummary` or 404

**POST /api/v1/games** — Body: `GameConfig`; requires auth; Response: `CreateGameResponse` (gameId)

**POST /api/v1/games/community** — Body: `CommunityGameRegisterRequest`; requires auth; Response: `GameSummary`

**POST /api/v1/games/{id}/join** — Body: `GameJoinRequest`; requires auth; Response: `GameJoinResponse`
- Validates password only. Does NOT increment playerCount (that happens at WebSocket connect).
- 200 OK with `{ wsUrl, gameId }` on success
- 403 `WRONG_PASSWORD` if password incorrect
- 409 `GAME_NOT_JOINABLE` if not WAITING_FOR_PLAYERS

**POST /api/v1/games/{id}/heartbeat** — No body; requires auth (owner); Response: 200 OK
- COMMUNITY games only. Returns `422 NOT_APPLICABLE` for SERVER games.

**POST /api/v1/games/{id}/start** — No body; requires auth (owner); Response: `GameSummary`
- SERVER: triggers `GameInstance.start()` + broadcasts `LOBBY_GAME_STARTING` to WS clients.
- COMMUNITY: updates DB status to `IN_PROGRESS` only. No WS broadcast (community clients connect to host's WS, not WAN server's).
- Replaces existing `startGame` that had no auth check.

**PUT /api/v1/games/{id}/settings** — Body: `GameSettingsRequest`; requires auth (owner); Response: `GameSummary`
- SERVER games only. Returns `422 NOT_APPLICABLE` for COMMUNITY games.

**POST /api/v1/games/{id}/kick** — Body: `KickRequest`; requires auth (owner); Response: 200 OK
- SERVER games only. Returns `422 NOT_APPLICABLE` for COMMUNITY games.

**DELETE /api/v1/games/{id}** — No body; requires auth (owner); Response: 204 No Content
- Both game types: marks as CANCELLED in DB.
- SERVER: also broadcasts `GAME_CANCELLED` to WS clients.
- COMMUNITY: DB update only (no WS reach to community players).

All 4xx errors use the existing `ErrorResponse` DTO and `GameServerExceptionHandler`.
`NOT_APPLICABLE` errors return HTTP 422 Unprocessable Entity.

### 6.2.5 Delete Legacy API GameController

**File to delete:** `code/api/src/main/java/com/donohoedigital/poker/api/controller/GameController.java`

The `api` module's `GameController` (at `/api/games`) duplicates the `pokergameserver` controller and uses the legacy `OnlineGame`/`wan_game` system. Delete it.

After deletion, check whether other classes in the `api` module reference `OnlineGame` for game discovery:
- If `OnlineGame` is still needed for tournament history display (it likely is), keep the entity — just remove the game-listing usage.
- If `GameListResponse`/`OnlineGameList` in the `api` module are now unused, delete them too.

---

## Phase 6.3: Lobby WebSocket Messages

The M3 WebSocket protocol covers in-game messages only. Clients connecting to a WAITING_FOR_PLAYERS game need a parallel set of lobby-phase messages.

### 6.3.1 Add 7 New `ServerMessageType` Values

**File:** `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/message/ServerMessageType.java`

Add after the existing values:

```java
/** Lobby state snapshot sent on WebSocket connect when game is WAITING_FOR_PLAYERS */
LOBBY_STATE,

/** A player joined the lobby (broadcast to all lobby connections) */
LOBBY_PLAYER_JOINED,

/** A player left the lobby voluntarily (broadcast to all lobby connections) */
LOBBY_PLAYER_LEFT,

/** Owner updated game settings (broadcast to all lobby connections) */
LOBBY_SETTINGS_CHANGED,

/** Owner started the game; lobby transitions to active gameplay */
LOBBY_GAME_STARTING,

/** A player was kicked from the lobby by the owner */
LOBBY_PLAYER_KICKED,

/** Game was cancelled by owner or cleanup job */
GAME_CANCELLED,
```

### 6.3.2 Add Lobby Fields to `ServerMessageData`

**File:** `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/message/ServerMessageData.java`

Add new fields (all nullable — populated only for their respective message type):

```java
// ---- Lobby messages ----

/** For LOBBY_STATE: full lobby snapshot */
private LobbyStateData lobbyState;

/** For LOBBY_PLAYER_JOINED / LOBBY_PLAYER_LEFT / LOBBY_PLAYER_KICKED */
private LobbyPlayerData lobbyPlayer;

/** For LOBBY_SETTINGS_CHANGED: updated game summary */
private GameSummary updatedSettings;

/** For LOBBY_GAME_STARTING: countdown before game goes live */
private Integer startingInSeconds;

/** For GAME_CANCELLED: human-readable reason */
private String cancelReason;
```

**Add inner data classes** (same file or separate):

```java
/** Full snapshot of lobby state — sent to players connecting while WAITING_FOR_PLAYERS */
public record LobbyStateData(
    String gameId,
    String name,
    String hostingType,
    String ownerName,
    long ownerProfileId,
    int maxPlayers,
    boolean isPrivate,
    List<LobbyPlayerData> players,
    GameSummary.BlindsSummary blinds
) {}

/** Single player entry for lobby messages */
public record LobbyPlayerData(
    long profileId,
    String name,
    boolean isOwner,
    boolean isAI,
    String aiSkillLevel   // null if human
) {}
```

### 6.3.3 Extend `GameWebSocketHandler` for Lobby Phase

**File:** `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/GameWebSocketHandler.java`

On WebSocket connect (`afterConnectionEstablished`), check the game's current state:
- If `IN_PROGRESS` or later: send existing `CONNECTED` + `GAME_STATE` messages (current behavior)
- If `WAITING_FOR_PLAYERS`: send `CONNECTED` + `LOBBY_STATE` instead

Add helper method:
```java
private void sendLobbyState(PlayerConnection connection, GameInstance game) {
    LobbyStateData data = buildLobbyState(game);
    ServerMessage msg = ServerMessage.of(ServerMessageType.LOBBY_STATE, data);
    connection.send(msg);
}
```

### 6.3.4 Add Lobby Broadcast Methods to `GameEventBroadcaster`

**File:** `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/GameEventBroadcaster.java`

Add:
```java
/** Broadcast to all connections in a game (lobby or in-game) */
public void broadcastLobbyPlayerJoined(String gameId, LobbyPlayerData player);
public void broadcastLobbyPlayerLeft(String gameId, LobbyPlayerData player);
public void broadcastLobbyPlayerKicked(String gameId, LobbyPlayerData player);
public void broadcastLobbySettingsChanged(String gameId, GameSummary updatedSettings);
public void broadcastLobbyGameStarting(String gameId, int startingInSeconds);
public void broadcastGameCancelled(String gameId, String reason);
```

These follow the same pattern as existing broadcast methods (iterate `PlayerConnection`s for the game, send `ServerMessage`).

**`GameService` calls broadcasters** after mutations:
- `startGame` → `broadcastLobbyGameStarting(gameId, 3)` then schedules actual `GameInstance.start()`
- `updateSettings` → `broadcastLobbySettingsChanged(gameId, updatedSummary)`
- `kickFromLobby` → `broadcastLobbyPlayerKicked(gameId, playerData)`
- `cancelGame` → `broadcastGameCancelled(gameId, "Game cancelled by owner")`

---

## Phase 6.4: Stale Game Cleanup

### New `StaleGameCleanupJob`

**File:** `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/service/StaleGameCleanupJob.java`

```java
// @EnableScheduling goes on GameServerAutoConfiguration, not here
@Component
public class StaleGameCleanupJob {

    private final GameInstanceRepository repo;
    private final GameEventBroadcaster broadcaster;
    private final GameServerProperties properties;

    // Runs every 60 seconds
    @Scheduled(fixedDelayString = "${game.server.cleanup-interval-seconds:60}000")
    @Transactional
    public void cleanup() {
        cancelStaleCommunityGames();
        cancelAbandonedServerLobbies();
        deleteExpiredGames();
    }

    private void cancelStaleCommunityGames() {
        Instant cutoff = Instant.now().minus(
            properties.communityHeartbeatTimeoutMinutes(), ChronoUnit.MINUTES);
        repo.findStaleCommmunityGames(cutoff).forEach(game -> {
            repo.updateStatusWithCompletionTime(game.getGameId(),
                GameInstanceState.CANCELLED, Instant.now());
            broadcaster.broadcastGameCancelled(game.getGameId(),
                "Community host disconnected");
        });
    }

    private void cancelAbandonedServerLobbies() {
        Instant cutoff = Instant.now().minus(
            properties.lobbyTimeoutHours(), ChronoUnit.HOURS);
        repo.findAbandonedServerLobbies(cutoff).forEach(game -> {
            repo.updateStatusWithCompletionTime(game.getGameId(),
                GameInstanceState.CANCELLED, Instant.now());
            broadcaster.broadcastGameCancelled(game.getGameId(),
                "Lobby expired — game never started");
        });
    }

    private void deleteExpiredGames() {
        Instant cutoff = Instant.now().minus(
            properties.completedGameRetentionDays(), ChronoUnit.DAYS);
        List<GameInstanceEntity> expired = repo.findExpiredGames(cutoff);
        repo.deleteAll(expired);
    }
}
```

### Add 3 Properties to `GameServerProperties`

**File:** `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/GameServerProperties.java`

`GameServerProperties` is a record — add 3 new fields:
- `int communityHeartbeatTimeoutMinutes` — default `5`
- `int lobbyTimeoutHours` — default `24`
- `int completedGameRetentionDays` — default `7`

Update both the canonical constructor (validation + defaults) and the no-arg default constructor.

### Add Config in `pokerserver` / `poker` (embedded) `application.properties`

**Standalone server** (`code/pokerserver/src/main/resources/application.properties`):
```properties
game.server.community-heartbeat-timeout-minutes=5
game.server.lobby-timeout-hours=24
game.server.completed-game-retention-days=7
game.server.base-url=wss://play.ddpoker.com   # used to build ws_url for SERVER games
```

**Embedded server profile** (desktop client, `code/poker/src/main/resources/application-embedded.properties`):
```properties
game.server.community-heartbeat-timeout-minutes=5
game.server.lobby-timeout-hours=24
game.server.completed-game-retention-days=7
game.server.base-url=ws://localhost            # placeholder; port appended at runtime
```

**New property `game.server.base-url`** — add to `GameServerProperties` record as `String serverBaseUrl`, default `"ws://localhost"`.

---

## Phase 6.5: Desktop Game List Updates

### Overview

The desktop `FindGames` screen currently fetches games using `GetWanList` (a `SendMessageDialog` that uses the legacy TCP `CAT_WAN_GAME_LIST` protocol). It displays `OnlineGame` objects in a 3-column Swing table. The join flow copies a URL from a text field.

Replace all of this with REST + the new API.

### Files to Modify

| File | Change |
|------|--------|
| `FindGames.java` | Major: replace fetch, update table model, add hosting type column, add lock icon, update join flow |
| `ListGames.java` | Minor: update to work with `GameSummaryDto` instead of `OnlineGame` |
| `GetWanList.java` | Delete (replaced by HTTP REST call in `FindGames`) |
| `SendWanGame.java` | Delete (replaced by `CommunityGameRegistrar`) |

### New File: `CommunityGameRegistrar`

**File:** `code/poker/src/main/java/com/donohoedigital/games/poker/online/CommunityGameRegistrar.java`

Handles community host game registration + heartbeat. Called when user creates a community-hosted game.

```java
public class CommunityGameRegistrar implements Closeable {

    /**
     * Register this machine as a community game host.
     * POSTs to POST /api/v1/games/community.
     * Starts a background heartbeat thread on success.
     *
     * @param serverBaseUrl  remote WAN server URL
     * @param jwtToken       owner's JWT token
     * @param wsUrl          this host's WebSocket URL (ws://public-ip:port/ws/games/{id})
     * @param name           game name
     * @param profile        GameConfig
     * @param password       optional password (null = public)
     * @return gameId on success
     */
    public String register(String serverBaseUrl, String jwtToken, String wsUrl,
                           String name, GameConfig profile, String password);

    /**
     * Deregister by calling DELETE /api/v1/games/{id}.
     * Also stops the heartbeat thread.
     */
    @Override
    public void close();

    // Background thread: POSTs to /api/v1/games/{id}/heartbeat every 45 s
    // (well within the 5-minute timeout)
}
```

### `FindGames` Changes

**File:** `code/poker/src/main/java/com/donohoedigital/games/poker/online/FindGames.java`

**Replace `getWanList()`:**

Current implementation calls `context_.processPhaseNow("GetWanList", hmParams)`. Replace with:

```java
private void fetchGameList() {
    // Run in background thread to avoid blocking Swing EDT
    CompletableFuture.supplyAsync(() -> {
        String url = remoteServerUrl + "/api/v1/games"
            + "?status=WAITING_FOR_PLAYERS,IN_PROGRESS"
            + "&page=" + currentPage
            + "&pageSize=10";
        // Use JDK HttpClient (already available — used by WebSocketGameClient in M4)
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + getJwtToken())
            .GET().build();
        HttpResponse<String> response = httpClient.send(request, ...);
        return objectMapper.readValue(response.body(), GameListResponse.class);
    }).thenAcceptAsync(result -> {
        // Update table model on EDT
        SwingUtilities.invokeLater(() -> updateTableModel(result));
    }, SwingUtilities::invokeLater);
}
```

**Update `WanGameModel`** (inner class in `FindGames`):

Replace `OnlineGame`-based model with `GameSummary`-based:

- Data source: `List<GameSummary>` instead of `OnlineGameList`
- Columns (5 total, replacing current 3):

| Column | Data | Width |
|--------|------|-------|
| (icon) | Lock icon if `isPrivate`; blank otherwise | 24px |
| Name | `gameSummary.name()` | ~250px |
| Type | "Server" / "Community" (styled badge) | 100px |
| Host | `gameSummary.ownerName()` | 120px |
| Players | `"4/9"` from playerCount/maxPlayers | 60px |

**Update join flow** (currently copies URL to text field; user clicks "Start"):

```
Old: user selects row → URL copied to text field → user clicks Join → TCP connect
New: user selects row → user clicks Join
  → if isPrivate: show password dialog (DDTextDialog or new PasswordDialog)
  → POST /api/v1/games/{id}/join { password }
  → 403: show "Incorrect password" error and keep dialog open
  → 200: receive { wsUrl, gameId } → hand off to WebSocketGameClient (M4)
```

**Remove:**
- `GetWanList` phase processing
- URL text field (`DDTextField` for connection URL)
- "Paste" / "Use Last" buttons (no longer needed)

**Keep:**
- Pagination (`DDPagingTable`, page navigation buttons)
- `GameInfoPanel` on the right (new component — see below)
- Refresh button
- Search/filter toolbar

### New `GameInfoPanel`

**File:** `code/poker/src/main/java/com/donohoedigital/games/poker/online/GameInfoPanel.java`

A new Swing panel built directly around `GameSummary`. Replaces `TournamentSummaryPanel` in `FindGames` and becomes the canonical game detail component. M5 (web) should mirror its field set in the web game detail view.

**Fields displayed:**

| Field | Source | Display |
|-------|--------|---------|
| Game name | `gameSummary.name()` | Large heading |
| Hosting type | `gameSummary.hostingType()` | "🖥 Server Hosted" / "👤 Community Hosted" badge |
| Host | `gameSummary.ownerName()` | Label row |
| Privacy | `gameSummary.isPrivate()` | "🔒 Password protected" or "Public" |
| Status | `gameSummary.status()` | "Registering" / "In Progress" |
| Players | `gameSummary.playerCount()` / `gameSummary.maxPlayers()` | "4 / 9 players" |
| Blinds | `gameSummary.blinds()` | "Small: 25 / Big: 50 / Ante: 0" |
| Created | `gameSummary.createdAt()` | Relative time ("5 minutes ago") |

**Behaviour:**
- Updated whenever the selected row in `FindGames` changes (same as current `TournamentSummaryPanel`)
- Shows a placeholder ("Select a game to see details") when no row is selected
- No interaction — display only

**Match existing style:** Use `DDLabel`, `DDPanel`, `DDSpacer` and the existing font/colour constants from the codebase to match the surrounding UI chrome. Do not introduce custom colours or fonts.

`TournamentSummaryPanel` is removed from `FindGames` and can be deleted if it has no other callers. Verify before deleting.

### `ListGames` Changes

**File:** `code/poker/src/main/java/com/donohoedigital/games/poker/online/ListGames.java`

This is the base class. Changes are minimal:
- Update any references to `OnlineGame` column name constants to the new `GameSummary` fields
- If `ListGames` contains the `WanGameModel` inner class, it moves to `FindGames` (or stays if shared)

### Add Password Dialog

**File:** `code/poker/src/main/java/com/donohoedigital/games/poker/online/PasswordDialog.java` (new, or reuse existing modal dialog pattern)

Simple Swing modal:
- Single `JPasswordField`
- OK / Cancel buttons
- Returns `char[]` on confirm, null on cancel
- Shown only when `gameSummary.isPrivate()` is true

Use the existing `DDDialog` or `DDOption` pattern from the codebase if one exists that fits.

---

## Phase 6.6: Configuration — Server Base URL

**New field in `GameServerProperties`:**
```java
String serverBaseUrl   // default: "ws://localhost"
```

Used when building `ws_url` for SERVER-hosted games at creation time:
```java
String wsUrl = properties.serverBaseUrl() + "/ws/games/" + gameId;
```

For the standalone server, this must be set in `application.properties` to the publicly reachable address (e.g., `wss://play.ddpoker.com`).

For the embedded server in the desktop client, the port is randomly assigned at startup (`server.port=0`). After `SpringApplication.run()` returns, `EmbeddedGameServer` reads the actual port from the `ApplicationContext`:

```java
int port = context.getEnvironment()
    .getRequiredProperty("local.server.port", Integer.class);
String wsUrl = "ws://localhost:" + port;
```

`GameService` must not read `serverBaseUrl` from the immutable `GameServerProperties` record at bean creation time. Instead, expose it via a settable field initialised after Spring context startup:

```java
// In GameService:
private String serverBaseUrl;  // set by EmbeddedGameServer post-startup, or by properties for standalone

public void setServerBaseUrl(String baseUrl) {
    this.serverBaseUrl = baseUrl;
}
```

`EmbeddedGameServer.start()` calls `gameService.setServerBaseUrl("ws://localhost:" + port)` after context starts. The standalone server sets `serverBaseUrl` directly from `GameServerProperties` in a `@PostConstruct` on `GameService`. This is the only mutable field on `GameService`; all other state remains immutable.

---

## Testing

### Unit Tests

All in `pokergameserver` module, following the existing TDD pattern (write tests first).

**`GameServiceDiscoveryTest`**
Location: `code/pokergameserver/src/test/java/.../service/GameServiceDiscoveryTest.java`

Cover:
- `listGames` — filtering by status, hostingType, search; pagination (page 0, page 1, empty page)
- `registerCommunityGame` — creates entity with `COMMUNITY` hosting type, stores hashed password, stores ws_url
- `joinGame` — public game: returns wsUrl without incrementing playerCount; private game correct password: returns wsUrl; wrong password: throws; non-WAITING_FOR_PLAYERS: throws
- `heartbeat` — updates `lastHeartbeat` for COMMUNITY game; non-owner throws; SERVER game throws `NOT_APPLICABLE`
- `startGame (SERVER)` — owner starts: triggers GameInstance, broadcasts `LOBBY_GAME_STARTING`; non-owner throws; already IN_PROGRESS throws
- `startGame (COMMUNITY)` — owner starts: updates DB status to IN_PROGRESS, no WS broadcast; non-owner throws
- `updateSettings` — SERVER game: owner updates, broadcasts `LOBBY_SETTINGS_CHANGED`; COMMUNITY game: throws `NOT_APPLICABLE`; non-owner throws
- `kickFromLobby` — SERVER game: owner kicks, broadcasts `LOBBY_PLAYER_KICKED`; COMMUNITY game: throws `NOT_APPLICABLE`
- `cancelGame (SERVER)` — owner cancels, broadcasts `GAME_CANCELLED`; non-owner throws; COMPLETED throws
- `cancelGame (COMMUNITY)` — owner cancels: DB update only, no WS broadcast; non-owner throws

**`GameControllerTest`** (Spring MockMvc)
Location: `code/pokergameserver/src/test/java/.../controller/GameControllerTest.java`

Cover all new/changed endpoints. Verify:
- 200/201/204 on success with correct response body
- 403 on wrong password for join
- 404 on unknown gameId
- 422 on `kick` or `settings` for COMMUNITY game
- 422 on `heartbeat` for SERVER game
- 401 on unauthenticated calls to auth-required endpoints
- 403 on non-owner calls to owner-only endpoints
- Pagination: correct `total`, `page`, `pageSize` in response
- `wsUrl` is absent from listing response for private games, present for public

**`StaleGameCleanupJobTest`**
Location: `code/pokergameserver/src/test/java/.../service/StaleGameCleanupJobTest.java`

Cover:
- COMMUNITY game with `lastHeartbeat` older than timeout → cancelled
- COMMUNITY game with null `lastHeartbeat` → cancelled
- COMMUNITY game with fresh heartbeat → not cancelled
- SERVER game in WAITING_FOR_PLAYERS older than `lobbyTimeoutHours` → cancelled
- SERVER game in WAITING_FOR_PLAYERS within timeout → not cancelled
- COMPLETED game older than retention period → deleted
- CANCELLED game older than retention period → deleted
- `GAME_CANCELLED` broadcast sent for each cancelled game

**`LobbyWebSocketMessageTest`**
Location: `code/pokergameserver/src/test/java/.../websocket/LobbyWebSocketMessageTest.java`

Cover:
- Connect to WAITING_FOR_PLAYERS game → receives `LOBBY_STATE` (not `GAME_STATE`)
- Connect to IN_PROGRESS game → receives `GAME_STATE` (existing behavior, regression)
- Player joins lobby → all existing connections receive `LOBBY_PLAYER_JOINED`
- Owner starts game → all connections receive `LOBBY_GAME_STARTING`
- Owner updates settings → all connections receive `LOBBY_SETTINGS_CHANGED`
- Owner cancels → all connections receive `GAME_CANCELLED`

### Integration Tests

**`GameDiscoveryIntegrationTest`**
Location: `code/pokergameserver/src/test/java/.../GameDiscoveryIntegrationTest.java`

Full lifecycle using `@SpringBootTest`:

1. Create SERVER game via `POST /api/v1/games` → appears in `GET /api/v1/games` listing
2. Create COMMUNITY game via `POST /api/v1/games/community` → appears with `hostingType=COMMUNITY`
3. Create private game → appears in listing with `isPrivate=true`, `wsUrl=null`
4. Join private game with wrong password → 403
5. Join private game with correct password → 200 with `wsUrl`
6. Send heartbeat as owner → 200; send heartbeat as non-owner → 403
7. Simulate heartbeat timeout: create COMMUNITY game, advance clock past timeout, run cleanup → game disappears from active listing
8. Cancel game as owner → 204; game in listing shows `CANCELLED`
9. Cancel game as non-owner → 403

---

## Files Summary

### New Files

| File | Purpose |
|------|---------|
| `pokergameserver/.../dto/GameListResponse.java` | Paginated listing response |
| `pokergameserver/.../dto/CommunityGameRegisterRequest.java` | Community registration body |
| `pokergameserver/.../dto/GameJoinRequest.java` | Join request with optional password |
| `pokergameserver/.../dto/GameJoinResponse.java` | Join response with wsUrl |
| `pokergameserver/.../dto/GameSettingsRequest.java` | Settings update body |
| `pokergameserver/.../dto/KickRequest.java` | Lobby kick body |
| `pokergameserver/.../service/StaleGameCleanupJob.java` | Scheduled stale-game cleanup |
| `poker/.../online/CommunityGameRegistrar.java` | Desktop community host registration + heartbeat |
| `poker/.../online/PasswordDialog.java` | Password prompt for private-game join |
| `poker/.../online/GameInfoPanel.java` | New canonical game detail panel (replaces TournamentSummaryPanel in FindGames; defines field set for M5 web view) |
| Tests (5 classes) | See Testing section |

### Modified Files

| File | Change |
|------|--------|
| `GameInstanceEntity.java` | +3 fields: `wsUrl`, `passwordHash`, `lastHeartbeat` |
| `GameInstanceRepository.java` | +4 query methods |
| `GameSummary.java` | Replace thin record with richer version |
| `GameController.java` (`pokergameserver`) | Add 6 endpoints, update 2 existing |
| `GameService.java` | Add 7 new methods, update 3 existing |
| `ServerMessageType.java` | +7 lobby message types |
| `ServerMessageData.java` | +5 lobby data fields + 2 new inner records |
| `GameWebSocketHandler.java` | Add lobby-state path on connect |
| `GameEventBroadcaster.java` | +6 lobby broadcast methods |
| `GameServerProperties.java` | +4 new config fields |
| `FindGames.java` | Replace fetch, update table model, update join flow |
| `ListGames.java` | Update for new DTO |
| `application.properties` (pokerserver) | +4 new game.server.* properties |
| `application-embedded.properties` (poker) | +4 new game.server.* properties |

### Deleted Files

| File | Reason |
|------|--------|
| `GetWanList.java` | Replaced by REST in FindGames |
| `SendWanGame.java` | Replaced by CommunityGameRegistrar |
| `api/.../controller/GameController.java` | Legacy; superseded by pokergameserver controller |
| `TournamentSummaryPanel.java` | Replaced by GameInfoPanel — verify no other callers before deleting |

---

## Effort Re-estimate

| Phase | Effort |
|-------|--------|
| 6.1 Schema | XS (1 day) |
| 6.2 Discovery API | M (4–5 days) |
| 6.3 Lobby WebSocket | S (2 days) |
| 6.4 Stale Cleanup | S (1–2 days) |
| 6.5 Desktop List | M (4–5 days) |
| 6.6 Config / base-url | XS (1 day) |
| Tests | M (4–5 days) |
| **Total** | **L (~3 weeks)** |
