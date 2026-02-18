# M6: Game Discovery & Management — Detailed Plan

**Plan file:** `.claude/plans/M6-GAME-DISCOVERY-MANAGEMENT.md`
**Parent plan:** `.claude/plans/SERVER-HOSTED-GAME-ENGINE.md`
**Depends on:** M1 ✅ M2 ✅ M3 ✅ M4 ✅
**Effort:** M

---

## Context

M1–M4 built the full server-side game engine, REST API, WebSocket protocol, and embedded desktop server. What's missing is the glue that makes games *discoverable*: the `GET /api/v1/games` response doesn't include hosting type, there are no REST endpoints for admin operations (pause/resume/cancel/kick), community-hosted games have no way to register with the WAN server in the new system, and the desktop `FindGames` screen still uses the legacy `CAT_WAN_GAME_LIST` servlet protocol. M6 closes all of these gaps.

---

## What Already Exists (Do Not Recreate)

| Item | Location | State |
|------|----------|-------|
| `hosting_type` column | `GameInstanceEntity` | ✅ exists, defaults to `"SERVER"` |
| `findByHostingType()` | `GameInstanceRepository` | ✅ exists |
| `pauseAsUser()` / `resumeAsUser()` / `cancelAsUser()` | `GameInstance` | ✅ exists, no REST |
| `ADMIN_KICK` / `ADMIN_PAUSE` / `ADMIN_RESUME` | `InboundMessageRouter` | ✅ WebSocket path works |
| `EmbeddedGameServer`, `WebSocketGameClient` | `poker` module | ✅ M4 work |
| `GameService.listGames()` | `pokergameserver` | ✅ exists, missing `hostingType`/`websocketUrl` in response |

**Critical bug found:** `POST /api/v1/games/{id}/start` has **no ownership check**. Any authenticated user can start any game. Fix is included in this plan.

---

## Architecture Decisions

### Decision 1: Where do community-hosted game registrations live?

**Choice: `game_instances` table with a new `external_ws_url VARCHAR(512) NULL` column.**

Community-hosted games are rows in `game_instances` with `hosting_type = 'COMMUNITY'` and `external_ws_url` set to the host's embedded server WebSocket URL. The `wan_game` table and legacy `CAT_WAN_GAME_ADD` path are untouched (M7 concern). The new system routes around them entirely.

### Decision 2: How does a community host register their game?

**Choice: New REST endpoint `POST /api/v1/games/register-community` on the WAN server.**

Consistent with the REST-first design from M2. The desktop client calls this after starting its embedded server. The returned `gameId` is used for subsequent status updates and deregistration.

### Decision 3: Unified listing — single endpoint or two?

**Choice: Single `GET /api/v1/games`.** Already exists. Add `hostingType` and `websocketUrl` to `GameSummary`. The endpoint filters to only active games (`WAITING_FOR_PLAYERS`, `IN_PROGRESS`) by default. Done.

### Decision 4: Pause/resume for community-hosted games via WAN server?

**Choice: Not supported — return `400 Bad Request`.** Community-hosted pause/resume is done via `ADMIN_PAUSE` WebSocket message on the host's embedded server. The WAN server doesn't have the live `GameInstance` for community games. Only `PUT /api/v1/games/{id}/status` is available for community games on the WAN server.

### Decision 5: Kick during IN_PROGRESS vs WAITING_FOR_PLAYERS?

- `WAITING_FOR_PLAYERS`: remove from game entirely (no fold needed)
- `IN_PROGRESS`: mark player as disconnected + set `kicked=true` flag on `ServerPlayerSession` to bar reconnection. Auto-fold handled by existing action timeout logic.

---

## Database Schema Changes

### Change: Add `external_ws_url` to `game_instances`

The project uses Hibernate `ddl-auto` (no Flyway). Add field to entity; Hibernate applies it on next startup.

**`GameInstanceEntity.java` — add field:**
```java
@Column(name = "external_ws_url", length = 512, nullable = true)
private String externalWsUrl;
// + getter/setter
```

No change to `wan_game` table.

---

## New/Modified REST Endpoints

### Phase 6.1: Unified Game Registry

#### Modified: `GET /api/v1/games`
Already exists. Change: `GameSummary` gains `hostingType` and `websocketUrl`. Default filter: only `WAITING_FOR_PLAYERS` and `IN_PROGRESS`. Optional query param `?status=COMPLETED` for historical queries.

**Modified `GameSummary` record:**
```java
public record GameSummary(
    String gameId,
    String name,
    String ownerName,
    int playerCount,
    int maxPlayers,
    String status,
    String hostingType,     // "SERVER" or "COMMUNITY"
    String websocketUrl     // null for SERVER (client constructs); explicit URL for COMMUNITY
)
```

For SERVER games, `websocketUrl` is `null` — clients construct it as `wss://{server-host}/ws/games/{gameId}`. For COMMUNITY games, it is the URL the host registered (e.g., `ws://203.0.113.42:8765/ws/games/{gameId}`).

#### Modified: `GET /api/v1/games/{id}`
Add `hostingType`, `websocketUrl`, `ownerName`, `ownerProfileId` to `GameStateResponse`.

#### New: `POST /api/v1/games/register-community`

Community desktop client registers their embedded server game with the WAN server.

**Request:**
```json
{
  "name": "Alice's Friday Game",
  "websocketUrl": "ws://203.0.113.42:8765/ws/games/local-uuid",
  "maxPlayers": 9,
  "profileData": "{ ... GameConfig JSON ... }"
}
```
**Validation:** `name` not blank; `websocketUrl` matches `wss?://.+`; `maxPlayers` 2–10; JWT required.

**Response `201 Created`:**
```json
{ "gameId": "server-assigned-uuid" }
```

Server creates a `game_instances` row: `hosting_type='COMMUNITY'`, `external_ws_url` = provided URL, `owner_profile_id` = JWT profile ID, `status = WAITING_FOR_PLAYERS`.

#### New: `DELETE /api/v1/games/{id}/registration`

Community host deregisters when their embedded server shuts down. Sets `status = CANCELLED`. JWT required, must be owner.

#### New: `PUT /api/v1/games/{id}/status`

Community host updates game status as it progresses. JWT required, must be owner.

**Request:** `{ "status": "IN_PROGRESS" }`

**Allowed transitions (community games only):**
- `WAITING_FOR_PLAYERS → IN_PROGRESS`
- `IN_PROGRESS → COMPLETED`

Returns `409 Conflict` for any other transition. Returns `400 Bad Request` if called on a SERVER-hosted game (server manages its own state).

---

### Phase 6.2: Owner/Admin Privilege Endpoints

All endpoints use the same ownership check pattern: extract `profileId` from JWT, compare with `game_instances.owner_profile_id`. Extract to `GameController.assertOwner(String gameId, Long profileId)` helper.

#### Fix: `POST /api/v1/games/{id}/start`
**Add ownership check.** Currently missing. `403 Forbidden` if not owner.

#### New: `POST /api/v1/games/{id}/pause`
```
Auth: JWT owner | Valid state: IN_PROGRESS (SERVER-hosted only)
Returns: 200 OK | 409 Conflict (wrong state) | 403 Forbidden | 400 (community game) | 404
```
Calls `GameInstance.pauseAsUser()`. The event bus publishes `GAME_PAUSED`; `GameEventBroadcaster` handles the WebSocket broadcast to all connected clients. Updates `game_instances.status = PAUSED` in DB.

#### New: `POST /api/v1/games/{id}/resume`
```
Auth: JWT owner | Valid state: PAUSED (SERVER-hosted only)
Returns: 200 OK | 409 Conflict | 403 | 400 | 404
```

#### New: `DELETE /api/v1/games/{id}`
Cancel a game entirely.
```
Auth: JWT owner | Valid state: not COMPLETED/CANCELLED
Returns: 200 OK | 409 Conflict | 403 | 404
```
For SERVER-hosted: calls `GameInstance.cancelAsUser()` which shuts down the director thread and broadcasts a cancel message to all WebSocket clients. For COMMUNITY-hosted: sets `status = CANCELLED` in DB only.

#### New: `POST /api/v1/games/{id}/kick`
```
Auth: JWT owner | Any active game state
Request: { "profileId": 42 }
Returns: 200 OK | 400 (self-kick) | 403 | 404 (game or player)
```
For SERVER-hosted: extracts existing kick logic from `InboundMessageRouter.handleAdminKick()` into a shared `GameInstance.kickPlayer(targetProfileId)` helper. Broadcasts `PLAYER_KICKED` message. Closes target player's WebSocket. Sets `kicked = true` on `ServerPlayerSession` to bar reconnection.
For COMMUNITY-hosted: updates `player_count` in DB only. Community host handles the actual disconnect via their embedded server's WebSocket.

---

## Complete Endpoint Table

| Method | Path | Auth | Phase |
|--------|------|------|-------|
| GET | `/api/v1/games` | JWT | 6.1 — modified |
| GET | `/api/v1/games/{id}` | JWT | 6.1 — modified |
| POST | `/api/v1/games/register-community` | JWT | 6.1 — new |
| DELETE | `/api/v1/games/{id}/registration` | JWT owner | 6.1 — new |
| PUT | `/api/v1/games/{id}/status` | JWT owner | 6.1 — new |
| POST | `/api/v1/games/{id}/start` | JWT owner | 6.2 — fix ownership |
| POST | `/api/v1/games/{id}/pause` | JWT owner | 6.2 — new |
| POST | `/api/v1/games/{id}/resume` | JWT owner | 6.2 — new |
| DELETE | `/api/v1/games/{id}` | JWT owner | 6.2 — new |
| POST | `/api/v1/games/{id}/kick` | JWT owner | 6.2 — new |

---

## New DTOs

**Location:** `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/dto/`

```java
// RegisterCommunityGameRequest.java
public record RegisterCommunityGameRequest(
    @NotBlank String name,
    @NotBlank @Pattern(regexp = "wss?://.+") String websocketUrl,
    @Min(2) @Max(10) int maxPlayers,
    String profileData   // nullable, GameConfig JSON
)

// UpdateGameStatusRequest.java
public record UpdateGameStatusRequest(@NotNull GameInstanceState status)

// KickPlayerRequest.java
public record KickPlayerRequest(long profileId)

// GameAdminResponse.java
public record GameAdminResponse(String gameId, String status, String message)
```

---

## Service Layer Changes

**`GameService.java`** — add/modify:

```java
// Modified — add hostingType/websocketUrl to output
public List<GameSummary> listGames()

// Modified — add ownership check
public void startGame(String gameId, Long requestingProfileId)

// New
public String registerCommunityGame(RegisterCommunityGameRequest req, Long ownerProfileId, String ownerName)
public void deregisterGame(String gameId, Long requestingProfileId)
public void updateGameStatus(String gameId, GameInstanceState newStatus, Long requestingProfileId)
public void pauseGame(String gameId, Long requestingProfileId)
public void resumeGame(String gameId, Long requestingProfileId)
public void cancelGame(String gameId, Long requestingProfileId)
public void kickPlayer(String gameId, Long targetProfileId, Long requestingProfileId)
```

**`pauseGame()` implementation note:** Uses `GameEventBus` to publish pause — `GameEventBroadcaster` handles WebSocket broadcast. No direct dependency from `GameService` on `GameConnectionManager`.

**`GameInstanceManager.java`** — add facade methods:
```java
public void pauseGame(String gameId, long requesterId)
public void resumeGame(String gameId, long requesterId)
public void cancelGame(String gameId, long requesterId)
public void kickPlayer(String gameId, long targetProfileId, long requesterId)
```

**`GameInstance.java`** — add:
```java
public void kickPlayer(long targetProfileId)  // extracted from InboundMessageRouter
```

Add `kicked` flag to `ServerPlayerSession`:
```java
private volatile boolean kicked = false;
```
WebSocket reconnection check: if `session.isKicked()`, reject reconnection with error.

**`GameInstanceRepository.java`** — add:
```java
List<GameInstanceEntity> findByStatusIn(List<GameInstanceState> statuses);
```

**`GameServerExceptionHandler.java`** — ensure these HTTP mappings exist:
- Ownership violation → `403 Forbidden`
- Invalid state transition → `409 Conflict`
- Kick self → `400 Bad Request`
- Community game admin → `400 Bad Request`

---

## New Spring Controllers

**`AdminGameController.java`** — pause, resume, cancel (DELETE game), kick
**`CommunityGameController.java`** — register-community, deregister (DELETE registration), PUT status

Separating from `GameController` keeps each controller focused.

---

## Stale Community Game Cleanup

**`StaleGameCleanupScheduler.java`** — new `@Component` in `pokergameserver`:

```java
@Scheduled(fixedDelay = 3_600_000)  // every hour
public void cleanupStaleCommunityGames() {
    Instant waitingCutoff = Instant.now().minus(24, HOURS);
    Instant inProgressCutoff = Instant.now().minus(72, HOURS);
    // Find COMMUNITY games in WAITING_FOR_PLAYERS older than 24h → CANCELLED
    // Find COMMUNITY games in IN_PROGRESS older than 72h → CANCELLED
}
```

Thresholds configurable via `GameServerProperties`:
```properties
game.server.community.stale-waiting-hours=24
game.server.community.stale-in-progress-hours=72
```

---

## Desktop Client Changes (Phase 6.3)

### New Classes

**`RestGameClient.java`** — `code/poker/src/main/java/com/donohoedigital/games/poker/online/`

Thin HTTP client using JDK `java.net.http.HttpClient` (same as `WebSocketGameClient`). Methods:
```java
public List<RemoteGameSummary> listGames()
public String registerCommunityGame(RegisterCommunityGameRequest req)
public void deregisterGame(String gameId)
public void updateGameStatus(String gameId, String status)
public void pauseGame(String gameId)
public void resumeGame(String gameId)
public void cancelGame(String gameId)
public void kickPlayer(String gameId, long profileId)
```
Reads server base URL from `EnginePrefs.OPTION_ONLINE_SERVER`. Uses JWT from `EmbeddedGameServer.getLocalUserJwt()` or the remote login token.

**`RemoteGameSummary.java`** — plain Java class matching the server's `GameSummary` JSON shape. Fields: `gameId`, `name`, `ownerName`, `playerCount`, `maxPlayers`, `status`, `hostingType`, `websocketUrl`.

**`RemoteGameSummaryConverter.java`** — maps `RemoteGameSummary` → `OnlineGame` for existing `WanGameModel`:
- `url` = `websocketUrl` (for COMMUNITY) or constructed `wss://{server}/ws/games/{gameId}` (for SERVER)
- `hostPlayer` = `ownerName`
- `mode` = `OnlineGame.MODE_REG` if `WAITING_FOR_PLAYERS`, `MODE_PLAY` if `IN_PROGRESS`

**`CommunityGameRegistration.java`** — manages the lifecycle of WAN registration for community-hosted games. Called from the community hosting flow. Uses a JVM shutdown hook and game-end callback to call `deregisterGame()`.

### Modified: `FindGames.java`

Replace `getWanList()` body:
- Old: `SendMessageDialog` → `GetWanList` → `CAT_WAN_GAME_LIST` → `PokerServlet`
- New: `RestGameClient.listGames()` → `RemoteGameSummaryConverter.toOnlineGame()` → populate table

**Add "Hosting" column to `WanGameModel` (inner class of `FindGames`):**

```java
// Add 4th column
private static final int[] COLUMN_WIDTHS = new int[]{150, 100, 60, 80};
private static final String[] COLUMN_NAMES = new String[]{
    TOURNAMENT_NAME, HOST_PLAYER, MODE, "Hosting"};

// getValueAt() for column 3:
case 3: return "SERVER".equals(game.getHostingType()) ? "Server" : "Community";
```

`OnlineGame` needs a transient `hostingType` field for the converter to populate (not persisted, not in DB, just held in memory for display). Add:
```java
private transient String hostingType;  // in OnlineGame.java
```

---

## Edge Cases and Handling

| # | Edge Case | Handling |
|---|-----------|----------|
| EC-1 | Community host goes offline without deregistering | `StaleGameCleanupScheduler` cancels WAITING games after 24h, IN_PROGRESS after 72h. Clients get "connection refused" on join attempt → show "Game unavailable" dialog. |
| EC-2 | Concurrent pause/resume | `GameInstance` uses `ReentrantLock`. Second request fails with `IllegalStateException` → `409 Conflict`. |
| EC-3 | Owner kicks themselves | `kickPlayer()` checks `targetProfileId == requestingProfileId` → `400 Bad Request`. |
| EC-4 | Kick player not in game | `GameInstance.kickPlayer()` returns false if not found → service returns `404 Not Found`. |
| EC-5 | Cancel while hand in progress | `GameInstance.cancel()` calls `director.shutdown()` via `volatile shutdownRequested` flag. Director exits on next loop iteration. `GAME_COMPLETE` broadcast with cancel reason. WebSocket connections closed. |
| EC-6 | Pause not propagated to WebSocket clients | `GameInstance.pauseAsUser()` publishes to `GameEventBus` → `GameEventBroadcaster` sends `GAME_PAUSED` to all connected clients. No direct `GameService` → `GameConnectionManager` dependency. |
| EC-7 | Community status update wrong order | `updateGameStatus()` validates: only WAITING→IN_PROGRESS and IN_PROGRESS→COMPLETED allowed. All others → `409 Conflict`. |
| EC-8 | JWT expires during long community game | `RestGameClient` checks token expiry before each call. If expiring within 5 minutes, re-authenticates using local identity at `~/.ddpoker/local-identity.properties` (already used by `EmbeddedGameServer`). |
| EC-9 | Too many games accumulate in list | `GET /api/v1/games` default filter: only WAITING_FOR_PLAYERS and IN_PROGRESS. Historical games excluded unless `?status=COMPLETED` param provided. |
| EC-10 | Community WebSocket URL unreachable | `WebSocketGameClient` already has 10s connection timeout. Error shown as "Cannot connect to game" dialog. Same behavior as current P2P model. |
| EC-11 | `startGame` ownership fix breaks embedded server | Embedded server creates and starts games as the same local user (JWT profileId matches `ownerProfileId`). No regression. |
| EC-12 | Pause/resume REST for community game | Returns `400 Bad Request`: "Pause/resume community-hosted games via your embedded server." |
| EC-13 | Kicked player attempts WebSocket reconnect | `ServerPlayerSession.kicked = true`. `GameWebSocketHandler.afterConnectionEstablished()` checks this flag and rejects with error message. |
| EC-14 | `register-community` called twice for same game | Second call creates a new UUID (idempotency not required). Desktop client must deregister the first before registering again, or the first becomes stale and is cleaned up automatically. |

---

## Test Plan

### Unit Tests (test-first)

**`GameSummaryTest`** — new fields present; null `websocketUrl` for SERVER type

**`GameServiceTest` additions:**
- `listGames_includesHostingTypeAndWebsocketUrl`
- `listGames_excludesCompletedGames`
- `registerCommunityGame_createsEntityWithCorrectFields`
- `updateGameStatus_allowedTransitions` (WAITING→IN_PROGRESS, IN_PROGRESS→COMPLETED)
- `updateGameStatus_forbiddenTransitions_throws409`
- `updateGameStatus_notOwner_throws403`
- `updateGameStatus_serverHostedGame_throws400`
- `startGame_ownershipCheck_notOwner_throws403`
- `pauseGame_serverHostedOnly` (community → 400)
- `pauseGame_wrongState_throws409`
- `cancelGame_serverHosted_updatesDb`
- `cancelGame_communityHosted_updatesDbOnly`
- `kickPlayer_ownerOnly`
- `kickPlayer_cannotKickSelf`
- `kickPlayer_notInGame_returns404`

**`GameControllerTest` additions:**
- HTTP 403 for `startGame` non-owner
- HTTP 200 for pause/resume/cancel/kick (mocked service)
- HTTP 403/409/400 for admin endpoints
- HTTP 201 for `register-community`

**`GameInstanceTest` additions:**
- Kick sets `kicked = true` on `ServerPlayerSession`
- Kicked player rejected on reconnect

**`StaleGameCleanupSchedulerTest`:**
- WAITING community game > 24h → CANCELLED
- IN_PROGRESS community game > 72h → CANCELLED
- SERVER games not cleaned up by scheduler

**Desktop client — `RestGameClientTest`:**
- `listGames_parsesHostingTypeAndUrl`
- `registerCommunityGame_postsCorrectJson`
- `deregisterGame_callsDeleteEndpoint`
- Error response → throws informative exception

**Desktop client — `RemoteGameSummaryConverterTest`:**
- SERVER game: `websocketUrl` null → constructs `wss://{server}/ws/games/{id}`
- COMMUNITY game: uses provided `websocketUrl` directly
- `WAITING_FOR_PLAYERS` → `MODE_REG`
- `IN_PROGRESS` → `MODE_PLAY`

### Integration Tests

**`GameDiscoveryIntegrationTest`:**
- Create SERVER game → `GET /api/v1/games` includes `hostingType="SERVER"`
- Register COMMUNITY game → `GET /api/v1/games` returns both types
- COMPLETED/CANCELLED games excluded from default listing

**`GameAdminIntegrationTest`:**
- Start game (ownership check) → pause → verify PAUSED in DB
- Pause → resume → verify IN_PROGRESS
- Create game → cancel → verify CANCELLED; WebSocket connections closed
- Start game → kick player → player cannot reconnect

**`StaleGameCleanupIntegrationTest`:**
- Insert community game with `created_at = 25 hours ago` → run scheduler → verify CANCELLED

### End-to-End (Manual)

- Desktop Find Games screen loads from REST, shows Server/Community column
- Desktop community host: game appears in list with "Community" badge
- Desktop: join server-hosted game from list
- REST: `GET /api/v1/games` returns merged list with correct fields
- Owner: pause via REST → connected WebSocket clients receive `GAME_PAUSED`
- Owner: kick player via REST → player's WebSocket closed, reconnect rejected
- Stale game cleanup runs and removes old community registrations

---

## Files to Create

### `pokergameserver` module

| File | Purpose |
|------|---------|
| `controller/AdminGameController.java` | Pause, resume, cancel (DELETE), kick endpoints |
| `controller/CommunityGameController.java` | Register-community, deregister, PUT status |
| `dto/RegisterCommunityGameRequest.java` | Community registration request |
| `dto/UpdateGameStatusRequest.java` | Status update request |
| `dto/KickPlayerRequest.java` | Kick request |
| `dto/GameAdminResponse.java` | Success response wrapper |
| `scheduler/StaleGameCleanupScheduler.java` | Hourly stale game cleanup |

### `poker` module (desktop)

| File | Purpose |
|------|---------|
| `online/RestGameClient.java` | HTTP client for WAN server REST calls |
| `online/RemoteGameSummary.java` | Client-side DTO |
| `online/RemoteGameSummaryConverter.java` | Maps RemoteGameSummary → OnlineGame |
| `online/CommunityGameRegistration.java` | Community game WAN registration lifecycle |

---

## Files to Modify

### `pokergameserver` module

| File | Change |
|------|--------|
| `dto/GameSummary.java` | Add `hostingType`, `websocketUrl` |
| `dto/GameStateResponse.java` | Add `hostingType`, `websocketUrl`, `ownerName`, `ownerProfileId` |
| `service/GameService.java` | New admin/community methods; fix `startGame` ownership; update list/getState DTOs |
| `controller/GameController.java` | Fix `startGame` to pass auth user; add `assertOwner()` helper |
| `persistence/entity/GameInstanceEntity.java` | Add `externalWsUrl` field |
| `persistence/repository/GameInstanceRepository.java` | Add `findByStatusIn()` |
| `GameInstanceManager.java` | Add `pauseGame`, `resumeGame`, `cancelGame`, `kickPlayer` facades |
| `GameInstance.java` | Add `kickPlayer()` method; add `kicked` flag to `ServerPlayerSession` |
| `websocket/InboundMessageRouter.java` | Extract `handleAdminKick` logic to `GameInstance.kickPlayer()` |
| `GameServerExceptionHandler.java` | Ensure 403/409/400 mappings for new error cases |
| `config/GameServerProperties.java` | Add stale game threshold properties |

### `poker` module (desktop)

| File | Change |
|------|--------|
| `online/FindGames.java` | Replace `getWanList()` with REST; add "Hosting" column to `WanGameModel` |
| `model/OnlineGame.java` (pokerengine) | Add transient `hostingType` field for display |

### Files NOT Modified

- `wan_game` table / `OnlineGame` JPA persistence — M7 concern
- `PokerServlet.java`, `GetWanList.java` — legacy path, M7 concern
- `WebSocketGameClient.java` — WebSocket connection unchanged
- `EmbeddedGameServer.java` — lifecycle unchanged
- `ListGames.java` — visual layout preserved (column added only in subclass)
- `GameWebSocketHandler.java` — connection handling unchanged (reconnect check is in existing handler)

---

## Implementation Order

1. **Schema + Entity + DTOs** — `GameInstanceEntity.externalWsUrl`, new `GameSummary` fields, new request/response DTOs. Write tests first.
2. **List games + game state** — update `GameService.listGames()` and `getGameState()` to include new fields. Run existing tests; verify they still pass.
3. **Fix `startGame` ownership** — add `assertOwner()` helper; update `GameController.startGame()` and `GameService.startGame()`. Write test first.
4. **Admin endpoints** — pause, resume, cancel, kick. Extend `GameInstance` with `kickPlayer()` and `kicked` flag; extend `GameInstanceManager` with facades; create `AdminGameController`. Write tests first.
5. **Community registration** — `registerCommunityGame`, `deregisterGame`, `updateGameStatus`; create `CommunityGameController`. Write tests first.
6. **Stale cleanup** — `StaleGameCleanupScheduler` + `GameServerProperties` thresholds. Write test first.
7. **Desktop client** — `RestGameClient`, `RemoteGameSummary`, `RemoteGameSummaryConverter`, modify `FindGames`. Write tests first.
8. **Community hosting flow** — `CommunityGameRegistration` wired into community game start/stop path.

---

## Verification

```bash
# Run all pokergameserver tests
mvn test -pl code/pokergameserver

# Run all poker module tests
mvn test -pl code/poker

# Full build with coverage
mvn verify -P coverage

# Spot check API manually (embedded server)
curl -H "Authorization: Bearer $JWT" http://localhost:{port}/api/v1/games
# → should include hostingType and websocketUrl in each entry

# Verify ownership fix
curl -X POST -H "Authorization: Bearer $OTHER_JWT" http://localhost:{port}/api/v1/games/{id}/start
# → 403 Forbidden
```
