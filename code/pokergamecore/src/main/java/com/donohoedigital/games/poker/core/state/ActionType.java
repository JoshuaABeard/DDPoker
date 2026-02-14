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
 * Enum representing player action types in poker. Replaces HandAction.ACTION_*
 * integer constants (lines 58-70).
 */
public enum ActionType {
    NONE(-1), FOLD(0), CHECK(1), CHECK_RAISE(2), CALL(3), BET(4), RAISE(5), BLIND_BIG(6), BLIND_SM(7), ANTE(8), WIN(
            9), OVERBET(10), LOSE(11);

    private final int legacyValue;

    ActionType(int legacyValue) {
        this.legacyValue = legacyValue;
    }

    /**
     * Convert from legacy integer constant to enum.
     *
     * @param action
     *            legacy integer value (e.g., HandAction.ACTION_FOLD)
     * @return corresponding ActionType enum value
     * @throws IllegalArgumentException
     *             if action value is unknown
     */
    public static ActionType fromLegacy(int action) {
        for (ActionType at : values()) {
            if (at.legacyValue == action) {
                return at;
            }
        }
        throw new IllegalArgumentException("Unknown action type: " + action);
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
