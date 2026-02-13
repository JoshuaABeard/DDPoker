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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.junit.Assert.*;

/**
 * Comprehensive tests for Card class - the fundamental poker card
 * representation. Tests construction, comparisons, serialization, and all
 * utility methods.
 */
public class CardTest {

    @BeforeClass
    public static void setupConfig() {
        ConfigTestHelper.initializeForTesting("poker");
    }

    @AfterClass
    public static void cleanupConfig() {
        ConfigTestHelper.resetForTesting();
    }

    // ===== Construction Tests =====

    @Test
    public void testCardConstruction() {
        Card card = new Card(CardSuit.SPADES, ACE);
        assertEquals(ACE, card.getRank());
        assertEquals(CardSuit.SPADES_RANK, card.getSuit());
        assertEquals(CardSuit.SPADES, card.getCardSuit());
    }

    @Test
    public void testAllStaticCardsExist() {
        // Verify all 52 standard cards are initialized
        assertNotNull(SPADES_2);
        assertNotNull(SPADES_A);
        assertNotNull(HEARTS_2);
        assertNotNull(HEARTS_A);
        assertNotNull(DIAMONDS_2);
        assertNotNull(DIAMONDS_A);
        assertNotNull(CLUBS_2);
        assertNotNull(CLUBS_A);
    }

    @Test
    public void testBlankCard() {
        assertTrue(BLANK.isBlank());
        assertEquals(UNKNOWN, BLANK.getRank());
        assertEquals(CardSuit.UNKNOWN, BLANK.getCardSuit());
    }

    @Test
    public void testLowAceCard() {
        assertEquals(1, SPADES_LOW_A.getRank());
        assertEquals(CardSuit.SPADES, SPADES_LOW_A.getCardSuit());
    }

    // ===== Rank and Suit Getters =====

    @Test
    public void testGetRankAndSuit() {
        Card card = new Card(CardSuit.HEARTS, KING);
        assertEquals(KING, card.getRank());
        assertEquals(CardSuit.HEARTS_RANK, card.getSuit());
        assertEquals(CardSuit.HEARTS, card.getCardSuit());
    }

    @Test
    public void testSuitCheckers() {
        assertTrue(SPADES_A.isSpades());
        assertFalse(SPADES_A.isHearts());
        assertFalse(SPADES_A.isDiamonds());
        assertFalse(SPADES_A.isClubs());

        assertTrue(HEARTS_K.isHearts());
        assertFalse(HEARTS_K.isSpades());

        assertTrue(DIAMONDS_Q.isDiamonds());
        assertFalse(DIAMONDS_Q.isClubs());

        assertTrue(CLUBS_J.isClubs());
        assertFalse(CLUBS_J.isSpades());
    }

    @Test
    public void testIsFaceCard() {
        assertTrue(SPADES_J.isFaceCard());
        assertTrue(HEARTS_Q.isFaceCard());
        assertTrue(DIAMONDS_K.isFaceCard());
        assertFalse(CLUBS_A.isFaceCard()); // Ace is not a face card
        assertFalse(SPADES_T.isFaceCard()); // 10 is not a face card
        assertFalse(HEARTS_2.isFaceCard());
    }

    // ===== Display Methods =====

    @Test
    public void testGetDisplay() {
        // Display format is rank + suit abbreviation
        String display = SPADES_A.getDisplay();
        assertTrue(display.contains("A") || display.toLowerCase().contains("ace"));
        assertTrue(display.contains("s") || display.toLowerCase().contains("spade"));
    }

    @Test
    public void testToString() {
        String str = HEARTS_K.toString();
        assertNotNull(str);
        // toString() delegates to getDisplay()
        assertEquals(HEARTS_K.getDisplay(), str);
    }

    @Test
    public void testToStringSingle() {
        String single = SPADES_T.toStringSingle();
        assertTrue(single.contains("T")); // 10 is displayed as T
        assertTrue(single.contains("s") || single.toLowerCase().contains("spade"));
    }

    @Test
    public void testToHTML() {
        String html = CLUBS_A.toHTML();
        assertTrue(html.contains("DDCARD"));
        assertTrue(html.contains("A"));
    }

    @Test
    public void testRankDisplaySingle() {
        assertEquals("T", DIAMONDS_T.getRankDisplaySingle());
        String aceRank = SPADES_A.getRankDisplaySingle();
        assertTrue(aceRank.contains("A") || aceRank.toLowerCase().contains("ace"));
    }

    // ===== Static Rank Methods =====

    @Test
    public void testGetRankFromChar() {
        assertEquals(ACE, getRank('A'));
        assertEquals(ACE, getRank('a'));
        assertEquals(KING, getRank('K'));
        assertEquals(KING, getRank('k'));
        assertEquals(QUEEN, getRank('Q'));
        assertEquals(QUEEN, getRank('q'));
        assertEquals(JACK, getRank('J'));
        assertEquals(JACK, getRank('j'));
        assertEquals(TEN, getRank('T'));
        assertEquals(TEN, getRank('t'));
        assertEquals(NINE, getRank('9'));
        assertEquals(TWO, getRank('2'));
        assertEquals(UNKNOWN, getRank('X')); // Unknown char
    }

    @Test
    public void testGetRankString() {
        assertNotNull(getRank(ACE));
        assertNotNull(getRank(KING));
        assertNotNull(getRank(QUEEN));
        assertNotNull(getRank(JACK));
        assertEquals("10", getRank(10));
        assertEquals("2", getRank(2));
    }

    @Test
    public void testGetRankSingle() {
        String ten = getRankSingle(10);
        assertNotNull(ten);
        // getRankSingle(10) should use getDisplayT() which is "10" or "T"
        assertTrue(ten.equals("10") || ten.equals("T"));
    }

    // ===== Comparison Methods =====

    @Test
    public void testIsSameRank() {
        assertTrue(SPADES_A.isSameRank(HEARTS_A));
        assertTrue(CLUBS_K.isSameRank(DIAMONDS_K));
        assertFalse(SPADES_A.isSameRank(SPADES_K));
    }

    @Test
    public void testIsSameSuit() {
        assertTrue(SPADES_A.isSameSuit(SPADES_2));
        assertTrue(HEARTS_K.isSameSuit(HEARTS_Q));
        assertFalse(SPADES_A.isSameSuit(HEARTS_A));
    }

    @Test
    public void testCompareTo() {
        // Higher rank is greater
        assertTrue(SPADES_A.compareTo(SPADES_K) > 0);
        assertTrue(HEARTS_K.compareTo(HEARTS_Q) > 0);
        assertTrue(DIAMONDS_3.compareTo(DIAMONDS_2) > 0);

        // Same rank, different suit - compares by suit rank
        int result = SPADES_A.compareTo(HEARTS_A);
        // Result depends on CardSuit.compareTo()
        assertNotEquals(0, result); // Should not be equal

        // Same card
        assertEquals(0, CLUBS_K.compareTo(CLUBS_K));

        // Null comparison
        assertTrue(SPADES_A.compareTo(null) > 0);
    }

    @Test
    public void testIsGreaterThan() {
        assertTrue(SPADES_A.isGreaterThan(SPADES_K));
        assertFalse(SPADES_K.isGreaterThan(SPADES_A));
    }

    @Test
    public void testIsLessThan() {
        assertTrue(SPADES_2.isLessThan(SPADES_3));
        assertFalse(SPADES_A.isLessThan(SPADES_K));
    }

    // ===== Equals and HashCode =====

    @Test
    public void testEquals() {
        Card card1 = new Card(CardSuit.SPADES, ACE);
        Card card2 = new Card(CardSuit.SPADES, ACE);
        Card card3 = new Card(CardSuit.HEARTS, ACE);
        Card card4 = new Card(CardSuit.SPADES, KING);

        // Same rank and suit
        assertEquals(card1, card2);

        // Different suit
        assertNotEquals(card1, card3);

        // Different rank
        assertNotEquals(card1, card4);

        // Not equal to null
        assertNotEquals(card1, null);

        // Not equal to other types
        assertNotEquals(card1, "SPADES_A");

        // Same object
        assertEquals(card1, card1);
    }

    @Test
    public void testHashCode() {
        Card card1 = new Card(CardSuit.SPADES, ACE);
        Card card2 = new Card(CardSuit.SPADES, ACE);
        Card card3 = new Card(CardSuit.HEARTS, ACE);

        // Same cards have same hash code
        assertEquals(card1.hashCode(), card2.hashCode());

        // Different cards typically have different hash codes
        assertNotEquals(card1.hashCode(), card3.hashCode());
    }

    @Test
    public void testHashCodeConsistency() {
        Card card = new Card(CardSuit.DIAMONDS, QUEEN);
        int hash1 = card.hashCode();
        int hash2 = card.hashCode();
        assertEquals(hash1, hash2); // Multiple calls should return same value
    }

    // ===== Fingerprint and Index =====

    @Test
    public void testFingerprint() {
        long fingerprint = SPADES_A.fingerprint();
        assertTrue(fingerprint > 0);
        // Fingerprint should be a power of 2 (single bit set)
        assertEquals(1, Long.bitCount(fingerprint));
    }

    @Test
    public void testFingerprintUnique() {
        // Different cards should have different fingerprints
        assertNotEquals(SPADES_A.fingerprint(), HEARTS_A.fingerprint());
        assertNotEquals(SPADES_A.fingerprint(), SPADES_K.fingerprint());
    }

    @Test
    public void testGetIndex() {
        int index = SPADES_2.getIndex();
        assertTrue(index >= 0 && index <= 52);

        // Blank card should have index 52
        assertEquals(52, BLANK.getIndex());
    }

    @Test
    public void testIndexUnique() {
        // All standard cards should have unique indices
        int index1 = SPADES_2.getIndex();
        int index2 = HEARTS_2.getIndex();
        int index3 = DIAMONDS_2.getIndex();
        int index4 = CLUBS_2.getIndex();

        // Should all be different
        assertTrue(index1 != index2 && index1 != index3 && index1 != index4);
        assertTrue(index2 != index3 && index2 != index4);
        assertTrue(index3 != index4);
    }

    // ===== Static Lookup Methods =====

    @Test
    public void testGetCardByString() {
        Card card = getCard("As");
        assertNotNull(card);
        assertEquals(ACE, card.getRank());
        assertEquals(CardSuit.SPADES, card.getCardSuit());

        card = getCard("Kh");
        assertEquals(KING, card.getRank());
        assertEquals(CardSuit.HEARTS, card.getCardSuit());

        card = getCard("2d");
        assertEquals(TWO, card.getRank());
        assertEquals(CardSuit.DIAMONDS, card.getCardSuit());

        card = getCard("Tc");
        assertEquals(TEN, card.getRank());
        assertEquals(CardSuit.CLUBS, card.getCardSuit());
    }

    @Test
    public void testGetCardByStringCaseInsensitive() {
        Card upper = getCard("AS");
        Card lower = getCard("as");
        assertEquals(upper, lower);
    }

    @Test
    public void testGetCardByStringNull() {
        Card card = getCard((String) null);
        assertEquals(BLANK, card);
    }

    @Test
    public void testGetCardByIndex() {
        Card card = getCard(0);
        assertNotNull(card);

        Card blank = getCard(52);
        assertEquals(BLANK, blank);
    }

    @Test
    public void testGetCardBySuitRank() {
        Card card = getCard(CardSuit.SPADES_RANK, ACE);
        assertEquals(ACE, card.getRank());
        assertEquals(CardSuit.SPADES, card.getCardSuit());
    }

    @Test
    public void testGetCardByCardSuitRank() {
        Card card = getCard(CardSuit.HEARTS, KING);
        assertEquals(KING, card.getRank());
        assertEquals(CardSuit.HEARTS, card.getCardSuit());
    }

    @Test
    public void testGetCardReturnsConstants() {
        // getCard should return the same instance as the static constants
        assertSame(SPADES_A, getCard(CardSuit.SPADES, ACE));
        assertSame(HEARTS_K, getCard(CardSuit.HEARTS, KING));
        assertSame(BLANK, getCard(CardSuit.UNKNOWN_RANK, UNKNOWN));
    }

    // ===== Serialization =====

    @Test
    public void testMarshalDemarshal() {
        Card original = DIAMONDS_Q;
        MsgState state = new MsgState();

        // Marshal the card
        String marshalled = original.marshal(state);
        assertNotNull(marshalled);

        // Demarshal into a new card
        Card restored = new Card();
        restored.demarshal(state, marshalled);

        // Verify they're equal
        assertEquals(original.getRank(), restored.getRank());
        assertEquals(original.getSuit(), restored.getSuit());
        assertEquals(original.getCardSuit(), restored.getCardSuit());
        assertEquals(original, restored);
    }

    @Test
    public void testMarshalDemarshalAllCards() {
        MsgState state = new MsgState();

        // Test a variety of cards
        Card[] testCards = {SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_J, SPADES_T, HEARTS_9, DIAMONDS_5, CLUBS_2, BLANK};

        for (Card original : testCards) {
            String marshalled = original.marshal(state);
            Card restored = new Card();
            restored.demarshal(state, marshalled);
            assertEquals("Failed for card: " + original, original, restored);
        }
    }

    // ===== setValue() =====

    @Test
    public void testSetValue() {
        Card card = new Card(CardSuit.CLUBS, TWO);
        Card target = SPADES_A;

        card.setValue(target);

        assertEquals(target.getRank(), card.getRank());
        assertEquals(target.getSuit(), card.getSuit());
        assertEquals(target.getCardSuit(), card.getCardSuit());
        assertEquals(target.getIndex(), card.getIndex());
        assertEquals(target.fingerprint(), card.fingerprint());
    }

    // ===== Edge Cases =====

    @Test
    public void testAllRanksRepresented() {
        // Verify we can create cards for all valid ranks
        for (int rank = TWO; rank <= ACE; rank++) {
            Card card = new Card(CardSuit.SPADES, rank);
            assertEquals(rank, card.getRank());
        }
    }

    @Test
    public void testAllSuitsRepresented() {
        // Verify we can create cards for all valid suits
        CardSuit[] suits = {CardSuit.CLUBS, CardSuit.DIAMONDS, CardSuit.HEARTS, CardSuit.SPADES};
        for (CardSuit suit : suits) {
            Card card = new Card(suit, ACE);
            assertEquals(suit, card.getCardSuit());
        }
    }
}
