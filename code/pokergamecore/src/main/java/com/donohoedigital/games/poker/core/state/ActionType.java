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
 * Enum representing player action types in poker. Replaces HandAction.ACTION_*
 * integer constants (lines 58-70).
 */
public enum ActionType {
    NONE(-1), FOLD(0), CHECK(1), CHECK_RAISE(2), CALL(3), BET(4), RAISE(5), BLIND_BIG(6), BLIND_SM(7), ANTE(8), WIN(
            9), OVERBET(10), LOSE(11);

    private final int legacyValue;

    // O(1) lookup array: legacy values range from -1 to 11
    private static final ActionType[] LOOKUP = new ActionType[13];

    static {
        for (ActionType at : values()) {
            LOOKUP[at.legacyValue + 1] = at; // offset by 1 to handle -1
        }
    }

    ActionType(int legacyValue) {
        this.legacyValue = legacyValue;
    }

    /**
     * Convert from legacy integer constant to enum (O(1) lookup).
     *
     * @param action
     *            legacy integer value (e.g., HandAction.ACTION_FOLD)
     * @return corresponding ActionType enum value
     * @throws IllegalArgumentException
     *             if action value is unknown
     */
    public static ActionType fromLegacy(int action) {
        int index = action + 1; // offset by 1 to handle -1
        if (index < 0 || index >= LOOKUP.length || LOOKUP[index] == null) {
            throw new IllegalArgumentException("Unknown action type: " + action);
        }
        return LOOKUP[index];
    }

    /**
     * Convert enum to legacy integer constant.
     *
     * @return legacy integer value (e.g., HandAction.ACTION_FOLD)
     */
    public int toLegacy() {
        return legacyValue;
    }
}
