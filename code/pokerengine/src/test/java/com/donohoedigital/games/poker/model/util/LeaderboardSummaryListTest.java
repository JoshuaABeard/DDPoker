/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
package com.donohoedigital.games.poker.model.util;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.donohoedigital.games.poker.model.LeaderboardSummary;

/**
 * Tests for LeaderboardSummaryList.
 *
 * This class extends PagedList with no additional functionality, so tests focus
 * on verifying it works as a typed list.
 */
public class LeaderboardSummaryListTest {

    private LeaderboardSummaryList list;

    @Before
    public void setUp() {
        list = new LeaderboardSummaryList();
    }

    @Test
    public void testDefaultConstructor() {
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testAddSummary() {
        LeaderboardSummary summary = new LeaderboardSummary();
        summary.setPlayerName("Player 1");
        list.add(summary);

        assertEquals(1, list.size());
        assertEquals("Player 1", list.get(0).getPlayerName());
    }

    @Test
    public void testAddMultipleSummaries() {
        LeaderboardSummary summary1 = new LeaderboardSummary();
        summary1.setPlayerName("Player 1");
        LeaderboardSummary summary2 = new LeaderboardSummary();
        summary2.setPlayerName("Player 2");

        list.add(summary1);
        list.add(summary2);

        assertEquals(2, list.size());
        assertEquals("Player 1", list.get(0).getPlayerName());
        assertEquals("Player 2", list.get(1).getPlayerName());
    }

    @Test
    public void testRemoveSummary() {
        LeaderboardSummary summary = new LeaderboardSummary();
        summary.setPlayerName("Player 1");
        list.add(summary);

        assertEquals(1, list.size());

        list.remove(0);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testClearList() {
        for (int i = 0; i < 5; i++) {
            LeaderboardSummary summary = new LeaderboardSummary();
            summary.setPlayerName("Player " + i);
            list.add(summary);
        }

        assertEquals(5, list.size());

        list.clear();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testIterateOverList() {
        for (int i = 0; i < 3; i++) {
            LeaderboardSummary summary = new LeaderboardSummary();
            summary.setPlayerName("Player " + i);
            list.add(summary);
        }

        int count = 0;
        for (LeaderboardSummary summary : list) {
            assertNotNull(summary);
            count++;
        }

        assertEquals(3, count);
    }

    @Test
    public void testContainsSummary() {
        LeaderboardSummary summary = new LeaderboardSummary();
        summary.setPlayerName("Player 1");

        assertFalse(list.contains(summary));

        list.add(summary);

        assertTrue(list.contains(summary));
    }
}
