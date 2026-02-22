## Review Request

**Branch:** feature-restore-host-online
**Worktree:** ../DDPoker-feature-restore-host-online
**Plan:** (none — spec provided inline)
**Requested:** 2026-02-22

## Summary

Restores the host-online game flow that was deleted in a prior cleanup. The implementation
adapts the original P2P networking approach to use the embedded WebSocket game server and
REST API. Six new/modified areas: `LobbyPlayerInfo` DTO, `GameSummary` player list,
`RestGameClient` new methods, `OnlineMenu`, `OnlineConfiguration`, `Lobby`, and `HostStart`.
Also fixes pre-existing switch exhaustiveness bugs in `GameEventBroadcaster` and `SwingEventBus`.

## Files Changed

### New files
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/dto/LobbyPlayerInfo.java` — new record DTO `(String name, String role)` for lobby player list
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineMenu.java` — online entry menu, extends `MenuPhase`, shows profile name/button
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineConfiguration.java` — host network config screen; detects LAN IP, shows public IP/URL, posts to public game list, creates game on embedded server, navigates to Lobby
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/Lobby.java` — pre-game waiting room; polls REST API every 2 sec for player list; host can start or cancel
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/HostStart.java` — countdown phase before game starts; overrides `ChainPhase.start()` to prevent immediate `nextPhase()` call

### Modified files
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/dto/GameSummary.java` — added `List<LobbyPlayerInfo> players` field
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/GameInstance.java` — added `getConnectedPlayers()` method
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/service/GameService.java` — `getGameSummary()` now populates players from `GameInstance`; added `toSummaryWithPlayers()` helper
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/GameEventBroadcaster.java` — added `default` arm to switch (pre-existing bug fix)
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/RestGameClient.java` — added `createGame(GameConfig)`, `startGame(String)`, `getGameSummary(String)`
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/SwingEventBus.java` — added `default` arms to both switch expressions (pre-existing bug fix)
- [ ] `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/controller/GameControllerTest.java` — updated `buildSummary()` with extra `emptyList()` arg
- [ ] `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/controller/GameControllerSecurityTest.java` — same fix
- [ ] `code/poker/src/test/java/com/donohoedigital/games/poker/online/GameSummaryConverterTest.java` — same fix
- [ ] `code/poker/src/main/resources/config/poker/gamedef.xml` — changed `online.phase` from `FindGames` to `OnlineMenu`; added phase definitions for `OnlineMenu`, `OnlineConfiguration`, `Lobby.Host`, `Lobby.Player`, `HostStart`
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/GamePrefsPanel.java` — replaced old P2P `testConnection()` with HTTP GET `/health` check against configured WAN server URL; removed unused P2P imports
- [ ] `code/poker/src/main/resources/config/poker/client.properties` — added `msg.testconnect.*` message keys for the test connection result dialogs

**Privacy Check:**
- SAFE - No private information found. No credentials, URLs, IPs, or personal data in source files.

## Verification Results

- **Tests:** `mvn test -P dev` — all tests pass (BUILD SUCCESS, 2:25 min)
- **Coverage:** N/A for new UI phases (Lobby, OnlineMenu, OnlineConfiguration, HostStart are Swing phases with no testable logic)
- **Build:** Clean with zero errors

## Context & Decisions

**`POST /api/v1/games` returns `CreateGameResponse(gameId)`, not `GameSummary`**: `RestGameClient.createGame()` makes two HTTP calls — POST to create, then GET to fetch the full summary. This is correct per the server API contract.

**Static `activeRegistration_` on `OnlineConfiguration`**: WAN registration stored as static field on `OnlineConfiguration` (not on `PokerGame` which has no registration field). `Lobby` accesses it via `OnlineConfiguration.getActiveRegistration()` during cancel. This matches the original design pattern for cross-phase state.

**`HostStart` overrides `ChainPhase.start()`**: `ChainPhase.start()` calls `process()` then `nextPhase()` immediately. `HostStart` needs async countdown before `nextPhase()`, so it overrides `start()` to call only `process()` and let the timer call `nextPhase()` later.

**`VerticalFlowLayout` — no `FULL` constant**: The class only has `LEFT=0`, `CENTER=1`, `RIGHT=2`, `TOP=3`, `BOTTOM=4`. Used 3-arg constructor `new VerticalFlowLayout(TOP, hgap, vgap)` throughout.

**`GuiUtils.WEST()` not `LEFT()`**: `GuiUtils` wraps components in `BorderLayout` slots by cardinal direction name. `LEFT` does not exist; used `WEST`.

**`BasePhase.start()` is abstract**: Cannot call `super.start()`. Both `OnlineConfiguration` and `Lobby` call `context_.setMainUIComponent()` directly in their `start()` implementations.

**`DDScrollTable` constructor**: Uses `DDScrollTable(name, style, bevelStyle, String[] cols, int[] widths)` — call `scroll.getDDTable()` to get the table reference for model assignment.

**Pre-existing switch exhaustiveness bugs**: `GameEventBroadcaster` and `SwingEventBus` both had switch statements on sealed `GameEvent` that lacked `default` arms, causing compile errors. Fixed as part of this change since they blocked compilation.

---

## Review Results

**Status:** CHANGES REQUIRED

**Reviewed by:** Claude Sonnet 4.6
**Date:** 2026-02-22

### Findings

#### Strengths

1. **Server-side changes are solid.** `LobbyPlayerInfo` DTO, `GameInstance.getConnectedPlayers()`, and `GameService.toSummaryWithPlayers()` are minimal, well-scoped, and correctly wired. The separation between `toSummary()` (list view, empty players) and `toSummaryWithPlayers()` (detail view, live players) is clean.

2. **REST polling in Lobby is correctly off-EDT.** Each `pollServer()` call spawns a daemon thread, performs the blocking HTTP call there, and dispatches player-list/status updates back via `SwingUtilities.invokeLater`. The 2-second `javax.swing.Timer` is correctly started in `start()` and stopped in `finish()`, preventing resource leaks across phase transitions.

3. **HostStart timer management is clean.** The override of `ChainPhase.start()` to suppress the immediate `nextPhase()` is the right approach and is well-documented. Timer is stopped before `nextPhase()` is called.

4. **RestGameClient new methods follow established patterns.** `createGame`, `startGame`, and `getGameSummary` all propagate `RestGameClientException` for error handling by callers, and the two-call pattern (POST then GET) for `createGame` correctly matches the server API contract.

5. **Switch exhaustiveness fixes are safe.** Both `default` arms in `GameEventBroadcaster` and `SwingEventBus` log/return null cleanly. These were genuine compile-blocking pre-existing bugs and the fix is minimal and surgical.

6. **Privacy is clean.** No credentials, private IPs, or personal data introduced.

7. **Copyright on `LobbyPlayerInfo.java` is correct.** New file, community copyright (Template 3). The `GameSummary.java` (which is also a community-created file, now modified) correctly retains community copyright.

---

#### Required Changes (Blocking)

**R1. `Lobby.cancel_` is null — NullPointerException when host clicks Start.**
`code/poker/src/main/java/com/donohoedigital/games/poker/online/Lobby.java:121`

`buttonbox_.getButton("cancel")` performs an exact name lookup. The Lobby.Host phase defines the button as `canceltourney` and Lobby.Player as `cancelleave`. Neither matches `"cancel"`, so `cancel_` is always `null`.

At line 232: `cancel_.setEnabled(false)` — NullPointerException the moment the host confirms the Start dialog.
At line 246: `cancel_.setEnabled(true)` — same in the error recovery path.

Fix: Use `buttonbox_.getButtonStartsWith("cancel")` instead of `buttonbox_.getButton("cancel")`.

The guard `if (cancel_ != null && button.getName().startsWith("cancel"))` on the cancel path (line 259) means cancel logic never executes either — the cancel button click falls through and returns `true`, causing the framework to attempt default phase navigation instead of the cancel cleanup.

**R2. `OnlineMenu` references a non-existent phase `OnlineOptions`.**
`code/poker/src/main/resources/config/poker/gamedef.xml:310`

```xml
<strvalue>hostonline:OnlineOptions</strvalue>
```

There is no `<phase name="OnlineOptions">` in gamedef.xml. `OnlineOptions` is an inner class (`private class OnlineOptions extends OptionTab`) inside `GamePrefsPanel` — it is a UI tab widget, not a game phase. Clicking "Host Online" from `OnlineMenu` will fail at runtime when the engine looks up the `OnlineOptions` phase and finds nothing.

The correct target for the `hostonline` button appears to be `OnlineConfiguration` (which is the "Setup Online Tournament (2 of 2)" screen — but there should be a "1 of 2" profile/tournament selection step first, presumably `TournamentOptions`). Alternatively, the intended flow may be `hostonline:TournamentOptions` then `okaystart → OnlineConfiguration`. This needs clarification and a fix.

**R3. Nine message keys referenced in code but missing from `client.properties`.**

The following keys are read via `PropertyConfig.getMessage(...)` in new code but are not defined in `code/poker/src/main/resources/config/poker/client.properties`:

| Key | Used in |
|-----|---------|
| `msg.hostonline.noserver` | `OnlineConfiguration.java:210` |
| `msg.hostonline.startfail` | `OnlineConfiguration.java:221` |
| `msg.hostonline.createfail` | `OnlineConfiguration.java:245` |
| `msg.hostonline.profile` | `OnlineConfiguration.java:166` |
| `msg.lobby.col.num` | `Lobby.java:145` |
| `msg.lobby.col.name` | `Lobby.java:146` |
| `msg.lobby.col.role` | `Lobby.java:147` |
| `msg.lobby.startfail` | `Lobby.java:250` |
| `msg.wanserver.url` | `OnlineConfiguration.java:270` |

`msg.wanserver.url` is particularly important: if missing, `PropertyConfig.getMessage("msg.wanserver.url", "")` will likely throw or return null/empty depending on the overload, silently skipping WAN registration entirely.

The column header keys (`msg.lobby.col.*`) are displayed in the player table on every Lobby open. `msg.hostonline.*` keys are shown in error dialogs. All of these must be added to `client.properties`.

**R4. Wrong copyright headers on four new UI phase files.**
`Lobby.java`, `OnlineConfiguration.java`, `OnlineMenu.java`, `HostStart.java` are new files (not in git history before this commit), so per the copyright guide they must use Template 3 (community copyright). All four currently use Template 1 (original Doug Donohoe copyright). Required change: update to Template 3.

---

#### Suggestions (Non-blocking)

**S1. `GamePrefsPanel.testConnection()` blocks the EDT.**
`code/poker/src/main/java/com/donohoedigital/games/poker/GamePrefsPanel.java:625`

`testConnection()` is called from an `ActionListener` (EDT). It calls `client.send(...)` with a 10-second timeout — blocking the entire UI for up to 10 seconds. Fix: run the HTTP call in a background thread and dispatch the result dialog back via `SwingUtilities.invokeLater`. This is a usability issue, not a correctness bug, since the 10s timeout and exception catch prevent a hang.

**S2. `OnlineConfiguration.actionPerformed()` blocks the EDT on public IP fetch.**
`code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineConfiguration.java:189`

`PublicIPDetector.fetchPublicIP()` makes blocking HTTP calls through up to 3 external services. Called on EDT via `ActionListener`. Same pattern as S1 — should run off-EDT. Non-blocking because there is a timeout in `DDHttpClient`, but it can freeze the UI.

**S3. Non-host cancel path does not navigate away from Lobby.**
`code/poker/src/main/java/com/donohoedigital/games/poker/online/Lobby.java:287`

For non-host players, the cancel handler calls `context_.setGame(null)` but does not call `context_.restart()` or `context_.processPhase(...)`. The UI stays on the Lobby screen after cancel. Compare to the host path (line 285) which correctly calls `context_.restart()`. Should add navigation for the non-host path.

**S4. `Lobby.cancel_` disable call on start is also null for non-host (per R1).**
When R1 is fixed using `getButtonStartsWith("cancel")`, verify the guard at line 259 still correctly matches `canceltourney` and `cancelleave` — both start with `"cancel"` so the `startsWith` check is appropriate.

**S5. `OnlineConfiguration.doOpenLobby()` calls `reg.deregister()` on EDT during cancel.**
`Lobby.java:269–271` — `CommunityGameRegistration.deregister()` calls `client.cancelGame(gameId)` which calls `http.send(...)` (blocking). This is on the EDT. Low severity since `cancelGame` swallows all exceptions and may return quickly, but it risks a UI freeze on a slow network.

**S6. `getConnectedPlayers()` could use an import instead of fully qualified names.**
`code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/GameInstance.java:573–578`

Uses fully qualified `com.donohoedigital.games.poker.gameserver.dto.LobbyPlayerInfo` and `java.util.stream.Collectors` in the method body. `LobbyPlayerInfo` is already in the same package group; add the import at the top of the file instead. Purely stylistic — Spotless may handle this on the next build.

---

### Verification

- Tests: Reported as passing by dev agent (`mvn test -P dev`, BUILD SUCCESS). Not re-run independently (worktree no longer available).
- Coverage: N/A for Swing phase classes (no testable logic outside EDT interaction).
- Build: Reported clean by dev agent. Tests pass so compilation is confirmed.
- Privacy: SAFE — no credentials, private IPs, or personal data. `msg.wanserver.url` is a config key, not a hardcoded URL.
- Security: No new authentication bypasses or injection vectors. REST calls use the existing JWT auth pattern. No issues.
