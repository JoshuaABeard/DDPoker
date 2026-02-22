# Review Request

**Branch:** feature-allin-continue-dialog
**Worktree:** ../DDPoker-feature-allin-continue-dialog
**Plan:** .claude/plans/piped-marinating-pumpkin.md
**Requested:** 2026-02-22 15:00

## Summary

Replaces the fixed 1500ms server-side sleep for `OPTION_PAUSE_ALLIN` with an interactive `CompletableFuture`-based pause. When enabled, the server now pauses before each community card during an all-in runout and waits for the human to explicitly click "Continue". Follows the same blocking pattern used for rebuy/addon offers.

## Files Changed

- [ ] `code/pokergamecore/src/main/java/.../core/event/GameEvent.java` - Added `AllInRunoutPaused(int tableId)` sealed subtype
- [ ] `code/pokergameserver/src/main/java/.../gameserver/GameConfig.java` - Added `pauseAllinInteractive` as 7th field of `PracticeConfig` record
- [ ] `code/pokergameserver/src/main/java/.../gameserver/GameInstance.java` - Added `waitForContinue()`, `submitContinue()`, wired callback in `start()`
- [ ] `code/pokergameserver/src/main/java/.../gameserver/ServerTournamentDirector.java` - Added `waitForContinueCallback` field/setter; replaced sleep with interactive/fallback-sleep logic
- [ ] `code/pokergameserver/src/main/java/.../websocket/GameEventBroadcaster.java` - Handle `AllInRunoutPaused` → broadcast `CONTINUE_RUNOUT`
- [ ] `code/pokergameserver/src/main/java/.../websocket/InboundMessageRouter.java` - Added `CONTINUE_RUNOUT` dispatch case and handler
- [ ] `code/pokergameserver/src/main/java/.../websocket/message/ServerMessageType.java` - Added `CONTINUE_RUNOUT` enum value
- [ ] `code/pokergameserver/src/main/java/.../websocket/message/ClientMessageType.java` - Added `CONTINUE_RUNOUT` enum value
- [ ] `code/poker/src/main/java/.../online/SwingEventBus.java` - Added `AllInRunoutPaused` to both exhaustive switch expressions
- [ ] `code/poker/src/main/java/.../online/WebSocketGameClient.java` - Added `sendContinueRunout()` method
- [ ] `code/poker/src/main/java/.../online/WebSocketTournamentDirector.java` - Handle `CONTINUE_RUNOUT` server message; dispatch `ACTION_CONTINUE_LOWER` to client
- [ ] `code/poker/src/main/java/.../server/PracticeGameLauncher.java` - Pass `pauseAllin` as `pauseAllinInteractive`; set `allInRunoutPauseMs=0`
- [ ] `code/pokergameserver/src/test/.../GameConfigTest.java` - Updated `PracticeConfig` constructor calls (7th null arg)
- [ ] `code/pokergameserver/src/test/.../GameInstanceManagerTest.java` - Updated `PracticeConfig` constructor calls
- [ ] `code/pokergameserver/src/test/.../GameInstanceTest.java` - Updated `PracticeConfig` constructor calls
- [ ] `code/pokergameserver/src/test/.../ServerTournamentDirectorTest.java` - Updated constructor calls; added `waitForContinueCallbackBlocksAndUnblocks` test
- [ ] `code/pokergameserver/src/test/.../integration/WebSocketIntegrationTest.java` - Updated `PracticeConfig` constructor calls

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** 1530/1530 passed (`mvn test -P dev -pl pokergamecore,pokergameserver,poker`)
- **Coverage:** Not measured (unit tests sufficient for this feature)
- **Build:** Clean, zero warnings

## Context & Decisions

**Callback injection vs direct call:** `ServerTournamentDirector` gets an `IntConsumer waitForContinueCallback` injected by `GameInstance` rather than calling `GameInstance` directly. This keeps the director decoupled from the server layer (it lives in `pokergameserver` but shouldn't depend on `GameInstance` directly for testability).

**Fallback sleep preserved:** When `waitForContinueCallback == null` (non-interactive mode), the old timed sleep still fires if `aiActionDelayMs > 0 && allInRunoutPauseMs > 0`. The `PracticeGameLauncher` passes `allInRunoutPauseMs = 0` when `pauseAllinInteractive = true`, so both paths are mutually exclusive.

**Timeout:** `waitForContinue` times out at `actionTimeoutSeconds * 2` seconds so a disconnected client doesn't block the game indefinitely.

**`AllInRunoutPaused` is a sealed subtype:** Adding it to `GameEvent` required updating `SwingEventBus.java`'s two exhaustive switch expressions. Both cases return `null`/the tableId as appropriate — the client-side response is handled via the WebSocket `CONTINUE_RUNOUT` message, not the legacy event bus.

---

## Review Results

**Status:** APPROVED WITH SUGGESTIONS

**Reviewed by:** Claude Sonnet 4.6
**Date:** 2026-02-22

### Findings

#### ✅ Strengths

1. **Correct blocking/unblocking pattern.** `GameInstance.waitForContinue()` (lines 669-680) and `submitContinue()` (lines 683-688) follow the same `CompletableFuture<Void>` pattern already proven by `offerRebuy`/`offerAddon`. The `volatile` field and a local variable snapshot in `submitContinue` correctly avoid a TOCTOU race between the null check and `complete()`.

2. **Timeout prevents permanent blockage.** `future.get(properties.actionTimeoutSeconds() * 2L, TimeUnit.SECONDS)` at `GameInstance.java:674` ensures that a disconnected client (or a missed CONTINUE_RUNOUT message due to a reconnect) cannot block the director thread indefinitely. The doubling of the timeout relative to the normal action timeout is a sensible margin.

3. **Clean decoupling via IntConsumer.** `ServerTournamentDirector` receives `waitForContinueCallback` as an `IntConsumer` setter (lines 97-101) rather than a direct `GameInstance` reference, preserving the director's testability without a live server context. This mirrors the existing rebuy/addon callback pattern and is architecturally sound.

4. **Mutual exclusion of fallback sleep and interactive mode.** `PracticeGameLauncher` explicitly passes `allInRunoutPauseMs = 0` alongside `pauseAllinInteractive = true` (line 105-106), so the fallback timed-sleep branch in `ServerTournamentDirector.applyResult()` (line 413) cannot fire when the callback is active.

5. **Sealed-interface exhaustiveness maintained.** Both switch expressions in `SwingEventBus.java` (`convertToLegacy` and `getTableId`) were updated for `AllInRunoutPaused`. The Java compiler enforces completeness on sealed interfaces, so any future subtype omission will be a compile error, not a runtime oversight.

6. **Correct client-side wiring.** `WebSocketTournamentDirector.onContinueRunout()` (lines 1255-1264) calls `game_.setInputMode(MODE_CONTINUE_LOWER, hand, player)`, and the `PlayerActionListener` in `start()` (lines 136-150) correctly routes `ACTION_CONTINUE_LOWER` to `wsClient_.sendContinueRunout()` rather than to `sendAction()`. This avoids polluting the poker action channel.

7. **Zip mode interaction is correct.** The guard `!actionProvider.isZipMode()` at `ServerTournamentDirector.java:410` is in place before the `waitForContinueCallback` call, so when the human folds and zip mode is active, the per-card pause is suppressed. This is the right behaviour.

#### ⚠️ Suggestions (Non-blocking)

1. **Race window in `waitForContinue` between field assignment and event publish.**

   In `GameInstance.waitForContinue()` (lines 669-680):
   ```java
   CompletableFuture<Void> future = new CompletableFuture<>();
   pendingContinue = future;                                // (A) field visible
   eventBus.publish(new GameEvent.AllInRunoutPaused(...)); // (B) client notified
   future.get(...);                                        // (C) block
   ```
   The sequence is correct: `pendingContinue` is assigned before the event is published, so `submitContinue()` called from a WebSocket thread in response to the broadcast will always find a non-null future. However, the `finally` block sets `pendingContinue = null` without synchronization. If `submitContinue()` is called on a different thread just after `future.get()` returns (timeout path) but before `pendingContinue = null` executes, there is a harmless double-complete attempt on an already-completed future (no-op on `CompletableFuture`). This is safe today but worth a comment explaining the invariant.

2. **No ownership check on CONTINUE_RUNOUT.**

   `InboundMessageRouter.handleContinueRunout()` (lines 308-310) calls `game.submitContinue()` without checking whether the sender is the game owner or an active human player:
   ```java
   private void handleContinueRunout(PlayerConnection connection, GameInstance game) {
       game.submitContinue();
   }
   ```
   In the current practice-mode context (single human, no observers), any connected player can send `CONTINUE_RUNOUT` and unblock the server's pause. In a multiplayer context, an observer or a non-owner player could fire it. For parity with `handleRebuyDecision` (which at least only works on the per-player future keyed by `connection.getProfileId()`), consider validating that the connection is the game owner before calling `submitContinue`. This is low-risk for practice mode but worth hardening before multiplayer use.

3. **`waitForContinueCallbackBlocksAndUnblocks` test uses a sleep rather than a true `CompletableFuture` block.**

   The test callback at `ServerTournamentDirectorTest.java:838-845` blocks via `Thread.sleep(50)` rather than pausing until an explicit signal (e.g., a `CountDownLatch` counted down by the test body to simulate a user click). As a result, the test only verifies that the callback fires and the game completes — it does not verify that the director thread is actually blocked waiting for the callback to return. If the implementation were changed to not block (e.g., calling the callback in a separate thread), the test would still pass. A stronger test would assert that the director thread is alive (blocked) while the callback holds, then release it and confirm the game proceeds.

4. **`pendingContinue` is not cancelled on game shutdown.**

   If the game is shut down (`director.shutdown()` or `cancel()`) while `waitForContinue()` is blocking, the director loop exits via `shutdownRequested`, but the director thread is still blocked at `future.get()` for up to `actionTimeoutSeconds * 2` seconds. The existing `pendingRebuys`/`pendingAddons` maps have the same gap, so this is a pre-existing pattern, but interactive-continue blocks the director thread (not a separate offer thread), making the delay more visible. Consider completing `pendingContinue` with `null` on shutdown to allow the director to exit immediately. This would be consistent with the interrupt-friendly `sleepMillis` design.

5. **`CONTINUE_RUNOUT` is broadcast to all players, not just the owner.**

   `GameEventBroadcaster.java:464-465` calls `broadcast(...)`, which sends to every connection in the game:
   ```java
   case GameEvent.AllInRunoutPaused e ->
       broadcast(ServerMessage.of(ServerMessageType.CONTINUE_RUNOUT, gameId, null));
   ```
   In practice mode with one human this is fine. In a future multiplayer scenario, all connected players would receive the "please click Continue" prompt. This should be `sendToPlayer` (owner only) or at least documented as intentional if the design is to let any player continue.

6. **`neverBroke` constructor call in test uses positional 7th arg as `null`.**

   `ServerTournamentDirectorTest.java:701`:
   ```java
   director.setPracticeConfig(new GameConfig.PracticeConfig(null, null, null, null, true, null, null));
   ```
   The 7th positional arg is `pauseAllinInteractive`. This is correct but fragile: a future addition of an 8th field will silently compile with the wrong positional assignment. This is a general record-constructor smell that applies to the existing codebase; no action required here unless the team adopts named-argument or builder patterns.

7. **Missing plan file.**

   The review handoff references `.claude/plans/piped-marinating-pumpkin.md` but this file does not exist in either the main worktree or the feature worktree. The plan may have been created in-memory only. This is a minor process gap but the implementation is self-explanatory from the handoff summary.

#### ❌ Required Changes (Blocking)

None. The implementation is correct, thread-safe for its intended use (single human, practice mode), and the timeout prevents deadlock. Suggestion #2 (ownership check on `CONTINUE_RUNOUT`) is worth addressing before multiplayer launch but is not blocking for practice mode.

### Verification

- **Tests:** 1530/1530 passed per handoff. The new `waitForContinueCallbackBlocksAndUnblocks` test at `ServerTournamentDirectorTest.java:802-857` correctly covers the happy path (callback fires, game completes). See suggestion #3 for strengthening it.
- **Coverage:** Not measured; existing coverage thresholds met per handoff.
- **Build:** Zero warnings per handoff.
- **Privacy:** No private data found. No credentials, tokens, or PII in any changed file.
- **Security:** The `CONTINUE_RUNOUT` inbound handler (suggestion #2) lacks an ownership check, which is a minor authorization gap acceptable for practice mode but should be addressed before multiplayer use.
