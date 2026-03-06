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
package com.donohoedigital.games.poker;

/**
 * Pure-logic calculations for betting decisions. Extracted from {@link Bet} for
 * testability.
 */
class BetCalculator {

    private BetCalculator() {
    }

    /**
     * Determines the input mode based on the current betting state.
     *
     * @param toCall
     *            the amount the player needs to call
     * @param currentBet
     *            the current bet on the table
     * @return one of {@link PokerTableInput#MODE_CHECK_BET},
     *         {@link PokerTableInput#MODE_CHECK_RAISE}, or
     *         {@link PokerTableInput#MODE_CALL_RAISE}
     */
    static int determineInputMode(int toCall, int currentBet) {
        if (toCall == 0) {
            return (currentBet == 0) ? PokerTableInput.MODE_CHECK_BET : PokerTableInput.MODE_CHECK_RAISE;
        }
        return PokerTableInput.MODE_CALL_RAISE;
    }

    /**
     * Rounds a bet amount to the nearest multiple of the minimum chip, rounding up
     * when the remainder is at least half the minimum chip.
     *
     * @param amount
     *            the raw bet amount
     * @param minChip
     *            the minimum chip denomination
     * @return the rounded amount
     */
    static int roundToMinChip(int amount, int minChip) {
        int odd = amount % minChip;
        if (odd == 0) {
            return amount;
        }
        int rounded = amount - odd;
        if ((float) odd >= (minChip / 2.0f)) {
            rounded += minChip;
        }
        return rounded;
    }

    /**
     * Determines whether the player action is a bet or a raise based on the current
     * input mode.
     *
     * @param inputMode
     *            the current input mode
     * @return {@link HandAction#ACTION_BET} if mode is
     *         {@link PokerTableInput#MODE_CHECK_BET}, otherwise
     *         {@link HandAction#ACTION_RAISE}
     */
    static int determineBetOrRaise(int inputMode) {
        return (inputMode == PokerTableInput.MODE_CHECK_BET) ? HandAction.ACTION_BET : HandAction.ACTION_RAISE;
    }

    /**
     * Determines whether the player action is a check or a call based on the
     * current input mode.
     *
     * @param inputMode
     *            the current input mode
     * @return {@link HandAction#ACTION_CHECK} if mode is
     *         {@link PokerTableInput#MODE_CHECK_BET} or
     *         {@link PokerTableInput#MODE_CHECK_RAISE}, otherwise
     *         {@link HandAction#ACTION_CALL}
     */
    static int determineCheckOrCall(int inputMode) {
        return (inputMode == PokerTableInput.MODE_CHECK_BET || inputMode == PokerTableInput.MODE_CHECK_RAISE)
                ? HandAction.ACTION_CHECK
                : HandAction.ACTION_CALL;
    }
}
