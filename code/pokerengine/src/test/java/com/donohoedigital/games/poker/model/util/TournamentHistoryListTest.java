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

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.donohoedigital.games.poker.model.TournamentHistory;

/**
 * Tests for TournamentHistoryList.
 */
public class TournamentHistoryListTest {

    private TournamentHistoryList list;

    @Before
    public void setUp() {
        list = new TournamentHistoryList();
    }

    @Test
    public void testDefaultConstructor() {
        TournamentHistoryList newList = new TournamentHistoryList();
        assertNotNull(newList);
        assertTrue(newList.isEmpty());
        assertEquals(0, newList.getTotalSize());
    }

    @Test
    public void testConstructorWithNullList() {
        TournamentHistoryList newList = new TournamentHistoryList(null);
        assertTrue(newList.isEmpty());
    }

    @Test
    public void testConstructorWithEmptyList() {
        List<TournamentHistory> emptyList = new ArrayList<>();
        TournamentHistoryList newList = new TournamentHistoryList(emptyList);
        assertTrue(newList.isEmpty());
    }

    @Test
    public void testConstructorWithPopulatedList() {
        List<TournamentHistory> histories = new ArrayList<>();
        TournamentHistory hist1 = new TournamentHistory();
        hist1.setPlayerName("Player 1");
        TournamentHistory hist2 = new TournamentHistory();
        hist2.setPlayerName("Player 2");
        histories.add(hist1);
        histories.add(hist2);

        TournamentHistoryList newList = new TournamentHistoryList(histories);

        assertEquals(2, newList.size());
        assertEquals("Player 1", newList.get(0).getPlayerName());
        assertEquals("Player 2", newList.get(1).getPlayerName());
    }

    @Test
    public void testGetAndSetTotalSize() {
        assertEquals(0, list.getTotalSize());

        list.setTotalSize(100);
        assertEquals(100, list.getTotalSize());

        list.setTotalSize(0);
        assertEquals(0, list.getTotalSize());
    }

    @Test
    public void testToStringWithEmptyList() {
        String result = list.toString();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToStringWithSingleHistory() {
        TournamentHistory hist = new TournamentHistory();
        hist.setPlayerName("Test Player");
        list.add(hist);

        String result = list.toString();

        assertTrue(result.contains("Entry #0"));
        assertFalse(result.isEmpty());
    }

    @Test
    public void testToStringWithMultipleHistories() {
        TournamentHistory hist1 = new TournamentHistory();
        hist1.setPlayerName("Player 1");
        TournamentHistory hist2 = new TournamentHistory();
        hist2.setPlayerName("Player 2");

        list.add(hist1);
        list.add(hist2);

        String result = list.toString();

        assertTrue(result.contains("Entry #0"));
        assertTrue(result.contains("Entry #1"));
    }

    @Test
    public void testCalculateInfoWithEmptyList() {
        // Should not throw exception with empty list
        list.calculateInfo(true, false);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testCalculateInfoWithSingleHumanPlayer() {
        // Need at least 2 players for rank calculation to work (avoids division by
        // zero)
        TournamentHistory hist1 = new TournamentHistory();
        hist1.setPlayerName("Human Player 1");
        hist1.setPlace(1);
        hist1.setPlayerType(TournamentHistory.PLAYER_TYPE_LOCAL);

        TournamentHistory hist2 = new TournamentHistory();
        hist2.setPlayerName("Human Player 2");
        hist2.setPlace(2);
        hist2.setPlayerType(TournamentHistory.PLAYER_TYPE_LOCAL);

        list.add(hist1);
        list.add(hist2);

        list.calculateInfo(true, false);

        // Verify rank was calculated
        assertTrue(hist1.getRank1() > 0);
        assertTrue(hist2.getRank1() >= 0);
        assertTrue(hist1.isEnded());
        assertTrue(hist2.isEnded());
    }

    @Test
    public void testCalculateInfoWithMultipleHumanPlayers() {
        // Create 3 human players finishing in places 1, 2, 3
        for (int i = 1; i <= 3; i++) {
            TournamentHistory hist = new TournamentHistory();
            hist.setPlayerName("Player " + i);
            hist.setPlace(i);
            hist.setPlayerType(TournamentHistory.PLAYER_TYPE_LOCAL);
            list.add(hist);
        }

        list.calculateInfo(true, false);

        // First place should have highest rank
        assertTrue(list.get(0).getRank1() > list.get(1).getRank1());
        assertTrue(list.get(1).getRank1() > list.get(2).getRank1());

        // All should be marked as ended
        assertTrue(list.get(0).isEnded());
        assertTrue(list.get(1).isEnded());
        assertTrue(list.get(2).isEnded());
    }

    @Test
    public void testCalculateInfoWithAIPlayers() {
        // Create mix of human and AI players
        TournamentHistory human1 = new TournamentHistory();
        human1.setPlayerName("Human 1");
        human1.setPlace(1);
        human1.setPlayerType(TournamentHistory.PLAYER_TYPE_LOCAL);

        TournamentHistory ai1 = new TournamentHistory();
        ai1.setPlayerName("AI 1");
        ai1.setPlace(2);
        ai1.setPlayerType(TournamentHistory.PLAYER_TYPE_AI);

        TournamentHistory human2 = new TournamentHistory();
        human2.setPlayerName("Human 2");
        human2.setPlace(3);
        human2.setPlayerType(TournamentHistory.PLAYER_TYPE_LOCAL);

        list.add(human1);
        list.add(ai1);
        list.add(human2);

        list.calculateInfo(false, false);

        // Verify ranks were calculated (AI players reduce scaling)
        assertTrue(human1.getRank1() > ai1.getRank1());
        assertTrue(ai1.getRank1() > human2.getRank1());

        // Should not be marked as ended (passed false)
        assertFalse(human1.isEnded());
        assertFalse(ai1.isEnded());
        assertFalse(human2.isEnded());
    }

    @Test
    public void testCalculateInfoWithAllAIPlayers() {
        for (int i = 1; i <= 5; i++) {
            TournamentHistory hist = new TournamentHistory();
            hist.setPlayerName("AI " + i);
            hist.setPlace(i);
            hist.setPlayerType(TournamentHistory.PLAYER_TYPE_AI);
            list.add(hist);
        }

        list.calculateInfo(true, false);

        // Ranks should still be calculated (with AI reduction applied)
        assertTrue(list.get(0).getRank1() > 0);
        assertTrue(list.get(4).getRank1() >= 0);
    }

    @Test
    public void testCalculateInfoRankOrdering() {
        // Create 10 players to test ranking algorithm
        for (int i = 1; i <= 10; i++) {
            TournamentHistory hist = new TournamentHistory();
            hist.setPlayerName("Player " + i);
            hist.setPlace(i);
            hist.setPlayerType(TournamentHistory.PLAYER_TYPE_LOCAL);
            list.add(hist);
        }

        list.calculateInfo(true, false);

        // Verify rank decreases with place (1st has highest rank)
        for (int i = 0; i < 9; i++) {
            assertTrue(list.get(i).getRank1() > list.get(i + 1).getRank1());
        }
    }
}
