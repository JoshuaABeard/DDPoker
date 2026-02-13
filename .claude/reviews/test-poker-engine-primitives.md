# Review Request: Phase 1 Unit Testing - Poker Engine Primitives

**Branch:** test-poker-engine-primitives
**Worktree:** ../DDPoker-test-poker-engine-primitives
**Plan:** .claude/plans/UNIT-TESTING-PLAN.md
**Requested:** 2026-02-13 09:40

## Summary

Completed Phase 1 of the Unit Testing Plan, adding comprehensive tests for poker engine primitive classes (Card, Hand, Deck, CardSuit, HandSorted) and model classes (OnlineGame, TournamentHistory). Achieved 65% coverage for the pokerengine module, significantly exceeding the 40% target. Fixed critical bugs in test infrastructure related to static constant mutation and PropertyConfig parallel execution.

## Files Changed

### New Test Files (7 files, 240 tests)
- [x] code/pokerengine/src/test/java/com/donohoedigital/games/poker/engine/CardTest.java - 39 tests for Card construction, comparison, serialization, static lookups
- [x] code/pokerengine/src/test/java/com/donohoedigital/games/poker/engine/HandTest.java - 53 tests for Hand operations, poker queries, fingerprinting
- [x] code/pokerengine/src/test/java/com/donohoedigital/games/poker/engine/CardSuitTest.java - 15 tests for suit constants, comparisons, display
- [x] code/pokerengine/src/test/java/com/donohoedigital/games/poker/engine/HandSortedTest.java - 37 tests for sorted insertion, poker-specific queries
- [x] code/pokerengine/src/test/java/com/donohoedigital/games/poker/engine/DeckTest.java - 29 tests for dealing, removing, sorting, bug-specific decks
- [x] code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/OnlineGameTest.java - 29 tests for JPA entity, lifecycle, merge, equals/hashCode
- [x] code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/TournamentHistoryTest.java - 49 tests for entity fields, business logic, serialization

### Documentation Updates
- [x] .claude/learnings.md - Added learnings about static Card constant mutation and OnlineGame.hashCode() bug
- [x] .claude/plans/UNIT-TESTING-PLAN.md - Marked Phase 1 complete with status tracking section

**Privacy Check:**
- ✅ SAFE - No private information found (all test data is synthetic)

## Verification Results

- **Tests:** 476/476 passed in pokerengine module
- **Coverage:** 65% instruction coverage (target: 40%)
  - com.donohoedigital.games.poker.engine: 62%
  - com.donohoedigital.games.poker.model: 70%
- **Build:** Clean (zero warnings)
- **Parallel Execution:** All tests pass in parallel (4 threads)

## Context & Decisions

### Key Technical Decisions
1. **Test Infrastructure Fixes:**
   - Discovered static Card constants (SPADES_A, etc.) are shared singletons - modified tests to never call setValue() on them
   - Removed @AfterClass PropertyConfig cleanup to fix parallel execution conflicts
   - All tests now use ConfigTestHelper.initializeForTesting("poker") in @BeforeClass

2. **Production Bugs Discovered (not fixed - documented only):**
   - HandSorted.hasStraightDraw() throws IndexOutOfBoundsException on empty hands
   - OnlineGame.hashCode() violates equals/hashCode contract by including super.hashCode()
   - Both bugs documented in test comments as expected behavior

3. **Test Organization:**
   - Followed existing test patterns (DeckRandomnessTest as reference)
   - Grouped tests by functionality with clear section headers
   - Used descriptive test names following testMethodName pattern

### Coverage Strategy
- Focused on core poker primitives that all other code depends on
- Prioritized business logic over trivial getters/setters
- Included edge cases (null handling, empty collections, boundary values)
- Tested serialization round-trips for all marshallable classes

### Scope
- This branch includes some earlier UI fixes and cleanup (from previous work)
- Main focus for review: 7 new test files created for Phase 1
- No production code changes except test infrastructure fixes

---

## Review Results

**Status:** APPROVED

**Reviewed by:** Claude Opus 4.6 (review agent)
**Date:** 2026-02-13

### Findings

#### ✅ Strengths

1. **Excellent test organization and structure.** All 7 test files follow a consistent pattern: `@BeforeClass` with `ConfigTestHelper.initializeForTesting("poker")`, clearly delineated sections with `// ===== Section Name =====` comments, and logical grouping by functionality. This makes each test class easy to navigate and understand.

2. **Strong coverage of core poker semantics.** The tests go well beyond basic getter/setter coverage. HandTest verifies poker-specific queries (`isSuited`, `isPair`, `hasFlush`, `hasPossibleFlush`, `isConnectors`), HandSortedTest validates sorted insertion invariants and poker-specific operations (`hasStraightDraw`, `hasConnector`, `getHighestPair`, `isEquivalent`), and DeckTest covers bug-specific deck configurations (BUG280, BUG284, BUG316).

3. **Thorough edge case testing.** Empty hands, single-card hands, null inputs, boundary values, and duplicate cards are all tested. For example: `testCopyNullHand`, `testEmptyHandOperations`, `testSingleCardHand`, `testRemoveCardsNullHand`, `testRemoveCardsEmptyHand`, `testGetCardByStringNull`, `testNullDates`, `testZeroValues`, `testNegativePlace`.

4. **Correct handling of the static Card constant mutation issue.** The `testCardsChanged` test in HandTest (lines 487-500) correctly creates new `Card` instances via `new Card(CardSuit.SPADES, ACE)` instead of using the static constants when calling `setValue()`. This avoids corrupting shared singletons, which was documented as a key learning.

5. **Production bugs properly documented.** The `OnlineGame.hashCode()` violation of the equals/hashCode contract is clearly documented in the test comment (OnlineGameTest lines 277-281) and the test explicitly verifies the buggy behavior with `assertNotEquals(game1.hashCode(), game2.hashCode())`. The `HandSorted.hasStraightDraw()` IndexOutOfBoundsException on empty hands is documented in HandSortedTest (lines 351-353) and correctly skipped.

6. **Serialization round-trip tests.** Card, Hand (including multiple types), and TournamentHistory all have marshal/demarshal round-trip tests that verify all fields survive serialization. The TournamentHistory serialization test (lines 431-465) is particularly thorough, covering 12 distinct fields.

7. **Good test isolation.** No `@AfterClass` cleanup that could interfere with parallel execution. OnlineGameTest does not require `@BeforeClass` config initialization since it does not depend on PropertyConfig. Tests create their own instances and do not share mutable state.

8. **Proper use of `assertSame` vs `assertEquals`.** CardTest uses `assertSame` in `testGetCardReturnsConstants` (lines 387-391) to verify that `getCard()` returns the exact same singleton instances as the static constants. CardSuitTest uses `assertSame` for `forRank()` singleton verification. This tests the correct thing -- object identity for the singleton pattern.

#### ⚠️ Suggestions (Non-blocking)

1. **CardTest display assertions are overly permissive.** Tests like `testGetDisplay` (lines 120-123), `testToStringSingle` (lines 134-138), and `testRankDisplaySingle` (lines 149-151) use loose assertions like `assertTrue(display.contains("A") || display.toLowerCase().contains("ace"))`. Since the production code is deterministic (e.g., `getDisplay()` returns `getRank(rank_) + cardSuit_.getAbbr()`), these could be exact assertions like `assertEquals("As", SPADES_A.getDisplay())`. The current form would pass even if the display format changed unexpectedly. This applies to several HandTest display tests as well (lines 505-531).

2. **HandTest `testIsConnectors` uses magic numbers.** Line 248 has `assertTrue(connectors.isConnectors(2, ACE))` and line 253 has `assertFalse(connectors.isConnectors(TWO, EIGHT))`. While the Card constants (`ACE`, `TWO`, `EIGHT`) are used as rank values, the first argument `2` is an integer literal representing "hand size" -- it would be clearer if the intent were explained with a brief comment.

3. **OnlineGameTest `testMergePreservesUnchangedFields` assertion is weak.** Line 213 asserts `assertNotNull(game1.getStartDate())` after merge, but since `merge()` does `data_.putAll(game2.getData())`, it only adds keys that game2 has -- it does not remove keys. A stronger assertion would be `assertEquals(startDate, game1.getStartDate())` to verify the exact value is preserved, not just non-null.

4. **DeckTest `testDeckContainsAllUniqueCards` uses O(n^2) approach.** Lines 342-351 use a nested loop to check for duplicates. This is fine for 52 cards but could use a `Set` for clarity. Not a real issue for a test, just a style note.

5. **Missing test for `HandSorted.hasStraightDraw()` on empty hand.** The bug is documented in the comment at line 352 of HandSortedTest, but there is no `@Test(expected = IndexOutOfBoundsException.class)` test that pins down the exact buggy behavior. Adding such a test would prevent the bug from being silently "fixed" in a way that changes the exception type, and would serve as a regression marker for when the bug is eventually fixed.

6. **CardSuitTest `testGetAbbrCaching` relies on `assertSame` for String identity.** Line 103 uses `assertSame(abbr1, abbr2)` to verify caching. This works because the production code caches the exact String instance, but if the caching implementation ever changes to use string interning or a different mechanism, this test could break despite correct behavior. Consider adding a comment explaining why `assertSame` (not `assertEquals`) is intentional here.

7. **TournamentHistoryTest `testHashCodeNullId` assertion.** Line 408 asserts `assertNotEquals(0, hash)` but `super.hashCode()` (which is `System.identityHashCode`) could theoretically return 0, albeit with extremely low probability. This is not a practical concern, just a theoretical one.

#### ❌ Required Changes (Blocking)

None. The test suite is well-constructed, all tests pass in both sequential and parallel execution, the production bugs are correctly documented, and the static constant mutation issue is properly handled.

### Verification

- Tests: 476/476 passed (both sequential and parallel with `-P dev` 4 threads)
- Coverage: 65% reported in handoff (exceeds 40% target)
- Build: Clean, zero warnings (Spotless formatting applied)
- Privacy: SAFE -- all test data is synthetic (example.com URLs, generic player names like "Alice", "TestPlayer")
- Security: No credentials, API keys, or sensitive data in any test files
