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
 * Level transition logic extracted from NewLevelActions.java. Contains pure
 * business logic for level changes, breaks, rebuys, and addons with no UI
 * dependencies. Part of Wave 2 testability refactoring.
 *
 * <p>
 * Extracted logic:
 * <ul>
 * <li>Message key selection for break notifications</li>
 * <li>Message key selection for level notifications</li>
 * <li>Rebuy/addon eligibility checks</li>
 * </ul>
 */
public class LevelTransitionLogic {

    // Utility class - no instantiation
    private LevelTransitionLogic() {
    }

    /**
     * Message key context for level transitions. Immutable value object.
     */
    public static class MessageKeyContext {
        private final boolean isBreak;
        private final boolean isOnline;
        private final boolean hasAnte;

        public MessageKeyContext(boolean isBreak, boolean isOnline, boolean hasAnte) {
            this.isBreak = isBreak;
            this.isOnline = isOnline;
            this.hasAnte = hasAnte;
        }

        public boolean isBreak() {
            return isBreak;
        }

        public boolean isOnline() {
            return isOnline;
        }

        public boolean hasAnte() {
            return hasAnte;
        }
    }

    /**
     * Determine the message key for break notification.
     *
     * <p>
     * Extracted from NewLevelActions.java lines 88-90.
     *
     * <p>
     * Online games use chat message key, practice games use dialog message key.
     *
     * @param isOnline
     *            true if online game
     * @return message key for break notification
     */
    public static String determineBreakMessageKey(boolean isOnline) {
        return isOnline ? "msg.chat.break" : "msg.dialog.break";
    }

    /**
     * Determine the message key for new level notification.
     *
     * <p>
     * Extracted from NewLevelActions.java lines 100-109.
     *
     * <p>
     * Message key varies based on:
     * <ul>
     * <li>Online vs practice game (chat vs dialog)</li>
     * <li>Whether level has an ante (affects message template)</li>
     * </ul>
     *
     * @param isOnline
     *            true if online game
     * @param hasAnte
     *            true if next level has an ante
     * @return message key for level notification
     */
    public static String determineLevelMessageKey(boolean isOnline, boolean hasAnte) {
        if (isOnline) {
            return hasAnte ? "msg.chat.next.ante" : "msg.chat.next";
        } else {
            return hasAnte ? "msg.dialog.next.ante" : "msg.dialog.next";
        }
    }

    /**
     * Determine the appropriate message key for a level transition.
     *
     * <p>
     * Combines break and level message key logic into a single method.
     *
     * @param context
     *            message key context with transition details
     * @return appropriate message key
     */
    public static String determineTransitionMessageKey(MessageKeyContext context) {
        if (context.isBreak) {
            return determineBreakMessageKey(context.isOnline);
        } else {
            return determineLevelMessageKey(context.isOnline, context.hasAnte);
        }
    }

    /**
     * Check if a player is eligible for rebuy.
     *
     * <p>
     * Extracted from NewLevelActions.java lines 156-157.
     *
     * <p>
     * Safety check to prevent rebuy for players who are observers or already
     * eliminated. This can happen if rebuy button is pressed/triggered before it
     * can be removed from UI.
     *
     * @param isObserver
     *            true if player is an observer
     * @param isEliminated
     *            true if player is eliminated
     * @return true if player is eligible for rebuy
     */
    public static boolean isPlayerEligibleForRebuy(boolean isObserver, boolean isEliminated) {
        return !isObserver && !isEliminated;
    }
}
