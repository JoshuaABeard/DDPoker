/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.engine;

/**
 * Integer constants for poker hand action types.
 *
 * <p>
 * These match the legacy {@code HandAction.ACTION_*} values exactly so that
 * server-side code can switch on action integers without depending on the
 * desktop-client {@code HandAction} class.
 */
public final class PokerActionConstants {

    public static final int ACTION_NONE = -1;
    public static final int ACTION_FOLD = 0;
    public static final int ACTION_CHECK = 1;
    public static final int ACTION_CHECK_RAISE = 2;
    public static final int ACTION_CALL = 3;
    public static final int ACTION_BET = 4;
    public static final int ACTION_RAISE = 5;
    public static final int ACTION_BLIND_BIG = 6;
    public static final int ACTION_BLIND_SM = 7;
    public static final int ACTION_ANTE = 8;
    public static final int ACTION_WIN = 9;
    public static final int ACTION_OVERBET = 10;
    public static final int ACTION_LOSE = 11;

    // Fold sub-type constants (stored in HandAction.nSubAmount_)
    public static final int FOLD_NORMAL = 0;
    public static final int FOLD_FORCED = -1;
    public static final int FOLD_SITTING_OUT = -2;

    private PokerActionConstants() {
    }
}
