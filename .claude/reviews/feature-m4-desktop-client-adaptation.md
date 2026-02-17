# Review Request

## Review Request

**Branch:** feature-m4-desktop-client-adaptation
**Worktree:** ../DDPoker-feature-m4-desktop-client-adaptation
**Plan:** .claude/plans/SERVER-HOSTED-GAME-ENGINE.md (Milestone 4)
**Requested:** 2026-02-17 01:40

## Summary

Implements M4 (Desktop Client Adaptation) of the Server-Hosted Game Engine plan. The desktop Swing client now embeds a Spring Boot game server (Phase 4.1), connects to it via WebSocket (Phase 4.2), drives game flow through `WebSocketTournamentDirector` (Phase 4.3), creates practice games via REST (Phase 4.4), removes unused legacy phase/logic files (Phase 4.5), and discovers resumable games at startup via `GameSaveManager` (Phase 4.6). All 1596 tests pass.

## Files Changed

**New production files:**
- [x] `poker/.../server/EmbeddedGameServer.java` - Starts/stops Spring Boot on a random port inside the desktop JVM
- [x] `poker/.../server/EmbeddedServerConfig.java` - Spring `@Configuration` for embedded mode (simplified auth, no-timeout practice settings)
- [x] `poker/.../server/GameServerRestClient.java` - HTTP client: `createPracticeGame()` + `listGames()` against the embedded server REST API
- [x] `poker/.../server/GameSaveManager.java` - Queries embedded server at startup for IN_PROGRESS/PAUSED games; exposes `loadResumableGames()` / `getResumableGames()` / `hasResumableGames()`
- [x] `poker/.../server/PracticeGameLauncher.java` - Orchestrates practice game creation: posts to REST, returns WebSocket URL
- [x] `poker/.../online/WebSocketGameClient.java` - JDK 11 WebSocket client; connects to embedded server, routes incoming JSON messages to registered handlers
- [x] `poker/.../online/WebSocketTournamentDirector.java` - Replaces `TournamentDirector` for WebSocket games; drives server via player action messages
- [x] `poker/.../online/PokerDirector.java` - Thin façade that delegates to `WebSocketTournamentDirector` for new game modes
- [x] `poker/.../online/RemotePokerTable.java` - Populates `PokerTable` state from WebSocket `GAME_STATE` / `PLAYER_ACTED` messages
- [x] `poker/.../online/RemoteHoldemHand.java` - Populates `HoldemHand` state from WebSocket hand-flow messages
- [x] `poker/src/main/resources/application-embedded.properties` - Spring profile for embedded mode (random port, local auth, H2 file store)
- [x] `pokergameserver/.../controller/PracticeGameController.java` - REST `POST /api/v1/games/practice` endpoint; creates a practice game on the embedded server
- [x] `pokergameserver/.../controller/TournamentProfileConverter.java` - Converts `TournamentProfile` + AI player list into `GameConfig` accepted by `GameInstanceManager`

**Modified production files:**
- [x] `poker/pom.xml` - Added `pokergameserver` dependency, Spring Boot WebSocket/web starters, JDK HTTP client
- [x] `poker/.../PokerMain.java` - Start `EmbeddedGameServer`, initialize `GameSaveManager`, add `getEmbeddedServer()` / `getGameSaveManager()` accessors
- [x] `poker/.../PokerGame.java` - Added `setLevel()` setter and `WebSocketConfig` support (accessor/setter for WebSocket connection params)
- [x] `poker/.../ShowTournamentTable.java` - `BanPlayer` inner class: replaced `OnlineManager mgr` with `Runnable kickAction` (severs OnlineManager dependency)
- [x] `poker/.../dashboard/ObserversDash.java` - Updated `BanPlayer` call site to pass lambda `() -> mgr.banPlayer(p)`
- [x] `poker/.../online/Lobby.java` - Updated `BanPlayer` call site to pass lambda `() -> mgr_.banPlayer(p)`
- [x] `poker/.../DealDisplay.java` - Gutted to static helper class (removed `ChainPhase`/`TournamentDirector` lifecycle; kept `syncCards()` + `displayCard()`)
- [x] `poker/.../DealCommunity.java` - Gutted to static helper class (removed `ChainPhase`/`TournamentDirector` lifecycle; kept `syncCards()` + `addCard()`)
- [x] `poker/.../Showdown.java` - Gutted to static helper class (removed `ChainPhase`/`TournamentDirector` lifecycle; kept `displayShowdown()` + `displayAllin()`)

**Deleted files:**
- [x] `poker/.../logic/HandOrchestrator.java` + test
- [x] `poker/.../logic/ShowdownCalculator.java`
- [x] `poker/.../logic/DealingRules.java`
- [x] `poker/.../logic/ColorUpLogic.java` + test
- [x] `poker/.../logic/LevelTransitionLogic.java` + test
- [x] `poker/.../logic/TableManager.java` + test
- [x] `poker/.../logic/OnlineCoordinator.java` + test
- [x] `poker/.../logic/TournamentClock.java` + test

**New test files:**
- [x] `poker/.../server/EmbeddedGameServerTest.java` (109 lines)
- [x] `poker/.../server/EmbeddedGameServerAuthTest.java` (88 lines)
- [x] `poker/.../server/GameServerRestClientTest.java` (164 lines)
- [x] `poker/.../server/GameSaveManagerTest.java` (189 lines)
- [x] `poker/.../online/RemotePokerTableTest.java` (128 lines)
- [x] `poker/.../online/RemoteHoldemHandTest.java` (132 lines)
- [x] `poker/.../online/WebSocketTournamentDirectorTest.java` (664 lines)
- [x] `pokergameserver/.../controller/PracticeGameControllerTest.java` (159 lines)
- [x] `pokergameserver/.../controller/TournamentProfileConverterTest.java` (296 lines)

**Privacy Check:**
- ✅ SAFE - No private information found. No credentials, no URLs, no personal data.

## Verification Results

- **Tests:** 1596/1596 passed (0 failures, 25 skipped — pre-existing skips)
- **Pre-existing failure:** `HibernateTest` in `pokerserver` module fails in CI; confirmed pre-existing by `git stash` + retest on clean main. Unrelated to this branch.
- **Coverage:** Not measured (used `-P fast` profile); existing thresholds should be maintained
- **Build:** Clean (Spotless auto-formats on compile)

## Context & Decisions

**Thin client, not full client replacement:** Per the plan's "thinness" principle, `WebSocketTournamentDirector`, `RemotePokerTable`, and `RemoteHoldemHand` are thin adapters that update existing `PokerTable`/`HoldemHand` state from WebSocket messages. The existing Swing rendering layer reads the same objects — no UI changes needed.

**Phase 4.5 scope decision (logic/ files):** The 8 `logic/` files (Wave 1 testability refactoring) had zero external references confirmed via `Grep`. Safe to delete. `TournamentDirector` (42 refs) and `OnlineManager` (19 refs) were deferred to M7 — too risky without the full WebSocket path complete.

**BanPlayer decoupling:** `ShowTournamentTable.BanPlayer` took `OnlineManager mgr` as its 5th param. Replaced with `Runnable kickAction` — now accepts any action (lambda), not just `OnlineManager.banPlayer()`. Three call sites updated: one already passing `null` (no change), two updated to pass lambdas.

**Display files strategy:** `DealDisplay`, `DealCommunity`, `Showdown` extended `ChainPhase` and held `TournamentDirector` instance fields. Their static methods (`syncCards`, `displayShowdown`, `displayAllin`) were needed by `ShowTournamentTable`. Solution: rewrite as utility classes with private constructors, keeping only the static methods, removing all phase lifecycle and `TournamentDirector` dependencies.

**GameSaveManager error handling:** Failures to load resumable games are logged and swallowed — startup is never blocked by a missing or unreachable embedded server. The user simply starts a new game.

---

## Review Results

**Status:** CHANGES REQUIRED

**Reviewed by:** Claude Opus 4.6 (review agent)
**Date:** 2026-02-17

### Findings

#### Strengths

1. **Architecture is clean and well-decomposed.** The separation between `EmbeddedGameServer` (lifecycle), `GameServerRestClient` (HTTP), `PracticeGameLauncher` (orchestration), `WebSocketGameClient` (WebSocket transport), and `WebSocketTournamentDirector` (message dispatch + state mapping) follows single-responsibility well. No class does too much.

2. **Thin client principle followed consistently.** `WebSocketTournamentDirector` contains zero poker logic -- it only translates server messages to view model updates and fires `PokerTableEvent`s. `RemotePokerTable` and `RemoteHoldemHand` are minimal view-model subclasses (~180 and ~150 lines each) that override just the getters the Swing UI reads. No over-engineering.

3. **BanPlayer decoupling is a clean surgical change.** Replacing `OnlineManager mgr` with `Runnable kickAction` eliminates a concrete dependency with minimal churn (3 call sites, zero behavioral change). The lambda pattern is idiomatic.

4. **Display file gutting is well-scoped.** `DealDisplay`, `DealCommunity`, and `Showdown` kept only the static methods that `ShowTournamentTable` actually calls, removing all `ChainPhase` and `TournamentDirector` dependencies. Private constructors prevent instantiation. The copyright headers correctly use dual copyright (substantial modification).

5. **Test coverage is thorough for the new code.** 9 new test files totaling ~1,929 lines. `WebSocketTournamentDirectorTest` (664 lines) covers all 27 message types. `GameServerRestClientTest` uses a real JDK embedded HTTP server rather than mocks, giving true HTTP-level confidence. `GameSaveManagerTest` covers error paths (server error, connection failure). `PracticeGameControllerTest` uses Spring MockMvc with proper security configuration.

6. **Error handling is appropriate.** `GameSaveManager` swallows failures gracefully (non-fatal, startup continues). `EmbeddedGameServer` shows a JOptionPane and exits on startup failure. `GameServerRestClient` wraps all checked exceptions into `GameServerClientException`.

7. **`TournamentProfileConverter` is comprehensive.** Maps all 20+ configuration fields from the legacy desktop `TournamentProfile` to the new server `GameConfig`, including blind structure, rebuys, addons, bounty, late registration, scheduled start, invite-only, betting config, house cut, and payout config. All branches are tested.

8. **Deleted logic/ files are confirmed safe.** The handoff confirms zero external references via Grep. All 8 files and their 6 test files are cleanly removed.

#### Suggestions (Non-blocking)

1. **WebSocketGameClient reconnect logic schedules all attempts upfront without cancellation.**
   `WebSocketGameClient.java:210-226` -- The `handleReconnect()` method schedules all 10 reconnect attempts with increasing delays in a single `for` loop. If attempt 3 succeeds, attempts 4-10 will still fire and call `openConnection()`, potentially creating redundant WebSocket connections. The `openConnection().thenAccept()` handler overwrites `this.webSocket` each time, leaking the previous connection.

   **Recommendation:** Either (a) store the `ScheduledFuture<?>` handles and cancel remaining attempts on success, or (b) use a recursive approach where the next attempt is only scheduled after the current one fails. This is non-blocking because in M4 practice mode the embedded server is always available (reconnect is rarely triggered), but it should be fixed before M6 multiplayer.

2. **`long` to `int` narrowing cast for player IDs.**
   `WebSocketTournamentDirector.java:833,871,882,892` -- The server uses `long` player IDs but `GamePlayer.getID()` returns `int`. The code casts with `(int) playerId` and `new PokerPlayer((int) sd.playerId(), ...)`. This is safe for M4 practice mode (small IDs) but will silently truncate IDs above `Integer.MAX_VALUE` in future multiplayer scenarios.

   **Recommendation:** Add a comment documenting this limitation, or consider adding a validation that throws if `playerId > Integer.MAX_VALUE`. Non-blocking for M4.

3. **JWT token in WebSocket URL query string.**
   `WebSocketGameClient.java:107` -- The JWT is passed as `?token=jwt` in the URL. While this is the standard approach for WebSocket auth (WS doesn't support custom headers in the initial handshake), the token will appear in server access logs, browser history (if applicable), and proxy logs.

   **Recommendation:** For M4 embedded mode (localhost only), this is acceptable. For M6+ (remote servers), consider documenting this tradeoff or switching to a short-lived token exchange. Non-blocking.

4. **`GameServerRestClient` creates a new `ObjectMapper` per instance.**
   `GameServerRestClient.java:57-58` -- Each `GameServerRestClient` instantiation creates a new `ObjectMapper` with `JavaTimeModule`. The client is created in `PracticeGameLauncher` (once per launcher) and also in `GameSaveManager.loadResumableGames()` (once per startup reload). This is fine for the current usage pattern, but if the client were used more frequently, a shared mapper would be more efficient.

   **Recommendation:** Minor -- no action needed for M4. Note for future if client usage increases.

5. **`WebSocketTournamentDirector.onHandComplete()` may add duplicate cards.**
   `WebSocketTournamentDirector.java:500-509` -- When processing showdown cards for other players, cards are added to the player's existing `Hand` via `showHand.addCard()`. If the player already has hole cards (e.g., from `HOLE_CARDS_DEALT` for the local player), the code would add duplicate cards. The check `!sp.cards().isEmpty()` guards against empty card lists, but not against already-populated hands.

   **Recommendation:** Consider clearing the hand before adding showdown cards, or checking whether the card is already present. In practice, `HOLE_CARDS_DEALT` is only sent for the local player and `showdownPlayers` reveals other players' cards, so this may not overlap. But for robustness, a `hand.clear()` before the loop would be safer. Non-blocking.

6. **Handoff documentation inaccuracy.**
   The handoff says `PokerGame.java` "Added `PROP_OBSERVERS` property-change constant" but the actual diff only adds `setLevel()` and `WebSocketConfig`. `PROP_OBSERVERS` is pre-existing on the parent `Game` class. Minor -- the handoff should be corrected for clarity.

7. **`ScheduledExecutorService` in `WebSocketGameClient` is never shut down on reconnect success.**
   `WebSocketGameClient.java:70-74` -- The `scheduler` is a daemon `ScheduledExecutorService` created at construction time. It's shut down in `disconnect()` but never cleaned up after successful reconnects. Since it's a daemon thread, this is harmless (JVM exit will clean it up), but it means reconnect tasks can linger even after a successful reconnect until the `intentionallyClosed` flag stops them. See suggestion #1 above.

8. **`PracticeGameLauncher.defaultSkillLevel()` always returns 4.**
   `PracticeGameLauncher.java:112-114` -- The method ignores the `profile` parameter and always returns 4. The Javadoc mentions "Uses the profile's percentage mix if set" but the implementation doesn't. A comment says "user-configurable in a future milestone" which is effectively a TODO.

   **Recommendation:** Either implement the profile-based skill level mapping now, or simplify the method to just `return 4` without the misleading Javadoc. The comment about "future milestone" should reference which milestone. Non-blocking.

#### Required Changes (Blocking)

1. **`WebSocketGameClient.handleReconnect()` will create duplicate connections on successful early reconnect.**
   `WebSocketGameClient.java:210-226` -- As described in suggestion #1, this is a correctness bug: if reconnect attempt N succeeds, attempts N+1 through 10 still fire and open additional WebSocket connections. Each successful `openConnection()` overwrites `this.webSocket`, leaving the previous connection dangling (not closed). This means:
   - Multiple active WebSocket connections to the same game simultaneously
   - The server sees multiple sessions for the same player
   - Duplicate message delivery to the client
   - Resource leak (unclosed WebSocket connections)

   While unlikely to trigger in M4 practice mode (localhost connections rarely drop), this is a correctness bug in the reconnection mechanism. The fix is straightforward: track whether reconnection has succeeded and cancel remaining attempts. For example:
   - Add an `AtomicBoolean reconnecting` flag
   - On successful reconnect, set the flag to `false` and skip remaining scheduled tasks
   - Or use a single recursive schedule-on-failure pattern instead of pre-scheduling all attempts

   This must be fixed before merge because the reconnect mechanism is a core part of the WebSocket client and will be exercised in multiplayer (M6).

### Verification

- **Tests:** 65 tests run in poker module (33 skipped due to PropertyConfig dependency -- handled correctly with JUnit 5 `Assumptions`). 33 tests run in pokergameserver module. All pass. 0 failures. The `pokerserver` module has 124 errors from `HibernateTest` cascade -- confirmed identical on main branch (pre-existing, unrelated).
- **Coverage:** Not measured (used `-P fast` profile). The handoff notes existing thresholds should be maintained. Given 1,929 lines of new tests for ~2,900 lines of new production code, coverage ratio is reasonable.
- **Build:** Clean build for poker and pokergameserver modules. Spotless auto-formats. The `pokerserver` failure is pre-existing on main.
- **Privacy:** SAFE. No credentials, private IPs, personal data, or hardcoded secrets. JWT keys are generated dynamically at `~/.ddpoker/jwt/`. Local identity uses a random UUID password stored in `~/.ddpoker/local-identity.properties`. The `application-embedded.properties` contains no secrets (uses property placeholders like `${user.home}`).
- **Security:** The embedded server uses JWT authentication, random port binding, and `localhost`-only access (hardcoded in REST client and WebSocket URLs). JWT in query string is standard for WebSocket auth. `PracticeGameController` validates authenticated user via `SecurityContextHolder`. No injection vectors found. AI player IDs use negative longs to avoid collision with real user IDs. The `sanitizeUsername()` method in `EmbeddedGameServer` strips non-alphanumeric characters, preventing injection in username registration.
