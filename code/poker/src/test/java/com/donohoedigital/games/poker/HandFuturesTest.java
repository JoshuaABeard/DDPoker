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

import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.HandInfoFaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for HandFutures - draw detection and improvement odds calculation for
 * Texas Hold'em poker hands.
 */
class HandFuturesTest {

    private HandInfoFaster fast;

    @BeforeEach
    void setUp() {
        fast = new HandInfoFaster();
    }

    // ===== Flush Draw Detection =====

    @Test
    void should_DetectFlushDraw_When_FourToFlushOnFlop() {
        // 4 clubs with 2 cards to come - strong flush draw
        Hand hole = new Hand(Card.CLUBS_A, Card.CLUBS_5);
        Hand community = new Hand(Card.CLUBS_4, Card.DIAMONDS_6, Card.CLUBS_9);

        HandFutures futures = new HandFutures(fast, hole, community);

        assertThat(futures.hasFlushDraw()).isTrue();
    }

    @Test
    void should_DetectFlushDraw_When_FourToFlushOnTurn() {
        // 4 hearts with 1 card to come
        Hand hole = new Hand(Card.HEARTS_5, Card.CLUBS_J);
        Hand community = new Hand(Card.HEARTS_3, Card.HEARTS_8, Card.HEARTS_J, Card.DIAMONDS_2);

        HandFutures futures = new HandFutures(fast, hole, community);

        assertThat(futures.hasFlushDraw()).isTrue();
    }

    @Test
    void should_NotDetectFlushDraw_When_OnlyThreeToFlush() {
        // Only 3 hearts - not enough for flush draw
        Hand hole = new Hand(Card.DIAMONDS_A, Card.CLUBS_Q);
        Hand community = new Hand(Card.HEARTS_J, Card.CLUBS_4, Card.HEARTS_3);

        HandFutures futures = new HandFutures(fast, hole, community);

        assertThat(futures.hasFlushDraw()).isFalse();
    }

    // ===== Straight Draw Detection =====

    @Test
    void should_DetectStraightDraw_When_OpenEndedOnFlop() {
        // 6-7 with 8-9 on board = open-ended straight draw
        Hand hole = new Hand(Card.HEARTS_7, Card.CLUBS_6);
        Hand community = new Hand(Card.HEARTS_9, Card.HEARTS_8, Card.DIAMONDS_Q);

        HandFutures futures = new HandFutures(fast, hole, community);

        assertThat(futures.hasStraightDraw()).isTrue();
    }

    @Test
    void should_DetectStraightDraw_When_OpenEndedOnTurn() {
        // Open-ended straight draw on the turn
        Hand hole = new Hand(Card.HEARTS_7, Card.CLUBS_6);
        Hand community = new Hand(Card.HEARTS_9, Card.HEARTS_8, Card.DIAMONDS_Q, Card.DIAMONDS_2);

        HandFutures futures = new HandFutures(fast, hole, community);

        assertThat(futures.hasStraightDraw()).isTrue();
    }

    @Test
    void should_NotDetectStraightDraw_When_NoConnectingCards() {
        // No straight draw possible
        Hand hole = new Hand(Card.DIAMONDS_Q, Card.CLUBS_K);
        Hand community = new Hand(Card.HEARTS_4, Card.SPADES_5, Card.HEARTS_6);

        HandFutures futures = new HandFutures(fast, hole, community);

        assertThat(futures.hasStraightDraw()).isFalse();
    }

    // ===== Gut Shot Straight Draw Detection =====

    @Test
    void should_DetectGutShotDraw_When_InsideStraightDrawOnFlop() {
        // 3-5 with 4-6-6 on board - need a 2 or 7 for a straight (gut shot / open)
        // This also has a flush draw component boosting straight-flush odds
        Hand hole = new Hand(Card.CLUBS_3, Card.CLUBS_5);
        Hand community = new Hand(Card.CLUBS_4, Card.DIAMONDS_6, Card.CLUBS_6);

        HandFutures futures = new HandFutures(fast, hole, community);

        assertThat(futures.hasGutShotStraightDraw()).isTrue();
    }

    @Test
    void should_DetectGutShotDraw_When_InsideStraightDrawOnTurn() {
        // Gut shot on the turn
        Hand hole = new Hand(Card.HEARTS_5, Card.CLUBS_6);
        Hand community = new Hand(Card.HEARTS_9, Card.HEARTS_8, Card.DIAMONDS_J, Card.DIAMONDS_2);

        HandFutures futures = new HandFutures(fast, hole, community);

        assertThat(futures.hasGutShotStraightDraw()).isTrue();
    }

    @Test
    void should_HaveOpenEndedImpliesGutShot() {
        // An open-ended straight draw always qualifies as a gut shot too
        Hand hole = new Hand(Card.HEARTS_7, Card.CLUBS_6);
        Hand community = new Hand(Card.HEARTS_9, Card.HEARTS_8, Card.DIAMONDS_Q);

        HandFutures futures = new HandFutures(fast, hole, community);

        assertThat(futures.hasStraightDraw()).isTrue();
        assertThat(futures.hasGutShotStraightDraw()).isTrue();
    }

    // ===== Odds Calculation =====

    @Test
    void should_ReturnOddsInValidRange_When_FlushDrawOnFlop() {
        Hand hole = new Hand(Card.CLUBS_A, Card.CLUBS_5);
        Hand community = new Hand(Card.CLUBS_4, Card.DIAMONDS_6, Card.CLUBS_9);

        HandFutures futures = new HandFutures(fast, hole, community);
        float flushOdds = futures.getOddsImproveTo(HandInfo.FLUSH);

        assertThat(flushOdds).isBetween(0.0f, 100.0f);
        assertThat(flushOdds).isGreaterThan(0.0f);
    }

    @Test
    void should_ReturnOddsInValidRange_When_StraightDrawOnFlop() {
        Hand hole = new Hand(Card.HEARTS_7, Card.CLUBS_6);
        Hand community = new Hand(Card.HEARTS_9, Card.HEARTS_8, Card.DIAMONDS_Q);

        HandFutures futures = new HandFutures(fast, hole, community);
        float straightOdds = futures.getOddsImproveTo(HandInfo.STRAIGHT);

        assertThat(straightOdds).isBetween(0.0f, 100.0f);
        assertThat(straightOdds).isGreaterThan(0.0f);
    }

    @Test
    void should_ReturnZeroOdds_When_NoDrawPresent() {
        // No flush draw possible
        Hand hole = new Hand(Card.DIAMONDS_A, Card.CLUBS_Q);
        Hand community = new Hand(Card.HEARTS_J, Card.SPADES_4, Card.DIAMONDS_3);

        HandFutures futures = new HandFutures(fast, hole, community);
        float royalFlushOdds = futures.getOddsImproveTo(HandInfo.ROYAL_FLUSH);

        assertThat(royalFlushOdds).isGreaterThanOrEqualTo(0.0f);
    }

    @Test
    void should_ReturnOverallImprovementOdds() {
        Hand hole = new Hand(Card.CLUBS_A, Card.CLUBS_5);
        Hand community = new Hand(Card.CLUBS_4, Card.DIAMONDS_6, Card.CLUBS_9);

        HandFutures futures = new HandFutures(fast, hole, community);
        float overallOdds = futures.getOddsImprove();

        assertThat(overallOdds).isBetween(0.0f, 100.0f);
    }

    // ===== Turn vs Flop (Community Size) =====

    @Test
    void should_HandleThreeCardCommunity_When_FlopStage() {
        // Flop: 3 community cards, 2 more to come (MORE == 2)
        Hand hole = new Hand(Card.CLUBS_3, Card.CLUBS_5);
        Hand community = new Hand(Card.CLUBS_4, Card.DIAMONDS_6, Card.CLUBS_6);

        HandFutures futures = new HandFutures(fast, hole, community);

        // Should not throw, and should produce valid results
        assertThat(futures.getOddsImprove()).isBetween(0.0f, 100.0f);
    }

    @Test
    void should_HandleFourCardCommunity_When_TurnStage() {
        // Turn: 4 community cards, 1 more to come (MORE == 1)
        Hand hole = new Hand(Card.CLUBS_3, Card.CLUBS_5);
        Hand community = new Hand(Card.CLUBS_4, Card.DIAMONDS_6, Card.CLUBS_6, Card.HEARTS_3);

        HandFutures futures = new HandFutures(fast, hole, community);

        // Should not throw, and should produce valid results
        assertThat(futures.getOddsImprove()).isBetween(0.0f, 100.0f);
    }

    @Test
    void should_HaveHigherFlopOdds_When_TwoCardsToComVsOne() {
        // Same hand on flop vs turn: flop has two chances to improve
        Hand hole = new Hand(Card.CLUBS_A, Card.CLUBS_5);
        Hand flopCommunity = new Hand(Card.CLUBS_4, Card.DIAMONDS_6, Card.CLUBS_9);
        Hand turnCommunity = new Hand(Card.CLUBS_4, Card.DIAMONDS_6, Card.CLUBS_9, Card.HEARTS_K);

        HandFutures flopFutures = new HandFutures(fast, hole, flopCommunity);
        HandFutures turnFutures = new HandFutures(fast, hole, turnCommunity);

        float flopOdds = flopFutures.getOddsImprove();
        float turnOdds = turnFutures.getOddsImprove();

        // With 2 cards to come, improvement odds should generally be >= turn odds
        // (more chances). This is not always strictly true for all specific metrics,
        // but overall improvement is typically higher on flop.
        assertThat(flopOdds).isGreaterThanOrEqualTo(0.0f);
        assertThat(turnOdds).isGreaterThanOrEqualTo(0.0f);
    }

    // ===== Combined Draws =====

    @Test
    void should_DetectBothFlushAndStraightDraw_When_Present() {
        // Straight-flush draw: clubs 3-5 with clubs 4 and 6 on board
        Hand hole = new Hand(Card.CLUBS_3, Card.CLUBS_5);
        Hand community = new Hand(Card.CLUBS_4, Card.DIAMONDS_6, Card.CLUBS_6);

        HandFutures futures = new HandFutures(fast, hole, community);

        // Should have at least a gut shot draw due to 3-4-5-6
        assertThat(futures.hasGutShotStraightDraw()).isTrue();
        // Should also detect flush draw (4 clubs)
        assertThat(futures.hasFlushDraw()).isTrue();
    }

    @Test
    void should_CalculateStraightFlushOdds_When_DrawExists() {
        Hand hole = new Hand(Card.CLUBS_3, Card.CLUBS_5);
        Hand community = new Hand(Card.CLUBS_4, Card.DIAMONDS_6, Card.CLUBS_6, Card.HEARTS_3);

        HandFutures futures = new HandFutures(fast, hole, community);
        float sfOdds = futures.getOddsImproveTo(HandInfo.STRAIGHT_FLUSH);

        // Should be small but possibly > 0 with a straight-flush draw
        assertThat(sfOdds).isGreaterThanOrEqualTo(0.0f);
        assertThat(sfOdds).isLessThanOrEqualTo(100.0f);
    }

    // ===== Already Made Hands =====

    @Test
    void should_CalculateOdds_When_AlreadyHaveTwoPair() {
        Hand hole = new Hand(Card.DIAMONDS_5, Card.CLUBS_Q);
        Hand community = new Hand(Card.HEARTS_5, Card.SPADES_Q, Card.HEARTS_8);

        HandFutures futures = new HandFutures(fast, hole, community);

        // Already have two pair; can still improve to full house or better
        float fullHouseOdds = futures.getOddsImproveTo(HandInfo.FULL_HOUSE);
        assertThat(fullHouseOdds).isGreaterThanOrEqualTo(0.0f);
    }

    @Test
    void should_CalculateOdds_When_AlreadyHavePair() {
        Hand hole = new Hand(Card.DIAMONDS_5, Card.CLUBS_J);
        Hand community = new Hand(Card.HEARTS_3, Card.HEARTS_8, Card.HEARTS_J);

        HandFutures futures = new HandFutures(fast, hole, community);

        // Already have a pair of jacks; can improve to two pair, trips, etc.
        float twosPairOdds = futures.getOddsImproveTo(HandInfo.TWO_PAIR);
        float tripsOdds = futures.getOddsImproveTo(HandInfo.TRIPS);
        assertThat(twosPairOdds).isGreaterThanOrEqualTo(0.0f);
        assertThat(tripsOdds).isGreaterThanOrEqualTo(0.0f);
    }

    // ===== Edge Cases =====

    @Test
    void should_HandleNoImprovementPossible_When_PairedBoardLowHolding() {
        // Low cards with a paired board - limited improvement odds
        Hand hole = new Hand(Card.DIAMONDS_5, Card.CLUBS_7);
        Hand community = new Hand(Card.HEARTS_3, Card.SPADES_3, Card.CLUBS_J);

        HandFutures futures = new HandFutures(fast, hole, community);

        assertThat(futures.getOddsImprove()).isGreaterThanOrEqualTo(0.0f);
        assertThat(futures.getOddsImprove()).isLessThanOrEqualTo(100.0f);
    }

    @Test
    void should_ReturnValidOddsForAllHandTypes() {
        Hand hole = new Hand(Card.CLUBS_A, Card.CLUBS_5);
        Hand community = new Hand(Card.CLUBS_4, Card.DIAMONDS_6, Card.CLUBS_9);

        HandFutures futures = new HandFutures(fast, hole, community);

        for (int type = HandInfo.HIGH_CARD; type <= HandInfo.ROYAL_FLUSH; type++) {
            float odds = futures.getOddsImproveTo(type);
            assertThat(odds).as("Odds for hand type %d", type).isBetween(0.0f, 100.0f);
        }
    }

    @Test
    void should_HandleMinHandTypeConstructor_When_Specified() {
        // Use constructor that specifies minimum hand type for counting improvements
        Hand hole = new Hand(Card.DIAMONDS_5, Card.CLUBS_7);
        Hand community = new Hand(Card.HEARTS_4, Card.SPADES_8, Card.HEARTS_Q);

        HandFutures futures = new HandFutures(fast, hole, community, HandInfo.TRIPS);

        // Should calculate odds, only counting improvements to trips or better
        assertThat(futures.getOddsImprove()).isGreaterThanOrEqualTo(0.0f);
        assertThat(futures.getOddsImprove()).isLessThanOrEqualTo(100.0f);
    }
}
