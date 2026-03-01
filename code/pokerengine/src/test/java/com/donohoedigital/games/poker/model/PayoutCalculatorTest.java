/*
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Non-commercial use of DD Poker artwork, logos, and other creative assets
 * is permitted under CC BY-NC-ND 4.0 (https://creativecommons.org/licenses/by-nc-nd/4.0/).
 * Commercial use is prohibited without written permission.
 */
package com.donohoedigital.games.poker.model;

import com.donohoedigital.comms.DMTypedHashMap;
import com.donohoedigital.games.poker.engine.PokerConstants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for PayoutCalculator - centralizes payout calculation across all modes.
 *
 * <p>
 * PayoutCalculator handles three payout allocation strategies: - PAYOUT_SPOTS:
 * Fixed number of payout spots with specific amounts - PAYOUT_PERC: Percentage
 * of players paid (e.g., top 10%) - PAYOUT_SATELLITE: All paid same amount
 * (satellite tournament buy-in)
 *
 * <p>
 * Also handles house take in two modes: - HOUSE_PERC: Percentage of total pool
 * - HOUSE_AMOUNT: Fixed amount per player
 */
public class PayoutCalculatorTest {

    // ========== getNumSpots() Tests ==========

    @Test
    public void should_ReturnPayoutSpots_WhenModeIsSpots() {
        // Given: payout mode is SPOTS with 10 spots defined
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_SPOTS);
        map.setInteger("payoutspots", 10);

        PayoutCalculator calc = new PayoutCalculator(map);

        // When: get num spots
        int spots = calc.getNumSpots();

        // Then: should return 10
        assertThat(spots).isEqualTo(10);
    }

    @Test
    public void should_CalculateSpotsFromPercent_WhenModeIsPerc() {
        // Given: payout mode is PERC, 10% of 100 players
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_PERC);
        map.setInteger("payoutperc", 10);
        map.setInteger("numplayers", 100);

        PayoutCalculator calc = new PayoutCalculator(map);

        // When: get num spots
        int spots = calc.getNumSpots();

        // Then: should return 10 (10% of 100)
        assertThat(spots).isEqualTo(10);
    }

    @Test
    public void should_RoundUpSpotsFromPercent_WhenNotExactMultiple() {
        // Given: 10% of 95 players = 9.5
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_PERC);
        map.setInteger("payoutperc", 10);
        map.setInteger("numplayers", 95);

        PayoutCalculator calc = new PayoutCalculator(map);

        // When: get num spots
        int spots = calc.getNumSpots();

        // Then: should round up to 10
        assertThat(spots).isEqualTo(10);
    }

    @Test
    public void should_CalculateSpotsFromSatellite_WhenModeIsSatellite() {
        // Given: satellite mode, prize pool 10,000, spot value 1,000
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_SATELLITE);
        map.setString("spotamount1", "1000");
        map.setInteger("prizepool", 10000);

        PayoutCalculator calc = new PayoutCalculator(map);

        // When: get num spots
        int spots = calc.getNumSpots();

        // Then: should return 10 (10000 / 1000)
        assertThat(spots).isEqualTo(10);
    }

    @Test
    public void should_IncludeRemainderSpot_WhenSatellitePoolNotDivisible() {
        // Given: prize pool 10,500, spot value 1,000 (10 full + 500 remainder)
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_SATELLITE);
        map.setString("spotamount1", "1000");
        map.setInteger("prizepool", 10500);

        PayoutCalculator calc = new PayoutCalculator(map);

        // When: get num spots
        int spots = calc.getNumSpots();

        // Then: should return 11 (10 full + 1 partial)
        assertThat(spots).isEqualTo(11);
    }

    // ========== House Take Tests ==========

    @Test
    public void should_CalculatePoolAfterHouseTake_WhenModeIsPercent() {
        // Given: total pool 10,000, house take 10%
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("housecuttype", PokerConstants.HOUSE_PERC);
        map.setInteger("housepercent", 10);

        PayoutCalculator calc = new PayoutCalculator(map);

        // When: calculate pool after house take
        int pool = calc.getPoolAfterHouseTake(10000);

        // Then: should be 9,000 (10,000 - 10%)
        assertThat(pool).isEqualTo(9000);
    }

    @Test
    public void should_CalculatePoolAfterHouseTake_WhenModeIsAmount() {
        // Given: 100 players, house take $10 per player
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("housecuttype", PokerConstants.HOUSE_AMOUNT);
        map.setInteger("houseamount", 10);
        map.setInteger("numplayers", 100);

        PayoutCalculator calc = new PayoutCalculator(map);

        // When: calculate pool after house take
        int pool = calc.getPoolAfterHouseTake(10000);

        // Then: should be 9,000 (10,000 - 100*10)
        assertThat(pool).isEqualTo(9000);
    }

    // ========== getPayout() Tests ==========

    @Test
    public void should_ReturnSpotAmount_WhenAmountStoredWithDollarPrefix() {
        // Given: AUTO allocation stores amounts with "$" prefix (e.g. "$180") via
        // TournamentProfile.setAutoSpots() which uses FORMAT_AMOUNT = "${0}"
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_SPOTS);
        map.setInteger("payoutspots", 3);
        map.setString("spotamount1", "$5000"); // AUTO format: dollar-prefixed
        map.setString("spotamount2", "$3000");
        map.setString("spotamount3", "$2000");

        PayoutCalculator calc = new PayoutCalculator(map);

        // When: get payout for each position
        int first = calc.getPayout(1, 3, 10000);
        int second = calc.getPayout(2, 3, 10000);
        int third = calc.getPayout(3, 3, 10000);

        // Then: should parse through "$" prefix and return correct amounts
        assertThat(first).isEqualTo(5000);
        assertThat(second).isEqualTo(3000);
        assertThat(third).isEqualTo(2000);
    }

    @Test
    public void should_ReturnSpotAmount_WhenModeIsSpots() {
        // Given: spot mode with defined amounts
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_SPOTS);
        map.setInteger("payoutspots", 3);
        map.setString("spotamount1", "5000"); // 1st place
        map.setString("spotamount2", "3000"); // 2nd place
        map.setString("spotamount3", "2000"); // 3rd place

        PayoutCalculator calc = new PayoutCalculator(map);

        // When: get payout for each position
        int first = calc.getPayout(1, 3, 10000);
        int second = calc.getPayout(2, 3, 10000);
        int third = calc.getPayout(3, 3, 10000);

        // Then: should return spot amounts
        assertThat(first).isEqualTo(5000);
        assertThat(second).isEqualTo(3000);
        assertThat(third).isEqualTo(2000);
    }

    @Test
    public void should_GiveFirstPlaceRemainder_WhenModeIsPercent() {
        // Given: percent mode where amounts don't sum exactly to pool
        // Spot values are percentages: 2nd gets 30%, 3rd gets 20%, 1st gets remainder
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_PERC);
        map.setInteger("alloc", PokerConstants.ALLOC_PERC); // Percent allocation
        map.setInteger("payoutspots", 3);
        map.setString("spotamount1", "0"); // Will be calculated as remainder
        map.setString("spotamount2", "30"); // 30% of pool
        map.setString("spotamount3", "20"); // 20% of pool

        PayoutCalculator calc = new PayoutCalculator(map);

        // When: get payout for 1st place with pool of 10,000
        int first = calc.getPayout(1, 3, 10000);

        // Then: should be 5,000 (10,000 - 3,000 - 2,000) to avoid rounding error
        assertThat(first).isEqualTo(5000);
    }

    @Test
    public void should_GiveLastSpotRemainder_WhenModeIsSatellite() {
        // Given: satellite mode with partial last spot
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_SATELLITE);
        map.setString("spotamount1", "1000");
        map.setInteger("prizepool", 10500); // 10 full spots + 500 remainder

        PayoutCalculator calc = new PayoutCalculator(map);

        // When: get payout for last spot (11th)
        int last = calc.getPayout(11, 11, 10500);

        // Then: should be 500 (remainder)
        assertThat(last).isEqualTo(500);
    }

    @Test
    public void should_GiveFullAmount_WhenNotLastSatelliteSpot() {
        // Given: satellite mode
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_SATELLITE);
        map.setString("spotamount1", "1000");
        map.setInteger("prizepool", 10500);

        PayoutCalculator calc = new PayoutCalculator(map);

        // When: get payout for non-last spots
        int first = calc.getPayout(1, 11, 10500);
        int tenth = calc.getPayout(10, 11, 10500);

        // Then: should be full amount (1000)
        assertThat(first).isEqualTo(1000);
        assertThat(tenth).isEqualTo(1000);
    }

    // ========== getPrizePool() Tests ==========

    @Test
    public void should_CalculatePrizePool_FromBuyinAndPlayers() {
        // Given: 100 players, $100 buyin, 10% house
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("numplayers", 100);
        map.setInteger("housecuttype", PokerConstants.HOUSE_PERC);
        map.setInteger("housepercent", 10);

        PayoutCalculator calc = new PayoutCalculator(map);

        // When: calculate prize pool
        int pool = calc.getPrizePool(100, 100);

        // Then: should be 9,000 (10,000 - 10%)
        assertThat(pool).isEqualTo(9000);
    }

    @Test
    public void should_CalculatePrizePool_WithFixedHouseTake() {
        // Given: 100 players, $100 buyin, $5 house per player
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("numplayers", 100);
        map.setInteger("housecuttype", PokerConstants.HOUSE_AMOUNT);
        map.setInteger("houseamount", 5);

        PayoutCalculator calc = new PayoutCalculator(map);

        // When: calculate prize pool
        int pool = calc.getPrizePool(100, 100);

        // Then: should be 9,500 (10,000 - 100*5)
        assertThat(pool).isEqualTo(9500);
    }

    // ========== Heads-Up Tests ==========

    @Test
    public void should_GiveWinnerEverything_WhenHeadsUp() {
        // Given: heads-up (2 players), winner-take-all: 1 payout spot
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_SPOTS);
        map.setInteger("payoutspots", 1);
        map.setString("spotamount1", "2000"); // full prize pool

        PayoutCalculator calc = new PayoutCalculator(map);

        // When: winner (1st of 2) collects payout; 2nd place is outside the spots
        int winner = calc.getPayout(1, 1, 2000);
        int loser = calc.getPayout(2, 1, 2000);

        // Then: winner takes all, loser gets nothing
        assertThat(winner).isEqualTo(2000);
        assertThat(loser).isEqualTo(0);
    }

    // ========== Full Table Tests ==========

    @Test
    public void should_PayTopThree_WhenFullTableOfNine() {
        // Given: full table of 9 players, top 3 paid, fixed spot amounts
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_SPOTS);
        map.setInteger("payoutspots", 3);
        map.setString("spotamount1", "4500"); // 50% of 9000
        map.setString("spotamount2", "2700"); // 30% of 9000
        map.setString("spotamount3", "1800"); // 20% of 9000

        PayoutCalculator calc = new PayoutCalculator(map);

        // When: calculate payouts for all 9 players
        int first = calc.getPayout(1, 3, 9000);
        int second = calc.getPayout(2, 3, 9000);
        int third = calc.getPayout(3, 3, 9000);
        int fourth = calc.getPayout(4, 3, 9000);

        // Then: top 3 receive their amounts, 4th and below receive nothing
        assertThat(first).isEqualTo(4500);
        assertThat(second).isEqualTo(2700);
        assertThat(third).isEqualTo(1800);
        assertThat(fourth).isEqualTo(0);
    }

    // ========== Rounding and Sum-to-100% Tests ==========

    @Test
    public void should_SumToFullPool_WhenUsingPercentAllocation() {
        // Given: 3 payout spots with percentages that must sum to 100%
        // 1st=50%, 2nd=30%, 3rd=20% — 1st gets remainder to avoid rounding drift
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_PERC);
        map.setInteger("alloc", PokerConstants.ALLOC_PERC);
        map.setInteger("payoutspots", 3);
        map.setString("spotamount1", "0"); // 1st gets remainder
        map.setString("spotamount2", "30"); // 30% of pool
        map.setString("spotamount3", "20"); // 20% of pool

        PayoutCalculator calc = new PayoutCalculator(map);
        int prizePool = 10000;

        // When: sum all payouts
        int first = calc.getPayout(1, 3, prizePool);
        int second = calc.getPayout(2, 3, prizePool);
        int third = calc.getPayout(3, 3, prizePool);

        // Then: exact amounts are correct and sum exactly to the full prize pool
        assertThat(second).isEqualTo(3000);
        assertThat(third).isEqualTo(2000);
        assertThat(first).isEqualTo(5000); // remainder: 10000 - 3000 - 2000
        assertThat(first + second + third).isEqualTo(prizePool);
    }

    // ========== Edge Cases ==========

    @Test
    public void should_Return1Spot_WhenPayoutTypeInvalid() {
        // Given: invalid payout type
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", 999); // Invalid type

        PayoutCalculator calc = new PayoutCalculator(map);

        // When: get num spots
        int spots = calc.getNumSpots();

        // Then: should default to 1
        assertThat(spots).isEqualTo(1);
    }

    @Test
    public void should_HandleZeroHouseTake() {
        // Given: no house take
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("housecuttype", PokerConstants.HOUSE_PERC);
        map.setInteger("housepercent", 0);

        PayoutCalculator calc = new PayoutCalculator(map);

        // When: calculate pool after house take
        int pool = calc.getPoolAfterHouseTake(10000);

        // Then: should be full amount
        assertThat(pool).isEqualTo(10000);
    }

    @Test
    public void should_Return0ForNonExistentSpot() {
        // Given: 3 spots defined
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_SPOTS);
        map.setInteger("payoutspots", 3);

        PayoutCalculator calc = new PayoutCalculator(map);

        // When: get payout for 4th spot (doesn't exist)
        int fourth = calc.getPayout(4, 3, 10000);

        // Then: should return 0
        assertThat(fourth).isEqualTo(0);
    }
}
