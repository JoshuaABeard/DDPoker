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
package com.donohoedigital.games.poker.model;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;

/**
 * Encapsulates tournament blind/ante structure with automatic doubling logic.
 *
 * <p>
 * Wraps a DMTypedHashMap to provide clean access to blind and ante amounts
 * across all levels, with support for:
 * <ul>
 * <li>Blind/ante access for any level (small, big, ante)
 * <li>Break level detection
 * <li>Automatic doubling after last defined level
 * <li>Overflow protection for MAX_BLINDANTE
 * <li>Skip breaks when retrieving last non-break blind
 * </ul>
 *
 * <p>
 * Extracted from TournamentProfile to improve testability.
 *
 * @see TournamentProfile#getSmallBlind(int)
 * @see TournamentProfile#getBigBlind(int)
 * @see TournamentProfile#getAnte(int)
 */
public class BlindStructure {

    private final DMTypedHashMap map;
    private final int lastLevel;
    private final boolean doubleAfterLast;

    /** Parameter keys (match TournamentProfile constants). */
    private static final String PARAM_SMALL = "small";
    private static final String PARAM_BIG = "big";
    private static final String PARAM_ANTE = "ante";

    /**
     * Create a blind structure wrapper around the given map.
     *
     * @param map
     *            The map containing blind/ante data
     */
    public BlindStructure(DMTypedHashMap map) {
        this.map = map;
        this.lastLevel = map.getInteger("lastlevel", 0);
        this.doubleAfterLast = map.getBoolean("doubleafterlast", false);
    }

    /**
     * Get small blind for the given level.
     *
     * <p>
     * If level > lastLevel and doubling is enabled, returns doubled amount. Throws
     * ApplicationError if called on a break level.
     *
     * @param level
     *            The level number
     * @return The small blind amount
     */
    public int getSmallBlind(int level) {
        return getAmount(PARAM_SMALL, level);
    }

    /**
     * Get big blind for the given level.
     *
     * <p>
     * If level > lastLevel and doubling is enabled, returns doubled amount. Throws
     * ApplicationError if called on a break level.
     *
     * @param level
     *            The level number
     * @return The big blind amount
     */
    public int getBigBlind(int level) {
        return getAmount(PARAM_BIG, level);
    }

    /**
     * Get ante for the given level.
     *
     * <p>
     * If level > lastLevel and doubling is enabled, returns doubled amount. Throws
     * ApplicationError if called on a break level.
     *
     * @param level
     *            The level number
     * @return The ante amount
     */
    public int getAnte(int level) {
        return getAmount(PARAM_ANTE, level);
    }

    /**
     * Check if the given level is a break.
     *
     * @param level
     *            The level number
     * @return True if level is a break, false otherwise
     */
    public boolean isBreak(int level) {
        return getAmountFromString(PARAM_ANTE + level, true) == TournamentProfile.BREAK_ANTE_VALUE;
    }

    /**
     * Get last small blind - if current level is a break, returns first prior
     * non-break level.
     *
     * @param level
     *            The level number
     * @return The small blind from the last non-break level
     */
    public int getLastSmallBlind(int level) {
        while (isBreak(level) && level > 0) {
            level--;
        }
        return getSmallBlind(level);
    }

    /**
     * Get last big blind - if current level is a break, returns first prior
     * non-break level.
     *
     * @param level
     *            The level number
     * @return The big blind from the last non-break level
     */
    public int getLastBigBlind(int level) {
        while (isBreak(level) && level > 0) {
            level--;
        }
        return getBigBlind(level);
    }

    /**
     * Get amount for the given parameter and level, with doubling logic.
     *
     * <p>
     * Extracted from TournamentProfile.getAmount() (lines 658-683).
     *
     * @param paramName
     *            The parameter name (small, big, ante)
     * @param level
     *            The level number
     * @return The amount, potentially doubled
     */
    private int getAmount(String paramName, int level) {
        ApplicationError.assertTrue(!isBreak(level), "Attempting to get value for a break level", paramName);

        int amount;
        int effectiveLastLevel = lastLevel;

        if (level > lastLevel) {
            // Skip breaks when beyond last level
            while (isBreak(effectiveLastLevel) && effectiveLastLevel > 0) {
                effectiveLastLevel--;
                level--;
            }

            amount = getAmountFromString(paramName + effectiveLastLevel, false);

            if (doubleAfterLast) {
                // Double blinds/antes for each level beyond last
                // Use long to detect overflow before it happens
                long longAmount = amount;
                for (int i = 0; i < (level - effectiveLastLevel); i++) {
                    longAmount *= 2;
                    if (longAmount >= TournamentProfile.MAX_BLINDANTE) {
                        longAmount /= 2; // Undo last doubling that caused overflow
                        break;
                    }
                }
                amount = (int) longAmount;
            }
        } else {
            amount = getAmountFromString(paramName + level, false);
        }

        return amount;
    }

    /**
     * Parse integer from string in map, with bounds checking.
     *
     * <p>
     * Extracted from TournamentProfile.getAmountFromString().
     *
     * @param key
     *            The map key
     * @param allowNegative
     *            Whether to allow negative values (for break detection)
     * @return The parsed amount
     */
    private int getAmountFromString(String key, boolean allowNegative) {
        String value = map.getString(key);
        if (value == null || value.length() == 0) {
            return 0;
        }

        int amount = Integer.parseInt(value);
        if (!allowNegative && amount < 0) {
            amount = 0;
        }
        if (amount > TournamentProfile.MAX_BLINDANTE) {
            amount = TournamentProfile.MAX_BLINDANTE;
        }

        return amount;
    }
}
