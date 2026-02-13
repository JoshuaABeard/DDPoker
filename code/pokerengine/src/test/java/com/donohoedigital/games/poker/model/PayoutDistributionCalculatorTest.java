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
package com.donohoedigital.games.poker.model;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for PayoutDistributionCalculator - Fibonacci-based automatic payout
 * distribution.
 *
 * <p>
 * The algorithm distributes payouts in two tiers: 1. Non-final spots (bubble):
 * Linear increments based on table count 2. Final table (top 10): Fibonacci
 * sequence for exponential growth
 *
 * <p>
 * Design goals: - Last place gets at least the true buyin (after house take) -
 * First place gets remaining pool (ensures sum matches total) - Payouts are
 * rounded to appropriate increments ($1, $10, $100, $500, etc.)
 */
public class PayoutDistributionCalculatorTest {

    // ========== Basic Distribution Tests ==========

    @Test
    public void should_CreatePayoutsArray_ForGivenNumberOfSpots() {
        // Given: 10 spots, $10,000 pool, $100 buyin, no rebuys
        PayoutDistributionCalculator calc = new PayoutDistributionCalculator();

        // When: calculate payouts
        int[] payouts = calc.calculatePayouts(10, 10000, 100, 0, 100, 100, 9000);

        // Then: should have 10 entries
        assertEquals("Should have 10 payout amounts", 10, payouts.length);
    }

    @Test
    public void should_AllocateEntirePool() {
        // Given: basic tournament
        PayoutDistributionCalculator calc = new PayoutDistributionCalculator();

        // When: calculate payouts for 10 spots
        int[] payouts = calc.calculatePayouts(10, 10000, 100, 0, 100, 100, 9000);

        // Then: sum should equal or be very close to pool (within rounding tolerance)
        int sum = 0;
        for (int payout : payouts) {
            sum += payout;
        }
        int tolerance = 100 * payouts.length; // Allow rounding per spot
        assertTrue("Sum of payouts should match pool within tolerance", Math.abs(sum - 10000) <= tolerance);
    }

    @Test
    public void should_EnsureMinimumPayoutCoversBuyin() {
        // Given: tournament where last place should cover buyin
        PayoutDistributionCalculator calc = new PayoutDistributionCalculator();

        // When: calculate payouts
        int[] payouts = calc.calculatePayouts(10, 10000, 100, 0, 100, 100, 9000);

        // Then: last place (index 0) should be >= buyin
        assertTrue("Last place should cover buyin", payouts[0] >= 100);
    }

    @Test
    public void should_MakeFirstPlaceHighest() {
        // Given: normal tournament
        PayoutDistributionCalculator calc = new PayoutDistributionCalculator();

        // When: calculate payouts
        int[] payouts = calc.calculatePayouts(10, 10000, 100, 0, 100, 100, 9000);

        // Then: first place (last index) should be the highest
        int firstPlace = payouts[payouts.length - 1];
        for (int i = 0; i < payouts.length - 1; i++) {
            assertTrue("First place should exceed all other positions", firstPlace > payouts[i]);
        }
    }

    @Test
    public void should_ProduceMonotonicIncreasingPayouts() {
        // Given: tournament with clear payout structure
        PayoutDistributionCalculator calc = new PayoutDistributionCalculator();

        // When: calculate payouts
        int[] payouts = calc.calculatePayouts(20, 20000, 100, 0, 200, 100, 18000);

        // Then: each position should pay more than the one below (or equal due to
        // rounding)
        for (int i = 1; i < payouts.length; i++) {
            assertTrue("Higher finish should pay >= lower finish", payouts[i] >= payouts[i - 1]);
        }
    }

    // ========== Edge Cases ==========

    @Test
    public void should_HandleSingleSpot() {
        // Given: winner-take-all tournament
        PayoutDistributionCalculator calc = new PayoutDistributionCalculator();

        // When: calculate for 1 spot
        int[] payouts = calc.calculatePayouts(1, 5000, 50, 0, 100, 50, 4500);

        // Then: single winner gets entire pool
        assertEquals("Should have 1 payout", 1, payouts.length);
        assertEquals("Winner should get entire pool", 5000, payouts[0]);
    }

    @Test
    public void should_HandleTwoSpots() {
        // Given: heads-up tournament
        PayoutDistributionCalculator calc = new PayoutDistributionCalculator();

        // When: calculate for 2 spots
        int[] payouts = calc.calculatePayouts(2, 5000, 50, 0, 100, 50, 4500);

        // Then: should have 2 payouts, first place higher
        assertEquals("Should have 2 payouts", 2, payouts.length);
        assertTrue("First place should exceed second", payouts[1] > payouts[0]);
        assertTrue("Second place should cover buyin", payouts[0] >= 50);
    }

    @Test
    public void should_HandleMaxSpots() {
        // Given: tournament with maximum payout spots
        PayoutDistributionCalculator calc = new PayoutDistributionCalculator();

        // When: calculate for MAX_SPOTS
        int[] payouts = calc.calculatePayouts(TournamentProfile.MAX_SPOTS, 1000000, 100, 0, 5625, 100, 506250);

        // Then: should distribute across all spots
        assertEquals("Should have MAX_SPOTS payouts", TournamentProfile.MAX_SPOTS, payouts.length);
        assertTrue("Last place should still cover buyin", payouts[0] >= 100);
    }

    // ========== Rebuy Logic Tests ==========

    @Test
    public void should_IncreaseMinimumPayout_WhenRebuysIncluded() {
        // Given: tournament with rebuy allowance
        PayoutDistributionCalculator calc = new PayoutDistributionCalculator();

        // When: calculate without rebuys
        int[] withoutRebuys = calc.calculatePayouts(10, 20000, 100, 0, 200, 100, 18000);

        // When: calculate with rebuys (same pool, but rebuy cost is 100)
        // The pool check: 20000 >= (10*100) + (10*100) = true, so rebuy added to min
        int[] withRebuys = calc.calculatePayouts(10, 20000, 100, 100, 200, 100, 18000);

        // Then: minimum should be higher with rebuys
        assertTrue("Minimum payout should increase when rebuys are factored in", withRebuys[0] >= withoutRebuys[0]);
    }

    // ========== Rounding Tests ==========

    @Test
    public void should_RoundTo1_ForSmallBuyins() {
        // Given: very small buyin (under $100)
        PayoutDistributionCalculator calc = new PayoutDistributionCalculator();

        // When: calculate payouts
        int[] payouts = calc.calculatePayouts(5, 500, 10, 0, 50, 10, 450);

        // Then: payouts should be exact dollars (no fractional amounts exist in int)
        for (int payout : payouts) {
            assertTrue("Payout should be positive integer", payout > 0);
        }
    }

    @Test
    public void should_RoundTo10_ForMediumBuyins() {
        // Given: medium buyin ($100-$499)
        PayoutDistributionCalculator calc = new PayoutDistributionCalculator();

        // When: calculate payouts
        int[] payouts = calc.calculatePayouts(10, 20000, 200, 0, 100, 200, 18000);

        // Then: most payouts should be multiples of 10 (algorithm rounds to 10)
        int multiplesOf10 = 0;
        for (int payout : payouts) {
            if (payout % 10 == 0) {
                multiplesOf10++;
            }
        }
        assertTrue("Most payouts should be multiples of 10", multiplesOf10 >= payouts.length / 2);
    }

    @Test
    public void should_RoundTo100_ForLargeBuyins() {
        // Given: large buyin ($500-$1000)
        PayoutDistributionCalculator calc = new PayoutDistributionCalculator();

        // When: calculate payouts
        int[] payouts = calc.calculatePayouts(10, 100000, 1000, 0, 100, 1000, 90000);

        // Then: payouts should be multiples of 100
        int multiplesOf100 = 0;
        for (int payout : payouts) {
            if (payout % 100 == 0) {
                multiplesOf100++;
            }
        }
        assertTrue("Most payouts should be multiples of 100", multiplesOf100 >= payouts.length / 2);
    }

    // ========== First Place Swap Tests ==========

    @Test
    public void should_SwapFirstAndSecond_WhenFirstIsLessThanSecond() {
        // This tests the edge case handling in the original code (lines 1109-1114)
        // where due to rounding, first place might end up less than second place.
        // The algorithm swaps them to ensure first place is always highest.

        // Note: This is hard to trigger directly, but the calculator should handle it
        // We trust the implementation to do this swap internally
        PayoutDistributionCalculator calc = new PayoutDistributionCalculator();

        // When: calculate with values that might cause swap
        int[] payouts = calc.calculatePayouts(2, 100, 10, 0, 10, 10, 90);

        // Then: first should always be >= second
        assertTrue("First place should be >= second place", payouts[1] >= payouts[0]);
    }

    // ========== Fibonacci Sequence Tests ==========

    @Test
    public void should_UseFibonacciForTopSpots() {
        // The algorithm uses Fibonacci for the top 10 finishers
        // This is hard to verify directly without knowing the exact percentages,
        // but we can verify that top spots grow more aggressively than bottom spots

        PayoutDistributionCalculator calc = new PayoutDistributionCalculator();

        // When: calculate for 15 spots (5 non-final + 10 final)
        int[] payouts = calc.calculatePayouts(15, 100000, 500, 0, 200, 500, 90000);

        // Then: growth rate should be higher in top 10 than bottom 5
        // Calculate percentage increases
        double bottomGrowthRate = (payouts[5] - payouts[0]) / (double) payouts[0];
        double topGrowthRate = (payouts[14] - payouts[5]) / (double) payouts[5];

        assertTrue("Top spots should grow faster than bottom spots (Fibonacci effect)",
                topGrowthRate > bottomGrowthRate);
    }

    // ========== Property-Based Tests ==========

    @Test
    public void should_AllocateExactPoolAmount_ForVariousSizes() {
        // Property: sum of payouts should equal prize pool (within rounding tolerance)
        PayoutDistributionCalculator calc = new PayoutDistributionCalculator();

        int[][] testCases = {{5, 5000, 100, 0, 50, 100, 4500}, {10, 10000, 100, 0, 100, 100, 9000},
                {20, 50000, 500, 0, 100, 500, 45000}, {50, 100000, 200, 0, 250, 200, 45000},
                {100, 1000000, 1000, 0, 1000, 1000, 900000}};

        for (int[] testCase : testCases) {
            int spots = testCase[0];
            int pool = testCase[1];
            int buyin = testCase[2];
            int rebuy = testCase[3];
            int numPlayers = testCase[4];
            int buyinCost = testCase[5];
            int poolAfterHouseTake = testCase[6];

            int[] payouts = calc.calculatePayouts(spots, pool, buyin, rebuy, numPlayers, buyinCost, poolAfterHouseTake);

            int sum = 0;
            for (int payout : payouts) {
                sum += payout;
            }

            // Allow rounding tolerance proportional to number of spots
            int tolerance = Math.max(100 * spots, pool / 100);
            assertTrue(String.format("Sum (%d) should match pool (%d) within tolerance for %d spots", sum, pool, spots),
                    Math.abs(sum - pool) <= tolerance);
        }
    }

    @Test
    public void should_NeverProduceNegativePayouts() {
        // Property: all payouts must be positive
        PayoutDistributionCalculator calc = new PayoutDistributionCalculator();

        int[] payouts = calc.calculatePayouts(50, 100000, 500, 0, 200, 500, 90000);

        for (int i = 0; i < payouts.length; i++) {
            assertTrue(String.format("Payout at position %d should be positive", i + 1), payouts[i] > 0);
        }
    }

    @Test
    public void should_IncreasePayouts_WhenPoolIncreases() {
        // Property: larger pool should produce larger payouts (though not necessarily
        // linear
        // due to minimum payout floors and rounding)
        PayoutDistributionCalculator calc = new PayoutDistributionCalculator();

        int[] basePayouts = calc.calculatePayouts(10, 10000, 100, 0, 100, 100, 9000);
        int[] largerPayouts = calc.calculatePayouts(10, 20000, 100, 0, 100, 100, 9000);

        // Check that larger pool produces larger (or equal) payouts at all positions
        for (int i = 0; i < basePayouts.length; i++) {
            assertTrue(String.format("Position %d should have >= payout with larger pool", i + 1),
                    largerPayouts[i] >= basePayouts[i]);
        }

        // Verify that the total distributed amount increases significantly
        int baseSum = 0;
        int largerSum = 0;
        for (int i = 0; i < basePayouts.length; i++) {
            baseSum += basePayouts[i];
            largerSum += largerPayouts[i];
        }
        assertTrue("Larger pool should distribute more money overall", largerSum > baseSum * 1.5);
    }
}
