/*
 * ============================================================================================
 * DD Poker - Source Code
 * Copyright (c) 2026  DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ============================================================================================
 */
package com.donohoedigital.games.poker.gameserver;

import static org.junit.jupiter.api.Assertions.*;

import com.donohoedigital.games.poker.engine.Card;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ServerDeckTest {

    @Test
    void newDeck_has52Cards() {
        ServerDeck deck = new ServerDeck(false);
        assertEquals(52, deck.size());
        assertFalse(deck.isEmpty());
    }

    @Test
    void unshuffledDeck_dealsInOrder() {
        ServerDeck deck = new ServerDeck(false);
        // First card should be Spades 2 (first card added in initializeDeck)
        assertEquals(Card.SPADES_2, deck.nextCard());
        assertEquals(51, deck.size());
    }

    @Test
    void dealAllCards_produces52UniqueCards() {
        ServerDeck deck = new ServerDeck();
        Set<Card> dealt = new HashSet<>();
        for (int i = 0; i < 52; i++) {
            dealt.add(deck.nextCard());
        }
        assertEquals(52, dealt.size());
        assertTrue(deck.isEmpty());
        assertEquals(0, deck.size());
    }

    @Test
    void dealFromEmptyDeck_throwsException() {
        ServerDeck deck = new ServerDeck();
        for (int i = 0; i < 52; i++) {
            deck.nextCard();
        }
        assertThrows(IllegalStateException.class, deck::nextCard);
    }

    @Test
    void shuffleDeck_resetsNextCardIndex() {
        ServerDeck deck = new ServerDeck();
        deck.nextCard();
        deck.nextCard();
        assertEquals(50, deck.size());

        deck.shuffle();
        assertEquals(52, deck.size());
    }

    @Test
    void orderedCardsDeck_dealsInProvidedOrder() {
        List<Card> ordered = List.of(Card.HEARTS_A, Card.DIAMONDS_K, Card.SPADES_Q);
        ServerDeck deck = new ServerDeck(ordered);

        assertEquals(3, deck.size());
        assertEquals(Card.HEARTS_A, deck.nextCard());
        assertEquals(Card.DIAMONDS_K, deck.nextCard());
        assertEquals(Card.SPADES_Q, deck.nextCard());
        assertTrue(deck.isEmpty());
    }

    @Test
    void seededDeck_producesReproducibleOrder() {
        ServerDeck deck1 = new ServerDeck(42L);
        ServerDeck deck2 = new ServerDeck(42L);

        for (int i = 0; i < 52; i++) {
            assertEquals(deck1.nextCard(), deck2.nextCard(), "Card at position " + i + " should match");
        }
    }

    @Test
    void seededDeck_differentSeedsProduceDifferentOrders() {
        ServerDeck deck1 = new ServerDeck(1L);
        ServerDeck deck2 = new ServerDeck(2L);

        boolean anyDifferent = false;
        for (int i = 0; i < 52; i++) {
            if (!deck1.nextCard().equals(deck2.nextCard())) {
                anyDifferent = true;
                break;
            }
        }
        assertTrue(anyDifferent, "Different seeds should produce different card orders");
    }

    @Test
    void shuffledDeck_isNotInOriginalOrder() {
        ServerDeck unshuffled = new ServerDeck(false);
        ServerDeck shuffled = new ServerDeck(true);

        boolean anyDifferent = false;
        for (int i = 0; i < 52; i++) {
            if (!unshuffled.nextCard().equals(shuffled.nextCard())) {
                anyDifferent = true;
                break;
            }
        }
        assertTrue(anyDifferent, "Shuffled deck should differ from unshuffled");
    }
}
