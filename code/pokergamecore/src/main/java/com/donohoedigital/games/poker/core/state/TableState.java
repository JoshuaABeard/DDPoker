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
 * Enum representing poker table states. Replaces PokerTable.STATE_* integer
 * constants (lines 106-124).
 */
public enum TableState {
    NONE(0), PENDING(1), DEAL_FOR_BUTTON(2), BEGIN(3), BEGIN_WAIT(4), CHECK_END_HAND(5), CLEAN(6), NEW_LEVEL_CHECK(
            7), COLOR_UP(8), START_HAND(9), BETTING(10), COMMUNITY(11), SHOWDOWN(
                    12), DONE(13), GAME_OVER(14), PENDING_LOAD(15), ON_HOLD(16), BREAK(17), PRE_SHOWDOWN(18);

    private final int legacyValue;

    // O(1) lookup array: legacy values range from 0 to 18
    private static final TableState[] LOOKUP = new TableState[19];

    static {
        for (TableState ts : values()) {
            LOOKUP[ts.legacyValue] = ts;
        }
    }

    TableState(int legacyValue) {
        this.legacyValue = legacyValue;
    }

    /**
     * Convert from legacy integer constant to enum (O(1) lookup).
     *
     * @param state
     *            legacy integer value (e.g., PokerTable.STATE_BETTING)
     * @return corresponding TableState enum value
     * @throws IllegalArgumentException
     *             if state value is unknown
     */
    public static TableState fromLegacy(int state) {
        if (state < 0 || state >= LOOKUP.length || LOOKUP[state] == null) {
            throw new IllegalArgumentException("Unknown table state: " + state);
        }
        return LOOKUP[state];
    }

    /**
     * Convert enum to legacy integer constant.
     *
     * @return legacy integer value (e.g., PokerTable.STATE_BETTING)
     */
    public int toLegacy() {
        return legacyValue;
    }
}
