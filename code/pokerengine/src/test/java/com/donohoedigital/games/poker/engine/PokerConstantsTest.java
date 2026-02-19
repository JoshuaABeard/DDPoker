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
package com.donohoedigital.games.poker.engine;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests for PokerConstants.
 */
public class PokerConstantsTest {

    @Test
    public void testVersionConstant() {
        assertNotNull(PokerConstants.VERSION);
        assertEquals(3, PokerConstants.VERSION.getMajor());
        assertEquals(3, PokerConstants.VERSION.getMinor());
        assertEquals(0, PokerConstants.VERSION.getPatch());
    }

    @Test
    public void testLatestVersionConstants() {
        assertNotNull(PokerConstants.LATEST_MAC);
        assertNotNull(PokerConstants.LATEST_LINUX);
        assertNotNull(PokerConstants.LATEST_WINDOWS);
    }

    @Test
    public void testVersionCompatConstants() {
        assertNotNull(PokerConstants.VERSION_ALIVE_CHECK_ADDED);
        assertNotNull(PokerConstants.VERSION_ALIVE_LOBBY_ADDED);
        assertNotNull(PokerConstants.VERSION_COUNTDOWN_CHANGED);
        assertNotNull(PokerConstants.VERSION_HOST_CHECK_ADDED);
        assertNotNull(PokerConstants.VERSION_LAST_COMPAT);
    }

    @Test
    public void testFormatPercentZero() {
        String result = PokerConstants.formatPercent(0.0);
        assertNotNull(result);
        assertTrue(result.contains("0.00"));
    }

    @Test
    public void testFormatPercentHalf() {
        String result = PokerConstants.formatPercent(0.5);
        assertNotNull(result);
        assertTrue(result.contains("0.50"));
    }

    @Test
    public void testFormatPercentFull() {
        String result = PokerConstants.formatPercent(1.0);
        assertNotNull(result);
        assertTrue(result.contains("1.00"));
    }

    @Test
    public void testFormatPercentDecimal() {
        String result = PokerConstants.formatPercent(0.123);
        assertNotNull(result);
        assertTrue(result.contains("0.12"));
    }

    @Test
    public void testToStringAdminTypeMsg() {
        String result = PokerConstants.toStringAdminType(PokerConstants.CHAT_ADMIN_MSG);
        assertEquals("message", result);
    }

    @Test
    public void testToStringAdminTypeJoin() {
        String result = PokerConstants.toStringAdminType(PokerConstants.CHAT_ADMIN_JOIN);
        assertEquals("join", result);
    }

    @Test
    public void testToStringAdminTypeLeave() {
        String result = PokerConstants.toStringAdminType(PokerConstants.CHAT_ADMIN_LEAVE);
        assertEquals("leave", result);
    }

    @Test
    public void testToStringAdminTypeWelcome() {
        String result = PokerConstants.toStringAdminType(PokerConstants.CHAT_ADMIN_WELCOME);
        assertEquals("welcome", result);
    }

    @Test
    public void testToStringAdminTypeError() {
        String result = PokerConstants.toStringAdminType(PokerConstants.CHAT_ADMIN_ERROR);
        assertEquals("error", result);
    }

    @Test
    public void testToStringAdminTypeUnknown() {
        String result = PokerConstants.toStringAdminType(99);
        assertEquals("unknown-99", result);
    }

    @Test
    public void testChatAdminConstants() {
        assertEquals(0, PokerConstants.CHAT_ADMIN_MSG);
        assertEquals(1, PokerConstants.CHAT_ADMIN_JOIN);
        assertEquals(2, PokerConstants.CHAT_ADMIN_LEAVE);
        assertEquals(3, PokerConstants.CHAT_ADMIN_WELCOME);
        assertEquals(4, PokerConstants.CHAT_ADMIN_ERROR);
    }

    @Test
    public void testGameTypeConstants() {
        assertEquals(1, PokerConstants.TYPE_NO_LIMIT_HOLDEM);
        assertEquals(2, PokerConstants.TYPE_POT_LIMIT_HOLDEM);
        assertEquals(3, PokerConstants.TYPE_LIMIT_HOLDEM);
    }

    @Test
    public void testStartIdConstants() {
        assertEquals(7000, PokerConstants.START_OBSERVER_ID);
        assertEquals(8000, PokerConstants.START_TABLE_ID);
        assertEquals(11000, PokerConstants.START_OTHER_ID);
    }

    @Test
    public void testPieceConstants() {
        assertEquals(5, PokerConstants.PIECE_CARD);
        assertEquals(10, PokerConstants.PIECE_BUTTON);
        assertEquals(15, PokerConstants.PIECE_RESULTS);
    }

    @Test
    public void testPayoutConstants() {
        assertEquals(1, PokerConstants.PAYOUT_SPOTS);
        assertEquals(2, PokerConstants.PAYOUT_PERC);
        assertEquals(3, PokerConstants.PAYOUT_SATELLITE);
    }

    @Test
    public void testHouseConstants() {
        assertEquals(1, PokerConstants.HOUSE_AMOUNT);
        assertEquals(2, PokerConstants.HOUSE_PERC);
    }

    @Test
    public void testAllocConstants() {
        assertEquals(1, PokerConstants.ALLOC_AUTO);
        assertEquals(2, PokerConstants.ALLOC_PERC);
        assertEquals(3, PokerConstants.ALLOC_AMOUNT);
    }

    @Test
    public void testRebuyConstants() {
        assertEquals(1, PokerConstants.REBUY_LT);
        assertEquals(2, PokerConstants.REBUY_LTE);
    }

    @Test
    public void testLateRegChipsConstants() {
        assertEquals(1, PokerConstants.LATE_REG_CHIPS_STARTING);
        assertEquals(2, PokerConstants.LATE_REG_CHIPS_AVERAGE);
    }

    @Test
    public void testDealerChatConstants() {
        assertEquals(1, PokerConstants.DEALER_NONE);
        assertEquals(2, PokerConstants.DEALER_NO_PLAYER_ACTION);
        assertEquals(3, PokerConstants.DEALER_ALL);
    }

    @Test
    public void testDisplayChatConstants() {
        assertEquals(1, PokerConstants.DISPLAY_ONE);
        assertEquals(2, PokerConstants.DISPLAY_TAB);
        assertEquals(3, PokerConstants.DISPLAY_SPLIT);
    }

    @Test
    public void testChatLevelConstants() {
        assertEquals(-1, PokerConstants.CHAT_PRIVATE);
        assertEquals(0, PokerConstants.CHAT_ALWAYS);
        assertEquals(1, PokerConstants.CHAT_1);
        assertEquals(2, PokerConstants.CHAT_2);
        assertEquals(3, PokerConstants.CHAT_TIMEOUT);
    }

    @Test
    public void testSeatsConstants() {
        assertEquals(10, PokerConstants.SEATS);
        assertEquals(10, PokerConstants.SEATS_FULL_RING);
        assertEquals(6, PokerConstants.SEATS_6MAX);
        assertEquals(2, PokerConstants.SEATS_HEADS_UP);
    }

    @Test
    public void testPotActionConstants() {
        assertEquals(0, PokerConstants.NO_POT_ACTION);
        assertEquals(1, PokerConstants.CALLED_POT);
        assertEquals(2, PokerConstants.RAISED_POT);
        assertEquals(3, PokerConstants.RERAISED_POT);
    }

    @Test
    public void testPlayerIdConstants() {
        assertEquals(-1, PokerConstants.PLAYER_ID_TEMP);
    }

    @Test
    public void testOnlineConstants() {
        assertEquals(3, PokerConstants.MAX_PROFILES_PER_EMAIL);
        assertEquals(2, PokerConstants.MIN_SCHEDULED_START_PLAYERS);
        assertEquals("n-", PokerConstants.ONLINE_GAME_PREFIX_TCP);
        assertEquals("u-", PokerConstants.ONLINE_GAME_PREFIX_UDP);
    }

    @Test
    public void testOptionConstants() {
        assertNotNull(PokerConstants.OPTION_CLOCK_COLOUP);
        assertNotNull(PokerConstants.OPTION_AUTODEAL);
        assertNotNull(PokerConstants.OPTION_CHECKFOLD);
        assertNotNull(PokerConstants.OPTION_LARGE_CARDS);
        assertNotNull(PokerConstants.OPTION_FOUR_COLOR_DECK);
    }

    @Test
    public void testChatFontConstants() {
        assertEquals(12, PokerConstants.DEFAULT_CHAT_FONT_SIZE);
        assertEquals(8, PokerConstants.MIN_CHAT_FONT_SIZE);
        assertEquals(24, PokerConstants.MAX_CHAT_FONT_SIZE);
    }

    @Test
    public void testContentTypeConstants() {
        assertEquals("application/x-ddpoker-join", PokerConstants.CONTENT_TYPE_JOIN);
        assertEquals("ddpokerjoin", PokerConstants.JOIN_FILE_EXT);
        assertEquals("?obs", PokerConstants.JOIN_OBSERVER_QUERY);
    }

    @Test
    public void testChatUserTypeConstants() {
        assertEquals(1, PokerConstants.USERTYPE_CHAT);
        assertEquals(2, PokerConstants.USERTYPE_HELLO);
    }

    @Test
    public void testMiscConstants() {
        assertEquals(3000, PokerConstants.PROFILE_RETRY_MILLIS);
        assertEquals(200, PokerConstants.VERTICAL_SCREEN_FREE_SPACE);
    }
}
