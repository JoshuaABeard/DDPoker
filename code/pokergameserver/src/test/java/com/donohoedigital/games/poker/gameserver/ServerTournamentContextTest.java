/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026  Doug Donohoe, DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file in the root directory
 * of this project.
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
package com.donohoedigital.games.poker.gameserver;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.core.GamePlayerInfo;
import com.donohoedigital.games.poker.core.GameTable;

/**
 * Test for ServerTournamentContext - validates tournament-level state
 * management, blind structure, level progression, and player tracking.
 */
class ServerTournamentContextTest {

    private ServerTournamentContext tournament;
    private List<ServerPlayer> players;

    private static final int STARTING_CHIPS = 1000;
    private static final int[] SMALL_BLINDS = {10, 20, 30, 50, 75, 100};
    private static final int[] BIG_BLINDS = {20, 40, 60, 100, 150, 200};
    private static final int[] ANTES = {0, 0, 5, 10, 15, 20};
    private static final int[] LEVEL_MINUTES = {10, 10, 10, 10, 10, 10};
    private static final boolean[] BREAK_LEVELS = {false, false, true, false, false, true};

    @BeforeEach
    void setUp() {
        players = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            players.add(new ServerPlayer(i, "Player" + i, i == 1, i == 1 ? 0 : 5, STARTING_CHIPS));
        }

        tournament = new ServerTournamentContext(players, 1, // 1 table (6 players)
                STARTING_CHIPS, SMALL_BLINDS, BIG_BLINDS, ANTES, LEVEL_MINUTES, BREAK_LEVELS, false, // not practice
                                                                                                        // (online)
                3, // max rebuys
                2, // rebuy max level
                true, // allow addons
                30 // timeout seconds
        );
    }

    // === Basic Properties ===

    @Test
    void testNumTables() {
        assertEquals(1, tournament.getNumTables());
    }

    @Test
    void testNumPlayers() {
        assertEquals(6, tournament.getNumPlayers());
    }

    @Test
    void testIsOnlineGame() {
        assertTrue(tournament.isOnlineGame());
        assertFalse(tournament.isPractice());
    }

    @Test
    void testStartingChips() {
        assertEquals(STARTING_CHIPS, tournament.getStartingChips());
    }

    // === Player Access ===

    @Test
    void testGetPlayerByID() {
        GamePlayerInfo player = tournament.getPlayerByID(1);
        assertNotNull(player);
        assertEquals("Player1", player.getName());
    }

    @Test
    void testGetPlayerByID_NotFound() {
        assertNull(tournament.getPlayerByID(999));
    }

    @Test
    void testGetLocalPlayer() {
        // For server, local player is the first human player
        GamePlayerInfo local = tournament.getLocalPlayer();
        assertNotNull(local);
        assertTrue(local.isHuman());
    }

    // === Table Access ===

    @Test
    void testGetTable() {
        GameTable table = tournament.getTable(0);
        assertNotNull(table);
        assertEquals(0, table.getNumber());
    }

    @Test
    void testGetCurrentTable() {
        // For server, current table is table 0 by default
        GameTable current = tournament.getCurrentTable();
        assertNotNull(current);
        assertEquals(0, current.getNumber());
    }

    // === Level Management ===

    @Test
    void testInitialLevel() {
        assertEquals(0, tournament.getLevel());
    }

    @Test
    void testNextLevel() {
        assertEquals(0, tournament.getLevel());

        tournament.nextLevel();
        assertEquals(1, tournament.getLevel());

        tournament.nextLevel();
        assertEquals(2, tournament.getLevel());
    }

    @Test
    void testGetSmallBlind() {
        assertEquals(10, tournament.getSmallBlind(0));
        assertEquals(20, tournament.getSmallBlind(1));
        assertEquals(30, tournament.getSmallBlind(2));
    }

    @Test
    void testGetBigBlind() {
        assertEquals(20, tournament.getBigBlind(0));
        assertEquals(40, tournament.getBigBlind(1));
        assertEquals(60, tournament.getBigBlind(2));
    }

    @Test
    void testGetAnte() {
        assertEquals(0, tournament.getAnte(0));
        assertEquals(0, tournament.getAnte(1));
        assertEquals(5, tournament.getAnte(2));
        assertEquals(10, tournament.getAnte(3));
    }

    // === Break Levels ===

    @Test
    void testIsBreakLevel() {
        assertFalse(tournament.isBreakLevel(0));
        assertFalse(tournament.isBreakLevel(1));
        assertTrue(tournament.isBreakLevel(2));
        assertFalse(tournament.isBreakLevel(3));
        assertFalse(tournament.isBreakLevel(4));
        assertTrue(tournament.isBreakLevel(5));
    }

    // === Level Timing ===

    @Test
    void testIsLevelExpired_NotExpired() {
        assertFalse(tournament.isLevelExpired(), "Level should not expire immediately");
    }

    @Test
    void testIsLevelExpired_AfterAdvanceClock() {
        // Create a practice tournament for testing clock advance
        List<ServerPlayer> practicePlayers = new ArrayList<>();
        practicePlayers.add(new ServerPlayer(1, "Human", true, 0, STARTING_CHIPS));
        practicePlayers.add(new ServerPlayer(2, "AI", false, 5, STARTING_CHIPS));

        ServerTournamentContext practiceTournament = new ServerTournamentContext(practicePlayers, 1, STARTING_CHIPS,
                SMALL_BLINDS, BIG_BLINDS, ANTES, LEVEL_MINUTES, BREAK_LEVELS, true, // practice mode
                0, 0, false, 0);

        // Advance clock repeatedly to simulate time passing
        for (int i = 0; i < 15; i++) { // More than 10 minutes worth
            practiceTournament.advanceClock();
        }
        // After many clock advances, level should be expired
        assertTrue(practiceTournament.isLevelExpired(), "Level should expire after sufficient clock advances");
    }

    @Test
    void testAdvanceClockBreak() {
        tournament.advanceClockBreak();
        // Clock should advance during break (no exception)
    }

    @Test
    void testStartGameClock() {
        tournament.startGameClock();
        // Game clock should start (no exception)
    }

    // === Chip Denomination ===

    @Test
    void testMinChip() {
        assertEquals(1, tournament.getMinChip());
    }

    @Test
    void testLastMinChip() {
        int minChip = tournament.getMinChip();
        tournament.nextLevel();
        assertEquals(minChip, tournament.getLastMinChip());
    }

    // === Game Over Detection ===

    @Test
    void testIsGameOver_NotOver() {
        assertFalse(tournament.isGameOver());
    }

    @Test
    void testIsOnePlayerLeft_Multiple() {
        assertFalse(tournament.isOnePlayerLeft());
    }

    @Test
    void testIsOnePlayerLeft_OneLeft() {
        // Eliminate all but one player
        for (int i = 1; i < players.size(); i++) {
            players.get(i).setChipCount(0);
        }
        assertTrue(tournament.isOnePlayerLeft());
    }

    @Test
    void testIsGameOver_OnePlayerLeft() {
        // Eliminate all but one player
        for (int i = 1; i < players.size(); i++) {
            players.get(i).setChipCount(0);
        }
        // Game should be over when only one player has chips
        assertTrue(tournament.isGameOver());
    }

    // === Rebuy Management ===

    @Test
    void testIsRebuyPeriodActive_WithinLevel() {
        GamePlayerInfo player = players.get(0);
        // At level 0, rebuy max is level 2, so rebuy should be active
        assertTrue(tournament.isRebuyPeriodActive(player));
    }

    @Test
    void testIsRebuyPeriodActive_AfterLevel() {
        GamePlayerInfo player = players.get(0);

        // Advance to level 3 (past rebuy max level of 2)
        tournament.nextLevel(); // level 1
        tournament.nextLevel(); // level 2
        tournament.nextLevel(); // level 3

        assertFalse(tournament.isRebuyPeriodActive(player), "Rebuy should not be active after max rebuy level");
    }

    @Test
    void testIsRebuyPeriodActive_MaxRebuysReached() {
        ServerPlayer player = players.get(0);

        // Set player to max rebuys
        for (int i = 0; i < 3; i++) {
            player.incrementRebuys();
        }

        assertFalse(tournament.isRebuyPeriodActive(player), "Rebuy should not be active when max rebuys reached");
    }

    // === Timeout Configuration ===

    @Test
    void testGetTimeoutSeconds() {
        assertEquals(30, tournament.getTimeoutSeconds());
    }

    @Test
    void testGetTimeoutForRound() {
        // Should return configured timeout for all rounds
        assertEquals(30, tournament.getTimeoutForRound(0));
        assertEquals(30, tournament.getTimeoutForRound(1));
        assertEquals(30, tournament.getTimeoutForRound(2));
    }

    // === Scheduled Start ===

    @Test
    void testScheduledStart_Disabled() {
        assertFalse(tournament.isScheduledStartEnabled());
    }

    // === Multi-Table Scenario ===

    @Test
    void testMultiTable() {
        List<ServerPlayer> manyPlayers = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            manyPlayers.add(new ServerPlayer(i, "Player" + i, false, 5, STARTING_CHIPS));
        }

        ServerTournamentContext multiTable = new ServerTournamentContext(manyPlayers, 2, // 2 tables
                STARTING_CHIPS, SMALL_BLINDS, BIG_BLINDS, ANTES, LEVEL_MINUTES, BREAK_LEVELS, true, // practice
                0, // no rebuys
                0, // no rebuy level
                false, // no addons
                0 // no timeout
        );

        assertEquals(2, multiTable.getNumTables());
        assertEquals(20, multiTable.getNumPlayers());

        // Verify players distributed across tables
        GameTable table1 = multiTable.getTable(0);
        GameTable table2 = multiTable.getTable(1);
        assertTrue(table1.getNumOccupiedSeats() > 0);
        assertTrue(table2.getNumOccupiedSeats() > 0);
        assertEquals(20, table1.getNumOccupiedSeats() + table2.getNumOccupiedSeats());
    }

    // === Practice Mode ===

    @Test
    void testPracticeMode() {
        List<ServerPlayer> practicePlayers = new ArrayList<>();
        practicePlayers.add(new ServerPlayer(1, "Human", true, 0, STARTING_CHIPS));
        practicePlayers.add(new ServerPlayer(2, "AI", false, 5, STARTING_CHIPS));

        ServerTournamentContext practice = new ServerTournamentContext(practicePlayers, 1, STARTING_CHIPS, SMALL_BLINDS,
                BIG_BLINDS, ANTES, LEVEL_MINUTES, BREAK_LEVELS, true, // practice
                0, 0, false, 0);

        assertTrue(practice.isPractice());
        assertFalse(practice.isOnlineGame());
    }
}
