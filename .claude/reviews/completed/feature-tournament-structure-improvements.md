# Review Request: Tournament Structure Improvements

## Review Request

**Branch:** feature-tournament-structure-improvements
**Worktree:** ../DDPoker-feature-tournament-structure-improvements
**Plan:** .claude/reviews/fix-phase3-medium-priority.md
**Requested:** 2026-02-13 08:05

## Summary

Implemented 4 medium-priority features to streamline tournament configuration: profile validation warnings, standard payout presets, blind level quick setup with templates, and bounty/knockout tournament support. These features address pain points in tournament setup (manual blind entry, limited payout options, no bounty support) and add validation to prevent problematic configurations.

## Files Changed

### Feature #14: Profile Validation Warnings
- [x] `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/ValidationWarning.java` - NEW: Enum for warning types (unreachable levels, too many payouts, shallow depth, excessive house take)
- [x] `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/ValidationResult.java` - NEW: Container for validation warnings with messages
- [x] `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/ProfileValidator.java` - Added 4 validation methods with thresholds
- [x] `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/TournamentProfile.java` - Added validateProfile() method
- [x] `code/poker/src/main/java/com/donohoedigital/games/poker/TournamentProfileDialog.java` - Integrated validation in DetailsTab.isValidCheck()
- [x] `code/poker/src/main/resources/config/poker/client.properties` - Added warning message properties
- [x] `code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/ProfileValidatorTest.java` - 27 tests covering all validation cases

### Feature #12: Standard Payout Presets
- [x] `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/PayoutPreset.java` - NEW: Enum with TOP_HEAVY/STANDARD/FLAT presets
- [x] `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/TournamentProfile.java` - Added setSpot() method
- [x] `code/poker/src/main/java/com/donohoedigital/games/poker/TournamentProfileDialog.java` - Added preset dropdown in createAlloc()
- [x] `code/poker/src/main/resources/config/poker/client.properties` - Added preset label properties
- [x] `code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/PayoutPresetTest.java` - NEW: 18 tests for all presets

### Feature #11: Blind Level Quick Setup
- [x] `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/BlindTemplate.java` - NEW: Enum with SLOW/STANDARD/TURBO/HYPER templates, generateLevels() logic
- [x] `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/TournamentProfile.java` - Added clearAllLevels() and setLevel() methods
- [x] `code/poker/src/main/java/com/donohoedigital/games/poker/BlindQuickSetupDialog.java` - NEW: Dialog with template selection, level count, break options, preview
- [x] `code/poker/src/main/java/com/donohoedigital/games/poker/TournamentProfileDialog.java` - Added Quick Setup button to LevelsPanel
- [x] `code/poker/src/main/resources/config/poker/gamedef.xml` - Registered BlindQuickSetup phase
- [x] `code/poker/src/main/resources/config/poker/client.properties` - Added template label properties
- [x] `code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/BlindTemplateTest.java` - NEW: 17 tests for templates, progression, breaks

### Feature #13: Bounty/Knockout Support
- [x] `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/TournamentProfile.java` - Added MAX_BOUNTY, PARAM_BOUNTY/PARAM_BOUNTY_AMOUNT, bounty methods
- [x] `code/poker/src/main/java/com/donohoedigital/games/poker/PokerPlayer.java` - Added nBountyCollected_/nBountyCount_ fields, addBounty() method, marshalling
- [x] `code/poker/src/main/java/com/donohoedigital/games/poker/TournamentProfileDialog.java` - Added bounty UI section, updated displayPrizePool()
- [x] `code/poker/src/main/java/com/donohoedigital/games/poker/HoldemHand.java` - Added bounty award logic in resolvePot()
- [x] `code/poker/src/main/resources/config/poker/client.properties` - Added bounty message properties
- [x] `code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/BountyTest.java` - NEW: 9 tests for bounty settings

### Debug/Temporary Files (DO NOT MERGE)
- [ ] `code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/Debug2.java` - REMOVE before merge
- [ ] `code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/DebugBlindTest.java` - REMOVE before merge

**Privacy Check:**
- ✅ SAFE - No private information found
- All test data uses generic names/values
- No API keys, credentials, or personal data

## Verification Results

- **Tests:** 1129/1129 passed (100%)
  - 27 new tests for validation warnings
  - 18 new tests for payout presets
  - 17 new tests for blind templates
  - 9 new tests for bounty settings
- **Coverage:** Not measured (TBD)
- **Build:** Clean, no warnings

## Context & Decisions

**Implementation Order:**
Features were implemented sequentially (#14 → #12 → #11 → #13) to avoid merge conflicts in TournamentProfileDialog, which all features modify.

**Key Design Decisions:**

1. **Validation (Feature #14):**
   - Warnings are non-blocking (logged but don't prevent saving)
   - Hard validation still blocks (e.g., total payout != 100%)
   - Thresholds chosen based on common tournament best practices

2. **Payout Presets (Feature #12):**
   - Distributions chosen from real-world tournament standards
   - TOP_HEAVY: ~50% winner (common in small fields)
   - STANDARD: ~25-30% winner (balanced)
   - FLAT: ~15-20% winner (large fields)

3. **Blind Templates (Feature #11):**
   - Progression rates: 1.5x (SLOW) vs 2.0x (STANDARD/TURBO/HYPER)
   - Time differentiators: 20/15/10/5 min
   - Smart rounding ensures readable blind values (25/50 not 23/47)
   - Break insertion: every N blind levels (not total levels)
   - Antes start at level 5 (industry standard)

4. **Bounties (Feature #13):**
   - Bounty awarded when player reaches 0 chips (not when eliminated from tournament)
   - Split bounty equally among multiple pot winners
   - Bounty adds to both nBountyCollected_ and nPrize_ (total winnings)
   - Award happens in HoldemHand.resolvePot() after chips distributed

**Test Strategy:**
- Each feature has comprehensive unit tests before implementation
- Tests cover edge cases (min/max values, boundary conditions)
- No integration tests yet (manual testing required)

**Known Limitations:**
- Bounty logic only tested via unit tests, needs manual tournament testing
- No UI tests for dialogs
- Debug test files accidentally committed (need removal)

---

## Review Results

**Status:** CHANGES REQUIRED

**Reviewed by:** Claude Opus 4.6
**Date:** 2026-02-13

### Findings

#### Strengths

1. **Clean separation of concerns.** New model classes (ValidationWarning, ValidationResult, ProfileValidator, PayoutPreset, BlindTemplate) are all in the `pokerengine` module, keeping engine logic independent of UI. Only UI integration lives in the `poker` module.

2. **Thorough test coverage.** 71 new tests across 4 test files cover happy paths, edge cases, boundary conditions, and error scenarios. The ProfileValidatorTest in particular is well-structured with clear Given/When/Then sections and descriptive test names.

3. **Good use of enums.** PayoutPreset and BlindTemplate are natural enum patterns -- finite, well-defined sets of configurations with behavior attached. ValidationWarning as a simple enum is also appropriate.

4. **Smart blind rounding algorithm.** The `roundBlind()` method in BlindTemplate uses magnitude-aware rounding that produces realistic poker blind values. The progression logic correctly handles antes starting at level 5.

5. **Defensive coding.** Input validation in `generateLevels()` with clear IllegalArgumentException messages. ValidationResult returns unmodifiable collections. `getStartingBlinds()` returns a defensive copy.

6. **Consistent with existing codebase patterns.** The TournamentProfileDialog changes follow existing UI patterns (DDPanel, DDLabelBorder, OptionMenu, GlassButton). The PokerPlayer marshalling additions follow the existing token-based serialization pattern. The gamedef.xml registration follows established phase configuration patterns.

7. **Payout distributions verified.** All three presets (TOP_HEAVY, STANDARD, FLAT) sum to exactly 100%, validated by dedicated tests.

#### Suggestions (Non-blocking)

1. **PayoutPreset.getPercentages() exposes mutable internal array.** The method returns the raw `double[]` reference. Unlike `getStartingBlinds()` in BlindTemplate (which returns `startingBlinds.clone()`), callers could mutate the enum's internal state. While unlikely in practice since it is a UI application, returning `percentages.clone()` would be safer and consistent with BlindTemplate's approach.

2. **PayoutPreset.fromFirstSpot() matching is fragile.** It only checks the first spot's percentage with 0.1 tolerance. Two presets with close first-spot values could collide. Currently safe since the values are well-separated (25, 40, 50), but a multi-spot comparison would be more robust for future presets.

3. **PayoutPreset description mismatch.** STANDARD is described as "~25-30% winner" in the display name, but its actual first-place percentage is 40%. The display name should say "~40% winner" or the distribution should be adjusted. TOP_HEAVY says "~50% winner" (accurate). FLAT says "~15-20% winner" but first place is 25%. These display names are misleading.

4. **BlindQuickSetupDialog preview logic is fragile.** The preview calculation at line 145 (`numLevels + (includeBreaks ? numLevels / breakFreq : 0)`) uses integer division and could undercount or overcount levels depending on break insertion. The preview also caps at 5 displayed levels but uses a loop with a computed upper bound that could miss levels or iterate unnecessarily. Consider using the actual generated profile levels directly.

5. **Validation warnings are only debug-logged, never shown to users.** The TODO comment in DetailsTab.isValidCheck() acknowledges this. While non-blocking for merge, the feature provides no user-visible value until warnings are actually displayed. Consider adding at minimum a tooltip or status bar message.

6. **Bounty splitting uses integer division, losing remainder.** In HoldemHand.java line 2694: `winner.addBounty(bountyAmount / nWinners)` -- if bountyAmount=100 and nWinners=3, each winner gets 33, and 1 chip is lost. The same pattern exists in chip distribution elsewhere in the codebase (handled via nRemainder), but bounties don't have equivalent remainder handling. For small bounty values with multiple winners, this is noticeable.

7. **Bounty award may fire multiple times per eliminated player.** The bounty code runs inside `resolvePot()` which is called once per pot (main pot, side pots). If a player has 0 chips after the main pot is resolved, the side pot resolution would not find them again (they are not in the side pot), so this is likely safe. However, the logic should be verified with multi-pot scenarios.

8. **setBountyAmount() has no range validation.** The getter `getBountyAmount()` clamps to `[0, MAX_BOUNTY]`, but `setBountyAmount(int)` writes the raw value to the map without clamping. This allows storing invalid values like negative numbers or values above MAX_BOUNTY. The UI spinner will constrain input, but programmatic callers are not protected. This is consistent with the existing pattern for other setters in TournamentProfile, so it is not blocking.

9. **PokerPlayer serialization is not backward compatible.** The new `nBountyCollected_` and `nBountyCount_` tokens are inserted between `nNumRebuy_` and `nPendingRebuyAmount_` in the marshal/demarshal methods. Any saved game state from before this change will fail to load because the token order has changed. New tokens should be appended at the end of the serialization sequence, or a version check should be added.

10. **BlindTemplate ante progression compounds.** Once antes start at level 5, the ante is multiplied by the progression factor each subsequent level. This means antes grow at the same rate as blinds. In many real tournaments, antes grow more slowly than blinds. This is a design choice, not a bug, but worth noting.

#### Required Changes (Blocking)

1. **CRITICAL: Remove debug files before merge.** `Debug2.java` and `DebugBlindTest.java` are committed to the branch. These are throwaway debug utilities with `main()` methods and no license headers. They must be deleted before merging.

2. **CRITICAL: PokerPlayer serialization breaks backward compatibility.** The bounty tokens (`nBountyCollected_`, `nBountyCount_`) are inserted in the middle of the existing marshal/demarshal sequence (between `nNumRebuy_` and `nPendingRebuyAmount_`). This will corrupt deserialization of any previously saved game state. The tokens must be appended at the end of the serialization sequence (after `nHandsSitout_` and `bBooted_` in the "Poker 2.0 Patch 8" section), or a version-aware deserialization strategy must be implemented.

3. **Bounty awarded even when bounty comes from external money (not pot).** The current implementation awards the full bounty amount as an in-game chip bonus (`nPrize_ += amount`). However, in real knockout tournaments, the bounty is typically a cash prize from the entry fee, not additional chips. The `nPrize_` addition is correct for tracking total winnings, but the bounty should not add chips to the player's stack (which it does not currently -- `addBounty` only modifies accounting fields, not `nChips_`). This is actually correct behavior on re-inspection. **Downgraded: not blocking.**

### Verification

- **Tests:** 71/71 new tests pass (27 validation + 18 payout + 17 blind + 9 bounty). Verified by running `mvn test -P dev` on the feature branch.
- **Coverage:** Not measured (no coverage profile run). Test coverage for new code appears thorough based on manual review.
- **Build:** Compiles cleanly with no errors. Spotless formatting passes.
- **Privacy:** SAFE -- No private information, API keys, credentials, or personal data found. All test data uses generic values.
- **Security:** No injection vectors identified. Profile validation uses integer comparisons and map lookups. No user-controlled strings are used in dangerous contexts. The bounty amount is bounded by `MAX_BOUNTY=10000` on read (via `getBountyAmount()` clamping).
