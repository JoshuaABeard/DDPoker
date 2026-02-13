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

import com.donohoedigital.games.poker.PokerPlayer;

import java.util.List;

/**
 * Color-up decision logic extracted from ColorUpFinish.java. Contains pure
 * business logic for chip color-up decisions with no UI dependencies. Part of
 * Wave 2 testability refactoring.
 *
 * <p>
 * Extracted logic:
 * <ul>
 * <li>Pause decision during color-up process</li>
 * <li>Phase 1 skip decision based on odd chips</li>
 * <li>Player odd chips counting</li>
 * </ul>
 */
public class ColorUpLogic {

    // Utility class - no instantiation
    private ColorUpLogic() {
    }

    /**
     * Determine whether to pause during color-up process.
     *
     * <p>
     * Extracted from ColorUpFinish.java lines 83-84.
     *
     * <p>
     * Pausing allows user to step through color-up phases in practice games. Online
     * games and autopilot mode never pause.
     *
     * @param isAutopilot
     *            true if autopilot testing mode is active
     * @param isOnlineGame
     *            true if this is an online game
     * @param pauseOptionEnabled
     *            true if user has enabled pause-on-color-up option
     * @return true if should pause for user input
     */
    public static boolean shouldPauseColorUp(boolean isAutopilot, boolean isOnlineGame, boolean pauseOptionEnabled) {
        return !isAutopilot && !isOnlineGame && pauseOptionEnabled;
    }

    /**
     * Determine whether to skip phase 1 of color-up.
     *
     * <p>
     * Extracted from ColorUpFinish.java lines 103-105.
     *
     * <p>
     * Phase 1 shows high card dealing for chip race. Skip if no players have odd
     * chips (can happen if game saved after phase 1 but before phase 2).
     *
     * @param numPlayersWithOddChips
     *            number of players with odd chips
     * @return true if phase 1 should be skipped
     */
    public static boolean shouldSkipPhase1(int numPlayersWithOddChips) {
        return numPlayersWithOddChips == 0;
    }

    /**
     * Count number of players with odd chips.
     *
     * <p>
     * Extracted from ColorUpFinish.java getNumWithOddChips() method (lines
     * 121-132).
     *
     * <p>
     * Odd chips are chips that don't evenly divide into the next chip denomination
     * during color-up. Players with odd chips participate in the chip race.
     *
     * @param players
     *            list of players to check (may contain nulls)
     * @return number of players with odd chips greater than 0
     */
    public static int countPlayersWithOddChips(List<PokerPlayer> players) {
        int count = 0;
        for (PokerPlayer player : players) {
            if (player != null && player.getOddChips() > 0) {
                count++;
            }
        }
        return count;
    }
}
