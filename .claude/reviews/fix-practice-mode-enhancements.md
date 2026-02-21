# Review Request

**Branch:** fix-practice-mode-enhancements
**Worktree:** ../DDPoker-fix-practice-mode-enhancements
**Plan:** none (both items were small enough to implement directly)
**Requested:** 2026-02-21

## Summary

Two practice-mode UX improvements: (1) AI think-time now varies randomly per action instead
of a fixed 1000ms — makes AI feel less robotic; (2) the countdown timer bar now correctly
shows the server-provided timeout when it is the human player's turn to act.

## Files Changed

- [ ] `code/pokergameserver/src/main/java/.../gameserver/ServerPlayerActionProvider.java` — randomize AI delay in [base/2, base*2] using `ThreadLocalRandom`; with `aiActionDelayMs=1000` this gives ~500–2000ms per AI action
- [ ] `code/poker/src/main/java/.../online/WebSocketTournamentDirector.java` — in `onActionRequired()`, call `localPlayer.setTimeoutMillis(d.timeoutSeconds() * 1000)` before `setInputMode` so `CountdownPanel.countdown(true)` reads the correct value instead of 0
- [ ] `code/poker/src/test/java/.../online/WebSocketTournamentDirectorTest.java` — new test `actionRequiredSetsTimeoutOnLocalPlayer` verifying timeout is 30000ms after `ACTION_REQUIRED` with `timeoutSeconds=30`

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** Full `mvn test -P dev` from root: BUILD SUCCESS
- **Coverage:** Not checked (UI timing and rendering path — unit-testable part covered by new test)
- **Build:** Clean

## Context & Decisions

**AI delay:** `aiActionDelayMs` stays as the base (1000ms). The random sleep is
`ThreadLocalRandom.current().nextLong(base/2, base*2+1)` → [500, 2000] ms. This requires
no AI architecture changes — no modifications to `PlayerAction`, `PlayerActionProvider`, or
any AI class. The `aiActionDelayMs=0` path (used by all tests) is unchanged; only the `> 0`
branch is affected.

**Countdown timer:** `CountdownPanel.countdown(true)` reads `player.getTimeoutMillis()`. In
remote/practice mode, `PokerPlayer` objects are created with `nTimeMillis_=0` and never set
from server data. The fix adds `localPlayer.setTimeoutMillis(d.timeoutSeconds() * 1000)` and
`localPlayer.setThinkBankMillis(0)` before `setInputMode()` so the value is populated before
`CountdownPanel` reads it. The `OPTION_ONLINE_COUNTDOWN` defaults to `true` in
`client.properties` (line 2232) and `VERSION_COUNTDOWN_CHANGED` is 2.5.0 — well below the
current 3.3.0, so `bSupported_` will be `true`.

---

## Review Results

**Status:** APPROVED

**Reviewed by:** Claude Opus 4.6 (review agent)
**Date:** 2026-02-21

### Findings

#### Strengths

1. **Correct call ordering in countdown timer fix.** `setTimeoutMillis` and `setThinkBankMillis` are called *before* `game_.setInputMode()`, which is essential because `setInputMode` triggers `ShowTournamentTable.setInputMode()` -> `countdown_.countdown(true)` -> `CountdownPanel.countdown(true)` which immediately reads `player.getTimeoutMillis()`. The temporal ordering is correct.

2. **Null guard on `localPlayer`.** The `if (localPlayer != null && d.timeoutSeconds() > 0)` guard correctly handles the case where `hand.getCurrentPlayer()` returns null (e.g., if the local player was not found in the hand's player order). The `timeoutSeconds > 0` guard also correctly skips the setter when no timeout is configured (practice mode without a timeout), avoiding setting a meaningless 0-value.

3. **AI delay randomization is well-bounded.** `ThreadLocalRandom.current().nextLong(aiActionDelayMs / 2L, aiActionDelayMs * 2L + 1)` uses long arithmetic throughout, avoiding integer overflow. The `+1` correctly makes the upper bound inclusive (since `nextLong(origin, bound)` is exclusive on bound). With the default `aiActionDelayMs=1000`, the range is [500, 2000] ms -- a sensible 4:1 ratio that adds natural variation without being jarringly fast or annoyingly slow.

4. **Zero-delay path is untouched.** The `if (aiActionDelayMs > 0)` guard is unchanged, so all existing tests (which use `aiActionDelayMs=0`) are completely unaffected. No test flakiness risk from the randomization.

5. **`InterruptedException` handling is correct.** The existing `Thread.currentThread().interrupt()` re-interruption pattern is preserved. The delay is a fire-and-forget sleep that doesn't affect the already-computed `action`, so interruption simply cuts the delay short.

6. **Test is thorough for the countdown timer fix.** `actionRequiredSetsTimeoutOnLocalPlayer` sets up the full message sequence (GAME_STATE -> HAND_STARTED -> ACTION_REQUIRED), verifies both `getTimeoutMillis()` == 30000 and `getThinkBankMillis()` == 0, and correctly uses the EDT drain pattern. It also validates the `PokerPlayer` encoding round-trip (30000ms -> stored as 300 tenths internally -> retrieved as 30000ms).

7. **`setThinkBankMillis(0)` is necessary.** Without this, the combined `nTimeMillis_` field could carry over stale think-bank data from a previous action. Explicitly zeroing it ensures the countdown bar renders only the timeout portion.

#### Suggestions (Non-blocking)

1. **No unit test for the randomized AI delay.** The `ServerPlayerActionProvider` delay randomization is not directly tested, which is understandable since testing random sleep durations in unit tests leads to timing-sensitive flakiness. However, a test that at minimum verifies `aiActionDelayMs > 0` does not crash (e.g., with `aiActionDelayMs=1`) would add marginal confidence. The `ServerTournamentDirectorTest` integration test at line 251 exercises a similar path with `aiActionDelayMs=20`, so this is already indirectly covered. Non-blocking.

2. **Comment about "100ms increments" in test.** Line 248-249 of the test says "stored internally in 100ms increments, so readable as 30000". While technically accurate about the internal encoding (`setTimeoutMillis` divides by 100, `getTimeoutMillis` multiplies by 100), the comment is slightly misleading since the caller passes and receives milliseconds. The internal encoding is an implementation detail. Very minor, non-blocking.

#### Required Changes (Blocking)

None.

### Verification

- Tests: Confirmed by handoff -- full `mvn test -P dev` BUILD SUCCESS. New test `actionRequiredSetsTimeoutOnLocalPlayer` exercises the end-to-end path from `ACTION_REQUIRED` message through `PokerPlayer` timeout storage.
- Coverage: Not checked (appropriate -- the `CountdownPanel.countdown()` rendering path is Swing UI code not amenable to unit testing; the testable portion is covered by the new test).
- Build: Clean per handoff.
- Privacy: SAFE -- all three files contain only game logic code. No credentials, tokens, personal data, or file paths.
- Security: SAFE -- `ThreadLocalRandom` is appropriate for non-cryptographic randomization. No user input is used unsanitized. The `timeoutSeconds * 1000` multiplication cannot overflow `int` for any realistic timeout value (max safe ~2.1M seconds = ~24 days).
