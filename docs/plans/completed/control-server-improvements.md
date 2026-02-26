# Plan: Control Server Improvements & Java Bug Fixes

**Status:** COMPLETED
**Created:** 2026-02-23
**Goal:** Fix Java client bugs uncovered by the scenario test suite and expand the dev control server API to enable comprehensive automated testing.

---

## Background

The scenario test suite (44 scripts) identified several cases where tests are softened to WARN instead of FAIL because the underlying Java code has bugs, or the control server lacks the API surface needed to observe/drive the behavior. This plan fixes the bugs and adds the missing API — then the tests can be hardened to assert the correct behavior.

---

## Part A: Java Client Bug Fixes

### A1 — Card.getCard() returns BLANK for invalid input (not null)

**File:** `code/pokerengine/src/main/java/com/donohoedigital/games/poker/engine/Card.java`

**Bug:** `Card.getCard(String)` returns `Card.BLANK` (index 52) for any string with an invalid rank or suit character (e.g. "Zz"). The `SimulatorHandler` checks `if (c == null)` to detect invalid cards — this check never fires. Invalid cards silently pass into the simulation as blank cards.

**Fix:** In `Card.getCard(String s)`, after computing rank and suit, if either is `UNKNOWN`/`UNKNOWN_RANK`, return `null`. Preserve the existing behavior for genuinely blank/null input if any callers depend on it — grep for all callers first.

**Test impact:** `test-simulator.sh` S-007 currently logs WARN ("invalid card response: completed=true"). After fix it must return 400 BadRequest and the test assertion should be hardened to FAIL.

---

### A2 — ShowTournamentTable.setInputMode(MODE_REBUY_CHECK) bypasses super

**File:** `code/poker/src/main/java/com/donohoedigital/games/poker/ShowTournamentTable.java`

**Bug:** Around line 1028, `setInputMode` returns early for `MODE_REBUY_CHECK` without calling `super.setInputMode()`. As a result `nInputMode_` is never set to `MODE_REBUY_CHECK`, so `StateHandler` can never report this mode in `/state`. Tests can't observe the rebuy dialog or send a DECLINE_REBUY action to advance the game.

**Fix:** Restructure so that `super.setInputMode(nMode)` is called for all modes including `MODE_REBUY_CHECK`, but the rebuy-specific UI (setRebuyButton etc.) still runs. The early return must be removed or replaced with a conditional after the super call.

**Caution:** Verify that calling super for REBUY_CHECK doesn't break the existing UI flow. Run `mvn test -P dev` after the change. If any test fails, investigate before proceeding.

**Test impact:** `test-rebuy-addon.sh` currently can't verify REBUY_CHECK mode. After fix, a new `test-rebuy-dialog.sh` can start a game with rebuys, go broke, assert `inputMode == "REBUY_CHECK"` in `/state`, send `DECLINE_REBUY`, and assert game continues.

---

### A3 — StateHandler: missing handNumber field

**File:** `code/poker/src/dev/java/com/donohoedigital/games/poker/control/StateHandler.java`

**Bug:** `/state` has no field for the current hand number (count of hands played since game start). `test-hand-history.sh` can't verify that playing N hands produces N history entries because it has no way to count hands precisely.

**Fix:** In `buildState()` or `buildTournamentInfo()`, add `handNumber` using the game's hand counter. Find the field on `HoldemHand` or `PokerGame` that tracks total hands dealt. It is likely `game.getHandNum()` or similar — grep for hand number/count in the game model.

**Test impact:** `test-hand-history.sh` and `test-game-info-data.sh` can assert a specific hand number.

---

### A4 — StateHandler: aifaceup cheat not reflected in opponent hole cards

**File:** `code/poker/src/dev/java/com/donohoedigital/games/poker/control/StateHandler.java`

**Bug:** `buildPlayerInfo()` explicitly hides AI hole cards regardless of the `OPTION_CHEAT_AIFACEUP` preference. When this cheat is enabled, the test has no way to verify the game is actually showing AI cards.

**Fix:** In `buildPlayerInfo()`, when `PokerUtils.isOptionOn(PokerConstants.OPTION_CHEAT_AIFACEUP)` is true and a hand is in progress (hand is not null), include opponent hole cards in the player state (same card format as the human player's cards). When aifaceup is false, AI cards remain hidden (current behavior).

**Test impact:** `test-cheats-toggle.sh` `cheat.aifaceup` check can be hardened from WARN to FAIL.

---

### A5 — StateHandler: pause-allin state not observable

**File:** `code/poker/src/dev/java/com/donohoedigital/games/poker/control/StateHandler.java`

**Bug:** When the game pauses because an AI player went all-in and `pauseAllin` is enabled, the `/state` `inputMode` is not a distinct observable value. The test can't wait for this state or send CONTINUE to advance past it.

**Investigation needed:** Determine what `inputMode` the game enters during a pause-allin (likely `QUITSAVE` or stays at the previous mode). Identify what triggers the pause and how it maps to game input mode. Then surface a distinct `inputMode: "PAUSE_ALLIN"` when this pause is active.

**How to find:** Search for `MODE_PAUSE_ALLIN` or `PAUSE_ALLIN` in the codebase; trace what `setInputMode` call is made when the pause fires.

**Test impact:** `test-pause-allin.sh` can be a real test: wait for PAUSE_ALLIN mode, assert it appears, send CONTINUE, assert game resumes.

---

## Part B: Control Server API Additions

### B1 — /game/start: disableAutoDeal parameter

**File:** `code/poker/src/dev/java/com/donohoedigital/games/poker/control/GameStartHandler.java`

**Gap:** The embedded game auto-deals immediately after each hand ends, so DEAL mode never appears. The D-key (deal card shortcut) test permanently skips because there's never a DEAL state to test against.

**Fix:** Add optional boolean parameter `"disableAutoDeal": true` to the `/game/start` request body. When true, configure the game so it does NOT auto-deal — it waits for an explicit DEAL action between hands. How to implement: find where auto-deal is configured (likely in `TournamentProfile` or the game launcher) and expose a toggle.

**Test impact:** `test-keyboard-shortcuts.sh` D-key test can be un-skipped.

---

### B2 — /cheat/complete: force game to end

**File:** `code/poker/src/dev/java/com/donohoedigital/games/poker/control/CheatHandler.java` (new action in existing handler)

**Gap:** There's no way to force a game to complete. `test-hand-history.sh` wants to play a game to completion, then verify the history entry was written. Without this, the test plays 30 hands but the game never ends (players still have chips), so no history is written.

**Fix:** Add `POST /cheat` body `{"action": "completeGame"}` that forces the game to end immediately — eliminate all but one AI player (give all chips to the human or to one AI) so the game ends naturally in one more hand. Alternatively, trigger the game-end phase directly.

**Test impact:** `test-hand-history.sh` can complete a game and assert the history file appears.

---

### B3 — /cheat/advanceClock: fast-forward tournament timer

**File:** `code/poker/src/dev/java/com/donohoedigital/games/poker/control/CheatHandler.java` (new action)

**Gap:** Clock tests wait real time for the level timer to expire. This makes tests slow and timing-sensitive.

**Fix:** Add `POST /cheat` body `{"action": "advanceClock", "seconds": N}` that fast-forwards the tournament clock by N seconds. Find the clock/timer mechanism and subtract N seconds from the remaining time, triggering level advancement if threshold is crossed.

**Test impact:** `test-level-advance.sh`, `test-clock-state.sh`, `test-clock-pause.sh` no longer need real-time waits.

---

### B4 — ValidateHandler: fix multi-table chip conservation baseline

**File:** `code/poker/src/dev/java/com/donohoedigital/games/poker/control/ValidateHandler.java`

**Bug:** ValidateHandler sums chips across all tables (correct), but computes `expectedTotal = buyinPerPlayer × initialPlayerCount`. In multi-table tournaments, players are eliminated over time — once eliminated, their chips are redistributed to remaining players, but `initialPlayerCount × buyinPerPlayer` remains the correct total (chips are conserved, not created or destroyed). The actual issue is likely that `numPlayers` in the game state doesn't match `initialPlayerCount` correctly at test time.

**Investigation:** Run a multi-table test with detailed logging to see what `expectedTotal` and `grandTotal` are, and why they differ. Fix the baseline computation if the issue is how `initialPlayerCount` is captured.

**Test impact:** `test-multi-table.sh` chip conservation check can be hardened.

---

## Part C: Build & Test Order

1. Implement all Part A and Part B changes in a single worktree branch `feature/control-server-improvements`
2. Run `mvn test -P dev` — all existing tests must still pass
3. Build dev JAR: `mvn clean package -DskipTests -P dev`
4. Run the full scenario suite — existing tests pass, new tests (written in parallel by the main agent) also pass

---

## Part D: Test Script Updates (written in parallel by main agent)

The following test scripts need to be created or updated after the Java fixes land. The main agent writes these while the implementation agent works.

### New scripts:
- `test-rebuy-dialog.sh` — start with rebuys, go broke, assert REBUY_CHECK mode, DECLINE_REBUY, continue

### Updated scripts (WARN → FAIL hardening):
- `test-simulator.sh` — S-007 invalid card must return 400 (not WARN)
- `test-cheats-toggle.sh` — aifaceup must show opponent hole cards in /state
- `test-hand-history.sh` — use /cheat completeGame, assert history file written
- `test-keyboard-shortcuts.sh` — un-skip D-key test using disableAutoDeal
- `test-pause-allin.sh` — real test: wait for PAUSE_ALLIN mode, CONTINUE, assert resumes
- `test-chip-conservation.sh` — final conservation must be hard FAIL not WARN
- `test-save-load.sh` — assert `accepted:true` from save/load

---

## Completion Summary

Completed in follow-on implementation work:

- Added `/game/start` support for `disableAutoDeal`.
- Added `/cheat` actions `completeGame` and `advanceClock`.
- Updated state surface for tournament hand number and cheat-driven AI card visibility.
- Improved `/validate` handling for multi-table visibility constraints.
- Added/updated scenario coverage for rebuy and related control-server flows.

Any remaining behavior gaps are now tracked under desktop test realism hardening in:
`.claude/plans/DESKTOP-CLIENT-TEST-REALISM-OVERHAUL.md`.

---

## Constraints

- All changes to Java files must be in a worktree, not on main
- Dev-only changes go in `src/dev/java/` — production game behavior must not change except A1 (Card.getCard null fix is a production fix) and A2 (ShowTournamentTable fix is production)
- A1 and A2 are production bug fixes — write unit tests for them
- Run `mvn test -P dev` after every Java change before moving to the next
- Follow copyright guide: modified production files get dual copyright header if substantially changed
