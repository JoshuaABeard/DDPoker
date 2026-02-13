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
    private static final String PARAM_REBUYS = "rebuys";
    private static final String PARAM_REBUY_UNTIL = "rebuyuntil";
    private static final String PARAM_LASTLEVEL = "lastlevel";
    private static final String PARAM_BUYINCHIPS = "buyinchips";
    private static final String PARAM_BIG = "big";
    private static final String PARAM_HOUSE = "house";
    private static final String PARAM_HOUSEPERC = "houseperc";
    private static final String PARAM_HOUSEAMOUNT = "houseamount";
    private static final String PARAM_BUYIN = "buyin";

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

    /**
     * Validate tournament profile settings and return warnings.
     *
     * <p>
     * Checks for common configuration issues that don't prevent profile creation
     * but may lead to unexpected behavior:
     * <ul>
     * <li>Unreachable blind levels (rebuy period ends too early)
     * <li>Too many payout spots for player count
     * <li>Shallow starting chip depth (< 10 big blinds)
     * <li>Excessive house take (> 20% of buy-in)
     * </ul>
     *
     * @return ValidationResult containing any warnings found
     */
    public ValidationResult validateProfile() {
        ValidationResult result = new ValidationResult();

        checkUnreachableLevels(result);
        checkPayoutSpots(result);
        checkStartingDepth(result);
        checkHouseTake(result);

        return result;
    }

    /**
     * Check if rebuy period ends before all blind levels are reached.
     */
    private void checkUnreachableLevels(ValidationResult result) {
        boolean rebuysEnabled = map.getBoolean(PARAM_REBUYS, false);
        if (!rebuysEnabled) {
            return;
        }

        int lastLevel = map.getInteger(PARAM_LASTLEVEL, 0);
        int rebuyUntilLevel = map.getInteger(PARAM_REBUY_UNTIL, 0);

        if (lastLevel > 0 && rebuyUntilLevel > 0 && rebuyUntilLevel < lastLevel) {
            result.addWarning(ValidationWarning.UNREACHABLE_LEVELS,
                    "Rebuy period ends at level " + rebuyUntilLevel + " but last level is " + lastLevel);
        }
    }

    /**
     * Check if payout spots exceed player count.
     */
    private void checkPayoutSpots(ValidationResult result) {
        int numPlayers = map.getInteger(PARAM_NUMPLAYERS, 0);
        int numSpots = callbacks.getNumSpots();

        if (numPlayers > 0 && numSpots > numPlayers) {
            result.addWarning(ValidationWarning.TOO_MANY_PAYOUT_SPOTS,
                    numSpots + " payout spots configured but only " + numPlayers + " players");
        }
    }

    /**
     * Check if starting chip depth is shallow (< 10 big blinds).
     */
    private void checkStartingDepth(ValidationResult result) {
        int buyinChips = map.getInteger(PARAM_BUYINCHIPS, 0);
        String bigBlindStr = map.getString(PARAM_BIG + "1");

        if (buyinChips <= 0 || bigBlindStr == null || bigBlindStr.isEmpty()) {
            return;
        }

        try {
            int bigBlind = Integer.parseInt(bigBlindStr.trim());
            if (bigBlind <= 0) {
                return;
            }

            int depth = buyinChips / bigBlind;
            if (depth < 10) {
                result.addWarning(ValidationWarning.SHALLOW_STARTING_DEPTH,
                        "Starting depth is " + depth + " big blinds (< 10)");
            }
        } catch (NumberFormatException e) {
            // Invalid big blind value, skip check
        }
    }

    /**
     * Check if house take exceeds 20% of buy-in.
     */
    private void checkHouseTake(ValidationResult result) {
        int houseType = map.getInteger(PARAM_HOUSE, PokerConstants.HOUSE_PERC);

        if (houseType == PokerConstants.HOUSE_PERC) {
            int housePercent = map.getInteger(PARAM_HOUSEPERC, 0);
            if (housePercent > 20) {
                result.addWarning(ValidationWarning.EXCESSIVE_HOUSE_TAKE,
                        "House take is " + housePercent + "% (> 20%)");
            }
        } else if (houseType == PokerConstants.HOUSE_AMOUNT) {
            int buyin = map.getInteger(PARAM_BUYIN, 0);
            int houseAmount = map.getInteger(PARAM_HOUSEAMOUNT, 0);

            if (buyin > 0) {
                double housePercent = (double) houseAmount / buyin * 100;
                if (housePercent > 20) {
                    result.addWarning(ValidationWarning.EXCESSIVE_HOUSE_TAKE, "House take is $" + houseAmount + " ("
                            + String.format("%.1f", housePercent) + "% of $" + buyin + " buy-in)");
                }
            }
        }
    }
}
