# Review Request

**Branch:** fix-elimination-visual
**Worktree:** ../DDPoker-fix-elimination
**Plan:** none (small targeted fix)
**Requested:** 2026-02-21

## Summary

When a player is eliminated in WebSocket practice mode, their hole cards, "all-in"
result overlay, and dealer-button position persisted visually across hands. Root cause:
`ShowTournamentTable.tableEventOccurred()` clears these via `TYPE_CLEANING_DONE` between
hands, but that event is never fired in WebSocket mode because the game engine lives on
the server. Fix: fire `TYPE_CLEANING_DONE` from `onHandStarted()` on the EDT before new
hand setup, reusing the existing cleanup logic in `ShowTournamentTable`.

## Files Changed

- [ ] `code/poker/src/main/java/.../online/WebSocketTournamentDirector.java` — fire `TYPE_CLEANING_DONE` at the start of the `onHandStarted()` EDT block, before the new hand is set up
- [ ] `code/poker/src/test/java/.../online/WebSocketTournamentDirectorTest.java` — new test `handStartedFiresCleaningDoneToResetVisualState` verifying `TYPE_CLEANING_DONE` is in the event list after `HAND_STARTED`

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** 55/55 passed (poker module alone, `-pl poker -P dev`); 1 pre-existing flaky failure in the full parallel build (`actionTimeoutFiresPlayerActionEvent` — same failure on main)
- **Coverage:** Not checked (Swing rendering path; unit-testable event dispatch covered by new test)
- **Build:** Clean

## Context & Decisions

**Root cause analysis:**
- `ShowTournamentTable.tableEventOccurred()` handles `TYPE_CLEANING_DONE` by calling
  `PokerUtils.clearCards(false)` (removes all `CardPiece` objects from territories),
  `PokerUtils.clearResults(context_, false)` (hides `ResultsPiece` overlays), and
  `GuiUtils.invoke(new SwingIt(SWING_POT_DISPLAY, true))` (repaints pot empty).
- In local engine mode this fires via `SwingEventBus.convertToLegacy()` when the
  engine transitions to `STATE_CLEAN` between hands.
- In WebSocket mode the engine is on the server. The client never receives
  `GameEvent.CleaningDone`, so `TYPE_CLEANING_DONE` is never dispatched on the client.
- `onHandStarted()` fires `TYPE_NEW_HAND` and `TYPE_DEALER_ACTION` but not
  `TYPE_CLEANING_DONE`, so stale visual pieces from prior hands accumulate.

**Why this fix is safe:**
- `TYPE_CLEANING_DONE` fires synchronously on the EDT (same thread as `onHandStarted`'s
  `invokeLater` block), so the cleanup completes before the new hand is set up.
- `table.setHoldemHand(null)` (called by the `CLEANING_DONE` handler) causes
  `table.getRemoteHand()` to return null, so `onHandStarted` creates a fresh
  `RemoteHoldemHand` — same as the path it takes on the very first hand.
- `PokerUtils.setNewHand()` resets display state; safe to call between any two hands.
- `clearCards()` removes old `CardPiece` objects from all territories; they are
  re-added by the subsequent `TYPE_DEALER_ACTION` → `SWING_SYNC` path for the new hand.

**Pre-existing flaky test:**
`actionTimeoutFiresPlayerActionEvent` fails only in the multi-module parallel build
(`mvn test -P dev`) due to shared Swing/PropertyConfig state across concurrent JVMs.
It passes in the isolated `poker`-module run and was already failing on `main` before
this change. Not caused by or related to this fix.

---

## Review Results

**Status:** APPROVED WITH SUGGESTIONS

**Reviewed by:** Claude Opus 4.6 (review agent)
**Date:** 2026-02-21

### Findings

#### Strengths

1. **Correct root cause analysis.** The diagnosis is accurate: `TYPE_CLEANING_DONE` is never fired in WebSocket mode because the server owns the game engine and the client never receives `GameEvent.CleaningDone`. Firing it from `onHandStarted()` is the right place to inject the missing lifecycle event.

2. **Minimal, surgical fix.** The production change is a single line of code (plus a well-written comment). It reuses the existing `ShowTournamentTable.TYPE_CLEANING_DONE` handler rather than duplicating cleanup logic in the WebSocket path. This is exactly the right approach.

3. **Safe event ordering.** The `firePokerTableEvent` call at line 470 of `WebSocketTournamentDirector.java` dispatches synchronously on the EDT (since the entire `invokeLater` block is already on the EDT). This means cleanup completes before the subsequent hand setup (lines 474-495). The ordering is: CLEANING_DONE -> hand setup -> BUTTON_MOVED -> NEW_HAND -> DEALER_ACTION. This mirrors the local-engine lifecycle correctly.

4. **Edge cases are safe.** On the very first hand, there is no prior visual state to clear. `PokerUtils.clearCards(false)` iterates territories and removes card pieces (no-op if none exist). `PokerUtils.clearResults(context_, false)` iterates territories to hide result pieces (no-op if none visible). `PokerUtils.setNewHand()` resets two static booleans. All are idempotent and safe to call unconditionally.

5. **Good comment quality.** The 4-line comment at lines 466-469 explains both the "what" (clear visual state) and the "why" (TYPE_CLEANING_DONE is not fired by the server). This will help future maintainers understand why this event is fired here rather than relying on the server lifecycle.

#### Suggestions (Non-blocking)

1. **Handoff document contains an inaccurate claim (lines 48-50).** The "Why this fix is safe" section states: "`table.setHoldemHand(null)` (called by the `CLEANING_DONE` handler) causes `table.getRemoteHand()` to return null, so `onHandStarted` creates a fresh `RemoteHoldemHand`." This is incorrect. `ShowTournamentTable` line 943 calls `table.setHoldemHand(null)`, which sets `hhand_` (the parent `PokerTable` field) to null. However, `RemotePokerTable.getHoldemHand()` (line 74) returns `remoteHand_`, not `hhand_`, and `RemotePokerTable` does not override `setHoldemHand()`. Therefore, `setHoldemHand(null)` does NOT affect `getRemoteHand()`. The existing `RemoteHoldemHand` survives the CLEANING_DONE cleanup and is correctly reused at line 474 of `onHandStarted()`. **The fix still works correctly** -- the visual cleanup (clearCards, clearResults, pot repaint) proceeds as intended, and the data model hand is preserved. But the handoff document should be corrected for accuracy. This is documentation-only; no code change needed.

2. **Test does not verify event ordering.** The test `handStartedFiresCleaningDoneToResetVisualState` (line 187-198 of the test file) asserts `events.contains(TYPE_CLEANING_DONE)` but does not verify that CLEANING_DONE fires *before* TYPE_NEW_HAND and TYPE_BUTTON_MOVED. An ordering assertion such as `assertThat(events.indexOf(TYPE_CLEANING_DONE)).isLessThan(events.indexOf(TYPE_NEW_HAND))` would make the contract stronger. The current test is adequate since the production code clearly sequences the calls, but an ordering assertion would guard against future rearrangement.

3. **Existing test `handStartedFiresNewHandAndButtonMovedEvents` could be combined.** The new CLEANING_DONE test (lines 187-198) and the existing BUTTON_MOVED/NEW_HAND test (lines 175-184) follow identical setup patterns and could be a single test verifying all three events plus ordering. Not required -- separate tests are fine for clarity -- but worth noting if test count ever becomes a maintenance concern.

#### Required Changes (Blocking)

None.

### Verification

- **Tests:** 55/55 passed in poker module (confirmed by handoff). New test `handStartedFiresCleaningDoneToResetVisualState` covers the added behavior. Pre-existing flaky `actionTimeoutFiresPlayerActionEvent` is unrelated (confirmed on main).
- **Coverage:** Visual cleanup path (`ShowTournamentTable.tableEventOccurred` CLEANING_DONE case) is Swing rendering code not amenable to unit testing. Event dispatch is covered by the new test. Acceptable.
- **Build:** Clean (confirmed by handoff).
- **Privacy:** No private information in the diff. SAFE.
- **Security:** No security-relevant changes. SAFE.
