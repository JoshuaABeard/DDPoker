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

import com.donohoedigital.games.poker.PokerTableInput;
import com.donohoedigital.games.poker.PokerTable;

/**
 * Bet validation logic extracted from Bet.java. Contains pure business logic
 * for determining input modes and validating bet amounts with no UI
 * dependencies. Part of Wave 1 testability refactoring.
 *
 * <p>
 * Extracted logic:
 * <ul>
 * <li>Input mode determination based on betting state</li>
 * <li>Bet amount validation and rounding</li>
 * </ul>
 */
public class BetValidator {

    // Utility class - no instantiation
    private BetValidator() {
    }

    /**
     * Determine which input mode should be used based on the current betting
     * situation.
     *
     * <p>
     * Logic extracted from Bet.java lines 173-181:
     * <ul>
     * <li>If no call needed and no bet: CHECK_BET mode</li>
     * <li>If no call needed but there's a bet: CHECK_RAISE mode</li>
     * <li>If call is needed: CALL_RAISE mode</li>
     * </ul>
     *
     * @param toCall
     *            amount player needs to call (0 if no bet to call)
     * @param currentBet
     *            current bet amount in the round (0 if no bet yet)
     * @return input mode constant from PokerTableInput
     */
    public static int determineInputMode(int toCall, int currentBet) {
        if (toCall == 0) {
            if (currentBet == 0) {
                return PokerTableInput.MODE_CHECK_BET;
            } else {
                return PokerTableInput.MODE_CHECK_RAISE;
            }
        } else {
            return PokerTableInput.MODE_CALL_RAISE;
        }
    }

    /**
     * Validate a bet amount and return validation result.
     *
     * @param table
     *            poker table with min chip setting
     * @param amount
     *            bet amount to validate
     * @return validation result with rounded amount and validity flag
     */
    public static BetValidationResult validateBetAmount(PokerTable table, int amount) {
        int roundedAmount = PokerLogicUtils.roundAmountMinChip(table, amount);
        boolean needsRounding = (roundedAmount != amount);

        return new BetValidationResult(amount, roundedAmount, needsRounding);
    }

    /**
     * Result of bet amount validation. Immutable value object.
     */
    public static class BetValidationResult {
        private final int originalAmount;
        private final int roundedAmount;
        private final boolean needsRounding;

        public BetValidationResult(int originalAmount, int roundedAmount, boolean needsRounding) {
            this.originalAmount = originalAmount;
            this.roundedAmount = roundedAmount;
            this.needsRounding = needsRounding;
        }

        /**
         * Get the original bet amount before rounding.
         *
         * @return original amount
         */
        public int getOriginalAmount() {
            return originalAmount;
        }

        /**
         * Get the rounded bet amount (conforms to table min chip).
         *
         * @return rounded amount
         */
        public int getRoundedAmount() {
            return roundedAmount;
        }

        /**
         * Check if the amount needs rounding to conform to table min chip.
         *
         * @return true if rounding is needed
         */
        public boolean needsRounding() {
            return needsRounding;
        }

        /**
         * Check if the bet amount is valid (exact multiple of min chip).
         *
         * @return true if no rounding needed
         */
        public boolean isValid() {
            return !needsRounding;
        }
    }
}
