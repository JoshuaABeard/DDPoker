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
 * Tournament level timing and progression logic extracted from
 * TournamentDirector.java. Contains pure business logic for level transitions,
 * breaks, and chip color-ups with no UI dependencies. Part of Wave 3
 * testability refactoring.
 *
 * <p>
 * Extracted logic:
 * <ul>
 * <li>Level change detection</li>
 * <li>Color-up necessity determination</li>
 * <li>All-computer table synchronization decisions</li>
 * </ul>
 */
public class TournamentClock {

    // Utility class - no instantiation
    private TournamentClock() {
    }

    /**
     * Determine if table has advanced to a new level.
     *
     * <p>
     * Extracted from TournamentDirector.doNewLevelCheck() lines 1835-1850.
     *
     * <p>
     * Tables must check if game level has advanced beyond table's current level.
     * When true, table needs rebuy/addon processing.
     *
     * @param gameLevel
     *            current game level
     * @param tableLevel
     *            current table level
     * @return true if table needs to advance to new level
     */
    public static boolean hasLevelChanged(int gameLevel, int tableLevel) {
        return gameLevel != tableLevel;
    }

    /**
     * Determine if chip color-up is needed.
     *
     * <p>
     * Extracted from TournamentDirector.doColorUp() lines 1878-1889.
     *
     * <p>
     * Color-up occurs when minimum chip denomination increases. This consolidates
     * smaller chips into larger denominations.
     *
     * @param previousMinChip
     *            previous minimum chip value
     * @param currentMinChip
     *            current minimum chip value
     * @return true if color-up is needed
     */
    public static boolean shouldColorUp(int previousMinChip, int currentMinChip) {
        return currentMinChip > previousMinChip;
    }

    /**
     * Determine if all-computer tables should process level change.
     *
     * <p>
     * Extracted from TournamentDirector.doLevelCheckAllComputers() lines 1856-1866.
     *
     * <p>
     * All-computer tables sync with current table for level changes (rebuy/addon).
     * Only host processes all-computer tables, and only when current table
     * advances.
     *
     * @param isHost
     *            true if running on host
     * @param isCurrentTable
     *            true if checking current table
     * @return true if should process all-computer table level changes
     */
    public static boolean shouldProcessAllComputerLevelCheck(boolean isHost, boolean isCurrentTable) {
        return isHost && isCurrentTable;
    }

    /**
     * Determine if all-computer tables should process color-up.
     *
     * <p>
     * Extracted from TournamentDirector.doColorUpAllComputers() lines 1901-1913.
     *
     * <p>
     * Similar to level check, but for chip color-up synchronization.
     *
     * @param isHost
     *            true if running on host
     * @param isCurrentTable
     *            true if checking current table
     * @param isColoringUp
     *            true if current table is coloring up
     * @return true if should process all-computer table color-ups
     */
    public static boolean shouldProcessAllComputerColorUp(boolean isHost, boolean isCurrentTable,
            boolean isColoringUp) {
        return isHost && isCurrentTable && isColoringUp;
    }

    /**
     * Determine if break level has ended.
     *
     * <p>
     * Helper method for break timing logic.
     *
     * @param currentLevel
     *            current level number
     * @param isBreakLevel
     *            true if current level is a break
     * @param hasLevelChanged
     *            true if level has changed
     * @return true if break has ended
     */
    public static boolean hasBreakEnded(int currentLevel, boolean isBreakLevel, boolean hasLevelChanged) {
        // Break ends when level changes and previous level was a break
        return hasLevelChanged && isBreakLevel;
    }

    /**
     * Calculate time remaining until level change.
     *
     * <p>
     * Helper method for level timing calculations.
     *
     * @param levelDurationSeconds
     *            duration of current level in seconds
     * @param elapsedSeconds
     *            seconds elapsed in current level
     * @return seconds remaining in current level (minimum 0)
     */
    public static int calculateTimeRemaining(int levelDurationSeconds, int elapsedSeconds) {
        int remaining = levelDurationSeconds - elapsedSeconds;
        return Math.max(0, remaining);
    }

    /**
     * Determine if level should automatically advance.
     *
     * <p>
     * Helper method for automated level progression.
     *
     * @param timeRemaining
     *            seconds remaining in level
     * @return true if time has expired and level should advance
     */
    public static boolean shouldAdvanceLevel(int timeRemaining) {
        return timeRemaining <= 0;
    }
}
