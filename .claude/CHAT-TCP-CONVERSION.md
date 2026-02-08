# Converting Chat from UDP to TCP

## Current Issue

The DD Poker chat server currently uses UDP (port 11886) for real-time chat communication. While this works fine on Linux servers, **Docker Desktop for Windows has known limitations with UDP port mapping**, making it difficult to test chat functionality locally on Windows.

## Why UDP Was Originally Used

- Low latency for real-time chat messages
- Connectionless protocol - simpler state management
- NAT traversal capabilities for peer-to-peer connections

## Why TCP Would Be Better

1. **Docker Compatibility**: TCP port mapping works reliably across all Docker platforms
2. **Firewall Friendly**: TCP is more likely to work through corporate firewalls
3. **Reliable Delivery**: Chat messages benefit from guaranteed delivery
4. **Connection State**: Easier to detect disconnections and manage user presence
5. **Modern Standard**: Most modern chat systems use WebSockets (TCP-based)

## Conversion Effort Estimate

Converting the chat system from UDP to TCP would require changes to:

### Server Side (~8-12 hours)
- `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/ChatServer.java`
  - Replace `UDPLink`, `UDPServer`, `UDPData` with TCP Socket equivalents
  - Implement connection management (accept, maintain, close connections)
  - Change from datagram-based message handling to stream-based
  - Add message framing (since TCP is stream-oriented)

### Client Side (~6-8 hours)
- `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineLobby.java`
  - Replace UDP client connection with TCP Socket
  - Implement reconnection logic
  - Handle TCP stream reading with message framing

- `code/poker/src/main/java/com/donohoedigital/games/poker/PokerUDPServer.java`
  - Might need to be split or refactored to separate chat from UDP connection testing

### Protocol Changes (~2-4 hours)
- `code/pokernetwork/src/main/java/com/donohoedigital/games/poker/network/PokerUDPTransporter.java`
  - Adapt message serialization for TCP streams
  - Add message length prefixes or delimiters for framing

### Testing (~4-6 hours)
- Test all chat functionality (join, leave, message sending, user list)
- Test reconnection scenarios
- Test with multiple concurrent users
- Verify no message loss or ordering issues

**Total Estimate**: 20-30 hours

## Implementation Plan

### Phase 1: Create TCP Chat Server (Parallel to UDP)
1. Create new `ChatServerTCP` class alongside existing `ChatServer`
2. Use existing TCP infrastructure from `GameServer` as template
3. Implement message framing (length-prefix encoding)
4. Add configuration property to enable TCP chat mode

### Phase 2: Create TCP Chat Client
1. Create new TCP client connection class
2. Implement message framing on client side
3. Add reconnection logic with exponential backoff
4. Update `OnlineLobby` to support both UDP and TCP modes

### Phase 3: Testing & Migration
1. Run both UDP and TCP chat servers simultaneously
2. Test with mixed clients (some UDP, some TCP)
3. Once TCP is stable, deprecate UDP chat
4. Remove UDP chat code in future version

## Alternative: Current Workarounds

Until TCP conversion is complete, use these workarounds:

1. **Production Linux Deployment**: UDP works fine - no changes needed
2. **Windows Development**:
   - Use WSL2 backend for Docker Desktop (better UDP support)
   - Run server natively on Windows for testing
   - Deploy to a Linux VM for testing
   - Test chat on Linux CI/CD pipeline

## Configuration Changes Needed

### Server Properties
```properties
# New TCP chat configuration
settings.tcp.chat.enabled=true
settings.tcp.chat.port=11886

# Existing UDP chat (will be deprecated)
settings.udp.chat.port=11886
```

### Docker Compose
```yaml
ports:
  - "11886:11886"     # TCP chat (new)
  # - "11886:11886/udp"  # UDP chat (deprecated)
```

## Message Framing Protocol

For TCP streams, we need message boundaries. Recommended approach:

```
[4-byte length][message bytes]
```

Example implementation:
```java
// Write message
int length = message.length;
outputStream.writeInt(length);
outputStream.write(message);

// Read message
int length = inputStream.readInt();
byte[] message = new byte[length];
inputStream.readFully(message);
```

## Benefits After Conversion

- ✅ Works on all Docker platforms (Windows, Mac, Linux)
- ✅ More reliable message delivery
- ✅ Easier to implement message acknowledgments
- ✅ Better connection state management
- ✅ Simpler firewall/NAT configuration
- ✅ Future-proof for WebSocket upgrade path
