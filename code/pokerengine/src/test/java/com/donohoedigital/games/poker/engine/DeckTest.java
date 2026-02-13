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
 * Tests for Deck class core functionality (dealing, removing, sorting).
 * Randomness/shuffle tests are in DeckRandomnessTest.
 */
public class DeckTest {

    @BeforeClass
    public static void setupConfig() {
        ConfigTestHelper.initializeForTesting("poker");
    }

    // ===== Construction Tests =====

    @Test
    public void testUnshuffledDeckConstruction() {
        Deck deck = new Deck(false);
        assertEquals(52, deck.size());

        // Unshuffled deck should be in order: Spades 2-A, Hearts 2-A, Diamonds 2-A,
        // Clubs 2-A
        assertEquals(SPADES_2, deck.get(0));
        assertEquals(SPADES_A, deck.get(12));
        assertEquals(HEARTS_2, deck.get(13));
        assertEquals(HEARTS_A, deck.get(25));
        assertEquals(DIAMONDS_2, deck.get(26));
        assertEquals(DIAMONDS_A, deck.get(38));
        assertEquals(CLUBS_2, deck.get(39));
        assertEquals(CLUBS_A, deck.get(51));
    }

    @Test
    public void testShuffledDeckHas52Cards() {
        Deck deck = new Deck(true);
        assertEquals(52, deck.size());
    }

    @Test
    public void testEmptyDeckConstruction() {
        Deck deck = new Deck();
        assertEquals(0, deck.size());
    }

    // ===== nextCard() Tests =====

    @Test
    public void testNextCardDealsFromTop() {
        Deck deck = new Deck(false);
        Card first = deck.nextCard();

        assertEquals(SPADES_2, first);
        assertEquals(51, deck.size());
    }

    @Test
    public void testNextCardRemovesCard() {
        Deck deck = new Deck(false);
        deck.nextCard(); // Remove first card

        // Second card should now be at index 0
        assertEquals(SPADES_3, deck.get(0));
    }

    @Test
    public void testDealingAllCards() {
        Deck deck = new Deck(false);

        for (int i = 0; i < 52; i++) {
            assertNotNull(deck.nextCard());
        }

        assertEquals(0, deck.size());
    }

    // ===== getCard() Tests =====

    @Test
    public void testGetCard() {
        Deck deck = new Deck(false);
        assertEquals(SPADES_2, deck.getCard(0));
        assertEquals(CLUBS_A, deck.getCard(51));
        assertEquals(HEARTS_A, deck.getCard(25));
    }

    @Test
    public void testGetCardDoesNotRemove() {
        Deck deck = new Deck(false);
        deck.getCard(0);
        assertEquals(52, deck.size()); // Size unchanged
    }

    // ===== removeCard() Tests =====

    @Test
    public void testRemoveCard() {
        Deck deck = new Deck(false);
        deck.removeCard(HEARTS_K);

        assertEquals(51, deck.size());
        assertFalse(deck.contains(HEARTS_K));
    }

    @Test
    public void testRemoveMultipleCards() {
        Deck deck = new Deck(false);
        deck.removeCard(SPADES_A);
        deck.removeCard(HEARTS_K);
        deck.removeCard(DIAMONDS_Q);

        assertEquals(49, deck.size());
        assertFalse(deck.contains(SPADES_A));
        assertFalse(deck.contains(HEARTS_K));
        assertFalse(deck.contains(DIAMONDS_Q));
    }

    // ===== moveToTop() Tests =====

    @Test
    public void testMoveToTop() {
        Deck deck = new Deck(false);
        deck.moveToTop(CLUBS_A); // Move last card to top

        assertEquals(CLUBS_A, deck.get(0));
        assertEquals(52, deck.size()); // Size unchanged
    }

    @Test
    public void testMoveToTopMultipleTimes() {
        Deck deck = new Deck(false);
        deck.moveToTop(HEARTS_K);
        deck.moveToTop(DIAMONDS_Q);
        deck.moveToTop(CLUBS_J);

        // Last moved should be on top
        assertEquals(CLUBS_J, deck.get(0));
        assertEquals(DIAMONDS_Q, deck.get(1));
        assertEquals(HEARTS_K, deck.get(2));
    }

    // ===== removeCards(Hand) Tests =====

    @Test
    public void testRemoveCardsFromHand() {
        Deck deck = new Deck(false);
        Hand hand = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);

        deck.removeCards(hand);

        assertEquals(49, deck.size());
        assertFalse(deck.contains(SPADES_A));
        assertFalse(deck.contains(HEARTS_K));
        assertFalse(deck.contains(DIAMONDS_Q));
    }

    @Test
    public void testRemoveCardsNullHand() {
        Deck deck = new Deck(false);
        deck.removeCards(null); // Should not throw

        assertEquals(52, deck.size());
    }

    @Test
    public void testRemoveCardsEmptyHand() {
        Deck deck = new Deck(false);
        Hand empty = new Hand();
        deck.removeCards(empty);

        assertEquals(52, deck.size());
    }

    // ===== addRandom() Tests =====

    @Test
    public void testAddRandomCard() {
        Deck deck = new Deck(false);
        deck.removeCard(SPADES_A);

        assertEquals(51, deck.size());

        deck.addRandom(SPADES_A);

        assertEquals(52, deck.size());
        assertTrue(deck.contains(SPADES_A));
    }

    @Test
    public void testAddRandomHand() {
        Deck deck = new Deck(false);
        Hand hand = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        deck.removeCards(hand);

        assertEquals(49, deck.size());

        deck.addRandom(hand);

        assertEquals(52, deck.size());
        assertTrue(deck.contains(SPADES_A));
        assertTrue(deck.contains(HEARTS_K));
        assertTrue(deck.contains(DIAMONDS_Q));
    }

    // ===== Sorting Tests =====

    @Test
    public void testSortAscending() {
        Deck deck = new Deck(true); // Shuffled
        deck.sortAscending();

        // Should be sorted by card comparison (rank-based)
        for (int i = 0; i < deck.size() - 1; i++) {
            assertTrue(deck.get(i).compareTo(deck.get(i + 1)) <= 0);
        }
    }

    @Test
    public void testSortDescending() {
        Deck deck = new Deck(true); // Shuffled
        deck.sortDescending();

        // Should be sorted descending by card comparison
        for (int i = 0; i < deck.size() - 1; i++) {
            assertTrue(deck.get(i).compareTo(deck.get(i + 1)) >= 0);
        }
    }

    @Test
    public void testSortAscendingAfterDealing() {
        Deck deck = new Deck(false);
        deck.nextCard();
        deck.nextCard();
        deck.nextCard();

        deck.sortAscending();

        assertEquals(49, deck.size());
        // Should still be sorted
        for (int i = 0; i < deck.size() - 1; i++) {
            assertTrue(deck.get(i).compareTo(deck.get(i + 1)) <= 0);
        }
    }

    // ===== toString() Tests =====

    @Test
    public void testToString() {
        Deck deck = new Deck(false);
        String str = deck.toString();

        assertNotNull(str);
        assertTrue(str.length() > 0);
        // Should contain card representations
        assertTrue(str.contains("2") || str.contains("s")); // Some card notation
    }

    @Test
    public void testToStringEmptyDeck() {
        Deck deck = new Deck();
        String str = deck.toString();

        assertNotNull(str);
        assertEquals("", str.trim());
    }

    // ===== Bug-Specific Deck Tests =====

    @Test
    public void testGetDeckBUG280() {
        Deck deck = Deck.getDeckBUG280();

        assertNotNull(deck);
        assertEquals(52, deck.size());

        // Check stacked order (specific cards on top for bug reproduction)
        assertEquals(SPADES_Q, deck.get(0)); // opp 1, card 1
        assertEquals(CLUBS_K, deck.get(1)); // opp 2, card 1
        assertEquals(SPADES_K, deck.get(2)); // opp 1, card 2
        assertEquals(HEARTS_Q, deck.get(3)); // opp 2, card 2
    }

    @Test
    public void testGetDeckBUG284() {
        Deck deck = Deck.getDeckBUG284();

        assertNotNull(deck);
        assertEquals(52, deck.size());

        // Check stacked order
        assertEquals(SPADES_5, deck.get(0));
        assertEquals(CLUBS_A, deck.get(1));
    }

    @Test
    public void testGetDeckBUG316() {
        Deck deck = Deck.getDeckBUG316();

        assertNotNull(deck);
        assertEquals(52, deck.size());

        // Check stacked order
        assertEquals(DIAMONDS_9, deck.get(0));
        assertEquals(CLUBS_A, deck.get(1));
    }

    // ===== Edge Cases =====

    @Test
    public void testDeckContainsAllUniqueCards() {
        Deck deck = new Deck(false);

        // Should have exactly 52 unique cards
        assertEquals(52, deck.size());

        // Check no duplicates
        for (int i = 0; i < deck.size(); i++) {
            Card card = deck.get(i);
            int count = 0;
            for (int j = 0; j < deck.size(); j++) {
                if (deck.get(j).equals(card)) {
                    count++;
                }
            }
            assertEquals("Card " + card + " should appear exactly once", 1, count);
        }
    }

    @Test
    public void testShuffledDeckContainsAllCards() {
        Deck deck = new Deck(true);

        // Even shuffled, should have all 52 unique cards
        assertEquals(52, deck.size());

        // Check all standard cards are present
        assertTrue(deck.contains(SPADES_A));
        assertTrue(deck.contains(HEARTS_K));
        assertTrue(deck.contains(DIAMONDS_Q));
        assertTrue(deck.contains(CLUBS_2));
    }

    @Test
    public void testDealingAndReaddingCards() {
        Deck deck = new Deck(false);

        // Deal 5 cards
        Hand dealt = new Hand();
        for (int i = 0; i < 5; i++) {
            dealt.addCard(deck.nextCard());
        }

        assertEquals(47, deck.size());
        assertEquals(5, dealt.size());

        // Add them back randomly
        deck.addRandom(dealt);

        assertEquals(52, deck.size());
    }

    @Test
    public void testShuffleMethodChangesOrder() {
        Deck deck1 = new Deck(false);
        Deck deck2 = new Deck(false);

        // Shuffle one deck
        deck2.shuffle();

        // At least some cards should be in different positions
        boolean foundDifference = false;
        for (int i = 0; i < 52; i++) {
            if (!deck1.get(i).equals(deck2.get(i))) {
                foundDifference = true;
                break;
            }
        }

        assertTrue("shuffle() should change card order", foundDifference);
    }
}
