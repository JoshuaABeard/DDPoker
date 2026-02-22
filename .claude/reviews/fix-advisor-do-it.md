# Review Request

**Branch:** fix-advisor-do-it
**Worktree:** ../DDPoker-fix-advisor-do-it
**Plan:** N/A (small targeted fix)
**Requested:** 2026-02-22 18:00

## Summary

The advisor "Do It" button did nothing in WebSocket/online mode. In that mode no `Bet` phase is ever started, so `context_.getCurrentPhase() instanceof Bet` always evaluated to false and the handler exited silently. The fix adds a fallback path that routes the AI action through `game_.playerActionPerformed()`, which dispatches to the `PlayerActionListener` registered by `WebSocketTournamentDirector`. A `toPokerGameAction()` helper maps `HandAction.ACTION_*` constants to `PokerGame.ACTION_*` constants.

## Files Changed

- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/dashboard/DashboardAdvisor.java` - Added WebSocket fallback in "Do It" button listener; added `toPokerGameAction()` helper.

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** Build has pre-existing compilation errors in `Lobby.java`/`GameServerRestClient.java` from other in-progress work; `DashboardAdvisor.java` itself produces no errors.
- **Coverage:** N/A (no new test class; this is UI event-handler code not unit-testable without full game context)
- **Build:** DashboardAdvisor compiles clean; other errors are pre-existing

## Context & Decisions

**Root cause:** `WebSocketTournamentDirector` bypasses the `Bet` phase entirely — it sets input mode directly and registers a `PlayerActionListener` lambda. The original "Do It" handler only checked for a `Bet` phase, which never exists in WS mode.

**Mapping `HandAction` → `PokerGame` action constants:** The two constant sets use different numbering (e.g., `HandAction.ACTION_FOLD=0` vs `PokerGame.ACTION_FOLD=1`). A switch expression maps them. `ACTION_CHECK_RAISE` is mapped to `PokerGame.ACTION_CHECK` since the AI doesn't produce it but it's covered defensively.

**Amount semantics:** The AI (`PokerAI.getHandAction()`) returns raise-by amounts (not raise-to), matching what the UI spinner passes to `playerActionPerformed()`. Call amounts from the AI are ignored by the server anyway.

**No change to practice mode path:** `Bet.doAI()` continues to handle practice mode unchanged.

---

## Review Results

**Status: APPROVED**

**Reviewed by:** Claude Sonnet 4.6
**Date:** 2026-02-22

### Findings

#### ✅ Strengths

**Root cause correctly diagnosed.** `WebSocketTournamentDirector` never starts a `Bet` phase — it sets input mode directly and registers a `PlayerActionListener` lambda. The original guard `phase instanceof Bet` was the right check for practice mode and a guaranteed dead end in WS mode. The fix correctly adds an `else if` branch gated on `game_.getPlayerActionListener() != null`, which is non-null only in WS mode (set in `WebSocketTournamentDirector.start()`, cleared in `finish()`).

**Dispatch path is correct.** `game_.playerActionPerformed(action, amount)` delegates to the registered `PlayerActionListener`, which in WS mode is the lambda in `WebSocketTournamentDirector.start()` (line 136). That lambda calls `mapPokerGameActionToWsString(action)` and `resolveActionAmount(action, amount)` and sends the result over the wire. This is exactly the same path the normal UI buttons take.

**`toPokerGameAction()` mapping is complete and correct.** All five actionable constants that `PokerAI.getHandAction()` can produce (`ACTION_FOLD`, `ACTION_CHECK`, `ACTION_CALL`, `ACTION_BET`, `ACTION_RAISE`) map to the correct `PokerGame.ACTION_*` values. `ACTION_CHECK_RAISE` (which the AI cannot actually produce) defensively maps to `ACTION_CHECK`. The default falls back to `ACTION_FOLD`, consistent with the analogous `mapActionToWsString` and `mapPokerGameActionToWsString` defaults in `WebSocketTournamentDirector`.

**Amount semantics are correct.** `PokerAI.getHandAction()` returns raise-by amounts for RAISE (`amount - call` at line 266 of `PokerAI.java`) and a direct bet amount for BET. `ServerHand.java` (lines 1059–1068) treats `action.amount()` as the raise-by increment that is subtracted from the player's chips and accumulated in `playerBets`. `validateAction()` clamps the amount to `[minRaise, maxRaise]`. The AI raise-by amount is within what the server expects and worst-case is clamped server-side, not rejected.

**Safety guards are appropriate.** The code checks three conditions before dispatching: `hh != null`, `pp != null`, `pp.isHumanControlled()`, and `pp.getPokerAI() != null`. The advisor panel already guarantees these (see `updateInfo()` conditions) but the explicit null checks are still sound defensive programming for a UI event handler.

**No double-action risk.** The `WebSocketTournamentDirector` lambda calls `game_.setInputMode(MODE_QUITSAVE)` as its first action, which hides the action buttons before sending to the wire. A second click after the first arrives will find the buttons hidden. This is the same behaviour as normal UI button presses.

**Surgical change.** Only `DashboardAdvisor.java` is touched. The practice mode path via `Bet.doAI()` is completely unchanged. `toPokerGameAction()` is `private static`, used only in this one call site, so there is no risk of unintended callers.

**Copyright correct.** Minor bug fix, so original copyright (Template 1) is preserved as specified in the licensing guide.

#### ⚠️ Suggestions (Non-blocking)

**`ACTION_ALL_IN` not in the mapping.** `PokerAI.getHandAction()` cannot return `HandAction.ACTION_ALL_IN` (it returns `ACTION_RAISE` with a capped amount instead), so there is no reachable code path through the default-to-fold case. That said, if a future AI implementation ever returns `ACTION_ALL_IN` the fix would silently fold the player's hand. Adding a comment on the `default` arm noting why `ACTION_ALL_IN` is absent would prevent future confusion. This is not blocking — the current behaviour is safe and consistent with the `mapActionToWsString` default.

**No null guard on `pp.getPokerAI()` before `pp.getAction(false)`.** The condition `pp.getPokerAI() != null` is checked, but if an AI throws inside `getHandAction()`, `PokerPlayer.getAction()` catches it and returns a synthetic FOLD action. This means a broken AI quietly folds the player's hand when "Do It" is clicked. This is the same behaviour as in `Bet.doAI()`, so it is consistent and not a regression introduced here.

#### ❌ Required Changes (Blocking)

None.

### Verification

- **Tests:** No new test class. This is a Swing event-handler that requires a live WebSocket game session. The handoff notes pre-existing build errors in `Lobby.java`/`GameServerRestClient.java` from other in-progress work; `DashboardAdvisor.java` itself compiles cleanly.
- **Coverage:** N/A — UI event-handler code not unit-testable without a full game context. Existing coverage thresholds are unaffected.
- **Build:** `DashboardAdvisor.java` produces no errors. Other errors are pre-existing and unrelated.
- **Privacy:** SAFE — no private information, credentials, or personal data in the changed file.
- **Security:** No security concerns. The fix routes through the same action path as normal UI buttons; no new surfaces are exposed.
