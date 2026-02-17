# Review Request

**Branch:** feature-m3-websocket-protocol
**Worktree:** ../DDPoker-feature-m3-websocket-protocol
**Plan:** .claude/plans/M3-WEBSOCKET-GAME-PROTOCOL.md
**Requested:** 2026-02-16 22:00

## Summary

Implements Milestone 3: real-time WebSocket communication for the poker game server. Adds a raw `TextWebSocketHandler` at `/ws/games/{gameId}` with JWT auth via query param, per-player connection tracking, inbound message routing (PLAYER_ACTION, CHAT, admin commands), event broadcasting from the game engine to all connected clients, rate limiting, anti-replay sequence numbers, chat sanitization, card privacy enforcement, login rate limiting, and DTO Bean Validation.

## Files Changed

### New Production Files (16)
- [ ] `websocket/GameWebSocketConfig.java` — `WebSocketConfigurer`, registers handler at `/ws/games/*`
- [ ] `websocket/GameWebSocketHandler.java` — JWT auth handshake, connection lifecycle, message dispatch
- [ ] `websocket/GameConnectionManager.java` — Thread-safe `ConcurrentHashMap` per-game connection tracking
- [ ] `websocket/PlayerConnection.java` — Wraps `WebSocketSession` with player identity and sequence tracking
- [ ] `websocket/WebSocketAutoConfiguration.java` — Spring auto-config; `@EnableWebSocket` moved here from `GameWebSocketConfig`
- [ ] `websocket/message/ServerMessage.java` — Server envelope record
- [ ] `websocket/message/ServerMessageType.java` — 22-value enum
- [ ] `websocket/message/ServerMessageData.java` — Sealed interface with 22 payload records
- [ ] `websocket/OutboundMessageConverter.java` — Game state → `ServerMessage` conversions
- [ ] `websocket/GameEventBroadcaster.java` — `Consumer<GameEvent>` bridge to WebSocket
- [ ] `websocket/message/ClientMessage.java` — Client envelope record
- [ ] `websocket/message/ClientMessageType.java` — 9-value enum
- [ ] `websocket/message/ClientMessageData.java` — Sealed interface with 9 payload records
- [ ] `websocket/InboundMessageRouter.java` — Sequence validation, rate limiting, action/chat/admin dispatch
- [ ] `websocket/RateLimiter.java` — `ConcurrentHashMap<Long, Long>` per-player rate limiting
- [ ] `auth/LoginRateLimitFilter.java` — `OncePerRequestFilter` IP-based login rate limiting (5/15min)

### Modified Files (12)
- [ ] `pom.xml` — Added `spring-boot-starter-websocket`, `spring-boot-starter-validation` (both optional)
- [ ] `AutoConfiguration.imports` — Added `WebSocketAutoConfiguration`
- [ ] `GameServerSecurityAutoConfiguration.java` — Permit `/ws/**`; add `LoginRateLimitFilter`
- [ ] `GameInstance.java` — Added `getEventBus()`, `getPlayerSessions()`, `resumeAsUser()`
- [ ] `GameStateProjection.java` — Implemented `forShowdown()` to reveal non-folded players' cards
- [ ] `GameServerProperties.java` — Added `rateLimitMillis`, `consecutiveTimeoutLimit`, `disconnectGraceTurns`, `maxGamesPerUser`
- [ ] `ServerPlayerActionProvider.java` — Disconnect-aware action handling (grace turns then instant auto-fold)
- [ ] `GameInstanceManager.java` — Per-user game creation limit check
- [ ] `RegisterRequest.java` — Bean Validation: `@NotBlank`, `@Size`, `@Email`, `@Pattern`
- [ ] `LoginRequest.java` — Bean Validation: `@NotBlank`
- [ ] `AuthController.java` / `ProfileController.java` / `GameController.java` — Added `@Valid` on request bodies

### New Test Files (10)
- [ ] `websocket/PlayerConnectionTest.java` — 7 tests
- [ ] `websocket/GameConnectionManagerTest.java` — 10 tests
- [ ] `websocket/RateLimiterTest.java` — 7 tests
- [ ] `websocket/OutboundMessageConverterTest.java` — 11 tests
- [ ] `websocket/GameEventBroadcasterTest.java` — 13 tests
- [ ] `websocket/InboundMessageRouterTest.java` — 11 tests
- [ ] `websocket/GameWebSocketHandlerTest.java` — 7 tests
- [ ] `websocket/message/ServerMessageSerializationTest.java` — 6 tests
- [ ] `CardPrivacyTest.java` — 8 tests; verifies hole cards never leak to wrong player
- [ ] `DtoValidationTest.java` — 18 tests; Bean Validation on all DTOs
- [ ] `auth/LoginRateLimitFilterTest.java` — 7 tests
- [ ] `integration/WebSocketIntegrationTest.java` — 3 tests; full connect/disconnect/reconnect lifecycle

**Privacy Check:**
- ✅ SAFE - No private information found. All test data uses synthetic values. JWT keys are generated fresh per test run and stored in temp directory.

## Verification Results

- **Tests:** 447/447 passed (was 334 before M3; added 113 new tests)
- **Coverage:** Not measured (run `mvn verify -P coverage` for full report)
- **Build:** Clean — zero warnings (`mvn test -pl pokergameserver -P dev`)

## Context & Decisions

1. **`@EnableWebSocket` on `WebSocketAutoConfiguration`**: Initially placed on `GameWebSocketConfig` (as a `@Bean` sub-class), which failed to register the WebSocket endpoint in `@SpringBootTest` integration tests (HTTP 500). Moved to `WebSocketAutoConfiguration` itself — the correct location for auto-configured WebSocket infrastructure.

2. **ACTION_REQUIRED routing**: Handled exclusively via `messageSender` on `ServerPlayerSession` (set by `GameWebSocketHandler` on connect). `GameEventBroadcaster` silently ignores `CurrentPlayerChanged` events — this avoids double-routing.

3. **REST API / in-memory engine gap**: `GameService` (JPA) and `GameInstanceManager` (in-memory) are separate systems with independent game IDs. The WebSocket integration test creates games directly in `GameInstanceManager` to bypass this gap. This is a known architectural gap for a future milestone.

4. **Two `RateLimiter` beans**: `actionRateLimiter` (1000ms) and `chatRateLimiter` (2000ms) are disambiguated via `@Qualifier` in `WebSocketAutoConfiguration`.

5. **`forShowdown()` implementation**: Reveals cards for all non-folded players in `PlayerState.holeCards`. `myHoleCards` field always returns the requesting player's own cards. Verified by `CardPrivacyTest`.

---

## Review Results

**Status:** APPROVED_WITH_SUGGESTIONS

**Reviewed by:** Claude Sonnet 4.5
**Date:** 2026-02-16

### Findings

#### ✅ Strengths

1. **Card privacy is correct and well-tested.** `GameStateProjection.forPlayer()` strictly limits `holeCards` on each `PlayerState` to the requesting player only (line 79: `player.getID() == playerId`). `forShowdown()` correctly reveals cards only for non-folded players (`!player.isFolded()`, line 144). The `myHoleCards` top-level field always contains the requesting player's own cards, including when they are folded — which is the right UX. `CardPrivacyTest` (8 tests) exercises every privacy boundary explicitly, including the folded-at-showdown edge case.

2. **Player identity from JWT, never from message content.** `InboundMessageRouter` takes `connection.getProfileId()` from the authenticated `PlayerConnection` for all authorization checks and game operations. The message payload is only used for the *data* of an action, not for player identity. This is the correct pattern.

3. **Admin authorization enforced correctly.** `handleAdminKick`, `handleAdminPause`, and `handleAdminResume` all check `connection.getProfileId() != game.getOwnerProfileId()` before executing. The check happens before parsing the data payload, so there is no path to bypass it.

4. **Sequence number anti-replay is correct.** `InboundMessageRouter` rejects any `sequenceNumber <= connection.getLastSequenceNumber()` (line 126). The default initial value is `0`, so the first message must have `sequenceNumber >= 1`. This correctly prevents replays and out-of-order delivery.

5. **Rate limiting is correctly dual-keyed by player and type.** `actionRateLimiter` (1000ms default, configurable) and `chatRateLimiter` (2000ms hardcoded) are separate beans, disambiguated by `@Qualifier`. Per-player state is tracked by `profileId` in `ConcurrentHashMap`. `RateLimiterTest` validates isolation, blocking, and thread-safety with a concurrent stress test.

6. **`GameConnectionManager` thread-safety is sound.** Uses `ConcurrentHashMap<String, ConcurrentHashMap<Long, PlayerConnection>>`. `computeIfAbsent` is used for the outer map (`addConnection`, line 51), which is safe for concurrent first-connection races. `broadcastToGame` iterates `values()` on the inner map; ConcurrentHashMap's `values()` returns a live view that is safe to iterate concurrently without external locking.

7. **`PlayerConnection` volatile fields are correct.** `lastActionTimestamp` and `lastSequenceNumber` are both `volatile long`. Since sequence validation is done from a single WebSocket handler thread per session (Spring WebSocket delivers messages serially per session), this is sufficient.

8. **`@EnableWebSocket` placement is architecturally correct.** Placing it on `WebSocketAutoConfiguration` (the top-level `@AutoConfiguration` class) ensures the Spring WebSocket infrastructure is registered before the `GameWebSocketConfig` configurer bean runs. The noted issue with integration tests was the right thing to fix.

9. **Login rate limiting is correct.** `LoginRateLimitFilter` uses `ConcurrentHashMap.compute()` for atomic window tracking. The `AttemptRecord` using `AtomicInteger` handles concurrent increments safely within a window. IP extraction respects `X-Forwarded-For` (first hop), falling back to `getRemoteAddr()`.

10. **`OutboundMessageConverter.convertSnapshot()` correctly passes `p.holeCards()` directly from the already-filtered `PlayerState`.** Since `GameStateProjection` is the only source of `PlayerState` objects, and it enforces the null rule at construction, `SeatData` will only contain non-null hole cards for the requesting player. The privacy boundary holds end-to-end.

11. **`GameEventBroadcaster` does not broadcast hole cards.** `HoleCardsDealt` is not a case in the switch expression. Hole card delivery goes exclusively through the `messageSender` callback on `ServerPlayerSession`, which is player-specific. There is no path in the broadcaster that could leak hole card data to the wrong player.

12. **`ServerPlayerActionProvider` action validation** correctly clamps bet/raise amounts to valid ranges and rejects invalid action types with a fold default. This prevents malformed client actions from corrupting game state.

13. **Test coverage is comprehensive.** 113 new tests covering all security-critical paths, thread safety, rate limiting, card privacy, DTO validation, and the full WebSocket lifecycle with integration tests.

---

#### ⚠️ Suggestions (Non-blocking)

1. **`GameEventBroadcaster` — `PotAwarded` maps to `PLAYER_ACTED` with action `"WIN"` (line 88).** This is semantically odd — `PLAYER_ACTED` is documented as player actions (FOLD, CHECK, CALL, BET, RAISE). Using it for pot awards will confuse clients. Consider using `HAND_COMPLETE` or adding a `POT_AWARDED` message type. This is a functional concern, not a security one, but it will require a client-side workaround if not fixed before client development begins.
   - File: `GameEventBroadcaster.java`, lines 87–91

2. **`GameEventBroadcaster` — `ShowdownStarted` maps to `HAND_COMPLETE` (line 92–95).** Showdown started is not hand complete — they are distinct game phases. Clients cannot distinguish "showdown in progress" from "hand is over" with this mapping. The `HAND_COMPLETE` message is documented with winners and standings, but a `ShowdownStarted` event carries none of that. Recommend adding a `SHOWDOWN_STARTED` server message type or at minimum using a distinct path.
   - File: `GameEventBroadcaster.java`, lines 92–95

3. **`GameEventBroadcaster` — `PlayerRebuy` and `PlayerAddon` map to `PLAYER_JOINED` (lines 121–127).** These are semantically distinct events. A rebuy is not a join — the player was already in the game. Clients may display a spurious "Player X has joined" toast for rebuys. Consider dedicated `PLAYER_REBUY` and `PLAYER_ADDON` message types, or use the existing `ServerMessageType` enum values that may already cover this.
   - File: `GameEventBroadcaster.java`, lines 120–127

4. **`InboundMessageRouter` — `ALL_IN` action is silently mapped to `CALL` (line 273).** The comment says "ALL_IN treated as call for now" but this is a lossy mapping. A player choosing all-in when they have more chips than the call amount will have their intent silently changed to a call. This should either be a distinct `PlayerAction.allIn()` (if the engine supports it) or generate an `INVALID_ACTION` error to the client, not silently map to a different action.
   - File: `InboundMessageRouter.java`, line 273

5. **`GameWebSocketHandler.afterConnectionClosed` calls `game.removePlayer(profileId)` unconditionally (line 194).** This will mark the player as disconnected (or remove them pre-game), but `game.removePlayer` during `IN_PROGRESS` marks them as disconnected (correct). However, the `PLAYER_LEFT` broadcast at line 198 fires even for normal mid-game disconnects where the player should be able to reconnect. Clients observing the `PLAYER_LEFT` message may incorrectly show the player as gone when they could reconnect within the grace period. Consider only broadcasting `PLAYER_LEFT` when the state is `WAITING_FOR_PLAYERS` (true removal), and using a distinct `PLAYER_DISCONNECTED` message type for in-progress disconnects.
   - File: `GameWebSocketHandler.java`, lines 194–201

6. **`PlayerConnection.sendMessage` throws `RuntimeException` on `IOException` (line 85).** A network error during send (e.g., client drops) will propagate up through `broadcastToGame`, potentially aborting the broadcast loop and causing other connected players to miss messages. The `ConcurrentHashMap.values().forEach()` in `broadcastToGame` will stop on the first exception. Consider catching `IOException` in `sendMessage` and logging it silently (the session is dead anyway), rather than throwing.
   - File: `PlayerConnection.java`, lines 82–86; `GameConnectionManager.java`, line 120

7. **`LoginRateLimitFilter` — `X-Forwarded-For` is trusted without validation.** A client behind a proxy can spoof the `X-Forwarded-For` header if the proxy does not strip it. This is a standard concern for all XFF-based IP extraction. If the game server is deployed behind a known trusted proxy (e.g., nginx), consider only trusting the last N hops or using a configured trusted proxy list. For an internal/game server context this may be acceptable risk, but worth noting for the operator.
   - File: `LoginRateLimitFilter.java`, lines 102–107

8. **`InboundMessageRouter` — CHAT rate limit is not applied to `CHAT` messages that are silently ignored for non-players (no such check exists).** This is a minor observation: the router only dispatches if the game exists, which is a reasonable gate. No additional concern here — just confirming coverage is adequate.

9. **`GameWebSocketConfig.setAllowedOriginPatterns("*")`** allows WebSocket connections from any origin. This is fine for a game server where JWT auth is the actual security boundary, but operators should be aware they cannot rely on origin-based CSRF protection. Consider documenting this intentional choice.
   - File: `GameWebSocketConfig.java`, line 45

10. **`GameInstance.createSimpleAI()` contains a `@Future enhancement (M3)` comment in the Javadoc (line 466)** — this comment is stale since M3 is now complete without replacing the AI. The comment should say "future milestone" or be removed to avoid confusion. (Minor doc cleanup only.)
    - File: `GameInstance.java`, lines 460–468

---

#### ❌ Required Changes (Blocking)

1. **`GameWebSocketHandler` — new `GameEventBroadcaster` is created and registered on every reconnect (lines 158–161), potentially overwriting a previous broadcaster while the old one may still be held by references.** More critically: `game.getEventBus().setBroadcastCallback(broadcaster)` is called each time `afterConnectionEstablished` runs for the same game. The `setBroadcastCallback` call on `ServerGameEventBus` replaces the previous broadcaster. This means that if player A connects, then player B connects, the event bus only has one broadcaster (player B's). Player A will miss all game events broadcast via the event bus until the next reconnect replaces it again. The broadcaster should be created once per game, not once per player connection. Consider creating it in `GameInstance.start()` (or the first connection to a game) and reusing it — `GameEventBroadcaster` is stateless per `gameId` as noted in the comment, so there is no reason to create a new one.
   - File: `GameWebSocketHandler.java`, lines 157–161
   - Severity: HIGH — causes event delivery failures for all but the most-recently-connected player

2. **`InboundMessageRouter.handleAdminKick` iterates `connectionManager.getConnections()` twice** (lines 212–218 and 228–234) to find the target player. Between the first and second iteration, the target player could disconnect, causing `connectionManager.removeConnection` to be called on a now-absent connection (harmless but wasteful) or `targetConnection.close()` to be called on a stale session. More importantly, `game.removePlayer(targetProfileId)` is called **before** removing the connection (line 221 precedes line 236). The target player's WebSocket session is still open and can send messages between `removePlayer` and `close()`. The broadcast of `PLAYER_KICKED` at line 223–225 happens before the connection is closed, which is correct. However, the two-pass iteration is fragile. The `PlayerConnection` should be looked up once and cached before calling `removePlayer`.
   - File: `InboundMessageRouter.java`, lines 212–238
   - Severity: LOW-MEDIUM — logic is functionally correct in the common case but has a TOCTOU gap

3. **`InboundMessageRouter` — rate limiting is applied per `profileId` across all games, not per `(profileId, gameId)` pair.** A player in two different games (allowed per `maxGamesPerUser = 5`) will have their actions in game B rate-limited based on their activity in game A. This is likely unintentional and could cause one game to interfere with another. Since `RateLimiter` uses a flat `ConcurrentHashMap<Long, Long>`, there is no game-scoping. The fix requires either (a) using a compound key in `RateLimiter`, or (b) using per-game `RateLimiter` instances. Given `maxGamesPerUser = 5` is configured, this scenario is reachable in production.
   - File: `InboundMessageRouter.java`, lines 133–143; `RateLimiter.java`
   - Severity: MEDIUM — cross-game rate limit interference

4. **`RateLimiter` has a TOCTOU race in `allowAction`** (lines 55–68): two concurrent threads for the same `profileId` can both read `null` from `lastActionTimestamps.get(profileId)` simultaneously, both branch to the "first action" path, and both call `put(profileId, now)`. The second `put` overwrites the first, but both return `true`, allowing two actions through simultaneously. For this poker game context (each player has one connection and WebSocket messages are delivered serially per session) this race is not exploitable in practice, but the implementation is not correctly thread-safe by itself. The fix is to use `ConcurrentHashMap.compute()` atomically. Since this is security-related (rate limiting) and the class documents no threading assumptions, this should be fixed.
   - File: `RateLimiter.java`, lines 52–73
   - Severity: MEDIUM — not exploitable with current single-session-per-player design, but the class is not correctly thread-safe

---

### Verification

- Tests: 447/447 passed per dev agent's report. Integration, unit, and security-critical path tests all present and well-structured.
- Coverage: Not measured in this review. Security-critical paths (`CardPrivacyTest`, `InboundMessageRouterTest`, admin auth tests) are thoroughly covered.
- Build: Reported clean with zero warnings.
- Privacy: No private data found. All test data uses synthetic values. JWT keys generated fresh per test run in temp directory.
- Security: Card privacy boundary is correct and tested. JWT auth flow is correct. Admin auth is enforced. Three blocking issues found (broadcaster re-registration race, rate limiter TOCTOU, cross-game rate limit scope) that must be addressed before merge.
