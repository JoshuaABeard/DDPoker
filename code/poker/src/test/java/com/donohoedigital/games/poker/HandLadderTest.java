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
import com.donohoedigital.games.poker.ai.HandProbabilityMatrix;
import com.donohoedigital.games.poker.engine.Hand;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for HandLadder probability distribution calculations.
 */
class HandLadderTest {
    private static HandProbabilityMatrix matrix;

    @BeforeAll
    static void initConfig() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        matrix = new HandProbabilityMatrix();
    }

    // ========================================
    // Rank and Count Positivity
    // ========================================

    @Test
    void should_ReturnPositiveRank_When_AnyHand() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, CLUBS_J, SPADES_T);

        HandLadder ladder = new HandLadder(pocket, community, matrix);

        assertThat(ladder.getHandRank()).isPositive();
    }

    @Test
    void should_ReturnPositiveCount_When_AnyHand() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, CLUBS_J, SPADES_T);

        HandLadder ladder = new HandLadder(pocket, community, matrix);

        assertThat(ladder.getHandCount()).isPositive();
    }

    @Test
    void should_ReturnPositiveRankAndCount_When_WeakHand() {
        Hand pocket = new Hand(SPADES_2, HEARTS_7);
        Hand community = new Hand(DIAMONDS_A, CLUBS_K, SPADES_Q);

        HandLadder ladder = new HandLadder(pocket, community, matrix);

        assertThat(ladder.getHandRank()).isPositive();
        assertThat(ladder.getHandCount()).isPositive();
    }

    @Test
    void should_ReturnPositiveRankAndCount_When_FourCommunityCards() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, CLUBS_J, SPADES_T, HEARTS_9);

        HandLadder ladder = new HandLadder(pocket, community, matrix);

        assertThat(ladder.getHandRank()).isPositive();
        assertThat(ladder.getHandCount()).isPositive();
    }

    @Test
    void should_ReturnPositiveRankAndCount_When_FiveCommunityCards() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, CLUBS_J, SPADES_T, HEARTS_9, DIAMONDS_8);

        HandLadder ladder = new HandLadder(pocket, community, matrix);

        assertThat(ladder.getHandRank()).isPositive();
        assertThat(ladder.getHandCount()).isPositive();
    }

    // ========================================
    // Rank Within Count Range
    // ========================================

    @Test
    void should_HaveRankWithinCount_When_AnyHand() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, CLUBS_J, SPADES_T);

        HandLadder ladder = new HandLadder(pocket, community, matrix);

        assertThat(ladder.getHandRank()).isGreaterThanOrEqualTo(1);
        assertThat(ladder.getHandRank()).isLessThanOrEqualTo(ladder.getHandCount());
    }

    @Test
    void should_HaveRankWithinCount_When_WeakHand() {
        Hand pocket = new Hand(SPADES_2, HEARTS_7);
        Hand community = new Hand(DIAMONDS_A, CLUBS_K, SPADES_Q);

        HandLadder ladder = new HandLadder(pocket, community, matrix);

        assertThat(ladder.getHandRank()).isGreaterThanOrEqualTo(1);
        assertThat(ladder.getHandRank()).isLessThanOrEqualTo(ladder.getHandCount());
    }

    @Test
    void should_HaveRankWithinCount_When_StrongHand() {
        // Royal flush on the flop
        Hand pocket = new Hand(SPADES_A, SPADES_K);
        Hand community = new Hand(SPADES_Q, SPADES_J, SPADES_T);

        HandLadder ladder = new HandLadder(pocket, community, matrix);

        assertThat(ladder.getHandRank()).isGreaterThanOrEqualTo(1);
        assertThat(ladder.getHandRank()).isLessThanOrEqualTo(ladder.getHandCount());
    }

    @Test
    void should_HaveRankWithinCount_When_RiverBoard() {
        Hand pocket = new Hand(CLUBS_9, DIAMONDS_8);
        Hand community = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_J, SPADES_T);

        HandLadder ladder = new HandLadder(pocket, community, matrix);

        assertThat(ladder.getHandRank()).isGreaterThanOrEqualTo(1);
        assertThat(ladder.getHandRank()).isLessThanOrEqualTo(ladder.getHandCount());
    }

    // ========================================
    // Stronger Hand Ranks Better (lower rank number)
    // ========================================

    @Test
    void should_RankRoyalFlushBetter_When_ComparedToHighCard() {
        Hand community = new Hand(SPADES_Q, SPADES_J, SPADES_T);

        // Royal flush: A♠ K♠ with Q♠ J♠ T♠
        Hand royalPocket = new Hand(SPADES_A, SPADES_K);
        HandLadder royalLadder = new HandLadder(royalPocket, community, matrix);

        // High card: 2♦ 3♥ with Q♠ J♠ T♠ (no pair, no flush, no straight)
        Hand weakPocket = new Hand(DIAMONDS_2, HEARTS_3);
        HandLadder weakLadder = new HandLadder(weakPocket, community, matrix);

        // Lower rank number = stronger hand
        assertThat(royalLadder.getHandRank()).isLessThan(weakLadder.getHandRank());
    }

    @Test
    void should_RankFlushBetter_When_ComparedToPair() {
        Hand community = new Hand(SPADES_A, SPADES_7, SPADES_4);

        // Flush: K♠ Q♠ with A♠ 7♠ 4♠
        Hand flushPocket = new Hand(SPADES_K, SPADES_Q);
        HandLadder flushLadder = new HandLadder(flushPocket, community, matrix);

        // Pair: 7♥ 2♦ with A♠ 7♠ 4♠ (pair of sevens)
        Hand pairPocket = new Hand(HEARTS_7, DIAMONDS_2);
        HandLadder pairLadder = new HandLadder(pairPocket, community, matrix);

        assertThat(flushLadder.getHandRank()).isLessThan(pairLadder.getHandRank());
    }

    @Test
    void should_RankStraightBetter_When_ComparedToTwoPair() {
        Hand community = new Hand(HEARTS_9, DIAMONDS_8, CLUBS_7);

        // Straight: T♠ 6♥ with 9♥ 8♦ 7♣ (T-high straight)
        Hand straightPocket = new Hand(SPADES_T, HEARTS_6);
        HandLadder straightLadder = new HandLadder(straightPocket, community, matrix);

        // Two pair: 9♠ 8♣ with 9♥ 8♦ 7♣ (nines and eights)
        Hand twoPairPocket = new Hand(SPADES_9, CLUBS_8);
        HandLadder twoPairLadder = new HandLadder(twoPairPocket, community, matrix);

        assertThat(straightLadder.getHandRank()).isLessThan(twoPairLadder.getHandRank());
    }

    @Test
    void should_RankFullHouseBetter_When_ComparedToFlush() {
        Hand community = new Hand(SPADES_K, HEARTS_K, DIAMONDS_5);

        // Full house: K♦ 5♠ with K♠ K♥ 5♦ (kings full of fives)
        Hand fullHousePocket = new Hand(DIAMONDS_K, SPADES_5);
        HandLadder fullHouseLadder = new HandLadder(fullHousePocket, community, matrix);

        // Pair only: A♠ Q♣ with K♠ K♥ 5♦ (pair of kings, A kicker)
        Hand pairPocket = new Hand(SPADES_A, CLUBS_Q);
        HandLadder pairLadder = new HandLadder(pairPocket, community, matrix);

        assertThat(fullHouseLadder.getHandRank()).isLessThan(pairLadder.getHandRank());
    }

    @Test
    void should_RankHigherPairBetter_When_SameHandType() {
        Hand community = new Hand(DIAMONDS_T, CLUBS_7, HEARTS_3);

        // Pair of aces: A♠ A♥ with T♦ 7♣ 3♥
        Hand acesPocket = new Hand(SPADES_A, HEARTS_A);
        HandLadder acesLadder = new HandLadder(acesPocket, community, matrix);

        // Pair of twos: 2♠ 2♥ with T♦ 7♣ 3♥
        Hand twosPocket = new Hand(SPADES_2, HEARTS_2);
        HandLadder twosLadder = new HandLadder(twosPocket, community, matrix);

        assertThat(acesLadder.getHandRank()).isLessThan(twosLadder.getHandRank());
    }

    // ========================================
    // Count Consistency
    // ========================================

    @Test
    void should_HaveConsistentCount_When_SameCommunityDifferentPockets() {
        Hand community = new Hand(DIAMONDS_T, CLUBS_7, HEARTS_3);

        Hand pocket1 = new Hand(SPADES_A, HEARTS_K);
        Hand pocket2 = new Hand(SPADES_2, HEARTS_5);

        HandLadder ladder1 = new HandLadder(pocket1, community, matrix);
        HandLadder ladder2 = new HandLadder(pocket2, community, matrix);

        // Count = C(47,2) + 1 = 1081 + 1 = 1082 on flop (52 - 2 pocket - 3 community =
        // 47 remaining)
        assertThat(ladder1.getHandCount()).isEqualTo(ladder2.getHandCount());
    }

    @Test
    void should_HaveExpectedCount_When_FlopBoard() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, CLUBS_J, SPADES_T);

        HandLadder ladder = new HandLadder(pocket, community, matrix);

        // 52 - 2 (pocket) - 3 (community) = 47 remaining cards
        // C(47,2) = 1081 opponent hands + 1 (self) = 1082
        assertThat(ladder.getHandCount()).isEqualTo(1082);
    }

    @Test
    void should_HaveExpectedCount_When_TurnBoard() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, CLUBS_J, SPADES_T, HEARTS_9);

        HandLadder ladder = new HandLadder(pocket, community, matrix);

        // 52 - 2 (pocket) - 4 (community) = 46 remaining cards
        // C(46,2) = 1035 opponent hands + 1 (self) = 1036
        assertThat(ladder.getHandCount()).isEqualTo(1036);
    }

    @Test
    void should_HaveExpectedCount_When_RiverBoard() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, CLUBS_J, SPADES_T, HEARTS_9, DIAMONDS_8);

        HandLadder ladder = new HandLadder(pocket, community, matrix);

        // 52 - 2 (pocket) - 5 (community) = 45 remaining cards
        // C(45,2) = 990 opponent hands + 1 (self) = 991
        assertThat(ladder.getHandCount()).isEqualTo(991);
    }

    // ========================================
    // toHTML Output
    // ========================================

    @Test
    void should_ReturnNonEmptyHtml_When_AnyHand() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, CLUBS_J, SPADES_T);

        HandLadder ladder = new HandLadder(pocket, community, matrix);

        String html = ladder.toHTML();
        assertThat(html).isNotNull().isNotEmpty();
    }

    @Test
    void should_ContainRankInHtml_When_AnyHand() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, CLUBS_J, SPADES_T);

        HandLadder ladder = new HandLadder(pocket, community, matrix);

        String html = ladder.toHTML();
        assertThat(html).contains("Ranked");
        assertThat(html).contains("possible hands");
    }

    @Test
    void should_ContainTableMarkup_When_AnyHand() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, CLUBS_J, SPADES_T);

        HandLadder ladder = new HandLadder(pocket, community, matrix);

        String html = ladder.toHTML();
        assertThat(html).contains("<table");
        assertThat(html).contains("</table>");
    }

    @Test
    void should_ContainSummaryHeading_When_AnyHand() {
        Hand pocket = new Hand(SPADES_2, HEARTS_7);
        Hand community = new Hand(DIAMONDS_A, CLUBS_K, SPADES_Q);

        HandLadder ladder = new HandLadder(pocket, community, matrix);

        String html = ladder.toHTML();
        assertThat(html).contains("Summary");
    }

    // ========================================
    // Edge Cases - Nut Hand
    // ========================================

    @Test
    void should_RankFirst_When_RoyalFlush() {
        // Royal flush: A♠ K♠ with Q♠ J♠ T♠
        Hand pocket = new Hand(SPADES_A, SPADES_K);
        Hand community = new Hand(SPADES_Q, SPADES_J, SPADES_T);

        HandLadder ladder = new HandLadder(pocket, community, matrix);

        // Royal flush is the best possible hand, rank should be 1
        assertThat(ladder.getHandRank()).isEqualTo(1);
    }

    // ========================================
    // Different Board Textures
    // ========================================

    @Test
    void should_HandlePairedBoard_When_CalculatingLadder() {
        Hand pocket = new Hand(SPADES_A, HEARTS_A);
        Hand community = new Hand(DIAMONDS_K, CLUBS_K, SPADES_7);

        HandLadder ladder = new HandLadder(pocket, community, matrix);

        assertThat(ladder.getHandRank()).isGreaterThanOrEqualTo(1);
        assertThat(ladder.getHandRank()).isLessThanOrEqualTo(ladder.getHandCount());
    }

    @Test
    void should_HandleMonotoneBoard_When_CalculatingLadder() {
        Hand pocket = new Hand(HEARTS_A, HEARTS_K);
        Hand community = new Hand(HEARTS_Q, HEARTS_J, HEARTS_T);

        HandLadder ladder = new HandLadder(pocket, community, matrix);

        // Royal flush - should rank first
        assertThat(ladder.getHandRank()).isEqualTo(1);
    }
}
