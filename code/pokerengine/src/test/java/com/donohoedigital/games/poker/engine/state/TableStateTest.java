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

class TableStateTest {

    @ParameterizedTest
    @EnumSource(TableState.class)
    void fromLegacy_roundTrip_allValues(TableState state) {
        assertEquals(state, TableState.fromLegacy(state.toLegacy()));
    }

    @Test
    void toLegacy_returnsExpectedValues() {
        assertEquals(0, TableState.NONE.toLegacy());
        assertEquals(1, TableState.PENDING.toLegacy());
        assertEquals(10, TableState.BETTING.toLegacy());
        assertEquals(12, TableState.SHOWDOWN.toLegacy());
        assertEquals(14, TableState.GAME_OVER.toLegacy());
        assertEquals(18, TableState.PRE_SHOWDOWN.toLegacy());
    }

    @Test
    void fromLegacy_negative_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> TableState.fromLegacy(-1));
    }

    @Test
    void fromLegacy_tooHigh_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> TableState.fromLegacy(19));
    }

    @Test
    void enumValues_has19Members() {
        assertEquals(19, TableState.values().length);
    }
}
