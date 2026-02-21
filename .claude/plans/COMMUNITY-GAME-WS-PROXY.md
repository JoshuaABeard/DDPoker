# Community Game WebSocket Proxy — Future Plan

**Status:** DRAFT
**Priority:** Low — Quality-of-life improvement, not blocking any milestone
**Depends On:** M5 ✅ (Web Client Game UI — complete)
**Created:** 2026-02-18

---

## Problem

When the DD Poker website is served over HTTPS, browsers block mixed-content `ws://` connections to community-hosted game servers (which typically lack TLS certificates). This means community-hosted games are only playable via the desktop client when the site uses HTTPS.

When the site is served over HTTP, community games are directly joinable via `ws://` — no issue.

## Current Behavior (M5)

- Community games appear in the `/games` lobby with a "Desktop client required" badge when the site is HTTPS
- The Join button is disabled for community games on HTTPS sites
- Community games are directly joinable when the site is HTTP
- Server-hosted games always use the site's own WebSocket endpoint (secure when HTTPS)

## Proposed Solution: Server-Side WebSocket Proxy

The DD Poker server acts as a relay between the web client (secure `wss://`) and the community host (insecure `ws://`).

### Flow

1. Web client requests to join a community game
2. Client connects to `wss://server/ws/proxy/{gameId}?host={communityHost}&port={communityPort}&token={wsToken}`
3. Server validates the ws-token (authenticated user)
4. Server verifies the community host is **registered** in the game discovery system (prevents open relay abuse)
5. Server opens `ws://communityHost:port/ws/games/{gameId}` with a server-to-server connection
6. Server relays messages bidirectionally between client and community host
7. On either side disconnecting, the other side is also disconnected

### Security Considerations

- **Not an open relay:** Only proxy to community hosts registered via the game discovery API
- **Authentication:** Client must present a valid ws-token; community host connection uses a server-generated token
- **Rate limiting:** Limit proxy connections per user (e.g., max 2 concurrent)
- **Timeouts:** Idle proxy connections closed after 5 minutes of no messages
- **Bandwidth:** Log proxy traffic volume; add per-connection byte limits if needed

### Implementation Sketch

**New server files:**
- `CommunityProxyWebSocketHandler.java` — Handles `wss://server/ws/proxy/{gameId}` connections
- `CommunityProxySession.java` — Manages the bidirectional relay (client WS ↔ community WS)

**Modified files:**
- `WebSocketConfig.java` — Register `/ws/proxy/{gameId}` endpoint
- `GameServerSecurityAutoConfiguration.java` — Permit `/ws/proxy/**` (auth handled in handler)

**Dependencies:**
- Spring `WebSocketClient` for outbound connections to community hosts

### Trade-offs

| Pro | Con |
|-----|-----|
| Community games playable from HTTPS sites | All traffic routes through the server (added latency) |
| Zero changes needed on community hosts | Server bandwidth/resource cost for relaying |
| Leverages existing game discovery for host validation | Additional server complexity |
| Transparent to the web client (same WS protocol) | Server becomes a dependency for community games |

### Effort Estimate

Small-Medium (2-3 days). The relay logic is straightforward with Spring's WebSocket client. Most of the work is in connection lifecycle management and error handling.

---

## Alternatives Considered

| Alternative | Why Not (for now) |
|-------------|-------------------|
| Community hosts add TLS | Requires domain name (not just IP), burdens community hosts, self-signed certs rejected by browsers |
| Separate HTTP subdomain for community games | Splits the app, complicates auth, confusing UX |
| Browser extension to allow mixed content | Non-standard, requires user action, not portable |
| Accept the limitation permanently | Viable for now, but limits community game accessibility as the project grows |
