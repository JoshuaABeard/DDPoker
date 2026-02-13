# Refactoring Plan: TournamentProfile Testability Improvement

## Context

### The Problem
TournamentProfile is a "God Object" at **1,926 lines** with only **30% test coverage**. It conflates multiple responsibilities:
- Tournament configuration storage (60+ parameters)
- Complex business logic (payout distribution, blind validation)
- Serialization (network/XML)
- Validation and normalization

This monolithic design makes it extremely difficult to test. To reach our **80% coverage goal** for the pokerengine module, we must refactor TournamentProfile to extract testable components.

### Why This Matters
- **Current module coverage: 55%** (need 80%)
- **TournamentProfile alone blocks 25%** of the coverage gap
- Complex algorithms (setAutoSpots: 137 lines, fixLevels: 228 lines) are completely untested
- Any bugs in payout calculation or blind validation affect real money tournaments

### Business Value
- Enables confident changes to tournament rules
- Reduces risk of payout calculation errors
- Makes onboarding new developers easier (smaller, focused classes)
- Supports future features (dynamic blind structures, custom payout curves)

## Goal

Extract complex business logic from TournamentProfile into independently testable components while maintaining 100% backward compatibility with existing serialization, UI, and network protocols.

**Target:** Increase coverage from 30% → 80% through 4 phased extractions.

## Strategy

Use the **Facade Pattern** - TournamentProfile remains the public API and delegates to specialized components. All data stays in `DMTypedHashMap map_` for serialization compatibility.

### Why This Works
1. **Zero API breakage** - All existing callers continue to work
2. **Gradual migration** - Extract high-value algorithms first
3. **Incremental testing** - Achieve coverage gains after each phase
4. **Low risk** - Each extraction is independently tested before integration

## Status

- ✅ **Phase 1 Complete** (2026-02-13): PayoutDistributionCalculator & LevelValidator extracted
  - Coverage gain: 30% → ~50% (estimated)
  - 37 new tests added
  - 346 lines removed from TournamentProfile (-18%)
  - All 73 tests passing
  - Code reviewed and approved by Opus

- ✅ **Phase 2 Complete** (2026-02-13): BlindStructure extracted
  - 16 new tests added
  - Removed 35-line getAmount() method from TournamentProfile
  - All 89 tests passing
  - Maintains 100% backward compatibility

- ✅ **Phase 3 Complete** (2026-02-13): PayoutCalculator extracted
  - 16 new tests added
  - Removed 82 lines of complex payout logic from TournamentProfile
  - All 105 tests passing
  - Maintains 100% backward compatibility

- ✅ **Phase 4 Complete** (2026-02-13): Integration tests and edge cases
  - 11 new tests added (TournamentProfileTest: 22 → 33)
  - All 116 tests passing across pokerengine module
  - Comprehensive integration testing of extracted components
  - Max payout constraints, edge cases, serialization round-trips

## Phases

### Phase 1: Extract Complex Algorithms ✅ COMPLETE

**Coverage Gain:** 30% → 50-55% (+20-25%)
**Effort:** 3-4 sessions
**Risk:** Low (pure functions)

#### 1.1 PayoutDistributionCalculator ✅
Extracted setAutoSpots() - 137-line Fibonacci-based payout distribution algorithm.
- Created with 17 comprehensive tests
- Integrated into TournamentProfile.setAutoSpots()

#### 1.2 LevelValidator ✅
Extracted fixLevels() - 228-line level validation and normalization logic.
- Created with 20 comprehensive tests
- Integrated into TournamentProfile.fixLevels()

**Results:**
- TournamentProfile: 1,926 → 1,580 lines (-346 lines, -18%)
- Tests: 22 → 59 (+37 new tests for extracted algorithms)
- All behavioral differences corrected after code review

---

### Phase 2: Extract Blind Structure Domain Logic ✅ COMPLETE

**Coverage Gain:** 50-55% → 65-70% (+15-20%) (not yet measured)
**Effort:** 1 session
**Risk:** Medium (many callers) - mitigated by comprehensive tests

Extracted blind/ante access logic with automatic doubling:
- Created BlindStructure with 16 comprehensive tests
- Integrated into TournamentProfile (blinds() helper method)
- Removed getAmount() method (35 lines)
- All 89 tests passing

---

### Phase 3: Extract PayoutCalculator ✅ COMPLETE

**Coverage Gain:** 65-70% → 75-80% (+10-15%) (not yet measured)
**Effort:** 1 session
**Risk:** Medium (complex conditionals) - mitigated by comprehensive tests

Extracted payout calculation logic across all modes:
- Created PayoutCalculator with 16 comprehensive tests
- Centralizes getNumSpots(), getPayout(), getPoolAfterHouseTake()
- Handles SPOTS, PERC, and SATELLITE modes
- Satellite remainder logic, percent first-place adjustment, house take modes
- Integrated into TournamentProfile (payouts() helper method)
- Removed 82 lines of complex payout logic
- All 105 tests passing

---

### Phase 4: Consolidate and Achieve 80% ✅ COMPLETE

**Coverage Gain:** Target 75-80% → 80-85% (not yet measured)
**Effort:** 1 session
**Risk:** Low (integration tests only)

Added comprehensive integration tests for TournamentProfile:
- Created 11 new tests for edge cases and integration scenarios
- Max payout constraint testing (getMaxPayoutSpots, getMaxPayoutPercent)
- Edge case testing (zero players, single player tournaments)
- Prize pool and payout integration tests
- Serialization round-trip tests for payout settings
- Component integration test (verifies all extracted components work together)

**Results:**
- TournamentProfileTest: 22 → 33 tests (+11 new tests)
- Total pokerengine tests: 116 passing
- All integration tests verify extracted components maintain backward compatibility
- Tests cover PayoutCalculator, BlindStructure, and PayoutDistributionCalculator integration

## Critical Files

**Created in Phase 1:**
- `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/PayoutDistributionCalculator.java`
- `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/LevelValidator.java`
- `code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/PayoutDistributionCalculatorTest.java`
- `code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/LevelValidatorTest.java`

**Created in Phase 2:**
- `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/BlindStructure.java`
- `code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/BlindStructureTest.java`

**Created in Phase 3:**
- `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/PayoutCalculator.java`
- `code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/PayoutCalculatorTest.java`

**Modified in Phases 1-3:**
- `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/TournamentProfile.java`

## Success Metrics

| Metric | Baseline | Phase 1 Actual | Phase 2 Actual | Phase 3 Actual | Phase 4 Actual |
|--------|----------|----------------|----------------|----------------|----------------|
| **Coverage** | 30% | ~50% | TBD | TBD | TBD (measurement pending) |
| **Test Count** | ~22 | 59 | 75 | 91 | 116 (11 integration + 105 from Phase 3) |
| **TournamentProfile Lines** | 1,926 | 1,580 | 1,610 | ~1,530 | ~1,530 (stable) |
| **TournamentProfileTest Tests** | 22 | 22 | 22 | 22 | 33 (+11 Phase 4 tests) |

## Rollback Plan

If a phase encounters issues:

1. **Revert Extraction:**
   - Keep new test files (they document expected behavior)
   - Revert TournamentProfile changes
   - Mark new classes as `@Deprecated` until issues resolved

2. **Partial Acceptance:**
   - Merge working extractors only
   - Defer problematic extractors to next iteration

3. **Coverage Threshold:**
   - Don't increase `pom.xml` threshold until phase complete
   - Each phase can be independently merged
