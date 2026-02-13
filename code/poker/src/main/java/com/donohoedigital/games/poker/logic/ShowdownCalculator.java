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

/**
 * Showdown display decision logic extracted from Showdown.java. Contains pure
 * business logic for determining card visibility and result types with no UI
 * dependencies. Part of Wave 2 testability refactoring.
 *
 * <p>
 * Extracted logic:
 * <ul>
 * <li>Card visibility decisions based on game state and options</li>
 * <li>Hand type display decisions</li>
 * <li>Result type determination (win/lose/overbet/allin)</li>
 * </ul>
 */
public class ShowdownCalculator {

    // Utility class - no instantiation
    private ShowdownCalculator() {
    }

    /**
     * Result type for showdown display.
     */
    public enum ResultType {
        WIN, LOSE, OVERBET, ALLIN
    }

    /**
     * Immutable context object for card visibility decisions. Encapsulates all the
     * factors that determine whether cards should be shown.
     */
    public static class VisibilityContext {
        private final boolean cardsExposed;
        private final boolean uncontested;
        private final boolean showMuck;
        private final boolean won;
        private final boolean showWin;
        private final boolean isHuman;
        private final boolean isLocallyControlled;
        private final boolean humanUpEnabled;
        private final boolean isComputer;
        private final boolean aiFaceUpEnabled;

        public VisibilityContext(boolean cardsExposed, boolean uncontested, boolean showMuck, boolean won,
                boolean showWin, boolean isHuman, boolean isLocallyControlled, boolean humanUpEnabled,
                boolean isComputer, boolean aiFaceUpEnabled) {
            this.cardsExposed = cardsExposed;
            this.uncontested = uncontested;
            this.showMuck = showMuck;
            this.won = won;
            this.showWin = showWin;
            this.isHuman = isHuman;
            this.isLocallyControlled = isLocallyControlled;
            this.humanUpEnabled = humanUpEnabled;
            this.isComputer = isComputer;
            this.aiFaceUpEnabled = aiFaceUpEnabled;
        }

        public boolean isCardsExposed() {
            return cardsExposed;
        }

        public boolean isUncontested() {
            return uncontested;
        }

        public boolean isShowMuck() {
            return showMuck;
        }

        public boolean isWon() {
            return won;
        }

        public boolean isShowWin() {
            return showWin;
        }

        public boolean isHuman() {
            return isHuman;
        }

        public boolean isLocallyControlled() {
            return isLocallyControlled;
        }

        public boolean isHumanUpEnabled() {
            return humanUpEnabled;
        }

        public boolean isComputer() {
            return isComputer;
        }

        public boolean isAiFaceUpEnabled() {
            return aiFaceUpEnabled;
        }
    }

    /**
     * Determine whether cards should be shown at showdown.
     *
     * <p>
     * Extracted from Showdown.java lines 184-186.
     *
     * <p>
     * Cards are shown if: cards were exposed during hand, cheat options are enabled
     * (show muck/show winning), or based on player type (human with cards up, AI
     * with face-up option).
     *
     * @param context
     *            visibility decision context
     * @return true if cards should be displayed
     */
    public static boolean shouldShowCards(VisibilityContext context) {
        return context.isCardsExposed() || (!context.isUncontested() && (context.isShowMuck() && !context.isWon()))
                || (context.isShowWin() && context.isWon())
                || (context.isHuman() && context.isLocallyControlled() && context.isHumanUpEnabled())
                || (context.isComputer() && context.isAiFaceUpEnabled());
    }

    /**
     * Determine whether hand type should be displayed.
     *
     * <p>
     * Extracted from Showdown.java lines 188-196.
     *
     * <p>
     * Hand type shown based on base decision, but with exceptions for human players
     * with rabbit hunt enabled, or players showing winning hands in uncontested
     * pots.
     *
     * @param showHandTypeBase
     *            base decision from calculateBaseShowHandType()
     * @param isHuman
     *            true if player is human
     * @param rabbitHunt
     *            true if rabbit hunt is enabled
     * @param uncontested
     *            true if pot was uncontested
     * @param showingWinningHand
     *            true if player is showing winning hand
     * @param seenRiver
     *            true if river was seen
     * @return true if hand type should be displayed
     */
    public static boolean shouldShowHandType(boolean showHandTypeBase, boolean isHuman, boolean rabbitHunt,
            boolean uncontested, boolean showingWinningHand, boolean seenRiver) {
        if (showHandTypeBase) {
            return true;
        }
        // human cards always known, so show if rabbit hunt enabled
        if (isHuman && rabbitHunt) {
            return true;
        }
        // uncontested, but winning player is showing hand
        if (uncontested && showingWinningHand && (rabbitHunt || seenRiver)) {
            return true;
        }
        return false;
    }

    /**
     * Determine result type based on amounts won.
     *
     * <p>
     * Extracted from Showdown.java lines 213-221.
     *
     * <p>
     * Result types: LOSE if nothing won, OVERBET if only overbet returned, WIN if
     * won chips from pot.
     *
     * @param amountWon
     *            amount won from pot
     * @param amountOverbet
     *            overbet amount returned
     * @return result type for display
     */
    public static ResultType determineResultType(int amountWon, int amountOverbet) {
        int total = amountWon + amountOverbet;
        if (total == 0) {
            return ResultType.LOSE;
        } else if (amountOverbet == total) {
            return ResultType.OVERBET;
        } else {
            return ResultType.WIN;
        }
    }

    /**
     * Determine result type for all-in showdown display.
     *
     * <p>
     * Extracted from Showdown.java lines 273-275.
     *
     * <p>
     * Players with max win amount shown as WIN, others as ALLIN.
     *
     * @param playerWinAmount
     *            player's win amount
     * @param maxWinAmount
     *            maximum win amount across all players
     * @return result type for all-in display
     */
    public static ResultType determineAllInResultType(int playerWinAmount, int maxWinAmount) {
        if (playerWinAmount == maxWinAmount) {
            return ResultType.WIN;
        } else {
            return ResultType.ALLIN;
        }
    }

    /**
     * Calculate base decision for showing hand types.
     *
     * <p>
     * Extracted from Showdown.java lines 109-110.
     *
     * <p>
     * Base rule: show hand types unless pot was uncontested AND neither rabbit hunt
     * nor river was seen.
     *
     * @param uncontested
     *            true if pot was uncontested
     * @param seenRiver
     *            true if river was seen
     * @param rabbitHunt
     *            true if rabbit hunt option enabled
     * @param showWin
     *            true if show winning hand option enabled
     * @return true if hand types should be shown by default
     */
    public static boolean calculateBaseShowHandType(boolean uncontested, boolean seenRiver, boolean rabbitHunt,
            boolean showWin) {
        if (!uncontested) {
            return true;
        }
        return (rabbitHunt || seenRiver) && showWin;
    }

    /**
     * Calculate decision for showing hand types on folded hands.
     *
     * <p>
     * Extracted from Showdown.java line 110.
     *
     * <p>
     * Similar to base calculation but for players who folded.
     *
     * @param uncontested
     *            true if pot was uncontested
     * @param seenRiver
     *            true if river was seen
     * @param rabbitHunt
     *            true if rabbit hunt option enabled
     * @return true if folded hand types should be shown
     */
    public static boolean calculateShowHandTypeFold(boolean uncontested, boolean seenRiver, boolean rabbitHunt) {
        if (!uncontested) {
            return true;
        }
        return rabbitHunt || seenRiver;
    }
}
