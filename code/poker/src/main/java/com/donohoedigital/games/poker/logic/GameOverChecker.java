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

import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.PokerTable;

/**
 * Game-over checking logic extracted from CheckEndHand.java. Contains pure
 * business logic for determining if a tournament/game is over with no UI
 * dependencies. Part of Wave 2 testability refactoring.
 *
 * <p>
 * Extracted logic:
 * <ul>
 * <li>Human player elimination detection</li>
 * <li>Rebuy eligibility checks</li>
 * <li>Single player remaining detection</li>
 * <li>Game-over state determination</li>
 * </ul>
 */
public class GameOverChecker {

    /**
     * Game-over result states.
     */
    public enum GameOverResult {
        /** Game continues - no game-over condition */
        CONTINUE,

        /** Human player is broke but can rebuy */
        REBUY_OFFERED,

        /** Human player is eliminated - game over for human */
        GAME_OVER,

        /** Only one player remains - tournament complete */
        TOURNAMENT_WON,

        /** Human is broke but "never broke" cheat is active */
        NEVER_BROKE_ACTIVE
    }

    // Utility class - no instantiation
    private GameOverChecker() {
    }

    /**
     * Check if the human player should be offered a rebuy.
     *
     * <p>
     * Extracted from CheckEndHand.java lines 86-87.
     *
     * @param human
     *            the human player
     * @param table
     *            current poker table
     * @return true if rebuy should be offered
     */
    public static boolean shouldOfferRebuy(PokerPlayer human, PokerTable table) {
        return human.getChipCount() == 0 && !human.isObserver() && table.isRebuyAllowed(human);
    }

    /**
     * Check if the human player is broke (eliminated).
     *
     * @param human
     *            the human player
     * @return true if player has no chips and is not an observer
     */
    public static boolean isHumanBroke(PokerPlayer human) {
        return human.getChipCount() == 0 && !human.isObserver();
    }

    /**
     * Determine the game-over status for a non-online game.
     *
     * <p>
     * Extracted from CheckEndHand.java isGameOver() method. This method separates
     * the decision logic from UI presentation (dialogs, etc.).
     *
     * <p>
     * Logic flow:
     * <ol>
     * <li>If human is broke and can rebuy → REBUY_OFFERED</li>
     * <li>If human is broke and cannot rebuy → GAME_OVER</li>
     * <li>If only one player left → TOURNAMENT_WON</li>
     * <li>Otherwise → CONTINUE</li>
     * </ol>
     *
     * @param game
     *            the poker game
     * @param human
     *            the human player
     * @param table
     *            current poker table
     * @param neverBrokeCheatActive
     *            true if "never broke" cheat option is enabled
     * @return game-over result indicating what action to take
     */
    public static GameOverResult checkGameOverStatus(PokerGame game, PokerPlayer human, PokerTable table,
            boolean neverBrokeCheatActive) {
        boolean bOnline = game.isOnlineGame();

        // If human player has no more chips, check rebuy/game-over status
        if (isHumanBroke(human)) {
            // If player can rebuy, offer them the chance
            if (shouldOfferRebuy(human, table)) {
                return GameOverResult.REBUY_OFFERED;
            }
            // Online game-over handled differently (by TournamentDirector)
            else if (!bOnline) {
                // Check for "never broke" cheat
                if (neverBrokeCheatActive) {
                    return GameOverResult.NEVER_BROKE_ACTIVE;
                }
                return GameOverResult.GAME_OVER;
            }
        }

        // See if only one player left with chips
        if (game.isOnePlayerLeft()) {
            return GameOverResult.TOURNAMENT_WON;
        }

        // Game continues
        return GameOverResult.CONTINUE;
    }

    /**
     * Calculate chip transfer amount for "never broke" cheat. Human gets half of
     * chip leader's stack, rounded to table min chip.
     *
     * <p>
     * Extracted from CheckEndHand.java lines 104-108.
     *
     * @param chipLeaderAmount
     *            chip count of the current chip leader
     * @param tableMinChip
     *            minimum chip denomination at the table
     * @return amount to transfer to human (rounded to min chip)
     */
    public static int calculateNeverBrokeTransfer(int chipLeaderAmount, int tableMinChip) {
        int nAdd = chipLeaderAmount / 2;
        nAdd -= nAdd % tableMinChip; // Round down to multiple of min chip
        return nAdd;
    }
}
