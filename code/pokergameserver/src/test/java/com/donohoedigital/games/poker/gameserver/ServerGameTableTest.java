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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.core.state.TableState;

/**
 * Test for ServerGameTable - validates seat management, button movement, hand
 * management, and state transitions.
 */
class ServerGameTableTest {

    private ServerGameTable table;
    private static final int TABLE_NUMBER = 1;
    private static final int NUM_SEATS = 10;
    private static final int STARTING_CHIPS = 1000;

    @BeforeEach
    void setUp() {
        table = new ServerGameTable(TABLE_NUMBER, NUM_SEATS, null, 0, 0, 0);
    }

    // === Basic Properties ===

    @Test
    void testTableNumber() {
        assertEquals(TABLE_NUMBER, table.getNumber());
    }

    @Test
    void testSeats() {
        assertEquals(NUM_SEATS, table.getSeats());
    }

    @Test
    void testInitialState() {
        assertEquals(TableState.BEGIN, table.getTableState());
        assertEquals(0, table.getNumOccupiedSeats());
        assertEquals(0, table.getHandNum());
        assertEquals(-1, table.getButton());
        assertNull(table.getHoldemHand());
    }

    // === Seat Management ===

    @Test
    void testAddPlayer() {
        ServerPlayer player = createPlayer(1, "Alice");
        table.addPlayer(player, 0);

        assertEquals(1, table.getNumOccupiedSeats());
        assertEquals(player, table.getPlayer(0));
        assertEquals(0, player.getSeat());
    }

    @Test
    void testAddMultiplePlayers() {
        ServerPlayer p1 = createPlayer(1, "Alice");
        ServerPlayer p2 = createPlayer(2, "Bob");
        ServerPlayer p3 = createPlayer(3, "Carol");

        table.addPlayer(p1, 0);
        table.addPlayer(p2, 3);
        table.addPlayer(p3, 7);

        assertEquals(3, table.getNumOccupiedSeats());
        assertEquals(p1, table.getPlayer(0));
        assertNull(table.getPlayer(1));
        assertNull(table.getPlayer(2));
        assertEquals(p2, table.getPlayer(3));
        assertEquals(p3, table.getPlayer(7));
    }

    @Test
    void testRemovePlayer() {
        ServerPlayer player = createPlayer(1, "Alice");
        table.addPlayer(player, 0);

        table.removePlayer(0);

        assertEquals(0, table.getNumOccupiedSeats());
        assertNull(table.getPlayer(0));
    }

    @Test
    void testGetPlayerAtEmptySeat() {
        assertNull(table.getPlayer(0));
        assertNull(table.getPlayer(5));
    }

    // === Button Management ===

    @Test
    void testSetButtonExplicitly() {
        table.setButton(5);
        assertEquals(5, table.getButton());
    }

    @Test
    void testSetButtonDealsCards() {
        // Add 3 players at different seats
        table.addPlayer(createPlayer(1, "Alice"), 0);
        table.addPlayer(createPlayer(2, "Bob"), 3);
        table.addPlayer(createPlayer(3, "Carol"), 7);

        // setButton() with no arg should deal cards and determine button
        table.setButton();

        // Button should be at one of the occupied seats
        int button = table.getButton();
        assertTrue(button == 0 || button == 3 || button == 7, "Button should be at an occupied seat, was: " + button);
    }

    @Test
    void testButtonAdvancement() {
        table.addPlayer(createPlayer(1, "Alice"), 0);
        table.addPlayer(createPlayer(2, "Bob"), 2);
        table.addPlayer(createPlayer(3, "Carol"), 5);

        table.setButton(0);
        assertEquals(0, table.getButton());

        table.advanceButton();
        assertEquals(2, table.getButton(), "Button should advance to next occupied seat");

        table.advanceButton();
        assertEquals(5, table.getButton());

        table.advanceButton();
        assertEquals(0, table.getButton(), "Button should wrap around to first occupied seat");
    }

    // === Seat Navigation ===

    @Test
    void testGetNextSeat() {
        table.addPlayer(createPlayer(1, "Alice"), 0);
        table.addPlayer(createPlayer(2, "Bob"), 3);
        table.addPlayer(createPlayer(3, "Carol"), 7);

        assertEquals(3, table.getNextSeat(0));
        assertEquals(7, table.getNextSeat(3));
        assertEquals(0, table.getNextSeat(7), "Should wrap to first occupied seat");
    }

    @Test
    void testGetNextSeatFromEmpty() {
        table.addPlayer(createPlayer(1, "Alice"), 2);
        table.addPlayer(createPlayer(2, "Bob"), 7);

        assertEquals(2, table.getNextSeat(0), "From empty seat 0, next occupied is 2");
        assertEquals(7, table.getNextSeat(2));
        assertEquals(2, table.getNextSeat(7), "Wrap from 7 to 2");
    }

    @Test
    void testGetNextSeatAfterButton() {
        table.addPlayer(createPlayer(1, "Alice"), 1);
        table.addPlayer(createPlayer(2, "Bob"), 4);
        table.addPlayer(createPlayer(3, "Carol"), 8);

        table.setButton(1);
        assertEquals(4, table.getNextSeatAfterButton());

        table.setButton(4);
        assertEquals(8, table.getNextSeatAfterButton());

        table.setButton(8);
        assertEquals(1, table.getNextSeatAfterButton(), "Should wrap to first seat after button");
    }

    // === State Management ===

    @Test
    void testStateTransitions() {
        assertEquals(TableState.BEGIN, table.getTableState());

        table.setTableState(TableState.BETTING);
        assertEquals(TableState.BETTING, table.getTableState());
    }

    @Test
    void testPendingState() {
        assertNull(table.getPendingTableState());

        table.setPendingTableState(TableState.BETTING);
        assertEquals(TableState.BETTING, table.getPendingTableState());
    }

    @Test
    void testPreviousState() {
        assertNull(table.getPreviousTableState());

        table.setTableState(TableState.BEGIN);
        table.setTableState(TableState.BETTING);
        assertEquals(TableState.BEGIN, table.getPreviousTableState());
    }

    @Test
    void testPendingPhase() {
        assertNull(table.getPendingPhase());

        table.setPendingPhase("ShowTournamentTable");
        assertEquals("ShowTournamentTable", table.getPendingPhase());
    }

    // === Hand Management ===

    @Test
    void testStartNewHand() {
        // Setup: add players and set button
        table.addPlayer(createPlayer(1, "Alice"), 0);
        table.addPlayer(createPlayer(2, "Bob"), 3);
        table.addPlayer(createPlayer(3, "Carol"), 7);
        table.setButton(0);

        // Start first hand
        assertEquals(0, table.getHandNum());
        table.startNewHand();

        // Verify: hand number incremented, hand created, button advanced
        assertEquals(1, table.getHandNum());
        assertNotNull(table.getHoldemHand());
        assertEquals(3, table.getButton(), "Button should advance to next seat");
    }

    @Test
    void testStartMultipleHands() {
        table.addPlayer(createPlayer(1, "Alice"), 0);
        table.addPlayer(createPlayer(2, "Bob"), 5);
        table.setButton(0);

        table.startNewHand();
        assertEquals(1, table.getHandNum());
        assertEquals(5, table.getButton());

        table.startNewHand();
        assertEquals(2, table.getHandNum());
        assertEquals(0, table.getButton(), "Button wraps around");
    }

    @Test
    void testSetHoldemHand() {
        assertNull(table.getHoldemHand());

        // Create a minimal hand (requires players seated)
        table.addPlayer(createPlayer(1, "Alice"), 0);
        table.addPlayer(createPlayer(2, "Bob"), 3);
        table.setButton(0);

        // ServerHand constructor: table, handNum, sb, bb, ante, button, sbSeat, bbSeat
        ServerHand hand = new ServerHand(table, 1, 10, 20, 0, 0, 1, 2);
        table.setHoldemHand(hand);

        assertEquals(hand, table.getHoldemHand());
    }

    // === Level Management ===

    @Test
    void testLevel() {
        table.setLevel(1);
        assertEquals(1, table.getLevel());

        table.setLevel(5);
        assertEquals(5, table.getLevel());
    }

    @Test
    void testHandNum() {
        assertEquals(0, table.getHandNum());

        table.setHandNum(10);
        assertEquals(10, table.getHandNum());
    }

    // === Configuration ===

    @Test
    void testAutoDeal() {
        assertTrue(table.isAutoDeal(), "Auto-deal should be true by default");
    }

    @Test
    void testZipMode() {
        assertFalse(table.isZipMode(), "Zip mode should be false by default");

        table.setZipMode(true);
        assertTrue(table.isZipMode());

        table.setZipMode(false);
        assertFalse(table.isZipMode());
    }

    // === Wait List ===

    @Test
    void testWaitList() {
        assertEquals(0, table.getWaitSize());

        ServerPlayer p1 = createPlayer(1, "Alice");
        ServerPlayer p2 = createPlayer(2, "Bob");

        table.addWait(p1);
        assertEquals(1, table.getWaitSize());
        assertEquals(p1, table.getWaitPlayer(0));

        table.addWait(p2);
        assertEquals(2, table.getWaitSize());
        assertEquals(p2, table.getWaitPlayer(1));

        table.removeWaitAll();
        assertEquals(0, table.getWaitSize());
    }

    // === Timing ===

    @Test
    void testMillisSinceLastStateChange() throws InterruptedException {
        table.setTableState(TableState.BEGIN);
        long t1 = table.getMillisSinceLastStateChange();

        Thread.sleep(50);

        long t2 = table.getMillisSinceLastStateChange();
        assertTrue(t2 >= t1 + 40, "Time should advance, was: " + t1 + " then " + t2);
    }

    @Test
    void testPause() {
        table.setPause(1000);
        // Pause is tracked internally - no getter to test directly
        // This just verifies no exception thrown
    }

    @Test
    void testAutoDealDelay() {
        int delay = table.getAutoDealDelay();
        assertTrue(delay >= 0, "Auto-deal delay should be non-negative");
    }

    // === Table Status ===

    @Test
    void testIsAllComputer() {
        assertFalse(table.isAllComputer(), "Empty table is not all-computer");

        table.addPlayer(createPlayer(1, "AI1", false), 0);
        table.addPlayer(createPlayer(2, "AI2", false), 3);
        assertTrue(table.isAllComputer(), "Table with only AI should be all-computer");

        table.addPlayer(createPlayer(3, "Human", true), 7);
        assertFalse(table.isAllComputer(), "Table with human should not be all-computer");
    }

    @Test
    void testIsRemoved() {
        assertFalse(table.isRemoved(), "Table should not be removed initially");
    }

    @Test
    void testIsCurrent() {
        assertTrue(table.isCurrent(), "Server tables are always current");
    }

    // === Helpers ===

    private ServerPlayer createPlayer(int id, String name) {
        return createPlayer(id, name, true);
    }

    private ServerPlayer createPlayer(int id, String name, boolean human) {
        return new ServerPlayer(id, name, human, human ? 0 : 5, STARTING_CHIPS);
    }
}
