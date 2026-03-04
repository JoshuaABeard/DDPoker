# Cross-Play Foundation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Status:** COMPLETED (2026-03-04) — All 11 implementation tasks completed and merged to main via `e17f43f6`. Full build passes. Task 13 (manual E2E) deferred to live testing.

**Goal:** Enable the Java desktop client to create and join online games on the central game server so desktop and web clients can play together.

**Architecture:** The embedded server is practice-only. For all online play, both clients hit the central `pokergameserver` via REST (`/api/v1/games/*`) and WebSocket (`/ws/games/{gameId}`). The desktop user authenticates against the central server; their JWT is cached in `RestAuthClient` and threaded through the online flow.

**Tech Stack:** Java 25, Java Swing, `java.net.http.WebSocket`, JUnit 5, Maven (`mvn test -P dev -pl poker`)

**Supersedes:** `2026-02-22-restore-host-online-flow.md` (archived). The restore-host plan assumed an embedded-server hosting model. This plan uses central server exclusively; the structural pieces from restore-host (gamedef.xml phases, HostStart, pokergameserver DTOs, RestGameClient methods) are incorporated here.

---

## Background

The desktop online flow was written assuming the embedded server was the host. Three specific problems block cross-play:

1. **WebSocket URL is hardcoded to `localhost`** — `WebSocketGameClient.connect(int port, ...)` and `Lobby.java` both build `ws://localhost:{port}/...`. For the central server the host is not localhost.
2. **JWT comes from the embedded server** — `FindGames` and `OnlineConfiguration` call `embeddedServer.getLocalUserJwt()`. That token is only valid for the embedded server; the central server issues its own JWT via `RestAuthClient.login()`.
3. **OnlineConfiguration starts the embedded server** — it calls `embeddedServer.start(...)`, detects public/LAN IPs, and registers with a WAN game list. All of this must be replaced with a single REST `createGame` call to the central server.

Two phase-name bugs were already fixed (commit `d659a64f`):
- `FindGames`: `processPhase("Lobby")` → `processPhase("Lobby.Player")`
- `OnlineConfiguration`: `processPhase("Lobby")` → `processPhase("Lobby.Host")`

---

## Key Files

| File | Role |
|------|------|
| `code/poker/src/main/java/com/donohoedigital/games/poker/PokerGame.java` | `WebSocketConfig` record — gameId, jwt, **host**, port, observer |
| `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketGameClient.java` | WebSocket connection; `connect(int port, ...)` hardcodes localhost |
| `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java` | Active-game WebSocket handler; reads `config.port()` |
| `code/poker/src/main/java/com/donohoedigital/games/poker/online/RestAuthClient.java` | Login/logout — currently stateless (no JWT cache) |
| `code/poker/src/main/java/com/donohoedigital/games/poker/online/RestGameClient.java` | REST game operations — missing createGame/startGame/cancelGame/getGameSummary |
| `code/poker/src/main/java/com/donohoedigital/games/poker/online/FindGames.java` | Lists and joins games; uses embedded server URL+JWT |
| `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineConfiguration.java` | Starts embedded server; must call central server `createGame` instead |
| `code/poker/src/main/java/com/donohoedigital/games/poker/online/Lobby.java` | Pre-game lobby; builds `restClient_` from embedded server |
| `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineMenu.java` | Online entry point; needs login gate |
| `code/poker/src/main/java/com/donohoedigital/games/poker/online/HostStart.java` | Countdown before InitializeOnlineGame — deleted in M7, needs restoration |
| `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineServerUrl.java` | Utility: normalizes configured server string to `http://host:port` |
| `code/poker/src/main/java/com/donohoedigital/games/poker/server/PracticeGameLauncher.java` | Sets `WebSocketConfig` for practice games — must keep using localhost |
| `code/poker/src/main/resources/config/poker/gamedef.xml` | Phase definitions — needs OnlineMenu, OnlineConfiguration, Lobby.Host/Player, HostStart |
| `code/pokergameserver/src/main/java/.../dto/GameSummary.java` | Needs `players` list added |
| `code/pokergameserver/src/main/java/.../dto/LobbyPlayerInfo.java` | New record — name + role |
| `code/pokergameserver/src/main/java/.../service/GameService.java` | Populate players in getGameSummary() |
| `code/pokergameserver/src/main/java/.../GameInstance.java` | Add getConnectedPlayers() |

---

## Task 1: Add `host` to `WebSocketConfig`

`WebSocketConfig` currently stores `(gameId, jwt, port, observer)` and all callers build WebSocket URLs as `ws://localhost:{port}/...`. Add a `host` field so central-server games can use a non-localhost host.

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerGame.java`

**Step 1: Read the current record (lines 654–673)**

```java
// CURRENT (PokerGame.java ~line 654)
public record WebSocketConfig(String gameId, String jwt, int port, boolean observer) {
    public WebSocketConfig(String gameId, String jwt, int port) {
        this(gameId, jwt, port, false);
    }
}
```

**Step 2: Replace the record and setters**

Replace the entire `WebSocketConfig` record and all `setWebSocketConfig` methods with:

```java
public record WebSocketConfig(String gameId, String jwt, String host, int port, boolean observer) {
    /** Convenience constructor for embedded/localhost server. */
    public WebSocketConfig(String gameId, String jwt, int port) {
        this(gameId, jwt, "localhost", port, false);
    }
    /** Convenience constructor for embedded/localhost server with observer flag. */
    public WebSocketConfig(String gameId, String jwt, int port, boolean observer) {
        this(gameId, jwt, "localhost", port, observer);
    }
}

/** Set config for embedded (practice) server — host defaults to localhost. */
public void setWebSocketConfig(String gameId, String jwt, int port) {
    webSocketConfig_ = new WebSocketConfig(gameId, jwt, port);
}

/** Set config for embedded (practice) server with observer flag. */
public void setWebSocketConfig(String gameId, String jwt, int port, boolean observer) {
    webSocketConfig_ = new WebSocketConfig(gameId, jwt, port, observer);
}

/** Set config for central server — caller provides explicit host. */
public void setWebSocketConfig(String gameId, String jwt, String host, int port) {
    webSocketConfig_ = new WebSocketConfig(gameId, jwt, host, port, false);
}

/** Set config for central server with observer flag. */
public void setWebSocketConfig(String gameId, String jwt, String host, int port, boolean observer) {
    webSocketConfig_ = new WebSocketConfig(gameId, jwt, host, port, observer);
}
```

**Step 3: Compile to verify no existing callers break**

```bash
cd code && mvn compile -pl poker -am -q
```

Expected: SUCCESS (existing callers use the 2-arg and 3-arg convenience constructors, which still exist).

**Step 4: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/PokerGame.java
git commit -m "feat(online): add host field to WebSocketConfig for central server support"
```

---

## Task 2: Update `WebSocketGameClient.connect()` to accept host

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketGameClient.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/Lobby.java` (caller)
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java` (caller)
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketGameClientTest.java`

**Step 1: Write the failing test**

Find the existing test class `WebSocketGameClientTest.java`. Add:

```java
@Test
void connect_usesProvidedHostInUrl() throws Exception {
    // Arrange: capture the URL passed to HttpClient.newWebSocketBuilder()
    HttpClient mockHttp = mock(HttpClient.class);
    HttpClient.WebSocketBuilder mockBuilder = mock(HttpClient.WebSocketBuilder.class);
    WebSocket mockWs = mock(WebSocket.class);
    when(mockHttp.newWebSocketBuilder()).thenReturn(mockBuilder);
    when(mockBuilder.header(any(), any())).thenReturn(mockBuilder);
    when(mockBuilder.buildAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(mockWs));

    WebSocketGameClient client = new WebSocketGameClient(new ObjectMapper(), mockHttp);
    client.setMessageHandler(msg -> {});

    // Act
    client.connect("game.example.com", 9090, "abc123", "tok");

    // Assert: URI passed to buildAsync contains game.example.com, not localhost
    ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
    verify(mockBuilder).buildAsync(uriCaptor.capture(), any());
    assertThat(uriCaptor.getValue().getHost()).isEqualTo("game.example.com");
    assertThat(uriCaptor.getValue().getPort()).isEqualTo(9090);
}
```

**Step 2: Run to verify it fails**

```bash
cd code && mvn test -pl poker -Dtest=WebSocketGameClientTest#connect_usesProvidedHostInUrl -q
```

Expected: FAIL — method `connect(String, int, String, String)` does not exist.

**Step 3: Add `host` parameter to `connect()` in `WebSocketGameClient`**

```java
// Change field (around line 71):
private String serverHost;
private int serverPort;

// Change connect() signature (around line 124):
public CompletableFuture<Void> connect(String serverHost, int serverPort, String gameId, String jwt) {
    this.serverHost = serverHost;
    this.serverPort = serverPort;
    this.gameId = gameId;
    this.wsUrl = buildWsUrl(jwt);
    // ... rest unchanged ...
}

// Change buildWsUrl (around line 279):
private String buildWsUrl(String token) {
    return "ws://" + serverHost + ":" + serverPort + "/ws/games/" + gameId + "?token=" + token;
}
```

**Step 4: Update callers**

In `Lobby.java` (around line 208):
```java
// OLD:
wsClient_.connect(config.port(), gameId_, config.jwt())
// NEW:
wsClient_.connect(config.host(), config.port(), gameId_, config.jwt())
```

In `WebSocketTournamentDirector.java` (around line 193-198):
```java
// Add field near serverPort_:
private String serverHost_;

// In start(), after reading config:
serverHost_ = config.host();
serverPort_ = config.port();
gameId_ = config.gameId();
jwt_ = config.jwt();

// Change connect call:
wsClient_.connect(serverHost_, serverPort_, gameId_, jwt_);
```

**Step 5: Run the new test**

```bash
cd code && mvn test -pl poker -Dtest=WebSocketGameClientTest#connect_usesProvidedHostInUrl -q
```

Expected: PASS

**Step 6: Run full poker tests**

```bash
cd code && mvn test -P dev -pl poker -q
```

Expected: all tests pass.

**Step 7: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketGameClient.java \
        code/poker/src/main/java/com/donohoedigital/games/poker/online/Lobby.java \
        code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java \
        code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketGameClientTest.java
git commit -m "feat(online): pass host through WebSocket connect so central server is reachable"
```

---

## Task 3: Add JWT caching to `RestAuthClient`

The `login()` method returns the JWT but the caller discards it after one use. The online flow needs to retrieve the JWT later without re-prompting. Add an in-memory session cache to the singleton.

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/RestAuthClient.java`
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/online/RestAuthClientTest.java`

**Step 1: Write the failing test**

```java
@Test
void login_cachesJwtAndServerUrl() {
    RestAuthClient client = new RestAuthClient();
    client.cacheSession("http://server:8080", "jwt-abc");
    assertThat(client.getCachedJwt()).isEqualTo("jwt-abc");
    assertThat(client.getCachedServerUrl()).isEqualTo("http://server:8080");
    assertThat(client.hasSession()).isTrue();
}

@Test
void clearSession_removesCache() {
    RestAuthClient client = new RestAuthClient();
    client.cacheSession("http://server:8080", "jwt-abc");
    client.clearSession();
    assertThat(client.hasSession()).isFalse();
    assertThat(client.getCachedJwt()).isNull();
}
```

**Step 2: Run to verify failure**

```bash
cd code && mvn test -pl poker -Dtest=RestAuthClientTest -q
```

Expected: FAIL — `cacheSession`, `getCachedJwt`, `hasSession` don't exist.

**Step 3: Add session cache to `RestAuthClient`**

```java
private volatile String cachedJwt_;
private volatile String cachedServerUrl_;

void cacheSession(String serverUrl, String jwt) {
    this.cachedServerUrl_ = serverUrl;
    this.cachedJwt_ = jwt;
}

public String getCachedJwt() { return cachedJwt_; }
public String getCachedServerUrl() { return cachedServerUrl_; }
public boolean hasSession() { return cachedJwt_ != null; }

public void clearSession() {
    cachedJwt_ = null;
    cachedServerUrl_ = null;
}
```

Update `login()` to cache on success:
```java
LoginResponse response = OBJECT_MAPPER.readValue(body, LoginResponse.class);
cacheSession(serverUrl, response.jwt());   // ADD
return response;
```

Update `logout()` to clear the cache:
```java
public void logout(String serverUrl, String jwt) {
    clearSession();   // ADD
    // ... rest unchanged ...
}
```

**Step 4: Run the new tests**

```bash
cd code && mvn test -pl poker -Dtest=RestAuthClientTest -q
```

Expected: PASS

**Step 5: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/RestAuthClient.java \
        code/poker/src/test/java/com/donohoedigital/games/poker/online/RestAuthClientTest.java
git commit -m "feat(online): cache JWT in RestAuthClient after login for use in online flow"
```

---

## Task 4: Add `toWsBaseUrl` helper to `OnlineServerUrl`

Multiple classes need to convert an HTTP base URL to WebSocket. Centralise in `OnlineServerUrl`.

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineServerUrl.java`
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/online/OnlineServerUrlTest.java`

**Step 1: Write the failing test**

```java
@Test
void toWsBaseUrl_convertsHttpToWs() {
    assertThat(OnlineServerUrl.toWsBaseUrl("http://server.example.com:8080"))
        .isEqualTo("ws://server.example.com:8080");
}

@Test
void toWsBaseUrl_convertsHttpsToWss() {
    assertThat(OnlineServerUrl.toWsBaseUrl("https://server.example.com:443"))
        .isEqualTo("wss://server.example.com:443");
}

@Test
void toWsBaseUrl_returnsNullForNull() {
    assertThat(OnlineServerUrl.toWsBaseUrl(null)).isNull();
}
```

**Step 2: Run to verify failure**

```bash
cd code && mvn test -pl poker -Dtest=OnlineServerUrlTest -q
```

Expected: FAIL — `toWsBaseUrl` doesn't exist.

**Step 3: Add `toWsBaseUrl` to `OnlineServerUrl`**

```java
public static String toWsBaseUrl(String httpBaseUrl) {
    if (httpBaseUrl == null) return null;
    if (httpBaseUrl.startsWith("https://"))
        return "wss://" + httpBaseUrl.substring("https://".length());
    if (httpBaseUrl.startsWith("http://"))
        return "ws://" + httpBaseUrl.substring("http://".length());
    return null;
}
```

**Step 4: Run the new tests**

```bash
cd code && mvn test -pl poker -Dtest=OnlineServerUrlTest -q
```

Expected: PASS

**Step 5: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineServerUrl.java \
        code/poker/src/test/java/com/donohoedigital/games/poker/online/OnlineServerUrlTest.java
git commit -m "feat(online): add toWsBaseUrl helper to OnlineServerUrl"
```

---

## Task 5: Add missing `RestGameClient` methods

`OnlineConfiguration` (Task 7) and `Lobby` (Task 9) call `createGame()`, `startGame()`, `cancelGame()`, and `getGameSummary()`. These methods do not yet exist. Add them before wiring up the callers.

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/RestGameClient.java`
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/online/RestGameClientTest.java`

**Step 1: Read `RestGameClient` to understand existing HTTP call patterns**

(Read the file; do not skip — the `RestGameClientException`, JSON handling, and auth header pattern must be consistent with existing methods.)

**Step 2: Add four methods**

```java
/**
 * Creates a new game on the central server.
 * POST /api/v1/games
 */
public GameSummary createGame(GameConfig config) throws RestGameClientException {
    // POST JSON body; parse GameSummary response
}

/**
 * Starts a lobby game (host-only).
 * POST /api/v1/games/{gameId}/start
 */
public void startGame(String gameId) throws RestGameClientException {
    // POST with empty body; 200 = success
}

/**
 * Cancels a game (host-only).
 * DELETE /api/v1/games/{gameId}
 */
public void cancelGame(String gameId) throws RestGameClientException {
    // DELETE request
}

/**
 * Returns a game summary including connected player list.
 * GET /api/v1/games/{gameId}
 */
public GameSummary getGameSummary(String gameId) throws RestGameClientException {
    // GET; parse GameSummary
}
```

**Step 3: Write tests**

Mock `HttpClient` (same pattern as existing `RestGameClientTest`). Verify each method sends the correct HTTP verb and path, and that `createGame()`/`getGameSummary()` parse the JSON response correctly.

**Step 4: Run tests**

```bash
cd code && mvn test -pl poker -Dtest=RestGameClientTest -P dev -q
```

Expected: PASS

**Step 5: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/RestGameClient.java \
        code/poker/src/test/java/com/donohoedigital/games/poker/online/RestGameClientTest.java
git commit -m "feat(online): add createGame, startGame, cancelGame, getGameSummary to RestGameClient"
```

---

## Task 6: Rework `FindGames` to use the central server

`FindGames.fetchGames()` currently returns an empty list if the embedded server is not running. It must instead connect to the central server.

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/FindGames.java`

**Background — current flow (lines 488–514):**

```java
// CURRENT — fetchGames():
if (embeddedServer == null || !embeddedServer.isRunning()) {
    return new OnlineGameList();  // silent empty list
}
String baseUrl = "http://localhost:" + embeddedServer.getPort();
String jwt = embeddedServer.getLocalUserJwt();        // WRONG: embedded JWT
restClient_ = new RestGameClient(baseUrl, jwt);
```

And in the join action (lines 192–198):
```java
GameJoinResponse response = restClient_.joinGame(gameId, null);
int port = extractPort(response.wsUrl());              // WRONG: loses host
game.setWebSocketConfig(response.gameId(),
    ((PokerMain) engine_).getEmbeddedServer().getLocalUserJwt(), port);  // WRONG: embedded JWT
```

**Step 1: Add a `getCentralServerUrl()` helper method to `FindGames`**

```java
private String getCentralServerUrl() {
    try {
        String node = Prefs.NODE_OPTIONS + engine_.getPrefsNodeName();
        String server = Prefs.getUserPrefs(node).get(EngineConstants.OPTION_ONLINE_SERVER, "");
        return OnlineServerUrl.normalizeBaseUrl(server);
    } catch (Exception e) {
        logger.warn("Could not read central server URL from preferences", e);
        return null;
    }
}
```

Required imports: `com.donohoedigital.config.Prefs`, `com.donohoedigital.games.engine.EngineConstants`.

**Step 2: Rewrite `fetchGames()`**

```java
private OnlineGameList fetchGames() {
    String baseUrl = getCentralServerUrl();
    String jwt = RestAuthClient.getInstance().getCachedJwt();

    if (baseUrl == null || jwt == null) {
        logger.warn("Central server not configured or not logged in — returning empty game list");
        return new OnlineGameList();
    }

    if (restClient_ == null) {
        restClient_ = new RestGameClient(baseUrl, jwt);
    } else {
        restClient_.setJwt(jwt);
    }

    URI serverUri;
    try {
        serverUri = new java.net.URI(baseUrl);
    } catch (java.net.URISyntaxException e) {
        logger.warn("Invalid central server URL: {}", baseUrl);
        return new OnlineGameList();
    }
    String serverHost = serverUri.getHost() + (serverUri.getPort() > 0 ? ":" + serverUri.getPort() : "");
    GameSummaryConverter converter = new GameSummaryConverter(serverHost);

    List<com.donohoedigital.games.poker.gameserver.dto.GameSummary> summaries;
    try {
        summaries = restClient_.listGames();
    } catch (RestGameClient.RestGameClientException e) {
        logger.warn("Failed to fetch game list from central server: {}", e.getMessage());
        return new OnlineGameList();
    }
    return converter.convertAll(summaries);
}
```

**Step 3: Fix the join action to use central server JWT and host**

```java
// OLD:
int port = extractPort(response.wsUrl());
game.setWebSocketConfig(response.gameId(),
    ((PokerMain) engine_).getEmbeddedServer().getLocalUserJwt(), port);

// NEW:
java.net.URI wsUri = java.net.URI.create(response.wsUrl());
String wsHost = wsUri.getHost();
int wsPort = wsUri.getPort();
String jwt = RestAuthClient.getInstance().getCachedJwt();
game.setWebSocketConfig(response.gameId(), jwt, wsHost, wsPort);
```

Apply the same fix to the observe action:

```java
// OLD:
int port = extractPort(response.wsUrl());
String jwt = response.token() != null
        ? response.token()
        : ((PokerMain) engine_).getEmbeddedServer().getLocalUserJwt();
game.setWebSocketConfig(response.gameId(), jwt, port, true);

// NEW:
java.net.URI wsUri = java.net.URI.create(response.wsUrl());
String wsHost = wsUri.getHost();
int wsPort = wsUri.getPort();
String jwt = response.token() != null
        ? response.token()
        : RestAuthClient.getInstance().getCachedJwt();
game.setWebSocketConfig(response.gameId(), jwt, wsHost, wsPort, true);
```

The `extractPort()` private method can stay (may be used elsewhere) but is no longer called from the join/observe paths.

**Step 4: Compile**

```bash
cd code && mvn compile -pl poker -am -q
```

Expected: SUCCESS

**Step 5: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/FindGames.java
git commit -m "feat(online): FindGames uses central server REST API and JWT instead of embedded server"
```

---

## Task 7: Rework `OnlineConfiguration` to create game on central server

`OnlineConfiguration.doOpenLobby()` currently starts the embedded server. Replace with a REST `createGame` call to the central server.

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineConfiguration.java`
- Modify: `code/poker/src/main/resources/config/poker/gamedef.xml`
- Modify: `code/poker/src/main/resources/config/poker/client.properties`

**Step 1: Add `getCentralServerUrl()` helper** (same pattern as Task 6)

**Step 2: Rewrite `doOpenLobby()`**

```java
private void doOpenLobby() {
    String baseUrl = getCentralServerUrl();
    String jwt = RestAuthClient.getInstance().getCachedJwt();

    if (baseUrl == null) {
        EngineUtils.displayInformationDialog(context_,
                PropertyConfig.getMessage("msg.online.noserver"));
        return;
    }
    if (jwt == null) {
        EngineUtils.displayInformationDialog(context_,
                PropertyConfig.getMessage("msg.online.notloggedin"));
        return;
    }

    GameConfig gameConfig = buildGameConfig();

    Thread t = new Thread(() -> {
        try {
            RestGameClient client = new RestGameClient(baseUrl, jwt);
            GameSummary summary = client.createGame(gameConfig);
            String gameId = summary.gameId();

            java.net.URI wsUri = java.net.URI.create(summary.wsUrl());
            String wsHost = wsUri.getHost();
            int wsPort = wsUri.getPort();

            SwingUtilities.invokeLater(() -> {
                PokerGame game = (PokerGame) context_.getGame();
                game.setWebSocketConfig(gameId, jwt, wsHost, wsPort);

                DMTypedHashMap params = new DMTypedHashMap();
                params.setBoolean("host", Boolean.TRUE);
                context_.processPhase("Lobby.Host", params);
            });
        } catch (Exception ex) {
            logger.error("Failed to create game on central server: {}", ex.getMessage());
            SwingUtilities.invokeLater(() ->
                EngineUtils.displayInformationDialog(context_,
                        PropertyConfig.getMessage("msg.online.createfail", ex.getMessage())));
        }
    }, "OnlineConfig-CreateGame");
    t.setDaemon(true);
    t.start();
}
```

**Step 3: Add `buildGameConfig()` helper**

```java
private GameConfig buildGameConfig() {
    return new GameConfig(
            profile_.getName(),
            profile_.getNumPlayers(),
            profile_.getBuyinChips(),
            null   // no password for now
    );
}
```

**Step 4: Add message keys to `client.properties`**

```properties
msg.online.noserver=No online server configured. Go to Options \u2192 Online to set a server URL.
msg.online.notloggedin=You are not logged in to the online server. Switch to an online profile first.
msg.online.createfail=Could not create game: {0}
```

**Step 5: Add phase definitions to `gamedef.xml`**

Add or verify the following phase entries (restore from M7 git history if missing: `git show 9ef67fe3:code/poker/src/main/resources/config/poker/gamedef.xml`).

Also update `online.phase` from `FindGames` to `OnlineMenu` so clicking "Online" from the start menu goes to the new landing page.

```xml
<!-- ONLINE MENU -->
<phase name="OnlineMenu" class="com.donohoedigital.games.poker.online.OnlineMenu" extends="Menu">
  <param name="menubox-title" boolvalue="true"/>
  <param name="menubox-title-prop" strvalue="msg.title.OnlineMenu"/>
  <param name="menubox-help-name" strvalue="onlinemenu"/>
  <param name="default-button" strvalue="hostonline"/>
  <param name="profile.phase" strvalue="PlayerProfileOptions"/>
  <paramlist name="buttons">
    <strvalue>hostonline:OnlineOptions</strvalue>
    <strvalue>joinonline:FindGames</strvalue>
    <strvalue>loadgameonline:LoadOnlineGameMenu</strvalue>
    <strvalue>website:LaunchOnlineWebsite</strvalue>
    <strvalue>forums:LaunchOnlineForums</strvalue>
    <strvalue>helpdetail:Help:online</strvalue>
    <strvalue>cancelprev:PreviousPhase</strvalue>
  </paramlist>
</phase>

<!-- ONLINE CONFIGURATION (setup 2 of 2) -->
<phase name="OnlineConfiguration" extends="TournamentOptions"
       class="com.donohoedigital.games.poker.online.OnlineConfiguration">
  <param name="menubox-title-prop" strvalue="msg.title.OnlineConfiguration"/>
  <param name="menubox-help-name" strvalue="onlineconfig"/>
  <param name="default-button" strvalue="okaybegin"/>
  <param name="profile.phase" strvalue="PlayerProfileOptions"/>
  <paramlist name="buttons">
    <strvalue>okaybegin:Lobby.Host</strvalue>
    <strvalue>helpdetail:Help:hostonline</strvalue>
    <strvalue>cancelprev:PreviousPhase</strvalue>
  </paramlist>
</phase>

<!-- LOBBY (host view) -->
<phase name="Lobby.Host" extends="TournamentOptions"
       class="com.donohoedigital.games.poker.online.Lobby">
  <param name="host" boolvalue="true"/>
  <param name="menubox-title-prop" strvalue="msg.title.Lobby.Host"/>
  <param name="menubox-help-name" strvalue="lobby.host"/>
  <paramlist name="buttons">
    <strvalue>okaystart:HostStart</strvalue>
    <strvalue>editonlinegame</strvalue>
    <strvalue>options:GamePrefsDialog</strvalue>
    <strvalue>helpdetail:Help:hostonline</strvalue>
    <strvalue>canceltourney</strvalue>
  </paramlist>
</phase>

<!-- LOBBY (joining player view) -->
<phase name="Lobby.Player" extends="Lobby.Host">
  <param name="host" boolvalue="false"/>
  <param name="menubox-title-prop" strvalue="msg.title.Lobby.Player"/>
  <param name="menubox-help-name" strvalue="lobby.player"/>
  <param name="default-button" strvalue="NONE"/>
  <paramlist name="buttons">
    <strvalue>info:GameInfoDialogLobby</strvalue>
    <strvalue>options:GamePrefsDialog</strvalue>
    <strvalue>helpdetail:Help:joinonline</strvalue>
    <strvalue>cancelleave:PreviousPhase</strvalue>
  </paramlist>
</phase>

<!-- HOST START (countdown + begin) -->
<phase name="HostStart" class="com.donohoedigital.games.poker.online.HostStart">
  <param name="next-phase" strvalue="InitializeOnlineGame"/>
</phase>
```

**Step 6: Remove now-dead imports and fields**

Remove any imports or fields related to:
- `CommunityHostingConfig`
- `CommunityGameRegistration`
- `EmbeddedGameServer.start()`
- `LanManager` (if referenced)
- LAN/public IP display fields (`lanIpField_`, `publicIpField_`, etc.) — only if unused after the rewrite

Do not remove anything used by the parent `TournamentOptions` class.

**Step 7: Compile**

```bash
cd code && mvn compile -pl poker -am -q
```

Expected: SUCCESS

**Step 8: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineConfiguration.java \
        code/poker/src/main/resources/config/poker/gamedef.xml \
        code/poker/src/main/resources/config/poker/client.properties
git commit -m "feat(online): OnlineConfiguration creates game on central server; add phase definitions"
```

---

## Task 8: Restore `HostStart.java`

`HostStart` provides a countdown sequence before navigating to `InitializeOnlineGame`. It was deleted in M7. Restore a simplified version adapted to the central server architecture.

**Files:**
- Create: `code/poker/src/main/java/com/donohoedigital/games/poker/online/HostStart.java`

**Step 1: Restore from M7 git history**

```bash
git show 9ef67fe3:code/poker/src/main/java/com/donohoedigital/games/poker/online/HostStart.java > /tmp/HostStart.java.orig
```

Review the original. Remove:
- `LanManager.wakeAliveThread()` calls
- AI fill logic (server handles seat filling)
- Any `OnlineManager`, `PokerURL`, or P2P networking references

Keep:
- Countdown timer (3-2-1) posting director chat messages
- `nextPhase()` → `InitializeOnlineGame` on countdown completion

**Step 2: Adapt the class**

The adapted `HostStart` should:
1. On `start()`: begin a 3-second countdown using a Swing `Timer`
2. Each second: add a director message to the lobby chat panel (e.g., "Game starting in 3...", "2...", "1...")
3. On countdown complete: call `context_.processPhase("InitializeOnlineGame")`

**Step 3: Compile**

```bash
cd code && mvn compile -pl poker -am -q
```

Expected: SUCCESS

**Step 4: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/HostStart.java
git commit -m "feat(online): restore HostStart countdown phase for central server game start"
```

---

## Task 9: Fix `Lobby` to use central server REST client, polling, and cancel

`Lobby.java` initialises `restClient_` from the embedded server and has no player list polling. Update to use the central server, add 2-second polling, and cancel the game on exit.

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/Lobby.java`

**Step 1: Locate `restClient_` initialisation**

Search for the line that builds `restClient_` using the embedded server (look for `embeddedServer.getPort()` or `embeddedServer.getLocalUserJwt()`).

**Step 2: Replace embedded server REST client with central server**

```java
// OLD:
EmbeddedGameServer embeddedServer = ((PokerMain) engine_).getEmbeddedServer();
restClient_ = new RestGameClient("http://localhost:" + embeddedServer.getPort(),
                                  embeddedServer.getLocalUserJwt());

// NEW:
String jwt = RestAuthClient.getInstance().getCachedJwt();
String baseUrl = getCentralServerUrl();
if (baseUrl != null && jwt != null) {
    restClient_ = new RestGameClient(baseUrl, jwt);
} else {
    logger.warn("Lobby: no central server URL or JWT — lobby REST operations will fail");
}
```

**Step 3: Add `getCentralServerUrl()` to `Lobby`** (same helper as Tasks 6 and 7)

**Step 4: Add player list polling**

Add field: `private javax.swing.Timer playerPollTimer_;`

In `start()`, after the REST client is set up:

```java
playerPollTimer_ = new javax.swing.Timer(2000, e -> refreshPlayerList());
playerPollTimer_.start();
```

Add method:

```java
private void refreshPlayerList() {
    String gameId = ((PokerGame) context_.getGame()).getWebSocketConfig().gameId();
    try {
        GameSummary summary = restClient_.getGameSummary(gameId);
        updatePlayerTable(summary.players());
    } catch (RestGameClient.RestGameClientException e) {
        logger.warn("Failed to poll lobby player list: {}", e.getMessage());
    }
}
```

In `finish()`, stop the timer:

```java
if (playerPollTimer_ != null) {
    playerPollTimer_.stop();
    playerPollTimer_ = null;
}
```

**Step 5: Cancel game on lobby exit (host only)**

In the cancel/leave action handler:

```java
if (isHost_) {
    String gameId = ((PokerGame) context_.getGame()).getWebSocketConfig().gameId();
    if (gameId != null) {
        try {
            restClient_.cancelGame(gameId);
        } catch (RestGameClient.RestGameClientException e) {
            logger.warn("Failed to cancel game on server during lobby exit: {}", e.getMessage());
        }
    }
}
context_.restart();
```

**Step 6: Compile**

```bash
cd code && mvn compile -pl poker -am -q
```

Expected: SUCCESS

**Step 7: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/Lobby.java
git commit -m "feat(online): Lobby uses central server REST client, adds 2s polling and cancelGame on exit"
```

---

## Task 10: Add player list to `GameSummary` in `pokergameserver`

The Lobby polls `GET /api/v1/games/{gameId}` to refresh its player list. The current `GameSummary` DTO does not include connected players. Add `LobbyPlayerInfo` and wire it in.

**Files:**
- Create: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/dto/LobbyPlayerInfo.java`
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/dto/GameSummary.java`
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/service/GameService.java`
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/GameInstance.java`
- Test: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/service/GameServiceTest.java`

**Step 1: Create `LobbyPlayerInfo`**

```java
package com.donohoedigital.games.poker.gameserver.dto;

/**
 * A player connected to a game lobby.
 *
 * @param name display name
 * @param role "PLAYER" or "OBSERVER"
 */
public record LobbyPlayerInfo(String name, String role) {}
```

**Step 2: Add `players` field to `GameSummary`**

Add `List<LobbyPlayerInfo> players()` to the existing record/class. Default to empty list when not set.

**Step 3: Add `getConnectedPlayers()` to `GameInstance`**

Returns the current `ServerPlayerSession` entries as a list. `ServerPlayerSession` already tracks player name and observer status.

**Step 4: Populate players in `GameService.getGameSummary()`**

```java
List<LobbyPlayerInfo> players = instance.getConnectedPlayers().stream()
    .map(s -> new LobbyPlayerInfo(s.getPlayerName(), s.isObserver() ? "OBSERVER" : "PLAYER"))
    .toList();
// include in GameSummary construction
```

**Step 5: Write tests**

In `GameServiceTest`, verify `getGameSummary()` returns correct player names and roles when sessions are present, and returns an empty list when no players are connected.

**Step 6: Compile and test**

```bash
cd code && mvn test -pl pokergameserver -P dev -q
```

Expected: PASS

**Step 7: Commit**

```bash
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/dto/LobbyPlayerInfo.java \
        code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/dto/GameSummary.java \
        code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/service/GameService.java \
        code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/GameInstance.java \
        code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/service/GameServiceTest.java
git commit -m "feat(pokergameserver): add LobbyPlayerInfo DTO and players list to GameSummary"
```

---

## Task 11: Add login gate to `OnlineMenu`

If the user enters the online flow without a cached JWT, present a login dialog. If login fails or is cancelled, navigate back.

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineMenu.java`

**Step 1: Read `OnlineMenu.java`**

Small phase (~87 lines). Extends a menu base class and overrides `init()` to display the player profile.

**Step 2: Override `start()` to check session**

```java
@Override
public void start() {
    if (!RestAuthClient.getInstance().hasSession()) {
        if (!promptLogin()) {
            context_.processPhase("PreviousPhase");
            return;
        }
    }
    super.start();
}

private boolean promptLogin() {
    String node = Prefs.NODE_OPTIONS + engine_.getPrefsNodeName();
    String server = Prefs.getUserPrefs(node).get(EngineConstants.OPTION_ONLINE_SERVER, "");
    String baseUrl = OnlineServerUrl.normalizeBaseUrl(server);

    if (baseUrl == null) {
        JOptionPane.showMessageDialog(null,
                PropertyConfig.getMessage("msg.online.noserver"),
                PropertyConfig.getMessage("msg.online.login.title"),
                JOptionPane.WARNING_MESSAGE);
        return false;
    }

    JTextField userField = new JTextField(20);
    JPasswordField passField = new JPasswordField(20);
    int result = JOptionPane.showConfirmDialog(null,
            new Object[]{
                PropertyConfig.getMessage("msg.online.login.prompt", baseUrl),
                PropertyConfig.getMessage("msg.online.login.username"), userField,
                PropertyConfig.getMessage("msg.online.login.password"), passField
            },
            PropertyConfig.getMessage("msg.online.login.title"),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);

    if (result != JOptionPane.OK_OPTION) return false;

    try {
        RestAuthClient.getInstance().login(baseUrl, userField.getText().trim(),
                new String(passField.getPassword()));
        return true;
    } catch (RestAuthClient.RestAuthException ex) {
        JOptionPane.showMessageDialog(null, ex.getMessage(),
                PropertyConfig.getMessage("msg.online.login.title"),
                JOptionPane.ERROR_MESSAGE);
        return false;
    }
}
```

Required imports: `javax.swing.JOptionPane`, `javax.swing.JTextField`, `javax.swing.JPasswordField`, `com.donohoedigital.config.Prefs`, `com.donohoedigital.games.engine.EngineConstants`.

**Step 3: Add message keys to `client.properties`**

```properties
msg.online.login.title=Online Login
msg.online.login.prompt=Sign in to {0}
msg.online.login.username=Username:
msg.online.login.password=Password:
```

**Step 4: Compile**

```bash
cd code && mvn compile -pl poker -am -q
```

Expected: SUCCESS

**Step 5: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineMenu.java \
        code/poker/src/main/resources/config/poker/client.properties
git commit -m "feat(online): gate OnlineMenu behind central server login check"
```

---

## Task 12: Full build and test verification

**Step 1: Run full poker module tests**

```bash
cd code && mvn test -P dev -pl poker -q
```

Expected: all tests pass.

**Step 2: Run full project build**

```bash
cd code && mvn test -P dev -q
```

Expected: all modules pass.

**Step 3: Commit any cleanup**

If any compilation warnings appeared and were fixed, commit them here.

---

## Task 13: Manual end-to-end verification

These cannot be automated with the current test harness.

### Prerequisites
- Central server running (Docker or local `mvn clean package -DskipTests && java -jar ...`)
- Central server URL configured in desktop client Options → Online
- At least one registered user account on the central server
- Web client accessible and pointing to the same central server

### Scenario A: Web hosts, Desktop joins

1. Open web client → Login → Games → Create game (2 AI players, 1 human seat)
2. On desktop: click Online → login when prompted → Join → find the game → click Join
3. Verify: desktop shows `Lobby.Player` with at least one player visible
4. Web client: click Start
5. Verify: both clients transition to active game table
6. Play one hand to completion
7. Verify: results screen appears on both clients

### Scenario B: Desktop creates, Web joins

1. On desktop: click Online → Host → select tournament profile → Create
2. Verify: `Lobby.Host` appears with Start button enabled
3. Open web client → Login → Games → find the desktop-created game → Join
4. Verify: player appears in desktop lobby player list within ~2 seconds
5. Desktop: click Start
6. Verify: both clients transition to active game
7. Play one hand to completion

### Scenario C: Practice mode still works (regression)

1. Desktop: click Practice → start a solo AI game
2. Verify: embedded server starts, game proceeds normally without requiring central server login

---

## Out of Scope (Phase 2+)

The following are tracked in the design doc (`2026-03-02-client-parity-design.md`) and will be separate plans:

- Tournament templates server-sync on desktop
- AI show winning hand toggle (web UI)
- Lobby chat panel on `FindGames`
- Profile password validation on desktop
- Omaha (PLO) support
- Game save/resume across server restarts
