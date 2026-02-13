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

### Phase 3: Extract PayoutCalculator (PLANNED)

See plan details in original plan document for Phase 3 and 4 specifications.

## Critical Files

**Created in Phase 1:**
- `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/PayoutDistributionCalculator.java`
- `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/LevelValidator.java`
- `code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/PayoutDistributionCalculatorTest.java`
- `code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/LevelValidatorTest.java`

**Created in Phase 2:**
- `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/BlindStructure.java`
- `code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/BlindStructureTest.java`

**Modified in Phases 1-2:**
- `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/TournamentProfile.java`

## Success Metrics

| Metric | Baseline | Phase 1 Actual | Phase 2 Actual | Phase 3 Target | Phase 4 Target |
|--------|----------|----------------|----------------|----------------|----------------|
| **Coverage** | 30% | ~50% | TBD | 75-80% | 80-85% |
| **Test Count** | ~22 | 59 | 75 (16 BlindStructure + 59 from Phase 1) | ~105 | ~140 |
| **TournamentProfile Lines** | 1,926 | 1,580 | 1,610 | 1,200 | 1,000 |

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
