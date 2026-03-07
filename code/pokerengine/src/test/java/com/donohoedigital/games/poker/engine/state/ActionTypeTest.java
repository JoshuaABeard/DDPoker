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

class ActionTypeTest {

    @ParameterizedTest
    @EnumSource(ActionType.class)
    void fromLegacy_roundTrip_allValues(ActionType actionType) {
        assertEquals(actionType, ActionType.fromLegacy(actionType.toLegacy()));
    }

    @Test
    void toLegacy_returnsExpectedValues() {
        assertEquals(-1, ActionType.NONE.toLegacy());
        assertEquals(0, ActionType.FOLD.toLegacy());
        assertEquals(1, ActionType.CHECK.toLegacy());
        assertEquals(2, ActionType.CHECK_RAISE.toLegacy());
        assertEquals(3, ActionType.CALL.toLegacy());
        assertEquals(4, ActionType.BET.toLegacy());
        assertEquals(5, ActionType.RAISE.toLegacy());
        assertEquals(6, ActionType.BLIND_BIG.toLegacy());
        assertEquals(7, ActionType.BLIND_SM.toLegacy());
        assertEquals(8, ActionType.ANTE.toLegacy());
        assertEquals(9, ActionType.WIN.toLegacy());
        assertEquals(10, ActionType.OVERBET.toLegacy());
        assertEquals(11, ActionType.LOSE.toLegacy());
    }

    @Test
    void fromLegacy_negativeOne_returnsNone() {
        assertEquals(ActionType.NONE, ActionType.fromLegacy(-1));
    }

    @Test
    void fromLegacy_tooLow_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> ActionType.fromLegacy(-2));
    }

    @Test
    void fromLegacy_tooHigh_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> ActionType.fromLegacy(12));
    }

    @Test
    void enumValues_has13Members() {
        assertEquals(13, ActionType.values().length);
    }
}
