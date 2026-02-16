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
 * Tests for ServerHandAction record.
 */
class ServerHandActionTest {

    @Test
    void testConstruction() {
        ServerPlayer player = new ServerPlayer(1, "Alice", true, 0, 5000);
        ServerHandAction action = new ServerHandAction(player, 1, ServerHandAction.ACTION_RAISE, 500, 200, false);

        assertSame(player, action.player());
        assertEquals(1, action.round());
        assertEquals(ServerHandAction.ACTION_RAISE, action.action());
        assertEquals(500, action.amount());
        assertEquals(200, action.subAmount());
        assertFalse(action.allIn());
    }

    @Test
    void testActionConstants() {
        // Verify action constants are defined
        assertEquals(0, ServerHandAction.ACTION_FOLD);
        assertEquals(1, ServerHandAction.ACTION_CHECK);
        assertEquals(2, ServerHandAction.ACTION_CALL);
        assertEquals(3, ServerHandAction.ACTION_BET);
        assertEquals(4, ServerHandAction.ACTION_RAISE);
    }

    @Test
    void testFoldAction() {
        ServerPlayer player = new ServerPlayer(1, "Bob", true, 0, 5000);
        ServerHandAction action = new ServerHandAction(player, 1, ServerHandAction.ACTION_FOLD, 0, 0, false);

        assertEquals(ServerHandAction.ACTION_FOLD, action.action());
        assertEquals(0, action.amount());
    }

    @Test
    void testCheckAction() {
        ServerPlayer player = new ServerPlayer(1, "Charlie", true, 0, 5000);
        ServerHandAction action = new ServerHandAction(player, 1, ServerHandAction.ACTION_CHECK, 0, 0, false);

        assertEquals(ServerHandAction.ACTION_CHECK, action.action());
        assertEquals(0, action.amount());
    }

    @Test
    void testCallAction() {
        ServerPlayer player = new ServerPlayer(1, "Dave", true, 0, 5000);
        ServerHandAction action = new ServerHandAction(player, 1, ServerHandAction.ACTION_CALL, 200, 0, false);

        assertEquals(ServerHandAction.ACTION_CALL, action.action());
        assertEquals(200, action.amount());
    }

    @Test
    void testBetAction() {
        ServerPlayer player = new ServerPlayer(1, "Eve", true, 0, 5000);
        ServerHandAction action = new ServerHandAction(player, 1, ServerHandAction.ACTION_BET, 300, 0, false);

        assertEquals(ServerHandAction.ACTION_BET, action.action());
        assertEquals(300, action.amount());
    }

    @Test
    void testRaiseAction() {
        ServerPlayer player = new ServerPlayer(1, "Frank", true, 0, 5000);
        // Raise to 500 (call 200 + raise 300)
        ServerHandAction action = new ServerHandAction(player, 1, ServerHandAction.ACTION_RAISE, 500, 200, false);

        assertEquals(ServerHandAction.ACTION_RAISE, action.action());
        assertEquals(500, action.amount());
        assertEquals(200, action.subAmount()); // Call portion
    }

    @Test
    void testAllInAction() {
        ServerPlayer player = new ServerPlayer(1, "Grace", true, 0, 1000);
        ServerHandAction action = new ServerHandAction(player, 1, ServerHandAction.ACTION_CALL, 1000, 0, true);

        assertTrue(action.allIn());
        assertEquals(1000, action.amount());
    }

    @Test
    void testRoundTracking() {
        ServerPlayer player = new ServerPlayer(1, "Test", true, 0, 5000);

        ServerHandAction preflop = new ServerHandAction(player, 0, ServerHandAction.ACTION_CALL, 100, 0, false);
        ServerHandAction flop = new ServerHandAction(player, 1, ServerHandAction.ACTION_CHECK, 0, 0, false);
        ServerHandAction turn = new ServerHandAction(player, 2, ServerHandAction.ACTION_BET, 200, 0, false);
        ServerHandAction river = new ServerHandAction(player, 3, ServerHandAction.ACTION_FOLD, 0, 0, false);

        assertEquals(0, preflop.round());
        assertEquals(1, flop.round());
        assertEquals(2, turn.round());
        assertEquals(3, river.round());
    }
}
