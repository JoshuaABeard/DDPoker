/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.display;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClientHandTest {

    @Test
    void fromStrings_createsHand() {
        ClientHand hand = ClientHand.fromStrings(List.of("Ah", "Ks"));
        assertEquals(2, hand.size());
        assertEquals(ClientCard.parse("Ah"), hand.getCard(0));
        assertEquals(ClientCard.parse("Ks"), hand.getCard(1));
    }

    @Test
    void fromCards_createsHand() {
        List<ClientCard> cards = List.of(ClientCard.parse("Ah"), ClientCard.parse("Ks"));
        ClientHand hand = ClientHand.fromCards(cards);
        assertEquals(2, hand.size());
        assertEquals(cards.get(0), hand.getCard(0));
    }

    @Test
    void empty_createsEmptyHand() {
        ClientHand hand = ClientHand.empty();
        assertEquals(0, hand.size());
        assertTrue(hand.isEmpty());
    }

    @Test
    void addCard_increasesSize() {
        ClientHand hand = ClientHand.empty();
        hand.addCard(ClientCard.parse("Ah"));
        assertEquals(1, hand.size());
        assertFalse(hand.isEmpty());
    }

    @Test
    void clear_removesAllCards() {
        ClientHand hand = ClientHand.fromStrings(List.of("Ah", "Ks"));
        hand.clear();
        assertEquals(0, hand.size());
        assertTrue(hand.isEmpty());
    }

    @Test
    void getCards_returnsUnmodifiableView() {
        ClientHand hand = ClientHand.fromStrings(List.of("Ah", "Ks"));
        List<ClientCard> cards = hand.getCards();
        assertEquals(2, cards.size());
        assertThrows(UnsupportedOperationException.class, () -> cards.add(ClientCard.parse("2c")));
    }

    @Test
    void sorted_returnsByRankDescending() {
        ClientHand hand = ClientHand.fromStrings(List.of("2c", "Ah", "Ks", "5d"));
        ClientHand sorted = hand.sorted();

        // Should be A, K, 5, 2 (descending by rank)
        assertEquals(14, sorted.getCard(0).rank());
        assertEquals(13, sorted.getCard(1).rank());
        assertEquals(5, sorted.getCard(2).rank());
        assertEquals(2, sorted.getCard(3).rank());

        // Original should be unchanged
        assertEquals(2, hand.getCard(0).rank());
    }

    @Test
    void sorted_emptyHand() {
        ClientHand sorted = ClientHand.empty().sorted();
        assertTrue(sorted.isEmpty());
    }

    @Test
    void containsCard_present() {
        ClientHand hand = ClientHand.fromStrings(List.of("Ah", "Ks"));
        assertTrue(hand.containsCard(ClientCard.parse("Ah")));
        assertTrue(hand.containsCard(ClientCard.parse("Ks")));
    }

    @Test
    void containsCard_absent() {
        ClientHand hand = ClientHand.fromStrings(List.of("Ah", "Ks"));
        assertFalse(hand.containsCard(ClientCard.parse("2c")));
    }

    @Test
    void containsCard_sameRankDifferentSuit() {
        ClientHand hand = ClientHand.fromStrings(List.of("Ah"));
        assertFalse(hand.containsCard(ClientCard.parse("As")));
    }

    @Test
    void getCard_outOfBoundsThrows() {
        ClientHand hand = ClientHand.fromStrings(List.of("Ah"));
        assertThrows(IndexOutOfBoundsException.class, () -> hand.getCard(1));
    }
}
