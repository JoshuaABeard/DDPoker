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

import org.junit.jupiter.api.Test;

/**
 * Tests for ServerPot.
 */
class ServerPotTest {

    @Test
    void testConstruction_Empty() {
        ServerPot pot = new ServerPot(1, 0); // Round 1, no side bet

        assertEquals(0, pot.getChips());
        assertEquals(1, pot.getRound());
        assertEquals(0, pot.getSideBet());
        assertTrue(pot.getEligiblePlayers().isEmpty());
        assertTrue(pot.getWinners().isEmpty());
    }

    @Test
    void testAddChips_SinglePlayer() {
        ServerPot pot = new ServerPot(1, 0);
        ServerPlayer player = new ServerPlayer(1, "Alice", true, 0, 5000);

        pot.addChips(player, 500);

        assertEquals(500, pot.getChips());
        assertTrue(pot.isPlayerEligible(player));
        assertEquals(1, pot.getEligiblePlayers().size());
    }

    @Test
    void testAddChips_MultiplePlayers() {
        ServerPot pot = new ServerPot(1, 0);
        ServerPlayer alice = new ServerPlayer(1, "Alice", true, 0, 5000);
        ServerPlayer bob = new ServerPlayer(2, "Bob", true, 0, 5000);
        ServerPlayer charlie = new ServerPlayer(3, "Charlie", true, 0, 5000);

        pot.addChips(alice, 200);
        pot.addChips(bob, 200);
        pot.addChips(charlie, 200);

        assertEquals(600, pot.getChips());
        assertTrue(pot.isPlayerEligible(alice));
        assertTrue(pot.isPlayerEligible(bob));
        assertTrue(pot.isPlayerEligible(charlie));
        assertEquals(3, pot.getEligiblePlayers().size());
    }

    @Test
    void testAddChips_SamePlayerTwice() {
        ServerPot pot = new ServerPot(1, 0);
        ServerPlayer player = new ServerPlayer(1, "Test", true, 0, 5000);

        pot.addChips(player, 300);
        pot.addChips(player, 200);

        assertEquals(500, pot.getChips());
        assertEquals(1, pot.getEligiblePlayers().size()); // Player only counted once in eligibility
    }

    @Test
    void testSetWinners() {
        ServerPot pot = new ServerPot(1, 0);
        ServerPlayer winner1 = new ServerPlayer(1, "Winner1", true, 0, 5000);
        ServerPlayer winner2 = new ServerPlayer(2, "Winner2", true, 0, 5000);

        pot.addWinner(winner1);
        pot.addWinner(winner2);

        assertEquals(2, pot.getWinners().size());
        assertTrue(pot.getWinners().contains(winner1));
        assertTrue(pot.getWinners().contains(winner2));
    }

    @Test
    void testIsOverbet_SingleEligiblePlayer() {
        ServerPot pot = new ServerPot(1, 0);
        ServerPlayer player = new ServerPlayer(1, "Overbet", true, 0, 5000);

        pot.addChips(player, 500);

        assertTrue(pot.isOverbet()); // Only one eligible player = overbet (uncalled amount)
    }

    @Test
    void testIsOverbet_MultipleEligiblePlayers() {
        ServerPot pot = new ServerPot(1, 0);
        ServerPlayer player1 = new ServerPlayer(1, "P1", true, 0, 5000);
        ServerPlayer player2 = new ServerPlayer(2, "P2", true, 0, 5000);

        pot.addChips(player1, 500);
        pot.addChips(player2, 500);

        assertFalse(pot.isOverbet()); // Multiple players = contested pot
    }

    @Test
    void testSideBetTracking() {
        ServerPot mainPot = new ServerPot(1, 0);
        ServerPot sidePot = new ServerPot(1, 1000); // Side pot created at 1000 chip all-in level

        assertEquals(0, mainPot.getSideBet());
        assertEquals(1000, sidePot.getSideBet());
    }

    @Test
    void testRoundTracking() {
        ServerPot preflopPot = new ServerPot(0, 0);
        ServerPot flopPot = new ServerPot(1, 0);
        ServerPot turnPot = new ServerPot(2, 500);

        assertEquals(0, preflopPot.getRound());
        assertEquals(1, flopPot.getRound());
        assertEquals(2, turnPot.getRound());
    }
}
