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

/**
 * Calculates tournament payouts across all payout modes.
 *
 * <p>
 * Handles three payout allocation strategies:
 * <ul>
 * <li><strong>PAYOUT_SPOTS:</strong> Fixed number of payout spots with specific
 * amounts
 * <li><strong>PAYOUT_PERC:</strong> Percentage of players paid (e.g., top 10%)
 * <li><strong>PAYOUT_SATELLITE:</strong> All paid same amount (satellite
 * tournament buy-in)
 * </ul>
 *
 * <p>
 * Also handles house take in two modes:
 * <ul>
 * <li><strong>HOUSE_PERC:</strong> Percentage of total pool
 * <li><strong>HOUSE_AMT:</strong> Fixed amount per player
 * </ul>
 *
 * <p>
 * Extracted from TournamentProfile to improve testability.
 *
 * @see TournamentProfile#getNumSpots()
 * @see TournamentProfile#getPayout(int)
 * @see TournamentProfile#getPoolAfterHouseTake(int)
 */
public class PayoutCalculator {

    private final DMTypedHashMap map;

    /** Parameter keys (match TournamentProfile constants). */
    private static final String PARAM_PAYOUT = "payout";
    private static final String PARAM_PAYOUTSPOTS = "payoutspots";
    private static final String PARAM_PAYOUTPERC = "payoutperc";
    private static final String PARAM_SPOTAMOUNT = "spotamount";
    private static final String PARAM_PRIZEPOOL = "prizepool";
    private static final String PARAM_HOUSE = "housecuttype";
    private static final String PARAM_HOUSEPERCENT = "housepercent";
    private static final String PARAM_HOUSEAMOUNT = "houseamount";
    private static final String PARAM_NUMPLAYERS = "numplayers";
    private static final String PARAM_ALLOC = "alloc";

    /**
     * Create a payout calculator wrapper around the given map.
     *
     * @param map
     *            The map containing payout configuration data
     */
    public PayoutCalculator(DMTypedHashMap map) {
        this.map = map;
    }

    /**
     * Get number of payout spots based on payout mode.
     *
     * <p>
     * Extracted from TournamentProfile.getNumSpots() (lines 747-774).
     *
     * @return Number of payout spots (always at least 1)
     */
    public int getNumSpots() {
        int numSpots;
        int payoutType = getPayoutType();

        if (payoutType == PokerConstants.PAYOUT_PERC) {
            // Percentage mode: calculate spots from percent of players
            int percent = map.getInteger(PARAM_PAYOUTPERC, 0);
            int numPlayers = map.getInteger(PARAM_NUMPLAYERS, 0);
            numSpots = (int) Math.ceil((((double) percent) / 100d) * (double) numPlayers);
        } else if (payoutType == PokerConstants.PAYOUT_SPOTS) {
            // Fixed spots mode
            numSpots = map.getInteger(PARAM_PAYOUTSPOTS, 0);
        } else {
            // Satellite mode: calculate spots from prize pool / spot value
            int prizePool = map.getInteger(PARAM_PRIZEPOOL, 0);
            int spotValue = getSatellitePayout();

            if (spotValue == 0) {
                numSpots = 1;
            } else {
                numSpots = prizePool / spotValue;
                int remainder = prizePool % spotValue;
                if (remainder > 0) {
                    numSpots++; // Add partial spot for remainder
                }
            }
        }

        // Always ensure at least 1 spot paid out
        if (numSpots == 0) {
            numSpots = 1;
        }

        return numSpots;
    }

    /**
     * Get payout amount for a specific finish position.
     *
     * <p>
     * Extracted from TournamentProfile.getPayout() (lines 966-1011).
     *
     * @param position
     *            Finish position (1 = first place)
     * @param numSpots
     *            Total number of payout spots
     * @param prizePool
     *            Total prize pool to distribute
     * @return Payout amount for this position (0 if position outside payout range)
     */
    public int getPayout(int position, int numSpots, int prizePool) {
        // Safety check
        if (position < 0 || position > numSpots) {
            return 0;
        }

        // Satellite allocation
        if (isAllocSatellite()) {
            int spotValue = getSatellitePayout();

            // Last spot gets any remaining amount
            int remainder = prizePool % spotValue;
            if (position == numSpots && remainder != 0) {
                return remainder;
            }

            if (prizePool < spotValue) {
                spotValue = prizePool;
            }

            return spotValue;
        } else {
            // Spot or percent allocation
            double spot = getSpot(position);

            if (isAllocPercent()) {
                // First place gets remainder to avoid rounding error
                if (position == 1) {
                    int total = 0;
                    for (int i = 2; i <= numSpots; i++) {
                        total += getPayout(i, numSpots, prizePool);
                    }
                    return prizePool - total;
                }
                return (int) (prizePool * spot / 100);
            } else {
                // Fixed amount mode
                return (int) spot;
            }
        }
    }

    /**
     * Calculate prize pool after house take.
     *
     * <p>
     * Extracted from TournamentProfile.getPoolAfterHouseTake() (lines 850-859).
     *
     * @param pool
     *            Total pool before house take
     * @return Pool after house take is deducted
     */
    public int getPoolAfterHouseTake(int pool) {
        int numPlayers = map.getInteger(PARAM_NUMPLAYERS, 0);

        if (getHouseCutType() == PokerConstants.HOUSE_PERC) {
            // Percentage-based house take
            int housePercent = map.getInteger(PARAM_HOUSEPERCENT, 0);
            pool -= (((double) housePercent) / 100d) * (double) pool;
        } else {
            // Fixed amount per player
            int houseAmount = map.getInteger(PARAM_HOUSEAMOUNT, 0);
            pool -= houseAmount * numPlayers;
        }

        return pool;
    }

    /**
     * Calculate prize pool from number of players and buyin cost.
     *
     * <p>
     * Simplified version of TournamentProfile.getPrizePool() that calculates pool
     * from players and buyin (doesn't check for pre-set pool value).
     *
     * @param numPlayers
     *            Number of players
     * @param buyinCost
     *            Buyin cost per player
     * @return Prize pool after house take
     */
    public int getPrizePool(int numPlayers, int buyinCost) {
        int totalPool = numPlayers * buyinCost;
        return getPoolAfterHouseTake(totalPool);
    }

    /**
     * Get payout type (SPOTS, PERC, or SATELLITE).
     */
    private int getPayoutType() {
        return map.getInteger(PARAM_PAYOUT, PokerConstants.PAYOUT_SPOTS);
    }

    /**
     * Get house cut type (PERC or AMT).
     */
    private int getHouseCutType() {
        return map.getInteger(PARAM_HOUSE, PokerConstants.HOUSE_PERC);
    }

    /**
     * Get satellite payout value (same as spot 1 amount).
     */
    private int getSatellitePayout() {
        return (int) getSpot(1);
    }

    /**
     * Check if allocation mode is satellite.
     */
    private boolean isAllocSatellite() {
        return getPayoutType() == PokerConstants.PAYOUT_SATELLITE;
    }

    /**
     * Check if allocation mode is percentage.
     */
    private boolean isAllocPercent() {
        int allocType = map.getInteger(PARAM_ALLOC, PokerConstants.ALLOC_AUTO);
        return !isAllocSatellite() && allocType == PokerConstants.ALLOC_PERC;
    }

    /**
     * Get payout spot amount as double.
     */
    private double getSpot(int position) {
        String value = map.getString(PARAM_SPOTAMOUNT + position);
        if (value == null || value.length() == 0) {
            return 0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
