# Omaha Poker Implementation Plan

## Context

DDPoker currently supports only Texas Hold'em with three betting structures (No Limit, Pot Limit, Limit). This plan outlines adding **Pot Limit Omaha (PLO)** support as a new game variant while maintaining full backward compatibility with existing Hold'em functionality.

**Why this change**: Pot Limit Omaha is the second most popular poker variant worldwide. Adding PLO support would significantly expand DDPoker's appeal and provide players with variety in tournament formats.

**Current state**:
- Zero Omaha references in codebase - exclusively Hold'em
- Hand evaluation hardcoded for 2 hole cards + 5 community cards
- AI system built around 169 Hold'em starting hand combinations
- UI layout designed for 2-card display

**Goal state**:
- Support Pot Limit Omaha (4 hole cards, must use exactly 2+3 rule)
- Maintain 100% backward compatibility with Hold'em
- Provide competent (not expert) Omaha AI
- Allow mixed-game tournaments (Hold'em + Omaha)

## Architecture Overview

**Strategy Pattern for Hand Evaluation**:
```
HandEvaluator (interface)
    ├── HoldemHandEvaluator (existing logic, 2 hole + 5 board)
    └── OmahaHandEvaluator (new logic, exactly 2 from 4 hole + 3 from 5 board)

HandEvaluatorFactory.getEvaluator(gameType) → dispatches to correct evaluator
```

**Game Type Dispatch Pattern**:
- All game-specific logic checks `PokerConstants.isOmaha(gameType)` vs `isHoldem(gameType)`
- Default behavior preserved for Hold'em (backward compatibility)
- New code paths only activated when gameType == TYPE_POT_LIMIT_OMAHA

**Key Insight**: Much of the codebase is already generic:
- ✅ Hand class supports variable card counts
- ✅ DealDisplay animation iterates hand.size()
- ✅ Network protocol serializes any Hand size
- ✅ Pot limit calculation already game-agnostic
- ❌ Hand evaluation assumes 2 hole cards (BLOCKER)
- ❌ AI uses 13x13 Hold'em starting hand matrix (BLOCKER)
- ❌ UI layout only defines 2 hole card positions (BLOCKER)

## TDD Methodology (CRITICAL)

**Following CLAUDE.md guidelines**: All implementation MUST follow strict Test-Driven Development.

### TDD Workflow (RED-GREEN-REFACTOR)

**For EVERY feature in EVERY phase**:

1. **🔴 RED**: Write a failing test FIRST
   - Test defines expected behavior
   - Run test → verify it fails (no implementation yet)
   - Commit the failing test: `git commit -m "RED: Add test for [feature]"`

2. **🟢 GREEN**: Write minimal code to pass
   - Implement just enough to make test pass
   - No extra features, no speculation
   - Run test → verify it passes
   - Commit passing implementation: `git commit -m "GREEN: Implement [feature]"`

3. **♻️ REFACTOR**: Clean up while keeping tests green
   - Simplify, remove duplication, improve readability
   - Run tests after each refactor → verify still passing
   - Commit refactoring: `git commit -m "REFACTOR: Simplify [component]"`

4. **🔁 REPEAT**: Next requirement, back to RED

### TDD Benefits for This Project

- **Hand evaluation correctness**: Tests define correct Omaha rules before implementation
- **Regression prevention**: Hold'em tests ensure backward compatibility
- **Incremental development**: Each test is a small, verifiable step
- **Documentation**: Tests serve as executable specification

### Test Coverage Requirements

| Phase | Minimum Coverage | Focus |
|-------|-----------------|-------|
| Phase 1 | 100% | Simple utility methods |
| Phase 2 | 95% | Hand evaluation logic (CRITICAL) |
| Phase 3 | 85% | Game flow integration |
| Phase 4 | Visual verification | UI layout correctness |
| Phase 5 | 80% | AI decision making |
| Phase 6 | Full E2E | Complete game simulation |

**Enforce**: `mvn clean install` must pass with coverage thresholds before proceeding to next phase.

---

## Implementation Phases

### Phase 1: Foundation - Game Type Infrastructure (2-3 days, LOW risk)

**Goal**: Add Omaha constants and utility methods without breaking existing functionality.

**Critical Files**:
- `C:\Repos\DDPoker\code\pokerengine\src\main\java\com\donohoedigital\games\poker\engine\PokerConstants.java`

---

#### TDD Workflow for Phase 1

**Step 1.1: 🔴 RED - Write test for isOmaha()**

**NEW FILE**: `C:\Repos\DDPoker\code\pokerengine\src\test\java\com\donohoedigital\games\poker\engine\PokerConstantsTest.java`

```java
@Test
void should_ReturnTrue_When_CheckingPotLimitOmaha() {
    // Given: Pot Limit Omaha game type
    int gameType = PokerConstants.TYPE_POT_LIMIT_OMAHA;

    // When: Checking if it's Omaha
    boolean isOmaha = PokerConstants.isOmaha(gameType);

    // Then: Should return true
    assertTrue(isOmaha);
}

@Test
void should_ReturnFalse_When_CheckingHoldemAsOmaha() {
    // Given: No Limit Hold'em game type
    int gameType = PokerConstants.TYPE_NO_LIMIT_HOLDEM;

    // When: Checking if it's Omaha
    boolean isOmaha = PokerConstants.isOmaha(gameType);

    // Then: Should return false
    assertFalse(isOmaha);
}
```

**Run test**: `mvn test -Dtest=PokerConstantsTest` → **FAILS** (method doesn't exist yet)

**Commit**: `git commit -m "RED: Add test for PokerConstants.isOmaha()"`

---

**Step 1.2: 🟢 GREEN - Implement isOmaha()**

**MODIFY**: `PokerConstants.java`

```java
// Add constants first (tests will reference them)
public static final int TYPE_POT_LIMIT_OMAHA = 4;
public static final int TYPE_NO_LIMIT_OMAHA = 5;
public static final int TYPE_LIMIT_OMAHA = 6;

public static final String DE_POT_LIMIT_OMAHA = "potlimitomaha";
public static final String DE_NO_LIMIT_OMAHA = "nolimitomaha";
public static final String DE_LIMIT_OMAHA = "limitomaha";

// Implement method
public static boolean isOmaha(int gameType) {
    return gameType == TYPE_POT_LIMIT_OMAHA ||
           gameType == TYPE_NO_LIMIT_OMAHA ||
           gameType == TYPE_LIMIT_OMAHA;
}
```

**Run test**: `mvn test -Dtest=PokerConstantsTest` → **PASSES**

**Commit**: `git commit -m "GREEN: Implement PokerConstants.isOmaha()"`

---

**Step 1.3: 🔴 RED - Write test for isHoldem()**

**ADD TO**: `PokerConstantsTest.java`

```java
@Test
void should_ReturnTrue_When_CheckingNoLimitHoldem() {
    assertTrue(PokerConstants.isHoldem(PokerConstants.TYPE_NO_LIMIT_HOLDEM));
}

@Test
void should_ReturnFalse_When_CheckingOmahaAsHoldem() {
    assertFalse(PokerConstants.isHoldem(PokerConstants.TYPE_POT_LIMIT_OMAHA));
}
```

**Run test** → **FAILS**

**Commit**: `git commit -m "RED: Add test for PokerConstants.isHoldem()"`

---

**Step 1.4: 🟢 GREEN - Implement isHoldem()**

```java
public static boolean isHoldem(int gameType) {
    return gameType == TYPE_NO_LIMIT_HOLDEM ||
           gameType == TYPE_POT_LIMIT_HOLDEM ||
           gameType == TYPE_LIMIT_HOLDEM;
}
```

**Run test** → **PASSES**

**Commit**: `git commit -m "GREEN: Implement PokerConstants.isHoldem()"`

---

**Step 1.5: 🔴 RED - Write test for getHoleCardCount()**

```java
@Test
void should_ReturnFour_When_GetHoleCardCountForOmaha() {
    assertEquals(4, PokerConstants.getHoleCardCount(PokerConstants.TYPE_POT_LIMIT_OMAHA));
    assertEquals(4, PokerConstants.getHoleCardCount(PokerConstants.TYPE_NO_LIMIT_OMAHA));
    assertEquals(4, PokerConstants.getHoleCardCount(PokerConstants.TYPE_LIMIT_OMAHA));
}

@Test
void should_ReturnTwo_When_GetHoleCardCountForHoldem() {
    assertEquals(2, PokerConstants.getHoleCardCount(PokerConstants.TYPE_NO_LIMIT_HOLDEM));
    assertEquals(2, PokerConstants.getHoleCardCount(PokerConstants.TYPE_POT_LIMIT_HOLDEM));
    assertEquals(2, PokerConstants.getHoleCardCount(PokerConstants.TYPE_LIMIT_HOLDEM));
}
```

**Run test** → **FAILS**

**Commit**: `git commit -m "RED: Add test for PokerConstants.getHoleCardCount()"`

---

**Step 1.6: 🟢 GREEN - Implement getHoleCardCount()**

```java
public static int getHoleCardCount(int gameType) {
    return isOmaha(gameType) ? 4 : 2;
}
```

**Run test** → **PASSES**

**Commit**: `git commit -m "GREEN: Implement PokerConstants.getHoleCardCount()"`

---

**Step 1.7: ♻️ REFACTOR - Review and clean up**

- Check for duplication (none found - methods are minimal)
- Ensure consistent naming (verified)
- Run all tests → **PASSES**

**No refactoring needed** - code is already minimal per TDD principles.

---

**Phase 1 Verification Checklist**:
- ✅ All new tests pass (100% coverage of new code)
- ✅ All existing tests pass (zero regression)
- ✅ `mvn clean install` succeeds
- ✅ Code coverage report shows 100% for PokerConstants new methods

**Phase 3 Verification Checklist**:
- ✅ All dealing tests pass (Omaha deals 4, Hold'em deals 2)
- ✅ All existing Hold'em tests pass (zero regression)
- ✅ `mvn clean install` succeeds
- ✅ TournamentProfile properly stores and retrieves game type

**Completion Criteria**: Cannot proceed to Phase 4 until all tests are green

---

### Phase 2: Hand Evaluation Core - Omaha Evaluator (8-10 days, HIGH risk)

**Goal**: Implement Omaha hand evaluation with "exactly 2 hole + 3 board" rule.

**Critical Algorithm**: Omaha requires evaluating C(4,2) × C(5,3) = 6 × 10 = 60 possible 5-card combinations per player, then selecting the best.

**TDD CRITICAL**: This is the highest-risk phase. Every test must be written BEFORE implementation. Hand evaluation bugs are catastrophic.

---

#### TDD Workflow for Phase 2 (Detailed)

**Step 2.1: 🔴 RED - Define HandEvaluator interface with test**

**NEW FILE**: `C:\Repos\DDPoker\code\poker\src\test\java\com\donohoedigital\games\poker\HandEvaluatorTest.java`

```java
@Test
void should_EvaluateHandInfo_When_CallingEvaluate() {
    // Given: A hand evaluator and test cards
    HandEvaluator evaluator = new MockHandEvaluator();
    Hand holeCards = createTestHand(HEARTS_A, HEARTS_K);
    Hand community = createTestHand(HEARTS_Q, HEARTS_J, HEARTS_T, SPADES_2, CLUBS_3);

    // When: Evaluating the hand
    HandInfo result = evaluator.evaluate(null, holeCards, community);

    // Then: Should return non-null HandInfo
    assertNotNull(result);
}
```

**Run test** → **FAILS** (HandEvaluator doesn't exist yet)

**Commit**: `git commit -m "RED: Add test for HandEvaluator interface"`

---

**Step 2.2: 🟢 GREEN - Create HandEvaluator interface**

**NEW FILE**: `C:\Repos\DDPoker\code\poker\src\main\java\com\donohoedigital\games\poker\HandEvaluator.java`

```java
public interface HandEvaluator {
    /**
     * Evaluate best 5-card poker hand from hole cards and community cards.
     * Implementation depends on game variant rules.
     */
    HandInfo evaluate(PokerPlayer player, Hand holeCards, Hand community);

    /**
     * Get score for hand combination (for comparison)
     */
    int getScore(Hand holeCards, Hand community);

    /**
     * Future Hi-Lo support: Evaluate best qualifying low hand.
     * Returns null if no qualifying low exists or variant doesn't support low.
     */
    default HandInfo evaluateLow(PokerPlayer player, Hand holeCards, Hand community) {
        return null; // No low hand by default
    }

    /**
     * Returns true if this evaluator supports low hand evaluation (for Hi-Lo variants)
     */
    default boolean supportsLowHand() {
        return false;
    }
}
```

**Create MockHandEvaluator** for test:
```java
class MockHandEvaluator implements HandEvaluator {
    public HandInfo evaluate(PokerPlayer player, Hand hole, Hand community) {
        return new HandInfo(player, new Hand(), 0);
    }
    public int getScore(Hand hole, Hand community) { return 0; }
}
```

**Run test** → **PASSES**

**Commit**: `git commit -m "GREEN: Create HandEvaluator interface with Hi-Lo abstraction"`

**Note**: Added `evaluateLow()` and `supportsLowHand()` methods with default implementations for future Omaha Hi-Lo support (as discussed).

**Step 2.2: Refactor Existing HandInfo**

**Strategy**: Keep `HandInfo` as facade for backward compatibility, extract core logic into `HoldemHandEvaluator`.

**NEW FILE**: `C:\Repos\DDPoker\code\poker\src\main\java\com\donohoedigital\games\poker\HoldemHandEvaluator.java`
- Copy existing `HandInfo.categorize()` method (lines 156-292)
- Keep current scoring logic (already works for 2+5=7 cards)
- HandInfo delegates to HoldemHandEvaluator internally

**Step 2.3: 🔴 RED - Write critical Omaha rule tests FIRST**

**NEW FILE**: `C:\Repos\DDPoker\code\poker\src\test\java\com\donohoedigital\games\poker\OmahaHandEvaluatorTest.java`

**CRITICAL TEST 1: Board-only flush must NOT count**

```java
@Test
void should_IgnoreBoardOnlyFlush_When_PlayerHasOnlyOneCardOfSuit() {
    // Given: Player has only 1 spade in hole cards
    Hand holeCards = createHand(
        SPADES_A,   // Only 1 spade
        HEARTS_K,
        DIAMONDS_Q,
        CLUBS_J
    );

    // Board has 4 spades (flush on board)
    Hand community = createHand(
        SPADES_9,
        SPADES_8,
        SPADES_7,
        SPADES_6,
        HEARTS_2
    );

    OmahaHandEvaluator evaluator = new OmahaHandEvaluator();

    // When: Evaluating the hand
    HandInfo result = evaluator.evaluate(null, holeCards, community);

    // Then: Should NOT be a flush (must use exactly 2 hole cards)
    // Best hand is ACE HIGH (A-K-9-8-7) not flush
    assertTrue(result.getScore() < HandInfo.FLUSH * HandInfo.BASE,
        "Board-only flush must not count in Omaha");
}
```

**CRITICAL TEST 2: Must use exactly 2 hole cards**

```java
@Test
void should_UseExactlyTwoHoleCards_When_Evaluating() {
    // Given: Board has royal flush available
    Hand holeCards = createHand(
        SPADES_A,
        SPADES_K,
        HEARTS_2,
        CLUBS_3
    );

    Hand community = createHand(
        SPADES_Q,
        SPADES_J,
        SPADES_T,
        HEARTS_5,
        DIAMONDS_7
    );

    OmahaHandEvaluator evaluator = new OmahaHandEvaluator();

    // When: Evaluating
    HandInfo result = evaluator.evaluate(null, holeCards, community);

    // Then: Should be ROYAL FLUSH (using A♠ K♠ from hole + Q♠ J♠ T♠ from board)
    assertEquals(HandInfo.ROYAL_FLUSH, result.getHandType());
}
```

**CRITICAL TEST 3: Cannot use 1 or 3 hole cards**

```java
@Test
void should_IgnoreBoardOnlyStraight_When_PlayerCannotUseExactlyTwo() {
    // Given: Board makes straight without player's help
    Hand holeCards = createHand(
        HEARTS_A,   // High card
        HEARTS_K,   // High card
        CLUBS_2,    // Low card (can't help straight)
        CLUBS_3     // Low card (can't help straight)
    );

    Hand community = createHand(
        SPADES_9,
        HEARTS_8,
        DIAMONDS_7,
        CLUBS_6,
        SPADES_5   // 9-8-7-6-5 straight on board
    );

    OmahaHandEvaluator evaluator = new OmahaHandEvaluator();

    // When: Evaluating
    HandInfo result = evaluator.evaluate(null, holeCards, community);

    // Then: Should be ACE HIGH (not straight - can't use board alone)
    assertEquals(HandInfo.HIGH_CARD, result.getHandType());
}
```

**Run tests** → **FAIL** (OmahaHandEvaluator doesn't exist yet)

**Commit**: `git commit -m "RED: Add critical Omaha rule tests (exactly 2+3)"`

---

**Step 2.4: 🟢 GREEN - Implement OmahaHandEvaluator**

**NEW FILE**: `C:\Repos\DDPoker\code\poker\src\main\java\com\donohoedigital\games\poker\OmahaHandEvaluator.java`

**Minimal implementation to pass tests**:
```java
public class OmahaHandEvaluator implements HandEvaluator {

    @Override
    public HandInfo evaluate(PokerPlayer player, Hand holeCards, Hand community) {
        // Validate Omaha requirements
        ApplicationError.assertTrue(holeCards.size() == 4,
            "Omaha requires exactly 4 hole cards");
        ApplicationError.assertTrue(community.size() >= 3 && community.size() <= 5,
            "Community must have 3-5 cards");

        int bestScore = 0;
        Hand bestHand = null;

        // Generate C(4,2) = 6 combinations of hole cards
        for (int h1 = 0; h1 < 4; h1++) {
            for (int h2 = h1 + 1; h2 < 4; h2++) {

                // Generate C(5,3) = 10 combinations of community cards
                for (int c1 = 0; c1 < community.size(); c1++) {
                    for (int c2 = c1 + 1; c2 < community.size(); c2++) {
                        for (int c3 = c2 + 1; c3 < community.size(); c3++) {

                            // Create 5-card hand: exactly 2 hole + exactly 3 board
                            Hand candidate = new Hand();
                            candidate.addCard(holeCards.getCard(h1));
                            candidate.addCard(holeCards.getCard(h2));
                            candidate.addCard(community.getCard(c1));
                            candidate.addCard(community.getCard(c2));
                            candidate.addCard(community.getCard(c3));

                            // Evaluate using existing 5-card scoring
                            int score = evaluateFiveCardHand(candidate);
                            if (score > bestScore) {
                                bestScore = score;
                                bestHand = candidate;
                            }
                        }
                    }
                }
            }
        }

        return new HandInfo(player, bestHand, bestScore);
    }

    private int evaluateFiveCardHand(Hand fiveCards) {
        // Reuse HandInfoFaster for 5-card evaluation
        // Already optimized and tested
    }
}
```

**Step 2.4: Factory Pattern**

**NEW FILE**: `C:\Repos\DDPoker\code\poker\src\main\java\com\donohoedigital\games\poker\HandEvaluatorFactory.java`
```java
public class HandEvaluatorFactory {
    private static final HoldemHandEvaluator holdemEvaluator = new HoldemHandEvaluator();
    private static final OmahaHandEvaluator omahaEvaluator = new OmahaHandEvaluator();

    public static HandEvaluator getEvaluator(int gameType) {
        if (PokerConstants.isOmaha(gameType)) {
            return omahaEvaluator;
        } else {
            return holdemEvaluator;
        }
    }
}
```

**Step 2.5: Integration Point**

**MODIFY**: `C:\Repos\DDPoker\code\poker\src\main\java\com\donohoedigital\games\poker\HoldemHand.java`

**At showdown logic** (around lines 2863-2895):
```java
// OLD:
info = player.getHandInfo(); // implicitly uses HandInfo with 2+5

// NEW:
int gameType = getGameType();
HandEvaluator evaluator = HandEvaluatorFactory.getEvaluator(gameType);
info = evaluator.evaluate(player, player.getHand(), getCommunity());
player.setHandInfo(info);
```

**Step 2.5: 🔴 RED - Add more comprehensive test cases**

**ADD TO**: `OmahaHandEvaluatorTest.java`

**Test all hand rankings work in Omaha**:

```java
@Test
void should_EvaluateNutFlush_When_TwoSuitedHoleCardsAndThreeBoardSuited() {
    Hand holeCards = createHand(SPADES_A, SPADES_K, HEARTS_2, CLUBS_3);
    Hand community = createHand(SPADES_Q, SPADES_J, SPADES_T, HEARTS_5, DIAMONDS_7);

    HandInfo result = new OmahaHandEvaluator().evaluate(null, holeCards, community);

    assertEquals(HandInfo.ROYAL_FLUSH, result.getHandType());
}

@Test
void should_EvaluateStraight_When_TwoConnectedHoleCardsAndThreeBoardConnected() {
    Hand holeCards = createHand(HEARTS_9, DIAMONDS_8, CLUBS_2, SPADES_3);
    Hand community = createHand(SPADES_7, HEARTS_6, DIAMONDS_5, CLUBS_K, HEARTS_A);

    HandInfo result = new OmahaHandEvaluator().evaluate(null, holeCards, community);

    assertEquals(HandInfo.STRAIGHT, result.getHandType());
}

@Test
void should_EvaluateFullHouse_When_PocketPairAndTripsOnBoard() {
    Hand holeCards = createHand(HEARTS_A, DIAMONDS_A, CLUBS_2, SPADES_3);
    Hand community = createHand(SPADES_A, HEARTS_K, DIAMONDS_K, CLUBS_K, HEARTS_Q);

    HandInfo result = new OmahaHandEvaluator().evaluate(null, holeCards, community);

    assertEquals(HandInfo.FULL_HOUSE, result.getHandType());
}
```

**Edge cases**:

```java
@Test
void should_HandleFourOfSameRankInHole_When_TwoOfRankOnBoard() {
    // Given: Player has AAAA in hole
    Hand holeCards = createHand(HEARTS_A, DIAMONDS_A, CLUBS_A, SPADES_A);
    Hand community = createHand(HEARTS_K, DIAMONDS_K, CLUBS_Q, SPADES_J, HEARTS_T);

    // When: Evaluating (can only use 2 aces from hole)
    HandInfo result = new OmahaHandEvaluator().evaluate(null, holeCards, community);

    // Then: Should be TWO_PAIR (AA from hole + KK from board)
    assertEquals(HandInfo.TWO_PAIR, result.getHandType());
}

@Test
void should_HandleAllCombinations_When_FourHoleCardsFiveBoardCards() {
    Hand holeCards = createHand(HEARTS_A, DIAMONDS_K, CLUBS_Q, SPADES_J);
    Hand community = createHand(HEARTS_T, DIAMONDS_9, CLUBS_8, SPADES_7, HEARTS_6);

    // Should evaluate all C(4,2) × C(5,3) = 60 combinations
    HandInfo result = new OmahaHandEvaluator().evaluate(null, holeCards, community);

    assertNotNull(result);
    assertTrue(result.getScore() > 0);
}
```

**Performance benchmark**:

```java
@Test
void should_Complete60Evaluations_InUnder5ms() {
    Hand holeCards = createHand(HEARTS_A, DIAMONDS_K, CLUBS_Q, SPADES_J);
    Hand community = createHand(HEARTS_T, DIAMONDS_9, CLUBS_8, SPADES_7, HEARTS_6);

    OmahaHandEvaluator evaluator = new OmahaHandEvaluator();

    long startTime = System.currentTimeMillis();
    evaluator.evaluate(null, holeCards, community);
    long endTime = System.currentTimeMillis();

    long duration = endTime - startTime;
    assertTrue(duration < 5, "Evaluation took " + duration + "ms, expected <5ms");
}
```

**Run tests** → Some **PASS** (from Step 2.4 implementation), others **FAIL** (edge cases not handled yet)

**Commit**: `git commit -m "RED: Add comprehensive Omaha hand evaluation tests"`

---

**Step 2.6: 🟢 GREEN - Complete OmahaHandEvaluator implementation**

**Ensure all 60 combinations are evaluated correctly** - refine implementation to pass all tests.

**Run all tests** → **PASSES**

**Commit**: `git commit -m "GREEN: Complete OmahaHandEvaluator with all edge cases"`

---

**Step 2.7: ♻️ REFACTOR - Optimize and clean up**

Extract combination generation to separate methods for clarity:

```java
private List<Hand> generateTwoCardCombos(Hand fourCards) {
    // Returns C(4,2) = 6 combinations
}

private List<Hand> generateThreeCardCombos(Hand fiveCards) {
    // Returns C(5,3) = 10 combinations
}
```

**Run tests after refactoring** → **PASSES**

**Commit**: `git commit -m "REFACTOR: Extract combination generation methods"`

**Verification**:
- All 60 Omaha combinations evaluated correctly
- Omaha-specific rules enforced (exactly 2+3)
- No regression in Hold'em evaluation (all existing HandInfoTest cases pass)
- Performance target: <5ms per Omaha hand evaluation

---

### Phase 3: Game Logic - Deal 4 Cards for Omaha (3-4 days, MEDIUM risk)

**Goal**: Modify dealing logic to deal 4 hole cards for Omaha, 2 for Hold'em.

**TDD Emphasis**: Write tests for dealing behavior BEFORE modifying game logic.

---

#### TDD Workflow for Phase 3

**Step 3.1: 🔴 RED - Test tournament profile stores game type**

**NEW FILE**: `TournamentProfileTest.java` (or add to existing)

```java
@Test
void should_StorePotLimitOmaha_When_SetGameType() {
    TournamentProfile profile = new TournamentProfile();

    profile.setGameType(PokerConstants.TYPE_POT_LIMIT_OMAHA);

    assertEquals(PokerConstants.TYPE_POT_LIMIT_OMAHA, profile.getGameType());
}

@Test
void should_DefaultToNoLimitHoldem_When_NewProfile() {
    TournamentProfile profile = new TournamentProfile();

    assertEquals(PokerConstants.TYPE_NO_LIMIT_HOLDEM, profile.getGameType());
}
```

**Run test** → **FAILS**

**Commit**: `git commit -m "RED: Add test for TournamentProfile game type storage"`

---

**Step 3.2: 🟢 GREEN - Add gameType to TournamentProfile**

**MODIFY**: `C:\Repos\DDPoker\code\pokerengine\src\main\java\com\donohoedigital\games\poker\model\TournamentProfile.java`

```java
private int gameType_ = PokerConstants.TYPE_NO_LIMIT_HOLDEM; // default for backward compatibility

public int getGameType() { return gameType_; }
public void setGameType(int type) { gameType_ = type; }

// Add to serialization methods (DataMarshal)
```

**Run test** → **PASSES**

**Commit**: `git commit -m "GREEN: Add gameType field to TournamentProfile"`

---

**Step 3.3: 🔴 RED - Test dealing behavior**

**NEW FILE**: `HoldemHandDealingTest.java` (or add to existing)

```java
@Test
void should_DealTwoCards_When_GameTypeIsHoldem() {
    // Given: No Limit Hold'em game
    HoldemHand hand = createHoldemHandWithGameType(PokerConstants.TYPE_NO_LIMIT_HOLDEM);

    // When: Dealing hole cards
    hand.dealHoleCards();

    // Then: Each player should have 2 cards
    for (PokerPlayer player : hand.getPlayers()) {
        assertEquals(2, player.getHand().size());
    }
}

@Test
void should_DealFourCards_When_GameTypeIsOmaha() {
    // Given: Pot Limit Omaha game
    HoldemHand hand = createHoldemHandWithGameType(PokerConstants.TYPE_POT_LIMIT_OMAHA);

    // When: Dealing hole cards
    hand.dealHoleCards();

    // Then: Each player should have 4 cards
    for (PokerPlayer player : hand.getPlayers()) {
        assertEquals(4, player.getHand().size());
    }
}

@Test
void should_DealCorrectNumberToAllPlayers_When_TenPlayerOmaha() {
    // Given: 10-player Omaha game
    HoldemHand hand = createHoldemHandWithGameType(PokerConstants.TYPE_POT_LIMIT_OMAHA);
    hand.addPlayers(10); // Add 10 players

    // When: Dealing
    hand.dealHoleCards();

    // Then: All 10 players should have 4 cards each
    assertEquals(10, hand.getPlayers().size());
    for (PokerPlayer player : hand.getPlayers()) {
        assertEquals(4, player.getHand().size());
    }
}
```

**Run tests** → **FAIL** (still dealing 2 cards for all game types)

**Commit**: `git commit -m "RED: Add tests for Omaha dealing (4 cards)"`

---

**Step 3.4: 🟢 GREEN - Modify dealing logic**

**MODIFY**: `C:\Repos\DDPoker\code\poker\src\main\java\com\donohoedigital\games\poker\HoldemHand.java`

**Line 304 currently**: `dealCards(2);`

**Change to**:
```java
int numHoleCards = PokerConstants.getHoleCardCount(getGameType());
dealCards(numHoleCards);
```

**Verify** `dealCards(int)` method (line 546) already handles variable counts generically.

**Run tests** → **PASSES**

**Commit**: `git commit -m "GREEN: Modify dealing to support Omaha 4-card hands"`

---

**Step 3.5: ♻️ REFACTOR - Verify backward compatibility**

**Run ALL existing tests**: `mvn test`

**Expected**: All Hold'em tests still pass (no regression)

**If any tests fail**: Fix immediately before proceeding

**Commit**: `git commit -m "REFACTOR: Verify backward compatibility with Hold'em"`

**Verification**:
- Omaha games deal 4 cards per player
- Hold'em games still deal 2 cards
- All existing Hold'em tests pass

---

### Phase 4: UI Layout - Display 4 Hole Cards (4-5 days, MEDIUM risk)

**Goal**: Update gameboard and card display to show 4 hole cards for Omaha.

**Step 4.1: Update Gameboard XML**

**MODIFY**: `C:\Repos\DDPoker\code\poker\src\main\resources\config\poker\gameboard.xml`

**Current** (Seat 1, lines 63-69):
```xml
<territory id="6002" name="Seat 1" area="table" type="land" scaleimages="40">
    <point x="834" y="195" type="hole1"/>
    ...
</territory>
```

**Add for each of 10 seats**:
```xml
<point x="864" y="195" type="hole2"/>  <!-- +30px offset -->
<point x="894" y="195" type="hole3"/>  <!-- +60px offset -->
<point x="924" y="195" type="hole4"/>  <!-- +90px offset -->
```

**Note**: Card width ~88px at scaleimages="40". May need visual adjustment after testing.

**Step 4.2: Update CardPiece Constants**

**MODIFY**: `C:\Repos\DDPoker\code\poker\src\main\java\com\donohoedigital\games\poker\CardPiece.java`

**Add after line 63**:
```java
public static final String POINT_HOLE2 = "hole2";
public static final String POINT_HOLE3 = "hole3";
public static final String POINT_HOLE4 = "hole4";
```

**Step 4.3: Verify DealDisplay Logic**

**CHECK**: `C:\Repos\DDPoker\code\poker\src\main\java\com\donohoedigital\games\poker\DealDisplay.java`

**Current code** (lines 284-289) already iterates `hand.size()`:
```java
for (c = 0; c < nCnt; c++)
{
    displayCard(context_, player, c, true, nCardDelay_);
}
```

**Should already work** for 4 cards if `displayCard()` maps index to point names generically.

**Investigate**: Verify `displayCard()` uses dynamic point mapping like `"hole" + (cardIndex + 1)`.

**Tests** (Manual/Visual):
- Create Omaha game, verify 4 cards display at all 10 seats
- Verify no visual overlap
- Verify deal animation works smoothly
- Test card positioning at different seats (especially seats 2, 5, 8 which may have different angles)

**Verification**:
- 4 hole cards display for Omaha at all seats
- 2 hole cards still display for Hold'em
- No overlap or misalignment
- Deal animation correct for both variants

---

### Phase 5: AI Foundation - Omaha Pre-Flop Evaluation (8-10 days, HIGH risk)

**Goal**: Implement competent Omaha AI using hand class grouping system.

**Challenge**: Texas Hold'em has 169 starting hands (13×13 matrix), Omaha has 270,725 combinations (52 choose 4).

**Solution**: Use **hand class grouping** based on Hutchison Point Count system instead of enumeration.

**Step 5.1: Omaha Hand Strength System**

**NEW FILE**: `C:\Repos\DDPoker\code\poker\src\main\java\com\donohoedigital\games\poker\ai\OmahaHandStrength.java`

**Hand Classes** (percentile-based):
1. **Premium**: AAxx with suited/connected cards (top 5%)
2. **Strong High Cards**: AKQJ combinations, double-suited (top 15%)
3. **Rundowns**: Connected like 9876, JT98 with suit potential (top 25%)
4. **Medium Pairs**: QQxx, JJxx with good support (top 35%)
5. **Speculative**: Any two >T with flush draw potential (top 50%)
6. **Trash**: Everything else (fold pre-flop)

**Method**:
```java
public static int getOmahaHandStrength(Hand fourCardHole) {
    // Returns 1-100 score (higher = better)
    int score = 0;

    // Add points for pairs (AA=+20, KK=+16, QQ=+12, etc.)
    // Add points for high cards (A=+8, K=+6, Q=+4)
    // Add points for suitedness (double-suited=+6, single=+3)
    // Add points for connectedness (4-card straight=+4, 3-card=+2)
    // Subtract points for danglers (-5 per low unconnected card)

    return Math.min(100, score);
}
```

**Step 5.2: Modify V1Player Pre-Flop**

**MODIFY**: `C:\Repos\DDPoker\code\poker\src\main\java\com\donohoedigital\games\poker\ai\V1Player.java`

**Current** (line 433): `int sklanskyRank = HoldemExpert.getSklanskyRank(_hole);`

**Add dispatch**:
```java
int handStrength;
if (PokerConstants.isOmaha(getGameType())) {
    handStrength = OmahaHandStrength.getOmahaHandStrength(_hole);
} else {
    int sklanskyRank = HoldemExpert.getSklanskyRank(_hole);
    handStrength = convertSklanskyToStrength(sklanskyRank); // map to 1-100
}

// Continue with unified decision logic using handStrength
```

**Step 5.3: Post-Flop AI (Simplified)**

**Strategy**: Reuse existing post-flop logic initially, but use `OmahaHandEvaluator` for strength calculation.

**Rationale**:
- Pot odds calculations already game-agnostic
- Outs counting similar between variants
- Main difference is hand evaluation (already handled by Phase 2)

**No major changes needed** if hand evaluation uses `HandEvaluatorFactory.getEvaluator(gameType)`.

**Tests** (`OmahaHandStrengthTest.java` - NEW FILE):
- `should_RankAAKKDoublesuited_AsStrongest()`
- `should_RankAAxx_StrongerThan_KKxx()`
- `should_RankDanglers_AsWeakHands()` (e.g., AA72 rainbow)
- `should_RankRundowns_AsSpeculativeHands()` (e.g., 9876 suited)

**Integration Test** (`OmahaAIIntegrationTest.java` - NEW FILE):
- `should_FoldTrashHands_PreFlop()`
- `should_RaisePremiumHands_PreFlop()`
- `should_NotChaseBoardOnlyFlush_PostFlop()` (CRITICAL Omaha mistake)

**Verification**:
- AI plays reasonable pre-flop strategy (doesn't fold AA, doesn't call with trash)
- AI correctly evaluates post-flop (uses Omaha evaluator)
- No regression in Hold'em AI

---

### Phase 6: Integration & Polish (3-4 days, LOW risk)

**Goal**: Wire everything together, add UI controls, comprehensive end-to-end testing.

**Step 6.1: Tournament Setup UI**

**MODIFY**: Tournament creation dialog (file TBD - needs investigation)

**Add to game type dropdown**:
```
Game Type: [No Limit Hold'em ▼]
           [Pot Limit Hold'em]
           [Limit Hold'em]
           [Pot Limit Omaha]    <-- NEW
```

**Persist to** `TournamentProfile.gameType_` field.

**Step 6.2: Display Labels**

**Update UI** to show game variant:
- Tournament info panel: "Pot Limit Omaha" or "No Limit Hold'em"
- Hand history: Indicate game type per hand

**Step 6.3: Integration Tests**

**NEW FILE**: `C:\Repos\DDPoker\code\poker\src\test\java\com\donohoedigital\games\poker\integration\OmahaIntegrationTest.java`

**Full game simulation**:
```java
@Test
void should_CompleteFullOmahaHand_When_RunningFromDealToShowdown() {
    TournamentProfile profile = new TournamentProfile();
    profile.setGameType(PokerConstants.TYPE_POT_LIMIT_OMAHA);

    // Create table, deal cards, simulate betting rounds
    // Verify showdown uses Omaha evaluation
    // Verify correct winner determination
}
```

**Step 6.4: Backward Compatibility Testing**

**Verify**:
- All existing Hold'em tests pass (zero regression)
- Saved Hold'em tournaments load correctly (default gameType)
- Online games with Hold'em clients still work
- Mixed-game tournaments (Hold'em level 1, Omaha level 2) function correctly

**Step 6.5: Documentation**

**NEW FILE**: `C:\Repos\DDPoker\docs\OMAHA-SUPPORT.md`

**Contents**:
- Overview of Omaha rules (4 hole cards, exactly 2+3 rule)
- How to create an Omaha tournament
- AI strategy notes and limitations
- Known limitations (no 5-card Omaha, no Omaha Hi-Lo yet)

**Verification**:
- Complete Omaha hand playable from deal to showdown
- Tournament setup includes Omaha option
- All existing tests pass
- Documentation complete

---

## Critical Files Summary

**Phase 2 (HIGHEST PRIORITY) - Hand Evaluation**:
- `HandInfo.java` - Understand existing scoring, refactor to HoldemHandEvaluator
- `HandInfoFaster.java` - Reuse for 5-card evaluation in Omaha
- `OmahaHandEvaluator.java` - NEW: Core 60-combination algorithm
- `HandEvaluatorFactory.java` - NEW: Game type dispatch

**Phase 3 - Game Logic**:
- `HoldemHand.java` - Modify dealCards (line 304) and showdown (lines 2863+)
- `TournamentProfile.java` - Add gameType field

**Phase 5 - AI**:
- `V1Player.java` - Dispatch pre-flop logic (line 433)
- `OmahaHandStrength.java` - NEW: Hutchison-based hand strength

**Phase 1 - Foundation**:
- `PokerConstants.java` - Add Omaha constants and utilities

**Phase 4 - UI**:
- `gameboard.xml` - Add hole2/3/4 points for all 10 seats
- `CardPiece.java` - Add POINT_HOLE2/3/4 constants

---

## Testing Strategy

**TDD Workflow** (per CLAUDE.md):
1. **RED**: Write failing test for next feature
2. **GREEN**: Implement minimal code to pass
3. **REFACTOR**: Clean up while keeping tests green
4. **REPEAT**: Next requirement

**Coverage Targets**:
| Phase | Test Type | Priority | Target |
|-------|-----------|----------|--------|
| Phase 1 | Unit | HIGH | 100% |
| Phase 2 | Unit + Property | CRITICAL | 95% |
| Phase 3 | Unit + Integration | HIGH | 85% |
| Phase 4 | Manual + Integration | MEDIUM | Visual |
| Phase 5 | Unit + Integration | HIGH | 80% |
| Phase 6 | Integration + E2E | HIGH | Full sim |

**Property-Based Testing** (Phase 2):
- Verify all 60 combinations explored for each hand
- Cross-reference with PokerStove/Equilab results
- Test with random 4-card hands (100+ iterations)

---

## TDD Discipline Summary

### Non-Negotiable Rules

**NEVER write production code without a failing test first**:
- ❌ BAD: "I'll just quickly add this method and test it later"
- ✅ GOOD: Write test → watch it fail → implement → watch it pass

**Tests define the contract**:
- Tests are the specification of what code should do
- If behavior isn't tested, it doesn't exist
- Omaha "exactly 2+3 rule" MUST have tests proving it works

**Refactor only when green**:
- Never refactor while tests are red
- Run tests after every refactoring step
- If tests fail during refactoring, revert immediately

**Commit discipline**:
- RED commits: Failing test only (no implementation)
- GREEN commits: Minimal implementation to pass
- REFACTOR commits: Code cleanup while tests stay green
- This creates a clear audit trail of TDD process

### Phase-Specific TDD Requirements

**Phase 1 (Foundation)**:
- Every utility method gets 2+ tests (happy path + edge case)
- 100% coverage required (simple code, easy to test)
- Tests must run in < 1 second (fast feedback)

**Phase 2 (Hand Evaluation) - MOST CRITICAL**:
- Write Omaha rule tests BEFORE any implementation
- Test suite must include "known bad" tests (board-only flush must fail)
- Cross-validation: Compare results with online Omaha calculators
- Performance tests: Ensure <5ms evaluation time
- Minimum 95% coverage (core algorithm is safety-critical)

**Phase 3 (Game Logic)**:
- Test dealing behavior for both Hold'em and Omaha
- Regression tests: Ensure Hold'em still works (backward compatibility)
- Integration tests: Full game setup → deal → verify

**Phase 4 (UI Layout)**:
- Manual testing required (visual verification)
- Automated tests for card positioning logic where possible
- Screenshot comparisons for regression (if tooling available)

**Phase 5 (AI)**:
- Test hand strength calculations with known hands
- Test AI doesn't make obvious mistakes (fold AA, call with trash)
- Property-based tests: AI decisions should be deterministic given same inputs

**Phase 6 (Integration)**:
- End-to-end test: Full Omaha hand from deal to showdown
- Backward compatibility suite: All existing Hold'em tests pass
- Mixed-game test: Tournament with both Hold'em and Omaha levels

### Test Organization

**File naming convention**:
- `*Test.java` for unit tests
- `*IntegrationTest.java` for integration tests
- `*E2ETest.java` for end-to-end tests

**Test method naming**:
- Format: `should_<ExpectedBehavior>_When_<Condition>()`
- Example: `should_IgnoreBoardOnlyFlush_When_PlayerHasOnlyOneCardOfSuit()`
- Makes test failures self-documenting

**Test structure** (Given-When-Then):
```java
@Test
void should_DealFourCards_When_GameTypeIsOmaha() {
    // Given: Pot Limit Omaha game
    HoldemHand hand = createOmahaGame();

    // When: Dealing hole cards
    hand.dealHoleCards();

    // Then: Each player should have 4 cards
    assertEquals(4, hand.getPlayer(0).getHand().size());
}
```

### Continuous Integration

**After every commit**:
1. Run: `mvn clean test`
2. Verify: All tests pass
3. Check: Coverage thresholds met
4. If any fail: Fix immediately (don't commit broken code)

**Before proceeding to next phase**:
1. Run: `mvn clean install`
2. Verify: Build succeeds with zero warnings
3. Check: Coverage report meets phase targets
4. Review: All TDD commits follow RED-GREEN-REFACTOR pattern

### What TDD Prevents

**Bugs prevented by TDD in this project**:
- ✅ Board-only flush counting in Omaha (caught by test before implementation)
- ✅ Using 1 or 3 hole cards instead of exactly 2 (caught by test)
- ✅ Dealing wrong number of cards for game type (caught by test)
- ✅ Regression in Hold'em when adding Omaha (caught by existing tests)
- ✅ AI making illogical decisions (caught by decision tests)
- ✅ Performance degradation (caught by benchmark tests)

**Without TDD, these bugs would likely reach production** and be discovered by users, damaging trust and requiring emergency fixes.

### Success Metrics

**TDD is working if**:
- Every feature has tests written first
- Test failures are caught immediately (not in production)
- Refactoring is safe (tests prevent regressions)
- Coverage targets are met for each phase
- Code is simple (minimal code to pass tests)

**TDD is failing if**:
- Tests are written after implementation
- Coverage is below targets
- Tests are green but bugs are found later
- Refactoring breaks tests frequently
- Commits don't follow RED-GREEN-REFACTOR pattern

---

## Verification Strategy

**End-to-End Verification**:
1. Create Pot Limit Omaha tournament
2. Start game with 6 AI players + 1 human
3. Deal hand - verify 4 cards to each player
4. Play through flop, turn, river
5. At showdown, verify:
   - Correct hand evaluation (exactly 2+3 rule)
   - Correct winner determination
   - Pot awarded correctly
6. Verify AI makes reasonable decisions:
   - Doesn't fold premium hands (AA)
   - Doesn't call with trash (2345 rainbow)
   - Doesn't chase board-only flushes post-flop

**Regression Testing**:
- Run full Hold'em test suite after each phase
- Verify backward compatibility: `mvn clean install -Dtest=*Holdem*`
- Test saved tournament files from older versions load correctly

**Performance Benchmarks**:
- Hand evaluation: <5ms per Omaha hand (target)
- Game flow: No noticeable lag during deal or showdown
- Memory: No leaks during extended play sessions

---

## Risk Mitigation

**High Risk: Hand Evaluation Correctness**
- **Mitigation**: Extensive unit tests with known outcomes
- Cross-reference with online calculators
- Property-based testing for exhaustive coverage

**Medium Risk: Performance (60 combinations)**
- **Mitigation**: Benchmark early in Phase 2
- Optimize hot path using HandInfoFaster
- Consider caching if needed (unlikely)

**Medium Risk: UI Layout (4 cards don't fit)**
- **Mitigation**: Test early with mock data
- May need to reduce card size for Omaha
- Option: stagger cards vertically

**Low Risk: AI Quality**
- **Mitigation**: Start conservative (fold more, call less)
- Reuse existing pot odds logic
- Incremental improvement in future releases

---

## Effort Estimation

| Phase | Complexity | Days | Dependencies |
|-------|------------|------|--------------|
| Phase 1 | LOW | 2-3 | None |
| Phase 2 | HIGH | 8-10 | Phase 1 |
| Phase 3 | MEDIUM | 3-4 | Phase 1, 2 |
| Phase 4 | MEDIUM | 4-5 | Phase 3 |
| Phase 5 | HIGH | 8-10 | Phase 2 |
| Phase 6 | LOW | 3-4 | All |
| **Total** | | **28-36** | **(4-6 weeks)** |

**Critical Path**: Phase 1 → Phase 2 → Phase 3 → Phase 6 (minimal working Omaha)

**Parallel Options**: Phase 4 (UI) can develop alongside Phase 5 (AI) after Phase 3 completes.

**Recommended Order**: Sequential (1→2→3→4→5→6) to minimize integration issues.

---

## Success Criteria

✅ **Functional**:
- Pot Limit Omaha tournament playable from start to finish
- Correct 4-card dealing
- Correct hand evaluation (exactly 2+3 rule enforced)
- Competent AI (doesn't make egregious mistakes)

✅ **Quality**:
- Zero regression in Hold'em functionality
- All existing tests pass
- New test coverage meets targets (95%+ for Phase 2)
- Performance targets met (<5ms hand evaluation)

✅ **User Experience**:
- UI displays 4 cards clearly at all seats
- Game type selection obvious in tournament setup
- Mixed-game tournaments supported

✅ **Maintainability**:
- Clean separation between Hold'em and Omaha logic
- Strategy pattern allows future game variants
- Comprehensive documentation for future developers
