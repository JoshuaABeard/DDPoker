# Plan: Convert All Point-to-Point UDP Communication to TCP

## Context

DD Poker's networking uses UDP with a custom reliability layer (`UDPLink`: ACKs, resends, MTU discovery, session management, message fragmentation — 1,500+ lines) for game P2P connections and lobby chat. This creates two problems:

1. **Docker compatibility**: Docker Desktop for Windows has known limitations with UDP port mapping, making local development and testing difficult
2. **Unnecessary complexity**: The reliability layer reimplements what TCP provides natively — guaranteed delivery, ordering, connection state — with significantly more code to maintain

This plan converts **all point-to-point UDP** to TCP, while keeping UDP multicast for LAN game discovery (which has no TCP equivalent). In-game chat for server-hosted games already uses TCP via `OnlineManagerQueue` → `GameServer`, so only lobby chat and P2P game connections need conversion.

## Current Architecture

### Three UDP Uses

| Use | Port | Transport | Conversion |
|-----|------|-----------|------------|
| Game P2P (actions + chat) | 11889 | `PokerUDPServer` → `UDPLink` | **→ TCP** |
| Lobby chat server | 11886 | `ChatServer` → `UDPLink` | **→ TCP** |
| LAN discovery | 7755 multicast | `Peer2PeerMulticast` → `MulticastSocket` | **Keep UDP** |

### Key Abstractions Already in Place

- **[PokerConnectionServer](code/pokernetwork/src/main/java/com/donohoedigital/games/poker/network/PokerConnectionServer.java)** — Interface with `init()`, `isBound()`, `start()`, `shutdown()`, `send()`, `closeConnection()`, `newMessage()`. Implemented by both `PokerUDPServer` (UDP) and `PokerTCPServer` (TCP, inner class of `PokerMain`).
- **[PokerConnection](code/pokernetwork/src/main/java/com/donohoedigital/games/poker/network/PokerConnection.java)** — Dual-mode wrapper with `UDPID udpConn` + `SocketChannel tcpConn`, `isTCP()`, `isUDP()`.
- **[Peer2PeerMessage](code/server/src/main/java/com/donohoedigital/p2p/Peer2PeerMessage.java)** — Existing TCP wire protocol with length-prefix framing, CRC32 validation, and timeout handling. Wire format: `'D'(2) | protocol(4) | type(4) | size(4) | CRC32(8) | DDMessage payload`.
- **[Peer2PeerServer](code/server/src/main/java/com/donohoedigital/p2p/Peer2PeerServer.java)** / **[Peer2PeerClient](code/server/src/main/java/com/donohoedigital/p2p/Peer2PeerClient.java)** — NIO TCP server/client infrastructure with Selector, thread pool, non-blocking connect, keep-alive.
- **[OnlineManager.isUDP()](code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineManager.java)** — Checks `p2p_ instanceof UDPServer` to branch between UDP and TCP send paths. TCP path uses `OnlineManagerQueue` for message delivery.

### What TCP Eliminates

| UDP Feature | Lines | TCP Equivalent |
|-------------|-------|----------------|
| ACK/resend logic | ~300 | Built into TCP |
| MTU discovery | ~150 | TCP handles fragmentation |
| Multi-part message assembly | ~200 | Stream-based, no fragmentation |
| Session IDs | ~100 | Connection IS the session |
| `IncomingQueue` reordering | ~200 | TCP guarantees ordering |
| `AckList` tracking | ~150 | Not needed |

---

## Phase 1: Game P2P → TCP Only

### Goal

Make all game P2P communication use TCP exclusively. Low risk because `PokerTCPServer` already exists and the TCP code path in `OnlineManager` already works for server-hosted games.

### TDD Tests (Write Before Implementation)

**File**: `code/server/src/test/java/com/donohoedigital/p2p/Peer2PeerMessageTest.java`

| Test | Purpose |
|------|---------|
| `testWriteAndReadRoundTrip` | Write DDMessage → Peer2PeerMessage → read back, verify content matches |
| `testCRC32ValidationRejectsCorruption` | Flip a byte in CRC, verify read throws error |
| `testLargeMessageNearSizeLimit` | Message near 500KB limit writes/reads correctly |
| `testEmptyDDMessage` | Empty message serializes and deserializes |
| `testPartialReadFromSlowChannel` | Data arriving in small chunks still reads correctly |
| `testEOFDuringReadThrows` | Channel closes mid-read, verify clean error |
| `testWriteTimeoutOnSlowChannel` | Slow writer gets timeout exception |

**File**: `code/pokernetwork/src/test/java/com/donohoedigital/games/poker/network/PokerConnectionTcpTest.java`

| Test | Purpose |
|------|---------|
| `testTcpConnectionIsTcp` | `isTCP()` true, `isUDP()` false |
| `testTcpConnectionEquality` | Two connections wrapping same channel are equal |
| `testTcpConnectionHashCode` | Consistent hashCode |
| `testTcpConnectionToString` | Returns IP address |

### Implementation

1. **[TournamentOptions.java](code/poker/src/main/java/com/donohoedigital/games/poker/TournamentOptions.java)** (~line 255) — Always generate TCP-prefix game IDs (`ONLINE_GAME_PREFIX_TCP` / `n-` prefix). Remove conditional logic that selects `ONLINE_GAME_PREFIX_UDP` / `u-` prefix.

2. **[OnlineManager.java](code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineManager.java)** — Remove `isUDP()` method and all `if (isUDP())` branches. Consolidate to TCP-only path using `oQueue_.addMessage()`. Remove UDP-specific imports.

3. **[PokerMain.java](code/poker/src/main/java/com/donohoedigital/games/poker/PokerMain.java)** — Simplify `getPokerConnectionServer()` to always return `PokerTCPServer`. Remove `UDPLinkHandler`, `UDPManagerMonitor`, `UDPLinkMonitor` implementations for game connections (keep for chat temporarily).

4. **[PokerConnect.java](code/pokernetwork/src/main/java/com/donohoedigital/games/poker/network/PokerConnect.java)** — Rewrite to use `Peer2PeerClient` instead of `UDPLink` for connecting to game hosts.

5. **[PokerGame.java](code/poker/src/main/java/com/donohoedigital/games/poker/PokerGame.java)** — Remove `isUDP()` usage. Always request TCP connection server.

### Edge Cases

| Edge Case | How TCP Handles It | Test Strategy |
|-----------|--------------------|---------------|
| Connection drop mid-game | IOException on read/write, immediate detection | Mock channel that throws IOException |
| Partial message delivery | `Peer2PeerMessage.readBytes()` loops until complete | Test with slow-drip mock channel |
| Client reconnection | New SocketChannel, old connection cleaned up via `socketClosing()` | Test connect → disconnect → reconnect |
| Half-open connection | TCP keepalive or application heartbeat (alive thread exists) | Verify alive thread detects dead connections |
| Back-pressure from slow receiver | TCP write blocks; `Peer2PeerMessage` has write timeout | Test with slow reader channel |
| Saved games with `u-` prefix | Treat `u-` as `n-` on load (backward compat) | Unit test for prefix migration |

### Verification

1. All new tests pass (`mvn test -pl server,pokernetwork`)
2. Existing `PokerGameTest`, `PokerTableTest` pass
3. Manual: Create online game → client joins → play hand → in-game chat works
4. Manual: Kill client mid-game → host detects disconnect → client rejoins
5. `mvn clean test` across all modules

---

## Phase 2: Lobby Chat → TCP

### Goal

Convert the lobby chat system from UDP to TCP. More involved because `ChatServer` is tightly coupled to `UDPLink`/`UDPLinkHandler`/`UDPLinkMonitor`. The chat server is a **long-lived push server** that broadcasts messages to all connected clients — different from the request-response game server model.

### TDD Tests (Write Before Implementation)

**File**: `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/TcpChatServerTest.java`

| Test | Purpose |
|------|---------|
| `testClientConnectReceivesWelcome` | Connect, send HELLO, receive WELCOME with player list |
| `testChatBroadcastToOtherClients` | Client A sends chat, Client B receives it, Client A does not |
| `testClientDisconnectNotifiesOthers` | Client disconnects, others receive LEAVE notification |
| `testDuplicateKeyRejected` | Second client with same key gets error |
| `testDuplicateProfileRejected` | Second client with same profile name gets error |
| `testClientReconnectAfterDisconnect` | Client reconnects, gets fresh WELCOME |
| `testBroadcastDoesNotBlockOnSlowClient` | Slow client does not prevent delivery to fast clients |
| `testServerShutdownDisconnectsAll` | All clients disconnected cleanly on shutdown |
| `testConcurrentConnectDisconnect` | 10 clients connect/disconnect rapidly, no race conditions |
| `testInvalidAuthRejected` | Bad credentials → error + disconnect |

**File**: `code/poker/src/test/java/com/donohoedigital/games/poker/TcpChatClientTest.java`

| Test | Purpose |
|------|---------|
| `testConnectAndSendHello` | Connect to chat server, send HELLO, receive WELCOME |
| `testReceiveBroadcastChat` | Receive chat broadcast from server |
| `testConnectionLostDetection` | Server goes away, client detects and notifies handler |
| `testReconnectAfterLoss` | Client auto-reconnects after connection loss |
| `testReceivePlayerList` | WELCOME message contains correct player list |

### New Files

1. **`code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/TcpChatServer.java`** — TCP-based chat server extending `GameServer`. Manages a list of `ChatConnection` objects (each wrapping a `SocketChannel`). Uses per-client write queues for push broadcasts. Implements the same validation logic as current `ChatServer` (auth, ban checks, duplicate detection).

2. **`code/pokernetwork/src/main/java/com/donohoedigital/games/poker/network/TcpChatClient.java`** — Client-side class replacing `PokerUDPServer.chatLink_`. Opens `SocketChannel` to chat server, sends HELLO, starts reader thread for incoming messages, provides `sendChat()`. Implements `ChatLobbyManager`.

### Files Modified

| File | Change |
|------|--------|
| [PokerMain.java](code/poker/src/main/java/com/donohoedigital/games/poker/PokerMain.java) | Replace `getChatServer()` to return `TcpChatClient`. Remove all remaining UDP monitor implementations. Remove `udp_` field. |
| [OnlineLobby.java](code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineLobby.java) | Use `TcpChatClient` via `ChatLobbyManager` interface instead of `PokerUDPServer`. |
| [PokerServer.java](code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/PokerServer.java) | Replace `UDPServer udp_` + `ChatServer chat_` with `TcpChatServer`. Remove `UDPLinkHandler`/`UDPManagerMonitor` implementations. |
| [PokerServlet.java](code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/PokerServlet.java) | Remove UDP references for P2P connect attempts. |
| [PokerUDPServer.java](code/poker/src/main/java/com/donohoedigital/games/poker/PokerUDPServer.java) | Remove chat methods (`chatLink_`, `sendChat()`, `checkConnected()`, `closeChatLink()`). After Phase 1 removed game methods, this file may be deletable here. |

### Edge Cases

| Edge Case | Handling | Test |
|-----------|----------|------|
| Slow receiver blocking broadcast | Per-client write queue with timeout; disconnect slow clients after threshold | `testBroadcastDoesNotBlockOnSlowClient` |
| Chat server restart | Client IOException on read, reconnect with backoff | `testReconnectAfterLoss` |
| Very long chat message | `Peer2PeerMessage` 500KB limit; validate on client before send | Unit test for message size validation |
| Empty chat message | Reject on client side before sending | Client-side validation test |
| Rapid connect/disconnect | Synchronized connection list, cleanup in `socketClosing()` | `testConcurrentConnectDisconnect` |
| `./stats` and `./dump` debug commands | Reimplement in `TcpChatServer` — return server stats as chat message | Manual verification |

### Verification

1. All new tests pass (`mvn test -pl pokerserver,poker`)
2. Manual: Connect to lobby → see player list → chat with multiple users
3. Manual: Disconnect/reconnect → player list updates correctly
4. Manual: `./stats` command still works
5. `mvn clean test` across all modules

---

## Phase 3: Remove UDP Dependencies and Dead Code

### Goal

After Phases 1 and 2, no code references the UDP module for point-to-point communication. Remove all dead code and UDP module dependencies.

### TDD Tests

**File**: `code/common/src/test/java/com/donohoedigital/build/NoUdpImportsTest.java`

| Test | Purpose |
|------|---------|
| `testNoUdpImportsInPokerModule` | Scan poker module sources for `import com.donohoedigital.udp` — must find none |
| `testNoUdpImportsInPokerNetwork` | Same for pokernetwork module |
| `testNoUdpImportsInPokerServer` | Same for pokerserver module |
| `testNoUdpImportsInGameEngine` | Same for gameengine module |

### Files to Delete

| File | Reason |
|------|--------|
| [PokerUDPServer.java](code/poker/src/main/java/com/donohoedigital/games/poker/PokerUDPServer.java) | All functionality replaced by TCP |
| [PokerUDPTransporter.java](code/pokernetwork/src/main/java/com/donohoedigital/games/poker/network/PokerUDPTransporter.java) | No longer needed |
| [PokerConnect.java](code/pokernetwork/src/main/java/com/donohoedigital/games/poker/network/PokerConnect.java) | Replaced by `Peer2PeerClient` |
| [ChatServer.java](code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/ChatServer.java) | Replaced by `TcpChatServer` |
| [PokerUDPDialog.java](code/poker/src/main/java/com/donohoedigital/games/poker/online/PokerUDPDialog.java) | UDP-specific dialog |
| [UDPStatus.java](code/gameengine/src/main/java/com/donohoedigital/games/engine/UDPStatus.java) | UDP status display |

### Files to Modify

| File | Change |
|------|--------|
| [PokerConnection.java](code/pokernetwork/src/main/java/com/donohoedigital/games/poker/network/PokerConnection.java) | Remove `UDPID udpConn`, `isUDP()`, `getUDPID()`. TCP-only. |
| [GameEngine.java](code/gameengine/src/main/java/com/donohoedigital/games/engine/GameEngine.java) | Remove `getUDPServer()` |
| `code/poker/pom.xml` | Remove `udp` module dependency |
| `code/pokernetwork/pom.xml` | Remove `udp` module dependency (if present) |
| `code/pokerserver/pom.xml` | Remove `udp` module dependency |
| `code/gameengine/pom.xml` | Remove `udp` module dependency |

### Keep

- **`code/udp/` module** — Retained in the repository but no longer depended upon by poker modules. Could be useful for reference or other projects.
- **[Peer2PeerMulticast.java](code/server/src/main/java/com/donohoedigital/p2p/Peer2PeerMulticast.java)** — Uses `java.net.MulticastSocket` directly (not the UDP module). No changes needed.
- **[LanManager.java](code/server/src/main/java/com/donohoedigital/p2p/LanManager.java)** — Uses `Peer2PeerMulticast`. No changes needed.

### Verification

1. Static analysis tests pass (no UDP imports in consumer modules)
2. `mvn clean compile` passes with UDP dependency removed from POMs
3. Full `mvn clean test` passes
4. Manual: All game scenarios work (online server-hosted, online P2P, LAN, lobby chat)

---

## Phase 4: Documentation Updates

All documentation referencing UDP ports, networking architecture, or Docker configuration must be updated.

### Files to Update

| File | Changes |
|------|---------|
| [docker/DEPLOYMENT.md](docker/DEPLOYMENT.md) | Update ports reference: 11886 and 11889 change from UDP to TCP. Update architecture diagram. Remove "Docker Desktop UDP limitation" caveat. Update docker-compose examples. |
| [docker/README.md](docker/README.md) | Update port list: remove `/udp` suffixes from 11886, 11889. |
| [docker/Dockerfile](docker/Dockerfile) | Change `EXPOSE 11886/udp 11889/udp` to `EXPOSE 11886 11889` (TCP is default). |
| [docker/docker-compose.yml](docker/docker-compose.yml) | Change `11886:11886/udp` and `11889:11889/udp` to `11886:11886` and `11889:11889`. |
| [LOCAL-DEVELOPMENT.md](LOCAL-DEVELOPMENT.md) | Remove "Do NOT use localhost for UDP chat" caveat (TCP works fine with localhost). Update port descriptions. Simplify networking instructions. |
| [BUILD.md](BUILD.md) | Update port references from UDP to TCP. |
| [README.md](README.md) | Update port list in Docker quick start section. |
| [unraid/README.md](unraid/README.md) | Update port table: 11886/11889 change from UDP to TCP. Remove "UDP port mapping can be unreliable" note. Mark all ports as Required (TCP is reliable). |
| [docs/PUBLIC-IP-DETECTION-TECHNICAL.md](docs/PUBLIC-IP-DETECTION-TECHNICAL.md) | Review P2P architecture description — update any UDP references to TCP. |
| [CHANGELOG.md](CHANGELOG.md) | Add entry for the TCP conversion describing the change and its benefits. |
| [.claude/plans/CHAT-TCP-CONVERSION.md](.claude/plans/CHAT-TCP-CONVERSION.md) | Mark as completed or update to reflect final implementation. |

### Configuration Files to Update

| File | Changes |
|------|---------|
| [pokerserver.properties](code/pokerserver/src/main/resources/pokerserver.properties) | Rename `settings.udp.port` → `settings.tcp.port`, `settings.udp.chat.port` → `settings.tcp.chat.port`. |
| Client properties referencing UDP ports | Update property names consistently. |
| Server cmdline properties | Update `settings.udp.port` references. |

### Verification

1. Search all `.md` files for "UDP" — only hits should be LAN discovery context or historical references
2. Search all `.properties` files for "udp" — only hits should be multicast config
3. Docker build and run works with new port mappings
4. All documentation accurately describes the TCP architecture

---

## Edge Cases Summary (All Phases)

### Connection Lifecycle

| Edge Case | Old UDP Handling | New TCP Handling |
|-----------|-----------------|------------------|
| Drop mid-game | UDPLink timeout (7.5s), resend failure after 25 attempts | IOException on read/write → immediate detection |
| Partial message | UDPData fragmentation + IncomingQueue reassembly | TCP handles at OS level; `Peer2PeerMessage.readBytes()` loops |
| Duplicate messages | AckList deduplication | TCP guarantees exactly-once in-order |
| Message reordering | IncomingQueue sorts by ID | TCP guarantees ordering |
| Half-open connection | Relies on timeout (7.5s) | TCP keepalive + alive thread heartbeat |
| Back-pressure | UDP drops packets silently | TCP write blocks; write timeout disconnects slow client |
| Message size limit | UDPData multi-part: ~44MB theoretical | Peer2PeerMessage: 500KB limit — sufficient for chat/game actions |

### Migration-Specific

| Edge Case | Strategy |
|-----------|----------|
| Saved games with `u-` (UDP) prefix | Treat as `n-` (TCP) prefix on load — backward compatible |
| Old clients connecting to new server | Not applicable — open source, all clients update together |
| Config files with `settings.udp.*` | Rename to `settings.tcp.*` with fallback check during migration |
| Properties file backward compat | Read `settings.tcp.*` first, fall back to `settings.udp.*` if not found |

### Chat-Specific

| Edge Case | Strategy |
|-----------|----------|
| Chat server restart mid-session | Client detects IOException, shows "disconnected" message, auto-reconnects |
| Broadcast to disconnected client | Write throws IOException → remove from connection list → notify others with LEAVE |
| `./stats` debug command | Reimplement in `TcpChatServer` to show TCP connection stats |
| `./dump` debug command | Reimplement — return all thread stack traces |

---

## Dependency Sequence

```
Phase 1 (Game P2P → TCP)     Phase 2 (Chat → TCP)         Phase 3 (Cleanup)          Phase 4 (Docs)
       |                            |                            |                         |
  No new modules              New TcpChatServer             Delete dead code          Update all .md files
  Modify consumers            New TcpChatClient             Remove UDP POM deps       Update Docker config
  to TCP-only path            Modify PokerServer            Simplify PokerConnection  Update properties
  Reuse existing TCP          Modify PokerMain              Static analysis tests     Update CHANGELOG
  infrastructure              Modify OnlineLobby
```

Each phase is independently deployable and testable. Phase 1 is highest value (game reliability) and lowest risk (TCP path already exists). Phase 2 requires the most new code (chat push model). Phase 3 is pure cleanup. Phase 4 is documentation only.

## Critical Files Reference

| File | Role |
|------|------|
| [OnlineManager.java](code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineManager.java) | Central hub with `isUDP()` branching — must consolidate to TCP-only |
| [PokerMain.java](code/poker/src/main/java/com/donohoedigital/games/poker/PokerMain.java) | Owns connection server lifecycle, UDP monitors, PokerTCPServer inner class |
| [PokerConnect.java](code/pokernetwork/src/main/java/com/donohoedigital/games/poker/network/PokerConnect.java) | Client connection logic — must rewrite from UDPLink to Peer2PeerClient |
| [ChatServer.java](code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/ChatServer.java) | Server-side chat — must reimplement on TCP |
| [Peer2PeerMessage.java](code/server/src/main/java/com/donohoedigital/p2p/Peer2PeerMessage.java) | Existing TCP wire protocol — reuse as foundation for all communication |
| [Peer2PeerServer.java](code/server/src/main/java/com/donohoedigital/p2p/Peer2PeerServer.java) | Existing TCP server infrastructure — extend for TcpChatServer |
| [PokerConnectionServer.java](code/pokernetwork/src/main/java/com/donohoedigital/games/poker/network/PokerConnectionServer.java) | Interface abstraction — keep as-is, implementations change |
