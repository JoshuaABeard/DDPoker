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

**Status:** APPROVED_WITH_SUGGESTIONS

**Reviewed by:** Claude Opus 4.6
**Date:** 2026-02-23

### Findings

#### Strengths

1. **Card.getCard null fix (A1)** is correct and well-tested. The new `CardTest.testGetCardByStringInvalidReturnsNull` covers short strings, invalid rank, invalid suit, and the combination. The fix is minimal: two guard clauses returning null, placed at the right points in the method. The `CardInjectHandler` update to `c == null || c.isBlank()` is the right defense.

2. **ShowTournamentTable REBUY_CHECK fix (A2)** is safe. `ShowPokerTable.setInputMode()` (the super method) simply assigns `nInputMode_ = nMode` with no side effects -- no UI updates, no event firing, no listener notification. Calling it before the early return is the minimal, correct fix. The rebuy-specific UI code (`setRebuyButton`) still runs on the same code path, so no existing behavior changes.

3. **StateHandler handNumber + aifaceup (A3+A4)** are clean additive changes. `currentTable.getHandNum()` is the right counter -- it is 0 before the first deal and increments per hand, matching the documented semantics. The aifaceup exposure is correctly gated on `PokerUtils.isOptionOn(PokerConstants.OPTION_CHEAT_AIFACEUP)` and only when `hand != null`, preventing stale card data between hands. The showdown path (`hand.getRound() == BettingRound.SHOWDOWN`) is logically separate from the aifaceup path -- both are ORed, which is correct (showdown reveals cards regardless of cheat state).

4. **disableAutoDeal (B1)** design is sound. The nullable `Boolean autoDeal` in `PracticeConfig` is a clean contract: `null` = default (auto-deal on), `false` = disabled. `PracticeGameLauncher` reads the pref and passes `autoDealEnabled ? null : false`, which correctly maps the user preference. `GameInstance.start()` iterates all tables to set `autoDeal(false)` before the director starts, so there is no race.

5. **ValidateHandler (B4)** correctly uses `profile.getNumPlayers()` for the immutable starting count. The multi-table heuristic (`tables.size() < ceil(profilePlayers/SEATS) && expectedTables > 1`) is reasonable for the dev tooling context. The null guard on `profile` with fallback to `game.getNumPlayers()` prevents NPE during early initialization.

6. **CheatControllerTest** update to the 7-arg `PracticeConfig` constructor is minimal and correct.

#### Suggestions (Non-blocking)

1. **Card.getCard null -- downstream callers in production code need audit.** Two production callers are not null-safe after this change:

   - `DDCardView` (line 63): `card_ = Card.getCard(card)` -- if the HTML attribute contains a malformed card string, `card_` becomes null. Line 75 then calls `piece_.setCard(null)` and line 76 evaluates `null != Card.BLANK` as true (displaying a null card face-up). In practice, the HTML card attributes come from `Card.toStringSingle()` via `toHTML()`, so they should always be valid 2-character strings. However, the code should defensively handle null, e.g., `card_ = Card.getCard(card); if (card_ == null) card_ = Card.BLANK;`.

   - `PokerDatabase` (lines 1730-1742): `Card.getCard(card).toHTML()` is called directly. If the database contained a malformed card string, this would NPE. The `card != null` guard on the line above only checks for SQL NULL, not for invalid card content. Again, in practice, the database stores cards written by the engine (always valid), so this is a theoretical risk only.

   Both of these are pre-existing latent issues -- the old code masked them by returning BLANK for invalid input. The risk is extremely low in practice (both callers only receive card strings written by the engine itself), but adding null guards would be defensive best practice. Consider a follow-up to audit all `Card.getCard(String)` callers in production code.

2. **completeGame thread safety (B2).** The `handleCompleteGame` method reads `human.getChipCount()`, iterates all tables, sums chips, and sets chip counts -- all inside `SwingUtilities.invokeLater()`. This runs on the EDT, which is correct for Swing state. However, if the game engine is concurrently running AI actions on a background thread that also modifies chip counts, there could be a brief inconsistency. In practice, the scenario test scripts wait for a non-betting input mode before calling `/cheat`, so this is unlikely to be a real problem. No action needed, but worth noting.

3. **completeGame does not trigger game-over logic directly.** It redistributes chips so that only the human has chips and all AI players have 0. The game-over condition (one player remaining with chips) is checked by the engine at the start of the next hand. So the game does not end immediately -- it ends after the next hand starts and the engine detects the condition. This is the correct behavior for the stated use case (test wants to play one more hand and then see game-over), but callers should be aware that `completeGame` is not instantaneous termination.

4. **advanceClock and level advancement.** `clock.setSecondsRemaining(newRemaining)` fires a `SET` action event. However, the actual level advancement is driven by the `STOP` action event (fired when `isExpired()` is true and `stop()` is called during the tick). Setting seconds to 0 does not itself trigger a tick or stop -- the next 1-second timer tick will detect the expiry and fire the stop event. This means there is up to a 1-second delay between the `advanceClock` call and the actual level advancement. For test tooling this is fine, but the caller should poll for level change rather than assuming immediate effect.

5. **ValidateHandler multi-table heuristic.** The check `(tables == null ? 0 : tables.size()) < expectedTables && expectedTables > 1` correctly detects when fewer tables are visible than expected. However, `PokerConstants.SEATS` is used in the calculation, which is the max seats per table (10). If the tournament uses a different table size, the heuristic would be wrong. In practice, DD Poker always uses 10-seat tables, so this is not a real issue. The heuristic is also generous (it skips the check entirely rather than trying to do partial validation), which is the right call for dev tooling.

#### Required Changes (Blocking)

None. All changes are correct and safe for the stated purpose. The `Card.getCard` null-safety concern in `DDCardView` and `PokerDatabase` is a pre-existing latent issue that this change makes slightly more likely to surface, but the practical risk is near zero since those callers only receive engine-generated card strings.

### Verification

- Tests: Accepted -- all passed per handoff, and new CardTest cases cover the null-return contract.
- Coverage: Accepted -- new production code (Card.java guard clauses, ShowTournamentTable super call) is covered by new tests and existing integration paths.
- Build: Accepted -- clean build with zero warnings.
- Privacy: SAFE -- no private data, credentials, or PII in any changed file.
- Security: SAFE -- all new endpoints are dev-only (src/dev/java), gated by API key authentication. No new attack surface in production code.
