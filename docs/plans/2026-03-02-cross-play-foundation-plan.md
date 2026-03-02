# Cross-Play Foundation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Enable the Java desktop client to create and join online games on the central game server so desktop and web clients can play together.

**Architecture:** The embedded server is practice-only. For all online play, both clients hit the central `pokergameserver` via REST (`/api/v1/games/*`) and WebSocket (`/ws/games/{gameId}`). The desktop user authenticates against the central server; their JWT is cached in `RestAuthClient` and threaded through the online flow.

**Tech Stack:** Java 25, Java Swing, `java.net.http.WebSocket`, JUnit 5, Maven (`mvn test -P dev -pl poker`)

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
| `code/poker/src/main/java/com/donohoedigital/games/poker/online/FindGames.java` | Lists and joins games; uses embedded server URL+JWT |
| `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineConfiguration.java` | Starts embedded server; must call central server `createGame` instead |
| `code/poker/src/main/java/com/donohoedigital/games/poker/online/Lobby.java` | Pre-game lobby; builds `restClient_` from embedded server |
| `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineMenu.java` | Online entry point; needs login gate |
| `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineServerUrl.java` | Utility: normalizes configured server string to `http://host:port` |
| `code/poker/src/main/java/com/donohoedigital/games/poker/server/PracticeGameLauncher.java` | Sets `WebSocketConfig` for practice games — must keep using localhost |

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
cd /c/Repos/DDPoker/code && mvn compile -pl poker -am -q
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
cd /c/Repos/DDPoker/code && mvn test -pl poker -Dtest=WebSocketGameClientTest#connect_usesProvidedHostInUrl -q
```

Expected: FAIL — method `connect(String, int, String, String)` does not exist.

**Step 3: Add `host` parameter to `connect()` in `WebSocketGameClient`**

Change the `connect` signature and the `buildWsUrl` helper. The existing `serverPort` field becomes `serverHost` + `serverPort`:

```java
// Change field (around line 71):
private String serverHost;   // was not present before
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

In `Lobby.java` (around line 208) — change:
```java
// OLD:
wsClient_.connect(config.port(), gameId_, config.jwt())
// NEW:
wsClient_.connect(config.host(), config.port(), gameId_, config.jwt())
```

In `WebSocketTournamentDirector.java` (around line 193-198) — add `serverHost_` field and change the connect call:
```java
// Add field near serverPort_:
private String serverHost_;

// In start(), after reading config (around line 193):
serverHost_ = config.host();
serverPort_ = config.port();
gameId_ = config.gameId();
jwt_ = config.jwt();

// Change connect call (around line 198):
wsClient_.connect(serverHost_, serverPort_, gameId_, jwt_);
```

**Step 5: Run the new test**

```bash
cd /c/Repos/DDPoker/code && mvn test -pl poker -Dtest=WebSocketGameClientTest#connect_usesProvidedHostInUrl -q
```

Expected: PASS

**Step 6: Run full poker tests**

```bash
cd /c/Repos/DDPoker/code && mvn test -P dev -pl poker -q
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

The `login()` method returns the JWT but the caller (profile-switch dialog) discards it after one use. The online flow needs to retrieve the JWT later without re-prompting. Add an in-memory session cache to the singleton.

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/RestAuthClient.java`
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/online/RestAuthClientTest.java`

**Step 1: Write the failing test**

Find or create `RestAuthClientTest.java`. Add:

```java
@Test
void login_cachesJwtAndServerUrl() {
    // This test calls the real constructor with a mocked HTTP layer.
    // RestAuthClient uses Jackson + HttpClient internally.
    // We test the caching contract, not the HTTP call itself.
    RestAuthClient client = new RestAuthClient();   // fresh instance, not singleton

    // Manually seed the cache via the package-private setter we'll add:
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
cd /c/Repos/DDPoker/code && mvn test -pl poker -Dtest=RestAuthClientTest -q
```

Expected: FAIL — `cacheSession`, `getCachedJwt`, `hasSession` don't exist.

**Step 3: Add session cache to `RestAuthClient`**

Add fields and methods:

```java
// New fields (thread-safe; login can be called from background threads):
private volatile String cachedJwt_;
private volatile String cachedServerUrl_;

/** Called by login() and externally for testing. Package-private. */
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

Update `login()` to cache on success (after the existing HTTP call, before returning):

```java
public LoginResponse login(String serverUrl, String username, String password)
        throws RestAuthException {
    // ... existing HTTP logic ...
    LoginResponse response = OBJECT_MAPPER.readValue(body, LoginResponse.class);
    cacheSession(serverUrl, response.jwt());   // ← ADD THIS LINE
    return response;
}
```

Also update `logout()` to clear the cache:

```java
public void logout(String serverUrl, String jwt) {
    clearSession();   // ← ADD THIS LINE
    // ... rest of existing logout logic ...
}
```

**Step 4: Run the new tests**

```bash
cd /c/Repos/DDPoker/code && mvn test -pl poker -Dtest=RestAuthClientTest -q
```

Expected: PASS

**Step 5: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/RestAuthClient.java \
        code/poker/src/test/java/com/donohoedigital/games/poker/online/RestAuthClientTest.java
git commit -m "feat(online): cache JWT in RestAuthClient after login for use in online flow"
```

---

## Task 4: Add a `getCentralServerUrl()` helper to `OnlineServerUrl`

Multiple classes need to read the configured central server URL from preferences. Centralise this in `OnlineServerUrl` so there's one place to change.

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
cd /c/Repos/DDPoker/code && mvn test -pl poker -Dtest=OnlineServerUrlTest -q
```

Expected: FAIL — `toWsBaseUrl` doesn't exist.

**Step 3: Add `toWsBaseUrl` to `OnlineServerUrl`**

```java
/**
 * Converts an HTTP(S) base URL to its WebSocket equivalent.
 * {@code http://host:port} → {@code ws://host:port}
 * {@code https://host:port} → {@code wss://host:port}
 * Returns {@code null} for null input.
 */
public static String toWsBaseUrl(String httpBaseUrl) {
    if (httpBaseUrl == null) return null;
    if (httpBaseUrl.startsWith("https://")) {
        return "wss://" + httpBaseUrl.substring("https://".length());
    }
    if (httpBaseUrl.startsWith("http://")) {
        return "ws://" + httpBaseUrl.substring("http://".length());
    }
    return null;
}
```

**Step 4: Run the new tests**

```bash
cd /c/Repos/DDPoker/code && mvn test -pl poker -Dtest=OnlineServerUrlTest -q
```

Expected: PASS

**Step 5: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineServerUrl.java \
        code/poker/src/test/java/com/donohoedigital/games/poker/online/OnlineServerUrlTest.java
git commit -m "feat(online): add toWsBaseUrl helper to OnlineServerUrl"
```

---

## Task 5: Rework `FindGames` to use the central server

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

Add a private helper (same pattern as `ChangePasswordDialog.getServerUrl()`):

```java
/** Returns the configured central server HTTP base URL, or null if not set. */
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

Replace the entire method body:

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

In the join button handler (around line 192–198), replace:

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

Apply the same fix to the observe action (around line 226–234):

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

The `extractPort()` private method can stay (it may be used elsewhere) but is no longer called from the join/observe paths.

**Step 4: Compile**

```bash
cd /c/Repos/DDPoker/code && mvn compile -pl poker -am -q
```

Expected: SUCCESS

**Step 5: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/FindGames.java
git commit -m "feat(online): FindGames uses central server REST API and JWT instead of embedded server"
```

---

## Task 6: Rework `OnlineConfiguration` to create game on central server

`OnlineConfiguration.doOpenLobby()` currently starts the embedded server as an externally-accessible host. Replace with a REST `createGame` call to the central server.

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineConfiguration.java`

**Background — current `doOpenLobby()` (around line 270–320):**

```java
// CURRENT (simplified):
embeddedServer.start(CommunityHostingConfig.getPort(), ...);   // REMOVE
int runningPort = embeddedServer.getPort();                    // REMOVE
String localJwt = embeddedServer.getLocalUserJwt();           // REPLACE
RestGameClient localClient = new RestGameClient("http://localhost:" + runningPort, localJwt);
GameSummary summary = localClient.createGame(gameConfig);
// WAN registration logic ...                                  // REMOVE
game.setWebSocketConfig(gameId, localJwt, runningPort);        // REPLACE
```

**Step 1: Add `getCentralServerUrl()` helper** (same pattern as Task 5):

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

**Step 2: Rewrite `doOpenLobby()`**

Replace the method body with:

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

    // Build GameConfig from the selected TournamentProfile.
    // TournamentOptions (parent class) already populated profile_ by this point.
    GameConfig gameConfig = buildGameConfig();

    // Run the REST call off the EDT.
    Thread t = new Thread(() -> {
        try {
            RestGameClient client = new RestGameClient(baseUrl, jwt);
            GameSummary summary = client.createGame(gameConfig);
            String gameId = summary.gameId();

            // Parse WebSocket host from the server's wsUrl.
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

This converts the `TournamentProfile` (already selected by the parent `TournamentOptions` step) into a `GameConfig` for the REST API. The existing `OnlineConfiguration` already has access to the profile — extract what's needed:

```java
private GameConfig buildGameConfig() {
    // profile_ is inherited from TournamentOptions and populated before doOpenLobby()
    return new GameConfig(
            profile_.getName(),
            profile_.getNumPlayers(),
            profile_.getBuyinChips(),
            null   // no password for now
    );
}
```

Check that `GameConfig` and the existing `createGame()` contract are satisfied — `RestGameClient.createGame(GameConfig)` calls `POST /api/v1/games`. The `GameConfig` record is in `pokergameserver` module.

**Step 4: Add missing message keys to `client.properties`**

File: `code/poker/src/main/resources/config/poker/client.properties`

Add:
```properties
msg.online.noserver=No online server configured. Go to Options \u2192 Online to set a server URL.
msg.online.notloggedin=You are not logged in to the online server. Switch to an online profile first.
msg.online.createfail=Could not create game: {0}
```

**Step 5: Remove now-dead imports and fields**

Remove any imports or fields related to:
- `CommunityHostingConfig`
- `CommunityGameRegistration`
- `EmbeddedGameServer.start()`
- `LanManager` (if referenced)
- LAN/public IP display fields (`lanIpField_`, `publicIpField_`, etc.) — only if they exist and are unused after the rewrite

Do not remove anything used by the parent `TournamentOptions` class.

**Step 6: Compile**

```bash
cd /c/Repos/DDPoker/code && mvn compile -pl poker -am -q
```

Expected: SUCCESS

**Step 7: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineConfiguration.java \
        code/poker/src/main/resources/config/poker/client.properties
git commit -m "feat(online): OnlineConfiguration creates game on central server instead of starting embedded host"
```

---

## Task 7: Fix `Lobby` to use central server REST client

`Lobby.java` initialises `restClient_` from the embedded server. For the central server flow the lobby must use the central server URL and the cached JWT.

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/Lobby.java`

**Step 1: Locate `restClient_` initialisation**

Search for the line in `Lobby.java` that builds `restClient_` using the embedded server (look for `embeddedServer.getPort()` or `embeddedServer.getLocalUserJwt()`).

**Step 2: Replace embedded server REST client with central server**

Replace:

```java
// OLD (whatever form it takes):
EmbeddedGameServer embeddedServer = ((PokerMain) engine_).getEmbeddedServer();
restClient_ = new RestGameClient("http://localhost:" + embeddedServer.getPort(),
                                  embeddedServer.getLocalUserJwt());

// NEW:
String jwt = RestAuthClient.getInstance().getCachedJwt();
String baseUrl = getCentralServerUrl();   // same helper as Tasks 5+6
if (baseUrl != null && jwt != null) {
    restClient_ = new RestGameClient(baseUrl, jwt);
} else {
    logger.warn("Lobby: no central server URL or JWT — lobby REST operations will fail");
}
```

**Step 3: Add `getCentralServerUrl()` to `Lobby`**

Same helper as Tasks 5 and 6 — copy/paste. (This repeated pattern is a candidate for a future shared utility, but YAGNI for now: three copies is acceptable until it actually causes pain.)

**Step 4: Compile**

```bash
cd /c/Repos/DDPoker/code && mvn compile -pl poker -am -q
```

Expected: SUCCESS

**Step 5: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/Lobby.java
git commit -m "feat(online): Lobby uses central server REST client for startGame/cancelGame/polling"
```

---

## Task 8: Add login gate to `OnlineMenu`

If the user enters the online flow without a cached JWT (e.g., first launch, or session cleared), present a login dialog before showing the menu. If login fails or is cancelled, navigate back to the previous phase.

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineMenu.java`

**Step 1: Read `OnlineMenu.java`**

It is a small phase (~87 lines). It extends a menu base class and overrides `init()` to display the player profile.

**Step 2: Override `start()` to check session**

Add to `OnlineMenu.java`:

```java
@Override
public void start() {
    if (!RestAuthClient.getInstance().hasSession()) {
        // Prompt for login before showing the menu.
        if (!promptLogin()) {
            context_.processPhase("PreviousPhase");
            return;
        }
    }
    super.start();
}

/**
 * Shows a username/password dialog and attempts login against the configured
 * central server. Returns true on success, false on cancel or failure.
 */
private boolean promptLogin() {
    String node = Prefs.NODE_OPTIONS + engine_.getPrefsNodeName();
    String server = Prefs.getUserPrefs(node).get(EngineConstants.OPTION_ONLINE_SERVER, "");
    String baseUrl = OnlineServerUrl.normalizeBaseUrl(server);

    if (baseUrl == null) {
        JOptionPane.showMessageDialog(
                null,
                PropertyConfig.getMessage("msg.online.noserver"),
                PropertyConfig.getMessage("msg.online.login.title"),
                JOptionPane.WARNING_MESSAGE);
        return false;
    }

    JTextField userField = new JTextField(20);
    JPasswordField passField = new JPasswordField(20);
    int result = JOptionPane.showConfirmDialog(
            null,
            new Object[]{
                PropertyConfig.getMessage("msg.online.login.prompt", baseUrl),
                PropertyConfig.getMessage("msg.online.login.username"), userField,
                PropertyConfig.getMessage("msg.online.login.password"), passField
            },
            PropertyConfig.getMessage("msg.online.login.title"),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);

    if (result != JOptionPane.OK_OPTION) {
        return false;
    }

    try {
        RestAuthClient.getInstance().login(baseUrl, userField.getText().trim(),
                new String(passField.getPassword()));
        return true;
    } catch (RestAuthClient.RestAuthException ex) {
        JOptionPane.showMessageDialog(
                null,
                ex.getMessage(),
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
cd /c/Repos/DDPoker/code && mvn compile -pl poker -am -q
```

Expected: SUCCESS

**Step 5: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineMenu.java \
        code/poker/src/main/resources/config/poker/client.properties
git commit -m "feat(online): gate OnlineMenu behind central server login check"
```

---

## Task 9: Full build and test verification

**Step 1: Run full poker module tests**

```bash
cd /c/Repos/DDPoker/code && mvn test -P dev -pl poker -q
```

Expected: all tests pass.

**Step 2: Run full project build**

```bash
cd /c/Repos/DDPoker/code && mvn test -P dev -q
```

Expected: all modules pass.

**Step 3: Commit any cleanup**

If any compilation warnings appeared and were fixed, commit them here.

---

## Task 10: Manual end-to-end verification

These cannot be automated with the current test harness. Record results as comments on this plan.

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
6. Play one hand to completion (fold or call through to showdown)
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
