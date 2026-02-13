/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.gui.*;
import org.junit.jupiter.api.*;

import java.lang.reflect.*;
import java.math.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for HoldemSimulator - poker hand simulation and statistics.
 */
class HoldemSimulatorTest {

    @BeforeEach
    void setUp() {
        // Initialize config for each test. Note: loadGuiConfig() is NOT called
        // as it's not needed and may cause issues in CI environments.
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
    }

    /**
     * Simple progress feedback for tests - allows simulation to proceed without
     * depending on HandGroup file loading.
     */
    private static class TestProgressFeedback implements DDProgressFeedback {
        @Override
        public boolean isStopRequested() {
            return false; // Never stop
        }

        @Override
        public void setMessage(String sMessage) {
            // No-op for tests
        }

        @Override
        public void setPercentDone(int n) {
            // No-op for tests
        }

        @Override
        public void setFinalResult(Object oResult) {
            // No-op for tests
        }

        @Override
        public void setIntermediateResult(Object oResult) {
            // No-op for tests
        }
    }

    // ========== getInterval() Tests (via reflection) ==========

    @Test
    void should_ReturnHalfHandCount_WhenDivideByExceedsHandCount() throws Exception {
        Method getInterval = HoldemSimulator.class.getDeclaredMethod("getInterval", int.class, int.class);
        getInterval.setAccessible(true);

        int result = (int) getInterval.invoke(null, 100, 200);

        assertThat(result).isEqualTo(50); // handCount / 2
    }

    @Test
    void should_Return1_WhenHandCountIs1() throws Exception {
        Method getInterval = HoldemSimulator.class.getDeclaredMethod("getInterval", int.class, int.class);
        getInterval.setAccessible(true);

        int result = (int) getInterval.invoke(null, 1, 100);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void should_Return1_WhenHandCountIs2() throws Exception {
        Method getInterval = HoldemSimulator.class.getDeclaredMethod("getInterval", int.class, int.class);
        getInterval.setAccessible(true);

        int result = (int) getInterval.invoke(null, 2, 100);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void should_ReturnCorrectInterval_WhenNormalCase() throws Exception {
        Method getInterval = HoldemSimulator.class.getDeclaredMethod("getInterval", int.class, int.class);
        getInterval.setAccessible(true);

        int result = (int) getInterval.invoke(null, 1000, 10);

        assertThat(result).isEqualTo(100); // 1000 / 10
    }

    @Test
    void should_Cap50000_WhenIntervalExceeds50000() throws Exception {
        Method getInterval = HoldemSimulator.class.getDeclaredMethod("getInterval", int.class, int.class);
        getInterval.setAccessible(true);

        int result = (int) getInterval.invoke(null, 10000000, 10);

        assertThat(result).isEqualTo(50000); // capped at 50000
    }

    // ========== nCm() Combinatorics Tests ==========

    @Test
    void should_Return0_WhenMGreaterThanN() {
        BigInteger result = HoldemSimulator.nCm(5, 10);

        assertThat(result).isEqualTo(BigInteger.ZERO);
    }

    @Test
    void should_Return1_WhenMEqualsN() {
        BigInteger result = HoldemSimulator.nCm(7, 7);

        assertThat(result).isEqualTo(BigInteger.ONE);
    }

    @Test
    void should_Return1_WhenMIsZero() {
        BigInteger result = HoldemSimulator.nCm(10, 0);

        assertThat(result).isEqualTo(BigInteger.ONE);
    }

    @Test
    void should_ReturnN_WhenMIsOne() {
        BigInteger result = HoldemSimulator.nCm(15, 1);

        assertThat(result).isEqualTo(BigInteger.valueOf(15));
    }

    @Test
    void should_Calculate5Choose2() {
        BigInteger result = HoldemSimulator.nCm(5, 2);

        // 5! / (2! * 3!) = 120 / (2 * 6) = 10
        assertThat(result).isEqualTo(BigInteger.valueOf(10));
    }

    @Test
    void should_Calculate52Choose5() {
        BigInteger result = HoldemSimulator.nCm(52, 5);

        // Number of 5-card poker hands
        assertThat(result).isEqualTo(BigInteger.valueOf(2598960));
    }

    @Test
    void should_Calculate10Choose3() {
        BigInteger result = HoldemSimulator.nCm(10, 3);

        // 10! / (3! * 7!) = 720 / 6 = 120
        assertThat(result).isEqualTo(BigInteger.valueOf(120));
    }

    @Test
    void should_HandleLargeNumbers_When100Choose50() {
        BigInteger result = HoldemSimulator.nCm(100, 50);

        // Should handle large factorial calculations
        assertThat(result).isGreaterThan(BigInteger.ZERO);
        assertThat(result.toString().length()).isGreaterThan(20); // Very large number
    }

    @Test
    void should_BeSymmetric_WhenNChooseM() {
        // nCm(n, m) == nCm(n, n-m)
        BigInteger result1 = HoldemSimulator.nCm(10, 3);
        BigInteger result2 = HoldemSimulator.nCm(10, 7);

        assertThat(result1).isEqualTo(result2);
    }

    // ========== getNumberIterations() Tests ==========

    @Test
    void should_Calculate1Iteration_WhenAllCardsDealt() {
        Hand[] hands = new Hand[2];
        hands[0] = new Hand(Card.SPADES_A, Card.HEARTS_K);
        hands[1] = new Hand(Card.CLUBS_Q, Card.DIAMONDS_J);
        Hand community = new Hand(Card.SPADES_T, Card.HEARTS_9, Card.CLUBS_8, Card.DIAMONDS_7, Card.SPADES_6);

        BigInteger iterations = HoldemSimulator.getNumberIterations(hands, community);

        assertThat(iterations).isEqualTo(BigInteger.ONE); // No blank cards
    }

    @Test
    void should_CalculateIterations_WhenOneBoardBlank() {
        Hand[] hands = new Hand[2];
        hands[0] = new Hand(Card.SPADES_A, Card.HEARTS_K);
        hands[1] = new Hand(Card.CLUBS_Q, Card.DIAMONDS_J);
        Hand community = new Hand(Card.SPADES_T, Card.HEARTS_9, Card.CLUBS_8, Card.BLANK);

        BigInteger iterations = HoldemSimulator.getNumberIterations(hands, community);

        // 52 - 4 (hole cards) - 3 (board cards) = 45 remaining cards
        // Need to fill 1 board card and 1 more = 2 cards
        // But we only have 1 blank, so it's 45 choose 1 = 45
        assertThat(iterations).isEqualTo(BigInteger.valueOf(45));
    }

    @Test
    void should_CalculateIterations_WhenOneHandBlank() {
        Hand[] hands = new Hand[1];
        hands[0] = new Hand(Card.SPADES_A, Card.BLANK);
        Hand community = new Hand(Card.HEARTS_K, Card.CLUBS_Q, Card.DIAMONDS_J, Card.SPADES_T, Card.HEARTS_9);

        BigInteger iterations = HoldemSimulator.getNumberIterations(hands, community);

        // 52 - 1 (known hole card) - 5 (board) = 46 remaining
        // 46 choose 1 = 46
        assertThat(iterations).isEqualTo(BigInteger.valueOf(46));
    }

    @Test
    void should_CalculateIterations_WhenMultipleBlanks() {
        Hand[] hands = new Hand[1];
        hands[0] = new Hand(Card.BLANK, Card.BLANK);
        Hand community = new Hand(Card.BLANK, Card.BLANK, Card.BLANK, Card.BLANK, Card.BLANK);

        BigInteger iterations = HoldemSimulator.getNumberIterations(hands, community);

        // All 52 cards available, need 7 cards total
        // 52 choose 2 for hand * 50 choose 5 for community
        // = 1326 * 2118760 = 2,809,475,760
        BigInteger expected = HoldemSimulator.nCm(52, 2).multiply(HoldemSimulator.nCm(50, 5));

        assertThat(iterations).isEqualTo(expected);
    }

    // ========== Basic Simulation Tests ==========

    @Test
    void should_ReturnNonNullResult_WhenSimulatingPocketAces() {
        Hand hole = new Hand(Card.SPADES_A, Card.HEARTS_A);
        Hand community = new Hand();
        DDProgressFeedback progress = new TestProgressFeedback();

        StatResults results = HoldemSimulator.simulate(hole, community, progress);

        assertThat(results).isNotNull();
        assertThat(results.size()).isGreaterThan(0); // Should have some results
    }

    @Test
    void should_ReturnNonNullResult_WhenSimulatingWithFlop() {
        Hand hole = new Hand(Card.SPADES_A, Card.HEARTS_K);
        Hand community = new Hand(Card.CLUBS_A, Card.DIAMONDS_K, Card.SPADES_2);
        DDProgressFeedback progress = new TestProgressFeedback();

        StatResults results = HoldemSimulator.simulate(hole, community, progress);

        assertThat(results).isNotNull();
        assertThat(results.size()).isGreaterThan(0);
    }

    @Test
    void should_ReturnNonNullResult_WhenSimulatingWithTurn() {
        Hand hole = new Hand(Card.SPADES_A, Card.HEARTS_K);
        Hand community = new Hand(Card.CLUBS_A, Card.DIAMONDS_K, Card.SPADES_2, Card.HEARTS_3);
        DDProgressFeedback progress = new TestProgressFeedback();

        StatResults results = HoldemSimulator.simulate(hole, community, progress);

        assertThat(results).isNotNull();
        assertThat(results.size()).isGreaterThan(0);
    }

    @Test
    void should_ReturnNonNullResult_WhenSimulatingWithRiver() {
        Hand hole = new Hand(Card.SPADES_A, Card.HEARTS_K);
        Hand community = new Hand(Card.CLUBS_A, Card.DIAMONDS_K, Card.SPADES_2, Card.HEARTS_3, Card.CLUBS_4);
        DDProgressFeedback progress = new TestProgressFeedback();

        StatResults results = HoldemSimulator.simulate(hole, community, progress);

        assertThat(results).isNotNull();
        assertThat(results.size()).isGreaterThan(0);
    }

    // ========== Multi-Hand Simulation Tests ==========

    @Test
    void should_ReturnArrayOfResults_WhenSimulatingMultipleHands() {
        Hand[] hands = new Hand[3];
        hands[0] = new Hand(Card.SPADES_A, Card.HEARTS_A);
        hands[1] = new Hand(Card.CLUBS_K, Card.DIAMONDS_K);
        hands[2] = new Hand(Card.SPADES_Q, Card.HEARTS_Q);
        Hand community = new Hand();

        StatResult[] results = HoldemSimulator.simulate(hands, community, 100, null);

        assertThat(results).hasSize(3);
        assertThat(results[0]).isNotNull();
        assertThat(results[1]).isNotNull();
        assertThat(results[2]).isNotNull();
    }

    @Test
    void should_OnlySimulateActiveHands_WhenSomeHandsAreNull() {
        Hand[] hands = new Hand[3];
        hands[0] = new Hand(Card.SPADES_A, Card.HEARTS_A);
        hands[1] = null; // Folded
        hands[2] = new Hand(Card.CLUBS_K, Card.DIAMONDS_K);
        Hand community = new Hand();

        StatResult[] results = HoldemSimulator.simulate(hands, community, 100, null);

        assertThat(results).hasSize(3);
        assertThat(results[0]).isNotNull();
        assertThat(results[1]).isNull();
        assertThat(results[2]).isNotNull();
    }

    @Test
    void should_CompletePartialHands_WhenSimulating() {
        Hand[] hands = new Hand[1];
        Hand partialHand = new Hand();
        partialHand.addCard(Card.SPADES_A); // Only 1 card
        partialHand.addCard(Card.BLANK); // Need to fill this
        hands[0] = partialHand;
        Hand community = new Hand(Card.BLANK, Card.BLANK, Card.BLANK, Card.BLANK, Card.BLANK);

        StatResult[] results = HoldemSimulator.simulate(hands, community, 10, null);

        assertThat(results).hasSize(1);
        assertThat(results[0]).isNotNull();
        // Should have completed the hand and community
    }

    @Test
    void should_CompletePartialCommunity_WhenSimulating() {
        Hand[] hands = new Hand[2];
        hands[0] = new Hand(Card.SPADES_A, Card.HEARTS_K);
        hands[1] = new Hand(Card.CLUBS_Q, Card.DIAMONDS_J);
        Hand community = new Hand(Card.SPADES_T, Card.HEARTS_9); // Only 2 cards

        StatResult[] results = HoldemSimulator.simulate(hands, community, 10, null);

        assertThat(results).hasSize(2);
        assertThat(results[0]).isNotNull();
        assertThat(results[1]).isNotNull();
    }

    // ========== Edge Cases ==========

    @Test
    void should_HandleZeroHandCount_WhenSimulating() {
        Hand hole = new Hand(Card.SPADES_A, Card.HEARTS_K);
        Hand community = new Hand();

        Hand[] hands = new Hand[]{hole};
        StatResult[] results = HoldemSimulator.simulate(hands, community, 0, null);

        assertThat(results).hasSize(1);
        // Should handle gracefully even with 0 iterations
    }

    @Test
    void should_HandleNullCommunity_WhenSimulating() {
        Hand[] hands = new Hand[1];
        hands[0] = new Hand(Card.SPADES_A, Card.HEARTS_K);

        StatResult[] results = HoldemSimulator.simulate(hands, null, 10, null);

        assertThat(results).hasSize(1);
        assertThat(results[0]).isNotNull();
    }

    @Test
    void should_HandleSingleHand_WhenSimulating() {
        Hand[] hands = new Hand[1];
        hands[0] = new Hand(Card.SPADES_A, Card.HEARTS_K);
        Hand community = new Hand();

        StatResult[] results = HoldemSimulator.simulate(hands, community, 10, null);

        assertThat(results).hasSize(1);
        assertThat(results[0]).isNotNull();
        // With only 1 hand, should always win (no opponents)
    }
}
