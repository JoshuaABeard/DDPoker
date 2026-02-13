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
package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.engine.*;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for HandStat - hand performance tracking with expectation calculations.
 */
class HandStatTest {

    private HandSorted hand;

    @BeforeEach
    void setUp() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        hand = new HandSorted(Card.SPADES_A, Card.SPADES_K); // AKs
        // Reset static noise to default
        HandStat.noise_ = 0.40;
    }

    // ========== Constructor Tests ==========

    @Test
    void should_CreateHandStat_WithInitialValues() {
        HandStat stat = new HandStat(hand);

        assertThat(stat.hand).isSameAs(hand);
        assertThat(stat.nChip).isZero();
        assertThat(stat.nCnt).isZero();
        assertThat(stat.nWon).isZero();
    }

    // ========== Record Tests ==========

    @Test
    void should_RecordWin() {
        HandStat stat = new HandStat(hand);

        stat.record(50);

        assertThat(stat.nCnt).isEqualTo(1);
        assertThat(stat.nChip).isEqualTo(50);
        assertThat(stat.nWon).isEqualTo(1);
    }

    @Test
    void should_RecordLoss() {
        HandStat stat = new HandStat(hand);

        stat.record(-10);

        assertThat(stat.nCnt).isEqualTo(1);
        assertThat(stat.nChip).isEqualTo(-10);
        assertThat(stat.nWon).isZero(); // No win recorded for negative amount
    }

    @Test
    void should_RecordZero() {
        HandStat stat = new HandStat(hand);

        stat.record(0);

        assertThat(stat.nCnt).isEqualTo(1);
        assertThat(stat.nChip).isZero();
        assertThat(stat.nWon).isZero();
    }

    @Test
    void should_RecordMultipleResults() {
        HandStat stat = new HandStat(hand);

        stat.record(50); // Win
        stat.record(-10); // Loss
        stat.record(30); // Win
        stat.record(-10); // Loss

        assertThat(stat.nCnt).isEqualTo(4);
        assertThat(stat.nChip).isEqualTo(60); // 50 - 10 + 30 - 10
        assertThat(stat.nWon).isEqualTo(2);
    }

    // ========== Expectation Calculation Tests ==========

    @Test
    void should_CalculateExpectation_AsMultiplierOfBet() {
        HandStat stat = new HandStat(hand);
        stat.record(20); // 20 chips on 10 bet = 2.0x

        double expectation = stat.getExpectation();

        assertThat(expectation).isEqualTo(2.0);
    }

    @Test
    void should_CalculateExpectation_ForBreakEven() {
        HandStat stat = new HandStat(hand);
        stat.record(10); // 10 chips on 10 bet = 1.0x (break even)

        assertThat(stat.getExpectation()).isEqualTo(1.0);
    }

    @Test
    void should_CalculateExpectation_ForLoss() {
        HandStat stat = new HandStat(hand);
        stat.record(-10); // -10 chips on 10 bet = -1.0x

        assertThat(stat.getExpectation()).isEqualTo(-1.0);
    }

    @Test
    void should_CalculateExpectation_AverageOverMultipleHands() {
        HandStat stat = new HandStat(hand);
        stat.record(50); // +50
        stat.record(-10); // -10
        // Total: +40 chips over 2 hands = 20 per hand = 2.0x

        assertThat(stat.getExpectation()).isEqualTo(2.0);
    }

    @Test
    void should_ReturnLastExpectation_WhenZeroCount() {
        HandStat stat = new HandStat(hand);
        stat.lastExpectation_ = 1.5;

        double expectation = stat.getExpectation();

        assertThat(expectation).isEqualTo(1.5);
    }

    @Test
    void should_GetExpectationX_MultiplyBy1000() {
        HandStat stat = new HandStat(hand);
        stat.record(20); // Expectation = 2.0

        assertThat(stat.getExpectationX()).isEqualTo(2000.0);
    }

    // ========== Fix Expectation Tests ==========

    @Test
    void should_FixExpectation_SaveCurrentValues() {
        HandStat stat = new HandStat(hand);
        stat.record(20);
        stat.record(30);

        stat.fixExpectation();

        assertThat(stat.lastExpectation_).isEqualTo(2.5); // (20+30)/2 / 10
        assertThat(stat.nLastChip).isEqualTo(50);
        assertThat(stat.nLastCnt).isEqualTo(2);
        assertThat(stat.nLastWon).isEqualTo(2);
    }

    @Test
    void should_PreserveFixedValues_AfterMoreRecords() {
        HandStat stat = new HandStat(hand);
        stat.record(20);
        stat.fixExpectation();

        stat.record(10);

        assertThat(stat.lastExpectation_).isEqualTo(2.0); // Still the fixed value
        assertThat(stat.nLastChip).isEqualTo(20); // Still the fixed value
    }

    // ========== isExpectationPositive Tests ==========

    @Test
    void should_BePositive_WhenExpectationAboveNegativeNoise() {
        HandStat stat = new HandStat(hand);
        stat.lastExpectation_ = 0.1; // Positive
        HandStat.noise_ = 0.4;

        assertThat(stat.isExpectationPositive()).isTrue();
    }

    @Test
    void should_BePositive_WhenExpectationEqualsNegativeNoise() {
        HandStat stat = new HandStat(hand);
        stat.lastExpectation_ = -0.4;
        HandStat.noise_ = 0.4; // lastExp + noise = 0.0

        assertThat(stat.isExpectationPositive()).isTrue(); // >= 0
    }

    @Test
    void should_NotBePositive_WhenExpectationBelowNegativeNoise() {
        HandStat stat = new HandStat(hand);
        stat.lastExpectation_ = -0.5;
        HandStat.noise_ = 0.4; // lastExp + noise = -0.1

        assertThat(stat.isExpectationPositive()).isFalse();
    }

    // ========== Noise Management Tests ==========

    @Test
    void should_LowerNoise_ByAdjustment() {
        HandStat.noise_ = 0.40;
        HandStat.noiseAdj_ = 0.05;

        HandStat.lowerNoise();

        assertThat(HandStat.noise_).isCloseTo(0.35, within(0.001));
    }

    @Test
    void should_NotGoNegative_WhenLoweringNoise() {
        HandStat.noise_ = 0.02;
        HandStat.noiseAdj_ = 0.05;

        HandStat.lowerNoise();

        assertThat(HandStat.noise_).isZero();
    }

    @Test
    void should_LowerNoise_MultipleTimes() {
        HandStat.noise_ = 0.40;
        HandStat.noiseAdj_ = 0.10;

        HandStat.lowerNoise();
        HandStat.lowerNoise();
        HandStat.lowerNoise();

        assertThat(HandStat.noise_).isCloseTo(0.10, within(0.001));
    }

    // ========== Comparable/Sorting Tests ==========

    @Test
    void should_SortByExpectation_Descending() {
        HandStat stat1 = new HandStat(hand);
        HandStat stat2 = new HandStat(hand);

        stat1.record(30); // Expectation = 3.0
        stat2.record(10); // Expectation = 1.0

        assertThat(stat1.compareTo(stat2)).isLessThan(0); // stat1 > stat2, so compareTo < 0
    }

    @Test
    void should_SortEqual_WhenSameExpectation() {
        HandStat stat1 = new HandStat(hand);
        HandStat stat2 = new HandStat(hand);

        stat1.record(20);
        stat2.record(20);

        assertThat(stat1.compareTo(stat2)).isZero();
    }

    // ========== toString Tests ==========

    @Test
    void should_GenerateToString_WithAllStats() {
        HandStat stat = new HandStat(hand);
        stat.record(50);
        stat.record(30);
        stat.fixExpectation();

        String str = stat.toString();

        assertThat(str).contains("K");
        assertThat(str).contains("A");
        assertThat(str).contains("80"); // chips
        assertThat(str).contains("2"); // hands
        assertThat(str).contains("2"); // wins
    }

    // ========== Edge Case Tests ==========

    @Test
    void should_HandleZeroExpectation() {
        HandStat stat = new HandStat(hand);
        stat.record(0);

        assertThat(stat.getExpectation()).isZero();
    }

    @Test
    void should_HandleLargePositiveExpectation() {
        HandStat stat = new HandStat(hand);
        stat.record(1000);

        assertThat(stat.getExpectation()).isEqualTo(100.0);
    }

    @Test
    void should_HandleLargeNegativeExpectation() {
        HandStat stat = new HandStat(hand);
        stat.record(-500);

        assertThat(stat.getExpectation()).isEqualTo(-50.0);
    }

    @Test
    void should_TrackWinsCorrectly_WithMixedResults() {
        HandStat stat = new HandStat(hand);

        stat.record(10); // Win
        stat.record(-5); // Loss
        stat.record(1); // Win (any positive is a win)
        stat.record(-10); // Loss
        stat.record(5); // Win

        assertThat(stat.nWon).isEqualTo(3);
        assertThat(stat.nCnt).isEqualTo(5);
    }
}
