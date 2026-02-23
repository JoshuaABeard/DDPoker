# Review Request

**Branch:** fix-desktop-server-bug-cleanup
**Worktree:** ../DDPoker-fix-desktop-server-bug-cleanup
**Plan:** `.claude/plans/DESKTOP-SERVER-BUG-LEDGER.md` (local working draft used for bug IDs)
**Requested:** 2026-02-23 13:43

## Summary

Completed bug-cleanup fixes for BUG-001 through BUG-007 spanning desktop networking, EDT safety, websocket reconnect behavior, lobby broadcast safety, and purger diagnostics.
Continued into the next hardening slice by tightening broad-catch/diagnostic behavior in desktop startup/dashboard paths and server purger CLIs.
Finished reconnect lifecycle hardening by preventing duplicate reconnect scheduling in both game and lobby websocket clients.
Stabilized remaining `pokergameserver` flaky tests by tightening event-order assertions and making long-running tournament tests deterministic.

## Files Changed

- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/GamePrefsPanel.java` - normalize server URL and use a valid REST endpoint for connection testing.
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/PlayerProfileDialog.java` - move profile REST actions off EDT with in-flight UI controls.
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/ChangePasswordDialog.java` - move password change REST call off EDT.
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineConfiguration.java` - move lobby/game create flows off EDT and guard controls while requests are in flight.
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/Lobby.java` - add in-flight poll guard to prevent overlapping lobby poll threads.
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/RestGameClient.java` - normalize base URL in REST client construction.
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/RestAuthClient.java` - replace swallowed parse exceptions with typed catches and debug diagnostics for error-body fallback parsing.
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketGameClient.java` - route initial connection failures into reconnect flow and invalidate stale reconnect attempts when a new connect session starts.
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/LobbyChatWebSocketClient.java` - guard reconnect scheduling/connect attempts to avoid duplicate loops and duplicate disconnect notifications.
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineServerUrl.java` - shared server URL normalization utility.
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/LobbyWebSocketHandler.java` - serialize per-session outbound websocket sends.
- [ ] `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/OnlineGamePurger.java` - include original `--date` input in parse error path, extract parse helper, and return non-zero process exit code on failure.
- [ ] `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/OnlineProfilePurger.java` - return non-zero process exit code on failure.
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/PokerStatsPanel.java` - replace `invokeLater(new Thread(...))` with direct EDT runnable.
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/PokerSimulatorPanel.java` - replace `invokeLater(new Thread(...))` with direct EDT runnable.
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/PokerMain.java` - narrow startup prereq broad catch to `Exception` and improve retry-path diagnostics.
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/dashboard/DashboardManager.java` - narrow demarshal catches to `RuntimeException` and add contextual logging without swallowing JVM errors.
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/ListGames.java` - replace clipboard `catch (Throwable)` with specific exception handling and debug logging.
- [ ] `code/poker/src/test/java/com/donohoedigital/games/poker/online/OnlineServerUrlTest.java` - URL normalization and URI composition coverage.
- [ ] `code/poker/src/test/java/com/donohoedigital/games/poker/online/RestGameClientTest.java` - normalized base URL handling coverage.
- [ ] `code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketGameClientTest.java` - initial connect failure reconnect coverage and stale reconnect invalidation coverage.
- [ ] `code/poker/src/test/java/com/donohoedigital/games/poker/online/LobbyChatWebSocketClientTest.java` - reconnect scheduling dedupe and disconnect notification dedupe coverage.
- [ ] `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/OnlineGamePurgerTest.java` - date parse helper behavior and invalid-input message coverage.
- [ ] `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/GameInstanceTest.java` - stabilize AI-only completion test by using a smaller deterministic AI-only setup.
- [ ] `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/ServerTournamentDirectorTest.java` - stabilize showdown/elimination/chip-conservation assertions and reduce timing flake with deterministic aggressive AI + zero hand-result pause where appropriate.

**Privacy Check:**
- ✅ SAFE - No private information found in changed files.

## Verification Results

- **Tests:**
  - `mvn test -pl poker,pokergameserver -P dev` ✅
  - `mvn test -pl pokergameserver -P dev` ✅
  - `mvn test -pl pokerserver -Dtest=OnlineGamePurgerTest -P dev` ✅
  - `mvn test -pl pokerserver -P dev` ✅
  - `mvn test -pl poker -P dev` ✅
  - `mvn test -pl poker,pokerserver -P dev` ✅
  - `mvn test -pl poker -Dtest=WebSocketGameClientTest,LobbyChatWebSocketClientTest -P dev` ✅
  - `mvn test -pl pokergameserver -Dtest=ServerTournamentDirectorTest#showdownEventsPublished -P dev` ✅
  - `mvn test -pl pokergameserver -Dtest=ServerTournamentDirectorTest#chipsAreConserved -P dev` (repeated loop) ✅
  - `mvn test -pl pokergameserver -Dtest=GameInstanceTest#testAIOnlyGameRunsToCompletion,ServerTournamentDirectorTest#playerEliminatedEventsPublished -P dev` ✅
  - `mvn test -pl pokergameserver -P dev` ✅
  - `mvn test -P dev` ✅
- **Coverage:** Not measured in this pass (coverage profile not run)
- **Build:** Clean test builds; expected ERROR-level log lines come from negative-path test scenarios.

## Context & Decisions

The URL issues (BUG-001/002) were fixed with one shared normalizer to avoid drift in endpoint construction.
UI freeze issues (BUG-003) were fixed at event-trigger call sites so existing UX/state handling remains intact while network calls run off EDT.
Reconnect and websocket send fixes (BUG-005/006) reuse existing lifecycle patterns with minimal structural change.
The purger fix (BUG-007) extracts date parsing into a helper to make invalid-input behavior directly testable.
Server purger mains now return non-zero exit status when startup/purge fails so automation can detect failures reliably.
Reconnect lifecycle hardening now prevents stale scheduled reconnect work from creating duplicate connections after a fresh explicit connect.

---

## Review Results

*[Review agent fills this section]*

**Status:**

**Reviewed by:**
**Date:**

### Findings

#### ✅ Strengths

#### ⚠️ Suggestions (Non-blocking)

#### ❌ Required Changes (Blocking)

### Verification

- Tests:
- Coverage:
- Build:
- Privacy:
- Security:
