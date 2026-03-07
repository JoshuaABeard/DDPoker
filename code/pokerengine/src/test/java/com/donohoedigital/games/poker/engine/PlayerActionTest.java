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
package com.donohoedigital.games.poker.engine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.engine.state.ActionType;

class PlayerActionTest {

    @Test
    void fold_hasCorrectTypeAndZeroAmount() {
        PlayerAction action = PlayerAction.fold();
        assertEquals(ActionType.FOLD, action.actionType());
        assertEquals(0, action.amount());
    }

    @Test
    void check_hasCorrectTypeAndZeroAmount() {
        PlayerAction action = PlayerAction.check();
        assertEquals(ActionType.CHECK, action.actionType());
        assertEquals(0, action.amount());
    }

    @Test
    void call_hasCorrectTypeAndZeroAmount() {
        PlayerAction action = PlayerAction.call();
        assertEquals(ActionType.CALL, action.actionType());
        assertEquals(0, action.amount());
    }

    @Test
    void bet_hasCorrectTypeAndAmount() {
        PlayerAction action = PlayerAction.bet(500);
        assertEquals(ActionType.BET, action.actionType());
        assertEquals(500, action.amount());
    }

    @Test
    void raise_hasCorrectTypeAndAmount() {
        PlayerAction action = PlayerAction.raise(1000);
        assertEquals(ActionType.RAISE, action.actionType());
        assertEquals(1000, action.amount());
    }

    @Test
    void equality_sameFactoryCallsAreEqual() {
        assertEquals(PlayerAction.fold(), PlayerAction.fold());
        assertEquals(PlayerAction.check(), PlayerAction.check());
        assertEquals(PlayerAction.call(), PlayerAction.call());
        assertEquals(PlayerAction.bet(100), PlayerAction.bet(100));
        assertEquals(PlayerAction.raise(200), PlayerAction.raise(200));
    }

    @Test
    void equality_differentActionsAreNotEqual() {
        assertNotEquals(PlayerAction.fold(), PlayerAction.check());
        assertNotEquals(PlayerAction.bet(100), PlayerAction.bet(200));
        assertNotEquals(PlayerAction.bet(100), PlayerAction.raise(100));
    }

    @Test
    void directConstructor_matchesFactoryMethod() {
        assertEquals(PlayerAction.fold(), new PlayerAction(ActionType.FOLD, 0));
        assertEquals(PlayerAction.bet(250), new PlayerAction(ActionType.BET, 250));
    }
}
