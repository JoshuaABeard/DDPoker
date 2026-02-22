# Plan: Restore Online Game Hosting Flow

**Status:** proposed
**Created:** 2026-02-22

---

## Overview

Restore the "Host Online" UI flow that was deleted in M7 (commit `9ef67fe3`). The original flow had four screens: OnlineMenu landing page → TournamentOptions (select/configure tournament) → OnlineConfiguration (setup screen 2 of 2) → Lobby (pre-game waiting room). All source code is recoverable from git history.

The M7 deletion removed P2P/TCP networking and replaced it with WebSocket + REST. The restored classes must be adapted to use the new architecture — they cannot be dropped in verbatim.

---

## User-Confirmed Decisions

| Topic | Decision |
|-------|----------|
| Online Menu screen | Restore as intermediate landing page |
| AI fill in lobby | No live toggle in lobby — AI fills empty seats on game start if `isFillComputer()` is set in tournament profile (server-side) |
| Test Connection (P2P) | **Remove** — no equivalent in new architecture |
| Test WAN server | Add "Test Connection" button in **Options** (GamePrefsPanel), not in OnlineConfiguration |
| Lobby | Restore: players list, observers list, chat, game URLs, Start! button |
| Resume Online Game | Defer — see section below |

---

## Flow Diagram

```
StartMenu
  └─ "Online" button
       └─ OnlineMenu (NEW)
            ├─ Host Online → TournamentOptions (existing, "1 of 2")
            │                    └─ OnlineConfiguration (NEW, "2 of 2")
            │                             └─ Lobby.Host (NEW)
            │                                   └─ HostStart (NEW)
            │                                         └─ InitializeOnlineGame
            │                                               └─ BeginOnlineGame → game
            │
            ├─ Join Online → FindGames (existing)
            ├─ Load → LoadOnlineGameMenu (existing)
            ├─ Website / Forums (existing)
            └─ Cancel → PreviousPhase
```

---

## Files to Create

### `poker` module — `online/` package

| File | Source | Changes from Original |
|------|--------|----------------------|
| `OnlineMenu.java` | Restored from M7 git | Remove `OnlineLobby.showLobby()` call (global chat lobby is a separate feature); keep profile display and all other buttons |
| `OnlineConfiguration.java` | Restored from M7 git | Remove: `LanManager.wakeAliveThread()`, `TestPublicConnect`, `OnlineServer.addWanGame()`, `DDMessage`/`PokerURL`/P2P imports. Keep: LAN IP display, LAN WebSocket URL, Public IP + Get button, Public WebSocket URL, "Post on Public Game List" checkbox, "Host as Observer" checkbox. New: "Open Lobby" creates game on embedded server |
| `Lobby.java` | Restored from M7 git | Remove: `OnlineManager`, `LanManager`, `HostStatus` panel, Fill Computer toggle. Replace: `PlayerModel` polls REST API every 2 s instead of listening to `PokerGame.PROP_PLAYERS`. Replace: Start! calls `RestGameClient.startGame(gameId)`. Chat: adapt `ChatPanel` usage to work with WebSocket client |
| `HostStart.java` | Restored from M7 git | Remove: AI fill logic (server handles it), `LanManager.wakeAliveThread()`. Keep: countdown timer and chat messages. Simplify: after countdown calls `nextPhase()` → `InitializeOnlineGame` |

---

## Files to Modify

### `poker` module

| File | Change |
|------|--------|
| `gamedef.xml` | Change `online.phase` from `FindGames` to `OnlineMenu`; add `OnlineMenu`, `OnlineConfiguration`, `Lobby.Host`, `Lobby.Player`, `HostStart` phase definitions (exact XML from M7 git, adapted) |
| `online/RestGameClient.java` | Add `createGame(GameConfig)` → `POST /api/v1/games`, `startGame(String gameId)` → `POST /api/v1/games/{id}/start`, `getGameSummary(String gameId)` → `GET /api/v1/games/{id}` |
| `GamePrefsPanel.java` | Add "Test Connection" button next to the WAN server URL field. On click: calls `GET {OPTION_ONLINE_SERVER}/health` and shows an information dialog with success/failure result |
| `client.properties` | Verify/add message keys used by restored classes that may be missing; remove/update any that reference deleted P2P concepts |

### `pokergameserver` module

| File | Change |
|------|--------|
| `dto/GameSummary.java` | Add `List<LobbyPlayerInfo> players` — each entry has `name` (String) and `role` (`"PLAYER"` or `"OBSERVER"`) |
| `dto/LobbyPlayerInfo.java` | New record: `(String name, String role)` |
| `service/GameService.java` | Populate `players` list in `getGameSummary()` from `GameInstance.getConnectedPlayers()` |
| `GameInstance.java` | Add `getConnectedPlayers()` returning list of name+role from `ServerPlayerSession` entries |

---

## Key Architecture Differences

### URLs
Old format: `ddpoker://IP:PORT/GAME_ID/PASSWORD` (custom P2P protocol)
New format: `ws://IP:PORT/ws/games/GAME_ID` (standard WebSocket)

`OnlineConfiguration` shows two URLs:
- **LAN**: `ws://LAN_IP:COMMUNITY_PORT/ws/games/{gameId}`
- **Public**: `ws://PUBLIC_IP:COMMUNITY_PORT/ws/games/{gameId}`

Both built from `CommunityHostingConfig` (already exists).

### "Open Lobby" button in OnlineConfiguration
Old: started P2P TCP server, added game to WAN server via old servlet protocol
New:
1. Start embedded server in external mode: `embeddedServer.start(CommunityHostingConfig.getPort())` (exposes on `0.0.0.0`)
2. Build `GameConfig` from the selected `TournamentProfile`
3. Call `RestGameClient.createGame(config)` → get back `gameId`
4. If "Post on Public Game List" checked: call `CommunityGameRegistration.register(gameName, wsUrl, null)` to register with WAN server
5. If "Host as Observer": flag for lobby to add host as observer instead of player
6. Navigate to `Lobby.Host`

### Player List in Lobby
Old: `PlayerModel` listened to `PokerGame.PROP_PLAYERS` PropertyChangeEvents fired by P2P `OnlineManager`
New: `PlayerModel` polls `RestGameClient.getGameSummary(gameId)` every 2 seconds on a background timer, updates table when player list changes. Timer stopped in `finish()`.

### Start! button in Lobby
Old: navigated to `HostStart` phase which ran a countdown and called `mgr_.sendDirectorChat()` etc.
New: calls `RestGameClient.startGame(gameId)` → navigates to `HostStart` which runs countdown chat via WebSocket then calls `nextPhase()` → `InitializeOnlineGame`.

### Cancel button in Lobby
Old: called `mgr_.cancelGame()` then `context_.restart()`
New: calls `RestGameClient.cancelGame(gameId)` (already exists), if registered with WAN server calls `CommunityGameRegistration.deregister()`, then `context_.restart()`

### Chat in Lobby
Old: `ChatPanel(game_, context_, mgr_, "ChatInGame", "BrushedMetal", false)` — used `OnlineManager` for sending
New: `ChatPanel` is adapted to use the `WebSocketGameClient` connection. Need to verify `ChatPanel` constructor/adapter — may need a thin adapter that sends via `wsClient.sendChat()`.

### HostStatus panel
Old: showed LED indicator for P2P host connection, with reconnect logic
New: **Removed entirely** — WebSocket handles reconnection transparently. Lobby shows a simpler static URL display for the host (reuse `createURLPanel()` pattern from old `Lobby.java`).

---

## gamedef.xml Phase Definitions

Restored from M7 git, with class names updated where needed:

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

<!-- LOBBY (joining player view - for when Join flow is also restored) -->
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

---

## Test Plan

- Build compiles with zero warnings
- `OnlineMenu` loads when clicking "Online" from main menu
- Profile name and online status display correctly
- "Host Online" → `TournamentOptions` → tournament selection works
- `OnlineConfiguration` opens with LAN IP pre-filled
- "Get Public IP" button fetches and displays public IP
- "Open Lobby" creates a game on the embedded server and navigates to `Lobby.Host`
- Lobby shows player list updating as a second client joins
- Chat works in lobby
- Game URLs displayed with working Copy buttons
- "Start!" starts the game and transitions to the table
- AI fills empty seats when `isFillComputer()` is true in tournament profile
- "Cancel" in lobby cancels the game on the server and returns to OnlineMenu
- Options: "Test Connection" shows success dialog when WAN server URL is valid and reachable

---

## Deferred: Save/Resume Online Games

**User question:** "I thought we had the ability to pause/resume online games via a database."

**Clarification on what exists today:**
- **In-game pause** (clock pause, stop dealing): `GameInstance.pauseAsUser()` / `resumeAsUser()` exist in the server. The `WebSocketGameClient.sendAdminPause/Resume()` exists in the client. The REST endpoints `POST /api/v1/games/{id}/pause` and `/resume` are designed in M6 but **not yet implemented**. This pause is in-memory — it stops the clock but game state is not persisted.
- **Save/resume across restarts** (persist to DB): This is **S19** from `SERVER-HOSTED-GAMES.md`. Not yet implemented. The original desktop P2P game did save to a local file, but the server-hosted game has no DB persistence of game state.

**Basic plan for S19 (future):**
1. Serialize `GameInstance` state (current table, hand, pot, player chips, blind level, hand history) to a `game_states` DB table on each hand completion
2. On server startup, restore any `IN_PROGRESS` games from DB
3. Client reconnects to restored game automatically
4. REST endpoint `POST /api/v1/games/{id}/save` for manual save
5. Restore flow: `LoadOnlineGameMenu` (already exists) → `RestGameClient.loadGame(id)` → server restores state → client connects via WebSocket

This will be filed as a separate plan when the host online flow is complete.
