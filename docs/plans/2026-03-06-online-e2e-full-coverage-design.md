# Online E2E Full Coverage Design

**Status:** APPROVED (2026-03-06)

**Goal:** Enable full end-to-end testing of all online workflows — registration, game discovery, multi-player gameplay, full tournament completion, lobby management, observer mode, reconnection, and account management — by extending the desktop client with missing features, adding control server test endpoints, and building a comprehensive E2E test suite with synthetic player support.

---

## Architecture

```
+-------------------------------------------+
| pokergameserver (child process)            |
| Spring Boot, in-memory H2, port 19877     |
| DevController for test helpers            |
+-------------------------------------------+
         | REST + WebSocket
+-------------------------------------------+
| Desktop Client (child process)            |
| DDPokerCE JAR with dev control server     |
| "Player A" - the host                    |
+-------------------------------------------+
         | HTTP (control server API)
+-------------------------------------------+
| JUnit 5 E2E Tests                         |
|                                           |
| +---------------------------------------+ |
| | SyntheticPlayer                       | |
| | "Player B/C/..." - join/observe       | |
| | Uses RestGameClient +                | |
| | WebSocketGameClient directly          | |
| +---------------------------------------+ |
|                                           |
| ControlServerClient (Player A control)    |
| GameServerTestProcess (server lifecycle)  |
+-------------------------------------------+
```

### Key Design Decisions

- **One pokergameserver process** shared across all tests in a class (via `@BeforeAll`)
- **One desktop client process** shared similarly (existing `ControlServerTestBase` pattern)
- **Synthetic players** are lightweight — no Swing, no subprocess, just REST + WebSocket in-process using the same `RestGameClient` and `WebSocketGameClient` classes the real desktop client uses
- **No registration on desktop client** — users register via web client; desktop supports login only
- **No new server-side endpoints** — existing `AdminController` already has unlock (`POST /api/v1/admin/profiles/{id}/unlock`) and verify (`POST /api/v1/admin/profiles/{id}/verify`)

---

## Part A: Desktop Client Feature Additions (Production Code)

### A1. RestGameClient — Add Missing Methods

`RestGameClient.java` is missing two methods that the web client already has:

| Method | Server Endpoint | Purpose |
|--------|----------------|---------|
| `kickPlayer(gameId, profileId)` | `POST /api/v1/games/{id}/kick` | Kick player from lobby |
| `updateSettings(gameId, settings)` | `PUT /api/v1/games/{id}/settings` | Update game settings in lobby |

### A2. Lobby.java — Kick + Settings UI

The desktop `Lobby.java` currently:
- **Can receive** `LOBBY_PLAYER_KICKED` and `LOBBY_SETTINGS_CHANGED` WebSocket messages (display-side done)
- **Cannot initiate** kick or settings changes

Add:
- **Kick button** (host only) — per-player button in the player table, calls `restClient_.kickPlayer(gameId, profileId)`
- **Settings editor** (host only) — panel for game name, max players, blind structure; calls `restClient_.updateSettings(gameId, settings)` on change

### A3. Account Management Screen

New dialog or phase accessible from `OnlineMenu` for:
- **View profile** — display username, email, verification status via `RestAuthClient.getCurrentUser()`
- **Change password** — old password + new password fields via `RestAuthClient.changePassword()`
- **Change email** — new email field via `RestAuthClient.updateProfile()`

---

## Part B: Control Server Endpoints (Dev Test Infrastructure)

8 new endpoints. All follow the existing `BaseHandler` pattern and exercise the desktop client's actual `RestAuthClient`/`RestGameClient` code paths.

### Online Endpoints

| Endpoint | Method | Request | Response | Underlying Client Call |
|----------|--------|---------|----------|----------------------|
| `POST /online/register` | POST | `{serverUrl, username, password, email}` | `{success, emailVerified}` | `RestAuthClient.register()` |
| `GET /online/games` | GET | query params: `?status=...&search=...` | `{games: [...]}` | `RestGameClient.listGames()` |
| `POST /online/observe` | POST | `{gameId}` | `{gameId, wsUrl}` | `RestGameClient.observeGame()` + navigate to Lobby.Player |
| `POST /online/lobby/kick` | POST | `{profileId}` | `{kicked: true}` | `RestGameClient.kickPlayer()` |
| `PUT /online/lobby/settings` | PUT | GameSettingsRequest JSON | `{updated: true}` | `RestGameClient.updateSettings()` |

### Account Endpoints

| Endpoint | Method | Request | Response | Underlying Client Call |
|----------|--------|---------|----------|----------------------|
| `POST /account/change-password` | POST | `{oldPassword, newPassword}` | `{success: true}` | `RestAuthClient.changePassword()` |
| `POST /account/change-email` | POST | `{newEmail}` | `{success: true}` | `RestAuthClient.updateProfile()` |
| `GET /account/profile` | GET | — | `{username, email, emailVerified, ...}` | `RestAuthClient.getCurrentUser()` |

---

## Part C: E2E Test Suite

### SyntheticPlayer Test Helper

Lightweight in-process player using the same client classes as the desktop client, without Swing or the control server.

```
Location: code/poker/src/test/java/.../e2e/SyntheticPlayer.java
```

**API:**
- `create(serverUrl, username, password, email)` — register + verify via GameServerTestProcess
- `connect(gameId)` — REST join + WebSocket connect
- `observe(gameId)` — REST observe + WebSocket connect
- `disconnect()` — close WebSocket
- `sendAction(type, amount)` — player actions
- `sendChat(message)` — chat
- `sendRebuyDecision(accept)` / `sendAddonDecision(accept)`
- `isConnected()`, `lastMessageType()`, `messages()`, `lastGameState()`
- `isActionRequired()`, `currentOptions()`
- `waitForMessage(type, timeout)`, `waitForActionRequired(timeout)`
- `autoPlay(timeout)` — auto-respond with call/check strategy

**Key behaviors:**
- Thread-safe message collection (WebSocket callbacks on background threads)
- `autoPlay()` useful when synthetic player just needs to participate without scripting

### E2E Test Classes

All tagged `@Tag("e2e")` and `@Tag("slow")`. All extend `ControlServerTestBase`.

| Test Class | Scenario |
|------------|----------|
| `OnlineCrossPlayE2ETest` | **Existing** — validate login, host, lobby, start, play one hand, chip conservation |
| `OnlineRegistrationE2ETest` | Register via `POST /online/register`, verify email via admin API, login, confirm profile |
| `OnlineGameDiscoveryE2ETest` | Host creates game, `GET /online/games` returns it, search by name filters correctly |
| `OnlineMultiPlayerE2ETest` | Host + SyntheticPlayer joins, lobby shows both, play hand with both acting |
| `OnlineTournamentE2ETest` | Host + 2-3 synthetic players, small stacks (500 chips), high blinds (50/100), play to completion, verify standings and chip conservation |
| `OnlineReconnectionE2ETest` | Mid-game WebSocket disconnect on synthetic player, reconnect, verify state recovery and continued play |
| `OnlineObserverE2ETest` | SyntheticPlayer observes game, receives state updates, verify cannot submit actions |
| `OnlineLobbyManagementE2ETest` | Host kicks synthetic player (verify removed from lobby), updates settings (verify broadcast received) |
| `OnlineAccountManagementE2ETest` | Change password (re-login works with new password), change email, view profile |

### GameServerTestProcess Enhancements

Add helper methods that use the existing admin API:
- `lookupProfileId(username)` — `GET /api/v1/admin/profiles?name=...`
- `unlockAccount(username)` — lookup ID then `POST /api/v1/admin/profiles/{id}/unlock`

---

## File Inventory

### New Files (Production)

| File | Purpose |
|------|---------|
| Account management phase/dialog (1-2 files in `code/poker/src/main/java/.../online/`) | Change password, change email, view profile UI |

### New Files (Dev - Control Server Handlers)

| File | Purpose |
|------|---------|
| `OnlineRegisterHandler.java` | `POST /online/register` |
| `OnlineGamesHandler.java` | `GET /online/games` |
| `OnlineObserveHandler.java` | `POST /online/observe` |
| `OnlineLobbyKickHandler.java` | `POST /online/lobby/kick` |
| `OnlineLobbySettingsHandler.java` | `PUT /online/lobby/settings` |
| `AccountPasswordHandler.java` | `POST /account/change-password` |
| `AccountEmailHandler.java` | `POST /account/change-email` |
| `AccountProfileHandler.java` | `GET /account/profile` |

### New Files (Test)

| File | Purpose |
|------|---------|
| `SyntheticPlayer.java` | Lightweight in-process player |
| `OnlineRegistrationE2ETest.java` | Registration workflow |
| `OnlineGameDiscoveryE2ETest.java` | Game discovery workflow |
| `OnlineMultiPlayerE2ETest.java` | Host + synthetic player |
| `OnlineTournamentE2ETest.java` | Full tournament to completion |
| `OnlineReconnectionE2ETest.java` | Disconnect/reconnect mid-game |
| `OnlineObserverE2ETest.java` | Observer/spectate flow |
| `OnlineLobbyManagementE2ETest.java` | Kick + settings |
| `OnlineAccountManagementE2ETest.java` | Password/email/profile |

### Modified Files

| File | Change |
|------|--------|
| `RestGameClient.java` | Add `kickPlayer()`, `updateSettings()` methods |
| `Lobby.java` | Add kick button (host, per-player) and settings editor (host) |
| `OnlineMenu.java` | Add account management menu option |
| `GameControlServer.java` | Register 8 new handler endpoints |
| `ControlServerClient.java` | Add methods for all new endpoints |
| `GameServerTestProcess.java` | Add `unlockAccount()`, `lookupProfileId()` helpers |

---

## Implementation Order

| Phase | Work | Rationale |
|-------|------|-----------|
| 1 | Validate existing: build JARs, run `OnlineCrossPlayE2ETest`, fix issues | Prove foundation works before building on it |
| 2 | `RestGameClient` additions (`kickPlayer`, `updateSettings`) | Required by both Lobby UI and control server endpoints |
| 3 | `Lobby.java` kick + settings UI | Desktop client feature parity |
| 4 | Account management screen + `OnlineMenu` integration | Desktop client feature parity |
| 5 | `SyntheticPlayer` test helper | Required by most subsequent tests |
| 6 | Control server endpoints (all 8) + `ControlServerClient` methods | Test infrastructure |
| 7 | `GameServerTestProcess` admin helpers | Test infrastructure |
| 8 | E2E tests: existing fix, registration, game discovery, account management | Core user journey tests |
| 9 | E2E tests: multi-player, full tournament | Core gameplay tests |
| 10 | E2E tests: lobby management, observer, reconnection | Edge case tests |
