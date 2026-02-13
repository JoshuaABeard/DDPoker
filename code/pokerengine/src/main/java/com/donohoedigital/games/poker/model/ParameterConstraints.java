/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.model;

import com.donohoedigital.comms.*;

/**
 * Calculates tournament parameter constraints and limits.
 *
 * <p>
 * Encapsulates business rules for valid parameter ranges:
 * <ul>
 * <li>Maximum payout spots based on player count
 * <li>Maximum payout percentage based on player count
 * <li>Maximum online players
 * <li>Maximum raises (with special heads-up handling)
 * <li>Maximum rebuys
 * <li>Maximum observers
 * </ul>
 *
 * <p>
 * Extracted from TournamentProfile to improve testability.
 *
 * @see TournamentProfile#getMaxPayoutSpots(int)
 * @see TournamentProfile#getMaxPayoutPercent(int)
 */
public class ParameterConstraints {

    private final DMTypedHashMap map;

    /** Parameter keys (match TournamentProfile constants). */
    private static final String PARAM_MAXRAISES = "maxraises";
    private static final String PARAM_MAXRAISES_NONE_HEADSUP = "maxraisesnoneheadsup";
    private static final String PARAM_MAXREBUYS = "maxrebuys";
    private static final String PARAM_MAX_OBSERVERS = "maxobs";
    private static final String PARAM_NUMPLAYERS = "numplayers";

    /**
     * Create a parameter constraints calculator.
     *
     * @param map
     *            The map containing tournament configuration data
     */
    public ParameterConstraints(DMTypedHashMap map) {
        this.map = map;
    }

    /**
     * Get max number of payout spots for given number of players.
     *
     * <p>
     * Extracted from TournamentProfile.getMaxPayoutSpots() (lines 893-902).
     *
     * @param numPlayers
     *            Number of players in tournament
     * @return Maximum allowed payout spots
     */
    public int getMaxPayoutSpots(int numPlayers) {
        int nMax = (int) (numPlayers * TournamentProfile.MAX_SPOTS_PERCENT);
        nMax = Math.min(nMax, TournamentProfile.MAX_SPOTS);
        if (nMax < TournamentProfile.MIN_SPOTS)
            nMax = TournamentProfile.MIN_SPOTS;
        if (nMax > numPlayers)
            nMax = numPlayers;

        return nMax;
    }

    /**
     * Get max percentage of spots.
     *
     * <p>
     * Extracted from TournamentProfile.getMaxPayoutPercent() (lines 907-914).
     *
     * @param numPlayers
     *            Number of players in tournament
     * @return Maximum allowed payout percentage
     */
    public int getMaxPayoutPercent(int numPlayers) {
        int nMax = 0;
        if (numPlayers > 0) {
            nMax = (Math.min(TournamentProfile.MAX_SPOTS, getMaxPayoutSpots(numPlayers))) * 100 / numPlayers;
        }

        return nMax;
    }

    /**
     * Get maximum online players.
     *
     * <p>
     * Extracted from TournamentProfile.getMaxOnlinePlayers() (lines 295-297).
     *
     * @param numPlayers
     *            Configured number of players
     * @return Maximum online players (capped at MAX_ONLINE_PLAYERS)
     */
    public int getMaxOnlinePlayers(int numPlayers) {
        return Math.min(numPlayers, TournamentProfile.MAX_ONLINE_PLAYERS);
    }

    /**
     * Get maximum raises for a betting round.
     *
     * <p>
     * Extracted from TournamentProfile.getMaxRaises() (lines 701-711).
     *
     * @param numWithCards
     *            Number of players with cards
     * @param isComputer
     *            Whether this is for a computer player
     * @param raiseCapIgnoredHeadsUp
     *            Whether raise cap is ignored when heads-up
     * @return Maximum allowed raises
     */
    public int getMaxRaises(int numWithCards, boolean isComputer, boolean raiseCapIgnoredHeadsUp) {
        if (numWithCards <= 2 && raiseCapIgnoredHeadsUp) {
            // cap ai players at 4 so they don't raise each other indefinitely
            if (isComputer)
                return TournamentProfile.MAX_AI_RAISES;
            return Integer.MAX_VALUE;
        }

        int nMax = (isComputer) ? TournamentProfile.MAX_AI_RAISES : TournamentProfile.MAX_MAX_RAISES;
        return map.getInteger(PARAM_MAXRAISES, 3, 1, nMax);
    }

    /**
     * Get maximum number of rebuys.
     *
     * <p>
     * Extracted from TournamentProfile.getMaxRebuys() (lines 1148-1150).
     *
     * @return Maximum allowed rebuys
     */
    public int getMaxRebuys() {
        return map.getInteger(PARAM_MAXREBUYS, 0, 0, TournamentProfile.MAX_REBUYS);
    }

    /**
     * Get maximum number of observers.
     *
     * <p>
     * Extracted from TournamentProfile.getMaxObservers() (lines 1204-1206).
     *
     * @return Maximum allowed observers
     */
    public int getMaxObservers() {
        return map.getInteger(PARAM_MAX_OBSERVERS, 5, 0, TournamentProfile.MAX_OBSERVERS);
    }
}
