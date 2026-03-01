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
 * Pure-logic calculations for showdown display decisions. Extracted from
 * {@link Showdown} for testability.
 */
class ShowdownCalculator {

    private ShowdownCalculator() {
    }

    /**
     * Determines whether a player's cards should be shown at showdown. Mirrors the
     * bShowCards logic in Showdown.displayShowdown().
     */
    static boolean shouldShowCards(boolean cardsExposed, boolean uncontested, boolean showMucked, boolean won,
            boolean showWinning, boolean isHuman, boolean locallyControlled, boolean humanCardsUp, boolean isComputer,
            boolean aiFaceUp) {
        return cardsExposed || (!uncontested && (showMucked && !won)) || (showWinning && won)
                || (isHuman && locallyControlled && humanCardsUp) || (isComputer && aiFaceUp);
    }

    /**
     * Determines whether the hand type description should be shown for a showdown
     * player (non-folded). Mirrors bShowHandTypeLocal logic in
     * Showdown.displayShowdown().
     *
     * @param uncontested
     *            whether the hand was uncontested (single winner)
     * @param showRiver
     *            whether the rabbit-hunt cheat is enabled
     * @param seenRiver
     *            whether river action actually occurred
     * @param showWinning
     *            whether the show-winning-hand cheat is enabled
     * @param isHuman
     *            whether the player is human
     * @param playerShowWinning
     *            whether the player is voluntarily showing their winning hand
     */
    static boolean shouldShowHandType(boolean uncontested, boolean showRiver, boolean seenRiver, boolean showWinning,
            boolean isHuman, boolean playerShowWinning) {
        // base: bShowHandType = !bUncontested || ((bShowRiver || bSeenRiver) &&
        // bShowWin)
        boolean base = !uncontested || ((showRiver || seenRiver) && showWinning);

        // human cards are always known to user, so show handtype if showing river
        if (isHuman && showRiver)
            return true;

        // uncontested, but winning player is showing hand
        if (uncontested && playerShowWinning && (showRiver || seenRiver))
            return true;

        return base;
    }

    /**
     * Determines whether the hand type description should be shown for a folded
     * player. Mirrors bShowHandTypeFold logic in Showdown.displayShowdown().
     */
    static boolean shouldShowHandTypeFold(boolean uncontested, boolean showRiver, boolean seenRiver) {
        return !uncontested || showRiver || seenRiver;
    }

    /**
     * Determines the result placard type for a player who reached showdown. Mirrors
     * the nResult logic in Showdown.displayShowdown().
     *
     * @param totalWon
     *            total amount returned to the player (win + overbet)
     * @param overbet
     *            the overbet portion of the total
     * @return one of {@link ResultsPiece#LOSE}, {@link ResultsPiece#OVERBET}, or
     *         {@link ResultsPiece#WIN}
     */
    static int determineResultType(int totalWon, int overbet) {
        if (totalWon == 0)
            return ResultsPiece.LOSE;
        if (overbet == totalWon)
            return ResultsPiece.OVERBET;
        return ResultsPiece.WIN;
    }
}
