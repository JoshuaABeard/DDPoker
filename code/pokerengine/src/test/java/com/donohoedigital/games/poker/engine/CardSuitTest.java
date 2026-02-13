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

import static org.junit.Assert.*;

/**
 * Tests for CardSuit enum-like class.
 */
public class CardSuitTest {

    @BeforeClass
    public static void setupConfig() {
        ConfigTestHelper.initializeForTesting("poker");
    }

    // ===== Constants Tests =====

    @Test
    public void testSuitConstants() {
        assertNotNull(CardSuit.CLUBS);
        assertNotNull(CardSuit.DIAMONDS);
        assertNotNull(CardSuit.HEARTS);
        assertNotNull(CardSuit.SPADES);
        assertNotNull(CardSuit.UNKNOWN);
    }

    @Test
    public void testNumSuits() {
        assertEquals(4, CardSuit.NUM_SUITS);
    }

    @Test
    public void testRankConstants() {
        assertEquals(0, CardSuit.CLUBS_RANK);
        assertEquals(1, CardSuit.DIAMONDS_RANK);
        assertEquals(2, CardSuit.HEARTS_RANK);
        assertEquals(3, CardSuit.SPADES_RANK);
        assertEquals(-1, CardSuit.UNKNOWN_RANK);
    }

    // ===== getRank() Tests =====

    @Test
    public void testGetRank() {
        assertEquals(CardSuit.CLUBS_RANK, CardSuit.CLUBS.getRank());
        assertEquals(CardSuit.DIAMONDS_RANK, CardSuit.DIAMONDS.getRank());
        assertEquals(CardSuit.HEARTS_RANK, CardSuit.HEARTS.getRank());
        assertEquals(CardSuit.SPADES_RANK, CardSuit.SPADES.getRank());
        assertEquals(CardSuit.UNKNOWN_RANK, CardSuit.UNKNOWN.getRank());
    }

    // ===== getName() Tests =====

    @Test
    public void testGetName() {
        assertEquals("club", CardSuit.CLUBS.getName());
        assertEquals("diamond", CardSuit.DIAMONDS.getName());
        assertEquals("heart", CardSuit.HEARTS.getName());
        assertEquals("spade", CardSuit.SPADES.getName());
        assertEquals("unknown", CardSuit.UNKNOWN.getName());
    }

    // ===== getAbbr() Tests =====

    @Test
    public void testGetAbbr() {
        // getAbbr() returns localized abbreviation from PropertyConfig
        assertNotNull(CardSuit.CLUBS.getAbbr());
        assertNotNull(CardSuit.DIAMONDS.getAbbr());
        assertNotNull(CardSuit.HEARTS.getAbbr());
        assertNotNull(CardSuit.SPADES.getAbbr());
        assertNotNull(CardSuit.UNKNOWN.getAbbr());
    }

    @Test
    public void testGetAbbrCaching() {
        // First call loads from PropertyConfig
        String abbr1 = CardSuit.CLUBS.getAbbr();
        // Second call should return cached value
        String abbr2 = CardSuit.CLUBS.getAbbr();
        assertSame(abbr1, abbr2); // Same instance (cached)
    }

    // ===== forRank() Tests =====

    @Test
    public void testForRank() {
        assertSame(CardSuit.CLUBS, CardSuit.forRank(CardSuit.CLUBS_RANK));
        assertSame(CardSuit.DIAMONDS, CardSuit.forRank(CardSuit.DIAMONDS_RANK));
        assertSame(CardSuit.HEARTS, CardSuit.forRank(CardSuit.HEARTS_RANK));
        assertSame(CardSuit.SPADES, CardSuit.forRank(CardSuit.SPADES_RANK));
        assertSame(CardSuit.UNKNOWN, CardSuit.forRank(CardSuit.UNKNOWN_RANK));
    }

    @Test
    public void testForRankInvalid() {
        assertNull(CardSuit.forRank(999));
        assertNull(CardSuit.forRank(-99));
        assertNull(CardSuit.forRank(5));
    }

    // ===== compareTo() Tests =====

    @Test
    public void testCompareTo() {
        // Based on bridge rank: Clubs < Diamonds < Hearts < Spades
        assertTrue(CardSuit.CLUBS.compareTo(CardSuit.DIAMONDS) < 0);
        assertTrue(CardSuit.DIAMONDS.compareTo(CardSuit.HEARTS) < 0);
        assertTrue(CardSuit.HEARTS.compareTo(CardSuit.SPADES) < 0);

        assertTrue(CardSuit.SPADES.compareTo(CardSuit.HEARTS) > 0);
        assertTrue(CardSuit.HEARTS.compareTo(CardSuit.DIAMONDS) > 0);
        assertTrue(CardSuit.DIAMONDS.compareTo(CardSuit.CLUBS) > 0);
    }

    @Test
    public void testCompareToSame() {
        assertEquals(0, CardSuit.CLUBS.compareTo(CardSuit.CLUBS));
        assertEquals(0, CardSuit.DIAMONDS.compareTo(CardSuit.DIAMONDS));
        assertEquals(0, CardSuit.HEARTS.compareTo(CardSuit.HEARTS));
        assertEquals(0, CardSuit.SPADES.compareTo(CardSuit.SPADES));
    }

    @Test
    public void testCompareToUnknown() {
        // UNKNOWN has rank -1, so it's less than all valid suits
        assertTrue(CardSuit.UNKNOWN.compareTo(CardSuit.CLUBS) < 0);
        assertTrue(CardSuit.UNKNOWN.compareTo(CardSuit.DIAMONDS) < 0);
        assertTrue(CardSuit.UNKNOWN.compareTo(CardSuit.HEARTS) < 0);
        assertTrue(CardSuit.UNKNOWN.compareTo(CardSuit.SPADES) < 0);

        assertTrue(CardSuit.CLUBS.compareTo(CardSuit.UNKNOWN) > 0);
    }

    // ===== Singleton Pattern Tests =====

    @Test
    public void testSingleton() {
        // forRank should return the same instance
        assertSame(CardSuit.CLUBS, CardSuit.forRank(0));
        assertSame(CardSuit.DIAMONDS, CardSuit.forRank(1));
        assertSame(CardSuit.HEARTS, CardSuit.forRank(2));
        assertSame(CardSuit.SPADES, CardSuit.forRank(3));
    }

    // ===== Edge Cases =====

    @Test
    public void testAllSuitsHaveUniqueRanks() {
        int[] ranks = {CardSuit.CLUBS.getRank(), CardSuit.DIAMONDS.getRank(), CardSuit.HEARTS.getRank(),
                CardSuit.SPADES.getRank()};

        // Check all ranks are unique
        for (int i = 0; i < ranks.length; i++) {
            for (int j = i + 1; j < ranks.length; j++) {
                assertNotEquals("Ranks should be unique", ranks[i], ranks[j]);
            }
        }
    }

    @Test
    public void testAllSuitsHaveUniqueNames() {
        String[] names = {CardSuit.CLUBS.getName(), CardSuit.DIAMONDS.getName(), CardSuit.HEARTS.getName(),
                CardSuit.SPADES.getName()};

        // Check all names are unique
        for (int i = 0; i < names.length; i++) {
            for (int j = i + 1; j < names.length; j++) {
                assertNotEquals("Names should be unique", names[i], names[j]);
            }
        }
    }
}
