# Review Request: Per-Street Action Timeouts

## Review Request

**Branch:** fix-critical-security-issues
**Worktree:** C:\Repos\DDPoker (main worktree)
**Plan:** .claude/plans/GAME-HOSTING-CONFIG-IMPROVEMENTS.md (item #9)
**Requested:** 2026-02-13 00:25

## Summary

Implemented per-street action timeouts for online tournaments, allowing hosts to configure different timeout values for each betting round (pre-flop, flop, turn, river). Players typically need less time for simple pre-flop decisions and more time for complex river decisions. The feature uses a fallback pattern where per-street timeouts default to 0, falling back to the base timeout when not configured.

## Files Changed

- [x] code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/TournamentProfile.java - Added 4 parameter constants, getTimeoutForRound() method with fallback logic, and 8 getter/setter methods for per-street timeouts
- [x] code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/TournamentProfileTest.java - Added 6 unit tests covering defaults, fallback behavior, overrides, boundary enforcement, serialization, and invalid rounds
- [x] code/poker/src/main/java/com/donohoedigital/games/poker/online/TournamentDirector.java - Modified doBettingTimeoutCheck() to use round-specific timeout instead of base timeout
- [x] code/poker/src/main/java/com/donohoedigital/games/poker/TournamentProfileDialog.java - Added collapsible "Advanced Timeout (Per-Street)" UI section with 4 spinners and toggle checkbox
- [x] code/poker/src/main/resources/config/poker/client.properties - Added labels, defaults, and help text for new UI controls; added per-street timeout display message
- [x] code/pokerengine/src/main/java/com/donohoedigital/games/poker/engine/TournamentProfileHtml.java - Updated toHTMLOnline() to display per-street timeouts in tournament summary
- [x] .claude/plans/GAME-HOSTING-CONFIG-IMPROVEMENTS.md - Marked item #9 as completed

**Privacy Check:**
- ✅ SAFE - No private information found. All changes are configuration parameters and UI controls.

## Verification Results

- **Tests:** 1,089/1,089 passed (37 tests in TournamentProfileTest, including 6 new tests)
- **Coverage:** Not measured (used -P dev profile which skips coverage)
- **Build:** Clean, no warnings

## Context & Decisions

**Module Independence:** Used raw numeric constants (0, 1, 2, 3) instead of `HoldemHand.ROUND_*` constants in TournamentProfile.java to avoid circular module dependency (pokerengine module cannot depend on poker module where HoldemHand lives). Added comments documenting the mapping.

**Fallback Pattern:** Chose to use 0 as "not set" rather than null because:
- Simpler UI: spinners default to 0, no special handling needed
- Clear fallback semantics: 0 means "use base timeout"
- Consistent with other optional integer parameters in the codebase

**Progressive Disclosure:** Made the advanced timeout section collapsible (starts collapsed) to avoid overwhelming users who don't need per-street customization.

**No Validation Conflicts:** Allowed per-street timeouts to be higher OR lower than base timeout, giving hosts full flexibility. The existing MIN_TIMEOUT and MAX_TIMEOUT boundaries are enforced on all timeout values.

---

## Review Results

**Status:** CHANGES REQUIRED

**Reviewed by:** Claude Opus 4.6 (review agent)
**Date:** 2026-02-13

**Note:** The changes are currently stashed (`git stash@{0}`) rather than committed or staged. Review was conducted against the stash diff. The stash also contains unrelated `PokerUtils.java` refactoring (extracting methods to `PokerLogicUtils.java`) that is not part of this feature and should be separated.

### Findings

#### Strengths

1. **Clean data model design.** The four new `PARAM_TIMEOUT_*` constants follow the exact naming and storage pattern used by all other `TournamentProfile` parameters. The `DMTypedHashMap`-backed getter/setter pairs are consistent with `getTimeoutSeconds()`, `getThinkBankSeconds()`, etc.

2. **Well-designed fallback logic.** `getTimeoutForRound()` uses a clear two-step approach: look up the round-specific param, return it if > 0, otherwise fall back to `getTimeoutSeconds()`. This is simple to reason about and impossible to misconfigure -- any unconfigured street naturally inherits the base timeout.

3. **Module dependency handled correctly.** Using raw int constants (0-3) in `TournamentProfile.java` with inline comments documenting the mapping to `HoldemHand.ROUND_*` is the right approach to avoid a circular dependency from `pokerengine` to `poker`. The caller side (`TournamentDirector.java`) correctly uses the symbolic constants.

4. **Thorough test coverage.** The 6 new tests cover all important cases: defaults (all zero), full fallback (all unset), selective override (some set, some not), boundary enforcement (> MAX_TIMEOUT), serialization round-trip, and invalid round numbers (ROUND_SHOWDOWN and 999). Edge cases are well-handled.

5. **Minimal integration change.** The `TournamentDirector.doBettingTimeoutCheck()` change is exactly 3 lines: get the hand, get the round, call `getTimeoutForRound()`. Surgical and easy to verify.

6. **Backward compatible.** Existing profiles with no per-street params will default to 0 for all streets, which means `getTimeoutForRound()` falls back to `getTimeoutSeconds()`, preserving existing behavior exactly.

7. **HTML display is sensible.** Only shows per-street values that are explicitly configured (> 0); falls back to a "Not configured" message when all are at default. This avoids cluttering the tournament summary for profiles that don't use the feature.

#### Suggestions (Non-blocking)

1. **Per-street timeouts should enforce MIN_TIMEOUT when non-zero.** Currently, `getTimeoutForRound()` uses `map_.getInteger(param, 0, 0, MAX_TIMEOUT)` which allows values 1-4 seconds. The base timeout enforces `MIN_TIMEOUT` (5) via `map_.getInteger(PARAM_TIMEOUT, 30, MIN_TIMEOUT, MAX_TIMEOUT)`. A per-street timeout of 1-4 seconds would bypass this floor. The fix is straightforward:

   In `getTimeoutForRound()`:
   ```java
   int roundTimeout = map_.getInteger(param, 0, 0, MAX_TIMEOUT);
   if (roundTimeout > 0 && roundTimeout < MIN_TIMEOUT) {
       roundTimeout = MIN_TIMEOUT;
   }
   return (roundTimeout > 0) ? roundTimeout : getTimeoutSeconds();
   ```

   Or alternatively, change the getters to use `MIN_TIMEOUT` as the minimum when non-zero. The UI spinner could also enforce this (min=5 instead of min=0, with a special "0 = use base" convention explained in help text).

2. **Consider adding a `setTimeoutSeconds()` test.** The new `setTimeoutSeconds()` setter method is tested indirectly via the per-street fallback tests (which call it to set the base timeout to 45), but a direct test of the base setter/getter pair would be a small improvement since this setter is newly added code.

3. **HTML display could be trimmed.** The `StringBuilder` approach in `TournamentProfileHtml.toHTMLOnline()` appends trailing spaces (e.g., `"Pre-flop: 15s "`) before the next entry. A minor cosmetic issue -- the trailing space after the last entry could be trimmed, or entries could be joined with a separator like " | " for better readability.

4. **Separate unrelated refactoring.** The stash includes `PokerUtils.java` changes (extracting `pow()`, `roundAmountMinChip()`, and `nChooseK()` to `PokerLogicUtils.java`). This is a separate refactoring concern and should be in its own commit/branch. Mixing feature work with refactoring makes the changeset harder to review and harder to revert if needed.

#### Required Changes (Blocking)

1. **CRITICAL: Missing `option.advancedtimeout.show.default` property.** The `OptionBoolean` constructor calls `PropertyConfig.getRequiredBooleanProperty(getDefaultKey())` which resolves to `option.advancedtimeout.show.default`. This property is **not present** in the stashed `client.properties` changes. The application will throw a runtime exception when the Online tab of `TournamentProfileDialog` is opened. Fix: add `option.advancedtimeout.show.default=false` to `client.properties`.

2. **CRITICAL: Missing `option.advancedtimeout.show.label` property.** The `DDOption.getLabel()` method returns `PropertyConfig.getMessage("option." + sOrigName_ + ".label")`, which resolves to `option.advancedtimeout.show.label`. The stash defines `option.advancedtimeout.show=` (without `.label` suffix), which is the wrong key. This will cause a missing message key error at runtime. Fix: rename `option.advancedtimeout.show=` to `option.advancedtimeout.show.label=` in `client.properties`.

3. **CRITICAL: Collapsible panel will never become visible.** The code sets `advancedDummy.setVisible(false)` to start collapsed, but `OptionBoolean.stateChanged()` only calls `extra_.setEnabledEmbedded(box_.isSelected())`, which delegates to `setEnabled()` -- NOT `setVisible()`. Checking the checkbox will enable the panel's children but the panel itself will remain invisible. The existing patterns for late registration and scheduled start do **not** use `setVisible(false)` -- they rely solely on enable/disable toggling. Two options to fix:
   - **Option A (recommended):** Remove `advancedDummy.setVisible(false)` and follow the same enable/disable pattern as late registration and scheduled start. The spinners will be visible but grayed out when unchecked.
   - **Option B:** Override `setEnabledEmbedded()` in `OptionDummy` to also toggle visibility, but this would change framework behavior and is more invasive.

### Verification

- **Tests:** Could not run directly (changes are stashed, not applied). The handoff reports 1,089/1,089 passed. The 6 new tests appear correct in their assertions and cover the right scenarios. However, given the 3 blocking UI/properties issues found above, the tests would not catch these runtime errors since they test the data model only (no UI tests).
- **Coverage:** Not measured (dev profile was used).
- **Build:** Could not verify (changes are stashed). The handoff reports clean build with no warnings, but the missing properties keys would only manifest at runtime, not at compile time.
- **Privacy:** SAFE. No private data, credentials, or sensitive information in any of the changed files. All changes are configuration parameters, UI controls, and display logic.
- **Security:** No security concerns. The timeout values are bounded by MAX_TIMEOUT (120) and validated through `DMTypedHashMap.getInteger()` clamping. No user input reaches any dangerous operations.

---

## Second Review Results

**Status:** APPROVED

**Reviewed by:** Claude Opus 4.6 (review agent)
**Date:** 2026-02-13

**Context:** Developer applied fixes for all 3 blocking issues from the first review plus implemented non-blocking suggestion #1 (MIN_TIMEOUT enforcement). Changes are now unstaged in working directory. The diff also includes unrelated refactoring (BetValidator/PokerLogicUtils extraction from PokerUtils/Bet.java) that was noted in the first review.

### Blocking Issue Resolution

1. **RESOLVED: Missing `option.advancedtimeout.show.default` property.** The property `option.advancedtimeout.show.default=false` is now present at line 2908 of `client.properties`. The `OptionBoolean` constructor will successfully look up this key. Verified that the key format matches `GetDefaultKey()` output: `"option." + "advancedtimeout.show" + ".default"`.

2. **RESOLVED: Missing `option.advancedtimeout.show.label` property.** The property `option.advancedtimeout.show.label=Customize timeout per betting round` is now present at line 2907 of `client.properties`. The key correctly includes the `.label` suffix that `DDOption.getLabel()` expects via `"option." + sOrigName_ + ".label"`.

3. **RESOLVED: Collapsible panel visibility issue.** The `setVisible(false)` call has been removed. The code now follows the same enable/disable pattern as late registration (line 417-422) and scheduled start (line 450-455): an `OptionDummy` wraps the nested controls and is passed to `OptionBoolean` as the `extra` parameter. When the checkbox is unchecked, `OptionBoolean.stateChanged()` calls `extra_.setEnabledEmbedded(false)`, which grays out the spinners. When checked, they become enabled. This is the correct framework pattern.

### MIN_TIMEOUT Enforcement (Non-blocking suggestion #1)

**Correctly implemented.** The `getTimeoutForRound()` method at lines 1490-1494 of `TournamentProfile.java` now reads:

```java
int roundTimeout = map_.getInteger(param, 0, 0, MAX_TIMEOUT);
if (roundTimeout > 0 && roundTimeout < MIN_TIMEOUT) {
    roundTimeout = MIN_TIMEOUT; // Enforce minimum when non-zero
}
return (roundTimeout > 0) ? roundTimeout : getTimeoutSeconds();
```

This correctly enforces that any non-zero per-street timeout is at least `MIN_TIMEOUT` (5 seconds), matching the floor enforced on the base timeout. Values of 0 are still allowed (meaning "use base timeout"). The implementation matches the exact code suggested in the first review.

**Test gap (non-blocking):** There is no unit test specifically verifying the MIN_TIMEOUT enforcement on per-street values (e.g., setting a pre-flop timeout to 3 and asserting it returns 5). The existing `should_EnforceMaxTimeout_ForPerStreet` test covers the upper bound but not the lower bound. The implementation is correct, but adding a `should_EnforceMinTimeout_ForPerStreetWhenNonZero` test would strengthen coverage.

### New Issues Check

No new issues were introduced by the fixes. Specifically verified:

- **UI pattern is consistent.** The advanced timeout section (`TournamentProfileDialog.java` lines 339-380) follows the identical structure as the late registration and scheduled start sections: `DDLabelBorder` -> child controls in `DDPanel` -> `OptionDummy` wrapper -> `OptionBoolean` with `extra` parameter. Border insets, layout gaps, and grid layout parameters are consistent.

- **Properties are complete.** All required property keys for the 4 new `OptionInteger` spinners are present: `.label`, `.default`, and `.help` for `timeoutpreflop`, `timeoutflop`, `timeoutturn`, and `timeoutriver`. The `OptionBoolean` has both `.label` and `.default`.

- **HTML display parameter alignment is correct.** The `msg.tournamentonline` message template now uses `{11}` for the per-street timeouts display. The `toHTMLOnline()` method passes 12 arguments (indices 0-11), with `sPerStreetTimeouts` as the 12th argument (index 11). Parameter ordering matches the template placeholders.

- **TournamentDirector integration is unchanged and correct.** The 3-line change at lines 1060-1063 remains surgical: get the `HoldemHand`, extract the round, call `getTimeoutForRound()`. The null guard `(hhand != null) ? hhand.getRound() : HoldemHand.ROUND_PRE_FLOP` is appropriate since `doBettingTimeoutCheck()` could theoretically be called during a transient state.

### Remaining Suggestions (Non-blocking, carried from first review)

1. **Missing `labelborder.advancedtimeout.label` property.** The `DDLabelBorder("advancedtimeout", STYLE)` looks up `labelborder.advancedtimeout.label` for its border title text, which is not defined. The border will display with no title. This follows the same pattern as the scheduled start feature (which also lacks `labelborder.scheduledstart.label`), so it is consistent. However, adding `labelborder.advancedtimeout.label=Advanced Timeout (Per-Street)` would provide a section title like the older late registration feature has. Note that the existing `option.advancedtimeout.label` and `option.advancedtimeout.help` properties are unused -- no `DDOption` component has the name `"advancedtimeout"` (the checkbox uses `"advancedtimeout.show"`).

2. **Add MIN_TIMEOUT enforcement test.** As noted above, a test for values between 1 and MIN_TIMEOUT-1 would explicitly verify the new enforcement logic.

3. **HTML trailing space.** Carried from first review -- the `StringBuilder` approach in `TournamentProfileHtml.toHTMLOnline()` can leave a trailing space after the last per-street entry.

4. **Separate unrelated refactoring.** The working tree still includes `Bet.java` and `PokerUtils.java` changes (extracting to `BetValidator` and `PokerLogicUtils`). These are functionally correct delegation refactorings but are unrelated to the per-street timeout feature. They should be committed separately. The `Bet.java` refactoring of `betRaise()` is behaviorally equivalent (the `BetValidationResult.needsRounding()` check is identical to the original `nNewAmount != nAmount` check).

### Verification

- **Tests:** The handoff reports 1,089/1,089 passed. The 6 per-street timeout tests are well-structured and cover the important paths. The 3 blocking runtime issues from the first review are now fixed.
- **Privacy:** SAFE. No private data in any changed files.
- **Security:** No concerns. All timeout values are bounded and validated.
