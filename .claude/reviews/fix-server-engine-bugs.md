## Review Request

**Branch:** fix-server-engine-bugs
**Worktree:** ../DDPoker-fix-server-engine-bugs
**Plan:** .claude/plans/snug-enchanting-reddy.md
**Requested:** 2026-02-20 00:00

## Summary

Fixed 9 server-side game engine bugs ranging from a critical RAISE betting infinite loop (Fix 1)
to minor empty player name fields in WebSocket broadcasts (Fix 9). The most critical bug caused
`currentBet` to be set to the addend rather than the running total, breaking `isPotGood()` and
re-prompting players indefinitely. A secondary high-severity bug in `initPlayerIndex()` had an
unreachable break guard that could loop forever when all players are all-in.

## Files Changed

- [ ] `code/pokergamecore/src/main/java/.../TournamentEngine.java` — Fix 4: sitting-out player now auto-folds via `processPlayerAction` instead of no-op `setSittingOut(true)`
- [ ] `code/pokergamecore/src/test/java/.../TournamentEngineTest.java` — Fix 4: added `lastAction` tracking to `StubGameHand`, updated sitting-out test to assert FOLD action
- [ ] `code/pokergameserver/src/main/java/.../GameInstance.java` — Fix 7: added `toIntId()` helper using `Math.toIntExact`, replaced all 5 `(int)` cast sites
- [ ] `code/pokergameserver/src/main/java/.../GameInstanceManager.java` — Fix 3: added `synchronized` to `createGame()` to eliminate TOCTOU race
- [ ] `code/pokergameserver/src/main/java/.../ServerGameTable.java` — Fix 5: `isCurrent()` returns `true` (was `false`)
- [ ] `code/pokergameserver/src/main/java/.../ServerHand.java` — Fix 1+2: BET/RAISE now use running total for `currentBet`; `initPlayerIndex()` replaced with direct scan loop
- [ ] `code/pokergameserver/src/main/java/.../ServerTournamentDirector.java` — Fix 6: added `addonLevel >= 0` guard to prevent premature addon offers
- [ ] `code/pokergameserver/src/main/java/.../websocket/GameEventBroadcaster.java` — Fix 8+9: added tableId bounds check in `PlayerActed` case; populated `playerName` from seat lookup; `ShowdownPlayerData` now uses `sp.getName()` instead of `""`
- [ ] `code/pokergameserver/src/main/java/.../websocket/InboundMessageRouter.java` — Fix 7: replaced two `(int)` casts with `Math.toIntExact()`
- [ ] `code/pokergameserver/src/test/java/.../ServerGameTableTest.java` — Fix 5: updated `testIsCurrent` to assert `true`
- [ ] `code/pokergameserver/src/test/java/.../ServerHandTest.java` — Fix 1+2: added `testRaise_SBRaisesPreflop_BettingCompletes` and `testInitPlayerIndex_AllPlayersAllIn_SetsNoCurrentPlayer`
- [ ] `code/pokergameserver/src/test/java/.../websocket/GameEventBroadcasterTest.java` — Fix 9: added `playerActed_withGameReference_populatesPlayerName` test

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** All passed — `mvn test -P dev` BUILD SUCCESS (12 files changed, 140 insertions, 26 deletions)
- **Coverage:** Not measured separately (dev profile skips coverage)
- **Build:** Clean — no warnings introduced

## Context & Decisions

**Fix 1 (Critical):** The root cause was that `currentBet` was assigned the *addend* (chips added
this action) rather than the player's *running total* in `playerBets`. When a player (e.g. SB) had
already posted a blind and then raised, `currentBet` ended up less than the raiser's total bet,
making `isPotGood()` incorrectly compute `amountToCall > 0` for all remaining players even after
they called, causing them to be re-prompted indefinitely.

**Fix 2:** `initPlayerIndex()` delegated to `playerActed(-1)`, whose break guard
`index == startIndex` was `index == -1`, but `index` only ranges 0..(size-1) — so the guard was
never reachable. Replaced with a simple direct scan. The `playerActed(int)` method itself is
unchanged (still used for mid-round advancement).

**Fix 4:** The sitting-out handler called `setSittingOut(true)` (a no-op since the player was
already sitting out) instead of actually folding the player. This left the player active in the
betting round, causing `isPotGood()` to never resolve.

**Fix 5:** The existing test (`testIsCurrent`) asserted `false`, which was consistent with the
broken implementation. The test was updated alongside the fix to assert `true`.

**Fix 7:** Used `Math.toIntExact()` rather than a custom helper or a simple `(int)` cast, per the
plan's guidance. All 7 cast sites across `GameInstance.java` (5) and `InboundMessageRouter.java`
(2) were updated.

---

## Review Results

**Status:** APPROVED WITH SUGGESTIONS

**Reviewed by:** Claude Opus 4.6 (review agent)
**Date:** 2026-02-21

### Findings

#### Strengths

1. **Fix 1 (currentBet running total) is correct and well-tested.** The root cause analysis is accurate: `currentBet = actualBet` stored the addend, not the running total. The fix (`currentBet = newBetTotal` / `currentBet = newRaiseTotal`) correctly sets `currentBet` to the player's cumulative bet for the round, which is exactly what `isPotGood()` compares against via `playerBets.getOrDefault(...)`. The test (`testRaise_SBRaisesPreflop_BettingCompletes`) validates the exact scenario: SB posts 50, raises 150 more (total 200), BB calls 100 more (total 200), pot = 400, hand done.

2. **Fix 2 (initPlayerIndex rewrite) is correct.** The old code delegated to `playerActed(-1)`, whose `index == startIndex` guard would check `index == -1`, but `index` only ever ranges 0..size-1 after the `index++; if (index >= size) index = 0` logic. The guard was unreachable, creating an infinite loop when all players are folded/all-in. The replacement is a clean linear scan that correctly handles empty `playerOrder`, all-folded, and all-all-in cases. The `isDone()` early-return is a good belt-and-suspenders guard.

3. **Fix 3 (synchronized createGame) addresses the TOCTOU race.** The method reads `games.size()`, checks per-user count, then `games.put()`. Without synchronization, two concurrent calls could both pass the limit check. `synchronized` on the method is appropriate here since `createGame` is not a hot path.

4. **Fix 4 (sitting-out auto-fold) is correct.** `processPlayerAction` calls `hand.applyPlayerAction()` which sets `sp.setFolded(true)` and calls `playerActed()` to advance. This properly removes the sitting-out player from the active betting round. The test correctly verifies a FOLD action is applied.

5. **Fix 5 (isCurrent returns true) is a trivial one-liner.** The rationale (server tables are always conceptually current) is sound. Test updated to match.

6. **Fix 6 (addonLevel guard) is a correct defensive improvement.** `addonLevel` defaults to `-1` (line 75 of `ServerTournamentContext.java`). While `getLevel()` normally starts at 0 (so `0 == -1` would already be false), the `>= 0` guard protects against edge cases where both values could be -1 (e.g., uninitialized tournament state) and clearly communicates the intent that -1 means "no addon level configured."

7. **Fix 7 (Math.toIntExact) covers all sites.** 5 sites in `GameInstance.java` and 2 in `InboundMessageRouter.java` = 7 total. The `toIntId()` helper in `GameInstance` avoids repetition. `InboundMessageRouter` uses inline `Math.toIntExact()` since there are only 2 call sites -- reasonable.

8. **Fix 8 (tableId bounds check) is correct.** The guard `e.tableId() >= 0 && e.tableId() < game.getTournament().getNumTables()` prevents `ArrayIndexOutOfBoundsException` when `PlayerActed` events arrive with stale table IDs.

9. **Fix 9 (player name population) is correct.** Both `PlayerActed` and `ShowdownPlayerData` now use `sp.getName()` instead of `""`. Test validates name propagation end-to-end using a real `ServerPlayer` and `ServerGameTable`.

#### Suggestions (Non-blocking)

1. **Fix 1 test assertion could be stricter.** Line 517 uses `assertTrue(hand.isDone() || hand.getCurrentPlayerInitIndex() < 0, ...)` which is a disjunction. After BB calls a completed pot, `isDone()` should return `true` unconditionally. Consider asserting just `assertTrue(hand.isDone())` for a more precise test. The current assertion would pass even if `isDone()` were still broken as long as `currentPlayerIndex` happened to be -1.

2. **Fix 3 synchronization scope.** `synchronized` on the method locks on `this` (the `GameInstanceManager` instance). Other methods that read/write `games` (e.g., `getGame`, `removeGame`) are not synchronized, which means concurrent reads during `createGame` could still see inconsistent state. Since `games` is a `ConcurrentHashMap`, individual operations are thread-safe, but the compound check-then-act in `createGame` is the real concern. This is fine for now, but if other methods gain similar compound logic, a `ReadWriteLock` or consistent synchronization would be needed.

3. **Reconnect-path broadcaster (non-blocking context).** The reconnect path in `GameWebSocketHandler` uses the no-game constructor by design — the reconnect broadcaster is temporary and only active until the game broadcaster takes over. This is correct existing behavior; no change was made to this file in the branch.

#### Required Changes (Blocking)

*None.* The two blocking issues originally identified by the review agent (`WebSocketTournamentDirector.java` and `GameWebSocketHandler.java`) were false positives. `git diff main...HEAD --name-only` confirms neither file was changed in this branch. The review agent compared against the wrong baseline. All 12 changed files exactly match the plan's "Files Changed" list.

### Verification

- **Tests:** `mvn test -P dev` BUILD SUCCESS -- all tests pass across all modules (verified by reviewer).
- **Coverage:** Not measured (dev profile skips coverage). The new tests for Fix 1 and Fix 2 cover the critical scenarios. Fix 4 test is minimal but sufficient. Fix 9 test is good.
- **Build:** Clean build, no warnings introduced.
- **Privacy:** No private information found in any changed file. SAFE.
- **Security:** Fix 7 (`Math.toIntExact`) correctly throws `ArithmeticException` on overflow rather than silently truncating, which is the right security posture for profile ID conversion. Fix 3 (`synchronized`) addresses a concurrency safety issue. No new attack surfaces introduced.
