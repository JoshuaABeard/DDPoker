/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
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

    BettingRound(int legacyValue) {
        this.legacyValue = legacyValue;
    }

    /**
     * Convert from legacy integer constant to enum.
     *
     * @param round
     *            legacy integer value (e.g., HoldemHand.ROUND_FLOP)
     * @return corresponding BettingRound enum value
     * @throws IllegalArgumentException
     *             if round value is unknown
     */
    public static BettingRound fromLegacy(int round) {
        for (BettingRound br : values()) {
            if (br.legacyValue == round) {
                return br;
            }
        }
        throw new IllegalArgumentException("Unknown betting round: " + round);
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
