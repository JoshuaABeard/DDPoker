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

import com.donohoedigital.games.poker.core.GamePlayerInfo;

/**
 * Tests for ServerPlayer. TDD: Test-first implementation.
 */
class ServerPlayerTest {

    @Test
    void testConstruction_HumanPlayer() {
        ServerPlayer player = new ServerPlayer(42, "Alice", true, 0, 5000);

        assertEquals(42, player.getID());
        assertEquals("Alice", player.getName());
        assertTrue(player.isHuman());
        assertFalse(player.isComputer());
        assertEquals(5000, player.getChipCount());
    }

    @Test
    void testConstruction_AIPlayer() {
        ServerPlayer player = new ServerPlayer(99, "Bot-Easy", false, 2, 5000);

        assertEquals(99, player.getID());
        assertEquals("Bot-Easy", player.getName());
        assertFalse(player.isHuman());
        assertTrue(player.isComputer());
        assertEquals(5000, player.getChipCount());
    }

    @Test
    void testChipManagement() {
        ServerPlayer player = new ServerPlayer(1, "Test", true, 0, 5000);

        player.setChipCount(3000);
        assertEquals(3000, player.getChipCount());

        player.addChips(1500);
        assertEquals(4500, player.getChipCount());

        player.subtractChips(2500);
        assertEquals(2000, player.getChipCount());
    }

    @Test
    void testSeatAssignment() {
        ServerPlayer player = new ServerPlayer(1, "Test", true, 0, 5000);

        player.setSeat(3);
        assertEquals(3, player.getSeat());

        player.setSeat(7);
        assertEquals(7, player.getSeat());
    }

    @Test
    void testFoldedState() {
        ServerPlayer player = new ServerPlayer(1, "Test", true, 0, 5000);

        assertFalse(player.isFolded());

        player.setFolded(true);
        assertTrue(player.isFolded());

        player.setFolded(false);
        assertFalse(player.isFolded());
    }

    @Test
    void testAllInState() {
        ServerPlayer player = new ServerPlayer(1, "Test", true, 0, 5000);

        assertFalse(player.isAllIn());

        player.setAllIn(true);
        assertTrue(player.isAllIn());

        player.setAllIn(false);
        assertFalse(player.isAllIn());
    }

    @Test
    void testSittingOutState() {
        ServerPlayer player = new ServerPlayer(1, "Test", true, 0, 5000);

        assertFalse(player.isSittingOut());

        player.setSittingOut(true);
        assertTrue(player.isSittingOut());

        player.setSittingOut(false);
        assertFalse(player.isSittingOut());
    }

    @Test
    void testObserverState() {
        ServerPlayer player = new ServerPlayer(1, "Test", true, 0, 5000);

        assertFalse(player.isObserver());

        player.setObserver(true);
        assertTrue(player.isObserver());

        player.setObserver(false);
        assertFalse(player.isObserver());
    }

    @Test
    void testHumanControlled_HumanPlayer() {
        ServerPlayer player = new ServerPlayer(1, "Human", true, 0, 5000);
        assertTrue(player.isHumanControlled());
    }

    @Test
    void testHumanControlled_AIPlayer() {
        ServerPlayer player = new ServerPlayer(1, "Bot", false, 5, 5000);
        assertFalse(player.isHumanControlled());
    }

    @Test
    void testLocallyControlled_ServerMode() {
        // In server mode, all players are "locally controlled" by the server
        ServerPlayer humanPlayer = new ServerPlayer(1, "Human", true, 0, 5000);
        ServerPlayer aiPlayer = new ServerPlayer(2, "Bot", false, 3, 5000);

        assertTrue(humanPlayer.isLocallyControlled());
        assertTrue(aiPlayer.isLocallyControlled());
    }

    @Test
    void testThinkBank() {
        ServerPlayer player = new ServerPlayer(1, "Test", true, 0, 5000);

        player.setThinkBankMillis(60000); // 60 seconds
        assertEquals(60000, player.getThinkBankMillis());
    }

    @Test
    void testTimeout() {
        ServerPlayer player = new ServerPlayer(1, "Test", true, 0, 5000);

        player.setTimeoutMillis(30000); // 30 seconds
        player.setTimeoutMessageSecondsLeft(10); // Warn at 10 seconds

        // No direct getters for these, but they should not throw
    }

    @Test
    void testAskShowCards_DefaultFalse() {
        ServerPlayer player = new ServerPlayer(1, "Test", true, 0, 5000);

        assertFalse(player.isAskShowWinning());
        assertFalse(player.isAskShowLosing());
    }

    @Test
    void testAskShowCards_CanBeSet() {
        ServerPlayer player = new ServerPlayer(1, "Test", true, 0, 5000);

        player.setAskShowWinning(true);
        player.setAskShowLosing(true);

        assertTrue(player.isAskShowWinning());
        assertTrue(player.isAskShowLosing());
    }

    @Test
    void testRebuys() {
        ServerPlayer player = new ServerPlayer(1, "Test", true, 0, 5000);

        assertEquals(0, player.getNumRebuys());

        player.incrementRebuys();
        assertEquals(1, player.getNumRebuys());

        player.incrementRebuys();
        assertEquals(2, player.getNumRebuys());
    }

    @Test
    void testGamePlayerInfoInterface() {
        ServerPlayer player = new ServerPlayer(42, "Alice", true, 0, 5000);

        // Verify it implements the interface
        GamePlayerInfo info = player;

        assertEquals(42, info.getID());
        assertEquals("Alice", info.getName());
        assertTrue(info.isHuman());
        assertEquals(5000, info.getChipCount());
    }

    @Test
    void testSkillLevel_AIPlayer() {
        ServerPlayer player = new ServerPlayer(1, "Bot", false, 7, 5000);

        assertEquals(7, player.getSkillLevel());
    }

    @Test
    void testSkillLevel_HumanPlayer_ZeroByDefault() {
        ServerPlayer player = new ServerPlayer(1, "Human", true, 0, 5000);

        assertEquals(0, player.getSkillLevel());
    }
}
