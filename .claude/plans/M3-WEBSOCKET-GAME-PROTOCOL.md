# Milestone 3: WebSocket Game Protocol — Detailed Plan

**Status:** COMPLETE
**Created:** 2026-02-16
**Parent Plan:** `.claude/plans/SERVER-HOSTED-GAME-ENGINE.md`
**Depends On:** M1 (complete), M2 (complete, merged)

---

## Context

M1 built the server game engine (GameInstance, ServerTournamentDirector, ServerPlayerActionProvider with CompletableFuture blocking, ServerGameEventBus with broadcastCallback, GameStateProjection for card privacy). M2 added JWT authentication, REST API, and database persistence. All 334 tests pass.

**What's missing:** There is no way for clients to actually play poker. The game engine runs server-side and fires events, but nothing delivers them to clients in real-time. Players can't submit actions during gameplay.

**What M3 adds:** Real-time WebSocket communication so clients can connect to a game, receive state updates (cards dealt, player actions, level changes), and submit their own actions (fold, call, raise). This is the bridge between the server game engine and any client (desktop, web, mobile).

---

## Architecture Decision: Raw WebSocket over STOMP

The master plan specifies "JSON text frames" with a per-game room model. STOMP would add topic subscription, message broker, and SockJS fallback — none of which fit. A raw `TextWebSocketHandler` maps directly to the per-game room design: gameId in the URL path, handler dispatches to the right GameInstance. Simpler, more testable, full control over the protocol.

---

## Existing Integration Points

The M1/M2 infrastructure already provides hooks for WebSocket integration:

**1. Player Action Flow (CompletableFuture):**
```
Director thread → engine.getAction(player, options)
→ ServerPlayerActionProvider.getHumanAction() creates CompletableFuture, blocks
→ actionRequestCallback sends ActionRequest to GameInstance
→ GameInstance calls session.messageSender.accept(actionRequest)
→ [M3: WebSocket sends ACTION_REQUIRED to client]
→ [M3: Client responds via WebSocket PLAYER_ACTION]
→ [M3: Handler calls GameInstance.onPlayerAction(profileId, action)]
→ submitAction(playerId, action) completes future, director unblocks
```

**2. Event Broadcasting:**
```
Director publishes events → ServerGameEventBus.publish()
→ eventStore.append() (persistence)
→ broadcastCallback (Consumer<GameEvent>)
→ [M3: GameEventBroadcaster converts events to JSON, sends via WebSocket]
```

**3. State Projection:**
```
GameStateProjection.forPlayer(table, hand, playerId)
→ Returns GameStateSnapshot with only that player's hole cards
→ [M3: Serialize to JSON, send as GAME_STATE message]
```

**4. Session Message Sender:**
```
ServerPlayerSession.setMessageSender(Consumer<Object> sender)
→ [M3: Connection manager sets this to a lambda that sends via WebSocket]
```

**Key type distinction:** `ServerPlayerActionProvider.submitAction()` takes `int playerId` (game-internal player ID), not `long profileId`. The mapping between profile IDs and game player IDs is through `ServerPlayerSession` and `GameInstance.onPlayerAction(long profileId, PlayerAction action)` which handles this internally.

---

## Phase Breakdown

### Phase 3.1: WebSocket Infrastructure
WebSocket config, handler, connection manager, auto-configuration, JWT auth on handshake.

### Phase 3.2: Server-to-Client Messages
Message records, event-to-message conversion, private vs broadcast routing, GameStateProjection serialization.

### Phase 3.3: Client-to-Server Messages
Inbound deserialization, PLAYER_ACTION dispatch, chat, admin commands with owner validation.

### Phase 3.4: Security & Testing
Rate limiting, sequence numbers, card privacy verification tests.

---

## Phase 3.1: WebSocket Infrastructure

### New Files

All under `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/`

**1. `GameWebSocketConfig.java`** — Spring WebSocket endpoint registration
```java
@Configuration
@EnableWebSocket
public class GameWebSocketConfig implements WebSocketConfigurer {
    // Registers handler at /ws/games/{gameId}
    void registerWebSocketHandlers(WebSocketHandlerRegistry registry)
}
```

**2. `GameWebSocketHandler.java`** — Connection lifecycle + message routing
- `afterConnectionEstablished()`: Extract JWT from `?token=<JWT>` query param, validate via `JwtTokenProvider`, look up GameInstance, register connection, send CONNECTED with full game state snapshot
- `handleTextMessage()`: Deserialize JSON, delegate to `InboundMessageRouter`
- `afterConnectionClosed()`: Unregister connection, mark player disconnected (auto-fold on their turn until reconnect)
- Dependencies: `JwtTokenProvider`, `GameInstanceManager`, `GameConnectionManager`, `InboundMessageRouter`, `ObjectMapper`

**3. `GameConnectionManager.java`** — Per-game connection tracking + disconnect state (thread-safe)
```java
// gameId -> Map<profileId, PlayerConnection>
ConcurrentHashMap<String, ConcurrentHashMap<Long, PlayerConnection>> connections

void addConnection(String gameId, long profileId, PlayerConnection connection)
void removeConnection(String gameId, long profileId)
void sendToPlayer(String gameId, long profileId, ServerMessage message)
void broadcastToGame(String gameId, ServerMessage message)
void broadcastToGame(String gameId, ServerMessage message, long excludeProfileId)
Collection<PlayerConnection> getConnections(String gameId)
```
On `addConnection`: Sets `messageSender` on `ServerPlayerSession`, calls `session.connect()`, broadcasts `PLAYER_JOINED`.
On `removeConnection`: Calls `session.disconnect()`, broadcasts `PLAYER_LEFT` (disconnect status visible to all players).

**4. `PlayerConnection.java`** — Single player's WebSocket session wrapper
```java
WebSocketSession session
long profileId
String username
String gameId
ObjectMapper objectMapper
volatile long lastActionTimestamp   // For rate limiting
volatile long lastSequenceNumber   // Anti-replay

void sendMessage(ServerMessage message)
boolean isOpen()
void close()
```

**5. `WebSocketAutoConfiguration.java`** — Auto-configuration
```java
@AutoConfiguration(after = GameServerAutoConfiguration.class)
@ConditionalOnWebApplication
@ConditionalOnProperty(name = "game.server.enabled", havingValue = "true", matchIfMissing = true)
// Creates: GameConnectionManager, GameWebSocketHandler, GameWebSocketConfig,
//          InboundMessageRouter, OutboundMessageConverter, GameEventBroadcaster,
//          RateLimiter
```

### Files to Modify

**`pom.xml`** — Add `spring-boot-starter-websocket` (optional, like existing starter-web)

**`META-INF/.../AutoConfiguration.imports`** — Add `WebSocketAutoConfiguration`

**`GameServerSecurityAutoConfiguration.java`** — Permit `/ws/**` through security filter (WebSocket auth handled in handler, not servlet filter):
```java
.requestMatchers("/ws/**").permitAll()
```

**`GameInstance.java`** — Add public accessors:
```java
public ServerGameEventBus getEventBus()
public Map<Long, ServerPlayerSession> getPlayerSessions()  // unmodifiable
```

### Authentication Flow

1. Client opens: `ws://server/ws/games/{gameId}?token=<JWT>`
2. Handler extracts `token` from query string
3. `jwtTokenProvider.validateToken(token)` — if invalid, close with `CloseStatus(4001, "Invalid token")`
4. Extract `profileId` and `username` from token claims
5. Look up `GameInstance` — if not found, close with `CloseStatus(4004, "Game not found")`
6. If game is WAITING_FOR_PLAYERS and player not yet in game → auto-join
7. If game is IN_PROGRESS and player was disconnected → reconnect
8. If player has no valid relationship to game → close with `CloseStatus(4003, "Not in game")`
9. Create `PlayerConnection`, register in `GameConnectionManager`
10. Set `messageSender` on `ServerPlayerSession` (sends ACTION_REQUIRED via WebSocket)
11. Send CONNECTED message with `GameStateProjection.forPlayer()` snapshot

---

## Phase 3.2: Server-to-Client Messages

### New Files

All under `.../websocket/message/`

**6. `ServerMessage.java`** — Envelope record
```java
public record ServerMessage(ServerMessageType type, String gameId, Instant timestamp, Object data) {
    public static ServerMessage of(ServerMessageType type, String gameId, Object data)
}
```

**7. `ServerMessageType.java`** — Enum (22 types)
```
CONNECTED, GAME_STATE,
HAND_STARTED, HOLE_CARDS_DEALT, COMMUNITY_CARDS_DEALT,
ACTION_REQUIRED, PLAYER_ACTED, ACTION_TIMEOUT, HAND_COMPLETE,
LEVEL_CHANGED, PLAYER_ELIMINATED, REBUY_OFFERED, ADDON_OFFERED, GAME_COMPLETE,
PLAYER_JOINED, PLAYER_LEFT, GAME_PAUSED, GAME_RESUMED,
PLAYER_KICKED, CHAT_MESSAGE, TIMER_UPDATE, ERROR
```

**8. `ServerMessageData.java`** — All server message payload records (sealed interface hierarchy matching master plan JSON spec exactly). Key records:
- `ConnectedData(long playerId, Object gameState)`
- `HandStartedData(int handNumber, int dealerSeat, ...)`
- `HoleCardsDealtData(List<String> cards)` — cards as "Ah", "Kd" via `Card.getDisplay()`
- `ActionRequiredData(int timeoutSeconds, ActionOptionsData options)`
- `PlayerActedData(long playerId, String playerName, String action, int amount, ...)`
- `HandCompleteData(int handNumber, List<WinnerInfo> winners, List<ShowdownPlayer> showdownPlayers)`
- `ErrorData(String code, String message)`
- ...all 22 data records per master plan spec

**9. `OutboundMessageConverter.java`** — Converts internal game state → ServerMessage records
```java
ServerMessage createConnectedMessage(String gameId, long profileId, GameStateSnapshot snapshot)
ServerMessage createActionRequiredMessage(String gameId, ActionOptions options, int timeoutSeconds)
ServerMessage createPlayerActedMessage(String gameId, GameEvent.PlayerActed event, ...)
// ... factory method per message type
static String cardToString(Card card)  // Uses Card.getDisplay()
```

**10. `GameEventBroadcaster.java`** — Bridge: `ServerGameEventBus` → WebSocket
- Implements `Consumer<GameEvent>` (set as broadcastCallback)
- Maps each of the 21 `GameEvent` sealed types to appropriate ServerMessage(s)
- Routes: most events broadcast to all; hole cards and ACTION_REQUIRED go to specific player only
- Critical privacy routing:
  - `HandStarted` → broadcast
  - `PlayerActed` → broadcast
  - `CommunityCardsDealt` → broadcast
  - `ShowdownStarted` → broadcast (with revealed cards for non-folded players)
  - `PotAwarded` → broadcast
  - `LevelChanged` → broadcast
  - `TournamentCompleted` → broadcast
  - `CurrentPlayerChanged` → private ACTION_REQUIRED to that player only
  - Hole cards → NEVER broadcast, private to card owner only

### Files to Modify

**`GameInstance.java`** — Wire `GameEventBroadcaster` as broadcastCallback on `eventBus` during `start()`.

**`GameStateProjection.java`** — Enhance `forShowdown()` to reveal non-folded players' hole cards (currently just delegates to `forPlayer()`).

---

## Phase 3.3: Client-to-Server Messages

### New Files

**11. `ClientMessage.java`** — Envelope
```java
public record ClientMessage(ClientMessageType type, long sequenceNumber, Object data)
```

**12. `ClientMessageType.java`** — Enum (9 types)
```
PLAYER_ACTION, REBUY_DECISION, ADDON_DECISION, CHAT,
SIT_OUT, COME_BACK, ADMIN_KICK, ADMIN_PAUSE, ADMIN_RESUME
```

**13. `ClientMessageData.java`** — Client payload records (sealed interface)
- `PlayerActionData(String action, int amount)` — action: FOLD/CHECK/CALL/BET/RAISE/ALL_IN
- `RebuyDecisionData(boolean accept)`
- `AddonDecisionData(boolean accept)`
- `ChatData(String message, boolean tableChat)`
- `AdminKickData(long playerId)`
- SIT_OUT, COME_BACK, ADMIN_PAUSE, ADMIN_RESUME have no data fields

**14. `InboundMessageRouter.java`** — Dispatches client messages
```java
void handleMessage(PlayerConnection connection, String rawJson)
```
- Deserializes JSON envelope
- Validates sequence number (anti-replay)
- Checks rate limit
- Switches on type:
  - `PLAYER_ACTION` → map action string to `PlayerAction`, call `gameInstance.onPlayerAction(profileId, action)`
  - `CHAT` → sanitize, broadcast `ChatMessageData` to all
  - `SIT_OUT` / `COME_BACK` → update player state
  - `ADMIN_KICK` → verify owner, remove player, broadcast, close kicked session
  - `ADMIN_PAUSE` → verify owner, call `gameInstance.pauseAsUser(profileId)`
  - `ADMIN_RESUME` → verify owner, call `gameInstance.resume()`
  - Invalid type → send ERROR message

---

## Phase 3.4: Security & Testing

### New Files

**15. `RateLimiter.java`** — Per-player rate limiting
```java
ConcurrentHashMap<Long, Long> lastActionTimestamps
long minIntervalMillis  // Default: 1000 (1 action/sec)

boolean allowAction(long profileId)
void removePlayer(long profileId)
```

### Files to Modify

**`GameServerProperties.java`** — Add fields to the record:
- `int rateLimitMillis` (default: 1000)
- `int consecutiveTimeoutLimit` (default: 3)
- `int disconnectGraceTurns` (default: 2)

### Player Identity Validation

Player actions are validated server-side through a chain of guarantees — no client-supplied player ID is ever trusted:

1. **JWT Authentication on Connect:** The WebSocket handshake extracts `profileId` from the JWT token (signed with RS256). The `PlayerConnection` stores this server-derived `profileId`. There is no mechanism for a client to claim a different identity.

2. **Server-Derived Routing:** When `InboundMessageRouter` processes a `PLAYER_ACTION` message, it uses `connection.getProfileId()` (from the JWT, set at connection time) — NOT any field from the client message body. The client message contains only the action (FOLD/CALL/RAISE/etc.) and amount.

3. **CompletableFuture Gating:** `GameInstance.onPlayerAction(profileId, action)` maps the profileId to the internal `ServerPlayerSession`, then calls `submitAction(playerId, action)`. This only succeeds if there is a **pending CompletableFuture** for that specific player — meaning the game engine is actively waiting for that player's action. If player A tries to submit an action when it's player B's turn, there is no pending future for player A, and the action is rejected.

4. **Single Connection Per Player:** `GameConnectionManager` maintains one connection per profileId per game. A new connection for the same profileId replaces the old one (reconnect).

### Reconnection Model

**No timer-based reconnection.** Disconnected players can reconnect anytime the game is still active:

- On disconnect: Mark player as disconnected in `GameConnectionManager`. Broadcast `PLAYER_LEFT` to other players.
- While disconnected: Player is flagged as disconnected. When it's their turn, `ServerPlayerActionProvider` auto-folds immediately (no action timeout wait). This keeps the game moving without penalizing connected players.
- On reconnect: Validate JWT, verify player is in the game, create new `PlayerConnection`, send full `GameStateProjection.forPlayer()` snapshot via CONNECTED message, broadcast `PLAYER_JOINED` to others. Resume normal play.
- Game ends: If the player never reconnects, they are eliminated normally through auto-folds.

**Disconnect-aware action handling:** `ServerPlayerSession` already tracks `disconnected` state and `consecutiveTimeouts` counter (from M1). `ServerPlayerActionProvider.getHumanAction()` will be modified to check disconnect state:
- If player is disconnected AND `consecutiveTimeouts < disconnectGraceTurns` (default: 2) → normal timeout flow (wait the full action timeout, giving them a chance to reconnect during the timeout window, then auto-check/fold)
- If player is disconnected AND `consecutiveTimeouts >= disconnectGraceTurns` → return `PlayerAction.fold()` immediately (no timeout wait, keeps game moving)
- The disconnect state is exposed to other players via `PLAYER_LEFT` broadcast message
- On reconnect, the player's disconnect state is cleared, `consecutiveTimeouts` resets, and they resume normal play

This eliminates the need for `ReconnectionManager` and its associated timer infrastructure.

### Additional Security Hardening (items 1-7)

These are security gaps discovered during M3 planning that will be addressed as part of this milestone:

**1. Add `resumeAsUser()` to GameInstance** — `resume()` currently has no owner authorization wrapper, unlike `startAsUser()`, `pauseAsUser()`, and `cancelAsUser()`. Add `resumeAsUser(long userId)` with `checkOwnership()`. Update `InboundMessageRouter` to call `resumeAsUser()` instead of `resume()`.

**2. Per-user game creation limit** — `GameInstanceManager.createGame()` has a global cap (50 games) but no per-user limit. Add a per-user cap (e.g., 5 active games). Count existing games by ownerProfileId before allowing creation.

**3. Chat input sanitization** — `InboundMessageRouter` CHAT handler must enforce:
- Max message length (500 chars), reject longer
- Strip HTML/script tags (prevent stored XSS if messages are displayed in web client)
- Separate chat rate limit (5 messages per 10 seconds per player)

**4. WebSocket connection limits** — Cap connections per user globally (not just per game). `GameConnectionManager` already enforces 1 per game via single-connection-per-player, but add a global per-user cap (e.g., 10 simultaneous connections across all games).

**5. WebSocket message size limit** — Configure `setMaxTextMessageBufferSize()` on the WebSocket handler (8KB). Prevents malicious clients from sending oversized JSON payloads.

**6. Login rate limiting** — Add rate limiting to `/api/v1/auth/login` endpoint (5 attempts per IP per 15 minutes). Implemented as a simple in-memory filter. Pre-existing M2 gap, but straightforward to add now.

**7. DTO input validation** — Add Bean Validation annotations to existing M2 DTOs:
- `RegisterRequest`: `@NotBlank` on all fields, `@Size(min=3, max=50)` on username, `@Size(min=8, max=128)` on password, `@Email` on email
- `LoginRequest`: `@NotBlank` on username/password
- `UpdateProfileRequest`: `@Size` constraints
- Add `@Valid` on controller method parameters
- Add username character whitelist (alphanumeric + underscore/hyphen)

### Security Rules

| Rule | Implementation |
|------|----------------|
| Player identity | Server-derived `profileId` from JWT, never client-supplied. CompletableFuture gating ensures actions only accepted when it's that player's turn. |
| Hole cards private | `GameEventBroadcaster` routes hole card events to owner only. `GameStateProjection.forPlayer()` strips other players' cards. |
| Action validation | `ServerPlayerActionProvider.submitAction()` validates action against pending `ActionOptions`. Invalid actions rejected. |
| Rate limiting | `RateLimiter` checks timestamp gap. Rejected → ERROR message. Chat has separate rate limit. |
| Timeout | `ServerPlayerActionProvider` CompletableFuture timeout → auto-check/fold. 3 consecutive → auto sit-out. |
| Disconnected player | First 2 turns: normal timeout (grace period for reconnect). After that: auto-fold immediately (skip timeout wait). Reconnect allowed anytime game is active. |
| Anti-replay | `PlayerConnection` tracks `lastSequenceNumber`. Rejects out-of-order. |
| Owner validation | Admin commands check `connection.profileId == gameInstance.getOwnerProfileId()`. `resume()` now requires ownership. |
| Input validation | Bean Validation on all DTOs. Chat sanitized (length, HTML stripping). WebSocket frame size capped at 8KB. |
| Resource limits | Per-user game creation cap (5). Per-user WebSocket connection cap (10). Login rate limiting (5/15min/IP). |

---

## Complete File Inventory

### New Production Files (16)

| # | Path (under `.../gameserver/`) | Purpose |
|---|-------------------------------|---------|
| 1 | `websocket/GameWebSocketConfig.java` | Spring WebSocket endpoint registration |
| 2 | `websocket/GameWebSocketHandler.java` | Connection lifecycle, JWT auth, message routing |
| 3 | `websocket/GameConnectionManager.java` | Per-game connection tracking + disconnect state |
| 4 | `websocket/PlayerConnection.java` | Player WebSocket session wrapper |
| 5 | `websocket/WebSocketAutoConfiguration.java` | Spring auto-configuration |
| 6 | `websocket/message/ServerMessage.java` | Server→client envelope |
| 7 | `websocket/message/ServerMessageType.java` | Server message type enum |
| 8 | `websocket/message/ServerMessageData.java` | Server payload records (sealed) |
| 9 | `websocket/OutboundMessageConverter.java` | Game state → ServerMessage conversion |
| 10 | `websocket/GameEventBroadcaster.java` | EventBus → WebSocket bridge |
| 11 | `websocket/message/ClientMessage.java` | Client→server envelope |
| 12 | `websocket/message/ClientMessageType.java` | Client message type enum |
| 13 | `websocket/message/ClientMessageData.java` | Client payload records (sealed) |
| 14 | `websocket/InboundMessageRouter.java` | Client message dispatch + chat sanitization |
| 15 | `websocket/RateLimiter.java` | Per-player action + chat rate limiting |
| 16 | `auth/LoginRateLimitFilter.java` | IP-based login attempt rate limiting (5/15min) |

### Modified Files (12)

| # | File | Changes |
|---|------|---------|
| 1 | `pom.xml` | Add `spring-boot-starter-websocket`, `spring-boot-starter-validation` |
| 2 | `AutoConfiguration.imports` | Add `WebSocketAutoConfiguration` |
| 3 | `GameServerSecurityAutoConfiguration.java` | Permit `/ws/**`; add login rate limit filter |
| 4 | `GameInstance.java` | Add `getEventBus()`, `getPlayerSessions()` accessors; wire broadcaster in `start()`; add `resumeAsUser()` |
| 5 | `GameStateProjection.java` | Enhance `forShowdown()` to reveal cards |
| 6 | `GameServerProperties.java` | Add `rateLimitMillis`, `consecutiveTimeoutLimit`, `disconnectGraceTurns`, `maxGamesPerUser` |
| 7 | `ServerPlayerActionProvider.java` | Check disconnect state in `getHumanAction()` — 2-turn grace period with normal timeouts, then instant auto-fold |
| 8 | `GameInstanceManager.java` | Add per-user game creation limit check |
| 9 | `RegisterRequest.java` | Add `@NotBlank`, `@Size`, `@Email`, `@Pattern` validation annotations |
| 10 | `LoginRequest.java` | Add `@NotBlank` validation annotations |
| 11 | `UpdateProfileRequest.java` | Add `@Size` validation annotations |
| 12 | `AuthController.java` / `ProfileController.java` / `GameController.java` | Add `@Valid` on request body parameters |

---

## Testing Strategy

### Unit Tests (~14 test files, TDD)

| Test | What It Covers |
|------|----------------|
| `PlayerConnectionTest` | Message sending, session lifecycle, sequence tracking |
| `GameConnectionManagerTest` | Add/remove connections, sendToPlayer, broadcastToGame, disconnect state, reconnect, per-user connection cap, thread safety |
| `GameWebSocketHandlerTest` | JWT validation (valid/invalid/missing), game lookup, reconnection, disconnect, message size limit |
| `OutboundMessageConverterTest` | All message conversions, card serialization, no card leakage |
| `GameEventBroadcasterTest` | Event routing (broadcast vs private), hole card privacy, showdown reveal |
| `ServerMessageSerializationTest` | JSON round-trip for every message type |
| `InboundMessageRouterTest` | Action dispatch, player identity validation, admin command owner validation (including resume), chat sanitization (length, HTML stripping), invalid messages |
| `ClientMessageDeserializationTest` | JSON parsing for all client message types |
| `RateLimiterTest` | Action rate limiting, chat rate limiting, per-player isolation, concurrent access |
| `CardPrivacyTest` | Verify hole cards NEVER appear in wrong player's messages |
| `LoginRateLimitFilterTest` | IP-based rate limiting, limit reset, concurrent requests |
| `GameInstanceManagerSecurityTest` | Per-user game creation limit enforcement |
| `DtoValidationTest` | Bean Validation on RegisterRequest, LoginRequest, UpdateProfileRequest (blank fields, length limits, email format, username pattern) |
| `WebSocketIntegrationTest` | Full lifecycle: connect → play hand → disconnect → reconnect |

### Integration Test Approach

`WebSocketIntegrationTest` uses `@SpringBootTest(webEnvironment = RANDOM_PORT)` with Java's `java.net.http.HttpClient` WebSocket API (or Spring's `StandardWebSocketClient`). Tests two players connecting, game starting, hand playing through ACTION_REQUIRED → PLAYER_ACTION → PLAYER_ACTED cycle.

---

## Implementation Sequence (TDD)

**Step 1 (Phase 3.1):** Add dependency → message envelope records → PlayerConnection → GameConnectionManager → GameWebSocketHandler + Config → WebSocketAutoConfiguration → security permit → GameInstance accessors

**Step 2 (Phase 3.2):** ServerMessageData records → OutboundMessageConverter → GameEventBroadcaster → wire broadcaster → enhance GameStateProjection.forShowdown()

**Step 3 (Phase 3.3):** ClientMessageData records → InboundMessageRouter → wire into handler

**Step 4 (Phase 3.4):** RateLimiter → GameServerProperties fields → wire into handler/router → card privacy tests → integration test → security hardening (resumeAsUser, per-user limits, chat sanitization, WS frame size, login rate limit, DTO validation)

---

## Configuration

```properties
# Existing (unchanged)
game.server.max-concurrent-games=50
game.server.action-timeout-seconds=30
game.server.reconnect-timeout-seconds=120  # Still used by GameServerProperties but no timer enforcement
game.server.thread-pool-size=10

# New for M3
game.server.rate-limit-millis=1000
game.server.consecutive-timeout-limit=3
game.server.disconnect-grace-turns=2
```

---

## Dependencies

**One new Maven dependency:**
- `spring-boot-starter-websocket` (version `${spring-boot.version}` = 3.5.8, optional)
- Brings: `spring-websocket`, `spring-messaging`
- Jackson already present via `spring-boot-starter-web`
- No new test dependencies needed

---

## Verification

1. **All 334 existing tests pass** — no regressions from M1/M2
2. **New unit tests pass** — ~14 test classes covering all WebSocket + security functionality
3. **Integration test passes** — full game lifecycle over WebSocket
4. **Card privacy verified** — automated test that hole cards never leak to wrong player
5. **Build clean** — `mvn test -pl pokergameserver -P dev` succeeds with zero warnings
6. **Manual smoke test** — connect via `websocat` or similar tool, send JSON, verify responses
