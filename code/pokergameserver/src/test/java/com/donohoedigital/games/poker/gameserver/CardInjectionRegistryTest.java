/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 DD Poker Community
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.gameserver;

import com.donohoedigital.games.poker.engine.Card;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CardInjectionRegistry} and the seeded {@link ServerDeck}
 * constructor.
 */
class CardInjectionRegistryTest {

    @BeforeEach
    @AfterEach
    void clearRegistry() {
        CardInjectionRegistry.clear();
    }

    // -------------------------------------------------------------------------
    // takeDeck() with no pending injection
    // -------------------------------------------------------------------------

    @Test
    void takeDeck_noInjection_returnsNull() {
        assertNull(CardInjectionRegistry.takeDeck());
    }

    // -------------------------------------------------------------------------
    // setCards() — explicit card order
    // -------------------------------------------------------------------------

    @Test
    void setCards_takeDeck_returnsDeckWithThoseCards() {
        List<Card> cards = List.of(Card.SPADES_A, Card.HEARTS_K, Card.CLUBS_2, Card.DIAMONDS_3, Card.SPADES_Q,
                Card.HEARTS_J, Card.CLUBS_T, Card.SPADES_9, Card.HEARTS_8, Card.CLUBS_7, Card.SPADES_6, Card.HEARTS_5,
                Card.CLUBS_4);
        CardInjectionRegistry.setCards(cards);

        ServerDeck deck = CardInjectionRegistry.takeDeck();
        assertNotNull(deck);
        assertEquals(cards.size(), deck.size());
        for (Card expected : cards) {
            assertEquals(expected, deck.nextCard());
        }
    }

    @Test
    void setCards_takeDeck_consumesInjection() {
        CardInjectionRegistry.setCards(List.of(Card.SPADES_A, Card.HEARTS_K));

        CardInjectionRegistry.takeDeck(); // consumes
        assertNull(CardInjectionRegistry.takeDeck()); // next call: no injection
    }

    @Test
    void setCards_doesNotMutateOriginalList() {
        List<Card> original = new java.util.ArrayList<>();
        original.add(Card.SPADES_A);
        original.add(Card.HEARTS_K);
        CardInjectionRegistry.setCards(original);

        original.clear(); // mutate after setting
        ServerDeck deck = CardInjectionRegistry.takeDeck();
        assertNotNull(deck);
        assertEquals(2, deck.size()); // registry made a defensive copy
    }

    // -------------------------------------------------------------------------
    // setSeed() — seeded shuffle
    // -------------------------------------------------------------------------

    @Test
    void setSeed_takeDeck_returnsDeckWith52Cards() {
        CardInjectionRegistry.setSeed(42L);
        ServerDeck deck = CardInjectionRegistry.takeDeck();
        assertNotNull(deck);
        assertEquals(52, deck.size());
    }

    @Test
    void setSeed_sameSeed_producesSameDealOrder() {
        CardInjectionRegistry.setSeed(42L);
        ServerDeck deck1 = CardInjectionRegistry.takeDeck();

        CardInjectionRegistry.setSeed(42L);
        ServerDeck deck2 = CardInjectionRegistry.takeDeck();

        assertNotNull(deck1);
        assertNotNull(deck2);
        for (int i = 0; i < 52; i++) {
            assertEquals(deck1.nextCard(), deck2.nextCard(), "Card " + i + " should be identical for same seed");
        }
    }

    @Test
    void setSeed_differentSeeds_produceDifferentDealOrder() {
        CardInjectionRegistry.setSeed(1L);
        ServerDeck deck1 = CardInjectionRegistry.takeDeck();

        CardInjectionRegistry.setSeed(2L);
        ServerDeck deck2 = CardInjectionRegistry.takeDeck();

        assertNotNull(deck1);
        assertNotNull(deck2);
        boolean anyDifference = false;
        for (int i = 0; i < 52; i++) {
            if (deck1.nextCard() != deck2.nextCard()) {
                anyDifference = true;
                break;
            }
        }
        assertTrue(anyDifference, "Different seeds should produce different card orders");
    }

    @Test
    void setSeed_takeDeck_consumesInjection() {
        CardInjectionRegistry.setSeed(99L);
        CardInjectionRegistry.takeDeck(); // consumes
        assertNull(CardInjectionRegistry.takeDeck());
    }

    // -------------------------------------------------------------------------
    // Mutual exclusion: last call wins
    // -------------------------------------------------------------------------

    @Test
    void setSeed_thenSetCards_cardsTakePrecedence() {
        CardInjectionRegistry.setSeed(42L);
        List<Card> cards = List.of(Card.SPADES_A, Card.HEARTS_K);
        CardInjectionRegistry.setCards(cards);

        ServerDeck deck = CardInjectionRegistry.takeDeck();
        assertNotNull(deck);
        assertEquals(2, deck.size());
        assertEquals(Card.SPADES_A, deck.nextCard());
    }

    @Test
    void setCards_thenSetSeed_seedTakePrecedence() {
        CardInjectionRegistry.setCards(List.of(Card.SPADES_A, Card.HEARTS_K));
        CardInjectionRegistry.setSeed(42L);

        ServerDeck deck = CardInjectionRegistry.takeDeck();
        assertNotNull(deck);
        assertEquals(52, deck.size()); // seeded deck has full 52 cards
    }

    // -------------------------------------------------------------------------
    // clear()
    // -------------------------------------------------------------------------

    @Test
    void clear_afterSetCards_takeDeckReturnsNull() {
        CardInjectionRegistry.setCards(List.of(Card.SPADES_A));
        CardInjectionRegistry.clear();
        assertNull(CardInjectionRegistry.takeDeck());
    }

    @Test
    void clear_afterSetSeed_takeDeckReturnsNull() {
        CardInjectionRegistry.setSeed(42L);
        CardInjectionRegistry.clear();
        assertNull(CardInjectionRegistry.takeDeck());
    }

    // -------------------------------------------------------------------------
    // ServerDeck(long seed) constructor directly
    // -------------------------------------------------------------------------

    @Test
    void serverDeckSeeded_has52Cards() {
        ServerDeck deck = new ServerDeck(0L);
        assertEquals(52, deck.size());
    }

    @Test
    void serverDeckSeeded_noDuplicateCards() {
        ServerDeck deck = new ServerDeck(123L);
        java.util.Set<Card> seen = new java.util.HashSet<>();
        while (!deck.isEmpty()) {
            Card c = deck.nextCard();
            assertTrue(seen.add(c), "Duplicate card dealt at index " + c.getIndex());
        }
        assertEquals(52, seen.size());
    }
}
