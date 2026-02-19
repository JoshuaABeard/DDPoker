# Review Request

**Branch:** fix-hand-racing
**Worktree:** ../DDPoker-fix-hand-racing
**Plan:** N/A (targeted bug fix)
**Requested:** 2026-02-19 17:05

## Summary

Fixes the hand-racing bug where all-AI hands after human elimination ran at millisecond
speed. The root cause: the previous fix hooked `nextState==CLEAN` and `TD.CheckEndHand`,
both of which are dead code when `isAutoDeal()=true` (all online/embedded practice games).
The correct hook is `nextState==BEGIN`, which `handleDone()` always returns after every
showdown, regardless of the auto-deal path. Also fixes `StateHandler.deriveGamePhase()`
to prefer `game.getCurrentTable()` over `tables.get(0)`, which could return a stale
loaded game causing the API to always show `BETWEEN_HANDS`.

## Files Changed

- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerTournamentDirector.java`
  - Moved `incrementHandsPlayed()` + `sleepMillis(aiActionDelayMs)` from `nextState==CLEAN`
    check to `nextState==BEGIN` check in `applyResult()`. Removed the `TD.CheckEndHand`
    phase check added in commit `86ec69e0` (that check also never fired for auto-deal games).
- [ ] `code/poker/src/dev/java/com/donohoedigital/games/poker/control/StateHandler.java`
  - `deriveGamePhase()`: use `game.getCurrentTable()` with fallback to `tables.get(0)`,
    so the API correctly shows the active practice game's phase when an old loaded game
    also exists in the context.

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** All passed (`mvn test -P dev -pl pokergameserver,poker -am`)
- **Build:** Clean
- **Manual test:** Console log confirmed HAND_STARTED events at exactly ~400ms intervals
  after the fix (vs millisecond-level racing before). Game ran from 6 players to human
  winning all 9000 chips successfully. No crashes or hangs.

## Context & Decisions

### Why `nextState==BEGIN` is the correct hook

The state machine path for auto-deal online games (embedded practice):
1. `DONE` → `handleDone()` returns `nextState(BEGIN)` ← **the only universal hook**
2. `BEGIN` → `handleBegin()` with `isAutoDeal()=true` → `nextState(START_HAND)` (skips WaitForDeal + CheckEndHand)
3. `START_HAND` → `handleStartHand()` → `phaseToRun("TD.DealDisplayHand")` → new hand

The path for manual-deal games (offline/testing):
1. `DONE` → `handleDone()` returns `nextState(BEGIN)` ← **same hook, still fires**
2. `BEGIN` → `handleBegin()` with `isAutoDeal()=false` → `phaseToRun("TD.WaitForDeal")`
3. `WaitForDeal` → user clicks Deal button → `CHECK_END_HAND` → `CLEAN` ← dead for auto-deal

`nextState==CLEAN` was dead because CLEAN is reached via `pendingState`, never `nextState`.
`TD.CheckEndHand` was dead because `handleBegin()` skips it when `isAutoDeal()=true`.

### aiActionDelayMs configuration
The embedded server uses `application-embedded.properties` which sets
`game.server.ai-action-delay-ms=400`. This 400ms pause per hand was confirmed by
observing HAND_STARTED events at exactly ~400ms intervals in the WebSocket debug log.

---

## Review Results

**Status:** APPROVED_WITH_SUGGESTIONS

**Reviewed by:** Claude Opus 4.6 (review agent)
**Date:** 2026-02-19

### Findings

#### Strengths

1. **Root cause analysis is correct and thorough.** The handoff document and code comments
   accurately explain why `nextState==CLEAN` was dead code (CLEAN is only reached via
   `pendingState` from `handleCheckEndHand`, never via `nextState`) and why `TD.CheckEndHand`
   was dead for auto-deal games (`handleBegin(isAutoDeal=true)` skips directly to
   `START_HAND`). The fix targets the right state transition.

2. **`nextState==BEGIN` is the correct hook.** Verified by tracing `TournamentEngine`:
   - `handleDone()` (line 784) unconditionally returns `nextState(BEGIN)` -- this is the
     only post-showdown path.
   - `handleDealForButton()` (line 277) uses `pendingState(BEGIN)`, not `nextState(BEGIN)`,
     so it does NOT trigger the hook in `applyResult()`. This is correct because deal-for-button
     happens once at game start, not after a hand.
   - `handleOnHold()` (line 251) uses `nextState(BEGIN)` -- see Issues below.

3. **StateHandler fix is correct.** Using `game.getCurrentTable()` with fallback to
   `tables.get(0)` correctly resolves the stale-table problem. The null check is proper
   since `getCurrentTable()` returns a nullable field (`currentTable_`). The existing code
   at line 200 already uses `game.getCurrentTable()` in `buildCurrentAction()`, so this
   change makes `deriveGamePhase()` consistent.

4. **Comments are excellent.** The 5-line block comment in `applyResult()` (lines 198-202)
   concisely explains WHY this hook point was chosen and what alternative hooks are dead.
   Future developers will not repeat the `nextState==CLEAN` mistake.

5. **Surgical change.** Only the lines that needed changing were modified -- no formatting
   changes, no unrelated refactoring. The net diff is +6/-7 lines in production code.

6. **Learnings documented.** The `.claude/learnings.md` additions capture the state machine
   gotchas so future sessions avoid the same dead-code trap.

#### Issues

1. **`handleOnHold()` also transitions via `nextState(BEGIN)` (minor, low risk).**
   `TournamentEngine.handleOnHold()` at line 251 returns `nextState(TableState.BEGIN)` when
   a table goes from ON_HOLD to BEGIN (multi-table consolidation scenario). This would
   trigger both `incrementHandsPlayed()` and the 400ms sleep even though no hand was completed.
   However, this is low risk because:
   - `ServerTournamentDirector` never sets any table to `ON_HOLD` -- it has its own
     consolidation logic in `consolidateTable()` that marks source tables as `GAME_OVER`.
   - The `ON_HOLD` state is legacy infrastructure from the Swing `TournamentDirector`.
   - Grep confirms zero references to `ON_HOLD` in the entire `pokergameserver` module.

   **Verdict:** Not blocking. The ON_HOLD -> BEGIN path is currently unreachable in the
   server. If multi-table support is added later, this would need revisiting, but the
   learnings doc should capture this for future reference.

#### Required Changes

None.

#### Suggestions

1. **Consider adding a note to learnings.md about the ON_HOLD edge case.**
   Something like: "[server] `handleOnHold()` also returns `nextState(BEGIN)` -- currently
   unreachable because ServerTournamentDirector never uses ON_HOLD, but would cause a
   spurious `incrementHandsPlayed()` + sleep if multi-table ON_HOLD support were added."
   This would help future developers who might add multi-table support.

2. **The comment says "DONE->BEGIN transition" but does not guard against other sources of
   `nextState(BEGIN)`.** A slightly more defensive approach would be to track the previous
   table state and check `previousState == DONE && nextState == BEGIN`. However, since the
   only reachable `nextState(BEGIN)` source is `handleDone()`, this is not required -- it
   would be over-engineering for a currently-unreachable edge case. Noted for awareness only.
