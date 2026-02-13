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

import com.donohoedigital.games.poker.HoldemHand;

/**
 * Hand lifecycle orchestration logic extracted from TournamentDirector.java.
 * Contains pure business logic for hand state transitions and phase decisions
 * with no UI dependencies. Part of Wave 3 testability refactoring.
 *
 * <p>
 * Extracted logic:
 * <ul>
 * <li>Next betting state determination based on hand status</li>
 * <li>Community card phase decisions</li>
 * <li>Player action type determination</li>
 * <li>Hand completion status checks</li>
 * </ul>
 */
public class HandOrchestrator {

    // Utility class - no instantiation
    private HandOrchestrator() {
    }

    /**
     * Player action type for betting decisions.
     */
    public enum PlayerActionType {
        /** Player sitting out - should auto-fold */
        SITTING_OUT,
        /** Locally controlled player on current table - run Bet phase */
        LOCAL_CURRENT_TABLE,
        /** Computer player on non-current table - handle locally */
        COMPUTER_OTHER_TABLE,
        /** Remote player - wait for client action */
        REMOTE
    }

    /**
     * Determine next table state after betting completes.
     *
     * <p>
     * Extracted from TournamentDirector.nextBettingState() lines 2107-2117.
     *
     * <p>
     * State transitions:
     * <ul>
     * <li>If betting not done: STATE_BETTING (continue betting)</li>
     * <li>If river round complete: STATE_PRE_SHOWDOWN</li>
     * <li>Otherwise: STATE_COMMUNITY (deal next card)</li>
     * </ul>
     *
     * @param handDone
     *            true if hand betting is complete
     * @param currentRound
     *            current round (FLOP, TURN, RIVER, etc.)
     * @param riverRound
     *            constant for river round
     * @param stateBetting
     *            constant for betting state
     * @param statePreShowdown
     *            constant for pre-showdown state
     * @param stateCommunity
     *            constant for community card state
     * @return next table state constant
     */
    public static int determineNextBettingState(boolean handDone, int currentRound, int riverRound, int stateBetting,
            int statePreShowdown, int stateCommunity) {

        if (!handDone) {
            return stateBetting;
        }

        if (currentRound == riverRound) {
            return statePreShowdown;
        }

        return stateCommunity;
    }

    /**
     * Determine if DealCommunity phase should run.
     *
     * <p>
     * Extracted from TournamentDirector.doCommunity() lines 2139-2145.
     *
     * <p>
     * DealCommunity runs in practice mode (for cheat options) or in online mode
     * when multiple players remain (for card animations).
     *
     * @param isOnlineGame
     *            true if online game
     * @param numWithCards
     *            number of players still in hand
     * @return true if DealCommunity phase should run
     */
    public static boolean shouldRunDealCommunityPhase(boolean isOnlineGame, int numWithCards) {
        if (!isOnlineGame) {
            // Practice mode - always run (for cheat options/card visibility)
            return true;
        } else {
            // Online mode - only run if multiple players (for animations)
            return numWithCards > 1;
        }
    }

    /**
     * Determine player action type for betting orchestration.
     *
     * <p>
     * Extracted from TournamentDirector.doBetting() lines 2027-2071.
     *
     * <p>
     * Determines how player's action should be handled based on player state and
     * table context.
     *
     * @param isSittingOut
     *            true if player is sitting out
     * @param isLocallyControlled
     *            true if player is locally controlled (host or AI)
     * @param isCurrentTable
     *            true if this is the current UI table
     * @param isComputer
     *            true if player is computer
     * @param isHost
     *            true if running on host
     * @return player action type
     */
    public static PlayerActionType determinePlayerActionType(boolean isSittingOut, boolean isLocallyControlled,
            boolean isCurrentTable, boolean isComputer, boolean isHost) {

        if (isSittingOut) {
            return PlayerActionType.SITTING_OUT;
        }

        if (isLocallyControlled) {
            if (isCurrentTable) {
                return PlayerActionType.LOCAL_CURRENT_TABLE;
            } else {
                return PlayerActionType.COMPUTER_OTHER_TABLE;
            }
        }

        // Remote player (only relevant on host)
        if (isHost) {
            return PlayerActionType.REMOTE;
        }

        // Should not reach here in normal operation
        throw new IllegalStateException("Cannot determine action type: sittingOut=" + isSittingOut
                + ", locallyControlled=" + isLocallyControlled + ", currentTable=" + isCurrentTable + ", computer="
                + isComputer + ", host=" + isHost);
    }

    /**
     * Determine if hand is complete (betting done).
     *
     * <p>
     * Wrapper for HoldemHand.isDone() for testability.
     *
     * @param hand
     *            holdem hand to check
     * @return true if hand betting is complete
     */
    public static boolean isHandComplete(HoldemHand hand) {
        return hand != null && hand.isDone();
    }

    /**
     * Determine if all-computer tables should process betting.
     *
     * <p>
     * Extracted logic from TournamentDirector.doBettingAllComputer() lines
     * 2089-2093.
     *
     * <p>
     * All-computer tables sync hand-for-hand with the current table (practice) or
     * host's table (online). Only process if table is in betting state.
     *
     * @param isHost
     *            true if running on host
     * @param isCurrentTable
     *            true if checking current table
     * @param tableState
     *            current table state
     * @param stateBetting
     *            constant for betting state
     * @return true if should process all-computer table betting
     */
    public static boolean shouldProcessAllComputerBetting(boolean isHost, boolean isCurrentTable, int tableState,
            int stateBetting) {

        // Only host processes all-computer tables
        if (!isHost) {
            return false;
        }

        // Only when current table is being processed
        if (!isCurrentTable) {
            return false;
        }

        // Only if table is in betting state (shortcut for subsequent calls)
        return tableState == stateBetting;
    }

    /**
     * Calculate appropriate pause duration for AI actions.
     *
     * <p>
     * Extracted from TournamentDirector.doBetting() line 2061.
     *
     * <p>
     * Pauses prevent overloading clients with messages on mixed human/AI tables.
     *
     * @param aiPauseTenths
     *            pause duration in tenths of a second
     * @return pause duration in milliseconds
     */
    public static int calculateAIPauseMillis(int aiPauseTenths) {
        return aiPauseTenths * 100;
    }
}
