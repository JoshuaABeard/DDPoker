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

/**
 * Client-side betting round enum for the display layer. Independent of
 * pokerengine's BettingRound.
 */
public enum ClientBettingRound {

    PRE_FLOP(0), FLOP(3), TURN(4), RIVER(5), SHOWDOWN(5);

    private final int communityCardCount;

    ClientBettingRound(int communityCardCount) {
        this.communityCardCount = communityCardCount;
    }

    /** Number of community cards visible in this round. */
    public int communityCardCount() {
        return communityCardCount;
    }

    /** Parse from a protocol string (case-insensitive). */
    public static ClientBettingRound fromString(String s) {
        if (s == null) {
            throw new IllegalArgumentException("Betting round string must not be null");
        }
        return valueOf(s.toUpperCase());
    }
}
