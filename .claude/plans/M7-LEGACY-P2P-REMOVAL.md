# M7: Legacy P2P/TCP Code Removal & Client Modernization

**Status:** APPROVED
**Created:** 2026-02-17
**Last Updated:** 2026-02-17
**Parent Plan:** `.claude/plans/SERVER-HOSTED-GAME-ENGINE.md`
**Depends On:** M1 ✅, M2 ✅, M3 ✅, M4 ✅, M6 ✅
**Effort:** L (3–4 weeks)

---

## Context

M1–M4 built a complete replacement stack: `EmbeddedGameServer` (Spring Boot lifecycle), `WebSocketGameClient` (transport), `WebSocketTournamentDirector` (game loop), `RemotePokerTable`/`RemoteHoldemHand` (view models), and `PokerDirector` (interface bridge). M6 added REST-based game discovery. The old P2P/TCP code is now entirely dead — no code path reaches it.

However, the legacy code still **exists** (~50+ files, ~12,000+ lines), creating confusion, bloating the codebase, and leaving dead references in 42+ files. Additionally, profile management (registration, password change/reset) still uses the legacy `DDMessage`-over-HTTP protocol through `PokerServlet` — this must be migrated to REST before `PokerServlet` can be removed. The community hosting configuration UI and server URL settings need modernization to work with the new REST/WebSocket architecture.

**Decisions (locked):**
- Delete the entire `server/p2p/` package (16 files). DD Poker is the only game in this codebase — no other consumer exists.
- Delete legacy `Lobby.java` without replacement. `FindGames` (M6) + WebSocket `LOBBY_*` messages provide the pre-game experience.
- Remove dead `TournamentDirector` references first, then refactor remaining live references to the `PokerDirector` interface.
- Include profile management REST migration in M7 scope — achieves complete legacy protocol removal.
- `PokerServlet` is deleted entirely after profile migration (no gutting/partial keep).
- `OnlineMessage.java` is deleted entirely — all message categories are replaced by WebSocket (`ServerMessageType`/`ClientMessageType`) or REST.
- **Keep `PublicIPDetector.java`** — pure utility (calls ipify.org, etc.), zero P2P dependencies. Needed for community hosting (host shares public IP with friends).
- **Refactor `GetPublicIP.java`** — keep the `PublicIPDetector` path, remove the legacy server-query fallback. Used in the new community hosting config UI.
- **Delete `OnlineConfiguration.java`** after building its replacement (`CommunityHostingConfig`, Phase 7.2.5). The old class is 100% P2P-wired but the UX it provides (~70% of it) must be preserved.
- **Lobby chat is a dedicated WebSocket feature** — players can connect to `/ws/lobby` to see who's online and chat without being in a game. This is a social feature, not game-specific.
- **Server URL remains user-configurable** — the Online Options tab in GamePrefsPanel keeps a single server URL field so players can point at community/self-hosted/WAN servers.

---

## What Already Exists (Do Not Re-Implement)

| Component | Module | Status | Notes |
|-----------|--------|--------|-------|
| `WebSocketGameClient` | poker | M4 ✅ | Replaces `OnlineManager` for all game communication |
| `WebSocketTournamentDirector` | poker | M4 ✅ | Replaces `TournamentDirector` in `gamedef.xml` |
| `RemotePokerTable` / `RemoteHoldemHand` | poker | M4 ✅ | WebSocket-backed view models |
| `PokerDirector` interface | poker | M4 ✅ | Bridge for `ShowTournamentTable` |
| `EmbeddedGameServer` | poker | M4 ✅ | Replaces `PokerConnectionServer` TCP listener |
| `FindGames` + `RestGameClient` | poker | M6 ✅ | Replaces `GetWanList` / `OnlineServer` |
| `CommunityGameRegistration` | poker | M6 ✅ | Replaces `SendWanGame` |
| `AuthController` (register/login/logout) | pokergameserver | M2 ✅ | Partial — missing change-password, forgot-password |
| `ProfileController` (get/update/delete) | pokergameserver | M2 ✅ | Partial — missing password ops, aliases, "me" |
| `PublicIPDetector` | poker | Existing ✅ | Pure HTTP utility for detecting public IP. No P2P deps. Keep. |

---

## Phase 7.1: Profile REST API Completion

**Goal:** Add the missing profile management REST endpoints to `pokergameserver` so the desktop client can drop the legacy WAN protocol entirely.

### 7.1.1 Gap Analysis

| Operation          | Legacy WAN                        | pokergameserver REST             | Gap                                          |
| ------------------ | --------------------------------- | -------------------------------- | -------------------------------------------- |
| Register           | `CAT_WAN_PROFILE_ADD`             | `POST /api/v1/auth/register` ✅   | None                                         |
| Login/validate     | `CAT_WAN_PROFILE_VALIDATE`        | `POST /api/v1/auth/login` ✅      | None                                         |
| Logout             | —                                 | `POST /api/v1/auth/logout` ✅     | None                                         |
| Current user       | —                                 | —                                | **Add `GET /api/v1/auth/me`**                |
| Change password    | `CAT_WAN_PROFILE_CHANGE_PASSWORD` | —                                | **Add `PUT /api/v1/profiles/{id}/password`** |
| Forgot password    | `CAT_WAN_PROFILE_SEND_PASSWORD`   | —                                | **Add `POST /api/v1/auth/forgot-password`**  |
| Reset password     | `CAT_WAN_PROFILE_COMPLETE_RESET`  | —                                | **Add `POST /api/v1/auth/reset-password`**   |
| Profile link       | `CAT_WAN_PROFILE_LINK`            | `POST /api/v1/auth/login` ✅      | None (login serves this purpose)             |
| Profile activation | `CAT_WAN_PROFILE_ACTIVATE`        | Immediate on register ✅          | None (simplified in community edition)       |
| Sync password      | `CAT_WAN_PROFILE_SYNC_PASSWORD`   | `POST /api/v1/auth/login` ✅      | None (login verifies credentials)            |
| Update email       | `CAT_WAN_PROFILE_RESET`           | `PUT /api/v1/profiles/{id}` ✅    | None                                         |
| Delete/retire      | —                                 | `DELETE /api/v1/profiles/{id}` ✅ | None                                         |
| Get aliases        | —                                 | —                                | Deferred — no consumer in M7 scope. Existing `api` module has `GET /api/profile/aliases`. Add to `pokergameserver` when needed. |

### 7.1.2 New Endpoints

**Add to `AuthController`:**

```
GET  /api/v1/auth/me                  → ProfileResponse (authenticated)
POST /api/v1/auth/forgot-password     → 200 OK (accepts ForgotPasswordRequest)
POST /api/v1/auth/reset-password      → 200 OK (accepts ResetPasswordRequest)
```

**Add to `ProfileController`:**

```
PUT  /api/v1/profiles/{id}/password   → 200 OK (accepts ChangePasswordRequest, must own profile)
```

### 7.1.3 New DTOs

**File:** `pokergameserver/.../dto/ProfileResponse.java`
```java
public record ProfileResponse(
    Long id,
    String username,
    String email,
    boolean isRetired
) {}
```

**File:** `pokergameserver/.../dto/ChangePasswordRequest.java`
```java
public record ChangePasswordRequest(
    @NotBlank String oldPassword,
    @NotBlank @Size(min = 8, max = 128) String newPassword
) {}
```

**File:** `pokergameserver/.../dto/ForgotPasswordRequest.java`
```java
public record ForgotPasswordRequest(
    @NotBlank String email
) {}
```

**File:** `pokergameserver/.../dto/ResetPasswordRequest.java`
```java
public record ResetPasswordRequest(
    @NotBlank String token,
    @NotBlank @Size(min = 8, max = 128) String newPassword
) {}
```

### 7.1.4 Service Methods

**Add to `AuthService`:**
- `ProfileResponse getCurrentUser(Long profileId)` — fetch profile, return DTO
- `void forgotPassword(String email)` — rate-check, generate reset token, store in `PasswordResetToken`, return token only in dev/embedded profile
- `void resetPassword(String token, String newPassword)` — validate token (not expired, not used), BCrypt-hash password, mark token used, update profile

**Add to `ProfileService`:**
- `void changePassword(Long profileId, String oldPassword, String newPassword)` — verify old password, BCrypt-hash new, update

**Password reset token storage:** Create a separate `PasswordResetToken` entity (not columns on `OnlineProfile`):
```java
@Entity
public class PasswordResetToken {
    Long id;
    String token;          // 32-byte SecureRandom, Base64URL encoded
    String email;
    Instant expiresAt;     // now + 1 hour
    Instant usedAt;        // null until redeemed
    Long profileId;        // FK to OnlineProfile
}
```
Token expires after 1 hour. Token is single-use: `usedAt` is set on first valid redemption; subsequent uses return 400.

**Token generation:** `SecureRandom` with 32 bytes encoded as Base64URL (no padding). Never use `UUID`, `Random`, or sequential IDs for security tokens.

**Token delivery:**
- `embedded` and `dev` Spring profiles: return token in `POST /forgot-password` response body as `{ "resetToken": "..." }` — the desktop UI can display it in a dialog
- All other profiles: return `503 Service Unavailable` with message `"Email delivery not configured. Contact your server administrator."` — this forces an explicit decision at deployment time

**Rate limiting on `POST /forgot-password`:** In-memory rate limit of 1 request per email address per hour using a Caffeine cache (same dependency already used in pokergameserver). Requests exceeding the limit return 200 (don't leak whether rate-limiting triggered).

**Timing attack mitigation on `POST /reset-password`:** All failure cases (token not found, token expired, token already used) return identical 400 responses with the same message `"Invalid or expired reset token"` and must take similar wall-clock time. Use constant-time token comparison.

**`PUT /profiles/{id}/password` authorization:** Server-side: JWT profile ID must match `{id}` in the path. Return 403 if they differ — do not expose whether the profile exists.

### 7.1.5 Lobby Chat WebSocket Endpoint

**Context:** `TcpChatServer` provides lobby chat (pre-game) over TCP using the `Peer2PeerMessage` binary protocol. Phase 7.4.1 deletes the entire `server/p2p/` package including `Peer2PeerMessage`, which `TcpChatServer` depends on. Lobby chat must be migrated to WebSocket before `TcpChatServer` can be deleted.

This is a **dedicated social feature** — players connect to lobby chat when the game opens, see who's online, and chat without needing to be in a game. This is distinct from in-game chat (already WebSocket via `InboundMessageRouter`). The lobby WebSocket endpoint has no `gameId` — it is global (connected players in the lobby, not in a game).

**New class:** `pokergameserver/.../LobbyWebSocketHandler.java`

- Spring WebSocket handler registered at `/ws/lobby`
- JWT auth on connect (query param `?token=JWT`, reuse existing `JwtService`)
- Ban check on connect (reuse `BannedKeyService`)
- On connect: send `LOBBY_PLAYER_LIST` with current connected players, broadcast `LOBBY_JOIN` to others
- On disconnect: broadcast `LOBBY_LEAVE`
- On `LOBBY_CHAT` message: validate length (≤500 chars), strip HTML, rate-limit, broadcast to all connected lobby clients
- Rate limiting: 30 messages per 60 seconds per player (Caffeine cache)
- Thread-safe connection tracking (`ConcurrentHashMap<Long, WebSocketSession>`)

**Register in `WebSocketConfig`:** Add `/ws/lobby` endpoint alongside existing `/ws/games/{gameId}`.

**Message types (JSON, same envelope as game WebSocket):**

```json
// Client → Server
{ "type": "LOBBY_CHAT", "message": "Hello everyone!" }

// Server → Client (broadcast on new connection)
{ "type": "LOBBY_PLAYER_LIST", "players": [{"playerId": 42, "playerName": "Alice"}] }

// Server → Client (broadcast on join/leave)
{ "type": "LOBBY_JOIN",  "playerId": 42, "playerName": "Alice" }
{ "type": "LOBBY_LEAVE", "playerId": 42, "playerName": "Alice" }

// Server → Client (broadcast on chat)
{ "type": "LOBBY_CHAT", "playerId": 42, "playerName": "Alice", "message": "Hello!", "timestamp": "2026-02-17T..." }
```

**Tests:** `LobbyWebSocketHandlerTest` (new):
- Connect with valid JWT → receives `LOBBY_PLAYER_LIST`
- Connect with invalid JWT → 401 close
- Connect with banned profile → 403 close
- Send chat → broadcast to all connected clients
- Send chat over 500 chars → rejected
- Rate limit exceeded → messages dropped (no error, just not broadcast)
- Disconnect → other clients receive `LOBBY_LEAVE`

### 7.1.6 Tests

**`AuthControllerProfileTest`** (new):
- `GET /me` authenticated → 200 with profile data
- `GET /me` unauthenticated → 401
- `POST /forgot-password` valid email (dev profile) → 200 with token in body
- `POST /forgot-password` unknown email → 200 (no token, don't leak existence)
- `POST /forgot-password` rate limit exceeded → 200 (same response, don't reveal throttle)
- `POST /forgot-password` (non-dev profile) → 503
- `POST /reset-password` valid token + password → 200, can login with new password
- `POST /reset-password` expired token → 400 with generic message
- `POST /reset-password` invalid token → 400 with same generic message
- `POST /reset-password` already-used token → 400 with same generic message

**`ProfileControllerPasswordTest`** (new):
- `PUT /profiles/{id}/password` correct old + valid new → 200
- `PUT /profiles/{id}/password` wrong old password → 403
- `PUT /profiles/{id}/password` not own profile (JWT mismatch) → 403
- `PUT /profiles/{id}/password` new password too short → 400

---

## Phase 7.2: Desktop Client Migration

**Goal:** Update the desktop client to use REST for all profile operations, replace P2P lobby chat with WebSocket, and build the community hosting configuration UI — removing all dependency on legacy WAN/P2P protocols.

### 7.2.1 New REST Auth Client

**File:** `poker/.../online/RestAuthClient.java` (new)

HTTP client for profile REST endpoints. Follows the same pattern as `RestGameClient` (M6) and `GameServerRestClient` (M4).

```java
public class RestAuthClient {
    // Uses JDK HttpClient (same as WebSocketGameClient)

    public LoginResponse login(String serverUrl, String username, String password);
    public LoginResponse register(String serverUrl, String username, String password, String email);
    public ProfileResponse getCurrentUser(String serverUrl, String jwt);
    public void changePassword(String serverUrl, String jwt, Long profileId,
                               String oldPassword, String newPassword);
    public void forgotPassword(String serverUrl, String email);
    public void resetPassword(String serverUrl, String token, String newPassword);
    public void logout(String serverUrl, String jwt);
}
```

**JWT handling:** The REST login/register responses include a JWT in the `Set-Cookie` header. `RestAuthClient` extracts and stores the JWT. For the embedded server, `EmbeddedGameServer.getLocalUserJwt()` already handles this — no change needed for practice mode. For remote server operations (WAN), `RestAuthClient` provides the JWT.

### 7.2.2 Update Desktop Profile Dialogs

**`PlayerProfileDialog.java`** — Major refactor:
- Replace `SendWanProfile` calls with `RestAuthClient` calls
- Registration: `restAuthClient.register(url, name, password, email)` — user chooses their own password (no emailed password)
- Login/validation: `restAuthClient.login(url, name, password)`
- Remove activation step (register is immediate)
- Remove "Link from another machine" option (login serves this purpose)
- Keep the same Swing UI layout, just change the data flow underneath

**`ChangePasswordDialog.java`** — Update:
- Replace `SendWanProfile` call with `restAuthClient.changePassword(url, jwt, profileId, oldPw, newPw)`

**`SyncPasswordDialog.java`** — Remove:
- Password sync is just login. Remove entirely, redirect to login flow.

**`FirstTimeWizard.java`** — Update:
- Replace profile creation flow to use `RestAuthClient.register()`
- Simplify: no email activation step

### 7.2.3 Desktop Lobby Chat Migration

Replace `TcpChatClient` with a WebSocket-based lobby chat client.

**New class:** `poker/.../online/LobbyChatWebSocketClient.java`

- JDK `java.net.http.HttpClient` WebSocket API (same pattern as `WebSocketGameClient`)
- Connects to `/ws/lobby?token=JWT` on the configured server URL
- Methods: `connect(serverUrl, jwt)`, `disconnect()`, `sendChat(message)`
- Dispatches received messages to `ChatHandler` (existing interface, already used for in-game chat)
- Auto-reconnect on unexpected disconnect

**Update lobby chat UI:**
- Wire `LobbyChatWebSocketClient` into a `ChatPanel` component (reuse the existing generic chat panel)
- Connect to lobby on application startup (after login), disconnect on application exit
- Lobby chat is independent of game state — players can chat while browsing games, during games, etc.

### 7.2.4 Community Hosting Configuration UI

**Goal:** Replace `OnlineConfiguration.java` (528 lines, 100% P2P-wired) with a new community hosting config that preserves the same UX patterns but uses `EmbeddedGameServer` + REST.

**File:** `poker/.../online/CommunityHostingConfig.java` (new)

The old `OnlineConfiguration` provided these UX features. The new class preserves them:

| Feature | Old (P2P) | New (WebSocket/REST) |
|---------|-----------|---------------------|
| Show LAN IP | `game_.getLocalIP()` | Same — JDK `InetAddress` utility |
| Detect Public IP | `GetPublicIP` → `PublicIPDetector` | Same — `PublicIPDetector` (kept, zero P2P deps) |
| Show connection URL | `poker://ip:port/gameId` | `ws://ip:port/ws/games/{gameId}` |
| Configure port | P2P server port field | **Embedded server port** — user specifies port for port forwarding |
| List on public server | `SendWanGame` (legacy DDMessage) | `CommunityGameRegistration` (REST, already M6) |
| Host as observer | Checkbox | Same — pass to game creation config |

**EmbeddedGameServer port configurability:**

Currently `EmbeddedGameServer` starts on port 0 (OS-assigned random). For community hosting, the host needs a **predictable port** for port forwarding. Changes needed:

- Add `start(int port)` overload to `EmbeddedGameServer` (or accept a port property override)
- Port 0 remains the default for practice mode (no external access needed)
- Community hosting config UI lets the user specify a port (default: `11885` — matches the old P2P port)
- Store the user's preferred community hosting port in preferences
- When binding for external access, bind to `0.0.0.0` instead of `localhost`

**Flow:**
1. User clicks "Host Community Game" → `CommunityHostingConfig` phase
2. Shows LAN IP, public IP (detect button), port field, connection URL
3. User clicks "Start Hosting":
   - `EmbeddedGameServer` restarts on the specified port bound to `0.0.0.0`
   - Registers game with WAN server via `CommunityGameRegistration` (REST)
   - Constructs WebSocket URL: `ws://{publicIP}:{port}/ws/games/{gameId}`
   - Displays URL for the user to share with friends
4. Host enters game via `FindGames` → joins own game

**Refactor `GetPublicIP.java`:**
- Remove the server-query fallback (legacy DDMessage path)
- Keep the `PublicIPDetector` primary path (calls external HTTP services)
- This phase class remains a `SendMessageDialog` subclass for the UI pattern (progress dialog while detecting IP)

### 7.2.5 GamePrefsPanel Online Options Modernization

**Goal:** Update the Online Options tab to work with the new architecture. Remove dead P2P settings, keep/modernize server URL configuration.

**`GamePrefsPanel.java`** — Surgical changes to `OnlineOptions` inner section:

| Setting | Action | Rationale |
|---------|--------|-----------|
| Online Enabled checkbox | **Keep** | Still controls whether online features are active |
| Online Server URL | **Rename + modernize** | Single "Server URL" field (was separate WAN/chat URLs). All REST, WebSocket, and lobby chat derive from this one URL. Default: community server. Format: `host:port` |
| Chat Server URL | **Remove** | Same server handles everything now |
| UDP hosting options | **Remove** | P2P-specific, dead |
| P2P pause/countdown options | **Remove** | P2P-specific, dead |
| Banned/muted player lists | **Keep** | Still useful for WebSocket games |
| Chat display options | **Keep** | Still useful |

**Server URL propagation:** The configured server URL must be readable by:
- `RestGameClient` / `FindGames` — for browsing remote server games
- `RestAuthClient` — for profile operations on remote server
- `LobbyChatWebSocketClient` — for lobby chat connection
- `CommunityGameRegistration` — for registering community-hosted games with WAN server

Currently `FindGames` hardcodes `localhost` + embedded port. Add a branch: if browsing remote games, read the server URL from preferences instead.

### 7.2.6 Files to Delete (Profile WAN Protocol)

| File | Lines | Reason |
|------|-------|--------|
| `poker/.../SendWanProfile.java` | ~200 | Replaced by `RestAuthClient` |
| `poker/.../online/ValidateProfile.java` | ~100 | Replaced by `RestAuthClient.login()` |
| `poker/.../SyncPasswordDialog.java` | ~100 | Login serves this purpose |
| `poker/.../online/OnlineConfiguration.java` | ~528 | Replaced by `CommunityHostingConfig` (7.2.4) |

### 7.2.7 Tests

**`RestAuthClientTest`** (new):
- Login success/failure
- Register success/duplicate username
- Change password success/wrong old password
- Forgot/reset password flow

**`CommunityHostingConfigTest`** (new):
- Public IP detection via `PublicIPDetector`
- Connection URL construction (WebSocket format)
- Port configuration persistence

**`LobbyChatWebSocketClientTest`** (new):
- Connect/disconnect lifecycle
- Send and receive chat messages
- Auto-reconnect on disconnect

---

## Phase 7.3: Remove P2P Game Hosting Code

**Goal:** Delete all P2P game hosting classes from the `poker` module. This is the largest deletion phase.

### 7.3.1 Files to Delete (poker/online/ package)

| File | Lines | Why Safe to Delete |
|------|-------|-------------------|
| `OnlineManager.java` | ~2,300 | Replaced by `WebSocketGameClient` (M4). 19 refs — all in files also being deleted or refactored. |
| `TournamentDirector.java` | ~1,969 | Replaced by `WebSocketTournamentDirector` (M4). `gamedef.xml` already points to WsTD. |
| `Lobby.java` | ~874 | Replaced by `FindGames` (M6) + WebSocket `LOBBY_*` messages. |
| `OnlineLobby.java` | ~462 | P2P lobby UI. Only called by `OnlineMenu` (also deleted). TCP chat integration. |
| `ListGames.java` | ~520 | Abstract base for P2P game join UI. Replaced by `FindGames` (M6). |
| `JoinGame.java` | ~477 | Replaced by `FindGames` (M6) join flow. |
| `OnlineManagerQueue.java` | ~485 | Only used by `OnlineManager`. |
| `OnlineServer.java` | ~272 | Replaced by `RestGameClient` (M6) and `CommunityGameRegistration` (M6). |
| `OnlineMenu.java` | ~95 | P2P menu phase. Only in `gamedef.xml`. Unreachable after M6 flow. |
| `HostStart.java` | ~181 | P2P host game start. Replaced by `PracticeGameLauncher` (M4). |
| `HostStatus.java` | ~308 | Host-only status display. Referenced by `HostStatusDash` (also deleted) and `ShowTournamentTable` (refactor in 7.5). |
| `HostStatusDash.java` | ~64 | Host-only dashboard item. Creates `HostStatus`. Delete alongside `HostStatus`. |
| `HostPauseDialog.java` | ~124 | Host-only pause dialog. Referenced by `HostDash` (also deleted). WsTD handles pause via `sendAdminPause()`. |
| `HostDash.java` | ~83 | Host-only dashboard. Calls `HostPauseDialog.autoClose()` statically. Delete alongside `HostPauseDialog`. |
| `HostConnectionListener.java` | ~41 | Interface only used by `OnlineManager`. |
| `OnlineMessageListener.java` | ~54 | Interface only used by `OnlineManager`. |
| `PokerP2PDialog.java` | ~255 | P2P connection dialog. Replaced by WebSocket connect. |
| `PokerP2PHeadless.java` | ~218 | Headless P2P sender. Replaced by WebSocket connect. |
| `GetWanList.java` | ~110 | Replaced by `FindGames` REST fetch (M6). Not deleted in M6 — confirmed still exists. |
| `SendWanGame.java` | ~103 | Replaced by `CommunityGameRegistration` (M6). Not deleted in M6 — confirmed still exists. |
| `TestPublicConnect.java` | ~192 | P2P connectivity test. Obsolete — WebSocket doesn't need P2P reachability tests. |
| `ChangeTableDialog.java` | ~148 | P2P multi-table dialog. Calls `TournamentDirector.changeTable()`. Server handles table changes. |
| `TournamentDirectorPauser.java` | ~80 | Pause listener for old TD. Only called from dead code paths in `PokerUtils`. |

**Keep (confirmed live — refactored, not deleted):**
- `GetPublicIP.java` — Refactored in 7.2.4 (remove server fallback, keep `PublicIPDetector` path)
- `PublicIPDetector.java` — Pure HTTP utility, zero P2P deps. Used by `GetPublicIP` and new `CommunityHostingConfig`.

**Before deleting each file:** Run `Grep` to verify no remaining callers outside the deletion set. If a file has callers in non-deleted code, it needs refactoring first (Phase 7.5).

### 7.3.2 Files to Delete (poker/online/ — Chat P2P)

| File | Lines | Notes |
|------|-------|-------|
| `ChatLobbyPanel.java` | ~163 | Legacy P2P lobby chat panel. Replaced by `LobbyChatWebSocketClient` + `ChatPanel`. |
| `ChatLobbyManager.java` | ~43 | Interface only. Referenced by `PokerMain.java` and `TcpChatClientTest` — remove usage from `PokerMain` as part of Phase 7.5.3 before deleting. |

**Keep:** `ChatPanel.java`, `ChatListPanel.java`, `ChatHandler.java`, `ChatManager.java` — these are the generic chat UI components still used by WebSocket chat.

### 7.3.3 Files to Delete (poker module — logic/)

| File | Lines | Why Safe to Delete |
|------|-------|-------------------|
| `BetValidator.java` | ~139 | Only caller is `SwingPlayerActionProvider` (also deleted). Server-side bet validation makes this dead. **Confirmed.** |
| `GameOverChecker.java` | ~166 | Only caller is `CheckEndHand` (old TD code path, dead). Server handles game-over. **Confirmed.** |
| `PokerLogicUtils.java` | ~108 | Only callers are `BetValidator` (deleted) and dead code in `Bet.java`/`PokerUtils.java`. **Confirmed.** |

Also delete their tests: `BetValidatorTest.java`, `GameOverCheckerTest.java`, `PokerLogicUtilsTest.java`.

**Keep (confirmed live):** `CheckEndHand.java`, `PreShowdown.java`, `NewLevelActions.java`, `ColorUpFinish.java` — these are **UI display phases** (animate hand resolution, show showdown, display level changes). They are actively referenced by `TournamentEngine.java` in `pokergamecore` and `ShowTournamentTable`. They render game state that arrives via WebSocket. **Update TD references to `PokerDirector` in Phase 7.5, but do not delete.**

### 7.3.4 `SwingPlayerActionProvider` Evaluation

`SwingPlayerActionProvider.java` was an M3 bridge between `pokergamecore` `PlayerActionProvider` and Swing UI. It references `TournamentDirector` directly. Since `WebSocketTournamentDirector` does NOT use it (WsTD gets actions via WebSocket, not via local Swing), and the embedded server uses `ServerPlayerActionProvider` (in pokergameserver), `SwingPlayerActionProvider` is dead code.

**Action:** Delete `SwingPlayerActionProvider.java` and its test.

---

## Phase 7.4: Remove Networking Infrastructure

**Goal:** Delete all P2P/TCP transport code from the `server` and `pokernetwork` modules.

### 7.4.1 Delete Entire `server/p2p/` Package (16 files)

**Path:** `code/server/src/main/java/com/donohoedigital/p2p/`

| File | Lines | Purpose |
|------|-------|---------|
| `Peer2PeerServer.java` | ~81 | TCP NIO server |
| `Peer2PeerClient.java` | ~238 | NIO TCP client |
| `Peer2PeerMessage.java` | ~380 | Binary wire protocol |
| `Peer2PeerMessenger.java` | ~225 | Async P2P messaging |
| `Peer2PeerServlet.java` | ~65 | Servlet (unused directly) |
| `Peer2PeerSocketThread.java` | ~170 | Socket connection handler |
| `Peer2PeerControllerInterface.java` | ~61 | Controller interface |
| `Peer2PeerMessageListener.java` | ~52 | Message listener interface |
| `LanManager.java` | ~374 | UDP multicast LAN discovery |
| `LanClientList.java` | ~288 | LAN client tracking |
| `LanClientInfo.java` | ~168 | LAN client data wrapper |
| `LanControllerInterface.java` | ~94 | LAN controller interface |
| `LanListener.java` | ~49 | LAN event listener interface |
| `LanEvent.java` | ~82 | LAN event object |
| `Peer2PeerMulticast.java` | ~241 | UDP multicast transport |
| `P2PURL.java` | ~123 | P2P URL scheme parser |

Also delete test files in `server/p2p/`: `P2PURLTest`, `Peer2PeerMessageTest`, `Peer2PeerMessageIOTest`, `Peer2PeerMessageSimpleTest` (~850 lines total).

**Before deletion:** Grep for imports of `com.donohoedigital.p2p` across the entire codebase. Expected consumers: `PokerMain`, `OnlineManager`, `TournamentDirector`, `Lobby`, `JoinGame` — all being deleted in Phase 7.3.

### 7.4.2 Delete `pokernetwork` P2P Classes

| File | Lines | Notes |
|------|-------|-------|
| `OnlineMessage.java` | ~657 | All 30+ message categories replaced by WebSocket + REST. |
| `TcpChatClient.java` | ~453 | TCP lobby chat client. Replaced by `LobbyChatWebSocketClient` (Phase 7.2.3). |
| `PokerConnection.java` | ~83 | TCP socket wrapper. Only used by `OnlineManager`. |
| `PokerConnectionServer.java` | ~74 | TCP server interface. Only implemented by `PokerMain.PokerTCPServer`. |
| `PokerURL.java` | ~74 | P2P URL parser (poker://host:port/...). Obsolete. |
| `OnlinePlayerInfo.java` | ~188 | Only referenced by `OnlineMessage` (deleted above) and `TcpChatServer` (deleted in 7.4.3). **Confirmed dead — delete.** |

**Also delete (pokernetwork tests):**
- `TcpChatClientTest.java` (~715 lines) — delete with `TcpChatClient`

### 7.4.3 Remove Legacy WAN Server Protocol

**`PokerServlet.java`** (`pokerserver` module) — Delete entirely:
- All game operations replaced by `GameController` REST endpoints (M6)
- All profile operations replaced by `AuthController`/`ProfileController` REST endpoints (Phase 7.1)
- After 7.1 and 7.2, no client sends `DDMessage` to `PokerServlet`

**`TcpChatServer.java`** (`pokerserver` module, ~590 lines) — Delete after Phase 7.1.5 and 7.2.3:
- TCP lobby chat server. Replaced by `LobbyWebSocketHandler` (Phase 7.1.5)
- Spring bean registered in `app-context-pokerserver.xml` — remove bean definitions (lines 59, 62)
- `PokerServer.java` injects and starts `TcpChatServer` — remove injection, `init()` call, and `start()` call
- Also delete `TcpChatServerTest.java` (~300 lines)
- Uses `Peer2PeerMessage` from `server/p2p/` (deleted in Phase 7.4.1) — cannot survive without this

**Transport chain — resolved decisions:**

| File | Module | Action | Reason |
|------|--------|--------|--------|
| `PokerServlet.java` | pokerserver | **Delete** | All operations replaced by REST endpoints in 7.1 |
| `EngineServlet.java` | gameserver | **Keep** | Extended by `PokerServlet`; also used by `ServerSideGame`, `EngineMailErrorHandler`. Framework code. |
| `GameMessenger.java` | gameengine | **Keep** | Framework code — used by remaining `SendMessageDialog` subclasses (`UserRegistrationSend`, `MessageErrorDialog`, `DDMessageCheck`). Not M7 scope. |
| `EngineMessenger.java` | gamecommon | **Keep** | Base class for `GameMessenger`. |
| `SendMessageDialog.java` | gameengine | **Keep** | 23 live references across codebase; extensive `gamedef.xml` usage. 4 non-poker subclasses remain. |
| `DDMessenger.java` | common | **Keep** | Generic HTTP client utility. |
| `GetOnlineGameStatus.java` | gameengine | **Keep** | Framework code (84 lines). Generic game status check. Not M7 scope. |

---

## Phase 7.5: Refactor Core Game Classes

**Goal:** Remove dead `TournamentDirector` references and update remaining live references to use the `PokerDirector` interface. Modernize GamePrefsPanel.

### 7.5.1 Strategy

1. **Delete `TournamentDirector.java`** and let the compiler identify all broken references
2. **Categorize each broken reference:**
   - **Dead code** (in host-only paths, P2P-only methods, methods only called from deleted classes) → Delete the dead code
   - **Live reference** (used in active code paths) → Refactor to `PokerDirector` interface
3. **Test after each batch** of changes

### 7.5.2 Known References (42 sites from M4 review)

**Files being deleted entirely (Phase 7.3) — no action needed:**
- `OnlineManager.java`, `Lobby.java`, `HostStart.java`, `HostPauseDialog.java`, `ChangeTableDialog.java`, `OnlineServer.java`, `SwingPlayerActionProvider.java`, `OnlineLobby.java`, `OnlineMenu.java`, `ListGames.java`, `TournamentDirectorPauser.java`

**Files already refactored in M4 — verify no regressions:**
- `ShowTournamentTable.java` — `td_` field already `PokerDirector` (M4). Also remove `HostStatus` reference (confirmed live — `HostStatus` deleted in 7.3).
- `WebSocketTournamentDirector.java` — implements `PokerDirector`
- `RemoteHoldemHand.java`, `RemotePokerTable.java` — no TD reference

**Files needing refactoring:**

| File | Expected Change |
|------|----------------|
| `PokerMain.java` | Remove `PokerTCPServer` inner class, remove `Peer2PeerControllerInterface`/`LanControllerInterface` implementations, remove P2P server startup code, remove `ChatLobbyManager` import/usage. Keep embedded server startup. |
| `PokerGame.java` | Update `initOnlineManager(TournamentDirector td)` parameter type to `PokerDirector`. Remove `TournamentDirector.DEBUG_CLEANUP_TABLE` static reference — move to `PokerConstants` or `PokerDirector`. Remove host-only setup methods if dead. |
| `HoldemHand.java` | Comments only reference TD (lines 268, 1048, 1184). Update comments. No executable code changes. |
| `PokerTable.java` | Replace `TournamentDirector.getStateForSave(state, this)` static call (line 1988) — move method to utility class or `PokerDirector`. Update comments. |
| `PokerGameboard.java` | Replace `TournamentDirector.DEBUG_EVENT_DISPLAY` (lines 291, 385) — move debug flag to `PokerConstants`. |
| `Bet.java` | Replace `TournamentDirector.AI_PAUSE_TENTHS` constant (line 195) — move to `PokerConstants`. Replace `(TournamentDirector) context_.getGameManager()` cast (line 308) — cast to `PokerDirector`. |
| `PokerUtils.java` | Replace `(TournamentDirector)` cast (line 432) — cast to `PokerDirector`. Evaluate `TournamentDirectorPauser` reference (lines 676-677) — pauser is deleted, remove or replace with `PokerDirector` equivalent. |
| `DealButton.java` | Display code — update TD reference. |
| `PokerSaveGame.java` | Replace `TournamentDirector td_` field (line 44) and cast (line 47) — change to `PokerDirector`. |
| `ExitPoker.java` | Replace `(TournamentDirector)` cast (line 66) — cast to `PokerDirector`. Remove `OnlineServer.getWanManager()` call and any WAN cleanup. |
| `OtherTables.java` | Remove WAN game update calls. |
| `ShowTournamentTable.java` | Remove `HostStatus` reference (deleted in 7.3). Already uses `PokerDirector` for `td_` field. |

**Confirmed live — update TD refs, do NOT delete:**
- `CheckEndHand.java` — UI display phase. Update any TD refs to `PokerDirector`.
- `PreShowdown.java` — UI display phase. Update any TD refs to `PokerDirector`.
- `NewLevelActions.java` — UI display phase. Update any TD refs to `PokerDirector`.
- `ColorUpFinish.java` — UI display phase. Update any TD refs to `PokerDirector`.

**Confirmed no TD reference — no action needed:**
- `RuleEngine.java` — AI logic. No `TournamentDirector` dependency. Confirmed live, keep.
- `PracticeGameLauncher.java` — M4 class. No TD reference.

**Evaluate during refactoring:**
- `Rank.java`, `OnlineDash.java`, `DashboardItem.java` — Dashboard references. Update or evaluate if dead.
- `PokerGameStateDelegate.java` — Evaluate if dead after server-hosted save/load.
- `DisplayTableMoves.java` — Table move display. Server handles moves.
- `ChatPanel.java` — Verify chat works through WebSocket only.

### 7.5.3 TournamentDirector Static Constants Migration

Several files reference static constants/debug flags on `TournamentDirector`. These must be moved before `TournamentDirector.java` is deleted:

| Constant | Used In | Move To |
|----------|---------|---------|
| `TournamentDirector.DEBUG_EVENT_DISPLAY` | `PokerGameboard` (×2), `ShowTournamentTable` | `PokerConstants` |
| `TournamentDirector.DEBUG_CLEANUP_TABLE` | `PokerGame` | `PokerConstants` |
| `TournamentDirector.AI_PAUSE_TENTHS` | `Bet` | `PokerConstants` |
| `TournamentDirector.getStateForSave()` | `PokerTable` | Move to `PokerTable` as private static or to a utility class |

### 7.5.4 PokerMain Cleanup

`PokerMain.java` is the most critical file. Changes:

1. **Remove inner class `PokerTCPServer`** (~90 lines)
   - `extends Peer2PeerServer implements PokerConnectionServer`
   - Handles incoming P2P TCP connections
   - Replaced by `EmbeddedGameServer`

2. **Remove interface implementations:**
   - `Peer2PeerControllerInterface` — P2P message routing
   - `LanControllerInterface` — LAN discovery

3. **Remove methods:**
   - `getPokerConnectionServer()` — returns the P2P TCP server
   - `connectionClosing()` — P2P disconnect handling
   - `messageReceived()` (P2P) — routes to `OnlineManager`
   - Any LAN-related methods: `getPlayerId()`, `getGUID()`, `handleDuplicateKey()`, etc.

4. **Remove ChatLobbyManager/ChatLobbyPanel initialization** — replaced by `LobbyChatWebSocketClient`

5. **Keep:**
   - `EmbeddedGameServer` startup/shutdown
   - Swing UI initialization
   - All non-P2P functionality

### 7.5.5 PokerGame Cleanup

`PokerGame.java` likely has methods for initializing local tournaments that are now dead (server creates games via REST). Evaluate and remove:
- `initOnlineManager(TournamentDirector td)` — update parameter type to `PokerDirector`, or remove entirely if dead
- `initTournament()` — if only called from old `TournamentDirector`
- `setupComputerPlayers()` — if only called from old flow
- Host/client mode flags — if they exist in `PokerGame`

**Keep:** `getWebSocketConfig()`, `setWebSocketConfig()`, `getPlayerProfile()`, and all state that the Swing UI reads.

---

## Phase 7.6: gamedef.xml, Config & Phase Chain Cleanup

**Goal:** Clean up configuration files to remove references to deleted classes and dead P2P settings.

### 7.6.1 gamedef.xml Phase Definitions

**File:** `code/poker/src/main/resources/config/poker/gamedef.xml`

- Verify lines 421, 424 already point to `WebSocketTournamentDirector` (confirmed in M4)
- Remove or update lines 840, 844, 848 that reference `TournamentDirector`/`ClientTournamentDirector` as `next-phase` string values
- Remove phase definitions for all deleted classes:
  - `Lobby`, `JoinGame`, `HostStart`, `HostPauseDialog`
  - `OnlineMenu`, `OnlineLobby`, `ListGames`
  - `GetWanList`, `SendWanGame`, `ValidateProfile`, `TestPublicConnect`
  - `OnlineConfiguration`, `ChangeTableDialog`
  - `SyncPasswordDialog` (if it's a phase)
  - `HostDash`, `HostStatusDash` (if defined as phases)

**Important:** Some phase names may be referenced by string in Java code (e.g., `context_.processPhase("Lobby")`). Grep for all deleted phase names as strings before removing from gamedef.xml.

### 7.6.2 client.properties P2P Settings Cleanup

**File:** `code/poker/src/main/resources/config/poker/client.properties`

**Remove dead P2P settings:**
```properties
# These are all dead after P2P removal:
settings.p2p.server.port=               11885
settings.server.threads=                3
settings.server.readtimeout.millis=     10000
settings.multicast.port=                7755
settings.multicast.address=             239.252.101.202
settings.udp.port=                      11889
settings.poker.connect.timeout.millis=  10000
```

**Keep (still used):**
```properties
# PublicIPDetector uses these:
settings.publicip.service.url=          https://api.ipify.org
settings.publicip.cache.ttl=            300000
```

### 7.6.3 Spring XML Bean Cleanup

**File:** `code/pokerserver/src/main/resources/app-context-pokerserver.xml`

- Remove bean definitions for `TcpChatServer` (lines 59, 62)
- Remove any bean definitions for deleted classes

---

## Phase 7.7: Testing & Verification

### Unit Tests

All in `pokergameserver` module:

**`AuthControllerProfileTest`** — Phase 7.1 endpoints (me, change password, forgot/reset password)

**`ProfileControllerPasswordTest`** — Phase 7.1 password change

**`LobbyWebSocketHandlerTest`** — Phase 7.1.5 lobby chat

**`RestAuthClientTest`** (poker module) — Phase 7.2 REST auth client

**`CommunityHostingConfigTest`** (poker module) — Phase 7.2.4 hosting config UI

**`LobbyChatWebSocketClientTest`** (poker module) — Phase 7.2.3 lobby chat client

### Integration Tests

**Profile REST Integration:**
- Register → login → change password → login with new password
- Register → forgot password → reset with token → login with new password
- Register → get current user → verify profile data

**Lobby Chat Integration:**
- Connect lobby WebSocket → see player list → send chat → receive broadcast
- Multiple clients connected → join/leave notifications

**Community Hosting Integration:**
- Detect public IP → configure port → start embedded server on port → register with WAN → verify game appears in discovery

### Regression Testing

After each phase:
1. Run `mvn test -P dev` (all unit tests)
2. Run `mvn test` (full test suite including integration)
3. Run `mvn test -pl poker` (poker module specifically)
4. Run `mvn test -pl pokergameserver` (gameserver module specifically)

**Critical verification points:**
- Practice game mode: create → play → complete (embedded server + AI)
- Game discovery: list games via REST
- WebSocket connection and game state updates
- Lobby chat via WebSocket (connect, chat, see online players)
- In-game chat via WebSocket
- Profile registration and login via REST
- Community hosting: create game → friends join via WebSocket URL

---

## Ordering & Dependencies

```
Phase 7.1: Profile REST API Completion
  └── Add missing auth/profile endpoints to pokergameserver
  └── Add LobbyWebSocketHandler (/ws/lobby) to pokergameserver   [7.1.5]
       ↓
Phase 7.2: Desktop Client Migration
  └── Update PlayerProfileDialog, ChangePasswordDialog to use REST
  └── Migrate lobby chat to LobbyChatWebSocketClient             [7.2.3]
  └── Build CommunityHostingConfig (replace OnlineConfiguration) [7.2.4]
  └── Modernize GamePrefsPanel Online Options                    [7.2.5]
  └── Delete SendWanProfile, ValidateProfile, SyncPasswordDialog, OnlineConfiguration
       ↓
Phase 7.3: Remove P2P Game Hosting Code          ← Can start after 7.2
  └── Delete ~28 files from poker/online/, dashboard/, and logic/
       ↓
Phase 7.4: Remove Networking Infrastructure       ← Can start after 7.3; 7.4.3 requires 7.1.5 + 7.2.3
  └── Delete server/p2p/ (16 files + tests)
  └── Delete pokernetwork P2P classes (+ TcpChatClient, OnlinePlayerInfo)
  └── Delete PokerServlet + TcpChatServer
       ↓
Phase 7.5: Refactor Core Game Classes            ← Must follow 7.3 (fully complete)
  └── Migrate TD static constants to PokerConstants
  └── Remove dead TD references
  └── Refactor live refs to PokerDirector
  └── Clean up PokerMain, PokerGame
       ↓
Phase 7.6: Config Cleanup                        ← Must follow 7.3 + 7.5
  └── Remove deleted phase definitions from gamedef.xml
  └── Remove dead P2P settings from client.properties
  └── Remove bean definitions from app-context-pokerserver.xml
       ↓
Phase 7.7: Testing & Verification
  └── Full regression testing
```

**Phases 7.3 and 7.4 can be done together** since the files don't overlap. However, **Phase 7.5 must not start until Phase 7.3 is fully complete** — the compiler-driven approach (delete TD, fix compile errors) requires a clean compile baseline. Starting 7.5 while 7.3 is still partially done produces a confusing mix of deletion-caused and refactoring-caused compile errors.

---

## Files Summary

### New Files

| File | Phase | Purpose |
|------|-------|---------|
| `pokergameserver/.../dto/ChangePasswordRequest.java` | 7.1 | Change password DTO |
| `pokergameserver/.../dto/ForgotPasswordRequest.java` | 7.1 | Forgot password DTO |
| `pokergameserver/.../dto/ResetPasswordRequest.java` | 7.1 | Reset password DTO |
| `pokergameserver/.../dto/ProfileResponse.java` | 7.1 | Current user response DTO |
| `pokergameserver/.../entity/PasswordResetToken.java` | 7.1 | Separate entity for reset tokens (not columns on OnlineProfile) |
| `pokergameserver/.../LobbyWebSocketHandler.java` | 7.1 | WebSocket lobby chat handler — replaces TcpChatServer |
| `poker/.../online/RestAuthClient.java` | 7.2 | Desktop REST auth client |
| `poker/.../online/LobbyChatWebSocketClient.java` | 7.2 | Desktop WebSocket lobby chat client — replaces TcpChatClient |
| `poker/.../online/CommunityHostingConfig.java` | 7.2 | Community hosting configuration UI — replaces OnlineConfiguration |
| Tests (8-9 classes) | 7.1, 7.2 | See Testing section |

### Modified Files

| File | Phase | Change |
|------|-------|--------|
| `AuthController.java` (pokergameserver) | 7.1 | +3 endpoints (me, forgot-password, reset-password) |
| `ProfileController.java` (pokergameserver) | 7.1 | +1 endpoint (change password) |
| `AuthService.java` | 7.1 | +3 methods |
| `ProfileService.java` | 7.1 | +1 method (changePassword) |
| `WebSocketConfig.java` (pokergameserver) | 7.1 | Register `/ws/lobby` endpoint |
| `PlayerProfileDialog.java` | 7.2 | Replace WAN calls with REST |
| `ChangePasswordDialog.java` | 7.2 | Replace WAN calls with REST |
| `FirstTimeWizard.java` | 7.2 | Replace WAN calls with REST |
| `GetPublicIP.java` | 7.2 | Remove server-query fallback, keep PublicIPDetector path |
| `EmbeddedGameServer.java` | 7.2 | Add configurable port support for community hosting |
| `GamePrefsPanel.java` | 7.2 | Modernize Online Options (single server URL, remove P2P settings) |
| `FindGames.java` | 7.2 | Read server URL from preferences for remote server browsing |
| `PokerMain.java` | 7.5 | Remove PokerTCPServer, P2P/LAN interfaces, ChatLobbyManager |
| `PokerGame.java` | 7.5 | Remove dead TD methods, update types to PokerDirector |
| `HoldemHand.java` | 7.5 | Update TD comments |
| `PokerTable.java` | 7.5 | Replace `TournamentDirector.getStateForSave()` static call |
| `PokerGameboard.java` | 7.5 | Replace TD debug flags with PokerConstants |
| `Bet.java` | 7.5 | Replace TD constant + cast to PokerDirector |
| `PokerUtils.java` | 7.5 | Replace TD cast, remove TournamentDirectorPauser ref |
| `PokerSaveGame.java` | 7.5 | Replace TD field/cast with PokerDirector |
| `ExitPoker.java` | 7.5 | Replace TD cast, remove WAN cleanup |
| `OtherTables.java` | 7.5 | Remove WAN update calls |
| `ShowTournamentTable.java` | 7.5 | Remove HostStatus reference |
| `CheckEndHand.java`, `PreShowdown.java`, `NewLevelActions.java`, `ColorUpFinish.java` | 7.5 | Update any TD refs to PokerDirector |
| `gamedef.xml` | 7.6 | Remove deleted phase definitions |
| `client.properties` | 7.6 | Remove dead P2P settings |
| `app-context-pokerserver.xml` | 7.6 | Remove TcpChatServer bean definitions |

### Deleted Files (~50 files, ~12,000+ lines)

**Phase 7.2 — Profile WAN & Old Hosting Config (4 files):**

| File | Lines |
|------|-------|
| `poker/.../SendWanProfile.java` | ~200 |
| `poker/.../online/ValidateProfile.java` | ~100 |
| `poker/.../SyncPasswordDialog.java` | ~100 |
| `poker/.../online/OnlineConfiguration.java` | ~528 |

**Phase 7.3 — P2P Game Hosting (~28 files + tests):**

| File | Lines |
|------|-------|
| `OnlineManager.java` | ~2,300 |
| `TournamentDirector.java` | ~1,969 |
| `Lobby.java` | ~874 |
| `OnlineLobby.java` | ~462 |
| `ListGames.java` | ~520 |
| `OnlineManagerQueue.java` | ~485 |
| `JoinGame.java` | ~477 |
| `OnlineServer.java` | ~272 |
| `SwingPlayerActionProvider.java` | ~260 |
| `PokerP2PDialog.java` | ~255 |
| `PokerP2PHeadless.java` | ~218 |
| `HostStart.java` | ~181 |
| `TestPublicConnect.java` | ~192 |
| `HostStatus.java` | ~308 |
| `ChangeTableDialog.java` | ~148 |
| `BetValidator.java` | ~139 |
| `GameOverChecker.java` | ~166 |
| `PokerLogicUtils.java` | ~108 |
| `ChatLobbyPanel.java` | ~163 |
| `HostPauseDialog.java` | ~124 |
| `OnlineMenu.java` | ~95 |
| `TournamentDirectorPauser.java` | ~80 |
| `HostDash.java` | ~83 |
| `HostStatusDash.java` | ~64 |
| `OnlineMessageListener.java` | ~54 |
| `ChatLobbyManager.java` | ~43 |
| `HostConnectionListener.java` | ~41 |
| `GetWanList.java` | ~110 |
| `SendWanGame.java` | ~103 |
| + Tests: `SwingPlayerActionProviderTest`, `BetValidatorTest`, `GameOverCheckerTest`, `PokerLogicUtilsTest` |

**Phase 7.4 — Networking Infrastructure (~24 files + tests):**

| File | Lines |
|------|-------|
| `server/p2p/` (16 files) | ~2,418 |
| `server/p2p/` tests (4 files) | ~850 |
| `pokernetwork/OnlineMessage.java` | ~657 |
| `pokernetwork/TcpChatClient.java` | ~453 |
| `pokernetwork/OnlinePlayerInfo.java` | ~188 |
| `pokernetwork/PokerConnection.java` | ~83 |
| `pokernetwork/PokerConnectionServer.java` | ~74 |
| `pokernetwork/PokerURL.java` | ~74 |
| `pokernetwork/TcpChatClientTest.java` | ~715 |
| `pokerserver/PokerServlet.java` | TBD |
| `pokerserver/TcpChatServer.java` | ~590 |
| `pokerserver/TcpChatServerTest.java` | ~300 |

---

## Risk Register

| # | Risk | Impact | Likelihood | Mitigation |
|---|------|--------|------------|------------|
| R1 | Dead code analysis misses a live reference | High | Medium | Compiler-driven approach: delete file, fix compile errors. Full test suite after each phase. |
| R2 | Profile REST migration breaks existing profile data | Medium | Low | No schema changes to OnlineProfile beyond adding 2 new columns. Existing profiles remain valid. |
| R3 | Core game classes (HoldemHand, PokerTable) break after TD removal | High | Medium | These are the most-referenced files. Careful, file-by-file refactoring with test runs between batches. |
| R4 | gamedef.xml phase chain breaks | High | Low | Grep for all deleted phase names as string references before removing. Run full application startup test. |
| R5 | Lobby chat breaks after removing TcpChatServer/TcpChatClient | Medium | Medium | `LobbyWebSocketHandler` must be fully tested (7.1.5 tests) and `LobbyChatWebSocketClient` must be wired into the desktop UI (7.2.3) before Phase 7.4 deletes the TCP infrastructure. In-game chat (WebSocket, via WsTD) is unaffected. |
| R6 | `PokerMain` becomes unstable after removing P2P code | High | Low | PokerMain is well-structured with clear separation. EmbeddedGameServer startup is independent of P2P code. |
| R7 | EmbeddedGameServer port change breaks practice mode | Medium | Low | Port 0 (random) remains default. Configurable port only used when community hosting is explicitly enabled. Test both modes. |
| R8 | Community hosting config UX regression vs OnlineConfiguration | Medium | Medium | Map every feature of old OnlineConfiguration to new CommunityHostingConfig. Manual UX comparison during 7.7 testing. |

---

## Effort Estimate

| Phase | Effort |
|-------|--------|
| 7.1 Profile REST API + Lobby Chat WS | S (2–3 days) |
| 7.2 Desktop Client Migration (profiles, lobby chat, hosting config, prefs) | L (5–7 days) |
| 7.3 Remove P2P Game Code | M (3–4 days) |
| 7.4 Remove Networking Infra | S (1–2 days) |
| 7.5 Refactor Core Classes | L (4–5 days) |
| 7.6 Config Cleanup | XS (< 1 day) |
| 7.7 Testing & Verification | M (2–3 days) |
| **Total** | **L (~3–4 weeks)** |
