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
package com.donohoedigital.games.poker.model;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for LeaderboardSummary.
 */
public class LeaderboardSummaryTest {

    private LeaderboardSummary summary;

    @Before
    public void setUp() {
        summary = new LeaderboardSummary();
    }

    @Test
    public void testDefaultConstructor() {
        assertNotNull(summary);
        assertEquals(0, summary.getRank());
        assertEquals(0, summary.getPercentile());
        assertEquals(0, summary.getGamesPlayed());
        assertEquals(0L, summary.getProfileId());
        assertNull(summary.getPlayerName());
        assertEquals(0, summary.getDdr1());
        assertEquals(0, summary.getTotalBuyin());
        assertEquals(0, summary.getTotalAddon());
        assertEquals(0, summary.getTotalRebuys());
        assertEquals(0, summary.getTotalPrizes());
    }

    @Test
    public void testSetRank() {
        summary.setRank(5);
        assertEquals(5, summary.getRank());
    }

    @Test
    public void testSetPercentile() {
        summary.setPercentile(75);
        assertEquals(75, summary.getPercentile());
    }

    @Test
    public void testSetGamesPlayed() {
        summary.setGamesPlayed(100);
        assertEquals(100, summary.getGamesPlayed());
    }

    @Test
    public void testSetProfileId() {
        summary.setProfileId(12345L);
        assertEquals(12345L, summary.getProfileId());
    }

    @Test
    public void testSetPlayerName() {
        summary.setPlayerName("TestPlayer");
        assertEquals("TestPlayer", summary.getPlayerName());
    }

    @Test
    public void testSetDdr1() {
        summary.setDdr1(500);
        assertEquals(500, summary.getDdr1());
    }

    @Test
    public void testSetTotalBuyin() {
        summary.setTotalBuyin(1000);
        assertEquals(1000, summary.getTotalBuyin());
    }

    @Test
    public void testSetTotalAddon() {
        summary.setTotalAddon(200);
        assertEquals(200, summary.getTotalAddon());
    }

    @Test
    public void testSetTotalRebuys() {
        summary.setTotalRebuys(300);
        assertEquals(300, summary.getTotalRebuys());
    }

    @Test
    public void testSetTotalPrizes() {
        summary.setTotalPrizes(2000);
        assertEquals(2000, summary.getTotalPrizes());
    }

    @Test
    public void testGetTotalSpent() {
        summary.setTotalBuyin(1000);
        summary.setTotalRebuys(300);
        summary.setTotalAddon(200);

        assertEquals(1500, summary.getTotalSpent());
    }

    @Test
    public void testGetTotalSpentWithZeros() {
        assertEquals(0, summary.getTotalSpent());
    }

    @Test
    public void testGetNet() {
        summary.setTotalPrizes(2000);
        summary.setTotalBuyin(1000);
        summary.setTotalRebuys(300);
        summary.setTotalAddon(200);

        assertEquals(500, summary.getNet());
    }

    @Test
    public void testGetNetNegative() {
        summary.setTotalPrizes(500);
        summary.setTotalBuyin(1000);
        summary.setTotalRebuys(300);
        summary.setTotalAddon(200);

        assertEquals(-1000, summary.getNet());
    }

    @Test
    public void testGetNetZero() {
        summary.setTotalPrizes(1500);
        summary.setTotalBuyin(1000);
        summary.setTotalRebuys(300);
        summary.setTotalAddon(200);

        assertEquals(0, summary.getNet());
    }

    @Test
    public void testGetRoi() {
        summary.setTotalPrizes(2000);
        summary.setTotalBuyin(1000);
        summary.setTotalRebuys(300);
        summary.setTotalAddon(200);

        double roi = summary.getRoi();
        assertEquals(0.3333, roi, 0.0001);
    }

    @Test
    public void testGetRoiNegative() {
        summary.setTotalPrizes(500);
        summary.setTotalBuyin(1000);
        summary.setTotalRebuys(300);
        summary.setTotalAddon(200);

        double roi = summary.getRoi();
        assertEquals(-0.6667, roi, 0.0001);
    }

    @Test
    public void testGetRoiZero() {
        summary.setTotalPrizes(1500);
        summary.setTotalBuyin(1000);
        summary.setTotalRebuys(300);
        summary.setTotalAddon(200);

        double roi = summary.getRoi();
        assertEquals(0.0, roi, 0.0001);
    }

    @Test
    public void testFullyPopulatedSummary() {
        summary.setRank(1);
        summary.setPercentile(99);
        summary.setGamesPlayed(150);
        summary.setProfileId(54321L);
        summary.setPlayerName("Champion");
        summary.setDdr1(1000);
        summary.setTotalBuyin(5000);
        summary.setTotalAddon(1000);
        summary.setTotalRebuys(2000);
        summary.setTotalPrizes(12000);

        assertEquals(1, summary.getRank());
        assertEquals(99, summary.getPercentile());
        assertEquals(150, summary.getGamesPlayed());
        assertEquals(54321L, summary.getProfileId());
        assertEquals("Champion", summary.getPlayerName());
        assertEquals(1000, summary.getDdr1());
        assertEquals(5000, summary.getTotalBuyin());
        assertEquals(1000, summary.getTotalAddon());
        assertEquals(2000, summary.getTotalRebuys());
        assertEquals(12000, summary.getTotalPrizes());
        assertEquals(8000, summary.getTotalSpent());
        assertEquals(4000, summary.getNet());
        assertEquals(0.5, summary.getRoi(), 0.0001);
    }
}
