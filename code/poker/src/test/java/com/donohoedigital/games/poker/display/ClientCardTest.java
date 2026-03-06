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

class ClientCardTest {

    // Suit index constants matching pokerengine Card.CLUBS=0, Card.DIAMONDS=1,
    // Card.HEARTS=2, Card.SPADES=3
    private static final int CLUBS = 0;
    private static final int DIAMONDS = 1;
    private static final int HEARTS = 2;
    private static final int SPADES = 3;

    @Test
    void parse_aceOfHearts() {
        ClientCard card = ClientCard.parse("Ah");
        assertEquals(14, card.rank());
        assertEquals(HEARTS, card.suit());
        assertEquals("Ah", card.display());
        assertEquals('A', card.rankChar());
        assertEquals('h', card.suitChar());
    }

    @Test
    void parse_tenOfDiamonds() {
        ClientCard card = ClientCard.parse("Td");
        assertEquals(10, card.rank());
        assertEquals(DIAMONDS, card.suit());
        assertEquals("Td", card.display());
        assertEquals('T', card.rankChar());
        assertEquals('d', card.suitChar());
    }

    @Test
    void parse_twoOfClubs() {
        ClientCard card = ClientCard.parse("2c");
        assertEquals(2, card.rank());
        assertEquals(CLUBS, card.suit());
        assertEquals("2c", card.display());
        assertEquals('2', card.rankChar());
        assertEquals('c', card.suitChar());
    }

    @Test
    void parse_kingOfSpades() {
        ClientCard card = ClientCard.parse("Ks");
        assertEquals(13, card.rank());
        assertEquals(SPADES, card.suit());
        assertEquals("Ks", card.display());
        assertEquals('K', card.rankChar());
        assertEquals('s', card.suitChar());
    }

    @Test
    void parse_allRanks() {
        assertEquals(14, ClientCard.parse("As").rank());
        assertEquals(13, ClientCard.parse("Ks").rank());
        assertEquals(12, ClientCard.parse("Qs").rank());
        assertEquals(11, ClientCard.parse("Js").rank());
        assertEquals(10, ClientCard.parse("Ts").rank());
        assertEquals(9, ClientCard.parse("9s").rank());
        assertEquals(8, ClientCard.parse("8s").rank());
        assertEquals(7, ClientCard.parse("7s").rank());
        assertEquals(6, ClientCard.parse("6s").rank());
        assertEquals(5, ClientCard.parse("5s").rank());
        assertEquals(4, ClientCard.parse("4s").rank());
        assertEquals(3, ClientCard.parse("3s").rank());
        assertEquals(2, ClientCard.parse("2s").rank());
    }

    @Test
    void parse_allSuits() {
        assertEquals(SPADES, ClientCard.parse("As").suit());
        assertEquals(CLUBS, ClientCard.parse("Ac").suit());
        assertEquals(DIAMONDS, ClientCard.parse("Ad").suit());
        assertEquals(HEARTS, ClientCard.parse("Ah").suit());
    }

    @Test
    void parse_caseInsensitive() {
        ClientCard lower = ClientCard.parse("ah");
        ClientCard upper = ClientCard.parse("Ah");
        assertEquals(lower.rank(), upper.rank());
        assertEquals(lower.suit(), upper.suit());
    }

    @Test
    void parse_invalidThrows() {
        assertThrows(IllegalArgumentException.class, () -> ClientCard.parse("Xh"));
        assertThrows(IllegalArgumentException.class, () -> ClientCard.parse("Az"));
        assertThrows(IllegalArgumentException.class, () -> ClientCard.parse(""));
        assertThrows(IllegalArgumentException.class, () -> ClientCard.parse("A"));
    }

    @Test
    void parse_nullThrows() {
        assertThrows(IllegalArgumentException.class, () -> ClientCard.parse(null));
    }

    @Test
    void blank_constant() {
        ClientCard blank = ClientCard.BLANK;
        assertNotNull(blank);
        assertTrue(blank.isBlank());
        assertEquals(0, blank.rank());
    }

    @Test
    void nonBlank_isBlankFalse() {
        assertFalse(ClientCard.parse("Ah").isBlank());
    }

    @Test
    void rankDisplay_faceCards() {
        assertEquals("Ace", ClientCard.parse("Ah").rankDisplay());
        assertEquals("King", ClientCard.parse("Kh").rankDisplay());
        assertEquals("Queen", ClientCard.parse("Qh").rankDisplay());
        assertEquals("Jack", ClientCard.parse("Jh").rankDisplay());
        assertEquals("10", ClientCard.parse("Th").rankDisplay());
    }

    @Test
    void rankDisplay_numericCards() {
        assertEquals("9", ClientCard.parse("9h").rankDisplay());
        assertEquals("2", ClientCard.parse("2h").rankDisplay());
    }

    @Test
    void suitDisplay() {
        assertEquals("Spades", ClientCard.parse("As").suitDisplay());
        assertEquals("Clubs", ClientCard.parse("Ac").suitDisplay());
        assertEquals("Diamonds", ClientCard.parse("Ad").suitDisplay());
        assertEquals("Hearts", ClientCard.parse("Ah").suitDisplay());
    }

    @Test
    void parseAll() {
        List<ClientCard> cards = ClientCard.parseAll(List.of("Ah", "Ks", "2c"));
        assertEquals(3, cards.size());
        assertEquals(14, cards.get(0).rank());
        assertEquals(13, cards.get(1).rank());
        assertEquals(2, cards.get(2).rank());
    }

    @Test
    void parseAll_emptyList() {
        List<ClientCard> cards = ClientCard.parseAll(List.of());
        assertTrue(cards.isEmpty());
    }

    @Test
    void equality_sameRankAndSuit() {
        ClientCard a = ClientCard.parse("Ah");
        ClientCard b = ClientCard.parse("Ah");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equality_differentCards() {
        assertNotEquals(ClientCard.parse("Ah"), ClientCard.parse("As"));
        assertNotEquals(ClientCard.parse("Ah"), ClientCard.parse("Kh"));
    }

    @Test
    void toString_returnsDisplay() {
        assertEquals("Ah", ClientCard.parse("Ah").toString());
    }
}
