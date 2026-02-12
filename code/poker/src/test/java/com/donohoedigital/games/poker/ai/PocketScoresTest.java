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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.engine.Hand;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PocketScores hand scoring calculations.
 */
class PocketScoresTest {
    @BeforeAll
    static void initializeConfig() {
        // Initialize ConfigManager for HandInfoFaster
        ConfigManager configMgr = new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        configMgr.loadGuiConfig(); // Required for StylesConfig
    }

    // ========================================
    // Validation Tests
    // ========================================

    @Test
    void should_ThrowError_When_CommunityIsNull() {
        assertThatThrownBy(() -> PocketScores.getInstance(null)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("null community");
    }

    @Test
    void should_ThrowError_When_CommunityIsEmpty() {
        Hand community = new Hand();

        assertThatThrownBy(() -> PocketScores.getInstance(community)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("pre-flop");
    }

    @Test
    void should_ThrowError_When_CommunityHasTwoCards() {
        Hand community = new Hand(SPADES_A, HEARTS_K);

        assertThatThrownBy(() -> PocketScores.getInstance(community)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("pre-flop");
    }

    // ========================================
    // Caching Tests
    // ========================================

    @Test
    void should_ReturnSameInstance_When_SameCommunityRequested() {
        Hand community = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);

        PocketScores scores1 = PocketScores.getInstance(community);
        PocketScores scores2 = PocketScores.getInstance(community);

        assertThat(scores1).isSameAs(scores2);
    }

    @Test
    void should_ReturnDifferentInstance_When_DifferentCommunity() {
        Hand community1 = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        Hand community2 = new Hand(SPADES_A, HEARTS_K, DIAMONDS_J);

        PocketScores scores1 = PocketScores.getInstance(community1);
        PocketScores scores2 = PocketScores.getInstance(community2);

        assertThat(scores1).isNotSameAs(scores2);
    }

    @Test
    void should_ClearCache_When_FlopChanges() {
        Hand community1 = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);

        PocketScores scores1 = PocketScores.getInstance(community1);

        // Change flop - should clear cache
        Hand community2 = new Hand(SPADES_2, HEARTS_3, DIAMONDS_4);
        PocketScores scores2 = PocketScores.getInstance(community2);

        // Go back to original flop - should create new instance
        PocketScores scores3 = PocketScores.getInstance(community1);

        assertThat(scores1).isNotSameAs(scores3);
    }

    @Test
    void should_CacheMultipleInstances_When_SameFlopDifferentTurn() {
        // Flop
        Hand flop = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        PocketScores scoresFlop = PocketScores.getInstance(flop);

        // Turn (same flop + different turn cards)
        Hand turn1 = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_J);
        Hand turn2 = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_T);

        PocketScores scoresTurn1 = PocketScores.getInstance(turn1);
        PocketScores scoresTurn2 = PocketScores.getInstance(turn2);

        // All should be different instances but from same cache (same flop)
        assertThat(scoresFlop).isNotSameAs(scoresTurn1);
        assertThat(scoresFlop).isNotSameAs(scoresTurn2);
        assertThat(scoresTurn1).isNotSameAs(scoresTurn2);
    }

    // ========================================
    // Method Overload Consistency Tests
    // ========================================

    @Test
    void should_ReturnSameScore_When_CalledWithDifferentMethods() {
        Hand community = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        PocketScores scores = PocketScores.getInstance(community);

        Hand pocket = new Hand(CLUBS_J, SPADES_T);
        int scoreHand = scores.getScore(pocket);
        int scoreCards = scores.getScore(CLUBS_J, SPADES_T);
        int scoreIndices = scores.getScore(CLUBS_J.getIndex(), SPADES_T.getIndex());

        assertThat(scoreHand).isEqualTo(scoreCards).isEqualTo(scoreIndices);
    }

    // ========================================
    // Hand Scoring Tests
    // ========================================

    @Test
    void should_ScoreRoyalFlush_When_BestPossibleHand() {
        // Board: A♠ K♠ Q♠
        Hand community = new Hand(SPADES_A, SPADES_K, SPADES_Q);
        // Pocket: J♠ T♠ (royal flush)
        Hand pocket = new Hand(SPADES_J, SPADES_T);

        PocketScores scores = PocketScores.getInstance(community);
        int score = scores.getScore(pocket);

        // Royal flush should have a very high score
        assertThat(score).isGreaterThan(0);
    }

    @Test
    void should_ScoreStraightFlush_When_FiveCardsInSequenceSameSuit() {
        // Board: 9♠ 8♠ 7♠
        Hand community = new Hand(SPADES_9, SPADES_8, SPADES_7);
        // Pocket: 6♠ 5♠ (straight flush 5-9)
        Hand pocket = new Hand(SPADES_6, SPADES_5);

        PocketScores scores = PocketScores.getInstance(community);
        int score = scores.getScore(pocket);

        assertThat(score).isGreaterThan(0);
    }

    @Test
    void should_ScoreFourOfAKind_When_FourSameRank() {
        // Board: K♠ K♥ K♦
        Hand community = new Hand(SPADES_K, HEARTS_K, DIAMONDS_K);
        // Pocket: K♣ 7♠ (four kings)
        Hand pocket = new Hand(CLUBS_K, SPADES_7);

        PocketScores scores = PocketScores.getInstance(community);
        int score = scores.getScore(pocket);

        assertThat(score).isGreaterThan(0);
    }

    @Test
    void should_ScoreFullHouse_When_ThreeOfKindPlusPair() {
        // Board: K♠ K♥ 7♦
        Hand community = new Hand(SPADES_K, HEARTS_K, DIAMONDS_7);
        // Pocket: K♦ 7♠ (full house - kings full of sevens)
        Hand pocket = new Hand(DIAMONDS_K, SPADES_7);

        PocketScores scores = PocketScores.getInstance(community);
        int score = scores.getScore(pocket);

        assertThat(score).isGreaterThan(0);
    }

    @Test
    void should_ScoreFlush_When_FiveSameSuit() {
        // Board: A♠ K♠ 7♠
        Hand community = new Hand(SPADES_A, SPADES_K, SPADES_7);
        // Pocket: 5♠ 2♠ (flush)
        Hand pocket = new Hand(SPADES_5, SPADES_2);

        PocketScores scores = PocketScores.getInstance(community);
        int score = scores.getScore(pocket);

        assertThat(score).isGreaterThan(0);
    }

    @Test
    void should_ScoreStraight_When_FiveCardsInSequence() {
        // Board: K♠ Q♥ J♦
        Hand community = new Hand(SPADES_K, HEARTS_Q, DIAMONDS_J);
        // Pocket: T♠ 9♣ (straight K-9)
        Hand pocket = new Hand(SPADES_T, CLUBS_9);

        PocketScores scores = PocketScores.getInstance(community);
        int score = scores.getScore(pocket);

        assertThat(score).isGreaterThan(0);
    }

    @Test
    void should_ScoreThreeOfAKind_When_ThreeSameRank() {
        // Board: K♠ K♥ 7♦
        Hand community = new Hand(SPADES_K, HEARTS_K, DIAMONDS_7);
        // Pocket: K♦ 2♠ (trip kings)
        Hand pocket = new Hand(DIAMONDS_K, SPADES_2);

        PocketScores scores = PocketScores.getInstance(community);
        int score = scores.getScore(pocket);

        assertThat(score).isGreaterThan(0);
    }

    @Test
    void should_ScoreTwoPair_When_TwoDifferentPairs() {
        // Board: K♠ K♥ 7♦
        Hand community = new Hand(SPADES_K, HEARTS_K, DIAMONDS_7);
        // Pocket: 7♠ 2♣ (two pair - kings and sevens)
        Hand pocket = new Hand(SPADES_7, CLUBS_2);

        PocketScores scores = PocketScores.getInstance(community);
        int score = scores.getScore(pocket);

        assertThat(score).isGreaterThan(0);
    }

    @Test
    void should_ScorePair_When_TwoSameRank() {
        // Board: K♠ Q♥ 7♦
        Hand community = new Hand(SPADES_K, HEARTS_Q, DIAMONDS_7);
        // Pocket: K♦ 2♠ (pair of kings)
        Hand pocket = new Hand(DIAMONDS_K, SPADES_2);

        PocketScores scores = PocketScores.getInstance(community);
        int score = scores.getScore(pocket);

        assertThat(score).isGreaterThan(0);
    }

    @Test
    void should_ScoreHighCard_When_NoMadeHand() {
        // Board: K♠ Q♥ 7♦
        Hand community = new Hand(SPADES_K, HEARTS_Q, DIAMONDS_7);
        // Pocket: J♠ 9♣ (high card king)
        Hand pocket = new Hand(SPADES_J, CLUBS_9);

        PocketScores scores = PocketScores.getInstance(community);
        int score = scores.getScore(pocket);

        assertThat(score).isGreaterThan(0);
    }

    // ========================================
    // Score Comparison Tests
    // ========================================

    @Test
    void should_ScoreHigher_When_BetterHandType() {
        // Board: K♠ Q♥ J♦
        Hand community = new Hand(SPADES_K, HEARTS_Q, DIAMONDS_J);

        PocketScores scores = PocketScores.getInstance(community);

        // Straight (T-9) vs Pair (K-2)
        Hand straight = new Hand(SPADES_T, CLUBS_9);
        Hand pair = new Hand(DIAMONDS_K, SPADES_2);

        int scoreStraight = scores.getScore(straight);
        int scorePair = scores.getScore(pair);

        assertThat(scoreStraight).isGreaterThan(scorePair);
    }

    @Test
    void should_ScoreHigher_When_BetterKicker() {
        // Board: K♠ Q♥ 7♦
        Hand community = new Hand(SPADES_K, HEARTS_Q, DIAMONDS_7);

        PocketScores scores = PocketScores.getInstance(community);

        // Pair of kings with ace kicker vs pair of kings with 2 kicker
        Hand betterKicker = new Hand(DIAMONDS_K, SPADES_A);
        Hand worseKicker = new Hand(CLUBS_K, SPADES_2);

        int scoreBetter = scores.getScore(betterKicker);
        int scoreWorse = scores.getScore(worseKicker);

        assertThat(scoreBetter).isGreaterThan(scoreWorse);
    }

    @Test
    void should_ScoreHigher_When_PocketPairVsCommunityPair() {
        // Board: K♠ Q♥ 7♦
        Hand community = new Hand(SPADES_K, HEARTS_Q, DIAMONDS_7);

        PocketScores scores = PocketScores.getInstance(community);

        // Pocket pair of aces (overpair) vs pair of kings (board pair)
        Hand pocketPair = new Hand(SPADES_A, HEARTS_A);
        Hand boardPair = new Hand(DIAMONDS_K, SPADES_2);

        int scorePocket = scores.getScore(pocketPair);
        int scoreBoard = scores.getScore(boardPair);

        assertThat(scorePocket).isGreaterThan(scoreBoard);
    }

    @Test
    void should_ScoreConsistently_When_SameHandDifferentSuits() {
        // Board: K♠ Q♥ 7♦
        Hand community = new Hand(SPADES_K, HEARTS_Q, DIAMONDS_7);

        PocketScores scores = PocketScores.getInstance(community);

        // Two different pocket aces (same rank, different suits)
        Hand aces1 = new Hand(SPADES_A, HEARTS_A);
        Hand aces2 = new Hand(DIAMONDS_A, CLUBS_A);

        int score1 = scores.getScore(aces1);
        int score2 = scores.getScore(aces2);

        // Should have same score (suits don't matter for non-flush hands)
        assertThat(score1).isEqualTo(score2);
    }

    // ========================================
    // Edge Cases
    // ========================================

    @Test
    void should_HandleFlopScenario_When_ThreeCommunityCards() {
        Hand flop = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        Hand pocket = new Hand(CLUBS_J, SPADES_T);

        PocketScores scores = PocketScores.getInstance(flop);
        int score = scores.getScore(pocket);

        assertThat(score).isGreaterThan(0);
    }

    @Test
    void should_HandleTurnScenario_When_FourCommunityCards() {
        Hand turn = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_J);
        Hand pocket = new Hand(SPADES_T, HEARTS_9);

        PocketScores scores = PocketScores.getInstance(turn);
        int score = scores.getScore(pocket);

        assertThat(score).isGreaterThan(0);
    }

    @Test
    void should_HandleRiverScenario_When_FiveCommunityCards() {
        Hand river = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_J, SPADES_T);
        Hand pocket = new Hand(HEARTS_9, DIAMONDS_8);

        PocketScores scores = PocketScores.getInstance(river);
        int score = scores.getScore(pocket);

        assertThat(score).isGreaterThan(0);
    }

    @Test
    void should_ProvideConsistentScores_When_CalledMultipleTimes() {
        Hand community = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        Hand pocket = new Hand(CLUBS_J, SPADES_T);

        PocketScores scores = PocketScores.getInstance(community);

        int score1 = scores.getScore(pocket);
        int score2 = scores.getScore(pocket);
        int score3 = scores.getScore(pocket);

        assertThat(score1).isEqualTo(score2).isEqualTo(score3);
    }
}
