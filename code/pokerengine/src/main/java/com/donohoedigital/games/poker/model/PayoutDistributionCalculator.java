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

import com.donohoedigital.games.poker.engine.PokerConstants;

/**
 * Calculates automatic payout distribution using Fibonacci sequence for top
 * finishers and proportional allocation for bubble spots.
 *
 * <p>
 * Algorithm Overview:
 *
 * <ol>
 * <li><strong>Non-Final Spots (Bubble):</strong> Players outside the final
 * table receive linear increments based on table count. The increment size is
 * dynamically calculated to ensure the non-final pool stays within 1-33% of
 * total pool (scaled by player count).
 * <li><strong>Final Table (Top 10):</strong> Uses Fibonacci sequence
 * (2,3,5,8,13...) to create exponential growth. Each position's share is
 * proportional to its Fibonacci value.
 * </ol>
 *
 * <p>
 * Design Goals:
 *
 * <ul>
 * <li>Last place covers the true buyin (after house take)
 * <li>First place gets the remaining pool (ensures sum matches total)
 * <li>Payouts are rounded to clean increments ($1, $10, $100, etc.)
 * <li>Rebuy cost can be factored into minimum payout
 * </ul>
 *
 * <p>
 * Extracted from TournamentProfile.setAutoSpots() to improve testability.
 *
 * @see TournamentProfile#setAutoSpots()
 */
public class PayoutDistributionCalculator {

    /** Number of top finishers who use Fibonacci-based payout distribution. */
    private static final int FINAL_SPOTS = 10;

    /**
     * Calculate payout amounts for each finishing position.
     *
     * <p>
     * The returned array is indexed from last place to first place: - Index 0 =
     * Last place payout - Index n-1 = First place payout
     *
     * @param numSpots
     *            Total number of payout spots
     * @param prizePool
     *            Total prize pool to distribute
     * @param trueBuyin
     *            Buyin amount less house take (sets minimum payout baseline)
     * @param rebuyCost
     *            Cost of a single rebuy (added to minimum if pool supports it)
     * @param numPlayers
     *            Total number of players in tournament (for non-final range
     *            scaling)
     * @param buyinCost
     *            Buyin cost per player (for rebuy check calculation)
     * @param poolAfterHouseTakeForPlayers
     *            Pool after house take for all players (for rebuy check)
     * @return Array of payout amounts, index 0 = last place, index n-1 = 1st place
     */
    public int[] calculatePayouts(int numSpots, int prizePool, int trueBuyin, int rebuyCost, int numPlayers,
            int buyinCost, int poolAfterHouseTakeForPlayers) {
        int[] amounts = new int[numSpots];

        // Calculate minimum payout (may include rebuy)
        int minPayout = calculateMinimumPayout(numSpots, prizePool, trueBuyin, rebuyCost, poolAfterHouseTakeForPlayers);

        // Determine rounding increment based on minimum payout size
        int roundingIncrement = determineRoundingIncrement(minPayout);

        // Calculate non-final (bubble) payouts
        int allocatedPool = 0;
        int index = 0;
        int numNonFinal = numSpots - FINAL_SPOTS;
        double nonFinalMultiplier = 1.0d;

        if (numNonFinal > 0) {
            nonFinalMultiplier = calculateNonFinalMultiplier(numPlayers, prizePool, minPayout, numNonFinal);

            allocatedPool = allocateNonFinalPayouts(amounts, index, numNonFinal, minPayout, nonFinalMultiplier,
                    roundingIncrement);
            index += numNonFinal;
        }

        // Calculate final table (top 10) payouts using Fibonacci
        // Pass the exact multiplier to avoid precision loss
        allocateFinalTablePayouts(amounts, index, numSpots - index, prizePool, allocatedPool, minPayout,
                roundingIncrement, nonFinalMultiplier);

        // Ensure first place is always highest (handle rounding edge cases)
        ensureFirstPlaceIsHighest(amounts);

        return amounts;
    }

    /**
     * Calculate minimum payout, potentially including rebuy cost.
     *
     * <p>
     * Rebuy is added to minimum if the prize pool has had enough rebuys to cover
     * each payout spot (as suggested by "Tex" in original implementation).
     */
    private int calculateMinimumPayout(int numSpots, int prizePool, int trueBuyin, int rebuyCost,
            int poolAfterHouseTakeForPlayers) {
        int minPayout = trueBuyin;

        // Original logic: if pool >= base pool (after house take) + (spots * rebuy
        // cost),
        // then add rebuy to minimum payout
        if (rebuyCost > 0) {
            int estimatedBasePlusRebuys = poolAfterHouseTakeForPlayers + (numSpots * rebuyCost);
            if (prizePool >= estimatedBasePlusRebuys) {
                minPayout += rebuyCost;
            }
        }

        return minPayout;
    }

    /** Determine rounding increment based on payout size. */
    private int determineRoundingIncrement(int minPayout) {
        if (minPayout < 100)
            return 1;
        else if (minPayout < 500)
            return 10;
        else if (minPayout <= 1000)
            return 100;
        else if (minPayout <= 5000)
            return 500;
        else if (minPayout <= 10000)
            return 1000;
        else
            return 5000;
    }

    /**
     * Calculate the multiplier for non-final table payouts.
     *
     * <p>
     * Non-final payouts increase linearly per table. This method finds the
     * increment size that keeps the non-final pool within an acceptable range
     * (1-33% of total pool).
     */
    private double calculateNonFinalMultiplier(int numPlayers, int prizePool, int minPayout, int numNonFinal) {
        // Calculate target range for non-final pool as percentage of total pool
        int minBottom = (int) (FINAL_SPOTS / TournamentProfile.MAX_SPOTS_PERCENT);
        double lowRange = 0.01d;
        double highRange = 0.33d;

        // Scale range based on player count (more players = higher percentage to
        // non-final)
        double targetRange = lowRange + ((highRange - lowRange) * (numPlayers - minBottom)
                / (double) (TournamentProfile.MAX_PLAYERS - minBottom));

        // Ensure we can at least pay minimum to all non-final spots
        double minRange = ((minPayout * numNonFinal) / (double) prizePool);
        if (targetRange < minRange)
            targetRange = minRange;

        // Find increment that keeps estimated non-final pool within target range
        double increment = 0.5d;
        while (true) {
            int fullTables = numNonFinal / PokerConstants.SEATS;
            int extraPlayers = numNonFinal % PokerConstants.SEATS;
            int incrementAmount = (int) (minPayout * increment);
            if (incrementAmount == 0)
                incrementAmount = 1;

            // Estimate non-final pool using formula:
            // (seats * full tables * min) + (seats * sum(1..full) * increment)
            double estimatedPool = (PokerConstants.SEATS * fullTables * minPayout)
                    + (PokerConstants.SEATS * ((fullTables * (fullTables + 1)) / 2) * incrementAmount);

            // Add extra players
            int finalIncrement = (int) (minPayout * (increment * (fullTables + 1)));
            estimatedPool += extraPlayers * (minPayout + finalIncrement);

            // Check if within range
            if (estimatedPool / prizePool <= targetRange) {
                break;
            } else {
                increment -= 0.05d;
                if (increment <= 0) {
                    increment = 0;
                    break;
                }
            }
        }

        return 1.0d + increment;
    }

    /**
     * Allocate payouts for non-final (bubble) positions.
     *
     * @return Total amount allocated to non-final positions
     */
    private int allocateNonFinalPayouts(int[] amounts, int startIndex, int numNonFinal, int minPayout,
            double multiplier, int roundingIncrement) {
        int allocatedPool = 0;
        int index = startIndex;
        int remaining = numNonFinal;
        double currentMultiplier = multiplier;

        while (remaining > 0) {
            int allocation = (int) (minPayout * currentMultiplier);

            // Round up to nearest increment
            if ((allocation % roundingIncrement) > 0) {
                allocation = allocation - (allocation % roundingIncrement) + roundingIncrement;
            }

            amounts[index] = allocation;
            allocatedPool += allocation;
            index++;
            remaining--;

            // Increase multiplier after each full table
            if (index % PokerConstants.SEATS == 0 && remaining > 0) {
                currentMultiplier += (multiplier - 1.0d); // Add increment again
            }
        }

        return allocatedPool;
    }

    /**
     * Allocate payouts for final table positions using Fibonacci sequence.
     *
     * <p>
     * Each position gets a share proportional to its Fibonacci value. First place
     * gets the remaining pool to ensure exact distribution.
     */
    private void allocateFinalTablePayouts(int[] amounts, int startIndex, int numFinalSpots, int prizePool,
            int alreadyAllocated, int minPayout, int roundingIncrement, double nonFinalMultiplier) {

        if (numFinalSpots == 0)
            return;

        // Generate Fibonacci sequence
        int[] fibonacci = generateFibonacciSequence(numFinalSpots);
        int fibonacciSum = 0;
        for (int fib : fibonacci) {
            fibonacciSum += fib;
        }

        // Calculate pool remaining for final table
        int poolRemaining = prizePool - alreadyAllocated;

        // Adjust minimum and rounding for final table
        // Use the exact non-final multiplier (not reverse-engineered from rounded
        // amount)
        double multiplierAdjustment = nonFinalMultiplier;

        int adjustedMin = (int) (minPayout * multiplierAdjustment);
        int split = poolRemaining / numFinalSpots;

        // If adjusted minimum is too high (>80% of equal split), use equal split
        // instead
        if (adjustedMin >= (split * 0.8))
            adjustedMin = 0;

        // Use smaller rounding for final table if minimum is small
        int finalRounding = adjustedMin == 0 ? 1 : roundingIncrement;

        // Round adjusted minimum
        if (adjustedMin > 0 && (adjustedMin % finalRounding) > 0) {
            adjustedMin = adjustedMin - (adjustedMin % finalRounding) + finalRounding;
        }

        // Allocate based on Fibonacci percentages
        int index = startIndex;
        int allocated = alreadyAllocated;

        for (int i = 0; i < numFinalSpots - 1; i++) {
            double percentage = (double) fibonacci[i] / (double) fibonacciSum;
            int allocation = (int) (poolRemaining * percentage);

            // Enforce minimum
            if (allocation < adjustedMin) {
                allocation = adjustedMin;
            } else {
                // Round to increment
                if ((allocation % finalRounding) > 0) {
                    allocation = allocation - (allocation % finalRounding) + finalRounding;
                }
            }

            amounts[index] = allocation;
            allocated += allocation;
            index++;
        }

        // First place gets remainder (ensures exact pool distribution)
        amounts[index] = prizePool - allocated;
    }

    /** Generate Fibonacci sequence of given length, starting with 2, 3. */
    private int[] generateFibonacciSequence(int length) {
        if (length == 0)
            return new int[0];
        if (length == 1)
            return new int[]{2};

        int[] fibonacci = new int[length];
        fibonacci[0] = 2;
        fibonacci[1] = 3;

        for (int i = 2; i < length; i++) {
            fibonacci[i] = fibonacci[i - 1] + fibonacci[i - 2];
        }

        return fibonacci;
    }

    /**
     * Ensure first place is always the highest payout.
     *
     * <p>
     * Due to rounding, first place might end up less than second place. If so, swap
     * them.
     */
    private void ensureFirstPlaceIsHighest(int[] amounts) {
        if (amounts.length > 1) {
            int firstPlace = amounts[amounts.length - 1];
            int secondPlace = amounts[amounts.length - 2];

            if (firstPlace < secondPlace) {
                amounts[amounts.length - 1] = secondPlace;
                amounts[amounts.length - 2] = firstPlace;
            }
        }
    }
}
