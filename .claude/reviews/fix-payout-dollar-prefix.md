# Review Request

**Branch:** fix-payout-dollar-prefix
**Worktree:** ../DDPoker-fix-payout-dollar-prefix
**Plan:** N/A (single-file bug fix)
**Requested:** 2026-02-22

## Summary

`PayoutCalculator.getSpot()` used `Double.parseDouble()` on spot-amount strings, but
`TournamentProfile.setAutoSpots()` stores amounts with a `$` prefix (e.g. `"$180"`) via
`FORMAT_AMOUNT = new MessageFormat("${0}")`. The `NumberFormatException` was silently
swallowed and 0 returned, causing every player — including the winner — to be awarded $0
in prize money at the end of a tournament.

Fix: strip the leading `$` before parsing.

## Files Changed

- [ ] `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/PayoutCalculator.java` — strip `$` prefix in `getSpot()` before `Double.parseDouble()`
- [ ] `code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/PayoutCalculatorTest.java` — add regression test using dollar-prefixed spot amounts

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** All pokerengine tests passed
- **Build:** Clean (Spotless check passed)

## Context & Decisions

The original `TournamentProfile.getSpotFromString()` used `Utils.parseStringToDouble()`
which strips all non-numeric characters. When `PayoutCalculator` was extracted from
`TournamentProfile`, it replicated the read logic with `Double.parseDouble()` directly
and lost that robustness.

Only the `$` prefix (at index 0) needs stripping; the stored format is always `"$NNN"`.
A more general "strip all non-numeric" approach would also work but `startsWith("$")`
is precise and matches the exact format written by `setAutoSpots()`.

---

## Review Results

**Status:** APPROVED

**Reviewed by:** Claude Opus 4.6 (review agent)
**Date:** 2026-02-22

### Findings

#### ✅ Strengths

- **Root cause correctly identified and fixed.** `TournamentProfile.setAutoSpots()` stores amounts via `FORMAT_AMOUNT = new MessageFormat("${0}")`, producing strings like `"$180"`. The original `TournamentProfile.getSpotFromString()` used `Utils.parseStringToDouble()` which strips all non-numeric characters (keeping only `0-9`, `-`, `.`). When `PayoutCalculator` was extracted, it used `Double.parseDouble()` directly, which chokes on the `$` prefix. The fix correctly strips the leading `$` before parsing.

- **Surgical change.** Only 3 lines of logic added plus a Javadoc update. No unrelated changes.

- **Good regression test.** `should_ReturnSpotAmount_WhenAmountStoredWithDollarPrefix` directly reproduces the bug scenario with dollar-prefixed values and asserts correct parsing for all three payout positions.

- **Existing non-prefixed tests preserved.** The `should_ReturnSpotAmount_WhenModeIsSpots` test continues to verify that plain numeric strings (without `$`) still parse correctly, confirming no regression.

- **Javadoc accurately documents the "why".** The added doc on `getSpot()` explains the `$` prefix origin (`FORMAT_AMOUNT` / `setAutoSpots()`), which will help future maintainers.

#### ⚠️ Suggestions (Non-blocking)

- **Consider using `Utils.parseStringToDouble()` instead.** The original `TournamentProfile.getSpotFromString()` used `Utils.parseStringToDouble(s, ROUND_MULT)` which strips ALL non-numeric characters (keeping `0-9`, `-`, `.`). This would handle not just `$` but also any other unexpected characters (e.g., commas in `"$1,000"` if localization ever changes the format, or stray whitespace). The current `startsWith("$")` approach is correct for the known format but less robust. That said, the current approach is precise, well-documented, and matches the exact format produced by `setAutoSpots()`. The `ROUND_MULT` rounding behavior in `parseStringToDouble` would also need consideration. This is non-blocking -- the fix is correct for the actual data format.

- **Note on TournamentProfile.getSpotFromString() bounds checking.** The original `getSpotFromString()` clamps values to `[0, MAX_BLINDANTE]`. `PayoutCalculator.getSpot()` does not apply these bounds. This is a pre-existing difference from the extraction, not introduced by this fix, so it is out of scope. Mentioning for awareness.

#### ❌ Required Changes (Blocking)

None.

### Verification

- Tests: 17/17 passed (PayoutCalculatorTest), including the new dollar-prefix regression test
- Coverage: Not separately measured; existing coverage infrastructure applies
- Build: Clean (Spotless check passed)
- Privacy: SAFE -- no private information in changed files
- Security: No security concerns -- change is purely string parsing in a local calculator
