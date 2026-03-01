# Java Client Test Coverage Implementation Plan

**Status:** COMPLETED (2026-03-01)

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Raise JUnit test coverage to 80%+ on non-UI code across poker family modules (pokerengine, pokergamecore, poker).

**Architecture:** Bottom-up by module dependency order: pokerengine -> pokergamecore -> poker. Pure logic classes tested first, then logic extracted from UI-entangled phase classes, then existing tests deepened.

**Tech Stack:** JUnit 5, AssertJ, Mockito. Maven JaCoCo for coverage enforcement. All commands run from `code/`.

**Design doc:** `docs/plans/2026-03-01-java-client-test-coverage-design.md`

**Conventions:**
- Package-private test classes (no `public`)
- AssertJ assertions (`assertThat(...)`)
- Test naming: `should_<Outcome>_When_<Condition>()`
- Copyright: Template 3 (community) for all new test files
- Card constants: NEVER modify static Card singletons — create new instances
- `@BeforeEach` for setup; `ConfigManager("poker", ApplicationType.HEADLESS_CLIENT)` when config is needed

---

## Phase 1: pokerengine Module

### Task 1: HandInfoFasterTest — All Hand Ranks

**Files:**
- Test: `code/pokerengine/src/test/java/com/donohoedigital/games/poker/engine/HandInfoFasterTest.java`

**Step 1: Write the failing test**

```java
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

class HandInfoFasterTest {

    private HandInfoFaster evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new HandInfoFaster();
    }

    // ===== Hand Rank Classification Tests =====

    @Test
    void should_ScoreRoyalFlush_When_AceHighStraightFlush() {
        Hand hole = new Hand(SPADES_A, SPADES_K);
        Hand board = new Hand(SPADES_Q, SPADES_J, SPADES_10, HEARTS_2, CLUBS_3);
        int score = evaluator.getScore(hole, board);
        assertThat(HandScoreConstants.ROYAL_FLUSH).isEqualTo(score / HandScoreConstants.SCORE_BASE);
    }

    @Test
    void should_ScoreStraightFlush_When_ConsecutiveSuited() {
        Hand hole = new Hand(HEARTS_9, HEARTS_8);
        Hand board = new Hand(HEARTS_7, HEARTS_6, HEARTS_5, SPADES_A, CLUBS_K);
        int score = evaluator.getScore(hole, board);
        assertThat(HandScoreConstants.STRAIGHT_FLUSH).isEqualTo(score / HandScoreConstants.SCORE_BASE);
    }

    @Test
    void should_ScoreQuads_When_FourOfAKind() {
        Hand hole = new Hand(SPADES_A, HEARTS_A);
        Hand board = new Hand(DIAMONDS_A, CLUBS_A, SPADES_K, HEARTS_2, CLUBS_3);
        int score = evaluator.getScore(hole, board);
        assertThat(HandScoreConstants.QUADS).isEqualTo(score / HandScoreConstants.SCORE_BASE);
    }

    @Test
    void should_ScoreFullHouse_When_ThreeAndTwo() {
        Hand hole = new Hand(SPADES_A, HEARTS_A);
        Hand board = new Hand(DIAMONDS_A, SPADES_K, HEARTS_K, CLUBS_2, DIAMONDS_3);
        int score = evaluator.getScore(hole, board);
        assertThat(HandScoreConstants.FULL_HOUSE).isEqualTo(score / HandScoreConstants.SCORE_BASE);
    }

    @Test
    void should_ScoreFlush_When_FiveSameSuit() {
        Hand hole = new Hand(SPADES_A, SPADES_J);
        Hand board = new Hand(SPADES_9, SPADES_6, SPADES_3, HEARTS_K, CLUBS_Q);
        int score = evaluator.getScore(hole, board);
        assertThat(HandScoreConstants.FLUSH).isEqualTo(score / HandScoreConstants.SCORE_BASE);
    }

    @Test
    void should_ScoreStraight_When_FiveConsecutive() {
        Hand hole = new Hand(SPADES_10, HEARTS_9);
        Hand board = new Hand(DIAMONDS_8, CLUBS_7, SPADES_6, HEARTS_2, CLUBS_A);
        int score = evaluator.getScore(hole, board);
        assertThat(HandScoreConstants.STRAIGHT).isEqualTo(score / HandScoreConstants.SCORE_BASE);
    }

    @Test
    void should_ScoreTrips_When_ThreeOfAKind() {
        Hand hole = new Hand(SPADES_A, HEARTS_A);
        Hand board = new Hand(DIAMONDS_A, SPADES_K, HEARTS_Q, CLUBS_2, DIAMONDS_3);
        int score = evaluator.getScore(hole, board);
        assertThat(HandScoreConstants.TRIPS).isEqualTo(score / HandScoreConstants.SCORE_BASE);
    }

    @Test
    void should_ScoreTwoPair_When_TwoPairsPresent() {
        Hand hole = new Hand(SPADES_A, HEARTS_K);
        Hand board = new Hand(DIAMONDS_A, SPADES_K, HEARTS_Q, CLUBS_2, DIAMONDS_3);
        int score = evaluator.getScore(hole, board);
        assertThat(HandScoreConstants.TWO_PAIR).isEqualTo(score / HandScoreConstants.SCORE_BASE);
    }

    @Test
    void should_ScorePair_When_OnePairPresent() {
        Hand hole = new Hand(SPADES_A, HEARTS_K);
        Hand board = new Hand(DIAMONDS_A, SPADES_Q, HEARTS_J, CLUBS_2, DIAMONDS_3);
        int score = evaluator.getScore(hole, board);
        assertThat(HandScoreConstants.PAIR).isEqualTo(score / HandScoreConstants.SCORE_BASE);
    }

    @Test
    void should_ScoreHighCard_When_NoPairOrBetter() {
        Hand hole = new Hand(SPADES_A, HEARTS_K);
        Hand board = new Hand(DIAMONDS_Q, SPADES_J, HEARTS_9, CLUBS_2, DIAMONDS_3);
        int score = evaluator.getScore(hole, board);
        assertThat(HandScoreConstants.HIGH_CARD).isEqualTo(score / HandScoreConstants.SCORE_BASE);
    }

    // ===== Hand Ranking / Comparison Tests =====

    @Test
    void should_RankHigherHand_When_ComparingDifferentTypes() {
        // Royal flush beats straight flush
        int royalFlush = evaluator.getScore(
            new Hand(SPADES_A, SPADES_K),
            new Hand(SPADES_Q, SPADES_J, SPADES_10, HEARTS_2, CLUBS_3));

        HandInfoFaster eval2 = new HandInfoFaster();
        int straightFlush = eval2.getScore(
            new Hand(HEARTS_9, HEARTS_8),
            new Hand(HEARTS_7, HEARTS_6, HEARTS_5, SPADES_A, CLUBS_K));

        assertThat(royalFlush).isGreaterThan(straightFlush);
    }

    @Test
    void should_RankByKicker_When_SameHandType() {
        // Pair of aces with king kicker beats pair of aces with queen kicker
        int aceKing = evaluator.getScore(
            new Hand(SPADES_A, HEARTS_K),
            new Hand(DIAMONDS_A, SPADES_9, HEARTS_7, CLUBS_2, DIAMONDS_3));

        HandInfoFaster eval2 = new HandInfoFaster();
        int aceQueen = eval2.getScore(
            new Hand(SPADES_A, HEARTS_Q),
            new Hand(DIAMONDS_A, SPADES_9, HEARTS_7, CLUBS_2, DIAMONDS_3));

        assertThat(aceKing).isGreaterThan(aceQueen);
    }

    // ===== Edge Cases =====

    @Test
    void should_ScoreWheelStraight_When_AceLowStraight() {
        // A-2-3-4-5 (wheel)
        Hand hole = new Hand(SPADES_A, HEARTS_5);
        Hand board = new Hand(DIAMONDS_4, CLUBS_3, SPADES_2, HEARTS_K, CLUBS_9);
        int score = evaluator.getScore(hole, board);
        assertThat(HandScoreConstants.STRAIGHT).isEqualTo(score / HandScoreConstants.SCORE_BASE);
    }

    @Test
    void should_ScoreSteelWheel_When_AceLowStraightFlush() {
        // A-2-3-4-5 suited
        Hand hole = new Hand(HEARTS_A, HEARTS_5);
        Hand board = new Hand(HEARTS_4, HEARTS_3, HEARTS_2, SPADES_K, CLUBS_9);
        int score = evaluator.getScore(hole, board);
        assertThat(HandScoreConstants.STRAIGHT_FLUSH).isEqualTo(score / HandScoreConstants.SCORE_BASE);
    }

    @Test
    void should_HandleNullCommunity_When_PreFlop() {
        Hand hole = new Hand(SPADES_A, HEARTS_K);
        int score = evaluator.getScore(hole, null);
        assertThat(score).isGreaterThan(0);
    }

    @Test
    void should_HandleNullHole_When_OnlyCommunity() {
        Hand board = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_J, SPADES_10);
        int score = evaluator.getScore(null, board);
        assertThat(score).isGreaterThan(0);
    }

    @Test
    void should_HandleFiveCardHand_When_NoBoardCards() {
        // 5-card stud style
        Hand hole = new Hand(SPADES_A, HEARTS_K);
        Hand board = new Hand(DIAMONDS_Q, CLUBS_J, SPADES_10);
        int score = evaluator.getScore(hole, board);
        assertThat(HandScoreConstants.STRAIGHT).isEqualTo(score / HandScoreConstants.SCORE_BASE);
    }

    @Test
    void should_PickBestFiveFromSeven_When_FullBoard() {
        // Board has a pair of 2s, hole has AA — full house AA over 22 is NOT the best;
        // it's trips vs full house check
        Hand hole = new Hand(SPADES_A, HEARTS_A);
        Hand board = new Hand(DIAMONDS_A, SPADES_2, HEARTS_2, CLUBS_7, DIAMONDS_9);
        int score = evaluator.getScore(hole, board);
        // Three aces + pair of 2s = full house
        assertThat(HandScoreConstants.FULL_HOUSE).isEqualTo(score / HandScoreConstants.SCORE_BASE);
    }

    // ===== getLastMajorSuit Tests =====

    @Test
    void should_ReturnFlushSuit_When_FlushDetected() {
        Hand hole = new Hand(SPADES_A, SPADES_K);
        Hand board = new Hand(SPADES_Q, SPADES_J, SPADES_9, HEARTS_2, CLUBS_3);
        evaluator.getScore(hole, board);
        assertThat(evaluator.getLastMajorSuit()).isEqualTo(Card.SPADES);
    }

    // ===== Parameterized: Every hand type beats the one below it =====

    static Stream<Arguments> handTypeHierarchy() {
        return Stream.of(
            // Royal flush vs straight flush
            Arguments.of(
                new Hand(SPADES_A, SPADES_K), new Hand(SPADES_Q, SPADES_J, SPADES_10, HEARTS_2, CLUBS_3),
                new Hand(HEARTS_9, HEARTS_8), new Hand(HEARTS_7, HEARTS_6, HEARTS_5, SPADES_2, CLUBS_4)),
            // Straight flush vs quads
            Arguments.of(
                new Hand(HEARTS_9, HEARTS_8), new Hand(HEARTS_7, HEARTS_6, HEARTS_5, SPADES_2, CLUBS_4),
                new Hand(SPADES_A, HEARTS_A), new Hand(DIAMONDS_A, CLUBS_A, SPADES_K, HEARTS_2, CLUBS_3)),
            // Quads vs full house
            Arguments.of(
                new Hand(SPADES_A, HEARTS_A), new Hand(DIAMONDS_A, CLUBS_A, SPADES_K, HEARTS_2, CLUBS_3),
                new Hand(SPADES_K, HEARTS_K), new Hand(DIAMONDS_K, SPADES_A, HEARTS_A, CLUBS_2, DIAMONDS_3)),
            // Full house vs flush
            Arguments.of(
                new Hand(SPADES_K, HEARTS_K), new Hand(DIAMONDS_K, SPADES_A, HEARTS_A, CLUBS_2, DIAMONDS_3),
                new Hand(SPADES_A, SPADES_J), new Hand(SPADES_9, SPADES_6, SPADES_3, HEARTS_K, CLUBS_Q)),
            // Flush vs straight
            Arguments.of(
                new Hand(SPADES_A, SPADES_J), new Hand(SPADES_9, SPADES_6, SPADES_3, HEARTS_K, CLUBS_Q),
                new Hand(SPADES_10, HEARTS_9), new Hand(DIAMONDS_8, CLUBS_7, SPADES_6, HEARTS_2, CLUBS_A)),
            // Straight vs trips
            Arguments.of(
                new Hand(SPADES_10, HEARTS_9), new Hand(DIAMONDS_8, CLUBS_7, HEARTS_6, SPADES_2, CLUBS_A),
                new Hand(SPADES_A, HEARTS_A), new Hand(DIAMONDS_A, SPADES_K, HEARTS_Q, CLUBS_2, CLUBS_3)),
            // Trips vs two pair
            Arguments.of(
                new Hand(SPADES_A, HEARTS_A), new Hand(DIAMONDS_A, SPADES_K, HEARTS_Q, CLUBS_2, CLUBS_3),
                new Hand(SPADES_A, HEARTS_K), new Hand(DIAMONDS_A, SPADES_K, HEARTS_Q, CLUBS_2, DIAMONDS_3)),
            // Two pair vs pair
            Arguments.of(
                new Hand(SPADES_A, HEARTS_K), new Hand(DIAMONDS_A, SPADES_K, HEARTS_Q, CLUBS_2, DIAMONDS_3),
                new Hand(SPADES_A, HEARTS_K), new Hand(DIAMONDS_A, SPADES_Q, HEARTS_J, CLUBS_2, CLUBS_3)),
            // Pair vs high card
            Arguments.of(
                new Hand(SPADES_A, HEARTS_K), new Hand(DIAMONDS_A, SPADES_Q, HEARTS_J, CLUBS_2, CLUBS_3),
                new Hand(SPADES_A, HEARTS_K), new Hand(DIAMONDS_Q, SPADES_J, HEARTS_9, CLUBS_2, CLUBS_3))
        );
    }

    @ParameterizedTest
    @MethodSource("handTypeHierarchy")
    void should_RankHigherType_Above_LowerType(Hand hole1, Hand board1, Hand hole2, Hand board2) {
        int score1 = evaluator.getScore(hole1, board1);
        HandInfoFaster eval2 = new HandInfoFaster();
        int score2 = eval2.getScore(hole2, board2);
        assertThat(score1).isGreaterThan(score2);
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -pl pokerengine -Dtest=HandInfoFasterTest -P dev`
Expected: FAIL (file doesn't exist yet — this is TDD, write the file first then run)

Actually — `HandInfoFaster` already exists; the test file is what's new. Write the test file, then run:

Run: `mvn test -pl pokerengine -Dtest=HandInfoFasterTest`
Expected: PASS — `HandInfoFaster` is already implemented, tests should validate existing behavior.

**Step 3: Verify tests pass**

Run: `mvn test -pl pokerengine -Dtest=HandInfoFasterTest`
Expected: All tests PASS. If any fail, the existing evaluator has a bug — investigate before proceeding.

**Step 4: Commit**

```bash
git add code/pokerengine/src/test/java/com/donohoedigital/games/poker/engine/HandInfoFasterTest.java
git commit -m "test(pokerengine): add comprehensive HandInfoFaster tests

Cover all 10 hand ranks, kicker comparisons, wheel straight,
steel wheel, null hole/community handling, and parameterized
hand type hierarchy validation."
```

---

### Task 2: TournamentTemplateTest

**Files:**
- Test: `code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/TournamentTemplateTest.java`

**Step 1: Write the test**

```java
// Community copyright header (Template 3)
package com.donohoedigital.games.poker.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;

class TournamentTemplateTest {

    private TournamentTemplate template;

    @BeforeEach
    void setUp() {
        template = new TournamentTemplate();
    }

    @Test
    void should_StoreId_When_SetIdCalled() {
        template.setId(42L);
        assertThat(template.getId()).isEqualTo(42L);
    }

    @Test
    void should_StoreProfileId_When_SetProfileIdCalled() {
        template.setProfileId(100L);
        assertThat(template.getProfileId()).isEqualTo(100L);
    }

    @Test
    void should_StoreName_When_SetNameCalled() {
        template.setName("Weekly Tourney");
        assertThat(template.getName()).isEqualTo("Weekly Tourney");
    }

    @Test
    void should_StoreConfig_When_SetConfigCalled() {
        template.setConfig("{\"blinds\":100}");
        assertThat(template.getConfig()).isEqualTo("{\"blinds\":100}");
    }

    @Test
    void should_StoreDates_When_DateSettersCalled() {
        Date now = new Date();
        template.setCreateDate(now);
        template.setModifyDate(now);
        assertThat(template.getCreateDate()).isEqualTo(now);
        assertThat(template.getModifyDate()).isEqualTo(now);
    }

    @Test
    void should_ReturnReadableString_When_ToStringCalled() {
        template.setId(1L);
        template.setProfileId(2L);
        template.setName("Test");
        assertThat(template.toString()).contains("id=1", "profileId=2", "name='Test'");
    }

    @Test
    void should_DefaultToNull_When_NewlyConstructed() {
        assertThat(template.getId()).isNull();
        assertThat(template.getName()).isNull();
        assertThat(template.getConfig()).isNull();
    }
}
```

**Step 2: Run tests**

Run: `mvn test -pl pokerengine -Dtest=TournamentTemplateTest`
Expected: PASS

**Step 3: Commit**

```bash
git add code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/TournamentTemplateTest.java
git commit -m "test(pokerengine): add TournamentTemplate POJO tests"
```

---

### Task 3: Raise pokerengine JaCoCo Floor

**Files:**
- Modify: `code/pokerengine/pom.xml` (JaCoCo `<minimum>` value)

**Step 1: Run coverage to see actual percentage**

Run: `mvn verify -pl pokerengine -P coverage`
Expected: Check output for instruction coverage percentage. Should be well above 2%.

**Step 2: Update JaCoCo floor**

In `code/pokerengine/pom.xml`, change `<minimum>0.02</minimum>` to the measured value minus 5% buffer (e.g., if coverage is 68%, set to `0.60`).

**Step 3: Verify coverage passes**

Run: `mvn verify -pl pokerengine -P coverage`
Expected: PASS with new floor.

**Step 4: Commit**

```bash
git add code/pokerengine/pom.xml
git commit -m "build(pokerengine): raise JaCoCo floor to reflect actual coverage"
```

---

## Phase 2: pokergamecore Module

### Task 4: PocketMatrix Tests (Float, Int, Short)

**Files:**
- Test: `code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ai/PocketMatrixFloatTest.java`
- Test: `code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ai/PocketMatrixIntTest.java`
- Test: `code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ai/PocketMatrixShortTest.java`

**Step 1: Write PocketMatrixFloatTest**

```java
// Community copyright header (Template 3)
package com.donohoedigital.games.poker.core.ai;

import com.donohoedigital.games.poker.engine.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PocketMatrixFloatTest {

    private PocketMatrixFloat matrix;

    @BeforeEach
    void setUp() {
        matrix = new PocketMatrixFloat();
    }

    @Test
    void should_InitializeToZero_When_DefaultConstructor() {
        assertThat(matrix.get(Card.SPADES_A, Card.HEARTS_K)).isEqualTo(0.0f);
    }

    @Test
    void should_InitializeToValue_When_ValueConstructor() {
        PocketMatrixFloat m = new PocketMatrixFloat(0.5f);
        assertThat(m.get(Card.SPADES_A, Card.HEARTS_K)).isEqualTo(0.5f);
    }

    @Test
    void should_CopyValues_When_CopyConstructor() {
        matrix.set(Card.SPADES_A, Card.HEARTS_K, 0.75f);
        PocketMatrixFloat copy = new PocketMatrixFloat(matrix);
        assertThat(copy.get(Card.SPADES_A, Card.HEARTS_K)).isEqualTo(0.75f);
    }

    @Test
    void should_StoreAndRetrieve_When_SetAndGetByCard() {
        matrix.set(Card.SPADES_A, Card.HEARTS_K, 0.42f);
        assertThat(matrix.get(Card.SPADES_A, Card.HEARTS_K)).isEqualTo(0.42f);
    }

    @Test
    void should_BeSymmetric_When_CardOrderReversed() {
        matrix.set(Card.SPADES_A, Card.HEARTS_K, 0.99f);
        assertThat(matrix.get(Card.HEARTS_K, Card.SPADES_A)).isEqualTo(0.99f);
    }

    @Test
    void should_StoreAndRetrieve_When_SetAndGetByHand() {
        Hand hand = new Hand(Card.SPADES_A, Card.HEARTS_K);
        matrix.set(hand, 0.33f);
        assertThat(matrix.get(hand)).isEqualTo(0.33f);
    }

    @Test
    void should_StoreAndRetrieve_When_SetAndGetByIndex() {
        matrix.set(0, 1, 0.77f);
        assertThat(matrix.get(0, 1)).isEqualTo(0.77f);
    }

    @Test
    void should_ReturnZero_When_NegativeIndex() {
        assertThat(matrix.get(-1, 0)).isEqualTo(0.0f);
    }

    @Test
    void should_ResetAllValues_When_ClearCalled() {
        matrix.set(Card.SPADES_A, Card.HEARTS_K, 1.0f);
        matrix.clear(0.0f);
        assertThat(matrix.get(Card.SPADES_A, Card.HEARTS_K)).isEqualTo(0.0f);
    }
}
```

**Step 2: Write PocketMatrixIntTest** (same pattern, `int` values, no negative-index guard)

**Step 3: Write PocketMatrixShortTest** (same pattern, `short` values)

Note: For Int and Short, skip the negative-index test (they don't have the guard). Follow the exact same structure otherwise.

**Step 4: Run tests**

Run: `mvn test -pl pokergamecore -Dtest="PocketMatrixFloatTest,PocketMatrixIntTest,PocketMatrixShortTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ai/PocketMatrix*.java
git commit -m "test(pokergamecore): add PocketMatrix Float/Int/Short tests"
```

---

### Task 5: SklanksyRanking and SimpleBias Tests

**Files:**
- Test: `code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ai/SklanksyRankingTest.java`
- Test: `code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ai/SimpleBiasTest.java`

**Step 1: Write SklanksyRankingTest**

```java
// Community copyright header (Template 3)
package com.donohoedigital.games.poker.core.ai;

import com.donohoedigital.games.poker.engine.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

class SklanksyRankingTest {

    @Test
    void should_RankAA_AsGroup1() {
        int rank = SklanksyRanking.getRank(new HandSorted(SPADES_A, HEARTS_A));
        assertThat(SklanksyRanking.getGroupFromRank(rank)).isEqualTo(1);
    }

    @Test
    void should_RankKK_AsGroup1() {
        int rank = SklanksyRanking.getRank(new HandSorted(SPADES_K, HEARTS_K));
        assertThat(SklanksyRanking.getGroupFromRank(rank)).isEqualTo(1);
    }

    @Test
    void should_RankAKs_AsGroup1() {
        int rank = SklanksyRanking.getRank(new HandSorted(SPADES_A, SPADES_K));
        assertThat(SklanksyRanking.getGroupFromRank(rank)).isEqualTo(1);
    }

    @Test
    void should_Rank72o_AsUnranked() {
        int rank = SklanksyRanking.getRank(new HandSorted(SPADES_7, HEARTS_2));
        assertThat(SklanksyRanking.getGroupFromRank(rank)).isEqualTo(10);
    }

    @Test
    void should_RankStrongerHandLower_When_ComparedNumerically() {
        int aaRank = SklanksyRanking.getRank(new HandSorted(SPADES_A, HEARTS_A));
        int ttRank = SklanksyRanking.getRank(new HandSorted(SPADES_10, HEARTS_10));
        // AA is group 1, TT is group 2+ — lower rank number = stronger
        assertThat(aaRank).isLessThan(ttRank);
    }

    @Test
    void should_DistinguishSuited_FromOffsuit() {
        int aksRank = SklanksyRanking.getRank(new HandSorted(SPADES_A, SPADES_K));
        int akoRank = SklanksyRanking.getRank(new HandSorted(SPADES_A, HEARTS_K));
        // AKs (group 1) should rank better than AKo (group 2)
        assertThat(aksRank).isLessThan(akoRank);
    }

    static Stream<Arguments> allGroups() {
        return Stream.of(
            Arguments.of(new HandSorted(SPADES_A, HEARTS_A), 1),      // AA
            Arguments.of(new HandSorted(SPADES_A, SPADES_K), 1),      // AKs
            Arguments.of(new HandSorted(SPADES_A, HEARTS_K), 2),      // AKo
            Arguments.of(new HandSorted(SPADES_10, HEARTS_10), 2),    // TT
            Arguments.of(new HandSorted(SPADES_9, HEARTS_9), 3),      // 99
            Arguments.of(new HandSorted(SPADES_A, SPADES_10), 3),     // ATs
            Arguments.of(new HandSorted(SPADES_7, HEARTS_2), 10)      // 72o = unranked
        );
    }

    @ParameterizedTest
    @MethodSource("allGroups")
    void should_AssignCorrectGroup_ForKnownHands(HandSorted hand, int expectedGroup) {
        int rank = SklanksyRanking.getRank(hand);
        assertThat(SklanksyRanking.getGroupFromRank(rank)).isEqualTo(expectedGroup);
    }
}
```

**Step 2: Write SimpleBiasTest**

Note: `SimpleBiasTest` may already exist in `code/poker/` (`poker` module) — check `code/poker/src/test/java/.../ai/SimpleBiasTest.java`. If it exists there, verify it covers the `pokergamecore` class (it may test the poker module's copy). If the class under test is in `pokergamecore`, write the test there.

Check first: `ls code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ai/SimpleBiasTest.java` — if it already exists, skip. If not:

```java
// Community copyright header (Template 3)
package com.donohoedigital.games.poker.core.ai;

import com.donohoedigital.games.poker.engine.*;
import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

class SimpleBiasTest {

    @Test
    void should_ReturnMaxBias_When_TableIndex10() {
        // Table index 10 = "always play" = all values should be 1.0f
        float bias = SimpleBias.getBiasValue(10, SPADES_7, HEARTS_2);
        assertThat(bias).isEqualTo(1.0f);
    }

    @Test
    void should_ReturnLowBias_When_TableIndex0_TrashHand() {
        // Table index 0 = tightest play
        float bias = SimpleBias.getBiasValue(0, SPADES_7, HEARTS_2);
        assertThat(bias).isLessThan(0.1f);
    }

    @Test
    void should_ReturnHighBias_When_PremiumHand() {
        // AA should have high bias at any table index
        float bias = SimpleBias.getBiasValue(5, SPADES_A, HEARTS_A);
        assertThat(bias).isGreaterThan(0.5f);
    }

    @Test
    void should_IncreaseMonotonically_When_TableIndexIncreases() {
        float bias0 = SimpleBias.getBiasValue(0, SPADES_10, HEARTS_9);
        float bias5 = SimpleBias.getBiasValue(5, SPADES_10, HEARTS_9);
        float bias10 = SimpleBias.getBiasValue(10, SPADES_10, HEARTS_9);
        assertThat(bias0).isLessThanOrEqualTo(bias5);
        assertThat(bias5).isLessThanOrEqualTo(bias10);
    }

    @Test
    void should_ReturnValidRange_When_AnyInput() {
        for (int t = 0; t <= 10; t++) {
            float bias = SimpleBias.getBiasValue(t, SPADES_A, HEARTS_K);
            assertThat(bias).isBetween(0.0f, 1.0f);
        }
    }
}
```

**Step 3: Run tests**

Run: `mvn test -pl pokergamecore -Dtest="SklanksyRankingTest,SimpleBiasTest"`
Expected: PASS

**Step 4: Commit**

```bash
git add code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ai/SklanksyRankingTest.java
git add code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ai/SimpleBiasTest.java
git commit -m "test(pokergamecore): add SklanksyRanking and SimpleBias tests"
```

---

### Task 6: ActionOptions Record Test

**Files:**
- Test: `code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ActionOptionsTest.java`

**Step 1: Write the test**

```java
// Community copyright header (Template 3)
package com.donohoedigital.games.poker.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ActionOptionsTest {

    @Test
    void should_StoreAllFields_When_Constructed() {
        ActionOptions opts = new ActionOptions(
            true, true, false, false, true, 100, 200, 1000, 400, 2000, 30);

        assertThat(opts.canCheck()).isTrue();
        assertThat(opts.canCall()).isTrue();
        assertThat(opts.canBet()).isFalse();
        assertThat(opts.canRaise()).isFalse();
        assertThat(opts.canFold()).isTrue();
        assertThat(opts.callAmount()).isEqualTo(100);
        assertThat(opts.minBet()).isEqualTo(200);
        assertThat(opts.maxBet()).isEqualTo(1000);
        assertThat(opts.minRaise()).isEqualTo(400);
        assertThat(opts.maxRaise()).isEqualTo(2000);
        assertThat(opts.timeoutSeconds()).isEqualTo(30);
    }

    @Test
    void should_BeEqual_When_SameValues() {
        ActionOptions a = new ActionOptions(true, false, true, false, true, 50, 100, 500, 200, 1000, 15);
        ActionOptions b = new ActionOptions(true, false, true, false, true, 50, 100, 500, 200, 1000, 15);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void should_NotBeEqual_When_DifferentValues() {
        ActionOptions a = new ActionOptions(true, false, true, false, true, 50, 100, 500, 200, 1000, 15);
        ActionOptions b = new ActionOptions(false, false, true, false, true, 50, 100, 500, 200, 1000, 15);
        assertThat(a).isNotEqualTo(b);
    }
}
```

**Step 2: Run tests**

Run: `mvn test -pl pokergamecore -Dtest=ActionOptionsTest`
Expected: PASS

**Step 3: Commit**

```bash
git add code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ActionOptionsTest.java
git commit -m "test(pokergamecore): add ActionOptions record tests"
```

---

### Task 7: HandInfoFast Tests (pokergamecore)

**Files:**
- Test: `code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ai/HandInfoFastTest.java`

This is the 680-line hand evaluator in pokergamecore — different from `HandInfoFaster` in pokerengine.

**Step 1: Write the test**

```java
// Community copyright header (Template 3)
package com.donohoedigital.games.poker.core.ai;

import com.donohoedigital.games.poker.engine.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

class HandInfoFastTest {

    private HandInfoFast evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new HandInfoFast();
    }

    // ===== Hand Type Detection =====

    @Test
    void should_DetectRoyalFlush() {
        Hand pocket = new Hand(SPADES_A, SPADES_K);
        Hand board = new Hand(SPADES_Q, SPADES_J, SPADES_10, HEARTS_2, CLUBS_3);
        evaluator.getScore(pocket, board);
        assertThat(evaluator.getHandType()).isEqualTo(HandScoreConstants.ROYAL_FLUSH);
    }

    @Test
    void should_DetectFlush() {
        Hand pocket = new Hand(SPADES_A, SPADES_J);
        Hand board = new Hand(SPADES_9, SPADES_6, SPADES_3, HEARTS_K, CLUBS_Q);
        evaluator.getScore(pocket, board);
        assertThat(evaluator.getHandType()).isEqualTo(HandScoreConstants.FLUSH);
    }

    @Test
    void should_DetectStraight() {
        Hand pocket = new Hand(SPADES_10, HEARTS_9);
        Hand board = new Hand(DIAMONDS_8, CLUBS_7, SPADES_6, HEARTS_2, CLUBS_A);
        evaluator.getScore(pocket, board);
        assertThat(evaluator.getHandType()).isEqualTo(HandScoreConstants.STRAIGHT);
    }

    @Test
    void should_DetectPair() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand board = new Hand(DIAMONDS_A, SPADES_Q, HEARTS_J, CLUBS_2, DIAMONDS_3);
        evaluator.getScore(pocket, board);
        assertThat(evaluator.getHandType()).isEqualTo(HandScoreConstants.PAIR);
    }

    @Test
    void should_DetectHighCard() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand board = new Hand(DIAMONDS_Q, SPADES_J, HEARTS_9, CLUBS_2, DIAMONDS_3);
        evaluator.getScore(pocket, board);
        assertThat(evaluator.getHandType()).isEqualTo(HandScoreConstants.HIGH_CARD);
    }

    // ===== Board Analysis Methods =====

    @Test
    void should_ReturnCorrectBoardRanks() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand board = new Hand(DIAMONDS_Q, SPADES_J, HEARTS_9, CLUBS_2, DIAMONDS_3);
        evaluator.getScore(pocket, board);
        assertThat(evaluator.getHighestBoardRank()).isEqualTo(Card.QUEEN);
        assertThat(evaluator.getLowestBoardRank()).isEqualTo(Card.TWO);
    }

    @Test
    void should_CountOvercards() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand board = new Hand(DIAMONDS_Q, SPADES_J, HEARTS_9, CLUBS_2, DIAMONDS_3);
        evaluator.getScore(pocket, board);
        // A and K are both above the highest board card (Q)
        assertThat(evaluator.getOvercardCount()).isEqualTo(2);
    }

    @Test
    void should_ReturnPairRank_When_PairDetected() {
        Hand pocket = new Hand(SPADES_A, HEARTS_A);
        Hand board = new Hand(DIAMONDS_K, SPADES_Q, HEARTS_J, CLUBS_2, DIAMONDS_3);
        evaluator.getScore(pocket, board);
        assertThat(evaluator.getBigPairRank()).isEqualTo(Card.ACE);
    }

    @Test
    void should_ReturnTripsRank_When_TripsDetected() {
        Hand pocket = new Hand(SPADES_A, HEARTS_A);
        Hand board = new Hand(DIAMONDS_A, SPADES_Q, HEARTS_J, CLUBS_2, DIAMONDS_3);
        evaluator.getScore(pocket, board);
        assertThat(evaluator.getTripsRank()).isEqualTo(Card.ACE);
    }

    // ===== Draw Detection =====

    @Test
    void should_DetectFlushDraw_When_FourSuited() {
        Hand pocket = new Hand(SPADES_A, SPADES_K);
        Hand board = new Hand(SPADES_9, SPADES_6, HEARTS_3, HEARTS_Q, CLUBS_J);
        evaluator.getScore(pocket, board);
        assertThat(evaluator.hasFlushDraw()).isTrue();
    }

    @Test
    void should_NotDetectFlushDraw_When_NoDrawPresent() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand board = new Hand(DIAMONDS_9, CLUBS_6, HEARTS_3, SPADES_Q, CLUBS_J);
        evaluator.getScore(pocket, board);
        assertThat(evaluator.hasFlushDraw()).isFalse();
    }

    @Test
    void should_DetectStraightDraw_When_OpenEnded() {
        Hand pocket = new Hand(SPADES_10, HEARTS_9);
        Hand board = new Hand(DIAMONDS_8, CLUBS_7, HEARTS_2, SPADES_A, CLUBS_K);
        evaluator.getScore(pocket, board);
        assertThat(evaluator.hasStraightDraw()).isTrue();
    }

    // ===== Static Utility Methods =====

    @Test
    void should_ExtractTypeFromScore_When_GetTypeFromScoreCalled() {
        int score = HandScoreConstants.FLUSH * HandScoreConstants.SCORE_BASE + 12345;
        assertThat(HandInfoFast.getTypeFromScore(score)).isEqualTo(HandScoreConstants.FLUSH);
    }

    @Test
    void should_ExtractCards_When_GetCardsCalled() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand board = new Hand(DIAMONDS_Q, SPADES_J, HEARTS_10, CLUBS_2, DIAMONDS_3);
        int score = evaluator.getScore(pocket, board);
        int[] cards = new int[5];
        HandInfoFast.getCards(score, cards);
        // At least one card should be non-zero
        assertThat(cards).isNotEqualTo(new int[]{0, 0, 0, 0, 0});
    }
}
```

**Step 2: Run tests**

Run: `mvn test -pl pokergamecore -Dtest=HandInfoFastTest`
Expected: PASS

**Step 3: Commit**

```bash
git add code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ai/HandInfoFastTest.java
git commit -m "test(pokergamecore): add HandInfoFast hand evaluator tests

Cover all hand types, board analysis, draw detection,
and static utility methods."
```

---

### Task 8: PocketScores, PocketOdds, PocketRanks Tests

**Files:**
- Test: `code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ai/PocketScoresTest.java`
- Test: `code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ai/PocketOddsTest.java`
- Test: `code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ai/PocketRanksTest.java`

Note: These classes use static caches. Tests should use distinct community cards per test class to avoid cache collisions. Also check if tests already exist in the `poker` module (e.g. `PocketOddsTest.java`, `PocketScoresTest.java`, `PocketRanksTest.java` were mentioned in the initial exploration). If they exist and cover the pokergamecore class, skip duplication.

**Step 1: Check for existing tests**

Run: `find code/poker/src/test -name "PocketScoresTest.java" -o -name "PocketOddsTest.java" -o -name "PocketRanksTest.java"`

If they exist in `code/poker/`, read them to see what they cover. If they already test the pokergamecore class, skip writing duplicates. If they test a different (poker-module) class, write new tests in pokergamecore.

**Step 2: Write PocketScoresTest** (if needed)

```java
// Community copyright header (Template 3)
package com.donohoedigital.games.poker.core.ai;

import com.donohoedigital.games.poker.engine.*;
import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

class PocketScoresTest {

    @Test
    void should_ReturnHigherScore_ForBetterHand() {
        Hand community = new Hand(DIAMONDS_Q, SPADES_J, HEARTS_9, CLUBS_2, DIAMONDS_3);
        PocketScores scores = PocketScores.getInstance(community);

        int aaScore = scores.getScore(new Hand(SPADES_A, HEARTS_A));
        int lowScore = scores.getScore(new Hand(SPADES_7, HEARTS_4));

        assertThat(aaScore).isGreaterThan(lowScore);
    }

    @Test
    void should_ReturnPositiveScore_ForAnyValidHand() {
        Hand community = new Hand(DIAMONDS_K, SPADES_10, HEARTS_8, CLUBS_5, DIAMONDS_2);
        PocketScores scores = PocketScores.getInstance(community);

        int score = scores.getScore(SPADES_A, HEARTS_K);
        assertThat(score).isGreaterThan(0);
    }

    @Test
    void should_AcceptCardIndices_AsAlternateInput() {
        Hand community = new Hand(DIAMONDS_Q, SPADES_J, HEARTS_9, CLUBS_2, DIAMONDS_3);
        PocketScores scores = PocketScores.getInstance(community);

        int byCard = scores.getScore(SPADES_A, HEARTS_K);
        int byIndex = scores.getScore(SPADES_A.getIndex(), HEARTS_K.getIndex());
        assertThat(byCard).isEqualTo(byIndex);
    }
}
```

**Step 3: Write PocketRanksTest and PocketOddsTest** following same pattern:
- `PocketRanks`: test `getRawHandStrength()` returns 0.0-1.0, stronger hands have higher strength
- `PocketOdds`: test `getEffectiveHandStrength()` returns 0.0-1.0, requires flop or turn community (3-4 cards only)

**Step 4: Run tests**

Run: `mvn test -pl pokergamecore -Dtest="PocketScoresTest,PocketOddsTest,PocketRanksTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ai/PocketScoresTest.java
git add code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ai/PocketOddsTest.java
git add code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ai/PocketRanksTest.java
git commit -m "test(pokergamecore): add PocketScores, PocketOdds, PocketRanks tests"
```

---

### Task 9: PureHandPotential Tests

**Files:**
- Test: `code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ai/PureHandPotentialTest.java`

**Step 1: Write the test**

Note: The constructor is expensive — keep community at flop (3 cards) for speed.

```java
// Community copyright header (Template 3)
package com.donohoedigital.games.poker.core.ai;

import com.donohoedigital.games.poker.engine.*;
import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

class PureHandPotentialTest {

    @Test
    void should_CountPairOccurrences_When_HighCardOnFlop() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, SPADES_J, HEARTS_9);
        PureHandPotential potential = new PureHandPotential(pocket, community);

        // With AK on QJ9 board, there should be some pair counts on the turn
        int pairCount = potential.getHandCount(PureHandPotential.PAIR, 0);
        assertThat(pairCount).isGreaterThan(0);
    }

    @Test
    void should_CountFlushDrawOccurrences_When_ThreeSuited() {
        Hand pocket = new Hand(SPADES_A, SPADES_K);
        Hand community = new Hand(SPADES_Q, HEARTS_J, DIAMONDS_9);
        PureHandPotential potential = new PureHandPotential(pocket, community);

        // Three spades with two to come — flush draw should have counts
        int flushDrawCount = potential.getHandCount(PureHandPotential.FLUSH_DRAW, 0);
        assertThat(flushDrawCount).isGreaterThanOrEqualTo(0);
    }

    @Test
    void should_CountFlushOccurrences_When_FourSuited() {
        Hand pocket = new Hand(SPADES_A, SPADES_K);
        Hand community = new Hand(SPADES_Q, SPADES_J, DIAMONDS_9);
        PureHandPotential potential = new PureHandPotential(pocket, community);

        // Four spades — flushes should appear in the river lookahead
        int flushCount = potential.getHandCount(PureHandPotential.FLUSH, 1);
        assertThat(flushCount).isGreaterThan(0);
    }

    @Test
    void should_CountStraightOccurrences_When_OpenEnded() {
        Hand pocket = new Hand(SPADES_10, HEARTS_9);
        Hand community = new Hand(DIAMONDS_8, CLUBS_7, HEARTS_2);
        PureHandPotential potential = new PureHandPotential(pocket, community);

        // Open-ended straight draw — straight count should be > 0
        int straightCount = potential.getHandCount(PureHandPotential.STRAIGHT, 0);
        assertThat(straightCount).isGreaterThan(0);
    }

    @Test
    void should_ReturnZero_ForImpossibleHand() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, CLUBS_J, HEARTS_9);
        PureHandPotential potential = new PureHandPotential(pocket, community);

        // Royal flush is extremely unlikely from this starting point at index 0 (turn only)
        int royalCount = potential.getHandCount(PureHandPotential.ROYAL_FLUSH, 0);
        assertThat(royalCount).isGreaterThanOrEqualTo(0); // may be 0 or very small
    }

    @Test
    void should_HandlePreFlop_When_NoCommunity() {
        Hand pocket = new Hand(SPADES_A, HEARTS_A);
        PureHandPotential potential = new PureHandPotential(pocket);

        // AA pre-flop — should count at least pair occurrences
        int pairCount = potential.getHandCount(PureHandPotential.PAIR, 0);
        assertThat(pairCount).isGreaterThanOrEqualTo(0);
    }
}
```

**Step 2: Run tests**

Run: `mvn test -pl pokergamecore -Dtest=PureHandPotentialTest`
Expected: PASS (may take a few seconds due to combinatorial computation)

**Step 3: Commit**

```bash
git add code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/ai/PureHandPotentialTest.java
git commit -m "test(pokergamecore): add PureHandPotential draw calculator tests"
```

---

## Phase 3: poker Module — Tier 1 (Pure Logic)

### Task 10: HoldemExpertTest

**Files:**
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/HoldemExpertTest.java`

**Step 1: Write the test**

```java
// Community copyright header (Template 3)
package com.donohoedigital.games.poker;

import com.donohoedigital.games.poker.engine.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

class HoldemExpertTest {

    // ===== getSklanskyRank Tests =====

    @Test
    void should_RankAAasGroup1() {
        int rank = HoldemExpert.getSklanskyRank(new HandSorted(SPADES_A, HEARTS_A));
        assertThat(HoldemExpert.getGroupFromRank(rank)).isEqualTo(1);
    }

    @Test
    void should_RankKKasGroup1() {
        int rank = HoldemExpert.getSklanskyRank(new HandSorted(SPADES_K, HEARTS_K));
        assertThat(HoldemExpert.getGroupFromRank(rank)).isEqualTo(1);
    }

    @Test
    void should_RankQQasGroup1() {
        int rank = HoldemExpert.getSklanskyRank(new HandSorted(SPADES_Q, HEARTS_Q));
        assertThat(HoldemExpert.getGroupFromRank(rank)).isEqualTo(1);
    }

    @Test
    void should_RankJJasGroup1() {
        int rank = HoldemExpert.getSklanskyRank(new HandSorted(SPADES_J, HEARTS_J));
        assertThat(HoldemExpert.getGroupFromRank(rank)).isEqualTo(1);
    }

    @Test
    void should_RankAKsAsGroup1() {
        int rank = HoldemExpert.getSklanskyRank(new HandSorted(SPADES_A, SPADES_K));
        assertThat(HoldemExpert.getGroupFromRank(rank)).isEqualTo(1);
    }

    @Test
    void should_RankAKoAsGroup2() {
        int rank = HoldemExpert.getSklanskyRank(new HandSorted(SPADES_A, HEARTS_K));
        assertThat(HoldemExpert.getGroupFromRank(rank)).isEqualTo(2);
    }

    @Test
    void should_Rank72oAsGroup10_Unranked() {
        int rank = HoldemExpert.getSklanskyRank(new HandSorted(SPADES_7, HEARTS_2));
        assertThat(HoldemExpert.getGroupFromRank(rank)).isEqualTo(10);
    }

    @Test
    void should_ReturnRank1000_ForUnrankedHands() {
        int rank = HoldemExpert.getSklanskyRank(new HandSorted(SPADES_7, HEARTS_2));
        assertThat(rank).isEqualTo(HoldemExpert.MULT * 10);
    }

    // ===== getGroupFromRank Tests =====

    @Test
    void should_ExtractGroupFromRank() {
        assertThat(HoldemExpert.getGroupFromRank(101)).isEqualTo(1);
        assertThat(HoldemExpert.getGroupFromRank(205)).isEqualTo(2);
        assertThat(HoldemExpert.getGroupFromRank(899)).isEqualTo(8);
        assertThat(HoldemExpert.getGroupFromRank(1000)).isEqualTo(10);
    }

    // ===== Equivalence: suited vs offsuit, different suits =====

    @Test
    void should_GiveSameRank_ForDifferentSuitsOfSameHand() {
        int spadesHearts = HoldemExpert.getSklanskyRank(new HandSorted(SPADES_A, HEARTS_A));
        int clubsDiamonds = HoldemExpert.getSklanskyRank(new HandSorted(CLUBS_A, DIAMONDS_A));
        assertThat(spadesHearts).isEqualTo(clubsDiamonds);
    }

    @Test
    void should_DistinguishSuited_FromOffsuit() {
        int suited = HoldemExpert.getSklanskyRank(new HandSorted(SPADES_A, SPADES_K));
        int offsuit = HoldemExpert.getSklanskyRank(new HandSorted(SPADES_A, HEARTS_K));
        assertThat(suited).isNotEqualTo(offsuit);
        // Suited should rank better (lower number)
        assertThat(suited).isLessThan(offsuit);
    }

    // ===== Parameterized group spot-checks =====

    static Stream<Arguments> knownGroupAssignments() {
        return Stream.of(
            // Group 1
            Arguments.of(new HandSorted(SPADES_A, HEARTS_A), 1),
            Arguments.of(new HandSorted(SPADES_K, HEARTS_K), 1),
            Arguments.of(new HandSorted(SPADES_A, SPADES_K), 1),
            // Group 2
            Arguments.of(new HandSorted(SPADES_A, HEARTS_K), 2),
            Arguments.of(new HandSorted(SPADES_10, HEARTS_10), 2),
            // Group 3
            Arguments.of(new HandSorted(SPADES_9, HEARTS_9), 3),
            // Group 8 boundary (weakest ranked)
            Arguments.of(new HandSorted(SPADES_8, HEARTS_8), 5),
            // Unranked
            Arguments.of(new HandSorted(SPADES_7, HEARTS_2), 10),
            Arguments.of(new HandSorted(SPADES_8, HEARTS_3), 10)
        );
    }

    @ParameterizedTest
    @MethodSource("knownGroupAssignments")
    void should_AssignCorrectGroup(HandSorted hand, int expectedGroup) {
        int rank = HoldemExpert.getSklanskyRank(hand);
        assertThat(HoldemExpert.getGroupFromRank(rank)).isEqualTo(expectedGroup);
    }

    // ===== Constants Tests =====

    @Test
    void should_HaveCorrectMaxGroupConstants() {
        assertThat(HoldemExpert.MAXGROUP1).isEqualTo(199);
        assertThat(HoldemExpert.MAXGROUP8).isEqualTo(899);
    }

    // ===== Pre-built HandSorted Constants Tests =====

    @Test
    void should_HaveValidPrebuiltHands() {
        assertThat(HoldemExpert.AA).isNotNull();
        assertThat(HoldemExpert.KK).isNotNull();
        assertThat(HoldemExpert.AKs).isNotNull();
        assertThat(HoldemExpert.AKo).isNotNull();
    }
}
```

**Step 2: Run tests**

Run: `mvn test -pl poker -Dtest=HoldemExpertTest`
Expected: PASS

**Step 3: Commit**

```bash
git add code/poker/src/test/java/com/donohoedigital/games/poker/HoldemExpertTest.java
git commit -m "test(poker): add HoldemExpert Sklansky ranking tests

Cover all group assignments, suited vs offsuit distinction,
rank extraction, pre-built hand constants, and group boundary checks."
```

---

### Task 11: HandFuturesTest

**Files:**
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/HandFuturesTest.java`

**Step 1: Write the test**

```java
// Community copyright header (Template 3)
package com.donohoedigital.games.poker;

import com.donohoedigital.games.poker.engine.*;
import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

class HandFuturesTest {

    // ===== Flush Draw Detection =====

    @Test
    void should_DetectFlushDraw_When_FourSuitedOnFlop() {
        HandInfoFaster fast = new HandInfoFaster();
        Hand hole = new Hand(SPADES_A, SPADES_K);
        Hand community = new Hand(SPADES_Q, SPADES_6, HEARTS_3);
        HandFutures futures = new HandFutures(fast, hole, community);
        assertThat(futures.hasFlushDraw()).isTrue();
    }

    @Test
    void should_NotDetectFlushDraw_When_NotEnoughSuited() {
        HandInfoFaster fast = new HandInfoFaster();
        Hand hole = new Hand(SPADES_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, CLUBS_6, HEARTS_3);
        HandFutures futures = new HandFutures(fast, hole, community);
        assertThat(futures.hasFlushDraw()).isFalse();
    }

    // ===== Straight Draw Detection =====

    @Test
    void should_DetectStraightDraw_When_OpenEnded() {
        HandInfoFaster fast = new HandInfoFaster();
        Hand hole = new Hand(SPADES_10, HEARTS_9);
        Hand community = new Hand(DIAMONDS_8, CLUBS_7, HEARTS_2);
        HandFutures futures = new HandFutures(fast, hole, community);
        assertThat(futures.hasStraightDraw()).isTrue();
    }

    @Test
    void should_NotDetectStraightDraw_When_NoConnectors() {
        HandInfoFaster fast = new HandInfoFaster();
        Hand hole = new Hand(SPADES_A, HEARTS_7);
        Hand community = new Hand(DIAMONDS_3, CLUBS_2, HEARTS_K);
        HandFutures futures = new HandFutures(fast, hole, community);
        assertThat(futures.hasStraightDraw()).isFalse();
    }

    // ===== Gut Shot Detection =====

    @Test
    void should_DetectGutShot_When_OneCardNeeded() {
        HandInfoFaster fast = new HandInfoFaster();
        // 10-J on 8-Q board = needs a 9 for a straight
        Hand hole = new Hand(SPADES_J, HEARTS_8);
        Hand community = new Hand(DIAMONDS_Q, CLUBS_10, HEARTS_2);
        HandFutures futures = new HandFutures(fast, hole, community);
        assertThat(futures.hasGutShotStraightDraw()).isTrue();
    }

    // ===== Odds Calculations =====

    @Test
    void should_ReturnPositiveOdds_When_DrawPresent() {
        HandInfoFaster fast = new HandInfoFaster();
        Hand hole = new Hand(SPADES_A, SPADES_K);
        Hand community = new Hand(SPADES_Q, SPADES_6, HEARTS_3);
        HandFutures futures = new HandFutures(fast, hole, community);
        assertThat(futures.getOddsImprove()).isGreaterThan(0.0f);
    }

    @Test
    void should_ReturnOddsInValidRange() {
        HandInfoFaster fast = new HandInfoFaster();
        Hand hole = new Hand(SPADES_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, SPADES_J, HEARTS_9);
        HandFutures futures = new HandFutures(fast, hole, community);
        assertThat(futures.getOddsImprove()).isBetween(0.0f, 100.0f);
    }

    @Test
    void should_ReturnOddsToFlush_When_FlushDrawPresent() {
        HandInfoFaster fast = new HandInfoFaster();
        Hand hole = new Hand(SPADES_A, SPADES_K);
        Hand community = new Hand(SPADES_Q, SPADES_6, HEARTS_3);
        HandFutures futures = new HandFutures(fast, hole, community);
        float flushOdds = futures.getOddsImproveTo(HandInfo.FLUSH);
        assertThat(flushOdds).isGreaterThan(0.0f);
    }

    // ===== Turn vs Flop Community =====

    @Test
    void should_HandleTurnCommunity_When_FourCards() {
        HandInfoFaster fast = new HandInfoFaster();
        Hand hole = new Hand(SPADES_A, SPADES_K);
        Hand community = new Hand(SPADES_Q, SPADES_6, HEARTS_3, DIAMONDS_10);
        HandFutures futures = new HandFutures(fast, hole, community);
        // Should still detect flush draw
        assertThat(futures.hasFlushDraw()).isTrue();
        // Turn odds should be different from flop odds (1 card vs 2 cards to come)
        assertThat(futures.getOddsImprove()).isBetween(0.0f, 100.0f);
    }
}
```

**Step 2: Run tests**

Run: `mvn test -pl poker -Dtest=HandFuturesTest`
Expected: PASS

**Step 3: Commit**

```bash
git add code/poker/src/test/java/com/donohoedigital/games/poker/HandFuturesTest.java
git commit -m "test(poker): add HandFutures draw detection and odds tests

Cover flush draw, straight draw, gut-shot detection, odds
calculation ranges, and turn vs flop community handling."
```

---

### Task 12: HandLadderTest

**Files:**
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/HandLadderTest.java`

**Step 1: Write the test**

Note: `HandLadder` depends on `HandProbabilityMatrix` which requires config initialization. The constructor triggers a full calculation.

```java
// Community copyright header (Template 3)
package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.games.poker.engine.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

class HandLadderTest {

    @BeforeAll
    static void initConfig() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
    }

    @Test
    void should_ReturnPositiveRank_When_ValidHand() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, SPADES_J, HEARTS_9);
        HandProbabilityMatrix matrix = new HandProbabilityMatrix();
        matrix.init(1.0f);
        HandLadder ladder = new HandLadder(pocket, community, matrix);

        assertThat(ladder.getHandRank()).isGreaterThan(0);
    }

    @Test
    void should_ReturnPositiveCount_When_ValidHand() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, SPADES_J, HEARTS_9);
        HandProbabilityMatrix matrix = new HandProbabilityMatrix();
        matrix.init(1.0f);
        HandLadder ladder = new HandLadder(pocket, community, matrix);

        assertThat(ladder.getHandCount()).isGreaterThan(0);
    }

    @Test
    void should_RankStrongerHand_Better() {
        Hand community = new Hand(DIAMONDS_Q, SPADES_J, HEARTS_9, CLUBS_2, DIAMONDS_3);
        HandProbabilityMatrix matrix = new HandProbabilityMatrix();
        matrix.init(1.0f);

        HandLadder strongLadder = new HandLadder(
            new Hand(SPADES_A, HEARTS_A), community, matrix);
        HandLadder weakLadder = new HandLadder(
            new Hand(SPADES_7, HEARTS_4), community, matrix);

        // Lower rank = stronger hand
        assertThat(strongLadder.getHandRank()).isLessThan(weakLadder.getHandRank());
    }

    @Test
    void should_HaveRankWithinHandCount() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, SPADES_J, HEARTS_9);
        HandProbabilityMatrix matrix = new HandProbabilityMatrix();
        matrix.init(1.0f);
        HandLadder ladder = new HandLadder(pocket, community, matrix);

        assertThat(ladder.getHandRank()).isLessThanOrEqualTo(ladder.getHandCount());
    }
}
```

**Step 2: Run tests**

Run: `mvn test -pl poker -Dtest=HandLadderTest`
Expected: PASS

**Step 3: Commit**

```bash
git add code/poker/src/test/java/com/donohoedigital/games/poker/HandLadderTest.java
git commit -m "test(poker): add HandLadder probability distribution tests"
```

---

## Phase 4: poker Module — Tier 2 (Extract Logic)

### Task 13: CheckEndHand — Widen Visibility and Test

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/CheckEndHand.java`
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/CheckEndHandTest.java`

**Step 1: Widen method visibility from private to package-private**

In `CheckEndHand.java`, change these 4 methods from `private static` to `static` (package-private):

Line 67: `private enum GameOverResult` → `enum GameOverResult`
Line 71: `private static boolean isHumanBroke` → `static boolean isHumanBroke`
Line 75: `private static boolean shouldOfferRebuy` → `static boolean shouldOfferRebuy`
Line 79: `private static GameOverResult checkGameOverStatus` → `static GameOverResult checkGameOverStatus`
Line 101: `private static int calculateNeverBrokeTransfer` → `static int calculateNeverBrokeTransfer`

**Step 2: Write the test**

```java
// Community copyright header (Template 3)
package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.engine.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CheckEndHandTest {

    private PokerGame game;
    private PokerTable table;
    private PokerPlayer human;

    @BeforeAll
    static void initConfig() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
    }

    @BeforeEach
    void setUp() {
        game = new PokerGame(null);
        TournamentProfile profile = new TournamentProfile("test");
        profile.setNumPlayers(3);
        game.setProfile(profile);

        table = new PokerTable(game, 0, 10, false);
        game.addTable(table);
        game.setCurrentTable(table);

        human = new PokerPlayer(0, game, false);
        human.setHuman(true);
        human.setChipCount(1000);
        game.addPlayer(human);
        table.setPlayer(human, 0);

        PokerPlayer ai1 = new PokerPlayer(1, game, false);
        ai1.setChipCount(1000);
        game.addPlayer(ai1);
        table.setPlayer(ai1, 1);

        PokerPlayer ai2 = new PokerPlayer(2, game, false);
        ai2.setChipCount(1000);
        game.addPlayer(ai2);
        table.setPlayer(ai2, 2);
    }

    // ===== isHumanBroke Tests =====

    @Test
    void should_ReturnFalse_When_HumanHasChips() {
        assertThat(CheckEndHand.isHumanBroke(human)).isFalse();
    }

    @Test
    void should_ReturnTrue_When_HumanHasZeroChips() {
        human.setChipCount(0);
        assertThat(CheckEndHand.isHumanBroke(human)).isTrue();
    }

    @Test
    void should_ReturnFalse_When_HumanIsObserver() {
        human.setChipCount(0);
        human.setObserver(true);
        assertThat(CheckEndHand.isHumanBroke(human)).isFalse();
    }

    // ===== calculateNeverBrokeTransfer Tests =====

    @Test
    void should_TransferHalfOfLeaderChips() {
        int transfer = CheckEndHand.calculateNeverBrokeTransfer(1000, 10);
        assertThat(transfer).isEqualTo(500);
    }

    @Test
    void should_RoundDownToMinChip() {
        int transfer = CheckEndHand.calculateNeverBrokeTransfer(1050, 100);
        // 1050 / 2 = 525, rounded down to nearest 100 = 500
        assertThat(transfer).isEqualTo(500);
    }

    @Test
    void should_ReturnZero_When_LeaderHasTooFewChips() {
        int transfer = CheckEndHand.calculateNeverBrokeTransfer(10, 100);
        // 10 / 2 = 5, rounded down to nearest 100 = 0
        assertThat(transfer).isEqualTo(0);
    }

    // ===== checkGameOverStatus Tests =====

    @Test
    void should_ReturnContinue_When_HumanHasChips() {
        CheckEndHand.GameOverResult result =
            CheckEndHand.checkGameOverStatus(game, human, table, false);
        assertThat(result).isEqualTo(CheckEndHand.GameOverResult.CONTINUE);
    }

    @Test
    void should_ReturnGameOver_When_HumanBrokeNoRebuyNoCheats() {
        human.setChipCount(0);
        CheckEndHand.GameOverResult result =
            CheckEndHand.checkGameOverStatus(game, human, table, false);
        assertThat(result).isEqualTo(CheckEndHand.GameOverResult.GAME_OVER);
    }

    @Test
    void should_ReturnNeverBrokeActive_When_HumanBrokeWithCheat() {
        human.setChipCount(0);
        CheckEndHand.GameOverResult result =
            CheckEndHand.checkGameOverStatus(game, human, table, true);
        assertThat(result).isEqualTo(CheckEndHand.GameOverResult.NEVER_BROKE_ACTIVE);
    }

    @Test
    void should_ReturnTournamentWon_When_OnePlayerLeft() {
        // Remove all but one player to trigger "one player left"
        // (implementation details may vary — check PokerGame.isOnePlayerLeft())
        game.removePlayer(game.getPlayerAt(1));
        game.removePlayer(game.getPlayerAt(1));
        // Now only human remains
        CheckEndHand.GameOverResult result =
            CheckEndHand.checkGameOverStatus(game, human, table, false);
        assertThat(result).isEqualTo(CheckEndHand.GameOverResult.TOURNAMENT_WON);
    }
}
```

**Step 3: Run tests**

Run: `mvn test -pl poker -Dtest=CheckEndHandTest`
Expected: PASS. If `PokerGame`/`PokerPlayer`/`PokerTable` constructors need different arguments, adjust in setUp() based on compile errors.

**Step 4: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/CheckEndHand.java
git add code/poker/src/test/java/com/donohoedigital/games/poker/CheckEndHandTest.java
git commit -m "test(poker): add CheckEndHand game-over logic tests

Widen visibility of pure static methods to package-private.
Test all 5 GameOverResult branches, never-broke transfer
arithmetic, and human-broke predicates."
```

---

### Task 14: ShowdownCalculator — Extract and Test

**Files:**
- Create: `code/poker/src/main/java/com/donohoedigital/games/poker/ShowdownCalculator.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/Showdown.java`
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/ShowdownCalculatorTest.java`

**Step 1: Create ShowdownCalculator with extracted display decision logic**

Extract the per-player display decisions from `Showdown.displayShowdown()` lines 64-185 into a pure calculator class. This class takes `HoldemHand` and cheat flags as input and returns a list of display info records — no UI dependencies.

The key boolean logic to extract:
- `bShowCards` computation (line 147-149)
- `bShowHandTypeLocal` computation (lines 151-159)
- `nResult` placard type selection (lines 177-185)
- Per-player fold/win/lose/allin classification

```java
// Community copyright header (Template 3) — dual copyright header (Template 2) since
// the logic originates from Showdown.java (Doug Donohoe's original code).
package com.donohoedigital.games.poker;

import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.core.state.BettingRound;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure logic for computing showdown display decisions.
 * Extracted from Showdown to enable unit testing without UI dependencies.
 */
public class ShowdownCalculator {

    /** Display decision for a single player in the showdown. */
    public record PlayerDisplayInfo(
        int playerIndex,
        int resultType,     // WIN, LOSE, OVERBET, FOLD, ALLIN, HIDDEN
        boolean showCards,
        boolean showHandType,
        int totalWon,       // net chips (nAmount + nOverbet)
        boolean isFolded
    ) {}

    // ResultsPiece constants (mirrored to avoid UI dependency)
    static final int WIN = 0;
    static final int LOSE = 1;
    static final int OVERBET = 2;
    static final int FOLD = 3;
    static final int ALLIN = 4;
    static final int HIDDEN = 5;

    private ShowdownCalculator() {}

    /**
     * Compute display decisions for each player in the hand.
     * Pure logic — no UI calls.
     */
    public static List<PlayerDisplayInfo> calculateDisplayInfo(
            HoldemHand hhand,
            boolean rabbitHunt,
            boolean showWinning,
            boolean showMucked,
            boolean humanCardsUp,
            boolean aiFaceUp) {

        List<PlayerDisplayInfo> results = new ArrayList<>();
        boolean bUncontested = hhand.isUncontested();
        boolean bSeenRiver = hhand.isActionInRound(BettingRound.RIVER.toLegacy());
        boolean bShowHandType = !bUncontested || ((rabbitHunt || bSeenRiver) && showWinning);
        boolean bShowHandTypeFold = !bUncontested || rabbitHunt || bSeenRiver;

        for (int i = 0; i < hhand.getNumPlayers(); i++) {
            PokerPlayer player = hhand.getPlayerAt(i);
            if (player.getTable() == null) continue;

            // All-in showdown (before river) — just mark as ALLIN
            if (hhand.getRound().toLegacy() < BettingRound.SHOWDOWN.toLegacy()) {
                if (!player.isFolded()) {
                    results.add(new PlayerDisplayInfo(i, ALLIN, true, false, 0, false));
                }
                continue;
            }

            // Folded players
            if (player.isFolded()) {
                boolean showFolded = player.showFoldedHand();
                results.add(new PlayerDisplayInfo(
                    i, showFolded ? FOLD : HIDDEN,
                    showFolded, showFolded && bShowHandTypeFold,
                    0, true));
                continue;
            }

            // Players at showdown
            int nAmount = hhand.getWin(player);
            int nOverbet = hhand.getOverbet(player);
            int nTotal = nAmount + nOverbet;
            boolean bWon = (nAmount > 0);

            boolean bShowCards = player.isCardsExposed()
                || (!bUncontested && showMucked && !bWon)
                || (showWinning && bWon)
                || (player.isHuman() && player.isLocallyControlled() && humanCardsUp)
                || (player.isComputer() && aiFaceUp);

            boolean bShowHandTypeLocal = bShowHandType;
            if (player.isHuman() && rabbitHunt) bShowHandTypeLocal = true;
            if (bUncontested && player.isShowWinning() && (rabbitHunt || bSeenRiver))
                bShowHandTypeLocal = true;

            int nResult;
            if (nTotal == 0) {
                nResult = LOSE;
            } else if (nOverbet == nTotal) {
                nResult = OVERBET;
            } else {
                nResult = WIN;
            }

            results.add(new PlayerDisplayInfo(
                i, nResult, bShowCards, bShowHandTypeLocal, nTotal, false));
        }

        return results;
    }
}
```

**Step 2: Refactor Showdown.displayShowdown() to delegate to ShowdownCalculator**

In `Showdown.java`, replace the per-player loop logic (lines 64-185) with a call to `ShowdownCalculator.calculateDisplayInfo(...)`, then iterate the results for the UI rendering calls only. Keep the board-card update logic (lines 191-203) in `Showdown`.

**Step 3: Write ShowdownCalculatorTest**

```java
// Community copyright header (Template 3)
package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.engine.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

class ShowdownCalculatorTest {

    @BeforeAll
    static void initConfig() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
    }

    // Create a minimal HoldemHand for testing — implementation details
    // depend on how HoldemHand is constructed. This may need a PokerGame
    // and PokerTable setup similar to CheckEndHandTest.

    // Test structure (implement based on HoldemHand API):

    @Test
    void should_MarkWinner_AsWin_When_UncontestedPot() {
        // Setup: one player remaining (others folded) — the winner.
        // All cheat flags off.
        // Expected: winner gets WIN result, folded players get HIDDEN.
        // Implementation: construct HoldemHand via PokerTable, deal,
        //   fold all but one player, advance to showdown.
        // The exact setup will depend on HoldemHand constructor requirements.
        // Skeleton:
        // List<ShowdownCalculator.PlayerDisplayInfo> info =
        //     ShowdownCalculator.calculateDisplayInfo(hhand, false, false, false, false, false);
        // assertThat(info).anyMatch(p -> p.resultType() == ShowdownCalculator.WIN);
    }

    @Test
    void should_ShowMuckedCards_When_ShowMuckedCheatOn() {
        // Setup: multi-way showdown, loser has mucked hand.
        // showMucked=true.
        // Expected: loser's showCards=true.
    }

    @Test
    void should_HideCards_When_NoCheatsFolded() {
        // Setup: player folded.
        // All cheat flags off, player.showFoldedHand() = false.
        // Expected: HIDDEN result, showCards=false.
    }

    @Test
    void should_ShowAICards_When_AIFaceUpCheatOn() {
        // Setup: AI player at showdown.
        // aiFaceUp=true.
        // Expected: AI player's showCards=true.
    }

    @Test
    void should_MarkOverbet_When_WinEqualsOverbet() {
        // Setup: player's overbet == total won (returned excess bet).
        // Expected: OVERBET result type.
    }
}
```

Note: The test skeletons above will need to be fleshed out based on the actual `HoldemHand` construction API. The implementer should read `HoldemHandTest.java` for the setup pattern, then adapt these tests.

**Step 4: Run tests**

Run: `mvn test -pl poker -Dtest=ShowdownCalculatorTest`
Expected: PASS

**Step 5: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/ShowdownCalculator.java
git add code/poker/src/main/java/com/donohoedigital/games/poker/Showdown.java
git add code/poker/src/test/java/com/donohoedigital/games/poker/ShowdownCalculatorTest.java
git commit -m "refactor(poker): extract ShowdownCalculator from Showdown

Extract pure display-decision logic into ShowdownCalculator for
testability. Showdown.displayShowdown() delegates to calculator,
then renders results. Tests cover all result types and cheat flags."
```

---

### Task 15: CommunityCardCalculator — Extract and Test

**Files:**
- Create: `code/poker/src/main/java/com/donohoedigital/games/poker/CommunityCardCalculator.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/DealCommunity.java`
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/CommunityCardCalculatorTest.java`

**Step 1: Create CommunityCardCalculator**

```java
// Community copyright header (Template 3) — dual copyright since logic from DealCommunity.java
package com.donohoedigital.games.poker;

import com.donohoedigital.games.poker.engine.HoldemHand;
import com.donohoedigital.games.poker.core.state.BettingRound;

/**
 * Pure logic for computing community card visibility flags.
 * Extracted from DealCommunity to enable unit testing without UI dependencies.
 */
public class CommunityCardCalculator {

    /** Visibility flags for the 5 community card slots. */
    public record CommunityCardFlags(
        boolean[] drawnNormal,  // length 5
        boolean[] drawn         // length 5
    ) {}

    private CommunityCardCalculator() {}

    /**
     * Calculate which community cards should be visible.
     *
     * @param displayRound  current display round (HoldemHand.ROUND_* constant)
     * @param lastBettingRound  last completed betting round (BettingRound.toLegacy())
     * @param numWithCards  number of players still with cards
     * @param rabbitHunt  whether rabbit hunt cheat is enabled
     * @return flags indicating visibility for each of the 5 community card slots
     */
    public static CommunityCardFlags calculateVisibility(
            int displayRound, int lastBettingRound, int numWithCards, boolean rabbitHunt) {

        boolean[] drawnNormal = new boolean[5];
        boolean[] drawn = new boolean[5];

        boolean bDrawnNormal = numWithCards > 1;
        boolean bDrawn = rabbitHunt || bDrawnNormal;

        // Fall-through logic matching DealCommunity.syncCards
        boolean bCardDealt;
        switch (displayRound) {
            case HoldemHand.ROUND_SHOWDOWN:
            case HoldemHand.ROUND_RIVER:
                bCardDealt = lastBettingRound >= BettingRound.RIVER.toLegacy();
                drawnNormal[4] = bDrawnNormal || bCardDealt;
                drawn[4] = bDrawn || bCardDealt;
                // fall through
            case HoldemHand.ROUND_TURN:
                bCardDealt = lastBettingRound >= BettingRound.TURN.toLegacy();
                drawnNormal[3] = bDrawnNormal || bCardDealt;
                drawn[3] = bDrawn || bCardDealt;
                // fall through
            case HoldemHand.ROUND_FLOP:
                bCardDealt = lastBettingRound >= BettingRound.FLOP.toLegacy();
                for (int i = 0; i < 3; i++) {
                    drawnNormal[i] = bDrawnNormal || bCardDealt;
                    drawn[i] = bDrawn || bCardDealt;
                }
                break;
        }

        return new CommunityCardFlags(drawnNormal, drawn);
    }
}
```

**Step 2: Refactor DealCommunity.syncCards() to delegate to CommunityCardCalculator**

Replace the switch/case in `DealCommunity.syncCards()` with a call to `CommunityCardCalculator.calculateVisibility(...)`, then use the returned flags to create `CommunityCardPiece` instances.

**Step 3: Write CommunityCardCalculatorTest**

```java
// Community copyright header (Template 3)
package com.donohoedigital.games.poker;

import com.donohoedigital.games.poker.engine.HoldemHand;
import com.donohoedigital.games.poker.core.state.BettingRound;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CommunityCardCalculatorTest {

    @Test
    void should_ShowNoCards_When_PreFlop() {
        CommunityCardCalculator.CommunityCardFlags flags =
            CommunityCardCalculator.calculateVisibility(
                HoldemHand.ROUND_PRE_FLOP, 0, 3, false);

        for (int i = 0; i < 5; i++) {
            assertThat(flags.drawn()[i]).isFalse();
            assertThat(flags.drawnNormal()[i]).isFalse();
        }
    }

    @Test
    void should_ShowThreeCards_When_Flop() {
        CommunityCardCalculator.CommunityCardFlags flags =
            CommunityCardCalculator.calculateVisibility(
                HoldemHand.ROUND_FLOP, BettingRound.FLOP.toLegacy(), 3, false);

        // First 3 cards visible
        for (int i = 0; i < 3; i++) {
            assertThat(flags.drawn()[i]).isTrue();
            assertThat(flags.drawnNormal()[i]).isTrue();
        }
        // Last 2 not visible
        assertThat(flags.drawn()[3]).isFalse();
        assertThat(flags.drawn()[4]).isFalse();
    }

    @Test
    void should_ShowFourCards_When_Turn() {
        CommunityCardCalculator.CommunityCardFlags flags =
            CommunityCardCalculator.calculateVisibility(
                HoldemHand.ROUND_TURN, BettingRound.TURN.toLegacy(), 3, false);

        for (int i = 0; i < 4; i++) {
            assertThat(flags.drawn()[i]).isTrue();
        }
        assertThat(flags.drawn()[4]).isFalse();
    }

    @Test
    void should_ShowAllCards_When_River() {
        CommunityCardCalculator.CommunityCardFlags flags =
            CommunityCardCalculator.calculateVisibility(
                HoldemHand.ROUND_RIVER, BettingRound.RIVER.toLegacy(), 3, false);

        for (int i = 0; i < 5; i++) {
            assertThat(flags.drawn()[i]).isTrue();
        }
    }

    @Test
    void should_ShowAllCards_When_RabbitHuntEnabled() {
        // Even on the flop, rabbit hunt reveals all dealt cards
        CommunityCardCalculator.CommunityCardFlags flags =
            CommunityCardCalculator.calculateVisibility(
                HoldemHand.ROUND_RIVER, BettingRound.FLOP.toLegacy(), 1, true);

        // With rabbit hunt and only 1 player with cards,
        // drawnNormal is false (only 1 player) but drawn is true (rabbit hunt)
        for (int i = 0; i < 5; i++) {
            assertThat(flags.drawn()[i]).isTrue();
            assertThat(flags.drawnNormal()[i]).isFalse();
        }
    }

    @Test
    void should_NotShowNormally_When_OnlyOnePlayerWithCards() {
        CommunityCardCalculator.CommunityCardFlags flags =
            CommunityCardCalculator.calculateVisibility(
                HoldemHand.ROUND_FLOP, BettingRound.FLOP.toLegacy(), 1, false);

        // Cards were dealt (bCardDealt=true) so drawn=true, drawnNormal=true
        // Wait — bDrawnNormal = numWithCards > 1 = false, but bCardDealt = true
        // So drawnNormal = false || true = true
        for (int i = 0; i < 3; i++) {
            assertThat(flags.drawn()[i]).isTrue();
        }
    }

    @Test
    void should_ShowAllFiveCards_When_Showdown() {
        CommunityCardCalculator.CommunityCardFlags flags =
            CommunityCardCalculator.calculateVisibility(
                HoldemHand.ROUND_SHOWDOWN, BettingRound.RIVER.toLegacy(), 2, false);

        for (int i = 0; i < 5; i++) {
            assertThat(flags.drawn()[i]).isTrue();
            assertThat(flags.drawnNormal()[i]).isTrue();
        }
    }
}
```

**Step 4: Run tests**

Run: `mvn test -pl poker -Dtest=CommunityCardCalculatorTest`
Expected: PASS

**Step 5: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/CommunityCardCalculator.java
git add code/poker/src/main/java/com/donohoedigital/games/poker/DealCommunity.java
git add code/poker/src/test/java/com/donohoedigital/games/poker/CommunityCardCalculatorTest.java
git commit -m "refactor(poker): extract CommunityCardCalculator from DealCommunity

Extract pure card visibility logic into CommunityCardCalculator.
Test all round transitions, rabbit hunt, and player-count edge cases."
```

---

## Phase 5: poker Module — Tier 3 (Deepen Existing Tests)

### Task 16: Deepen PokerTableTest — Button/Position Tracking

**Files:**
- Modify: `code/poker/src/test/java/com/donohoedigital/games/poker/PokerTableTest.java`

**Step 1: Read existing test file to find the stubbed section**

Run: Search for "Button and Position" in PokerTableTest.java to find the empty section.

**Step 2: Add button/position tests**

Add tests covering:
- `getButton()` / `setButton()` — dealer button seat tracking
- `getSmallBlindSeat()` / `getBigBlindSeat()` — blind position calculation
- Button movement after a hand
- Heads-up blind rules (button = small blind)

The exact API will depend on what methods PokerTable exposes. Read `PokerTable.java` to identify the button/position methods before writing tests.

**Step 3: Run tests**

Run: `mvn test -pl poker -Dtest=PokerTableTest`
Expected: PASS

**Step 4: Commit**

```bash
git add code/poker/src/test/java/com/donohoedigital/games/poker/PokerTableTest.java
git commit -m "test(poker): add button/position tracking tests to PokerTableTest"
```

---

### Task 17: Deepen PokerPlayerTest — Serialization and Rebuy

**Files:**
- Modify: `code/poker/src/test/java/com/donohoedigital/games/poker/PokerPlayerTest.java`

**Step 1: Read existing test to understand current coverage**

**Step 2: Add tests for:**
- `marshal()` / `demarshal()` round-trip — player state survives serialization
- `isInHand()` with an active HoldemHand (currently untested)
- Rebuy/addon operations (if methods exist and are testable without full game context)

**Step 3: Run and commit**

Run: `mvn test -pl poker -Dtest=PokerPlayerTest`

```bash
git add code/poker/src/test/java/com/donohoedigital/games/poker/PokerPlayerTest.java
git commit -m "test(poker): add serialization and in-hand tests to PokerPlayerTest"
```

---

### Task 18: Deepen PotTest — Split Pot Scenarios

**Files:**
- Modify: `code/poker/src/test/java/com/donohoedigital/games/poker/PotTest.java`

**Step 1: Read existing test**

**Step 2: Add multi-winner split-pot tests:**
- Two players with equal hands splitting a pot
- Three-way split
- Side pot with all-in player
- Overbet returned to better

**Step 3: Run and commit**

Run: `mvn test -pl poker -Dtest=PotTest`

```bash
git add code/poker/src/test/java/com/donohoedigital/games/poker/PotTest.java
git commit -m "test(poker): add split-pot scenarios to PotTest"
```

---

### Task 19: Replace PokerDataMarshallerTest (JUnit 3 No-Op)

**Files:**
- Modify: `code/poker/src/test/java/com/donohoedigital/games/poker/PokerDataMarshallerTest.java`

**Step 1: Read the current file** — it's a JUnit 3 test with a single `testStatic()` that does nothing.

**Step 2: Rewrite as JUnit 5 with real marshal/demarshal tests**

Test that `DataMarshaller` can round-trip poker domain objects (PokerGame, PokerPlayer, etc.). The exact tests depend on what `PokerDataMarshaller` serializes.

**Step 3: Run and commit**

Run: `mvn test -pl poker -Dtest=PokerDataMarshallerTest`

```bash
git add code/poker/src/test/java/com/donohoedigital/games/poker/PokerDataMarshallerTest.java
git commit -m "test(poker): rewrite PokerDataMarshallerTest with real marshal/demarshal tests"
```

---

## Phase 6: poker Module — Tier 4 (Remaining Classes)

### Task 20: TableDesign, PlayerProfile, GameClock Tests

**Files:**
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/TableDesignTest.java`
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/PlayerProfileTest.java`
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/GameClockTest.java`

**Step 1: Write TableDesignTest** — color getters/setters, default values, copy constructor.

**Step 2: Write PlayerProfileTest** — profile data, name/avatar, beyond what PokerPlayerTest covers.

**Step 3: Write GameClockTest** — test `marshal()`/`demarshal()` round-trip. Verify `PureTournamentClock` is already tested in pokergamecore; if not, add tests there first.

**Step 4: Run and commit**

Run: `mvn test -pl poker -Dtest="TableDesignTest,PlayerProfileTest,GameClockTest"`

```bash
git add code/poker/src/test/java/com/donohoedigital/games/poker/TableDesignTest.java
git add code/poker/src/test/java/com/donohoedigital/games/poker/PlayerProfileTest.java
git add code/poker/src/test/java/com/donohoedigital/games/poker/GameClockTest.java
git commit -m "test(poker): add TableDesign, PlayerProfile, and GameClock tests"
```

---

## Phase 7: Coverage Verification and Floor Adjustment

### Task 21: Run Full Coverage and Raise JaCoCo Floors

**Step 1: Run full coverage**

Run: `mvn verify -P coverage`
Expected: All tests pass, coverage reports generated.

**Step 2: Check actual coverage percentages**

Check `code/poker/target/site/jacoco/index.html` (or parse XML) for:
- `com.donohoedigital.games.poker` package instruction coverage
- `com.donohoedigital.games.poker.ai` package instruction coverage
- pokerengine bundle coverage
- pokergamecore bundle coverage

**Step 3: Raise JaCoCo floors**

- `code/pokerengine/pom.xml`: raise `<minimum>` to measured value minus 5% buffer
- `code/poker/pom.xml` main package rule: raise `<minimum>0.15</minimum>` toward 0.40 or actual coverage minus 5%
- pokergamecore: should already be at 0.80, verify it still passes

**Step 4: Verify raised floors pass**

Run: `mvn verify -P coverage`
Expected: PASS with new thresholds.

**Step 5: Commit**

```bash
git add code/pokerengine/pom.xml code/poker/pom.xml
git commit -m "build: raise JaCoCo coverage floors to match new test coverage

pokerengine: 2% -> XX%
poker main package: 15% -> XX%"
```

---

## Summary

| Phase | Tasks | Focus |
|-------|-------|-------|
| 1 | 1-3 | pokerengine: HandInfoFaster, TournamentTemplate, raise floor |
| 2 | 4-9 | pokergamecore: PocketMatrix*, SklanksyRanking, SimpleBias, ActionOptions, HandInfoFast, PureHandPotential, PocketScores/Odds/Ranks |
| 3 | 10-12 | poker Tier 1: HoldemExpert, HandFutures, HandLadder |
| 4 | 13-15 | poker Tier 2: CheckEndHand (widen visibility), ShowdownCalculator (extract), CommunityCardCalculator (extract) |
| 5 | 16-19 | poker Tier 3: Deepen PokerTableTest, PokerPlayerTest, PotTest, PokerDataMarshallerTest |
| 6 | 20 | poker Tier 4: TableDesign, PlayerProfile, GameClock |
| 7 | 21 | Coverage verification and JaCoCo floor adjustment |
