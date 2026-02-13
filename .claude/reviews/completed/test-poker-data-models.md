# Code Review: test-poker-data-models

## Branch Info
- **Branch:** test-poker-data-models
- **Base:** main
- **Type:** Test coverage improvement
- **Reviewer:** @opus (automated)

## Summary

Added comprehensive test coverage for 4 poker data model classes in the main poker package:

- **HandStatTest** (26 tests) - Hand performance tracking with expectation calculations
- **PreFlopBiasTest** (24 tests) - AI pre-flop hand weight calculation
- **DeckProfileTest** (23 tests) - Deck profile management for custom card images
- **StatResultsTest** (19 tests) - Simulation results collection with HTML formatting

**Total:** 92 new tests, all passing

## Goals

1. Improve main poker package test coverage from 16% → 50%+
2. Test pure data model classes with high testability scores (80-100)
3. Establish reliable test patterns for future poker package testing

## What to Review

### Test Quality
- [ ] Test names follow should_ExpectedBehavior_WhenCondition pattern
- [ ] Tests are focused and test one behavior each
- [ ] Edge cases are covered (null, empty, boundary values)
- [ ] Floating-point comparisons use `isCloseTo()` not `isEqualTo()`
- [ ] Real object instantiation used instead of mocking where possible

### Test Coverage
- [ ] HandStatTest covers: constructors, record(), getExpectation(), fixExpectation(), noise management, sorting
- [ ] PreFlopBiasTest covers: premium hands, position-based weights, tightness blending, suited vs offsuit
- [ ] DeckProfileTest covers: setFile() name parsing, canDelete() logic, Swing FileFilter behavior
- [ ] StatResultsTest covers: Map operations, HTML generation with different key types, insertion order

### Code Patterns
- [ ] All tests use JUnit 5 (`@BeforeEach`, no public modifiers)
- [ ] All tests use AssertJ (`assertThat()`)
- [ ] ConfigManager initialized in @BeforeEach for all tests
- [ ] No use of Mockito (learned from Java 25 compatibility issues)

### Specific Areas

**HandStatTest:**
- Uses `HandSorted` with Card objects (e.g., `Card.SPADES_A`, `Card.SPADES_K`)
- Tests static noise_ field management with lowerNoise()
- Floating-point precision handled with `isCloseTo(value, within(0.001))`

**PreFlopBiasTest:**
- Tests all position constants (EARLY, MIDDLE, LATE, BUTTON, SMALL, BIG)
- Validates tightness blending between matrices (loose, avg, tight)
- Verifies card order doesn't affect results

**DeckProfileTest:**
- Uses `@TempDir` for file system testing
- Tests "card-" prefix removal logic
- Tests file size filtering (35KB limit)
- Only tests public API (DeckFileFilter is private)

**StatResultsTest:**
- Tests LinkedHashMap behavior (insertion order, null keys)
- HTML encoding verified (< becomes &lt;)
- Mixed key types (HandGroup, String, Object)

## Known Issues / Decisions

1. **DeckFileFilter tests removed:** DeckFileFilter is a private inner class, so tests focus on public API (DeckFilter)
2. **Floating-point precision:** Changed from `isEqualTo()` to `isCloseTo()` for all double comparisons
3. **HandSorted constructor:** Uses Card objects (e.g., `Card.SPADES_A`) not int rank values
4. **toString() format:** HandStat shows "[K A]*" not "AK", tests adjusted accordingly

## Testing Done

```bash
cd code/poker
mvn test -Dtest=HandStatTest,PreFlopBiasTest,DeckProfileTest,StatResultsTest -P dev
# Tests run: 92, Failures: 0, Errors: 0, Skipped: 0
```

All tests pass cleanly.

## Next Steps After Approval

1. Merge to main
2. Verify coverage improvement with full test run
3. Consider additional data model classes if coverage < 50%

## Questions for Reviewer

1. Are test names clear and descriptive?
2. Is the test structure (sections with comments) helpful?
3. Should we add more edge case tests for any class?
4. Any concerns with the pattern of using real objects vs mocks?
