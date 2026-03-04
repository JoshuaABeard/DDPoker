# Desktop + Server Bug Ledger (Phase 1)

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Status:** COMPLETED (2026-03-02) — all bugs fixed: BUG-001 through BUG-007 resolved, NEW-001 through NEW-013 resolved
**Created:** 2026-02-23
**Scope:** non-AI issues only

---

## Baseline Execution Status

- All tests passing: 628 pokergameserver, 1518 poker, 724 pokergamecore, 275 pokerengine (verified 2026-02-24).
- Bug hunt (NEW-001 through NEW-013) fixed in commit `1e5fbad3`.

---

## Confirmed Bugs / Concerns

### BUG-001 - Online server test button cannot form a valid HTTP URL

- **Area:** desktop client options
- **Severity:** High (P1)
- **Impact:** users cannot reliably verify WAN server connectivity from Options.
- **Evidence:**
  - `code/poker/src/main/java/com/donohoedigital/games/poker/GamePrefsPanel.java:466` restricts input to `host:port` (no scheme).
  - `code/poker/src/main/java/com/donohoedigital/games/poker/GamePrefsPanel.java:620` builds `healthUrl` directly from that value.
  - `code/poker/src/main/java/com/donohoedigital/games/poker/GamePrefsPanel.java:627` passes it to `URI.create(...)`, which requires an HTTP(S) scheme.
- **Repro:**
  1. Open Options -> Online settings.
  2. Enter a valid value accepted by current regex, e.g. `example.com:8877`.
  3. Click `Test Connection`.
  4. Request fails before any network roundtrip because URI is not a valid HTTP URL.
- **Fix direction:** normalize stored host to `http://<host:port>` for request construction (while preserving current preference format), then test against a server endpoint that exists.

### BUG-002 - "Post on Public Game List" uses malformed WAN base URL

- **Area:** host online flow
- **Severity:** High (P1)
- **Impact:** hosting can continue, but WAN posting silently fails for normal `host:port` config values.
- **Evidence:**
  - `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineConfiguration.java:283` reads `OPTION_ONLINE_SERVER` raw.
  - `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineConfiguration.java:285` passes raw value to `new RestGameClient(...)`.
  - `code/poker/src/main/java/com/donohoedigital/games/poker/online/RestGameClient.java:200` composes request URI as `baseUrl + "/api/v1/games/community"` and expects `baseUrl` to include scheme.
- **Repro:**
  1. Set Online Server preference to `your-server.com:8877`.
  2. Host an online game with `Post on Public Game List` enabled.
  3. Observe warning log and no WAN listing.
- **Fix direction:** shared URL normalization utility for all REST clients and online flows.

### BUG-003 - Blocking REST calls executed on Swing EDT

- **Area:** desktop dialogs and host setup flow
- **Severity:** High (P1)
- **Impact:** UI can freeze during network slowness/outage; users may think app is hung.
- **Evidence:**
  - `code/poker/src/main/java/com/donohoedigital/games/poker/PlayerProfileDialog.java:337` (register), `:350` (login), `:371` (update email) execute synchronously in `processButton(...)`.
  - `code/poker/src/main/java/com/donohoedigital/games/poker/PlayerProfileDialog.java:490` executes forgot-password synchronously from button action path.
  - `code/poker/src/main/java/com/donohoedigital/games/poker/ChangePasswordDialog.java:120` executes change-password synchronously in `processButton(...)`.
  - `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineConfiguration.java:252` executes game creation synchronously in button processing.
- **Repro:**
  1. Point Online Server to an unreachable host or add high latency.
  2. Trigger register/login/change-password/open-lobby.
  3. UI blocks until HTTP request timeout/exception.
- **Fix direction:** move all REST operations off EDT; disable relevant controls while request is in-flight; marshal result back with `SwingUtilities.invokeLater(...)`.

### BUG-004 - Lobby polling creates unbounded background threads

- **Area:** pre-game lobby polling
- **Severity:** Medium-High (P2)
- **Impact:** slow or failing server can accumulate `LobbyPoll` threads and increase memory/CPU usage.
- **Evidence:**
  - `code/poker/src/main/java/com/donohoedigital/games/poker/online/Lobby.java:188` starts a timer every 2s.
  - `code/poker/src/main/java/com/donohoedigital/games/poker/online/Lobby.java:201` spawns a new thread each tick without in-flight guard.
- **Repro:**
  1. Enter lobby with polling active.
  2. Delay server responses to >2s or simulate partial outage.
  3. Observe multiple concurrent `LobbyPoll` threads.
- **Fix direction:** single-thread scheduled executor or in-flight atomic guard with skip-while-running behavior.

### BUG-005 - Initial WebSocket connect failure does not trigger retry cycle

- **Area:** online gameplay connection lifecycle
- **Severity:** High (P1)
- **Impact:** transient startup/connect races can leave client disconnected with no automatic recovery.
- **Evidence:**
  - `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketGameClient.java:199` attempts async connect.
  - `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketGameClient.java:205` logs failure in `exceptionally(...)` and swallows it.
  - Reconnect scheduling exists only in listener callbacks (`:316` `onClose`, `:323` `onError`).
- **Repro:**
  1. Start online session while WS endpoint is briefly unavailable.
  2. Initial `buildAsync` fails.
  3. No reconnect attempts occur because failure happened before listener lifecycle callbacks.
- **Fix direction:** route initial connect failure into the same reconnect state machine as disconnect failures.

### BUG-006 - Lobby WebSocket sends are not serialized per session

- **Area:** server-side lobby chat broadcasting
- **Severity:** Medium (P2)
- **Impact:** potential intermittent send failures or frame corruption under concurrent broadcasts.
- **Evidence:**
  - `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/LobbyWebSocketHandler.java:284` calls `session.sendMessage(...)` directly.
  - `connectedPlayers` is concurrent (`:96`), and broadcast paths can execute concurrently.
  - Spring `WebSocketSession.sendMessage` is not thread-safe unless externally serialized.
- **Repro:**
  1. Connect multiple users to lobby.
  2. Produce concurrent join/leave/chat bursts.
  3. Observe sporadic send errors or dropped deliveries in logs.
- **Fix direction:** serialize sends per session (wrapper lock or dedicated outbound queue).

### BUG-007 - Online game purger parse error logs wrong value

- **Area:** server command-line tool diagnostics
- **Severity:** Low (P3)
- **Impact:** misleads operator during date parse failures.
- **Evidence:**
  - `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/OnlineGamePurger.java:91` logs `date_` instead of raw `date` input in parse error path.
- **Repro:**
  1. Run purger with invalid `--date` value.
  2. Error message reports null/incorrect value.
- **Fix direction:** report the original input string.

---

## Fixed Bugs (Bug Hunt — 2026-02-24)

Fixed in `1e5fbad3` — Plan: `.claude/plans/replicated-meandering-stardust.md`

### NEW-001 [FIXED] — `ServerGameEventBus.publish` broadcastCallback unprotected

- **Area:** server event pipeline
- **Severity:** Critical
- **Fix:** Wrapped `broadcastCallback.accept(event)` in try/catch so a single callback failure no longer crashes the event pipeline.

### NEW-002 [FIXED] — `PlayerConnection.sendMessage` rethrows `JsonProcessingException`

- **Area:** server WebSocket broadcast
- **Severity:** Critical
- **Fix:** Changed from `throw new RuntimeException(...)` to `logger.error(...)`. Serialization failure no longer crashes the game director thread.

### NEW-003 [FIXED] — `GameConnectionManager.broadcastToGame` one failure kills stream

- **Area:** server WebSocket broadcast
- **Severity:** Critical
- **Fix:** Added per-connection try/catch in the broadcast stream so one failed send doesn't skip remaining connections.

### NEW-004 [FIXED] — `onActionRequest` silently drops when messageSender null — game deadlocks

- **Area:** server game instance
- **Severity:** Critical
- **Fix:** When messageSender is null, auto-submit check/fold via `actionProvider.submitAction()` to prevent infinite deadlock (especially in practice mode where timeout=0).

### NEW-005 [FIXED] — `offerNeverBroke` missing zero-timeout handling

- **Area:** server game instance
- **Severity:** High
- **Fix:** Added `if (timeout <= 0) future.get()` pattern matching `offerRebuy`/`offerAddon`.

### NEW-006 [FIXED] — `ServerTournamentDirector.run()` fires COMPLETED after ERROR

- **Area:** server game lifecycle
- **Severity:** High
- **Fix:** Added `boolean fatalError` flag; skip COMPLETED lifecycle callback in finally block when a fatal error already fired ERROR.

### NEW-007 [FIXED] — `directorFuture` never monitors uncaught `Error`

- **Area:** server game lifecycle
- **Severity:** High
- **Fix:** Changed `catch (Exception e)` to `catch (Throwable e)` so `Error` subclasses don't silently kill the game. Updated `handleFatalError` signature to accept `Throwable`.

### NEW-008 [FIXED] — `ExitPoker.java:76` NPE on `getLocalPlayer()`

- **Area:** desktop client
- **Severity:** High
- **Fix:** Null guard `getLocalPlayer()` before chained `.getID()` call.

### NEW-009 [FIXED] — `ShowTournamentTable.java:309` NPE on rebuy button chain

- **Area:** desktop client
- **Severity:** High
- **Fix:** Null guard `getHumanPlayer()` and `.getTable()` before calling `NewLevelActions.rebuy(...)`.

### NEW-010 [FIXED] — `getLocalPlayer().isHost()` NPE in multiple files

- **Area:** desktop client (ShowTournamentTable, GameInfoDialog, ChipLeaderPanel)
- **Severity:** High
- **Fix:** Null guard `getLocalPlayer()` before `.isHost()` in all three locations.

### NEW-011 [FIXED] — `getCurrentTable()` NPE in multiple files

- **Area:** desktop client (GameInfoDialog, SidePotsDialog, WaitForDeal)
- **Severity:** High
- **Fix:** Null guard `getCurrentTable()` before chained calls in all three locations.

### NEW-012 [FIXED] — `sendComeBack()` missing from `WebSocketGameClient`

- **Area:** client WebSocket protocol
- **Severity:** Medium
- **Fix:** Added `sendComeBack()` method sending `ClientMessageType.COME_BACK`.

### NEW-013 [FIXED] — `sendAdminKick()` missing from `WebSocketGameClient`

- **Area:** client WebSocket protocol
- **Severity:** Medium
- **Fix:** Added `sendAdminKick(long playerId)` method sending `ClientMessageType.ADMIN_KICK`.

### NEW-014 — `TIMER_UPDATE` server message type is dead code

- **Area:** server WebSocket protocol
- **Severity:** Low (informational)
- **Status:** Known — intentional reserved type for future action timer feature. No runtime impact.

---

## Next Fix Slice Proposal (Non-AI)

1. URL normalization pass for all online REST entry points (`GamePrefsPanel`, `OnlineConfiguration`, auth dialogs).
2. EDT offloading for all blocking REST calls in dialogs and host setup.
3. Lobby polling concurrency guard.
4. WebSocket initial-connect retry unification.
5. Server lobby send serialization.
