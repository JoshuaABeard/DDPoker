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
        Hand board = new Hand(SPADES_Q, SPADES_J, SPADES_T, HEARTS_2, CLUBS_3);
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
        Hand hole = new Hand(SPADES_T, HEARTS_9);
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
        int royalFlush = evaluator.getScore(new Hand(SPADES_A, SPADES_K),
                new Hand(SPADES_Q, SPADES_J, SPADES_T, HEARTS_2, CLUBS_3));

        HandInfoFaster eval2 = new HandInfoFaster();
        int straightFlush = eval2.getScore(new Hand(HEARTS_9, HEARTS_8),
                new Hand(HEARTS_7, HEARTS_6, HEARTS_5, SPADES_A, CLUBS_K));

        assertThat(royalFlush).isGreaterThan(straightFlush);
    }

    @Test
    void should_RankByKicker_When_SameHandType() {
        // Pair of aces with king kicker beats pair of aces with queen kicker
        int aceKing = evaluator.getScore(new Hand(SPADES_A, HEARTS_K),
                new Hand(DIAMONDS_A, SPADES_9, HEARTS_7, CLUBS_2, DIAMONDS_3));

        HandInfoFaster eval2 = new HandInfoFaster();
        int aceQueen = eval2.getScore(new Hand(SPADES_A, HEARTS_Q),
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
        Hand board = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_J, SPADES_T);
        int score = evaluator.getScore(null, board);
        assertThat(score).isGreaterThan(0);
    }

    @Test
    void should_HandleFiveCardHand_When_NoBoardCards() {
        // 5-card stud style
        Hand hole = new Hand(SPADES_A, HEARTS_K);
        Hand board = new Hand(DIAMONDS_Q, CLUBS_J, SPADES_T);
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
                Arguments.of(new Hand(SPADES_A, SPADES_K), new Hand(SPADES_Q, SPADES_J, SPADES_T, HEARTS_2, CLUBS_3),
                        new Hand(HEARTS_9, HEARTS_8), new Hand(HEARTS_7, HEARTS_6, HEARTS_5, SPADES_2, CLUBS_4)),
                // Straight flush vs quads
                Arguments.of(new Hand(HEARTS_9, HEARTS_8), new Hand(HEARTS_7, HEARTS_6, HEARTS_5, SPADES_2, CLUBS_4),
                        new Hand(SPADES_A, HEARTS_A), new Hand(DIAMONDS_A, CLUBS_A, SPADES_K, HEARTS_2, CLUBS_3)),
                // Quads vs full house
                Arguments.of(new Hand(SPADES_A, HEARTS_A), new Hand(DIAMONDS_A, CLUBS_A, SPADES_K, HEARTS_2, CLUBS_3),
                        new Hand(SPADES_K, HEARTS_K), new Hand(DIAMONDS_K, SPADES_A, HEARTS_A, CLUBS_2, DIAMONDS_3)),
                // Full house vs flush
                Arguments.of(new Hand(SPADES_K, HEARTS_K),
                        new Hand(DIAMONDS_K, SPADES_A, HEARTS_A, CLUBS_2, DIAMONDS_3), new Hand(SPADES_A, SPADES_J),
                        new Hand(SPADES_9, SPADES_6, SPADES_3, HEARTS_K, CLUBS_Q)),
                // Flush vs straight
                Arguments.of(new Hand(SPADES_A, SPADES_J), new Hand(SPADES_9, SPADES_6, SPADES_3, HEARTS_K, CLUBS_Q),
                        new Hand(SPADES_T, HEARTS_9), new Hand(DIAMONDS_8, CLUBS_7, SPADES_6, HEARTS_2, CLUBS_A)),
                // Straight vs trips
                Arguments.of(new Hand(SPADES_T, HEARTS_9), new Hand(DIAMONDS_8, CLUBS_7, HEARTS_6, SPADES_2, CLUBS_A),
                        new Hand(SPADES_A, HEARTS_A), new Hand(DIAMONDS_A, SPADES_K, HEARTS_Q, CLUBS_2, CLUBS_3)),
                // Trips vs two pair
                Arguments.of(new Hand(SPADES_A, HEARTS_A), new Hand(DIAMONDS_A, SPADES_K, HEARTS_Q, CLUBS_2, CLUBS_3),
                        new Hand(SPADES_A, HEARTS_K), new Hand(DIAMONDS_A, SPADES_K, HEARTS_Q, CLUBS_2, DIAMONDS_3)),
                // Two pair vs pair
                Arguments.of(new Hand(SPADES_A, HEARTS_K),
                        new Hand(DIAMONDS_A, SPADES_K, HEARTS_Q, CLUBS_2, DIAMONDS_3), new Hand(SPADES_A, HEARTS_K),
                        new Hand(DIAMONDS_A, SPADES_Q, HEARTS_J, CLUBS_2, CLUBS_3)),
                // Pair vs high card
                Arguments.of(new Hand(SPADES_A, HEARTS_K), new Hand(DIAMONDS_A, SPADES_Q, HEARTS_J, CLUBS_2, CLUBS_3),
                        new Hand(SPADES_A, HEARTS_K), new Hand(DIAMONDS_Q, SPADES_J, HEARTS_9, CLUBS_2, CLUBS_3)));
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
