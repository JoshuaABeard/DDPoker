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
package com.donohoedigital.games.poker.engine;

import com.donohoedigital.config.ConfigTestHelper;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.junit.Assert.*;

/**
 * Tests for HandSorted class - an ascending sorted hand that maintains sort
 * order.
 */
public class HandSortedTest {

    @BeforeClass
    public static void setupConfig() {
        ConfigTestHelper.initializeForTesting("poker");
    }

    // ===== Construction Tests =====

    @Test
    public void testEmptyConstruction() {
        HandSorted hand = new HandSorted();
        assertEquals(0, hand.size());
    }

    @Test
    public void testConstructionWithSize() {
        HandSorted hand = new HandSorted(10);
        assertEquals(0, hand.size());
    }

    @Test
    public void testConstructionFromHand() {
        Hand unsorted = new Hand(HEARTS_K, SPADES_2, DIAMONDS_A);
        HandSorted sorted = new HandSorted(unsorted);

        assertEquals(3, sorted.size());
        // Should be sorted ascending (2, K, A)
        assertEquals(TWO, sorted.getCard(0).getRank());
        assertEquals(KING, sorted.getCard(1).getRank());
        assertEquals(ACE, sorted.getCard(2).getRank());
    }

    @Test
    public void testConstructionFromHandSorted() {
        HandSorted original = new HandSorted(SPADES_2, HEARTS_K, DIAMONDS_A);
        HandSorted copy = new HandSorted(original);

        assertEquals(3, copy.size());
        // Should maintain sorted order
        assertEquals(TWO, copy.getCard(0).getRank());
        assertEquals(KING, copy.getCard(1).getRank());
        assertEquals(ACE, copy.getCard(2).getRank());
    }

    @Test
    public void testConstructionWithTwoCards() {
        HandSorted hand = new HandSorted(HEARTS_K, SPADES_2);
        assertEquals(2, hand.size());
        // Should be sorted (2, K)
        assertEquals(TWO, hand.getCard(0).getRank());
        assertEquals(KING, hand.getCard(1).getRank());
    }

    @Test
    public void testConstructionWithThreeCards() {
        HandSorted hand = new HandSorted(HEARTS_K, SPADES_2, DIAMONDS_A);
        assertEquals(3, hand.size());
        assertEquals(TWO, hand.getCard(0).getRank());
        assertEquals(KING, hand.getCard(1).getRank());
        assertEquals(ACE, hand.getCard(2).getRank());
    }

    @Test
    public void testConstructionWithFourCards() {
        HandSorted hand = new HandSorted(HEARTS_K, SPADES_2, DIAMONDS_A, CLUBS_7);
        assertEquals(4, hand.size());
        assertEquals(TWO, hand.getCard(0).getRank());
        assertEquals(SEVEN, hand.getCard(1).getRank());
        assertEquals(KING, hand.getCard(2).getRank());
        assertEquals(ACE, hand.getCard(3).getRank());
    }

    @Test
    public void testConstructionWithFiveCards() {
        HandSorted hand = new HandSorted(HEARTS_K, SPADES_2, DIAMONDS_A, CLUBS_7, HEARTS_J);
        assertEquals(5, hand.size());
        assertEquals(TWO, hand.getCard(0).getRank());
        assertEquals(SEVEN, hand.getCard(1).getRank());
        assertEquals(JACK, hand.getCard(2).getRank());
        assertEquals(KING, hand.getCard(3).getRank());
        assertEquals(ACE, hand.getCard(4).getRank());
    }

    // ===== Sorted addCard Tests =====

    @Test
    public void testAddCardMaintainsSortOrder() {
        HandSorted hand = new HandSorted();
        hand.addCard(HEARTS_K);
        hand.addCard(SPADES_2);
        hand.addCard(DIAMONDS_A);
        hand.addCard(CLUBS_7);

        // Should be sorted ascending
        assertEquals(TWO, hand.getCard(0).getRank());
        assertEquals(SEVEN, hand.getCard(1).getRank());
        assertEquals(KING, hand.getCard(2).getRank());
        assertEquals(ACE, hand.getCard(3).getRank());
    }

    @Test
    public void testAddCardWithStartIndex() {
        HandSorted hand = new HandSorted();
        int idx1 = hand.addCard(SPADES_5, 0);
        int idx2 = hand.addCard(HEARTS_9, 0);
        int idx3 = hand.addCard(DIAMONDS_7, 0);

        assertEquals(0, idx1); // First card at index 0
        assertEquals(1, idx2); // 9 added after 5
        assertEquals(1, idx3); // 7 inserted between 5 and 9

        assertEquals(FIVE, hand.getCard(0).getRank());
        assertEquals(SEVEN, hand.getCard(1).getRank());
        assertEquals(NINE, hand.getCard(2).getRank());
    }

    @Test
    public void testAddCardReturnsCorrectIndex() {
        HandSorted hand = new HandSorted();
        assertEquals(0, hand.addCard(SPADES_5, 0)); // Added at 0
        assertEquals(1, hand.addCard(HEARTS_K, 0)); // Added at 1 (after 5)
        assertEquals(1, hand.addCard(DIAMONDS_9, 0)); // Inserted at 1 (between 5 and K)
    }

    // ===== addAll Tests =====

    @Test
    public void testAddAllFromHandSorted() {
        HandSorted hand1 = new HandSorted(SPADES_2, HEARTS_7);
        HandSorted hand2 = new HandSorted(DIAMONDS_5, CLUBS_K);

        hand1.addAll(hand2);

        assertEquals(4, hand1.size());
        assertEquals(TWO, hand1.getCard(0).getRank());
        assertEquals(FIVE, hand1.getCard(1).getRank());
        assertEquals(SEVEN, hand1.getCard(2).getRank());
        assertEquals(KING, hand1.getCard(3).getRank());
    }

    @Test
    public void testAddAllFromRegularHand() {
        HandSorted sorted = new HandSorted(SPADES_2, HEARTS_7);
        Hand unsorted = new Hand(CLUBS_K, DIAMONDS_5);

        sorted.addAll(unsorted);

        assertEquals(4, sorted.size());
        // Should maintain sorted order
        assertEquals(TWO, sorted.getCard(0).getRank());
        assertEquals(FIVE, sorted.getCard(1).getRank());
        assertEquals(SEVEN, sorted.getCard(2).getRank());
        assertEquals(KING, sorted.getCard(3).getRank());
    }

    // ===== isEquivalent Tests =====

    @Test
    public void testIsEquivalentSameRanksSuited() {
        HandSorted hand1 = new HandSorted(SPADES_A, SPADES_K);
        HandSorted hand2 = new HandSorted(HEARTS_A, HEARTS_K);

        assertTrue(hand1.isEquivalent(hand2));
        assertTrue(hand2.isEquivalent(hand1));
    }

    @Test
    public void testIsEquivalentSameRanksNotSuited() {
        HandSorted hand1 = new HandSorted(SPADES_A, HEARTS_K);
        HandSorted hand2 = new HandSorted(DIAMONDS_A, CLUBS_K);

        assertTrue(hand1.isEquivalent(hand2));
    }

    @Test
    public void testIsEquivalentDifferentRanks() {
        HandSorted hand1 = new HandSorted(SPADES_A, SPADES_K);
        HandSorted hand2 = new HandSorted(HEARTS_A, HEARTS_Q);

        assertFalse(hand1.isEquivalent(hand2));
    }

    @Test
    public void testIsEquivalentDifferentSuitedness() {
        HandSorted hand1 = new HandSorted(SPADES_A, SPADES_K); // Suited
        HandSorted hand2 = new HandSorted(HEARTS_A, DIAMONDS_K); // Not suited

        assertFalse(hand1.isEquivalent(hand2));
    }

    @Test
    public void testIsEquivalentDifferentSizes() {
        HandSorted hand1 = new HandSorted(SPADES_A, SPADES_K);
        HandSorted hand2 = new HandSorted(HEARTS_A, HEARTS_K, DIAMONDS_Q);

        assertFalse(hand1.isEquivalent(hand2));
    }

    // ===== hasStraightDraw Tests =====

    @Test
    public void testHasStraightDrawWithOpenEnded() {
        // 5 6 7 has straight draw (4-8 completes)
        HandSorted hand = new HandSorted(SPADES_5, HEARTS_6, DIAMONDS_7);
        assertTrue(hand.hasStraightDraw());
    }

    @Test
    public void testHasStraightDrawWithGutshot() {
        // 5 7 has gutshot (6 completes)
        HandSorted hand = new HandSorted(SPADES_5, HEARTS_7);
        assertTrue(hand.hasStraightDraw());
    }

    @Test
    public void testHasStraightDrawWithAceLow() {
        // A 2 3 has straight draw (A-2-3-4-5 wheel)
        HandSorted hand = new HandSorted(SPADES_A, HEARTS_2, DIAMONDS_3);
        assertTrue(hand.hasStraightDraw());
    }

    @Test
    public void testHasStraightDrawNoGaps() {
        // 2 7 K - gaps too large
        HandSorted hand = new HandSorted(SPADES_2, HEARTS_7, DIAMONDS_K);
        assertFalse(hand.hasStraightDraw());
    }

    // ===== hasPair Tests =====

    @Test
    public void testHasPair() {
        HandSorted hand = new HandSorted(SPADES_K, HEARTS_K, DIAMONDS_7);
        assertTrue(hand.hasPair());
    }

    @Test
    public void testHasPairNoPair() {
        HandSorted hand = new HandSorted(SPADES_K, HEARTS_Q, DIAMONDS_7);
        assertFalse(hand.hasPair());
    }

    @Test
    public void testHasPairWithMultiplePairs() {
        HandSorted hand = new HandSorted(SPADES_7, HEARTS_7, DIAMONDS_K, CLUBS_K);
        assertTrue(hand.hasPair());
    }

    // ===== hasConnector Tests =====

    @Test
    public void testHasConnectorNoGap() {
        // K Q has connector with 0 gap
        HandSorted hand = new HandSorted(SPADES_K, HEARTS_Q);
        assertTrue(hand.hasConnector(0));
    }

    @Test
    public void testHasConnectorWithGap() {
        // K J has connector with 1 gap
        HandSorted hand = new HandSorted(SPADES_K, HEARTS_J);
        assertTrue(hand.hasConnector(1));
    }

    @Test
    public void testHasConnectorNoConnector() {
        // K 9 has 2-gap, so no connector with gap 1
        HandSorted hand = new HandSorted(SPADES_K, HEARTS_9);
        assertFalse(hand.hasConnector(1));
    }

    @Test
    public void testHasConnectorWithMinBottom() {
        // 3 4 has connector, but min bottom is 5
        HandSorted hand = new HandSorted(SPADES_3, HEARTS_4);
        assertFalse(hand.hasConnector(0, 5));
    }

    @Test
    public void testHasConnectorAceLow() {
        // A 2 is a connector (ace counts as 1)
        HandSorted hand = new HandSorted(SPADES_A, HEARTS_2);
        assertTrue(hand.hasConnector(0, 1));
    }

    // ===== getHighestPair Tests =====

    @Test
    public void testGetHighestPairSinglePair() {
        HandSorted hand = new HandSorted(SPADES_K, HEARTS_K, DIAMONDS_7);
        assertEquals(KING, hand.getHighestPair());
    }

    @Test
    public void testGetHighestPairMultiplePairs() {
        HandSorted hand = new HandSorted(SPADES_7, HEARTS_7, DIAMONDS_K, CLUBS_K);
        assertEquals(KING, hand.getHighestPair()); // Returns highest
    }

    @Test
    public void testGetHighestPairNoPair() {
        HandSorted hand = new HandSorted(SPADES_K, HEARTS_Q, DIAMONDS_7);
        assertEquals(0, hand.getHighestPair());
    }

    @Test
    public void testGetHighestPairWithTrips() {
        HandSorted hand = new HandSorted(SPADES_K, HEARTS_K, DIAMONDS_K);
        assertEquals(KING, hand.getHighestPair()); // Trips counts as pair
    }

    // ===== Edge Cases =====

    @Test
    public void testEmptyHandOperations() {
        HandSorted hand = new HandSorted();
        assertFalse(hand.hasPair());
        // Note: hasStraightDraw() throws IndexOutOfBoundsException on empty hand -
        // production code bug (tested explicitly in testEmptyHandStraightDrawBug)
        assertEquals(0, hand.getHighestPair());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testEmptyHandStraightDrawBug() {
        // Production bug: hasStraightDraw() throws IndexOutOfBoundsException on empty
        // hands
        // This test pins down the buggy behavior as a regression marker
        HandSorted hand = new HandSorted();
        hand.hasStraightDraw(); // Should check size > 0 before accessing getCard(nSize - 1)
    }

    @Test
    public void testSingleCardHand() {
        HandSorted hand = new HandSorted();
        hand.addCard(SPADES_A);

        assertEquals(1, hand.size());
        assertFalse(hand.hasPair());
        assertFalse(hand.hasStraightDraw());
        assertEquals(0, hand.getHighestPair());
    }

    @Test
    public void testAddDuplicateCards() {
        HandSorted hand = new HandSorted();
        hand.addCard(SPADES_K);
        hand.addCard(HEARTS_K);
        hand.addCard(DIAMONDS_K);

        assertEquals(3, hand.size());
        // All three should be Kings in sorted order
        assertEquals(KING, hand.getCard(0).getRank());
        assertEquals(KING, hand.getCard(1).getRank());
        assertEquals(KING, hand.getCard(2).getRank());
    }
}
