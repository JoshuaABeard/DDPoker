/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026  Doug Donohoe, DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file in the root directory
 * of this project.
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

import java.util.List;

import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.HandScoreConstants;

/**
 * Tests for ServerHandEvaluator covering all hand types.
 */
class ServerHandEvaluatorTest implements HandScoreConstants {

    @Test
    void testRoyalFlush() {
        ServerHandEvaluator eval = new ServerHandEvaluator();

        // Royal flush in spades
        List<Card> hole = List.of(Card.SPADES_A, Card.SPADES_K);
        List<Card> community = List.of(Card.SPADES_Q, Card.SPADES_J, Card.SPADES_T, Card.HEARTS_2, Card.CLUBS_3);

        int score = eval.getScore(hole, community);
        int handType = score / SCORE_BASE;

        assertEquals(ROYAL_FLUSH, handType);
    }

    @Test
    void testStraightFlush() {
        ServerHandEvaluator eval = new ServerHandEvaluator();

        // Straight flush 9-high in hearts
        List<Card> hole = List.of(Card.HEARTS_9, Card.HEARTS_8);
        List<Card> community = List.of(Card.HEARTS_7, Card.HEARTS_6, Card.HEARTS_5, Card.SPADES_A, Card.CLUBS_K);

        int score = eval.getScore(hole, community);
        int handType = score / SCORE_BASE;

        assertEquals(STRAIGHT_FLUSH, handType);
    }

    @Test
    void testStraightFlush_Wheel() {
        ServerHandEvaluator eval = new ServerHandEvaluator();

        // Wheel straight flush (A-2-3-4-5) in clubs
        List<Card> hole = List.of(Card.CLUBS_A, Card.CLUBS_2);
        List<Card> community = List.of(Card.CLUBS_3, Card.CLUBS_4, Card.CLUBS_5, Card.SPADES_K, Card.HEARTS_Q);

        int score = eval.getScore(hole, community);
        int handType = score / SCORE_BASE;

        assertEquals(STRAIGHT_FLUSH, handType);
    }

    @Test
    void testQuads() {
        ServerHandEvaluator eval = new ServerHandEvaluator();

        // Four aces
        List<Card> hole = List.of(Card.SPADES_A, Card.HEARTS_A);
        List<Card> community = List.of(Card.DIAMONDS_A, Card.CLUBS_A, Card.SPADES_K, Card.HEARTS_Q, Card.DIAMONDS_J);

        int score = eval.getScore(hole, community);
        int handType = score / SCORE_BASE;

        assertEquals(QUADS, handType);
    }

    @Test
    void testFullHouse() {
        ServerHandEvaluator eval = new ServerHandEvaluator();

        // Aces full of kings
        List<Card> hole = List.of(Card.SPADES_A, Card.HEARTS_A);
        List<Card> community = List.of(Card.DIAMONDS_A, Card.SPADES_K, Card.DIAMONDS_K, Card.HEARTS_Q, Card.CLUBS_J);

        int score = eval.getScore(hole, community);
        int handType = score / SCORE_BASE;

        assertEquals(FULL_HOUSE, handType);
    }

    @Test
    void testFlush() {
        ServerHandEvaluator eval = new ServerHandEvaluator();

        // Ace-high flush in spades
        List<Card> hole = List.of(Card.SPADES_A, Card.SPADES_K);
        List<Card> community = List.of(Card.SPADES_9, Card.SPADES_7, Card.SPADES_2, Card.HEARTS_K, Card.CLUBS_Q);

        int score = eval.getScore(hole, community);
        int handType = score / SCORE_BASE;

        assertEquals(FLUSH, handType);
    }

    @Test
    void testStraight() {
        ServerHandEvaluator eval = new ServerHandEvaluator();

        // Broadway straight (A-K-Q-J-T)
        List<Card> hole = List.of(Card.SPADES_A, Card.HEARTS_K);
        List<Card> community = List.of(Card.DIAMONDS_Q, Card.CLUBS_J, Card.SPADES_T, Card.HEARTS_2, Card.CLUBS_3);

        int score = eval.getScore(hole, community);
        int handType = score / SCORE_BASE;

        assertEquals(STRAIGHT, handType);
    }

    @Test
    void testStraight_Wheel() {
        ServerHandEvaluator eval = new ServerHandEvaluator();

        // Wheel straight (A-2-3-4-5)
        List<Card> hole = List.of(Card.SPADES_A, Card.HEARTS_2);
        List<Card> community = List.of(Card.DIAMONDS_3, Card.CLUBS_4, Card.SPADES_5, Card.HEARTS_K, Card.CLUBS_Q);

        int score = eval.getScore(hole, community);
        int handType = score / SCORE_BASE;

        assertEquals(STRAIGHT, handType);
    }

    @Test
    void testTrips() {
        ServerHandEvaluator eval = new ServerHandEvaluator();

        // Three aces
        List<Card> hole = List.of(Card.SPADES_A, Card.HEARTS_A);
        List<Card> community = List.of(Card.DIAMONDS_A, Card.SPADES_K, Card.HEARTS_Q, Card.CLUBS_9, Card.DIAMONDS_7);

        int score = eval.getScore(hole, community);
        int handType = score / SCORE_BASE;

        assertEquals(TRIPS, handType);
    }

    @Test
    void testTwoPair() {
        ServerHandEvaluator eval = new ServerHandEvaluator();

        // Aces and kings
        List<Card> hole = List.of(Card.SPADES_A, Card.HEARTS_A);
        List<Card> community = List.of(Card.SPADES_K, Card.DIAMONDS_K, Card.HEARTS_Q, Card.CLUBS_8, Card.DIAMONDS_5);

        int score = eval.getScore(hole, community);
        int handType = score / SCORE_BASE;

        assertEquals(TWO_PAIR, handType);
    }

    @Test
    void testPair() {
        ServerHandEvaluator eval = new ServerHandEvaluator();

        // Pair of aces
        List<Card> hole = List.of(Card.SPADES_A, Card.HEARTS_A);
        List<Card> community = List.of(Card.SPADES_K, Card.HEARTS_Q, Card.CLUBS_9, Card.DIAMONDS_7, Card.SPADES_3);

        int score = eval.getScore(hole, community);
        int handType = score / SCORE_BASE;

        assertEquals(PAIR, handType);
    }

    @Test
    void testHighCard() {
        ServerHandEvaluator eval = new ServerHandEvaluator();

        // Ace high
        List<Card> hole = List.of(Card.SPADES_A, Card.HEARTS_K);
        List<Card> community = List.of(Card.DIAMONDS_Q, Card.CLUBS_J, Card.SPADES_9, Card.HEARTS_7, Card.CLUBS_2);

        int score = eval.getScore(hole, community);
        int handType = score / SCORE_BASE;

        assertEquals(HIGH_CARD, handType);
    }

    @Test
    void testHandRankings() {
        ServerHandEvaluator eval = new ServerHandEvaluator();

        // Royal flush beats straight flush
        int royalFlush = eval.getScore(List.of(Card.SPADES_A, Card.SPADES_K),
                List.of(Card.SPADES_Q, Card.SPADES_J, Card.SPADES_T, Card.HEARTS_2, Card.CLUBS_3));

        int straightFlush = eval.getScore(List.of(Card.HEARTS_9, Card.HEARTS_8),
                List.of(Card.HEARTS_7, Card.HEARTS_6, Card.HEARTS_5, Card.SPADES_A, Card.CLUBS_K));

        assertTrue(royalFlush > straightFlush);

        // Straight flush beats quads
        int quads = eval.getScore(List.of(Card.SPADES_A, Card.HEARTS_A),
                List.of(Card.DIAMONDS_A, Card.CLUBS_A, Card.SPADES_K, Card.HEARTS_Q, Card.CLUBS_J));

        assertTrue(straightFlush > quads);

        // Quads beats full house
        int fullHouse = eval.getScore(List.of(Card.SPADES_A, Card.HEARTS_A),
                List.of(Card.DIAMONDS_A, Card.SPADES_K, Card.DIAMONDS_K, Card.HEARTS_Q, Card.CLUBS_J));

        assertTrue(quads > fullHouse);

        // Full house beats flush
        int flush = eval.getScore(List.of(Card.SPADES_A, Card.SPADES_K),
                List.of(Card.SPADES_9, Card.SPADES_7, Card.SPADES_2, Card.HEARTS_K, Card.CLUBS_Q));

        assertTrue(fullHouse > flush);

        // Flush beats straight
        int straight = eval.getScore(List.of(Card.SPADES_A, Card.HEARTS_K),
                List.of(Card.DIAMONDS_Q, Card.CLUBS_J, Card.SPADES_T, Card.HEARTS_2, Card.CLUBS_3));

        assertTrue(flush > straight);

        // Straight beats trips
        int trips = eval.getScore(List.of(Card.SPADES_A, Card.HEARTS_A),
                List.of(Card.DIAMONDS_A, Card.SPADES_K, Card.HEARTS_Q, Card.CLUBS_9, Card.DIAMONDS_7));

        assertTrue(straight > trips);

        // Trips beats two pair
        int twoPair = eval.getScore(List.of(Card.SPADES_A, Card.HEARTS_A),
                List.of(Card.SPADES_K, Card.DIAMONDS_K, Card.HEARTS_Q, Card.CLUBS_8, Card.DIAMONDS_5));

        assertTrue(trips > twoPair);

        // Two pair beats pair
        int pair = eval.getScore(List.of(Card.SPADES_A, Card.HEARTS_A),
                List.of(Card.SPADES_K, Card.HEARTS_Q, Card.CLUBS_9, Card.DIAMONDS_7, Card.SPADES_3));

        assertTrue(twoPair > pair);

        // Pair beats high card
        int highCard = eval.getScore(List.of(Card.SPADES_A, Card.HEARTS_K),
                List.of(Card.DIAMONDS_Q, Card.CLUBS_J, Card.SPADES_9, Card.HEARTS_7, Card.CLUBS_2));

        assertTrue(pair > highCard);
    }

    @Test
    void testKickers() {
        ServerHandEvaluator eval = new ServerHandEvaluator();

        // Pair of aces with king kicker beats pair of aces with queen kicker
        int aceKingKicker = eval.getScore(List.of(Card.SPADES_A, Card.HEARTS_A),
                List.of(Card.SPADES_K, Card.HEARTS_Q, Card.CLUBS_J, Card.SPADES_9, Card.CLUBS_7));

        int aceQueenKicker = eval.getScore(List.of(Card.DIAMONDS_A, Card.CLUBS_A),
                List.of(Card.HEARTS_Q, Card.CLUBS_J, Card.DIAMONDS_T, Card.HEARTS_8, Card.DIAMONDS_6));

        assertTrue(aceKingKicker > aceQueenKicker);
    }

    @Test
    void testEmptyHand() {
        ServerHandEvaluator eval = new ServerHandEvaluator();

        int score = eval.getScore(null, null);
        // Empty hand evaluates to high card with no kickers
        assertEquals(HIGH_CARD * SCORE_BASE, score);
    }
}
