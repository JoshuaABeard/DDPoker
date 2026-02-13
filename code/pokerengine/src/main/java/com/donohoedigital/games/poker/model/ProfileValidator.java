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
import com.donohoedigital.games.poker.engine.*;
import java.text.*;

/**
 * Validates and normalizes tournament profile settings.
 *
 * <p>
 * Handles constraint enforcement when tournament parameters change:
 * <ul>
 * <li>Adjusts payout spots when player count changes
 * <li>Cleans up allocation entries
 * <li>Fixes rebuy expression edge cases
 * </ul>
 *
 * <p>
 * Extracted from TournamentProfile to improve testability.
 *
 * @see TournamentProfile#updateNumPlayers(int)
 * @see TournamentProfile#fixAll()
 */
public class ProfileValidator {

    private final DMTypedHashMap map;
    private final ValidationCallbacks callbacks;

    /** Formatting for payout amounts and percentages. */
    private static final MessageFormat FORMAT_AMOUNT = new MessageFormat("{0,number,#}");
    private static final MessageFormat FORMAT_PERC = new MessageFormat("{0,number,#.##}");

    /** Parameter keys (match TournamentProfile constants). */
    private static final String PARAM_PAYOUT = "payout";
    private static final String PARAM_PAYOUTSPOTS = "payoutspots";
    private static final String PARAM_PAYOUTPERC = "payoutperc";
    private static final String PARAM_SPOTAMOUNT = "spotamount";
    private static final String PARAM_ALLOC = "alloc";
    private static final String PARAM_NUMPLAYERS = "numplayers";
    private static final String PARAM_REBUYCHIPS = "rebuychips";
    private static final String PARAM_REBUYEXPR = "rebuyexpr";

    /**
     * Callbacks for operations that require TournamentProfile methods.
     */
    public interface ValidationCallbacks {
        int getMaxPayoutSpots(int numPlayers);

        int getMaxPayoutPercent(int numPlayers);

        boolean isAllocAuto();

        boolean isAllocFixed();

        boolean isAllocPercent();

        boolean isAllocSatellite();

        void setAutoSpots();

        void fixLevels();

        int getNumSpots();

        double getSpot(int position);
    }

    /**
     * Create a profile validator wrapper around the given map.
     *
     * @param map
     *            The map containing tournament configuration data
     * @param callbacks
     *            Callbacks for operations requiring TournamentProfile methods
     */
    public ProfileValidator(DMTypedHashMap map, ValidationCallbacks callbacks) {
        this.map = map;
        this.callbacks = callbacks;
    }

    /**
     * Update number of players, adjusting payout spots if necessary.
     *
     * <p>
     * Extracted from TournamentProfile.updateNumPlayers() (lines 709-749).
     *
     * @param nNumPlayers
     *            New number of players
     */
    public void updateNumPlayers(int nNumPlayers) {
        boolean bChange = false;

        int nType = map.getInteger(PARAM_PAYOUT, PokerConstants.PAYOUT_SPOTS);
        if (nType == PokerConstants.PAYOUT_PERC) {
            int spot = map.getInteger(PARAM_PAYOUTPERC, 0);
            int max = callbacks.getMaxPayoutPercent(nNumPlayers);
            if (spot > max) {
                bChange = true;
                map.setInteger(PARAM_PAYOUTPERC, max);
            }
        } else if (nType == PokerConstants.PAYOUT_SPOTS) {
            int spot = map.getInteger(PARAM_PAYOUTSPOTS, 0);
            int max = callbacks.getMaxPayoutSpots(nNumPlayers);
            if (spot > max) {
                bChange = true;
                map.setInteger(PARAM_PAYOUTSPOTS, max);
            }
        } else // PokerConstants.PAYOUT_SATELLITE
        {
            // no need to update if num players change
        }

        // store new num players
        map.setInteger(PARAM_NUMPLAYERS, nNumPlayers);

        // if a change in payout spots occurred, update
        if (bChange) {
            if (callbacks.isAllocFixed() || callbacks.isAllocPercent()) {
                map.setInteger(PARAM_ALLOC, PokerConstants.ALLOC_AUTO);
            }
        }

        // if auto alloc, update spots
        if (callbacks.isAllocAuto()) {
            callbacks.setAutoSpots();
        }

        // fix all (html may have changed, remove old spots)
        fixAll();
    }

    /**
     * Fix all validation issues.
     *
     * <p>
     * Extracted from TournamentProfile.fixAll() (lines 1419-1427).
     */
    public void fixAll() {
        callbacks.fixLevels();
        fixAllocs();

        // rebuys if < 0, change to <=
        if (map.getInteger(PARAM_REBUYEXPR, PokerConstants.REBUY_LT) == PokerConstants.REBUY_LT
                && map.getInteger(PARAM_REBUYCHIPS, 0) == 0) {
            map.setInteger(PARAM_REBUYEXPR, PokerConstants.REBUY_LTE);
        }
    }

    /**
     * Clean up allocation entries.
     *
     * <p>
     * Extracted from TournamentProfile.fixAllocs() (lines 1316-1336).
     */
    public void fixAllocs() {
        int nNumSpots = callbacks.getNumSpots();
        if (callbacks.isAllocSatellite())
            nNumSpots = 1; // only need 1 entry for satellite
        double d;
        String s;
        for (int i = 1; i <= nNumSpots; i++) {
            d = callbacks.getSpot(i);
            if (callbacks.isAllocPercent()) {
                s = FORMAT_PERC.format(new Object[]{d});
            } else {
                s = FORMAT_AMOUNT.format(new Object[]{(int) d});
            }
            map.setString(PARAM_SPOTAMOUNT + i, s);
        }

        // BUG 315 - clear out other spots
        for (int i = nNumSpots + 1; i <= TournamentProfile.MAX_SPOTS; i++) {
            map.removeString(PARAM_SPOTAMOUNT + i);
        }
    }
}
