/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
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
package com.donohoedigital.games.poker.engine.state;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class BettingRoundTest {

    @ParameterizedTest
    @EnumSource(BettingRound.class)
    void fromLegacy_roundTrip_allValues(BettingRound round) {
        assertEquals(round, BettingRound.fromLegacy(round.toLegacy()));
    }

    @Test
    void toLegacy_returnsExpectedValues() {
        assertEquals(-1, BettingRound.NONE.toLegacy());
        assertEquals(0, BettingRound.PRE_FLOP.toLegacy());
        assertEquals(1, BettingRound.FLOP.toLegacy());
        assertEquals(2, BettingRound.TURN.toLegacy());
        assertEquals(3, BettingRound.RIVER.toLegacy());
        assertEquals(4, BettingRound.SHOWDOWN.toLegacy());
    }

    @Test
    void staticConstants_matchEnumValues() {
        assertEquals(BettingRound.NONE.toLegacy(), BettingRound.ROUND_NONE);
        assertEquals(BettingRound.PRE_FLOP.toLegacy(), BettingRound.ROUND_PRE_FLOP);
        assertEquals(BettingRound.FLOP.toLegacy(), BettingRound.ROUND_FLOP);
        assertEquals(BettingRound.TURN.toLegacy(), BettingRound.ROUND_TURN);
        assertEquals(BettingRound.RIVER.toLegacy(), BettingRound.ROUND_RIVER);
        assertEquals(BettingRound.SHOWDOWN.toLegacy(), BettingRound.ROUND_SHOWDOWN);
    }

    @Test
    void fromLegacy_negativeOne_returnsNone() {
        assertEquals(BettingRound.NONE, BettingRound.fromLegacy(-1));
    }

    @Test
    void fromLegacy_tooLow_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> BettingRound.fromLegacy(-2));
    }

    @Test
    void fromLegacy_tooHigh_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> BettingRound.fromLegacy(5));
    }

    @Test
    void getRoundName_allRounds() {
        assertEquals("preflop", BettingRound.getRoundName(0));
        assertEquals("flop", BettingRound.getRoundName(1));
        assertEquals("turn", BettingRound.getRoundName(2));
        assertEquals("river", BettingRound.getRoundName(3));
        assertEquals("show", BettingRound.getRoundName(4));
    }

    @Test
    void getRoundName_unknownRound_returnsNoneWithValue() {
        assertEquals("none: -1", BettingRound.getRoundName(-1));
        assertEquals("none: 99", BettingRound.getRoundName(99));
    }

    @Test
    void enumValues_has6Members() {
        assertEquals(6, BettingRound.values().length);
    }
}
