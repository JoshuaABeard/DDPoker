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

import com.donohoedigital.comms.MsgState;
import com.donohoedigital.config.ConfigTestHelper;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.junit.Assert.*;

/**
 * Comprehensive tests for Hand class - collection of cards with poker-specific
 * queries. Tests construction, card management, sorting, poker queries, and
 * serialization.
 */
public class HandTest {

    @BeforeClass
    public static void setupConfig() {
        ConfigTestHelper.initializeForTesting("poker");
    }

    // ===== Construction Tests =====

    @Test
    public void testEmptyConstruction() {
        Hand hand = new Hand();
        assertEquals(0, hand.size());
        assertEquals(Hand.TYPE_NORMAL, hand.getType());
    }

    @Test
    public void testConstructionWithSize() {
        Hand hand = new Hand(5);
        assertEquals(0, hand.size());
        assertEquals(Hand.TYPE_NORMAL, hand.getType());
    }

    @Test
    public void testConstructionWithType() {
        Hand hand = new Hand(Hand.TYPE_FACE_UP);
        assertEquals(Hand.TYPE_FACE_UP, hand.getType());
    }

    @Test
    public void testConstructionWithTwoCards() {
        Hand hand = new Hand(SPADES_A, HEARTS_K);
        assertEquals(2, hand.size());
        assertEquals(SPADES_A, hand.getCard(0));
        assertEquals(HEARTS_K, hand.getCard(1));
    }

    @Test
    public void testConstructionWithThreeCards() {
        Hand hand = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        assertEquals(3, hand.size());
        assertEquals(SPADES_A, hand.getCard(0));
        assertEquals(HEARTS_K, hand.getCard(1));
        assertEquals(DIAMONDS_Q, hand.getCard(2));
    }

    @Test
    public void testConstructionWithFourCards() {
        Hand hand = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_J);
        assertEquals(4, hand.size());
    }

    @Test
    public void testConstructionWithFiveCards() {
        Hand hand = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_J, SPADES_T);
        assertEquals(5, hand.size());
    }

    @Test
    public void testCopyConstruction() {
        Hand original = new Hand(SPADES_A, HEARTS_K);
        original.setType(Hand.TYPE_FACE_UP);

        Hand copy = new Hand(original);
        assertEquals(2, copy.size());
        assertEquals(SPADES_A, copy.getCard(0));
        assertEquals(HEARTS_K, copy.getCard(1));
        assertEquals(Hand.TYPE_FACE_UP, copy.getType());
    }

    @Test
    public void testPartialCopyConstruction() {
        Hand original = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_J, SPADES_T);

        Hand partial = new Hand(original, 3);
        assertEquals(3, partial.size());
        assertEquals(SPADES_A, partial.getCard(0));
        assertEquals(HEARTS_K, partial.getCard(1));
        assertEquals(DIAMONDS_Q, partial.getCard(2));
    }

    @Test
    public void testCopyNullHand() {
        Hand copy = new Hand(null);
        assertEquals(0, copy.size());
    }

    // ===== Card Management Tests =====

    @Test
    public void testAddCard() {
        Hand hand = new Hand();
        hand.addCard(SPADES_A);
        hand.addCard(HEARTS_K);

        assertEquals(2, hand.size());
        assertEquals(SPADES_A, hand.getCard(0));
        assertEquals(HEARTS_K, hand.getCard(1));
    }

    @Test
    public void testInsertCard() {
        Hand hand = new Hand(SPADES_A, HEARTS_K);
        hand.insertCard(DIAMONDS_Q);

        assertEquals(3, hand.size());
        assertEquals(DIAMONDS_Q, hand.getCard(0)); // Inserted at front
        assertEquals(SPADES_A, hand.getCard(1));
        assertEquals(HEARTS_K, hand.getCard(2));
    }

    @Test
    public void testInsertCardAtIndex() {
        Hand hand = new Hand(SPADES_A, HEARTS_K);
        hand.insertCard(DIAMONDS_Q, 1);

        assertEquals(3, hand.size());
        assertEquals(SPADES_A, hand.getCard(0));
        assertEquals(DIAMONDS_Q, hand.getCard(1)); // Inserted at index 1
        assertEquals(HEARTS_K, hand.getCard(2));
    }

    @Test
    public void testRemoveCard() {
        Hand hand = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        Card removed = hand.removeCard(1);

        assertEquals(2, hand.size());
        assertEquals(HEARTS_K, removed);
        assertEquals(SPADES_A, hand.getCard(0));
        assertEquals(DIAMONDS_Q, hand.getCard(1));
    }

    @Test
    public void testSetCard() {
        Hand hand = new Hand(SPADES_A, HEARTS_K);
        Card old = hand.setCard(1, DIAMONDS_Q);

        assertEquals(HEARTS_K, old);
        assertEquals(DIAMONDS_Q, hand.getCard(1));
    }

    @Test
    public void testRemoveBlank() {
        Hand hand = new Hand();
        hand.addCard(SPADES_A);
        hand.addCard(BLANK);
        hand.addCard(HEARTS_K);
        hand.addCard(BLANK);

        hand.removeBlank();

        assertEquals(2, hand.size());
        assertEquals(SPADES_A, hand.getCard(0));
        assertEquals(HEARTS_K, hand.getCard(1));
    }

    @Test
    public void testCountCard() {
        Hand hand = new Hand();
        hand.addCard(SPADES_A);
        hand.addCard(HEARTS_A);
        hand.addCard(DIAMONDS_K);
        hand.addCard(SPADES_A);

        assertEquals(2, hand.countCard(SPADES_A));
        assertEquals(1, hand.countCard(HEARTS_A));
        assertEquals(1, hand.countCard(DIAMONDS_K));
        assertEquals(0, hand.countCard(CLUBS_Q));
    }

    // ===== Sorting Tests =====

    @Test
    public void testSortAscending() {
        Hand hand = new Hand(HEARTS_K, SPADES_2, DIAMONDS_A, CLUBS_7);
        hand.sortAscending();

        // Should be sorted by rank (2, 7, K, A)
        assertTrue(hand.getCard(0).getRank() <= hand.getCard(1).getRank());
        assertTrue(hand.getCard(1).getRank() <= hand.getCard(2).getRank());
        assertTrue(hand.getCard(2).getRank() <= hand.getCard(3).getRank());
    }

    @Test
    public void testSortDescending() {
        Hand hand = new Hand(HEARTS_K, SPADES_2, DIAMONDS_A, CLUBS_7);
        hand.sortDescending();

        // Should be sorted by rank (A, K, 7, 2)
        assertTrue(hand.getCard(0).getRank() >= hand.getCard(1).getRank());
        assertTrue(hand.getCard(1).getRank() >= hand.getCard(2).getRank());
        assertTrue(hand.getCard(2).getRank() >= hand.getCard(3).getRank());
    }

    // ===== Poker Query Tests =====

    @Test
    public void testIsSuited() {
        Hand suited = new Hand(SPADES_A, SPADES_K, SPADES_Q);
        assertTrue(suited.isSuited());

        Hand notSuited = new Hand(SPADES_A, HEARTS_K, SPADES_Q);
        assertFalse(notSuited.isSuited());

        Hand empty = new Hand();
        assertFalse(empty.isSuited());
    }

    @Test
    public void testIsConnectors() {
        Hand connectors = new Hand(SPADES_9, HEARTS_T);
        assertTrue(connectors.isConnectors(2, ACE));

        Hand notConnectors = new Hand(SPADES_9, HEARTS_J);
        assertFalse(notConnectors.isConnectors(2, ACE));

        // Out of range
        assertFalse(connectors.isConnectors(TWO, EIGHT));
    }

    @Test
    public void testIsRanked() {
        Hand ranked = new Hand(SPADES_K, HEARTS_K, DIAMONDS_K);
        assertTrue(ranked.isRanked());

        Hand notRanked = new Hand(SPADES_K, HEARTS_K, DIAMONDS_Q);
        assertFalse(notRanked.isRanked());

        Hand empty = new Hand();
        assertFalse(empty.isRanked());
    }

    @Test
    public void testIsPair() {
        Hand pair = new Hand(SPADES_K, HEARTS_K);
        assertTrue(pair.isPair());

        Hand notPair = new Hand(SPADES_K, HEARTS_Q);
        assertFalse(notPair.isPair());

        // Three of a kind is not a pair
        Hand trips = new Hand(SPADES_K, HEARTS_K, DIAMONDS_K);
        assertFalse(trips.isPair());
    }

    // ===== Hand Analysis Tests =====

    @Test
    public void testHasPair() {
        Hand pair = new Hand(SPADES_K, HEARTS_K, DIAMONDS_Q);
        assertTrue(pair.hasPair());

        Hand noPair = new Hand(SPADES_K, HEARTS_Q, DIAMONDS_J);
        assertFalse(noPair.hasPair());

        Hand exactPair = new Hand(SPADES_K, HEARTS_K);
        assertTrue(exactPair.hasPair());
    }

    @Test
    public void testHasTrips() {
        Hand trips = new Hand(SPADES_K, HEARTS_K, DIAMONDS_K);
        assertTrue(trips.hasTrips());

        Hand tripsInFive = new Hand(SPADES_K, HEARTS_K, DIAMONDS_K, CLUBS_Q, SPADES_J);
        assertTrue(tripsInFive.hasTrips());

        Hand noTrips = new Hand(SPADES_K, HEARTS_K, DIAMONDS_Q);
        assertFalse(noTrips.hasTrips());
    }

    @Test
    public void testHasQuads() {
        Hand quads = new Hand(SPADES_K, HEARTS_K, DIAMONDS_K, CLUBS_K);
        assertTrue(quads.hasQuads());

        Hand quadsInFive = new Hand(SPADES_K, HEARTS_K, DIAMONDS_K, CLUBS_K, SPADES_A);
        assertTrue(quadsInFive.hasQuads());

        Hand noQuads = new Hand(SPADES_K, HEARTS_K, DIAMONDS_K);
        assertFalse(noQuads.hasQuads());
    }

    @Test
    public void testHasFlush() {
        Hand flush = new Hand(SPADES_A, SPADES_K, SPADES_Q, SPADES_J, SPADES_9);
        assertTrue(flush.hasFlush());

        Hand noFlush = new Hand(SPADES_A, HEARTS_K, SPADES_Q, SPADES_J, SPADES_9);
        assertFalse(noFlush.hasFlush());
    }

    @Test
    public void testHasPossibleFlush() {
        Hand possibleFlush = new Hand(SPADES_A, SPADES_K, SPADES_Q);
        assertTrue(possibleFlush.hasPossibleFlush());

        Hand noPossibleFlush = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        assertFalse(noPossibleFlush.hasPossibleFlush());
    }

    // ===== Rank Query Tests =====

    @Test
    public void testGetHighestRank() {
        Hand hand = new Hand(HEARTS_K, SPADES_2, DIAMONDS_7);
        assertEquals(KING, hand.getHighestRank());

        Hand withAce = new Hand(HEARTS_K, SPADES_A, DIAMONDS_7);
        assertEquals(ACE, withAce.getHighestRank());

        Hand empty = new Hand();
        assertEquals(UNKNOWN, empty.getHighestRank());
    }

    @Test
    public void testGetLowestRank() {
        Hand hand = new Hand(HEARTS_K, SPADES_2, DIAMONDS_7);
        assertEquals(TWO, hand.getLowestRank());

        Hand withoutTwo = new Hand(HEARTS_K, SPADES_7, DIAMONDS_9);
        assertEquals(SEVEN, withoutTwo.getLowestRank());

        Hand empty = new Hand();
        assertEquals(UNKNOWN, empty.getLowestRank());
    }

    @Test
    public void testGetHighestSuited() {
        Hand hand = new Hand(SPADES_A, SPADES_K, HEARTS_Q, DIAMONDS_J, SPADES_T);
        assertEquals(3, hand.getHighestSuited()); // 3 spades

        Hand allDifferent = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_J);
        assertEquals(1, allDifferent.getHighestSuited());
    }

    @Test
    public void testGetNumSuits() {
        Hand hand = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_J);
        assertEquals(4, hand.getNumSuits());

        Hand twoSuits = new Hand(SPADES_A, SPADES_K, HEARTS_Q);
        assertEquals(2, twoSuits.getNumSuits());

        Hand oneSuit = new Hand(SPADES_A, SPADES_K, SPADES_Q);
        assertEquals(1, oneSuit.getNumSuits());
    }

    // ===== Containment Tests =====

    @Test
    public void testContainsCard() {
        Hand hand = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        assertTrue(hand.containsCard(SPADES_A));
        assertTrue(hand.containsCard(HEARTS_K));
        assertFalse(hand.containsCard(CLUBS_J));
    }

    @Test
    public void testContainsCardByIndex() {
        Hand hand = new Hand(SPADES_A, HEARTS_K);
        assertTrue(hand.containsCard(SPADES_A.getIndex()));
        assertTrue(hand.containsCard(HEARTS_K.getIndex()));
        assertFalse(hand.containsCard(CLUBS_J.getIndex()));
    }

    @Test
    public void testContainsAny() {
        Hand hand1 = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        Hand hand2 = new Hand(SPADES_A, CLUBS_J);

        assertTrue(hand1.containsAny(hand2)); // Both have SPADES_A

        Hand hand3 = new Hand(CLUBS_T, CLUBS_9);
        assertFalse(hand1.containsAny(hand3));
    }

    @Test
    public void testContainsRank() {
        Hand hand = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        assertTrue(hand.containsRank(ACE));
        assertTrue(hand.containsRank(KING));
        assertTrue(hand.containsRank(QUEEN));
        assertFalse(hand.containsRank(JACK));
    }

    @Test
    public void testContainsSuit() {
        Hand hand = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        assertTrue(hand.containsSuit(CardSuit.SPADES_RANK));
        assertTrue(hand.containsSuit(CardSuit.HEARTS_RANK));
        assertTrue(hand.containsSuit(CardSuit.DIAMONDS_RANK));
        assertFalse(hand.containsSuit(CardSuit.CLUBS_RANK));
    }

    @Test
    public void testIsInHandByRank() {
        Hand hand = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        assertTrue(hand.isInHand(ACE));
        assertTrue(hand.isInHand(KING));
        assertFalse(hand.isInHand(JACK));
    }

    @Test
    public void testIsInHandByRankAndSuit() {
        Hand hand = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        assertTrue(hand.isInHand(ACE, CardSuit.SPADES_RANK));
        assertTrue(hand.isInHand(KING, CardSuit.HEARTS_RANK));
        assertFalse(hand.isInHand(ACE, CardSuit.HEARTS_RANK)); // Wrong suit
        assertFalse(hand.isInHand(JACK, CardSuit.SPADES_RANK)); // Not in hand
    }

    // ===== Fingerprint Tests =====

    @Test
    public void testFingerprint() {
        Hand hand = new Hand(SPADES_A, HEARTS_K);
        long fingerprint = hand.fingerprint();
        assertTrue(fingerprint > 0);

        // Fingerprint should be cached
        long fingerprint2 = hand.fingerprint();
        assertEquals(fingerprint, fingerprint2);
    }

    @Test
    public void testFingerprintRecalculatesAfterModification() {
        Hand hand = new Hand(SPADES_A, HEARTS_K);
        long fingerprint1 = hand.fingerprint();

        hand.addCard(DIAMONDS_Q);
        long fingerprint2 = hand.fingerprint();

        assertNotEquals(fingerprint1, fingerprint2);
    }

    @Test
    public void testFingerprintN() {
        Hand hand = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_J, SPADES_T);

        long fingerprint3 = hand.fingerprint(3);
        assertTrue(fingerprint3 > 0);

        long fingerprint5 = hand.fingerprint(5);
        assertNotEquals(fingerprint3, fingerprint5);

        // Asking for more cards than in hand returns 0
        assertEquals(0L, hand.fingerprint(10));
    }

    @Test
    public void testCardsChanged() {
        Hand hand = new Hand();
        // Create non-constant cards to avoid modifying static constants
        hand.addCard(new Card(CardSuit.SPADES, ACE));
        hand.addCard(new Card(CardSuit.HEARTS, KING));
        long fingerprint1 = hand.fingerprint();

        // Modify a card directly (bypassing add/remove)
        hand.getCard(0).setValue(DIAMONDS_Q);
        hand.cardsChanged();

        long fingerprint2 = hand.fingerprint();
        assertNotEquals(fingerprint1, fingerprint2);
    }

    // ===== Display Tests =====

    @Test
    public void testToStringRankSuit() {
        Hand hand = new Hand(SPADES_A, HEARTS_K);
        String display = hand.toStringRankSuit();
        assertNotNull(display);
        assertTrue(display.contains("A") || display.toLowerCase().contains("ace"));
        assertTrue(display.contains("K") || display.toLowerCase().contains("king"));
    }

    @Test
    public void testToStringRank() {
        Hand hand = new Hand(SPADES_A, HEARTS_K);
        String display = hand.toStringRank();
        assertNotNull(display);
        assertTrue(display.contains("A") || display.toLowerCase().contains("ace"));
        assertTrue(display.contains("K") || display.toLowerCase().contains("king"));
    }

    @Test
    public void testToStringSuited() {
        Hand suited = new Hand(SPADES_A, SPADES_K);
        String display = suited.toStringSuited();
        assertNotNull(display);
        assertTrue(display.endsWith("*")); // Suited hands end with *

        Hand notSuited = new Hand(SPADES_A, HEARTS_K);
        display = notSuited.toStringSuited();
        assertFalse(display.endsWith("*"));
    }

    @Test
    public void testToHTML() {
        Hand hand = new Hand(SPADES_A, HEARTS_K);
        String html = hand.toHTML();
        assertNotNull(html);
        assertTrue(html.contains("DDCARD"));

        Hand empty = new Hand();
        assertEquals("", empty.toHTML());
    }

    // ===== Serialization Tests =====

    @Test
    public void testMarshalDemarshal() {
        Hand original = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        original.setType(Hand.TYPE_FACE_UP);

        MsgState state = new MsgState();
        String marshalled = original.marshal(state);
        assertNotNull(marshalled);

        Hand restored = new Hand();
        restored.demarshal(state, marshalled);

        assertEquals(original.size(), restored.size());
        assertEquals(original.getType(), restored.getType());
        for (int i = 0; i < original.size(); i++) {
            assertEquals(original.getCard(i), restored.getCard(i));
        }
    }

    @Test
    public void testMarshalDemarshalEmpty() {
        Hand empty = new Hand();
        MsgState state = new MsgState();

        String marshalled = empty.marshal(state);
        Hand restored = new Hand();
        restored.demarshal(state, marshalled);

        assertEquals(0, restored.size());
    }

    @Test
    public void testMarshalDemarshalDifferentTypes() {
        char[] types = {Hand.TYPE_NORMAL, Hand.TYPE_FACE_UP, Hand.TYPE_DEAL_HIGH, Hand.TYPE_COLOR_UP};

        for (char type : types) {
            Hand original = new Hand(type);
            original.addCard(SPADES_A);

            MsgState state = new MsgState();
            String marshalled = original.marshal(state);

            Hand restored = new Hand();
            restored.demarshal(state, marshalled);

            assertEquals(type, restored.getType());
            assertEquals(1, restored.size());
            assertEquals(SPADES_A, restored.getCard(0));
        }
    }

    // ===== Type Tests =====

    @Test
    public void testGetAndSetType() {
        Hand hand = new Hand();
        assertEquals(Hand.TYPE_NORMAL, hand.getType());

        hand.setType(Hand.TYPE_FACE_UP);
        assertEquals(Hand.TYPE_FACE_UP, hand.getType());
    }

    // ===== Edge Cases =====

    @Test
    public void testEmptyHandOperations() {
        Hand hand = new Hand();

        assertFalse(hand.isSuited());
        assertFalse(hand.isRanked());
        assertFalse(hand.isPair());
        assertFalse(hand.hasPair());
        assertFalse(hand.hasTrips());
        assertFalse(hand.hasQuads());
        assertFalse(hand.hasFlush());
        assertFalse(hand.hasPossibleFlush());

        assertEquals(0L, hand.fingerprint());
        assertEquals(UNKNOWN, hand.getHighestRank());
        assertEquals(UNKNOWN, hand.getLowestRank());
    }

    @Test
    public void testSingleCardHand() {
        Hand hand = new Hand();
        hand.addCard(SPADES_A);

        assertEquals(1, hand.size());
        assertTrue(hand.isSuited()); // Single card is "suited"
        assertTrue(hand.isRanked()); // Single card is "ranked"
        assertFalse(hand.isPair());
        assertFalse(hand.hasPair());

        assertEquals(ACE, hand.getHighestRank());
        assertEquals(ACE, hand.getLowestRank());
    }
}
