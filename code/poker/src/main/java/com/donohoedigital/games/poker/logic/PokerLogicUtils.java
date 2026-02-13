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
package com.donohoedigital.games.poker.logic;

import com.donohoedigital.games.poker.PokerTable;

import java.math.BigInteger;

/**
 * Pure logic utility methods for poker game calculations. This class contains
 * no UI dependencies and can be tested in headless mode. Methods extracted from
 * PokerUtils.java as part of the testability refactoring (Wave 1).
 *
 * <p>
 * Contains:
 * <ul>
 * <li>Math helpers (pow, nChooseK)</li>
 * <li>Chip calculation logic (roundAmountMinChip)</li>
 * </ul>
 */
public class PokerLogicUtils {

    // Pre-calculated factorials for combinatorics (0! through 52!)
    private static final BigInteger[] FACTORIAL = new BigInteger[53];

    static {
        FACTORIAL[0] = new BigInteger("1");

        for (int i = 1; i < 53; ++i) {
            FACTORIAL[i] = FACTORIAL[i - 1].multiply(new BigInteger(Integer.toString(i)));
        }
    }

    // Utility class - no instantiation
    private PokerLogicUtils() {
    }

    /**
     * Calculate integer power (n^p).
     *
     * @param n
     *            base
     * @param p
     *            exponent
     * @return n raised to the power p
     */
    public static int pow(int n, int p) {
        int res = 1;
        while (p-- > 0)
            res *= n;
        return res;
    }

    /**
     * Calculate n choose k (binomial coefficient). Used for calculating poker hand
     * probabilities.
     *
     * @param n
     *            total items
     * @param k
     *            items to choose
     * @return number of ways to choose k items from n
     */
    public static long nChooseK(int n, int k) {
        return FACTORIAL[n].divide(FACTORIAL[k]).divide(FACTORIAL[n - k]).longValue();
    }

    /**
     * Round chips to be a multiple of the min chip on the table. Used to ensure bet
     * amounts conform to table minimums (e.g., if min chip is 5, round 123 to 125).
     *
     * @param table
     *            poker table with min chip setting
     * @param chips
     *            amount to round
     * @return chips rounded to nearest multiple of table min chip
     */
    public static int roundAmountMinChip(PokerTable table, int chips) {
        int nNewAmount = chips;
        int nMinChip = table.getMinChip();
        int nOdd = chips % nMinChip;
        if (nOdd != 0) {
            nNewAmount = chips - nOdd;
            if ((float) nOdd >= (nMinChip / 2.0f)) {
                nNewAmount += nMinChip;
            }
        }

        return nNewAmount;
    }
}
