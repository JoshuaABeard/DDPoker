# Phase 1: Server as Relay Implementation

**Status:** Planning
**Created:** 2026-02-12
**Estimated Effort:** 1-2 days
**Priority:** High

---

## Summary

Implement a TCP relay server that allows all clients to connect to the central server instead of directly to the host. The host client still runs the game engine (TournamentDirector) but connects TO the relay instead of listening for incoming connections.

**Before:** `Client → Host (NAT/firewall issues)`
**After:** `Client → Server Relay → Host (no NAT issues)`

---

## Architecture

```
┌─────────┐         ┌──────────────┐         ┌──────────┐
│ Client  │◄───────►│ Server Relay │◄───────►│   Host   │
│ (Join)  │         │  (Forward)   │         │ (Engine) │
└─────────┘         └──────────────┘         └──────────┘
     │                      │                      │
     └──────────────────────┴──────────────────────┘
              All traffic relayed through server
```

### Key Components

1. **GameRelay** (server) - Manages relay sessions, forwards messages
2. **RelaySession** (server) - Tracks one game's connections (1 host + N clients)
3. **RelayConnectionServer** (client) - Host-side connection that connects OUT instead of listening
4. **Protocol Extension** - Relay handshake and game ID routing

---

## Implementation Steps

### Step 1: Server-Side Relay Infrastructure

**New Class: `GameRelay.java`** (`code/pokerserver/src/main/java/.../server/`)

```java
public class GameRelay {
    private final Map<String, RelaySession> sessions = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // Create a new relay session when host registers game
    public RelaySession createSession(String gameId, Socket hostSocket) {
        RelaySession session = new RelaySession(gameId, hostSocket);
        sessions.put(gameId, session);
        return session;
    }

    // Client joins existing relay session
    public void joinSession(String gameId, Socket clientSocket) {
        RelaySession session = sessions.get(gameId);
        if (session != null) {
            session.addClient(clientSocket);
        }
    }

    // Cleanup when game ends
    public void removeSession(String gameId) {
        RelaySession session = sessions.remove(gameId);
        if (session != null) {
            session.close();
        }
    }
}
```

**New Class: `RelaySession.java`**

```java
public class RelaySession implements Runnable {
    private final String gameId;
    private final Socket hostSocket;
    private final List<Socket> clientSockets = new CopyOnWriteArrayList<>();
    private final Thread relayThread;

    public RelaySession(String gameId, Socket hostSocket) {
        this.gameId = gameId;
        this.hostSocket = hostSocket;
        this.relayThread = new Thread(this, "RelaySession-" + gameId);
        relayThread.start();
    }

    public void addClient(Socket clientSocket) {
        clientSockets.add(clientSocket);
        // Start forwarding thread for this client
        startClientForwarding(clientSocket);
    }

    @Override
    public void run() {
        // Read from host, broadcast to all clients
        try (InputStream in = hostSocket.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                // Forward to all connected clients
                broadcastToClients(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            // Host disconnected - end session
        } finally {
            close();
        }
    }

    private void startClientForwarding(Socket clientSocket) {
        // Read from client, forward to host
        Thread thread = new Thread(() -> {
            try (InputStream in = clientSocket.getInputStream();
                 OutputStream out = hostSocket.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    out.flush();
                }
            } catch (IOException e) {
                // Client disconnected
                clientSockets.remove(clientSocket);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void broadcastToClients(byte[] data, int offset, int length) {
        for (Socket client : clientSockets) {
            try {
                OutputStream out = client.getOutputStream();
                out.write(data, offset, length);
                out.flush();
            } catch (IOException e) {
                // Client disconnected
                clientSockets.remove(client);
            }
        }
    }

    public void close() {
        // Close all sockets
        try { hostSocket.close(); } catch (IOException ignored) {}
        for (Socket client : clientSockets) {
            try { client.close(); } catch (IOException ignored) {}
        }
    }
}
```

---

### Step 2: Integrate Relay into PokerServer

**Modify: `PokerServer.java`**

```java
public class PokerServer {
    private GameRelay gameRelay;
    private ServerSocket relayServerSocket;

    public void start() {
        // ... existing code ...

        // Start relay server on port 8878 (game server port + 1)
        startRelayServer();
    }

    private void startRelayServer() {
        int relayPort = getGameServerPort() + 1; // 8878
        try {
            relayServerSocket = new ServerSocket(relayPort);
            gameRelay = new GameRelay();

            // Accept relay connections
            Thread relayAcceptThread = new Thread(() -> {
                while (!relayServerSocket.isClosed()) {
                    try {
                        Socket socket = relayServerSocket.accept();
                        handleRelayConnection(socket);
                    } catch (IOException e) {
                        // Server stopped
                    }
                }
            });
            relayAcceptThread.setDaemon(true);
            relayAcceptThread.start();

            logger.info("Game relay server started on port " + relayPort);
        } catch (IOException e) {
            logger.error("Failed to start relay server", e);
        }
    }

    private void handleRelayConnection(Socket socket) {
        // Read handshake to determine if this is host or client
        // Protocol: "RELAY_HOST <gameId>" or "RELAY_CLIENT <gameId>"
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            String command = in.readUTF();
            String gameId = in.readUTF();

            if ("RELAY_HOST".equals(command)) {
                gameRelay.createSession(gameId, socket);
            } else if ("RELAY_CLIENT".equals(command)) {
                gameRelay.joinSession(gameId, socket);
            }
        } catch (IOException e) {
            logger.error("Relay handshake failed", e);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
```

---

### Step 3: Client-Side Relay Connection

**New Class: `RelayConnectionServer.java`** (`code/pokernetwork/src/main/java/.../network/`)

```java
/**
 * Connection server that connects OUT to a relay instead of listening for connections.
 * Used when host creates a server-relayed game.
 */
public class RelayConnectionServer extends PokerConnectionServer {
    private final String relayHost;
    private final int relayPort;
    private final String gameId;
    private Socket relaySocket;

    public RelayConnectionServer(String relayHost, int relayPort, String gameId) {
        this.relayHost = relayHost;
        this.relayPort = relayPort;
        this.gameId = gameId;
    }

    @Override
    public void start() throws IOException {
        // Connect to relay server
        relaySocket = new Socket(relayHost, relayPort);

        // Send handshake: identify as host
        DataOutputStream out = new DataOutputStream(relaySocket.getOutputStream());
        out.writeUTF("RELAY_HOST");
        out.writeUTF(gameId);
        out.flush();

        // Now all client connections will come through this socket
        // Start message processing thread
        startMessageProcessing();
    }

    @Override
    public void stop() {
        if (relaySocket != null) {
            try { relaySocket.close(); } catch (IOException ignored) {}
        }
    }

    private void startMessageProcessing() {
        // Similar to existing PokerTCPServer logic
        // But reads from single relaySocket instead of accepting multiple connections
        Thread thread = new Thread(() -> {
            try (DataInputStream in = new DataInputStream(relaySocket.getInputStream())) {
                while (!relaySocket.isClosed()) {
                    // Read and process messages
                    processMessage(in);
                }
            } catch (IOException e) {
                // Connection lost
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    // ... implement abstract methods from PokerConnectionServer ...
}
```

---

### Step 4: Client Relay Connection (Joining)

**Modify: `PokerConnectionClient.java`**

```java
public class PokerConnectionClient {

    public void connect(String host, int port, String gameId, boolean useRelay) throws IOException {
        if (useRelay) {
            // Connect to relay server instead of directly to host
            connectViaRelay(host, port, gameId);
        } else {
            // Direct connection (existing behavior)
            connectDirect(host, port);
        }
    }

    private void connectViaRelay(String relayHost, int relayPort, String gameId) throws IOException {
        socket_ = new Socket(relayHost, relayPort);

        // Send handshake: identify as client
        DataOutputStream out = new DataOutputStream(socket_.getOutputStream());
        out.writeUTF("RELAY_CLIENT");
        out.writeUTF(gameId);
        out.flush();

        // Continue with normal connection setup
        startMessageProcessing();
    }

    private void connectDirect(String host, int port) throws IOException {
        // Existing direct connection logic
        socket_ = new Socket(host, port);
        startMessageProcessing();
    }
}
```

---

### Step 5: Game Registration Changes

**Modify: `PokerServlet.java`** (or equivalent game registration endpoint)

When host creates a game, check if "server relay" option is selected:

```java
private void addWanGame(HttpServletRequest request, ...) {
    // ... existing validation ...

    boolean useRelay = Boolean.parseBoolean(request.getParameter("useRelay"));

    if (useRelay) {
        // Store relay info in OnlineGame
        game.setRelayEnabled(true);
        game.setRelayHost(getServerHostname()); // e.g., "poker.example.com"
        game.setRelayPort(8878); // Relay port
    } else {
        // Direct connection (existing behavior)
        game.setRelayEnabled(false);
        game.setUrl(hostUrl); // Host's direct IP:port
    }

    // Save game
    onlineGameService.save(game);
}
```

**Modify: `OnlineGame.java`** (entity)

```java
public class OnlineGame {
    // ... existing fields ...

    @Column(name = "relay_enabled")
    private boolean relayEnabled;

    @Column(name = "relay_host")
    private String relayHost;

    @Column(name = "relay_port")
    private int relayPort;

    // Getters/setters
}
```

---

### Step 6: UI Changes

**Add "Use Server Relay" Checkbox** to tournament creation UI

**Client: `TournamentProfileDialog.java`** (Online tab)

```java
// Add checkbox in Online tab
JCheckBox relayCheckbox = new JCheckBox("Use Server Relay (recommended)");
relayCheckbox.setSelected(true); // Default to ON
relayCheckbox.setToolTipText(
    "Route all connections through server (no port forwarding needed)"
);
```

**Web: `code/web/app/online/create-game/page.tsx`** (if applicable)

```tsx
<label>
  <input
    type="checkbox"
    name="useRelay"
    defaultChecked
  />
  Use Server Relay (recommended - no port forwarding required)
</label>
```

---

## Protocol Design

### Relay Handshake Protocol

**Host → Relay:**
```
RELAY_HOST <gameId>
```

**Client → Relay:**
```
RELAY_CLIENT <gameId>
```

**Relay Behavior:**
- First connection with `RELAY_HOST` for a gameId creates a new `RelaySession`
- Subsequent connections with `RELAY_CLIENT` join the existing session
- All messages are forwarded bidirectionally:
  - `Host → Relay → All Clients`
  - `Client → Relay → Host`

---

## Testing Strategy

### Unit Tests

1. **RelaySessionTest.java**
   - Test session creation
   - Test client join
   - Test message forwarding (host → clients)
   - Test message forwarding (client → host)
   - Test client disconnect handling
   - Test host disconnect handling

2. **GameRelayTest.java**
   - Test concurrent sessions
   - Test session cleanup
   - Test invalid gameId handling

### Integration Tests

3. **RelayIntegrationTest.java**
   - Create relay session
   - Connect host
   - Connect 2 clients
   - Send messages both directions
   - Verify message delivery

### Manual Tests

4. **End-to-End Relay Test**
   - Start PokerServer with relay enabled
   - Create game with "Use Server Relay" checked
   - Join from 2 different machines
   - Verify game functions normally
   - Test chat, betting, all game features

---

## Configuration

**Add to `application.properties`:**

```properties
# Game Relay Configuration
game.relay.enabled=true
game.relay.port=8878
game.relay.max.sessions=100
game.relay.session.timeout.ms=14400000
```

---

## Migration Strategy

### Backward Compatibility

- **Relay is OPTIONAL** - existing direct connections still work
- Games created before this feature use direct connections
- Games created after can choose relay or direct
- No database migration needed (just add new nullable columns)

### Rollout Plan

**Phase 1.1: Server Relay (This Plan)**
- Deploy relay server
- Add "Use Server Relay" checkbox (default ON for new games)
- Monitor relay performance

**Phase 1.2: Default to Relay**
- After 1-2 weeks of stable relay operation
- Make relay the default for all new games
- Keep direct connection as fallback option

**Phase 1.3: Deprecate Direct**
- After 1-2 months
- Remove direct connection option from UI
- Keep code for backward compatibility with old games

---

## Performance Considerations

### Latency
- Relay adds one network hop: `Client → Server → Host → Server → Client`
- Expect ~10-50ms additional latency (depends on server location)
- Acceptable for turn-based poker (actions are seconds apart)

### Bandwidth
- Each message is sent twice (relay doubles bandwidth)
- Poker messages are small (~1KB typical)
- Not a concern for game server (100+ concurrent games feasible)

### Scaling
- Each RelaySession uses 2 threads (host reader + N client readers)
- 100 concurrent games with 10 players each = ~1000 threads
- Well within Java thread pool capabilities

---

## Security Considerations

### Connection Authentication

**Problem:** Relay allows anyone to join a game if they know the gameId

**Solution:** Add authentication token

```java
// When client requests to join game via API
String joinToken = generateJoinToken(gameId, username);
response.setJoinToken(joinToken);

// Client includes token in relay handshake
out.writeUTF("RELAY_CLIENT");
out.writeUTF(gameId);
out.writeUTF(joinToken); // NEW: auth token

// Relay validates token before allowing join
if (!validateJoinToken(gameId, joinToken)) {
    socket.close();
    return;
}
```

### DoS Protection

- Limit max clients per session (e.g., 10)
- Timeout idle sessions (e.g., 4 hours)
- Rate limit relay connection attempts per IP

---

## Success Criteria

### Functional
- ✅ Host can create relay game
- ✅ Clients can join relay game
- ✅ All game features work through relay
- ✅ Host disconnect ends game gracefully
- ✅ Client disconnect doesn't affect others

### Non-Functional
- ✅ Latency < 100ms added by relay
- ✅ 20+ concurrent relay games without issues
- ✅ No memory leaks over 24-hour run
- ✅ All existing tests still pass

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Message ordering issues | High | Use TCP (guaranteed ordering) |
| Relay becomes bottleneck | Medium | Monitor performance, optimize if needed |
| Host disconnect kills game | Medium | Document limitation, implement Phase 2 later |
| Security: unauthorized joins | High | Implement join token authentication |
| Increased latency | Low | Acceptable for turn-based game |

---

## Future Enhancements (Phase 2)

After Phase 1 is stable, consider:

1. **Server-Hosted Games (Phase 2)**
   - Server spawns headless JVM per game
   - Relay routes to `localhost:<port>`
   - Game survives creator disconnect

2. **Connection Pooling**
   - Multiplex multiple games over single TCP connection
   - Reduce connection overhead

3. **Compression**
   - Compress messages before relaying
   - Reduce bandwidth usage

---

## Implementation Checklist

**Server-Side:**
- [ ] Create `GameRelay.java`
- [ ] Create `RelaySession.java`
- [ ] Integrate into `PokerServer.java`
- [ ] Add relay port configuration
- [ ] Add database columns for relay info
- [ ] Update game registration servlet

**Client-Side:**
- [ ] Create `RelayConnectionServer.java`
- [ ] Update `PokerConnectionClient.java`
- [ ] Update `OnlineManager.java` to use relay mode
- [ ] Add "Use Server Relay" checkbox to UI

**Testing:**
- [ ] Write unit tests for relay classes
- [ ] Write integration tests
- [ ] Manual end-to-end testing
- [ ] Performance testing (latency, concurrent games)

**Documentation:**
- [ ] Update deployment docs
- [ ] Update user guide
- [ ] Add troubleshooting section

---

## Next Steps

1. Review this plan with user
2. Start with Step 1: Server-Side Relay Infrastructure
3. Use TDD: write tests first, then implementation
4. Test each step before proceeding
5. Deploy to staging environment for testing
6. Monitor performance and stability
7. Roll out to production

---

**Estimated Timeline:**
- Server infrastructure: 4-6 hours
- Client integration: 3-4 hours
- Testing: 2-3 hours
- Documentation: 1 hour
- **Total: 1-2 days**
