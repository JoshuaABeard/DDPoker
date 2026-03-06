/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 DD Poker Community
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
package com.donohoedigital.games.poker.display;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientBettingRoundTest {

    @Test
    void values() {
        assertEquals(6, ClientBettingRound.values().length);
    }

    @Test
    void communityCardCount() {
        assertEquals(0, ClientBettingRound.PRE_FLOP.communityCardCount());
        assertEquals(3, ClientBettingRound.FLOP.communityCardCount());
        assertEquals(4, ClientBettingRound.TURN.communityCardCount());
        assertEquals(5, ClientBettingRound.RIVER.communityCardCount());
        assertEquals(5, ClientBettingRound.SHOWDOWN.communityCardCount());
    }

    @Test
    void fromString_validValues() {
        assertEquals(ClientBettingRound.PRE_FLOP, ClientBettingRound.fromString("PRE_FLOP"));
        assertEquals(ClientBettingRound.FLOP, ClientBettingRound.fromString("FLOP"));
        assertEquals(ClientBettingRound.TURN, ClientBettingRound.fromString("TURN"));
        assertEquals(ClientBettingRound.RIVER, ClientBettingRound.fromString("RIVER"));
        assertEquals(ClientBettingRound.SHOWDOWN, ClientBettingRound.fromString("SHOWDOWN"));
    }

    @Test
    void fromString_caseInsensitive() {
        assertEquals(ClientBettingRound.FLOP, ClientBettingRound.fromString("flop"));
        assertEquals(ClientBettingRound.PRE_FLOP, ClientBettingRound.fromString("pre_flop"));
    }

    @Test
    void fromString_invalidThrows() {
        assertThrows(IllegalArgumentException.class, () -> ClientBettingRound.fromString("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> ClientBettingRound.fromString(""));
        assertThrows(IllegalArgumentException.class, () -> ClientBettingRound.fromString(null));
    }
}
