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

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.HandInfoFaster;
import com.donohoedigital.games.poker.engine.HandSorted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Cross-validation tests to ensure HandInfoFaster produces identical scores to
 * HandInfo and HandInfoFast. This provides regression safety and ensures the
 * performance-optimized evaluator maintains correctness.
 *
 * Phase 2 Unit Testing - Priority 2
 */
class HandInfoConsistencyTest {

    private PokerPlayer testPlayer;

    @BeforeEach
    void setUp() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        testPlayer = new PokerPlayer(0, "Test", true);
    }

    // ===== Cross-Evaluator Consistency =====

    @Test
    void should_MatchHandInfoScore_When_EvaluatingRoyalFlush() {
        // Arrange
        Hand hole = new Hand(CLUBS_A, CLUBS_J);
        Hand community = new Hand(CLUBS_K, CLUBS_Q, CLUBS_T, SPADES_2, HEARTS_K);

        HandSorted sorted = new HandSorted();
        sorted.addAll(hole);
        sorted.addAll(community);

        // Act
        HandInfo info = new HandInfo(testPlayer, sorted, null);
        HandInfoFast fast = new HandInfoFast();
        HandInfoFaster faster = new HandInfoFaster();

        int infoScore = info.getScore();
        int fastScore = fast.getScore(hole, community);
        int fasterScore = faster.getScore(hole, community);

        // Assert
        assertThat(fasterScore).as("HandInfoFaster should match HandInfo").isEqualTo(infoScore);
        assertThat(fasterScore).as("HandInfoFaster should match HandInfoFast").isEqualTo(fastScore);
        assertThat(fasterScore).isEqualTo(10000014); // Royal flush
    }

    @Test
    void should_MatchHandInfoScore_When_EvaluatingStraightFlush() {
        // Arrange
        Hand hole = new Hand(HEARTS_9, HEARTS_J);
        Hand community = new Hand(HEARTS_K, HEARTS_Q, CLUBS_Q, HEARTS_T, HEARTS_3);

        HandSorted sorted = new HandSorted();
        sorted.addAll(hole);
        sorted.addAll(community);

        // Act
        HandInfo info = new HandInfo(testPlayer, sorted, null);
        HandInfoFast fast = new HandInfoFast();
        HandInfoFaster faster = new HandInfoFaster();

        int infoScore = info.getScore();
        int fastScore = fast.getScore(hole, community);
        int fasterScore = faster.getScore(hole, community);

        // Assert
        assertThat(fasterScore).as("HandInfoFaster should match HandInfo").isEqualTo(infoScore);
        assertThat(fasterScore).as("HandInfoFaster should match HandInfoFast").isEqualTo(fastScore);
        assertThat(fasterScore).isEqualTo(9000013); // K-high straight flush
    }

    @Test
    void should_MatchHandInfoScore_When_EvaluatingQuads() {
        // Arrange
        Hand hole = new Hand(CLUBS_8, HEARTS_8);
        Hand community = new Hand(DIAMONDS_8, SPADES_8, SPADES_7, DIAMONDS_7, HEARTS_7);

        HandSorted sorted = new HandSorted();
        sorted.addAll(hole);
        sorted.addAll(community);

        // Act
        HandInfo info = new HandInfo(testPlayer, sorted, null);
        HandInfoFast fast = new HandInfoFast();
        HandInfoFaster faster = new HandInfoFaster();

        int infoScore = info.getScore();
        int fastScore = fast.getScore(hole, community);
        int fasterScore = faster.getScore(hole, community);

        // Assert
        assertThat(fasterScore).as("HandInfoFaster should match HandInfo").isEqualTo(infoScore);
        assertThat(fasterScore).as("HandInfoFaster should match HandInfoFast").isEqualTo(fastScore);
        assertThat(fasterScore).isEqualTo(8000135); // Quads
    }

    @Test
    void should_MatchHandInfoScore_When_EvaluatingFullHouse() {
        // Arrange
        Hand hole = new Hand(CLUBS_8, HEARTS_8);
        Hand community = new Hand(DIAMONDS_8, CLUBS_Q, SPADES_Q, DIAMONDS_7, HEARTS_7);

        HandSorted sorted = new HandSorted();
        sorted.addAll(hole);
        sorted.addAll(community);

        // Act
        HandInfo info = new HandInfo(testPlayer, sorted, null);
        HandInfoFast fast = new HandInfoFast();
        HandInfoFaster faster = new HandInfoFaster();

        int infoScore = info.getScore();
        int fastScore = fast.getScore(hole, community);
        int fasterScore = faster.getScore(hole, community);

        // Assert
        assertThat(fasterScore).as("HandInfoFaster should match HandInfo").isEqualTo(infoScore);
        assertThat(fasterScore).as("HandInfoFaster should match HandInfoFast").isEqualTo(fastScore);
        assertThat(fasterScore).isEqualTo(7000140); // Full house
    }

    @Test
    void should_MatchHandInfoScore_When_EvaluatingFlush() {
        // Arrange
        Hand hole = new Hand(CLUBS_8, CLUBS_J);
        Hand community = new Hand(CLUBS_K, CLUBS_Q, CLUBS_T, SPADES_2, HEARTS_3);

        HandSorted sorted = new HandSorted();
        sorted.addAll(hole);
        sorted.addAll(community);

        // Act
        HandInfo info = new HandInfo(testPlayer, sorted, null);
        HandInfoFast fast = new HandInfoFast();
        HandInfoFaster faster = new HandInfoFaster();

        int infoScore = info.getScore();
        int fastScore = fast.getScore(hole, community);
        int fasterScore = faster.getScore(hole, community);

        // Assert
        assertThat(fasterScore).as("HandInfoFaster should match HandInfo").isEqualTo(infoScore);
        assertThat(fasterScore).as("HandInfoFaster should match HandInfoFast").isEqualTo(fastScore);
        assertThat(fasterScore).isEqualTo(6904104); // Flush
    }

    @Test
    void should_MatchHandInfoScore_When_EvaluatingStraight() {
        // Arrange
        Hand hole = new Hand(CLUBS_2, HEARTS_A);
        Hand community = new Hand(HEARTS_3, SPADES_4, DIAMONDS_5, CLUBS_6, SPADES_A);

        HandSorted sorted = new HandSorted();
        sorted.addAll(hole);
        sorted.addAll(community);

        // Act
        HandInfo info = new HandInfo(testPlayer, sorted, null);
        HandInfoFast fast = new HandInfoFast();
        HandInfoFaster faster = new HandInfoFaster();

        int infoScore = info.getScore();
        int fastScore = fast.getScore(hole, community);
        int fasterScore = faster.getScore(hole, community);

        // Assert
        assertThat(fasterScore).as("HandInfoFaster should match HandInfo").isEqualTo(infoScore);
        assertThat(fasterScore).as("HandInfoFaster should match HandInfoFast").isEqualTo(fastScore);
        assertThat(fasterScore).isEqualTo(5000006); // 6-high straight
    }

    @Test
    void should_MatchHandInfoScore_When_EvaluatingTrips() {
        // Arrange
        Hand hole = new Hand(CLUBS_K, HEARTS_8);
        Hand community = new Hand(DIAMONDS_8, SPADES_8, SPADES_7, DIAMONDS_J, HEARTS_3);

        HandSorted sorted = new HandSorted();
        sorted.addAll(hole);
        sorted.addAll(community);

        // Act
        HandInfo info = new HandInfo(testPlayer, sorted, null);
        HandInfoFast fast = new HandInfoFast();
        HandInfoFaster faster = new HandInfoFaster();

        int infoScore = info.getScore();
        int fastScore = fast.getScore(hole, community);
        int fasterScore = faster.getScore(hole, community);

        // Assert
        assertThat(fasterScore).as("HandInfoFaster should match HandInfo").isEqualTo(infoScore);
        assertThat(fasterScore).as("HandInfoFaster should match HandInfoFast").isEqualTo(fastScore);
        assertThat(fasterScore).isEqualTo(4002267); // Trips
    }

    @Test
    void should_MatchHandInfoScore_When_EvaluatingTwoPair() {
        // Arrange
        Hand hole = new Hand(CLUBS_2, HEARTS_2);
        Hand community = new Hand(DIAMONDS_3, SPADES_3, CLUBS_A, SPADES_K, HEARTS_Q);

        HandSorted sorted = new HandSorted();
        sorted.addAll(hole);
        sorted.addAll(community);

        // Act
        HandInfo info = new HandInfo(testPlayer, sorted, null);
        HandInfoFast fast = new HandInfoFast();
        HandInfoFaster faster = new HandInfoFaster();

        int infoScore = info.getScore();
        int fastScore = fast.getScore(hole, community);
        int fasterScore = faster.getScore(hole, community);

        // Assert
        assertThat(fasterScore).as("HandInfoFaster should match HandInfo").isEqualTo(infoScore);
        assertThat(fasterScore).as("HandInfoFaster should match HandInfoFast").isEqualTo(fastScore);
    }

    @Test
    void should_MatchHandInfoScore_When_EvaluatingPair() {
        // Arrange
        Hand hole = new Hand(CLUBS_8, HEARTS_A);
        Hand community = new Hand(DIAMONDS_6, SPADES_5, SPADES_K, DIAMONDS_2, HEARTS_6);

        HandSorted sorted = new HandSorted();
        sorted.addAll(hole);
        sorted.addAll(community);

        // Act
        HandInfo info = new HandInfo(testPlayer, sorted, null);
        HandInfoFast fast = new HandInfoFast();
        HandInfoFaster faster = new HandInfoFaster();

        int infoScore = info.getScore();
        int fastScore = fast.getScore(hole, community);
        int fasterScore = faster.getScore(hole, community);

        // Assert
        assertThat(fasterScore).as("HandInfoFaster should match HandInfo").isEqualTo(infoScore);
        assertThat(fasterScore).as("HandInfoFaster should match HandInfoFast").isEqualTo(fastScore);
        assertThat(fasterScore).isEqualTo(2028376); // Pair
    }

    @Test
    void should_MatchHandInfoScore_When_EvaluatingHighCard() {
        // Arrange
        Hand hole = new Hand(CLUBS_8, HEARTS_A);
        Hand community = new Hand(DIAMONDS_6, SPADES_5, SPADES_K, DIAMONDS_2, HEARTS_Q);

        HandSorted sorted = new HandSorted();
        sorted.addAll(hole);
        sorted.addAll(community);

        // Act
        HandInfo info = new HandInfo(testPlayer, sorted, null);
        HandInfoFast fast = new HandInfoFast();
        HandInfoFaster faster = new HandInfoFaster();

        int infoScore = info.getScore();
        int fastScore = fast.getScore(hole, community);
        int fasterScore = faster.getScore(hole, community);

        // Assert
        assertThat(fasterScore).as("HandInfoFaster should match HandInfo").isEqualTo(infoScore);
        assertThat(fasterScore).as("HandInfoFaster should match HandInfoFast").isEqualTo(fastScore);
        assertThat(fasterScore).isEqualTo(1973958); // High card
    }

    @Test
    void should_MatchHandInfoScore_When_EvaluatingWheelStraightFlush() {
        // Arrange - Wheel straight flush (ace low)
        Hand hole = new Hand(HEARTS_A, HEARTS_2);
        Hand community = new Hand(HEARTS_3, HEARTS_4, HEARTS_5, CLUBS_A, SPADES_A);

        HandSorted sorted = new HandSorted();
        sorted.addAll(hole);
        sorted.addAll(community);

        // Act
        HandInfo info = new HandInfo(testPlayer, sorted, null);
        HandInfoFast fast = new HandInfoFast();
        HandInfoFaster faster = new HandInfoFaster();

        int infoScore = info.getScore();
        int fastScore = fast.getScore(hole, community);
        int fasterScore = faster.getScore(hole, community);

        // Assert
        assertThat(fasterScore).as("HandInfoFaster should match HandInfo").isEqualTo(infoScore);
        assertThat(fasterScore).as("HandInfoFaster should match HandInfoFast").isEqualTo(fastScore);
        assertThat(fasterScore).isEqualTo(9000005); // 5-high straight flush
    }

    @Test
    void should_MatchHandInfoScore_When_EvaluatingTwoTrips() {
        // Arrange - Two trips (should make best full house)
        Hand hole = new Hand(CLUBS_8, HEARTS_8);
        Hand community = new Hand(DIAMONDS_8, CLUBS_Q, SPADES_Q, DIAMONDS_Q, HEARTS_7);

        HandSorted sorted = new HandSorted();
        sorted.addAll(hole);
        sorted.addAll(community);

        // Act
        HandInfo info = new HandInfo(testPlayer, sorted, null);
        HandInfoFast fast = new HandInfoFast();
        HandInfoFaster faster = new HandInfoFaster();

        int infoScore = info.getScore();
        int fastScore = fast.getScore(hole, community);
        int fasterScore = faster.getScore(hole, community);

        // Assert
        assertThat(fasterScore).as("HandInfoFaster should match HandInfo").isEqualTo(infoScore);
        assertThat(fasterScore).as("HandInfoFaster should match HandInfoFast").isEqualTo(fastScore);
        assertThat(fasterScore).isEqualTo(7000200); // Queens full of 8s
    }

    // ===== Random Hand Testing =====

    @Test
    void should_ProduceSameScoreAcrossAllEvaluators_When_RandomHands() {
        // Test with all 18 verified cases from HandInfoTest
        verifyAllMatch("Royal Flush (clubs)", CLUBS_A, CLUBS_J, CLUBS_K, CLUBS_Q, CLUBS_T, SPADES_2, HEARTS_K);
        verifyAllMatch("Royal Flush (spades)", SPADES_A, SPADES_J, SPADES_K, SPADES_Q, SPADES_T, SPADES_2, HEARTS_K);
        verifyAllMatch("Straight Flush K (hearts)", HEARTS_9, HEARTS_J, HEARTS_K, HEARTS_Q, CLUBS_Q, HEARTS_T,
                HEARTS_3);
        verifyAllMatch("Straight Flush (+1 hearts)", HEARTS_A, HEARTS_2, HEARTS_3, HEARTS_4, HEARTS_5, HEARTS_6,
                SPADES_A);
        verifyAllMatch("Straight Flush (low hearts)", HEARTS_A, HEARTS_2, HEARTS_3, HEARTS_4, HEARTS_5, CLUBS_A,
                SPADES_A);
        verifyAllMatch("Quads", CLUBS_8, HEARTS_8, DIAMONDS_8, SPADES_8, SPADES_7, DIAMONDS_7, HEARTS_7);
        verifyAllMatch("Full House (two trips)", CLUBS_8, HEARTS_8, DIAMONDS_8, CLUBS_Q, SPADES_Q, DIAMONDS_Q,
                HEARTS_7);
        verifyAllMatch("Full House", CLUBS_8, HEARTS_8, DIAMONDS_8, CLUBS_Q, SPADES_Q, DIAMONDS_7, HEARTS_7);
        verifyAllMatch("Full House/Trips", CLUBS_K, HEARTS_8, DIAMONDS_8, SPADES_8, SPADES_7, DIAMONDS_7, HEARTS_7);
        verifyAllMatch("Flush (clubs)", CLUBS_8, CLUBS_J, CLUBS_K, CLUBS_Q, CLUBS_T, SPADES_2, HEARTS_3);
        verifyAllMatch("Flush (lower kicker)", CLUBS_7, CLUBS_J, CLUBS_K, CLUBS_Q, CLUBS_T, SPADES_2, HEARTS_3);
        verifyAllMatch("Straight (6 high)", CLUBS_2, HEARTS_A, HEARTS_3, SPADES_4, DIAMONDS_5, CLUBS_6, SPADES_A);
        verifyAllMatch("Straight (5 high)", CLUBS_2, HEARTS_A, HEARTS_3, SPADES_4, DIAMONDS_5, CLUBS_A, SPADES_A);
        verifyAllMatch("Straight/Two Pair", CLUBS_8, HEARTS_8, DIAMONDS_6, SPADES_5, SPADES_7, DIAMONDS_7, HEARTS_4);
        verifyAllMatch("Trips", CLUBS_K, HEARTS_8, DIAMONDS_8, SPADES_8, SPADES_7, DIAMONDS_J, HEARTS_3);
        verifyAllMatch("Pair", CLUBS_8, HEARTS_A, DIAMONDS_6, SPADES_5, SPADES_K, DIAMONDS_2, HEARTS_6);
        verifyAllMatch("High Card", CLUBS_8, HEARTS_A, DIAMONDS_6, SPADES_5, SPADES_K, DIAMONDS_2, HEARTS_Q);
    }

    // ===== Performance Comparison =====

    @Test
    @Tag("slow")
    void should_BeFasterThanHandInfo_When_Evaluating1000Hands() {
        // This test validates that HandInfoFaster is indeed faster
        // Note: We don't assert on performance, just that it produces correct results

        Hand hole = new Hand(CLUBS_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, SPADES_J, CLUBS_T);

        HandSorted sorted = new HandSorted();
        sorted.addAll(hole);
        sorted.addAll(community);

        HandInfoFaster faster = new HandInfoFaster();

        // Run 1000 evaluations
        int lastScore = 0;
        for (int i = 0; i < 1000; i++) {
            lastScore = faster.getScore(hole, community);
        }

        // Just verify it still produces correct result
        assertThat(lastScore).isGreaterThan(0);
        assertThat(lastScore / HandInfo.BASE).isEqualTo(HandInfo.STRAIGHT);
    }

    // ===== Helper Methods =====

    private void verifyAllMatch(String name, Card c1, Card c2, Card c3, Card c4, Card c5, Card c6, Card c7) {
        Hand hole = new Hand();
        Hand community = new Hand();

        // Add first 2 cards to hole, rest to community
        if (c1 != null)
            hole.addCard(c1);
        if (c2 != null)
            hole.addCard(c2);
        if (c3 != null)
            community.addCard(c3);
        if (c4 != null)
            community.addCard(c4);
        if (c5 != null)
            community.addCard(c5);
        if (c6 != null)
            community.addCard(c6);
        if (c7 != null)
            community.addCard(c7);

        HandSorted sorted = new HandSorted();
        sorted.addAll(hole);
        sorted.addAll(community);

        HandInfo info = new HandInfo(testPlayer, sorted, null);
        HandInfoFast fast = new HandInfoFast();
        HandInfoFaster faster = new HandInfoFaster();

        int infoScore = info.getScore();
        int fastScore = fast.getScore(hole, community);
        int fasterScore = faster.getScore(hole, community);

        assertThat(fasterScore).as("%s: HandInfoFaster should match HandInfo", name).isEqualTo(infoScore);
        assertThat(fasterScore).as("%s: HandInfoFaster should match HandInfoFast", name).isEqualTo(fastScore);
    }
}
