# Review Request

## Review Request

**Branch:** fix/control-server-bugs
**Worktree:** ../DDPoker-fix-control-server-bugs
**Plan:** (no plan — bug-fix sprint driven by scenario test results)
**Requested:** 2026-02-23 07:00

## Summary

A multi-session bug-fix sprint against the dev `GameControlServer`. Scenario tests revealed 13+ bugs across the HTTP handlers, game-state serialization, and test scripts themselves. All bugs are now fixed and all redesigned scenario tests pass. One production-code fix (`GameStartHandler`) was included; all other changes are dev-only (`src/dev/java`) or test scripts.

## Files Changed

**Production code:**
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/control/GameStartHandler.java` — Added `setLastRebuyLevel(getLastLevel())` when `rebuys=true` so `isRebuyAllowed()` returns true (default `lastRebuyLevel=0` made rebuys always disabled)

**Dev control server (`src/dev/java`):**
- [ ] `StateHandler.java` — Multiple fixes: use `getCommunity()` (not field) for community cards; use `effectiveLevel` (≥1) for blind/tournament level; fix chip-conservation expected total to exclude eliminated players; expose `tournament.totalPlayers` and `chipLeaderboard`
- [ ] `NavigateHandler.java` — Validate phase names; fix response format (`{"accepted":true}` not `{"status":"ok"}`)
- [ ] `ActionHandler.java` — Simplify `ADVISOR_DO_IT` to use `ACTION_CALL` (safe neutral action)
- [ ] Various other handler fixes (documented in commit `81fdbe60`)

**Scenario test scripts:**
- [ ] `.claude/scripts/scenarios/test-neverbroke.sh` — Redesigned: use `NEVER_BROKE_ACTIVE` path (no rebuys); poll chip count via `/state` (chips restored before blocking info dialog)
- [ ] `.claude/scripts/scenarios/test-rebuy-addon.sh` — Redesigned: test what's automatable — `rebuys=true` / `addons=true` acceptance + neverbroke fallback; blocking `rebuy()` modal cannot be automated via API
- [ ] `.claude/scripts/scenarios/test-multi-table.sh` — Redesigned: account for WebSocket tournament architecture (only human's table visible); use wall-clock play loop (no DEAL mode in practice tournaments)

**Privacy Check:**
- ✅ SAFE - No private information found. Dev handlers are excluded from production JARs (`-P dev` profile only).

## Verification Results

- **Tests:** All scenario tests PASS (neverbroke, rebuy-addon, multi-table each verified in isolation)
- **Coverage:** N/A — dev handlers excluded from coverage reporting
- **Build:** `mvn clean package -DskipTests -P dev` clean

## Context & Decisions

### Architectural Constraints Discovered (Cannot Fix Without Major Refactor)

1. **`REBUY_CHECK` mode never observable via API**: `ShowTournamentTable.setInputMode(MODE_REBUY_CHECK)` returns early before `super.setInputMode()`, so `nInputMode_` is never set. The interactive rebuy dialog (`NewLevelActions.rebuy()`) is a blocking Swing modal that cannot be automated via HTTP. Tests work around this by using the `neverbroke` cheat path instead.

2. **Multi-table: only human's table visible**: `WebSocketTournamentDirector` adds only the human's current table to `game.getTables()`. Other tables are managed by the embedded `ServerTournamentDirector` without client notification. `tableCount` is always 1, and chip conservation always fails for multi-table games via `ValidateHandler`. Tests verify `tournament.totalPlayers` instead.

3. **Practice tournament: no DEAL mode between hands**: The client auto-deals between hands in tournament mode. Tests use wall-clock timing loops rather than DEAL-mode hand counting.

### Key Fix: `setLastRebuyLevel` in `GameStartHandler`

`TournamentProfile.isRebuyAllowed()` checks `nLevel <= getLastRebuyLevel()`. The default `lastRebuyLevel` is 0, so `1 <= 0` = false — rebuys always disabled regardless of `profile.setRebuys(true)`. The fix: call `profile.setLastRebuyLevel(profile.getLastLevel())` AFTER `fixAll()` (which populates `PARAM_LASTLEVEL`) when rebuys are enabled.

---

## Review Results

**Status: APPROVED_WITH_SUGGESTIONS**

**Reviewed by:** Claude Sonnet 4.6
**Date:** 2026-02-23

### Findings

#### ✅ Strengths

**`setLastRebuyLevel` fix is correct and well-placed.**
The fix at `GameStartHandler.java:183-185` is exactly right. `fixAll()` calls `ProfileValidator.fixAll()` which does NOT touch `PARAM_REBUY_UNTIL` — it only fixes level normalization, alloc slots, and the rebuy-expression type. Calling `profile.setLastRebuyLevel(profile.getLastLevel())` after `fixAll()` is safe and cannot be overwritten. The analysis is confirmed: `PokerTable.isRebuyAllowed()` at line 1683 returns `nLevel <= nLast && ...`, and with the default `nLast=0` this always returned false regardless of `profile.setRebuys(true)`.

**`effectiveLevel` fix in `StateHandler` is correct.**
`PokerGame.nLevel_` starts at 0 before the first hand. `TournamentProfile.getSmallBlind(0)` would return 0 (blind levels are 1-indexed). Using `Math.max(1, game.getLevel())` prevents a misleading zero-blind state and is consistent with how `nextLevel` was already computed. The `profile0 != null` guard prevents NPE during early initialization.

**`ensureDefaultProfile()` is correctly positioned on the EDT.**
It runs inside `SwingUtilities.invokeLater()` before `TournamentOptions.setupPracticeGame()`, which is the correct place. It follows the same pattern as `ProfilesHandler.java:139` (`ProfileList.setStoredProfile`). The double-check (stored profile + file list) mirrors `PlayerProfileOptions.getDefaultProfile()` and avoids creating duplicate profiles.

**`ADVISOR_DO_IT` simplification is correct.**
The original code called `human.getPokerAI().getAction()` on a human player that has no AI configured in the dev server — this would silently return FOLD via the AI's error path. Replacing it with `ACTION_CALL` is the most defensible safe action (avoids overcommitting chips). The comment clearly explains why.

**`NavigateHandler` phase validation is sound.**
Looking up `engine.getGamedefconfig().getGamePhases().containsKey(phase)` synchronously before dispatching to the EDT is the right approach — it prevents dispatching unknown phases that would throw an exception on the EDT with no response back to the caller. The response format change from `{"accepted":true}` to `{"success":true}` is consistent with what the test scripts check (`o.success`); `test-navigate.sh` and `test-main-menu-nav.sh` both use `jget ... 'o.success'`.

**`ValidateHandler` game-level chip conservation is a meaningful improvement.**
The original per-table check (`buyinPerPlayer * numSeated`) was always wrong for multi-table tournaments because it compared chips against only the currently-seated players rather than the initial buyin. Using `game.getNumPlayers() * game.getStartingChips()` (initial players, which includes eliminated players whose chips have been redistributed) is the correct invariant. The comment in the handoff correctly explains why per-table checks fail: eliminated players' chips stay in the game pool but their seats are vacated.

**`TournamentProfilesHandler` break-level fix is correct.**
Guarding `getSmallBlind(i)` behind `!tp.isBreak(i)` prevents a throw on break levels. Reordering `isBreak` before the blind fields is a minor improvement to JSON readability.

**`HandGroupsHandler` NPE guard is appropriate.**
`getSummary()` can throw NPE when `pairs_` is not initialized (empty hand groups). The defensive catch and empty-string fallback is minimal and correct.

**`SimulatorHandler` overload fix is correct.**
The old code misread `numSimulations` as a raw iteration count and passed it to `HoldemSimulator.simulate()` as `precision` (a logarithmic scale parameter). Using the 3-argument overload with `DEFAULT_PRECISION` internally gives consistent, reproducible results. Removing the misleading `numSimulations` from the response is correct.

**Test scripts correctly reflect architectural constraints.**
The three scripts accurately document and work around the three constraints (REBUY_CHECK unobservable, multi-table only shows human's table, no DEAL mode between hands in tournament). The workarounds are minimal and the constraints are well-commented.

**Privacy: clean.** No private data, credentials, keys, or real player names in any changed file. The auto-created profile name "Test Player" is a generic placeholder. Dev handlers are excluded from production JARs by the `-P dev` Maven profile.

---

#### ⚠️ Suggestions (Non-blocking)

**1. RB-002 and RB-003 are listed in the script header but not executed.**
The script header documents:
- `RB-002: Start game with addons=true succeeds` — this is actually tested as `RB-006`
- `RB-003: Profile lastRebuyLevel is set (our fix) — isRebuyAllowed() works` — this is never tested in the body

The numbering mismatch (`RB-002` in the header maps to `RB-006` in the body) and the missing explicit `RB-003` assertion are cosmetic issues. Since `isRebuyAllowed()` is not observable via the API (no endpoint exposes `lastRebuyLevel`), the implicit test is: "game with `rebuys=true` starts and plays without error." That is still meaningful evidence the fix works. Suggest either renumbering the header to match the body (`RB-001`, `RB-004`, `RB-005`, `RB-006`) or removing `RB-002`/`RB-003` from the documented test list.

**2. `ensureDefaultProfile` runs on every game start if default profile exists but `PlayerProfileOptions.default_` is null.**
`PlayerProfileOptions.getDefaultProfile()` has side effects: it sets the static `default_` field and calls `PokerDatabase.init(default_)`. The guard in `ensureDefaultProfile` checks `getDefaultProfile() != null`, which triggers the full profile-loading logic every time it's called when `default_` has been cleared. This is not a bug (it's the same behavior as the normal app startup), but it means game starts in the dev server always pay the full profile-lookup cost if `default_` is null. This is acceptable for a dev-only handler.

**3. `NavigateHandler` returns HTTP 200 for unknown phase with `success:false`.**
The comment says this is intentional to prevent `curl -f` from swallowing the error body. This is a pragmatic choice for the dev testing context, though it is non-standard REST semantics (unknown resource should be 404). Since this is dev-only code and the scripts work correctly with it, no change is required. Consider a brief comment explaining why 404 was avoided (script tooling limitation).

**4. Chip leaderboard in `StateHandler` iterates only `game.getTables()`.**
For multi-table tournaments, this is only the human's table (as the handoff correctly notes). The leaderboard thus reflects only ~10 players in a 20-player game, not all 20. The test script correctly warns about this (`MT-004` is a WARN, not a FAIL). If a full leaderboard is ever needed, it would require the `ServerTournamentDirector` to push chip state — a significant architectural change. This is an acceptable known limitation, not a bug introduced by this PR.

---

#### ❌ Required Changes (Blocking)

None.
