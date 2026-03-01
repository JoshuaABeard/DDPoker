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
package com.donohoedigital.games.poker.gameserver;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.engine.Card;

/**
 * Tests for AdvisorService covering hand evaluation, equity, pot odds,
 * recommendation, and starting hand categorization.
 */
class AdvisorServiceTest {

    private AdvisorService service;
    private Random seededRandom;

    @BeforeEach
    void setUp() {
        service = new AdvisorService();
        seededRandom = new Random(42);
    }

    // --- Pre-flop with premium hand ---

    @Test
    void preFlopPocketAces_hasPremiumCategory() {
        Card[] hole = {Card.SPADES_A, Card.HEARTS_A};
        Card[] community = {};

        AdvisorResult result = service.compute(hole, community, 100, 50, 1, 1000, seededRandom);

        assertEquals("premium", result.startingHandCategory());
        assertEquals("AA", result.startingHandNotation());
    }

    @Test
    void preFlopPocketAces_hasHighEquity() {
        Card[] hole = {Card.SPADES_A, Card.HEARTS_A};
        Card[] community = {};

        AdvisorResult result = service.compute(hole, community, 100, 50, 1, 5000, seededRandom);

        // AA vs 1 opponent should be around 85% equity
        assertTrue(result.equity() > 75.0, "AA equity should be high, got: " + result.equity());
    }

    @Test
    void preFlopPocketAces_handDescriptionIsNull() {
        Card[] hole = {Card.SPADES_A, Card.HEARTS_A};
        Card[] community = {};

        AdvisorResult result = service.compute(hole, community, 100, 50, 1, 100, seededRandom);

        // Only 2 cards, so no hand description
        assertNull(result.handDescription());
    }

    // --- Post-flop with made hand ---

    @Test
    void postFlopPairOfAces_correctHandRankAndDescription() {
        Card[] hole = {Card.SPADES_A, Card.HEARTS_K};
        Card[] community = {Card.DIAMONDS_A, Card.CLUBS_8, Card.HEARTS_5, Card.SPADES_3, Card.DIAMONDS_2};

        AdvisorResult result = service.compute(hole, community, 200, 100, 1, 1000, seededRandom);

        // HandRank 1 = ONE_PAIR (0-based: HIGH_CARD=0, ONE_PAIR=1)
        assertEquals(1, result.handRank());
        assertEquals("One Pair, Aces", result.handDescription());
    }

    @Test
    void postFlopTwoPair_correctDescription() {
        Card[] hole = {Card.SPADES_A, Card.HEARTS_K};
        Card[] community = {Card.DIAMONDS_A, Card.CLUBS_K, Card.HEARTS_5, Card.SPADES_3, Card.DIAMONDS_2};

        AdvisorResult result = service.compute(hole, community, 200, 100, 1, 100, seededRandom);

        assertEquals(2, result.handRank()); // TWO_PAIR
        assertEquals("Two Pair, Aces and Kings", result.handDescription());
    }

    @Test
    void postFlopTrips_correctDescription() {
        Card[] hole = {Card.SPADES_A, Card.HEARTS_A};
        Card[] community = {Card.DIAMONDS_A, Card.CLUBS_K, Card.HEARTS_5, Card.SPADES_9, Card.DIAMONDS_2};

        AdvisorResult result = service.compute(hole, community, 200, 100, 1, 100, seededRandom);

        assertEquals(3, result.handRank()); // TRIPS
        assertEquals("Three of a Kind, Aces", result.handDescription());
    }

    @Test
    void postFlopStraight_correctDescription() {
        Card[] hole = {Card.SPADES_A, Card.HEARTS_K};
        Card[] community = {Card.DIAMONDS_Q, Card.CLUBS_J, Card.HEARTS_T, Card.SPADES_3, Card.DIAMONDS_2};

        AdvisorResult result = service.compute(hole, community, 200, 100, 1, 100, seededRandom);

        assertEquals(4, result.handRank()); // STRAIGHT
        assertEquals("Straight, Ace High", result.handDescription());
    }

    @Test
    void postFlopFlush_correctDescription() {
        Card[] hole = {Card.SPADES_A, Card.SPADES_K};
        Card[] community = {Card.SPADES_9, Card.SPADES_7, Card.SPADES_2, Card.HEARTS_K, Card.CLUBS_Q};

        AdvisorResult result = service.compute(hole, community, 200, 100, 1, 100, seededRandom);

        assertEquals(5, result.handRank()); // FLUSH
        assertEquals("Flush, Ace High", result.handDescription());
    }

    @Test
    void postFlopFullHouse_correctDescription() {
        Card[] hole = {Card.SPADES_A, Card.HEARTS_A};
        Card[] community = {Card.DIAMONDS_A, Card.SPADES_K, Card.DIAMONDS_K, Card.HEARTS_Q, Card.CLUBS_J};

        AdvisorResult result = service.compute(hole, community, 200, 100, 1, 100, seededRandom);

        assertEquals(6, result.handRank()); // FULL_HOUSE
        assertEquals("Full House, Aces over Kings", result.handDescription());
    }

    @Test
    void postFlopQuads_correctDescription() {
        Card[] hole = {Card.SPADES_A, Card.HEARTS_A};
        Card[] community = {Card.DIAMONDS_A, Card.CLUBS_A, Card.SPADES_K, Card.HEARTS_Q, Card.CLUBS_J};

        AdvisorResult result = service.compute(hole, community, 200, 100, 1, 100, seededRandom);

        assertEquals(7, result.handRank()); // QUADS
        assertEquals("Four of a Kind, Aces", result.handDescription());
    }

    @Test
    void postFlopStraightFlush_correctDescription() {
        Card[] hole = {Card.HEARTS_9, Card.HEARTS_8};
        Card[] community = {Card.HEARTS_7, Card.HEARTS_6, Card.HEARTS_5, Card.SPADES_A, Card.CLUBS_K};

        AdvisorResult result = service.compute(hole, community, 200, 100, 1, 100, seededRandom);

        assertEquals(8, result.handRank()); // STRAIGHT_FLUSH
        assertEquals("Straight Flush, Nine High", result.handDescription());
    }

    @Test
    void postFlopRoyalFlush_correctDescription() {
        Card[] hole = {Card.SPADES_A, Card.SPADES_K};
        Card[] community = {Card.SPADES_Q, Card.SPADES_J, Card.SPADES_T, Card.HEARTS_2, Card.CLUBS_3};

        AdvisorResult result = service.compute(hole, community, 200, 100, 1, 100, seededRandom);

        assertEquals(9, result.handRank()); // ROYAL_FLUSH
        assertEquals("Royal Flush", result.handDescription());
    }

    @Test
    void postFlopHighCard_correctDescription() {
        Card[] hole = {Card.SPADES_A, Card.HEARTS_K};
        Card[] community = {Card.DIAMONDS_Q, Card.CLUBS_J, Card.SPADES_9, Card.HEARTS_7, Card.CLUBS_2};

        AdvisorResult result = service.compute(hole, community, 200, 100, 1, 100, seededRandom);

        assertEquals(0, result.handRank()); // HIGH_CARD
        assertEquals("High Card, Ace", result.handDescription());
    }

    // --- Post-flop: starting hand fields should be null ---

    @Test
    void postFlop_startingHandFieldsAreNull() {
        Card[] hole = {Card.SPADES_A, Card.HEARTS_K};
        Card[] community = {Card.DIAMONDS_Q, Card.CLUBS_J, Card.SPADES_9};

        AdvisorResult result = service.compute(hole, community, 200, 100, 1, 100, seededRandom);

        assertNull(result.startingHandCategory());
        assertNull(result.startingHandNotation());
    }

    // --- Pot odds calculation ---

    @Test
    void potOdds_correctCalculation() {
        Card[] hole = {Card.SPADES_A, Card.HEARTS_K};
        Card[] community = {};

        // callAmount=50, potSize=150 => 50/(150+50)*100 = 25.0
        AdvisorResult result = service.compute(hole, community, 150, 50, 1, 100, seededRandom);

        assertEquals(25.0, result.potOdds(), 0.01);
    }

    @Test
    void potOdds_zeroCallAmount() {
        Card[] hole = {Card.SPADES_A, Card.HEARTS_K};
        Card[] community = {};

        AdvisorResult result = service.compute(hole, community, 200, 0, 1, 100, seededRandom);

        assertEquals(0.0, result.potOdds(), 0.01);
    }

    @Test
    void potOdds_equalPotAndCall() {
        Card[] hole = {Card.SPADES_A, Card.HEARTS_K};
        Card[] community = {};

        // callAmount=100, potSize=100 => 100/(100+100)*100 = 50.0
        AdvisorResult result = service.compute(hole, community, 100, 100, 1, 100, seededRandom);

        assertEquals(50.0, result.potOdds(), 0.01);
    }

    // --- Recommendation logic ---

    @Test
    void recommendation_check_whenCallAmountIsZero() {
        Card[] hole = {Card.SPADES_A, Card.HEARTS_K};
        Card[] community = {};

        AdvisorResult result = service.compute(hole, community, 200, 0, 1, 100, seededRandom);

        assertEquals("Check", result.recommendation());
    }

    @Test
    void recommendation_raiseOrCall_whenEdgeOverTen() {
        // AA pre-flop vs 1 opponent should have ~85% equity
        // With potOdds ~33%, edge ~52% => "Raise or Call"
        Card[] hole = {Card.SPADES_A, Card.HEARTS_A};
        Card[] community = {};

        AdvisorResult result = service.compute(hole, community, 200, 100, 1, 5000, seededRandom);

        assertEquals("Raise or Call", result.recommendation());
    }

    @Test
    void recommendation_considerFolding_whenEquityBelowPotOdds() {
        // 72o pre-flop vs 1 opponent, with high pot odds
        // callAmount=900, potSize=100 => potOdds=90%
        // 72o equity ~35%, so edge = 35-90 = -55 => "Consider folding"
        Card[] hole = {Card.HEARTS_7, Card.CLUBS_2};
        Card[] community = {};

        AdvisorResult result = service.compute(hole, community, 100, 900, 1, 5000, seededRandom);

        assertEquals("Consider folding", result.recommendation());
    }

    // --- Starting hand categories ---

    @Test
    void startingHand_premiumPairs() {
        Card[] hole = {Card.SPADES_K, Card.HEARTS_K};
        Card[] community = {};

        AdvisorResult result = service.compute(hole, community, 100, 50, 1, 100, seededRandom);

        assertEquals("premium", result.startingHandCategory());
        assertEquals("KK", result.startingHandNotation());
    }

    @Test
    void startingHand_suitedAceKing() {
        Card[] hole = {Card.SPADES_A, Card.SPADES_K};
        Card[] community = {};

        AdvisorResult result = service.compute(hole, community, 100, 50, 1, 100, seededRandom);

        assertEquals("premium", result.startingHandCategory());
        assertEquals("AKs", result.startingHandNotation());
    }

    @Test
    void startingHand_offsuitAceKing() {
        Card[] hole = {Card.SPADES_A, Card.HEARTS_K};
        Card[] community = {};

        AdvisorResult result = service.compute(hole, community, 100, 50, 1, 100, seededRandom);

        assertEquals("strong", result.startingHandCategory());
        assertEquals("AKo", result.startingHandNotation());
    }

    @Test
    void startingHand_foldHand() {
        Card[] hole = {Card.HEARTS_7, Card.CLUBS_2};
        Card[] community = {};

        AdvisorResult result = service.compute(hole, community, 100, 50, 1, 100, seededRandom);

        assertEquals("fold", result.startingHandCategory());
        assertEquals("72o", result.startingHandNotation());
    }

    @Test
    void startingHand_marginalHand() {
        Card[] hole = {Card.SPADES_5, Card.HEARTS_5};
        Card[] community = {};

        AdvisorResult result = service.compute(hole, community, 100, 50, 1, 100, seededRandom);

        assertEquals("marginal", result.startingHandCategory());
        assertEquals("55", result.startingHandNotation());
    }

    @Test
    void startingHand_playableHand_suitedAceTen() {
        Card[] hole = {Card.HEARTS_A, Card.HEARTS_T};
        Card[] community = {};

        AdvisorResult result = service.compute(hole, community, 100, 50, 1, 100, seededRandom);

        assertEquals("playable", result.startingHandCategory());
        assertEquals("ATs", result.startingHandNotation());
    }

    @Test
    void startingHand_strongHand_pocketJacks() {
        Card[] hole = {Card.SPADES_J, Card.HEARTS_J};
        Card[] community = {};

        AdvisorResult result = service.compute(hole, community, 100, 50, 1, 100, seededRandom);

        assertEquals("strong", result.startingHandCategory());
        assertEquals("JJ", result.startingHandNotation());
    }

    // --- Equity behavior ---

    @Test
    void equity_rangeIsBounded() {
        Card[] hole = {Card.SPADES_A, Card.HEARTS_K};
        Card[] community = {};

        AdvisorResult result = service.compute(hole, community, 100, 50, 1, 1000, seededRandom);

        assertTrue(result.equity() >= 0.0 && result.equity() <= 100.0,
                "Equity should be between 0 and 100, got: " + result.equity());
    }

    @Test
    void equity_moreOpponentsReducesEquity() {
        Card[] hole = {Card.SPADES_A, Card.HEARTS_K};
        Card[] community = {};

        Random r1 = new Random(42);
        Random r2 = new Random(42);

        AdvisorResult oneOpp = service.compute(hole, community, 100, 50, 1, 5000, r1);
        AdvisorResult fiveOpp = service.compute(hole, community, 100, 50, 5, 5000, r2);

        assertTrue(oneOpp.equity() > fiveOpp.equity(), "Equity with 1 opponent (" + oneOpp.equity()
                + ") should be higher than with 5 (" + fiveOpp.equity() + ")");
    }

    @Test
    void equity_postFlopWithNuts_isHigh() {
        // Royal flush on board effectively - player has royal flush
        Card[] hole = {Card.SPADES_A, Card.SPADES_K};
        Card[] community = {Card.SPADES_Q, Card.SPADES_J, Card.SPADES_T};

        AdvisorResult result = service.compute(hole, community, 200, 100, 1, 1000, seededRandom);

        // Royal flush should have ~100% equity
        assertTrue(result.equity() > 95.0, "Royal flush equity should be near 100, got: " + result.equity());
    }

    // --- Hand description on flop (5 cards) ---

    @Test
    void flopExactlyFiveCards_handDescriptionIsPopulated() {
        Card[] hole = {Card.SPADES_A, Card.HEARTS_K};
        Card[] community = {Card.DIAMONDS_A, Card.CLUBS_8, Card.HEARTS_5};

        AdvisorResult result = service.compute(hole, community, 200, 100, 1, 100, seededRandom);

        assertNotNull(result.handDescription());
        assertEquals("One Pair, Aces", result.handDescription());
    }

    // --- Notation ordering ---

    @Test
    void startingHand_lowerCardFirst_stillCorrectNotation() {
        // 2h 7c should still produce "72o"
        Card[] hole = {Card.CLUBS_2, Card.HEARTS_7};
        Card[] community = {};

        AdvisorResult result = service.compute(hole, community, 100, 50, 1, 100, seededRandom);

        assertEquals("72o", result.startingHandNotation());
        assertEquals("fold", result.startingHandCategory());
    }

    // --- Improvement odds ---

    @Test
    void improvementOdds_flopWithFlushDraw_includesFlushOdds() {
        // Ah 2h on a board of Kh 7h 3c = flush draw (9 outs)
        Card[] hole = {Card.getCard("Ah"), Card.getCard("2h")};
        Card[] community = {Card.getCard("Kh"), Card.getCard("7h"), Card.getCard("3c")};
        AdvisorResult result = service.compute(hole, community, 0, 0, 1, 500, seededRandom);
        assertNotNull(result.improvementOdds());
        assertTrue(result.improvementOdds().containsKey("FLUSH"));
        assertTrue(result.improvementOdds().get("FLUSH") > 15.0,
                "Flush odds should be > 15%, got: " + result.improvementOdds().get("FLUSH"));
    }

    @Test
    void improvementOdds_turnWithFlushDraw_includesFlushOdds() {
        // Ah 2h on Kh 7h 3c 9d = flush draw on turn (9 outs from 46 cards)
        Card[] hole = {Card.getCard("Ah"), Card.getCard("2h")};
        Card[] community = {Card.getCard("Kh"), Card.getCard("7h"), Card.getCard("3c"), Card.getCard("9d")};
        AdvisorResult result = service.compute(hole, community, 0, 0, 1, 500, seededRandom);
        assertNotNull(result.improvementOdds());
        assertTrue(result.improvementOdds().containsKey("FLUSH"));
        assertTrue(result.improvementOdds().get("FLUSH") > 15.0,
                "Flush odds should be > 15%, got: " + result.improvementOdds().get("FLUSH"));
    }

    @Test
    void improvementOdds_river_returnsNull() {
        // 5 community cards = river, no improvement possible
        Card[] hole = {Card.getCard("Ah"), Card.getCard("Kh")};
        Card[] community = {Card.getCard("2c"), Card.getCard("7d"), Card.getCard("Js"), Card.getCard("3h"),
                Card.getCard("9c")};
        AdvisorResult result = service.compute(hole, community, 0, 0, 1, 500, seededRandom);
        assertNull(result.improvementOdds());
    }

    @Test
    void improvementOdds_preflop_returnsNull() {
        Card[] hole = {Card.getCard("Ah"), Card.getCard("Kh")};
        Card[] community = {};
        AdvisorResult result = service.compute(hole, community, 0, 0, 1, 500, seededRandom);
        assertNull(result.improvementOdds());
    }

    @Test
    void improvementOdds_flopWithStraightDraw_includesStraightOdds() {
        // 9h 8c on 7d 6s 2h = open-ended straight draw (8 outs)
        Card[] hole = {Card.getCard("9h"), Card.getCard("8c")};
        Card[] community = {Card.getCard("7d"), Card.getCard("6s"), Card.getCard("2h")};
        AdvisorResult result = service.compute(hole, community, 0, 0, 1, 500, seededRandom);
        assertNotNull(result.improvementOdds());
        assertTrue(result.improvementOdds().containsKey("STRAIGHT"), "Should include STRAIGHT improvement odds");
    }
}
