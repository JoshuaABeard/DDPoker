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
 * pokerengine's ClientBettingRound.
 */
public enum ClientBettingRound {

    NONE(-1, 0), PRE_FLOP(0, 0), FLOP(1, 3), TURN(2, 4), RIVER(3, 5), SHOWDOWN(4, 5);

    /** Legacy integer constants matching pokerengine ClientBettingRound. */
    public static final int ROUND_NONE = -1;
    public static final int ROUND_PRE_FLOP = 0;
    public static final int ROUND_FLOP = 1;
    public static final int ROUND_TURN = 2;
    public static final int ROUND_RIVER = 3;
    public static final int ROUND_SHOWDOWN = 4;

    private final int legacyValue;
    private final int communityCardCount;

    // O(1) lookup array: legacy values range from -1 to 4
    private static final ClientBettingRound[] LOOKUP = new ClientBettingRound[6];

    static {
        for (ClientBettingRound br : values()) {
            LOOKUP[br.legacyValue + 1] = br;
        }
    }

    ClientBettingRound(int legacyValue, int communityCardCount) {
        this.legacyValue = legacyValue;
        this.communityCardCount = communityCardCount;
    }

    /** Number of community cards visible in this round. */
    public int communityCardCount() {
        return communityCardCount;
    }

    /** Convert to legacy integer constant. */
    public int toLegacy() {
        return legacyValue;
    }

    /**
     * Convert from legacy integer constant to enum (O(1) lookup).
     */
    public static ClientBettingRound fromLegacy(int round) {
        int index = round + 1;
        if (index < 0 || index >= LOOKUP.length || LOOKUP[index] == null) {
            throw new IllegalArgumentException("Unknown betting round: " + round);
        }
        return LOOKUP[index];
    }

    /** Parse from a protocol string (case-insensitive). */
    public static ClientBettingRound fromString(String s) {
        if (s == null) {
            throw new IllegalArgumentException("Betting round string must not be null");
        }
        return valueOf(s.toUpperCase());
    }

    /**
     * Get name for debugging (matches engine ClientBettingRound.getRoundName).
     */
    public static String getRoundName(int n) {
        return switch (n) {
            case ROUND_PRE_FLOP -> "preflop";
            case ROUND_FLOP -> "flop";
            case ROUND_TURN -> "turn";
            case ROUND_RIVER -> "river";
            case ROUND_SHOWDOWN -> "show";
            default -> "none: " + n;
        };
    }
}
