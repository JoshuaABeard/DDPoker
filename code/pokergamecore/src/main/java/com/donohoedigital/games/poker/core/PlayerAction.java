/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
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
package com.donohoedigital.games.poker.core;

import com.donohoedigital.games.poker.core.state.ActionType;

/**
 * A player's decision (action + amount). Immutable record returned by
 * PlayerActionProvider.
 *
 * @param actionType
 *            the type of action (fold, check, call, bet, raise)
 * @param amount
 *            the bet/raise amount (0 for fold/check/call)
 */
public record PlayerAction(ActionType actionType, int amount) {
    /** Factory: create a fold action. */
    public static PlayerAction fold() {
        return new PlayerAction(ActionType.FOLD, 0);
    }

    /** Factory: create a check action. */
    public static PlayerAction check() {
        return new PlayerAction(ActionType.CHECK, 0);
    }

    /** Factory: create a call action. */
    public static PlayerAction call() {
        return new PlayerAction(ActionType.CALL, 0);
    }

    /**
     * Factory: create a bet action.
     *
     * @param amount
     *            bet amount
     */
    public static PlayerAction bet(int amount) {
        return new PlayerAction(ActionType.BET, amount);
    }

    /**
     * Factory: create a raise action.
     *
     * @param amount
     *            raise amount
     */
    public static PlayerAction raise(int amount) {
        return new PlayerAction(ActionType.RAISE, amount);
    }
}
