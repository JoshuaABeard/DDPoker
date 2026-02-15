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
package com.donohoedigital.games.poker.core.state;

/**
 * Enum representing betting rounds in Texas Hold'em. Replaces
 * HoldemHand.ROUND_* integer constants (lines 73-78).
 */
public enum BettingRound {
    NONE(-1), PRE_FLOP(0), FLOP(1), TURN(2), RIVER(3), SHOWDOWN(4);

    private final int legacyValue;

    // O(1) lookup array: legacy values range from -1 to 4
    private static final BettingRound[] LOOKUP = new BettingRound[6];

    static {
        for (BettingRound br : values()) {
            LOOKUP[br.legacyValue + 1] = br; // offset by 1 to handle -1
        }
    }

    BettingRound(int legacyValue) {
        this.legacyValue = legacyValue;
    }

    /**
     * Convert from legacy integer constant to enum (O(1) lookup).
     *
     * @param round
     *            legacy integer value (e.g., HoldemHand.ROUND_FLOP)
     * @return corresponding BettingRound enum value
     * @throws IllegalArgumentException
     *             if round value is unknown
     */
    public static BettingRound fromLegacy(int round) {
        int index = round + 1; // offset by 1 to handle -1
        if (index < 0 || index >= LOOKUP.length || LOOKUP[index] == null) {
            throw new IllegalArgumentException("Unknown betting round: " + round);
        }
        return LOOKUP[index];
    }

    /**
     * Convert enum to legacy integer constant.
     *
     * @return legacy integer value (e.g., HoldemHand.ROUND_FLOP)
     */
    public int toLegacy() {
        return legacyValue;
    }
}
