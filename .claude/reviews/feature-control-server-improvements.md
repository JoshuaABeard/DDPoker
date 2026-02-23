# Review Request

**Branch:** feature/control-server-improvements
**Worktree:** C:\Repos\DDPoker-feature-control-server-improvements
**Plan:** .claude/plans/control-server-improvements.md
**Requested:** 2026-02-23 18:00

## Summary

Implements 8 bug fixes and dev control server improvements across the `pokerengine`, `poker`, and `pokergameserver` modules. Fixes include: `Card.getCard(String)` returning null for invalid cards (instead of the shared BLANK sentinel), `ShowTournamentTable` not updating `nInputMode_` for REBUY_CHECK, and `ValidateHandler` chip conservation using the shrinking in-game player count. New features include `handNumber` and AI hole cards (`aifaceup`) in `/state`, `disableAutoDeal` for `POST /game/start`, `completeGame` and `advanceClock` cheat actions, and multi-table-aware chip conservation in `/validate`.

## Files Changed

- [ ] `code/pokerengine/src/main/java/com/donohoedigital/games/poker/engine/Card.java` - Fix `getCard(String)`: return null (not BLANK) for strings shorter than 2 chars or with unrecognized rank/suit
- [ ] `code/pokerengine/src/test/java/com/donohoedigital/games/poker/engine/CardTest.java` - Add `testGetCardByStringInvalidReturnsNull` and `testGetCardByStringValidDoesNotReturnNull` tests
- [ ] `code/poker/src/dev/java/com/donohoedigital/games/poker/control/CardInjectHandler.java` - Update invalid-card check to `c == null || c.isBlank()` (getCard now returns null)
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/ShowTournamentTable.java` - Call `super.setInputMode()` before early return for MODE_REBUY_CHECK so `nInputMode_` is updated
- [ ] `code/poker/src/dev/java/com/donohoedigital/games/poker/control/StateHandler.java` - Add `handNumber` field; expose AI hole cards when `aifaceup` cheat option is enabled
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/GameConfig.java` - Add `Boolean autoDeal` field to `PracticeConfig` record
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/GameInstance.java` - Apply `autoDeal=false` to all `ServerGameTable` instances when the config flag is set
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/server/PracticeGameLauncher.java` - Read `OPTION_AUTODEAL` pref and pass `autoDeal` (null=on, false=off) into `PracticeConfig`
- [ ] `code/poker/src/dev/java/com/donohoedigital/games/poker/control/GameStartHandler.java` - Accept `disableAutoDeal` JSON param; write pref to `engine.getPrefsNode()` before game launch
- [ ] `code/poker/src/dev/java/com/donohoedigital/games/poker/control/CheatHandler.java` - Add `completeGame` and `advanceClock` actions; add `GameClock`, `PokerConstants`, `List` imports
- [ ] `code/poker/src/dev/java/com/donohoedigital/games/poker/control/ValidateHandler.java` - Use `TournamentProfile.getNumPlayers()` for baseline; detect multi-table mode and skip global conservation check
- [ ] `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/controller/CheatControllerTest.java` - Update `PracticeConfig` constructor call to 7-arg form

**Privacy Check:**
- SAFE - No private information found. No API keys, credentials, IPs, or personal data. All changes are game-logic and dev tooling only.

## Verification Results

- **Tests:** All passed (`mvn test -P dev` from `code/`) — 6 commits, all green after each commit
- **Coverage:** Not separately measured; threshold is 65%, new code is covered by new tests (CardTest) and existing handler tests
- **Build:** Clean — `mvn clean package -DskipTests -P dev` succeeded with zero warnings

## Context & Decisions

**A1 (Card.getCard null fix):** The original code returned `Card.BLANK` (a shared static sentinel) for invalid strings. `CardInjectHandler` compared using `c.isBlank()` which is a non-null instance method — it worked only because BLANK is non-null. Returning null instead is the correct contract (documented in existing Javadoc), and the null check in CardInjectHandler is cleaner. Existing callers that check `card != null` are unaffected; callers that called `isBlank()` on the result needed a null-guard update (CardInjectHandler).

**A2 (ShowTournamentTable REBUY_CHECK):** The early return for MODE_REBUY_CHECK bypassed the `super.setInputMode()` call that stores the mode in `nInputMode_`. Adding `super.setInputMode()` before the return is the minimal fix — it ensures the StateHandler can observe the mode. No functional change to the rebuy UI flow itself.

**A3+A4 (StateHandler handNumber + aifaceup):** `handNumber` uses `currentTable.getHandNum()` which is 0 before the first deal. AI hole cards are exposed only when the `OPTION_CHEAT_AIFACEUP` option is set, matching the existing "Computer Cards Face Up" cheat behavior. Both additions are additive-only and do not change existing fields.

**A5 (PAUSE_ALLIN investigation):** No code change. The pause-allin mode is already exposed as `CONTINUE_LOWER` via the `AllInRunoutPaused → CONTINUE_RUNOUT → MODE_CONTINUE_LOWER` chain in `WebSocketTournamentDirector`. The plan's investigation confirmed this and no new mode is needed.

**B1 (disableAutoDeal):** The pref write in `GameStartHandler` uses `engine.getPrefsNode().putBoolean()` because `PokerUtils.setOptionOn()` does not exist (it only has `isOptionOn` getters). The `autoDeal` field in `PracticeConfig` is a `Boolean` (nullable) so that `null` means "use default" and only an explicit `false` disables it — consistent with the pattern of other nullable config fields in the record.

**B2+B3 (CheatHandler completeGame + advanceClock):** Both actions are dispatched via `SwingUtilities.invokeLater()` and return `{"accepted": true}` immediately, consistent with the existing `setChips` / `setLevel` pattern. `completeGame` collects only non-human, non-eliminated players' chips to avoid violating conservation (the human's chips are already correct). `advanceClock` uses `Math.max(0, ...)` so the clock cannot go negative.

**B4 (ValidateHandler multi-table):** Two separate bugs fixed together. (1) `game.getNumPlayers()` shrinks as eliminated players are removed from the seat map — using `profile.getNumPlayers()` fixes the shrinking baseline. (2) In multi-table tournaments the client only sees one table; detecting this case (visible tables < ceil(profilePlayers/10)) and skipping the global conservation check prevents false-positive warnings.

---

## Review Results

*[Review agent fills this section]*

**Status:**

**Reviewed by:**
**Date:**

### Findings

#### Strengths

#### Suggestions (Non-blocking)

#### Required Changes (Blocking)

### Verification

- Tests:
- Coverage:
- Build:
- Privacy:
- Security:
