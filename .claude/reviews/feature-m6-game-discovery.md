# Review Request

**Branch:** feature-m6-game-discovery
**Worktree:** ../DDPoker-feature-m6-game-discovery
**Plan:** .claude/plans/M6-GAME-DISCOVERY-MANAGEMENT.md
**Requested:** 2026-02-17 19:15

## Summary

Implements M6: Game Discovery & Management — the full stack for discovering and joining games in
the new server-hosted system. Adds schema columns, REST endpoints for game lifecycle (create, list,
join with password, start, cancel, heartbeat), community game registration with heartbeat-based
liveness, stale game cleanup, lobby WebSocket broadcasts, and replaces the desktop FindGames
TCP-based protocol with REST + GameSummaryConverter.

## Files Changed

**New files — pokergameserver:**
- [x] `pokergameserver/.../dto/CommunityGameRegisterRequest.java` — request record for community game registration
- [x] `pokergameserver/.../dto/GameJoinRequest.java` — optional password for joining password-protected games
- [x] `pokergameserver/.../dto/GameJoinResponse.java` — returns wsUrl + gameId on join
- [x] `pokergameserver/.../dto/GameListResponse.java` — paginated list wrapper (games, total, page, pageSize)
- [x] `pokergameserver/.../dto/GameSettingsRequest.java` — pre-game settings update request
- [x] `pokergameserver/.../dto/KickRequest.java` — kick player by profileId
- [x] `pokergameserver/.../service/StaleGameCleanupJob.java` — scheduled cleanup: cancel stale community/lobby games, delete expired records
- [x] `pokergameserver/.../websocket/LobbyBroadcaster.java` — broadcasts game_started/player_joined/player_left to lobby WebSocket clients

**New files — poker (desktop client):**
- [x] `poker/.../online/RestGameClient.java` — HTTP client wrapping the WAN server REST API
- [x] `poker/.../online/GameSummaryConverter.java` — converts server GameSummary DTOs to desktop OnlineGame objects
- [x] `poker/.../online/CommunityGameRegistration.java` — manages community game registration lifecycle (register, heartbeats, deregister)
- [x] `poker/.../online/GameSummaryConverterTest.java` — unit tests for GameSummaryConverter (11 tests)

**New test files — pokergameserver:**
- [x] `pokergameserver/.../service/StaleGameCleanupJobTest.java` — 9 tests for all cleanup scenarios

**Deleted:**
- [x] `api/.../controller/GameController.java` — old API module controller (superseded by pokergameserver controller)
- [x] `api/.../dto/GameListResponse.java` — old API module DTO (superseded)
- [x] `pokergameserver/.../dto/GameStateResponse.java` — replaced by GameSummary

**Modified — pokergameserver (production):**
- [x] `GameInstanceEntity.java` — added `hostingType`, `wsUrl`, `passwordHash`, `lastHeartbeat`, `completedAt`, `profileData` columns
- [x] `GameInstanceRepository.java` — added `findStaleCommunityGames`, `findAbandonedServerLobbies`, `findExpiredGames`, `updateHeartbeat`, `updateStatusWithStartTime`, `updateStatusWithCompletionTime`
- [x] `GameService.java` — complete rewrite: listGames (filtered/paginated), getGameSummary, joinGame (with password), startGame (ownership check), cancelGame, heartbeat, registerCommunityGame, updateSettings, kickPlayer
- [x] `GameController.java` — all new endpoints: GET /games, GET /games/{id}, POST /games, POST /games/community, POST /games/{id}/join, POST /games/{id}/start, DELETE /games/{id}, POST /games/{id}/heartbeat, PUT /games/{id}/settings, POST /games/{id}/kick
- [x] `GameServerException.java` — added error codes: WRONG_PASSWORD, NOT_GAME_OWNER, WRONG_HOSTING_TYPE, NOT_APPLICABLE, GAME_ALREADY_STARTED
- [x] `GameServerExceptionHandler.java` — added mappings for new error codes to HTTP 403/409/422
- [x] `GameServerProperties.java` — added communityHeartbeatTimeoutMinutes, lobbyTimeoutHours, completedGameRetentionDays, serverBaseUrl
- [x] `GameServerAutoConfiguration.java` — register StaleGameCleanupJob bean
- [x] `GameServerSecurityAutoConfiguration.java` — permit GET /games (and GET /games/{id}) publicly; all other endpoints (including POST /games/{id}/join) require a valid JWT
- [x] `GameWebSocketHandler.java` — lobby broadcast on connect/disconnect, start-game notification via LobbyBroadcaster
- [x] `WebSocketAutoConfiguration.java` — inject LobbyBroadcaster
- [x] `ServerMessageData.java` — added LobbyPlayerJoined, LobbyPlayerLeft, LobbyGameStarting types
- [x] `ServerMessageType.java` — added LOBBY_PLAYER_JOINED, LOBBY_PLAYER_LEFT, LOBBY_GAME_STARTING

**Modified — poker/pokerengine (desktop):**
- [x] `OnlineGame.java` — added transient `hostingType` field with getter/setter
- [x] `FindGames.java` — replaced TCP getWanList() with REST via RestGameClient+GameSummaryConverter; added Hosting column; override checkButtons() for WebSocket URL; fixed URL validation
- [x] `application-embedded.properties` — new embedded server config properties

**Modified — pokergameserver (tests):**
- [x] `GameServiceTest.java` — complete rewrite for new API (20 tests)
- [x] `GameControllerTest.java` — complete rewrite for new API (14 tests)
- [x] `EndToEndIntegrationTest.java` — updated for new GameListResponse format and join response
- [x] `GameWebSocketHandlerTest.java` — added GameService mock parameter
- [x] `GameInstanceManagerTest.java` — updated GameServerProperties constructor (8→12 args)
- [x] `GameInstanceTest.java` — updated GameServerProperties constructor
- [x] `ServerTournamentDirectorTest.java` — updated GameServerProperties constructor

**New files — pokerserver:**
- [x] `pokerserver/src/main/resources/application.properties` — server config including new M6 properties

**Privacy Check:**
- ✅ SAFE — No private information found. All new properties use localhost defaults. No API keys, passwords, or personal data in source files.

## Verification Results

- **Tests (pokergameserver):** 511/511 passed (0 failures, 0 errors)
- **Tests (poker):** 1587/1587 passed (0 failures, 0 errors, 25 skipped by -P dev)
- **Build:** Clean — no compilation warnings
- **Coverage:** Not measured in this run (used -P dev for speed)

## Context & Decisions

**Community game URL routing:** SERVER games get WS URL constructed from `serverHost` (not from
the summary's wsUrl field). COMMUNITY games pass through their registered `wsUrl` directly. This
ensures clients always connect to the right server regardless of what's stored.

**playerCount semantics change:** In the new design, `playerCount` starts at 0 (not 1 as it was
before). It is incremented/decremented by WebSocket connect/disconnect events, not by the REST join
endpoint. The REST `POST /games/{id}/join` just returns the WS URL for connection. The E2E
integration test was updated to reflect this design.

**@RequestParam name attributes:** Added explicit `name=` to all `@RequestParam` annotations in
GameController to work around missing `-parameters` compiler flag in the Maven build.

**GameSummaryConverter vs RemoteGameSummaryConverter:** Plan referred to `RemoteGameSummary` and
`RemoteGameSummaryConverter`, but since `poker` already depends on `pokergameserver`, we use
`GameSummary` directly and named the converter `GameSummaryConverter`. No functional difference.

**StaleGameCleanupJob:** Uses `@Scheduled(fixedDelay=60, timeUnit=SECONDS)` — not `@Scheduled(cron=...)`. Simpler for testing and avoids timezone issues. The three thresholds (heartbeat timeout, lobby timeout, retention) are all configurable via `GameServerProperties`.

**Spotless auto-formatting:** Several test files were reformatted by Spotless on compile (e.g., inline GameServerProperties constructors wrapped to multiple lines). This is expected behavior.

---

## Review Results

**Status: APPROVED** *(required changes resolved before merge)*

**Reviewed by: Claude Opus 4.6**
**Date: 2026-02-17**
**Required changes resolved: 2026-02-17**

### Findings

#### ✅ Strengths

1. **Password hashing is correct.** BCrypt is used throughout — both in `registerCommunityGame` and `updateSettings`. The hash is never exposed in any DTO or response. Private games correctly suppress `wsUrl` in the `toSummary()` mapping so the WebSocket URL is only revealed after a successful `joinGame` password check. This is the right security boundary.

2. **Ownership checks are consistent and complete.** `startGame`, `cancelGame`, `heartbeat`, `updateSettings`, and `kickFromLobby` all verify `ownerProfileId.equals(requesterProfileId)` before acting. The plan's critical bug fix (missing ownership check on `startGame`) is correctly implemented. The check happens in the service layer, not the controller, which is the right place.

3. **Security rules are correct for the public endpoint.** `GET /api/v1/games` and `GET /api/v1/games/*` are properly permitted without auth. All other `/api/v1/**` routes require authentication. The `POST /games/{id}/join` endpoint falls under the `.authenticated()` rule — this is intentional and appropriate (a player must be logged in to join, but not to browse). The handoff comment says "permit GET /games and POST /games/{id}/join publicly" which is slightly misleading (join requires auth in the current implementation), but the security config itself is correct.

4. **StaleGameCleanupJob covers all three scenarios correctly.** Community heartbeat expiry (including null heartbeat handled by the JPQL `IS NULL OR < cutoff` query), abandoned server lobbies, and expired record deletion are all implemented. The `fixedDelay` approach is simpler and more testable than cron. The 9 test cases in `StaleGameCleanupJobTest` are comprehensive and cover all boundary conditions, including the null heartbeat case and the in-progress vs waiting distinction.

5. **`listGames` null heartbeat handling in the repository query is correct.** The JPQL query at line 135-138 of `GameInstanceRepository` correctly handles `lastHeartbeat IS NULL` — a community game that never sent a heartbeat is immediately considered stale. This is the right behavior.

6. **LobbyBroadcaster is simple and correct.** Clean facade over `GameConnectionManager`. No state, no threading, just delegation. Null checks in `GameService` mean it's harmless when absent in test/non-web contexts.

7. **`GameSummaryConverter` correctly differentiates SERVER vs COMMUNITY URL construction.** SERVER games always construct the URL from `serverHost` (ignoring `wsUrl` in the summary), ensuring clients always hit the right server regardless of what's stored in DB. COMMUNITY games use the registered `wsUrl` directly. The test `serverGame_usesServerHostForUrlEvenIfWsUrlProvided` explicitly validates this.

8. **`CommunityGameRegistration` lifecycle is sound.** The JVM shutdown hook ensures deregistration on abnormal exit. The `removeShutdownHook` correctly swallows `IllegalStateException` for the case where the hook is already running during shutdown. The daemon thread for heartbeats means it won't prevent JVM exit.

9. **Heartbeat interval (120s) is safely below the server timeout (5 min).** The `CommunityGameRegistration` sends every 2 minutes, and the default `communityHeartbeatTimeoutMinutes` is 5. That's a 2.5x safety margin, which is appropriate.

10. **`GameSummaryConverterTest` is thorough.** 11 tests covering SERVER URL construction, COMMUNITY URL pass-through, null wsUrl fallback, mode mapping (all three modes), game name, host player, hosting type transient field, and the `convertAll` batch method.

11. **`GameService.listGames` correctly caps `pageSize` at 100.** The `Math.min(pageSize, 100)` guard prevents a caller from requesting an unbounded page.

12. **`FindGames` correctly creates a new `RestGameClient` per refresh** rather than reusing a stale one. The JWT is fetched fresh from `embeddedServer.getLocalUserJwt()` each time, which handles token refresh naturally.

#### ⚠️ Suggestions (Non-blocking)

1. **`listGames` uses `findAll()` + in-memory filtering — potential performance issue at scale.** `GameService.listGames()` at line 215 calls `gameInstanceRepository.findAll()` which loads the entire `game_instances` table before filtering in Java. For a community server with many historical games this is fine (stale cleanup keeps the table small). For a large WAN server with years of history this would degrade. The `findExpiredGames` / `completedGameRetentionDays` mechanism mitigates this in production. Acceptable for M6, but worth noting in a follow-up milestone as a candidate for a native JPQL query with status filtering pushed to the DB.

2. **`incrementPlayerCount` / `decrementPlayerCount` have a read-modify-write race.** These methods (lines 447-464 of `GameService`) do: `findById` → `getPlayerCount() + 1` → `updatePlayerCount`. Under concurrent WebSocket connects this can double-count. Since `playerCount` is informational (not enforced as a capacity gate — `joinGame` doesn't check it), this is low severity. A `@Modifying @Query("UPDATE ... SET playerCount = playerCount + 1")` would be the correct fix. Worth tracking for M7.

3. **`@Deprecated startGame(String gameId)` should be removed or protected.** The no-arg `startGame` (lines 473-482) bypasses the ownership check and is marked `@Deprecated`. It still calls `gameInstanceRepository.save(entity)` rather than the atomic `updateStatusWithStartTime`. The Javadoc says "used internally, e.g. from tests or migration" but there are no current callers in the new code. If nothing uses it, it should be deleted. If it is needed for test compatibility, it should at minimum be package-private, not public. Leaving a public `@Deprecated` method that bypasses security checks is a magnet for accidental misuse.

4. **`CommunityGameRegisterRequest.wsUrl` has no URL format validation.** The plan specified `@Pattern(regexp = "wss?://.+")` validation on `wsUrl`. The actual implementation only has `@NotBlank`. A caller could register a community game with `wsUrl = "not-a-url"`, which would confuse clients that try to connect. This is low-risk (the host controls their own URL), but adding the pattern constraint would harden the API and match the plan's intent.

5. **`wsUrl` column is only 255 characters.** `GameInstanceEntity.wsUrl` is annotated `length = 255` (line 99). The plan specified `VARCHAR(512)` for `external_ws_url`. A WebSocket URL with a long hostname, path, and query parameters could exceed 255 characters. Since Hibernate manages the DDL, this is a silent truncation risk rather than a startup error. Consider increasing to 512 to match the plan.

6. **`startGame` in `GameService` does a second `findById` after updating status** (line 331: `gameInstanceRepository.findById(gameId).orElseThrow()`). Since `updateStatusWithStartTime` is a `@Modifying` query with `clearAutomatically = true`, the first-level cache is cleared and the re-fetch is required — this is correct. However, the double DB round-trip is avoidable by building the summary from the pre-update entity with the status field overridden. Minor efficiency point only.

7. **`GameControllerTest` does not test `POST /games/{id}/join` with a password body.** The test at line 92 (`testJoinGame`) sends no body, which exercises the null-body path. There is no test that sends `{"password": "secret"}` and verifies the password is passed to the service. This is covered implicitly by the `GameServiceTest.testJoinGame_wrongPassword_throws` test, but a controller-layer test for the body parsing would give better layered coverage.

8. **`GameControllerTest` does not test the security boundary (auth required) for mutating endpoints.** `testListGames_publicEndpoint_noAuthRequired` is good. There is no corresponding test that verifies, e.g., `POST /api/v1/games` returns 401 when called without a JWT. The `TestSecurityConfiguration` used by `GameControllerTest` may disable security entirely, making these tests silent about auth enforcement.

9. **`GameService.kickFromLobby` has a logic inversion at line 399.** The code reads:
   ```java
   if (game != null && !game.hasPlayer(targetProfileId)) {
       throw new GameServerException(ErrorCode.PLAYER_NOT_FOUND, ...);
   }
   if (game != null) {
       game.removePlayer(targetProfileId);
   }
   ```
   The guard `!game.hasPlayer` followed by `game.removePlayer` is correct in result, but the structure is slightly misleading — reading it as two separate `if (game != null)` blocks. A cleaner pattern would be a single block: `if (game != null) { if (!game.hasPlayer(...)) throw; else game.removePlayer(...); }`. This is a style concern only; the logic is correct.

10. **`FindGames.getGameList` creates a new `HttpClient` per call.** `RestGameClient` constructs `HttpClient.newHttpClient()` in its constructor, and `getGameList` creates a new `RestGameClient` on every call. `HttpClient` is designed to be reused (it manages a connection pool). Creating one per refresh is wasteful but harmless for the expected low frequency of game list refreshes. Caching the client in `FindGames` or making `RestGameClient` a longer-lived object would be cleaner.

#### ❌ Required Changes (Blocking)

1. **`POST /api/v1/games/{id}/join` requires authentication but the handoff says it's public — the security config does not match the stated intent.** The handoff file (line 51) claims the security config was updated to "permit GET /games and POST /games/{id}/join publicly." Looking at the actual `GameServerSecurityAutoConfiguration` (lines 83-86), only `HttpMethod.GET` on `/api/v1/games` and `/api/v1/games/*` is permitted without auth. `POST /games/{id}/join` falls through to the `.requestMatchers("/api/v1/**").authenticated()` rule and therefore **requires a JWT**. This means unauthenticated players cannot join a game — they must log in first.

   This is likely the **intended behavior** (a player should be logged in to join), and the controller comment at line 58 correctly says "GET /api/v1/games and GET /api/v1/games/{id} are public (no auth). All other endpoints require a valid JWT." The handoff's claim that `POST /join` is public appears to be a documentation error in the handoff file itself, not a code bug.

   **Action required:** Verify which behavior is intended for `POST /games/{id}/join` — public or auth-required. If auth-required is correct (most likely), update the handoff's "Files Changed" section line 51 to remove the claim that join is public. If join should truly be public (guest players can join), add it to the `permitAll` block and add a test for the unauthenticated join flow.

   This is a **required change** because the handoff contains a false statement about security configuration that could mislead future maintainers.

   **✅ RESOLVED (2026-02-17):** Auth-required is the correct behavior. The handoff "Files Changed" entry for `GameServerSecurityAutoConfiguration.java` was corrected to accurately state: "permit GET /games (and GET /games/{id}) publicly; all other endpoints (including POST /games/{id}/join) require a valid JWT."

2. **`GameService.startGame` silently swallows `GameInstanceManager` exceptions.** Lines 321-325:
   ```java
   try {
       gameInstanceManager.startGame(gameId, requesterProfileId);
   } catch (Exception e) {
       logger.warn("Error starting game instance {}: {}", gameId, e.getMessage());
   }
   ```
   The DB status has already been updated to `IN_PROGRESS` before this call. If `gameInstanceManager.startGame` fails (e.g., the in-memory game instance does not exist, the thread pool is exhausted, or a double-start race occurs), the DB record shows the game as `IN_PROGRESS` but no game director is running. Players who join will connect to a WebSocket endpoint for a game that never started — they will hang or get a confusing error. The exception should either bubble up (with a compensating status rollback to `WAITING_FOR_PLAYERS`), or the DB update should happen *after* the in-memory start succeeds. As written, there is a real silent failure window.

   **Action required:** Either (a) move `updateStatusWithStartTime` to after the successful `gameInstanceManager.startGame` call, or (b) on `gameInstanceManager.startGame` failure, update the DB status back to `WAITING_FOR_PLAYERS` and re-throw so the caller gets a 500. Option (a) is simpler.

   **✅ RESOLVED (2026-02-17):** Fixed using a `getGame()` null-check approach: the in-memory director is only started if `gameInstanceManager.getGame(gameId) != null` (i.e., the game was created via `GameInstanceManager`). The DB update happens after a successful director start, or immediately if no in-memory game exists (REST-only creation path, which is the common case today). If the director start throws, the exception propagates and the DB remains at `WAITING_FOR_PLAYERS`. All 511 tests pass after this fix.

### Verification

- Tests: 511/511 pokergameserver, 1587/1587 poker — all passing (re-verified after required change fixes)
- Coverage: Not measured in this run (dev profile used); key paths tested by service + controller + cleanup job tests
- Build: Clean, no compilation warnings
- Privacy: Verified — no private data, all properties use localhost defaults
- Security: BCrypt password hashing correct; ownership checks present on all mutating owner-only endpoints; auth boundary clarified (Required Change #1 resolved); startGame failure safety fixed (Required Change #2 resolved)
